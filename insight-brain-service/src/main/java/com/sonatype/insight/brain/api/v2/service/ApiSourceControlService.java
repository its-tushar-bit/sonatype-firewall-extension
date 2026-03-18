/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.utils.SourceControlAuthenticationTransitionHandler;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.sourcecontrol.ApiSourceControlRepositoryUserDTO;
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
import com.sonatype.insight.brain.sourcecontrol.SourceControlDataService;
import com.sonatype.insight.brain.git.EnhancedPullRequestResult;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.git.IqForScmLicenseChecker;
import com.sonatype.insight.brain.git.ScmRepoVisibilityService;
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
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlRepositoryUtils;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.nexus.scm.api.GeneralSCMApiClient;
import com.sonatype.nexus.scm.api.model.RateLimitsResponse;

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

  private static final int EMAIL_AND_COMMIT_DATE_MAP_LIMIT = 1000;

  private static final String REPO_VISIBILITY_PRIVATE = "private";

  private static final String REPO_VISIBILITY_PUBLIC = "public";

  private final PasswordHandler passwordHandler;

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

  private final SourceControlUserActivityService sourceControlUserActivityService;

  private final TelemetryUtils telemetryUtils;

  private final ScmRepoVisibilityService scmRepoVisibilityService;

  private final ApiSourceControlAdapter apiSourceControlAdapter;

  private final SourceControlDataService sourceControlDataService;

  private final SourceControlAuthenticationTransitionHandler sourceControlAuthenticationTransitionHandler;

  @Inject
  public ApiSourceControlService(
      final PasswordHandler passwordHandler,
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
      final GitClientFactory gitClientFactory,
      final SourceControlUserActivityService sourceControlUserActivityService,
      final TelemetryUtils telemetryUtils,
      final ScmRepoVisibilityService scmRepoVisibilityService,
      final ApiSourceControlAdapter apiSourceControlAdapter,
      final SourceControlDataService sourceControlDataService,
      final SourceControlAuthenticationTransitionHandler sourceControlAuthenticationTransitionHandler)
  {
    this.passwordHandler = passwordHandler;
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
    this.sourceControlUserActivityService = sourceControlUserActivityService;
    this.telemetryUtils = telemetryUtils;
    this.scmRepoVisibilityService = scmRepoVisibilityService;
    this.apiSourceControlAdapter = apiSourceControlAdapter;
    this.sourceControlDataService = sourceControlDataService;
    this.sourceControlAuthenticationTransitionHandler = sourceControlAuthenticationTransitionHandler;
  }

  @Authorize(permission = Permission.READ)
  public List<ApiSourceControlDTO> getAll() {
    checkLicense();
    List<SourceControl> sourceControlDAOAll = sourceControlDAO.getAll();

    return sourceControlDAOAll.stream()
        .map(this::setTokenValueForReturn)
        .map(apiSourceControlAdapter::convertToDTO)
        .collect(Collectors.toList());
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ApiSourceControlDTO addOrUpdateSourceControl(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) final String publicId,
      final String repositoryUrl,
      final ApiSourceControlRepositoryUserDTO apiSourceControlRepositoryUserDTO)
  {
    if (isBlank(repositoryUrl)) {
      throw new BadRequestException("Query parameter 'repositoryUrl' is required");
    }
    if (apiSourceControlRepositoryUserDTO != null) {
      trySaveRepoUserActivityOrFailSilently(publicId, apiSourceControlRepositoryUserDTO.emailAndCommitDateMap);
    }

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

        SourceControl compositeSourceControl = getCompositeSourceControl(OwnerType.APPLICATION, sourceControl);
        sendSourceControlTelemetryData(METHOD.ADD_OR_UPDATE, compositeSourceControl, OwnerType.APPLICATION);

        setTokenValueForReturn(sourceControl);
      }
      else if (shouldUpdateSourceControlRepositoryUrl(sourceControl.getRepositoryUrl())) {
        sourceControl.setRepositoryUrl(repositoryUrl);
        sourceControl.setRepositorySshUrl(sshUrl);
        sourceControlDAO.update(sourceControl);
        auditSourceControl(sourceControl);

        SourceControl compositeSourceControl = getCompositeSourceControl(OwnerType.APPLICATION, sourceControl);
        sendSourceControlTelemetryData(METHOD.ADD_OR_UPDATE, compositeSourceControl, OwnerType.APPLICATION);

        setTokenValueForReturn(sourceControl);
      }
      else {
        log.debug("Skipping update of source control repository URL from {} to {}",
            sourceControl.getRepositoryUrl(), repositoryUrl);
      }
    }

    return apiSourceControlAdapter.convertToDTO(setTokenValueForReturn(sourceControl));
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

    return apiSourceControlAdapter.convertToDTO(sourceControl);
  }

  @Authorize(permission = Permission.WRITE)
  public ApiSourceControlDTO addSourceControlByOwner(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      ApiSourceControlDTO sourceControlDTO)
  {
    sourceControlDTO.ownerId = ownerId;
    checkLicense();

    SourceControl sourceControl = apiSourceControlAdapter.convertFromDTO(sourceControlDTO);
    setTokenValueForSave(sourceControl);
    encryptToken(sourceControl);

    // fail if there's already a sourcecontrol in place for the owner
    if (null != sourceControlDAO.getByOwnerId(ownerId)) {
      throw new BadRequestException(String.format(
          "SourceControl already exists for %s with id: %s", ownerType, getPublicOwnerId(ownerId)));
    }

    sourceControlDAO.insert(sourceControl);
    auditSourceControl(sourceControl);

    SourceControl compositeSourceControl = getCompositeSourceControl(ownerType, sourceControl);
    sendSourceControlTelemetryData(METHOD.ADD, compositeSourceControl, ownerType);

    setTokenValueForReturn(sourceControl);
    return apiSourceControlAdapter.convertToDTO(sourceControl);
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

    SourceControl sourceControl = apiSourceControlAdapter.convertFromDTO(sourceControlDTO);
    sourceControl.setId(storedSourceControl.getId());
    sourceControl.setPullRequestPollTime(storedSourceControl.getPullRequestPollTime());
    sourceControl.setPullRequestErrorCount(storedSourceControl.getPullRequestErrorCount());
    sourceControlAuthenticationTransitionHandler.handleAuthTransition(storedSourceControl, sourceControl);

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

    SourceControl compositeSourceControl = getCompositeSourceControl(ownerType, sourceControl);
    sendSourceControlTelemetryData(METHOD.UPDATE, compositeSourceControl, ownerType);

    setTokenValueForReturn(sourceControl);
    return apiSourceControlAdapter.convertToDTO(sourceControl);
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
    sourceControlAuthenticationTransitionHandler.deleteGitHubAppInstallation(compositeSourceControl);
    sourceControlDAO.delete(sourceControl);
    auditSourceControl(sourceControl);

    sendSourceControlTelemetryData(METHOD.DELETE, compositeSourceControl, ownerType);
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

  /**
   * Builds a composite source control record starting from the given ownerId and looking up the owner hierarchy for
   * missing fields. It also decrypts any non-empty tokens.
   * <br/>
   * Note: The composite source control owner ID can be different from the given owner ID.
   *
   * @param ownerId an application or organization ID
   */
  public SourceControl getCompositeSourceControlByOwnerDecrypted(final String ownerId) {
    return sourceControlDataService.getCompositeSourceControlByOwnerDecrypted(ownerId);
  }

  public SourceControl getCompositeSourceControlByApplicationId(final String applicationId) {
    return sourceControlDataService.getCompositeSourceControlByApplicationId(applicationId);
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
    String token;
    try {
      token = passwordHandler.encryptPassword(sourceControl.getToken());
    }
    catch (IllegalStateException e) {
      log.error("Unable to encrypt SourceControl token", e);
      throw e;
    }
    sourceControl.setToken(token);
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
      final SourceControl sourceControl,
      final OwnerType ownerType)
  {
    Map<String, Object> attributes = new HashMap<>();
    String repoVisibility = getRepoVisibility(sourceControl, ownerType);
    if (repoVisibility != null) {
      attributes.put("repo_visibility", repoVisibility);
      // public_repository_url is only populated when the repository is public
      attributes.put("public_repository_url",
          REPO_VISIBILITY_PUBLIC.equals(repoVisibility) ? sourceControl.getRepositoryUrl() : null);
    }
    // repository_url is still populated for backward compatibility
    attributes.put("repository_url", HdsClientAnalytics.obfuscate(sourceControl.getRepositoryUrl()));
    attributes.put("method", method);
    attributes.put("owner_id", HdsClientAnalytics.obfuscate(sourceControl.getOwnerId()));
    attributes.put("provider", sourceControl.getProvider() != null ? sourceControl.getProvider().toString() : null);
    attributes.put("enable_pull_requests", sourceControl.getRemediationPullRequestsEnabled());
    attributes.put("enable_status_checks", sourceControl.getStatusChecksEnabled());
    attributes.put("base_branch", sourceControl.getBaseBranch());

    telemetryUtils.includeRealOwnerId(attributes, sourceControl.getOwnerId());

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL);
    telemetryData.setAttributes(attributes);
    telemetrySender.send(telemetryData);
  }

  private String getRepoVisibility(final SourceControl sourceControl, final OwnerType ownerType) {
    // only applications can have repositories
    if (!OwnerType.APPLICATION.equals(ownerType)) {
      return null;
    }

    GitRepositoryInfo gitRepositoryInfo =
        SourceControlUtils.getGitRepositoryInfoForApplicationStatic(sourceControl, sourceControl.getOwnerId());
    if (gitRepositoryInfo == null) {
      return null;
    }

    try {
      gitRepositoryInfo.token = sourceControlDataService.decryptToken(sourceControl.getToken());
      if (gitRepositoryInfo.token == null) {
        return null;
      }

      boolean isPrivateRepository = scmRepoVisibilityService.isPrivateRepository(gitRepositoryInfo);
      return isPrivateRepository ? REPO_VISIBILITY_PRIVATE : REPO_VISIBILITY_PUBLIC;
    }
    catch (Exception e) {
      log.error("Unable to determine repository visibility for owner {}.", sourceControl.getOwnerId(), e);
      return null;
    }
  }

  private SourceControl getCompositeSourceControl(OwnerType ownerType, SourceControl sourceControl) {
    if (OwnerType.APPLICATION.equals(ownerType)) {
      return sourceControlDAO.buildCompositeSourceControlInApplication(sourceControl.getOwnerId());
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
        repositoryUrl.startsWith("https:") // HTTPS URL
            || repositoryUrl.startsWith("http:") // HTTP URL
    ;
    if (!validUrl) {
      throw new BadRequestException("Unsupported repository URL format: `" + repositoryUrl + "`");
    }
  }

  /**
   * Retrieves the source control metrics for the specified application. These metrics will be displayed in the "Daily
   * Automated Pull Requests" table. Note that manual pull requests are excluded from the response.
   */
  @Authorize(permission = Permission.READ)
  public ApiPullRequestResults getSourceControlMetricsForApplication(
      @AuthzContext(Key.TYPE) @SuppressWarnings("unused") final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId)
  {
    checkLicense();

    List<EnhancedPullRequestResult> enhancedPullRequestResults =
        sourceControlPullRequestMetrics.metricsForApplication(ownerId)
            .stream()
            .filter(Predicate.not(EnhancedPullRequestResult::isManualPR))
            .toList();

    return ApiSourceControlMetricsAdapter.convertToDTO(enhancedPullRequestResults);
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
    sourceControlDataService.fillWithDecryptedToken(sourceControl);
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
            definingOwnersByToken.get(sourceControl.getToken())
                .stream()
                .map(ApiOwnerDTO::fromOwner)
                .collect(
                    Collectors.toSet()));
        dto.associatedApplications.add(ApiOwnerDTO.fromOwner(application));
      }
      else {
        dto = new ApiUserRateLimitsDTO();
        dto.user = user;
        dto.provider = sourceControl.getProvider();
        dto.definingOwners = new TreeSet<>();
        dto.definingOwners.addAll(
            definingOwnersByToken.get(sourceControl.getToken())
                .stream()
                .map(ApiOwnerDTO::fromOwner)
                .collect(
                    Collectors.toSet()));
        dto.associatedApplications = new TreeSet<>();
        dto.associatedApplications.add(ApiOwnerDTO.fromOwner(application));
        dto.rateLimits = getRateLimits(sourceControl).getRateLimitResponses()
            .stream()
            .map(ApiRateLimitDTO::convert)
            .sorted(Comparator.comparing(rateLimitDTO -> rateLimitDTO.category))
            .collect(Collectors.toList());
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
    return gitClientFactory.createApiClient(gitRepositoryInfo).getSynchronizationKey();
  }

  private void trySaveRepoUserActivityOrFailSilently(
      final String publicId,
      final Map<String, Collection<Instant>> emailAndCommitDateMap)
  {
    if (Objects.nonNull(emailAndCommitDateMap)
        && emailAndCommitDateMap.size() > EMAIL_AND_COMMIT_DATE_MAP_LIMIT)
    {
      log.warn("Email and commit date map must have " + EMAIL_AND_COMMIT_DATE_MAP_LIMIT + " or less entries");
    }
    else {
      try {
        sourceControlUserActivityService.saveRepoUserList(publicId, emailAndCommitDateMap);
      }
      catch (Exception e) {
        log.warn("Unable to save the repository user activity.", e);
      }
    }
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
