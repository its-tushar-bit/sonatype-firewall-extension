/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.experimental.dto.ApiOwnerUserRateLimitsDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiRateLimitDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiUserRateLimitsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOwnerDTO;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiPullRequestResults;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.git.IqForScmLicenseChecker;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlRepositoryUtils;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.nexus.scm.api.GeneralSCMApiClient;
import com.sonatype.nexus.scm.api.model.RateLimitsResponse;

import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_PRIORITY_HIGHER;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.REPOSITORY_URL_UPDATED_EVENT;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Named
@Singleton
public class ApiSourceControlService
{
  private static final Logger log = LoggerFactory.getLogger(ApiSourceControlService.class);

  static final String ENC = "CMMDwoV";

  private final PlexusCipher plexusCipher;

  private final SourceControlDAO sourceControlDAO;

  private final OwnerDAO ownerDAO;

  private final ApplicationDAO applicationDAO;

  private final AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO;

  private final IqForScmLicenseChecker licenseChecker;

  private final TelemetrySender telemetrySender;

  private final SourceControlPullRequestMetrics sourceControlPullRequestMetrics;

  private final SourceControlEventDAO sourceControlEventDAO;

  private final InsightWork insightWork;

  private final FileCleaner fileCleaner;

  private final SourceControlRepositoryUtils sourceControlRepositoryUtils;

  private final GitClientFactory gitClientFactory;

  @Inject
  public ApiSourceControlService(
      final PlexusCipher plexusCipher,
      final SourceControlDAO sourceControlDAO,
      final OwnerDAO ownerDAO,
      final ApplicationDAO applicationDAO,
      final AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO,
      final IqForScmLicenseChecker licenseChecker,
      final TelemetrySender telemetrySender,
      final SourceControlPullRequestMetrics sourceControlPullRequestMetrics,
      final SourceControlEventDAO sourceControlEventDAO,
      final InsightWork insightWork,
      final FileCleaner fileCleaner,
      final SourceControlRepositoryUtils sourceControlRepositoryUtils,
      final GitClientFactory gitClientFactory)
  {
    this.plexusCipher = plexusCipher;
    this.sourceControlDAO = sourceControlDAO;
    this.ownerDAO = ownerDAO;
    this.applicationDAO = applicationDAO;
    this.automaticSourceControlConfigurationDAO = automaticSourceControlConfigurationDAO;
    this.licenseChecker = licenseChecker;
    this.telemetrySender = telemetrySender;
    this.sourceControlPullRequestMetrics = sourceControlPullRequestMetrics;
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.insightWork = insightWork;
    this.fileCleaner = fileCleaner;
    this.sourceControlRepositoryUtils = sourceControlRepositoryUtils;
    this.gitClientFactory = gitClientFactory;
  }

  @Authorize(permission = Permission.READ)
  public List<ApiSourceControlDTO> getAll() {
    checkLicense();
    List<SourceControl> sourceControlDAOAll = sourceControlDAO.getAll();
    sourceControlDAOAll.forEach(this::encryptToken);
    return sourceControlDAOAll.stream()
        .map(ApiSourceControlAdapter::convertToDTO)
        .collect(Collectors.toList());
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ApiSourceControlDTO addOrUpdateSourceControlFromAppEvaluation(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) final String publicId,
      final String repositoryUrl)
  {
    return addOrUpdateSourceControl(publicId, repositoryUrl, false);
  }

