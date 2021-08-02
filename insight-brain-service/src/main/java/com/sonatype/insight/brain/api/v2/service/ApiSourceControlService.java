/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiPullRequestResults;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

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
import static com.sonatype.nexus.git.utils.repository.RepositoryUrlFinderUtils.sanitizeUrl;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;

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

  private final ApiSourceControlAdapter apiSourceControlAdapter;

  private final ApiSourceControlMetricsAdapter apiSourceControlMetricsAdapter;

  private final ProductLicense productLicense;

  private final TelemetrySender telemetrySender;

  private final SourceControlPullRequestMetrics sourceControlPullRequestMetrics;

  private final SourceControlEventDAO sourceControlEventDAO;

  @Inject
  public ApiSourceControlService(
      final PlexusCipher plexusCipher,
      final SourceControlDAO sourceControlDAO,
      final OwnerDAO ownerDAO,
      final ApplicationDAO applicationDAO,
      final AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO,
      final ApiSourceControlAdapter apiSourceControlAdapter,
      final ApiSourceControlMetricsAdapter apiSourceControlMetricsAdapter,
      final ProductLicense productLicense,
      final TelemetrySender telemetrySender,
      final SourceControlPullRequestMetrics sourceControlPullRequestMetrics,
      final SourceControlEventDAO sourceControlEventDAO)
  {
    this.plexusCipher = plexusCipher;
    this.sourceControlDAO = sourceControlDAO;
    this.ownerDAO = ownerDAO;
    this.applicationDAO = applicationDAO;
    this.automaticSourceControlConfigurationDAO = automaticSourceControlConfigurationDAO;
    this.apiSourceControlAdapter = apiSourceControlAdapter;
    this.apiSourceControlMetricsAdapter = apiSourceControlMetricsAdapter;
    this.productLicense = productLicense;
    this.telemetrySender = telemetrySender;
    this.sourceControlPullRequestMetrics = sourceControlPullRequestMetrics;
    this.sourceControlEventDAO = sourceControlEventDAO;
  }

  @Authorize(permission = Permission.READ)
  public List<ApiSourceControlDTO> getAll() {
    checkLicense();
    List<SourceControl> sourceControlDAOAll = sourceControlDAO.getAll();
    sourceControlDAOAll.forEach(this::encryptToken);
    return sourceControlDAOAll.stream()
        .map(apiSourceControlAdapter::convertToDTO)
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
      final String defaultBranch)
  {
    return addOrUpdateSourceControl(publicId, repositoryUrl, true, defaultBranch);
  }

  private ApiSourceControlDTO addOrUpdateSourceControl(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) final String publicId,
      final String repositoryUrl,
      final boolean bypassAutomatedSCM)
  {
    return addOrUpdateSourceControl(publicId, repositoryUrl, bypassAutomatedSCM, null);
  }

  private ApiSourceControlDTO addOrUpdateSourceControl(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) final String publicId,
      final String repositoryUrl,
      final boolean bypassAutomatedSCM,
      final String defaultBranch)
  {
    checkLicense();

    Application application = applicationDAO.getByPublicId(publicId);
    if (application == null) {
      throw new NotFoundException("Cannot find application with public ID: '" + publicId + "'");
    }
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(application.getId());
    String convertedRepositoryUrl = convertUrlIfNeeded(repositoryUrl);

    // check if automatic source control is enabled or bypassed
    if (bypassAutomatedSCM || automaticSourceControlConfigurationDAO.isSourceControlConfigurationEnabled()) {
      if (sourceControl == null) { // create new record
        sourceControl = new SourceControl.Builder()
            .setOwnerId(application.getId())
            .setRepositoryUrl(convertedRepositoryUrl)
            .setBaseBranch(defaultBranch)
            .build();
        sourceControlDAO.insert(sourceControl);
        auditAndSendTelemetry(sourceControl, application.getId());
      }
      else if (shouldUpdateSourceControlRepositoryUrl(sourceControl.getRepositoryUrl(), convertedRepositoryUrl)) {
        sourceControl.setRepositoryUrl(convertedRepositoryUrl);
        sourceControlDAO.update(sourceControl);
        auditAndSendTelemetry(sourceControl, application.getId());
      }
      else {
        log.debug("Skipping update of source control repository URL from {} to {}",
            sourceControl.getRepositoryUrl(), convertedRepositoryUrl);
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
    sendSourceControlTelemetryData(METHOD.GET_BY_OWNER_ID, ownerId);

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
    convertRepositoryUrlIfNeeded(sourceControl);

    sourceControlDAO.insert(sourceControl);
    auditSourceControl(sourceControl);
    setTokenValueForReturn(sourceControl);
    sendSourceControlTelemetryData(METHOD.ADD, ownerId, sourceControl);
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

    setTokenValueForSave(sourceControl);
    // updates may come with our 'fake' token
    if (FAKE_SECRET_KEY.equalsIgnoreCase(sourceControl.getToken())) {
      sourceControl.setToken(storedSourceControl.getToken());
    }
    else {
      encryptToken(sourceControl);
    }
    convertRepositoryUrlIfNeeded(sourceControl);

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
    sendSourceControlTelemetryData(METHOD.UPDATE, ownerId, sourceControl);
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
    sourceControlDAO.delete(sourceControl);
    auditSourceControl(sourceControl);
    sendSourceControlTelemetryData(METHOD.DELETE, ownerId, sourceControl);
  }

  public SourceControl getSourceControlByOwnerDecrypted(final String ownerId) {
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(ownerId);
    if (sourceControl == null) {
      return null;
    }
    decryptToken(sourceControl);
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
        sourceControl.setToken(plexusCipher.decrypt(sourceControl.getToken(), ENC));
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
    if (!(productLicense.hasFeature(LicensedFeature.NOTIFICATIONS)
        || productLicense.hasFeature(LicensedFeature.AUTOMATION))) {
      log.debug("License does not support SourceControl notification or automation features");
      throw new InvalidLicenseException();
    }
  }

  private void sendSourceControlTelemetryData(final METHOD method, final String ownerId) {
    sendSourceControlTelemetryData(method, ownerId, null);
  }

  private void sendSourceControlTelemetryData(
      final METHOD method,
      final String ownerId,
      final SourceControl sourceControl)
  {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("method", method);
    attributes.put("owner_id", HdsClientAnalytics.obfuscate(ownerId));
    if (sourceControl != null) {
      attributes.put("repository_url", HdsClientAnalytics.obfuscate(sourceControl.getRepositoryUrl()));
      attributes.put("provider", (sourceControl.getProvider() != null)
          ? sourceControl.getProvider().toString() : null);
      attributes.put("enable_pull_requests", sourceControl.getEnablePullRequests());
      attributes.put("enable_status_checks", sourceControl.getEnableStatusChecks());
      attributes.put("base_branch", sourceControl.getBaseBranch());
    }

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL);
    telemetryData.setAttributes(attributes);
    telemetrySender.send(telemetryData);
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

  private void auditAndSendTelemetry(SourceControl sourceControl, String appId) {
    auditSourceControl(setTokenValueForReturn(sourceControl));
    sendSourceControlTelemetryData(METHOD.ADD_OR_UPDATE, appId);
  }

  private boolean shouldUpdateSourceControlRepositoryUrl(String currentValue, String newValue) {
    return !equalsIgnoreCase(trim(currentValue), trim(newValue)) && isBlank(currentValue);
  }

  @VisibleForTesting
  String convertUrlIfNeeded(String repositoryUrl) {
    if (repositoryUrl.startsWith("https:") || repositoryUrl.startsWith("http:")) {
      return sanitizeUrl(repositoryUrl);
    }
    if (repositoryUrl.startsWith("ssh:")) {
      String url = repositoryUrl.replaceAll("/[^/@]+@", "/");
      return sanitizeUrl(url.replace("ssh:", "https:"));
    }
    if (repositoryUrl.contains("@") && repositoryUrl.contains(":")) {
      String url = repositoryUrl.replaceAll("[^@]+@", "");
      return sanitizeUrl("https://" + url.replace(":", "/"));
    }
    throw new BadRequestException("Unsupported repository URL format: `" + repositoryUrl + "`");
  }

  private void convertRepositoryUrlIfNeeded(SourceControl sourceControl) {
    if (isNotBlank(sourceControl.getRepositoryUrl())) {
      sourceControl.setRepositoryUrl(convertUrlIfNeeded(sourceControl.getRepositoryUrl()));
    }
  }

  @Authorize(permission = Permission.READ)
  public ApiPullRequestResults getSourceControlMetricsForApplication(
      @AuthzContext(Key.TYPE) @SuppressWarnings("unused") final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId)
  {
    checkLicense();

    return apiSourceControlMetricsAdapter.convertToDTO(sourceControlPullRequestMetrics.metricsForApplication(ownerId));
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