  @Authorize(permission = Permission.ADD_APPLICATION)
  public ApiSourceControlDTO addOrUpdateSourceControl(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) final String publicId,
      final String repositoryUrl,
      final String sshUrl,
      final String defaultBranch)
  {
    return addOrUpdateSourceControl(publicId, repositoryUrl, sshUrl, true, defaultBranch);
  }

  private ApiSourceControlDTO addOrUpdateSourceControl(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) final String publicId,
      final String repositoryUrl,
      final boolean bypassAutomatedSCM)
  {
    return addOrUpdateSourceControl(publicId, repositoryUrl, null, bypassAutomatedSCM, null);
  }

  private ApiSourceControlDTO addOrUpdateSourceControl(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) final String publicId,
      String repositoryUrl,
      String sshUrl,
      final boolean bypassAutomatedSCM,
      final String defaultBranch)
  {
    checkLicense();

    Application application = applicationDAO.getByPublicId(publicId);
    if (application == null) {
      throw new NotFoundException("Cannot find application with public ID: '" + publicId + "'");
    }

    // check if we can find the http url of the repository from the ssh url
    if (sourceControlDAO.getByOwnerId(application.getId()) == null && repositoryUrl.startsWith("git@")) {
      String httpUrlFromSshUrl = sourceControlRepositoryUtils.getRepositoryHttpUrlFromSshUrl(repositoryUrl);
      log.debug("HTTP URL derived: {} from provided repository URL: {}", httpUrlFromSshUrl, repositoryUrl);
      if (httpUrlFromSshUrl != null) {
        if (sourceControlRepositoryUtils.isRepositoryReachable(application, httpUrlFromSshUrl)) {
          log.debug("Using derived URL {} for source control.", httpUrlFromSshUrl);
          sshUrl = repositoryUrl;
          repositoryUrl = httpUrlFromSshUrl;
        }
        else {
          log.debug("Derived HTTP URL not reachable/accessible.");
        }
      }
    }

    SourceControl sourceControl = sourceControlDAO.getByOwnerId(application.getId());
    validateUrl(repositoryUrl);

    // check if automatic source control is enabled or bypassed
    if (bypassAutomatedSCM || automaticSourceControlConfigurationDAO.isSourceControlConfigurationEnabled()) {
      if (sourceControl == null) { // create new record
        sourceControl = new SourceControl.Builder()
            .setOwnerId(application.getId())
            .setRepositoryUrl(repositoryUrl)
            .setRepositorySshUrl(sshUrl)
            .setBaseBranch(defaultBranch)
            .build();
        sourceControlDAO.insert(sourceControl);
        auditSourceControl(sourceControl);
        setTokenValueForReturn(sourceControl);
        sendSourceControlTelemetryData(METHOD.ADD_OR_UPDATE,
            getCompositeSourceControl(OwnerType.APPLICATION, sourceControl));
      }
      else if (shouldUpdateSourceControlRepositoryUrl(sourceControl.getRepositoryUrl())) {
        sourceControl.setRepositoryUrl(repositoryUrl);
        sourceControl.setRepositorySshUrl(sshUrl);
        sourceControlDAO.update(sourceControl);
        auditSourceControl(sourceControl);
        setTokenValueForReturn(sourceControl);
        sendSourceControlTelemetryData(METHOD.ADD_OR_UPDATE,
            getCompositeSourceControl(OwnerType.APPLICATION, sourceControl));
      }
      else {
        log.debug("Skipping update of source control repository URL from {} to {}",
            sourceControl.getRepositoryUrl(), repositoryUrl);
      }
    }

    return ApiSourceControlAdapter.convertToDTO(setTokenValueForReturn(sourceControl));
  }

  @Authorize(permission = Permission.READ)
  public ApiSourceControlDTO getSourceControlByOwner(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId)
  {
    checkLicense();
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(ownerId);
    if (null == sourceControl) {
      throw new NotFoundException(String.format(
          "Cannot find SourceControl for %s with id: %s", ownerType, getPublicOwnerId(ownerId)));
    }
    setTokenValueForReturn(sourceControl);

    return ApiSourceControlAdapter.convertToDTO(sourceControl);
  }

  @Authorize(permission = Permission.WRITE)
  public ApiSourceControlDTO addSourceControlByOwner(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      ApiSourceControlDTO sourceControlDTO)
  {
    sourceControlDTO.ownerId = ownerId;
    checkLicense();

    SourceControl sourceControl = ApiSourceControlAdapter.convertFromDTO(sourceControlDTO);
    setTokenValueForSave(sourceControl);
    encryptToken(sourceControl);

    // fail if there's already a sourcecontrol in place for the owner
    if (null != sourceControlDAO.getByOwnerId(ownerId)) {
      throw new BadRequestException(String.format(
          "SourceControl already exists for %s with id: %s", ownerType, getPublicOwnerId(ownerId)));
    }

    sourceControlDAO.insert(sourceControl);
    auditSourceControl(sourceControl);
    setTokenValueForReturn(sourceControl);
    sendSourceControlTelemetryData(METHOD.ADD, getCompositeSourceControl(ownerType, sourceControl));
    return ApiSourceControlAdapter.convertToDTO(sourceControl);
  }

  @Authorize(permission = Permission.WRITE)
  public ApiSourceControlDTO updateSourceControlByOwner(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      ApiSourceControlDTO sourceControlDTO)
  {
    sourceControlDTO.ownerId = ownerId;
    checkLicense();

    SourceControl storedSourceControl = sourceControlDAO.getByOwnerId(sourceControlDTO.ownerId);
    if (null == storedSourceControl) {
      throw new NotFoundException(String.format(
          "Cannot find SourceControl for %s with id: %s", ownerType, getPublicOwnerId(ownerId)));
    }

    SourceControl sourceControl = ApiSourceControlAdapter.convertFromDTO(sourceControlDTO);
    sourceControl.setId(storedSourceControl.getId());
    sourceControl.setPullRequestPollTime(storedSourceControl.getPullRequestPollTime());
    sourceControl.setPullRequestErrorCount(storedSourceControl.getPullRequestErrorCount());

    setTokenValueForSave(sourceControl);
    // updates may come with our 'fake' token
    if (FAKE_SECRET_KEY.equalsIgnoreCase(sourceControl.getToken())) {
      sourceControl.setToken(storedSourceControl.getToken());
    }
    else {
      encryptToken(sourceControl);
    }
    if (isNotBlank(sourceControl.getRepositoryUrl())) {
      validateUrl(sourceControl.getRepositoryUrl());
    }

    boolean hasRepositoryUrlChanged = storedSourceControl.getRepositoryUrl() != null &&
        !storedSourceControl.getRepositoryUrl().equalsIgnoreCase(sourceControl.getRepositoryUrl());
    sourceControlDAO.update(sourceControl);
    if (hasRepositoryUrlChanged) {
      sourceControlEventDAO.clearEventsAndInsert(new SourceControlEvent()
          .setApplicationId(ownerId)
          .setEventType(REPOSITORY_URL_UPDATED_EVENT)
          .setEventPriority(EVENT_PRIORITY_HIGHER));
    }
    auditSourceControl(sourceControl);
    setTokenValueForReturn(sourceControl);
    sendSourceControlTelemetryData(METHOD.UPDATE, getCompositeSourceControl(ownerType, sourceControl));
    return ApiSourceControlAdapter.convertToDTO(sourceControl);
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteSourceControlByOwner(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId)
  {
    checkLicense();
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(ownerId);
    if (null == sourceControl) {
      throw new NotFoundException(String.format(
          "Cannot find SourceControl for %s with id: %s", ownerType, getPublicOwnerId(ownerId)));
    }

    if (OwnerType.APPLICATION.equals(ownerType)) {
      deleteSourceControlDirectory(ownerId);
    }
    SourceControl compositeSourceControl = getCompositeSourceControl(ownerType, sourceControl);
    sourceControlDAO.delete(sourceControl);
    auditSourceControl(sourceControl);
    sendSourceControlTelemetryData(METHOD.DELETE, compositeSourceControl);
  }

  private void deleteSourceControlDirectory(String appId) {
    File sourceControlDir = insightWork.getSourceControlDir(appId);
    try {
      fileCleaner.delete(sourceControlDir);
    }
    catch (FileDeletionException e) {
      throw new UncheckedIOException(
          "Cannot delete source control directory '" + sourceControlDir.getAbsolutePath() + "' for application ID "
              + appId + ": " + e.getMessage(),
          e);
    }
  }

  /** Builds a composite source control record starting from the given ownerId and looking up the owner hierarchy for
   *  missing fields. It also decrypts any non-empty tokens.
   *  <br/>
   *  Note: The composite source control owner ID can be different from the given owner ID.
   * @param ownerId an application or organization ID
   */
  public SourceControl getCompositeSourceControlByOwnerDecrypted(final String ownerId) {
    SourceControl sourceControl = sourceControlDAO.getCompositeSourceControlByOwnerId(ownerId);
    if (sourceControl != null && StringUtils.isNotEmpty(sourceControl.getToken())) {
      decryptToken(sourceControl);
    }
    return sourceControl;
  }

  private String getPublicOwnerId(final String ownerId) {
    Owner owner = ownerDAO.getById(ownerId);
    if (owner != null) {
      return owner.getPublicId();
    }
    return ownerId;
  }

  @VisibleForTesting
  void encryptToken(final SourceControl sourceControl) {
    synchronized (plexusCipher) {
      try {
        sourceControl.setToken(plexusCipher.encrypt(sourceControl.getToken(), ENC));
      }
      catch (PlexusCipherException e) {
        log.error("Unable to encrypt SourceControl token", e);
        throw new IllegalStateException(e);
      }
    }
  }

  private void decryptToken(final SourceControl sourceControl) {
    synchronized (plexusCipher) {
      try {
        String decrypted = plexusCipher.decrypt(sourceControl.getToken(), ENC);
        if (StringUtils.isNotBlank(decrypted)) {
          sourceControl.setToken(decrypted);
        }
        else {
          sourceControl.setToken(null);
        }
      }
      catch (PlexusCipherException e) {
        log.error("Unable to decrypt SourceControl token", e);
        throw new IllegalStateException(e);
      }
    }
  }

  private void auditSourceControl(final SourceControl sourceControl) {
    AuditData.get()
        .setData("sourceControlId", sourceControl.getId())
        .setData("repositoryUrl", sourceControl.getRepositoryUrl())
        .setData("provider", sourceControl.getProvider());
  }

  private void checkLicense() {
    if (!licenseChecker.isIqForScmSupported()) {
      log.debug("License does not support source control notification or automation features");
      throw new InvalidLicenseException();
    }
  }

  private void sendSourceControlTelemetryData(
      final METHOD method,
      final SourceControl sourceControl)
  {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("method", method);
    attributes.put("owner_id", HdsClientAnalytics.obfuscate(sourceControl.getOwnerId()));
    attributes.put("repository_url", HdsClientAnalytics.obfuscate(sourceControl.getRepositoryUrl()));
    attributes.put("provider", sourceControl.getProvider() != null ? sourceControl.getProvider().toString() : null);
    attributes.put("enable_pull_requests", sourceControl.getRemediationPullRequestsEnabled());
    attributes.put("enable_status_checks", sourceControl.getStatusChecksEnabled());
    attributes.put("base_branch", sourceControl.getBaseBranch());

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL);
    telemetryData.setAttributes(attributes);
    telemetrySender.send(telemetryData);
  }

  private SourceControl getCompositeSourceControl(OwnerType ownerType, SourceControl sourceControl) {
    if (OwnerType.APPLICATION.equals(ownerType)) {
      return sourceControlDAO.getCompositeSourceControlByOwnerId(sourceControl.getOwnerId());
    }
    return sourceControl;
  }

  private SourceControl setTokenValueForReturn(final SourceControl sourceControl) {
    if (sourceControl != null) {
      sourceControl
          .setToken(Strings.isNullOrEmpty(sourceControl.getToken()) ? null : FAKE_SECRET_KEY);
    }
    return sourceControl;
  }

  private void setTokenValueForSave(final SourceControl sourceControl) {
    sourceControl.setToken(StringUtils.isBlank(sourceControl.getToken()) ? null : sourceControl.getToken());
  }

  private boolean shouldUpdateSourceControlRepositoryUrl(String currentValue) {
    return isBlank(currentValue);
  }

  @VisibleForTesting
  void validateUrl(final String repositoryUrl) {
    boolean validUrl =
        repositoryUrl.startsWith("https:")                              // HTTPS URL
        || repositoryUrl.startsWith("http:")                            // HTTP URL
    ;
    if (!validUrl) {
      throw new BadRequestException("Unsupported repository URL format: `" + repositoryUrl + "`");
    }
  }

  @Authorize(permission = Permission.READ)
  public ApiPullRequestResults getSourceControlMetricsForApplication(
      @AuthzContext(Key.TYPE) @SuppressWarnings("unused") final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId)
  {
    checkLicense();

    return ApiSourceControlMetricsAdapter.convertToDTO(sourceControlPullRequestMetrics.metricsForApplication(ownerId));
  }

  @Authorize(permission = Permission.READ)
  public ApiOwnerUserRateLimitsDTO getRateLimits(
      @AuthzContext(Key.TYPE) @SuppressWarnings("unused") OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId)
  {
    checkLicense();
    Owner owner = ownerDAO.getById(ownerId);
    Map<String, ApiUserRateLimitsDTO> rateLimitsByUser = new HashMap<>();
    Map<String, Set<Owner>> definingOwnersByToken = new HashMap<>();
    ownerDAO.getDescendantOrSelfApplications(owner)
        .forEach(application -> addRateLimits(rateLimitsByUser, definingOwnersByToken, application));
    List<ApiUserRateLimitsDTO> rateLimits = new ArrayList<>(rateLimitsByUser.values());
    rateLimits.sort(Comparator.comparing(dto -> dto.user));
    ApiOwnerUserRateLimitsDTO result = new ApiOwnerUserRateLimitsDTO();
    result.ownerType = owner.getType().toString();
    result.ownerId = owner.getId();
    result.ownerPublicId = owner.getPublicId();
    result.ownerName = owner.getName();
    result.userRateLimits = rateLimits;
    return result;
  }

  private void addRateLimits(
      Map<String, ApiUserRateLimitsDTO> rateLimitsByUser,
      Map<String, Set<Owner>> definingOwnersByToken,
      Application application)
  {
    List<SourceControl> sourceControlsInHierarchy = getSourceControlsInHierarchy(application.getId());
    SourceControl sourceControl = new SourceControl();
    sourceControlsInHierarchy.forEach(sc -> SourceControl.coalesce(sourceControl, sc));
    if (sourceControl.getToken() == null) {
      return;
    }
    decryptToken(sourceControl);
    Owner definingOwner = getOwnerDefiningToken(sourceControlsInHierarchy);
    definingOwnersByToken.computeIfAbsent(sourceControl.getToken(), token -> new LinkedHashSet<>())
        .add(definingOwner);
    GitRepositoryInfo gitRepositoryInfo =
        SourceControlUtils.getGitRepositoryInfoForApplicationStatic(sourceControl, application.getId());
    if (gitRepositoryInfo == null) {
      return;
    }
    try {
      String user = getUser(gitRepositoryInfo);
      ApiUserRateLimitsDTO dto = rateLimitsByUser.get(user);
      if (dto != null) {
        dto.definingOwners.addAll(
            definingOwnersByToken.get(sourceControl.getToken()).stream().map(ApiOwnerDTO::fromOwner).collect(
                Collectors.toSet()));
        dto.associatedApplications.add(ApiOwnerDTO.fromOwner(application));
      }
      else {
        dto = new ApiUserRateLimitsDTO();
        dto.user = user;
        dto.provider = sourceControl.getProvider();
        dto.definingOwners = new TreeSet<>();
        dto.definingOwners.addAll(
            definingOwnersByToken.get(sourceControl.getToken()).stream().map(ApiOwnerDTO::fromOwner).collect(
                Collectors.toSet()));
        dto.associatedApplications = new TreeSet<>();
        dto.associatedApplications.add(ApiOwnerDTO.fromOwner(application));
        dto.rateLimits = getRateLimits(sourceControl).getRateLimitResponses().stream().map(ApiRateLimitDTO::convert)
            .sorted(Comparator.comparing(rateLimitDTO -> rateLimitDTO.category)).collect(Collectors.toList());
        rateLimitsByUser.put(user, dto);
      }
    }
    catch (Exception e) {
      log.error("Unable to determine rate limits for application with ID {}.", application.getId(), e);
    }
  }

  private Owner getOwnerDefiningToken(List<SourceControl> sourceControlsInHierarchy) {
    for (SourceControl sourceControl : sourceControlsInHierarchy) {
      if (sourceControl.getToken() != null) {
        return ownerDAO.getById(sourceControl.getOwnerId());
      }
    }
    return null;
  }

  private List<SourceControl> getSourceControlsInHierarchy(String applicationId) {
    List<String> ownerIds = ownerDAO.getOwnerIds(applicationId);
    List<SourceControl> unordered = sourceControlDAO.getByOwnerIds(ownerIds);
    return sourceControlDAO.orderByHierarchy(ownerIds, unordered);
  }

  private RateLimitsResponse getRateLimits(SourceControl sourceControl) throws IOException {
    GeneralSCMApiClient generalSCMApiClient =
        gitClientFactory.createGeneralApiClient(sourceControl.getProvider(), sourceControl.getRepositoryUrl(),
            sourceControl.getUsername(), sourceControl.getToken());
    return generalSCMApiClient.listAllRateLimits();
  }

  private String getUser(GitRepositoryInfo gitRepositoryInfo) {
    return gitClientFactory.createApiClient(gitRepositoryInfo).getUserId();
  }

  enum METHOD
  {
    GET_BY_OWNER_ID,
    GET_BY_APP_ID,
    ADD,
    UPDATE,
    DELETE,
    ADD_OR_UPDATE
  }
}
