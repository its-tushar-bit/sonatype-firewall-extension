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

import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Named
@Singleton
public class ApiSourceControlService
{
  private static final Logger log = LoggerFactory.getLogger(ApiSourceControlService.class);

  private static final String ENC = "CMMDwoV";

  private final PlexusCipher plexusCipher;

  private final SourceControlDAO sourceControlDAO;

  private final ApplicationDAO applicationDAO;

  private final ApiSourceControlAdapter apiSourceControlAdapter;

  private final ProductLicense productLicense;

  private final TelemetrySender telemetrySender;

  @Inject
  public ApiSourceControlService(
      final PlexusCipher plexusCipher,
      final SourceControlDAO sourceControlDAO,
      final ApplicationDAO applicationDAO,
      final ApiSourceControlAdapter apiSourceControlAdapter,
      final ProductLicense productLicense,
      final TelemetrySender telemetrySender)
  {
    this.plexusCipher = plexusCipher;
    this.sourceControlDAO = sourceControlDAO;
    this.applicationDAO = applicationDAO;
    this.apiSourceControlAdapter = apiSourceControlAdapter;
    this.productLicense = productLicense;
    this.telemetrySender = telemetrySender;
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

  @Authorize(permission = Permission.READ)
  public ApiSourceControlDTO getSourceControlByApplicationId(
      @AuthzContext(Key.APPLICATION_ID) final String applicationId)
  {
    return addApplicationId(getSourceControlByOwner(
        OwnerType.APPLICATION, applicationId));
  }

  @Authorize(permission = Permission.WRITE)
  public ApiSourceControlDTO addSourceControl(
      @AuthzContext(Key.APPLICATION_ID) final String applicationId,
      ApiSourceControlDTO sourceControlDTO)
  {
    return addApplicationId(addSourceControlByOwner(
        OwnerType.APPLICATION, applicationId, sourceControlDTO));
  }

  @Authorize(permission = Permission.WRITE)
  public ApiSourceControlDTO updateSourceControl(
      @AuthzContext(Key.APPLICATION_ID) final String applicationId,
      ApiSourceControlDTO sourceControlDTO)
  {
    return addApplicationId(updateSourceControlByOwner(
        OwnerType.APPLICATION, applicationId, sourceControlDTO));
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteSourceControl(
      @AuthzContext(Key.APPLICATION_ID) final String applicationId,
      String sourceControlId)
  {
    deleteSourceControlByOwner(OwnerType.APPLICATION, applicationId,
        sourceControlId);
  }

  @Authorize(permission = Permission.WRITE)
  public ApiSourceControlDTO addOrUpdateSourceControl(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) final String publicId,
      final String repositoryUrl)
  {
    checkLicense();
    Application application = applicationDAO.getByPublicId(publicId);
    if (application == null) {
      throw new NotFoundException("Cannot find application with public ID: '" + publicId + "'");
    }
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(application.getId());
    if (sourceControl == null) { // create new record
      sourceControl = new SourceControl();
      sourceControl.setOwnerId(application.getId());
      sourceControl.setRepositoryUrl(repositoryUrl);
      sourceControlDAO.insert(sourceControl);
    }
    else { // update existing record
      sourceControl.setRepositoryUrl(repositoryUrl);
      sourceControlDAO.update(sourceControl);
    }
    if (sourceControl.getToken() != null) {
      sourceControl.setToken(FAKE_SECRET_KEY);
    }
    auditSourceControl(sourceControl);
    sendSourceControlTelemetryData(METHOD.ADD_OR_UPDATE, application.getId());
    return addApplicationId(apiSourceControlAdapter.convertToDTO(sourceControl));
  }

  public ApiSourceControlDTO getSourceControlByApplicationIdDecrypted(
      final String applicationId)
  {
    try {
      ApiSourceControlDTO sourceControl = getSourceControlByApplicationId(
          applicationId);
      return getSourceControlDecrypted(applicationId, sourceControl.id);
    }
    catch (NotFoundException e) {
      return null;
    }
  }

  @Authorize(permission = Permission.READ)
  ApiSourceControlDTO getSourceControlDecrypted(
      @AuthzContext(Key.APPLICATION_ID) final String applicationId,
      String sourceControlId)
  {
    SourceControl sourceControl = sourceControlDAO.getByIdNotNull(sourceControlId);
    validateOwnerId(OwnerType.APPLICATION, applicationId, sourceControl);
    decryptToken(sourceControl);
    return addApplicationId(apiSourceControlAdapter.convertToDTO(sourceControl));
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
          "Cannot find SourceControl for %s with id: %s", ownerType, ownerId));
    }
    sourceControl.setToken(FAKE_SECRET_KEY);
    sendSourceControlTelemetryData(METHOD.GET_BY_OWNER_ID, ownerId);

    return apiSourceControlAdapter.convertToDTO(sourceControl);
  }

  @Authorize(permission = Permission.WRITE)
  public ApiSourceControlDTO addSourceControlByOwner(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      ApiSourceControlDTO sourceControlDTO)
  {
    checkLicense();
    SourceControl sourceControl = apiSourceControlAdapter.convertFromDTO(
        sourceControlDTO);
    encryptToken(sourceControl);
    sourceControl.setOwnerId(ownerId);
    sourceControlDAO.insert(sourceControl);
    auditSourceControl(sourceControl);
    sourceControl.setToken(FAKE_SECRET_KEY);
    sendSourceControlTelemetryData(METHOD.ADD, ownerId, sourceControl);
    return apiSourceControlAdapter.convertToDTO(sourceControl);
  }

  @Authorize(permission = Permission.WRITE)
  public ApiSourceControlDTO updateSourceControlByOwner(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      ApiSourceControlDTO sourceControlDTO)
  {
    checkLicense();

    SourceControl sourceControl = apiSourceControlAdapter.convertFromDTO(
        sourceControlDTO);

    // updates may come with our 'fake' token or simply omit it
    if (isEmpty(sourceControl.getToken())
        || FAKE_SECRET_KEY.equalsIgnoreCase(sourceControl.getToken())) {
      SourceControl storedSourceControl = sourceControlDAO.getByIdNotNull(
          sourceControl.getId());
      sourceControl.setToken(storedSourceControl.getToken());
    }
    else {
      encryptToken(sourceControl);
    }
    validateOwnerId(ownerType, ownerId, sourceControl);
    sourceControlDAO.update(sourceControl);
    auditSourceControl(sourceControl);
    sourceControl.setToken(FAKE_SECRET_KEY);
    sendSourceControlTelemetryData(METHOD.UPDATE, ownerId, sourceControl);
    return apiSourceControlAdapter.convertToDTO(sourceControl);
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteSourceControlByOwner(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      String sourceControlId)
  {
    checkLicense();
    SourceControl sourceControl = sourceControlDAO.getByIdNotNull(sourceControlId);
    validateOwnerId(ownerType, ownerId, sourceControl);
    sourceControlDAO.delete(sourceControl);
    auditSourceControl(sourceControl);
    sendSourceControlTelemetryData(METHOD.DELETE, ownerId, sourceControl);
  }

  public ApiSourceControlDTO populateProviderAndTokenFromOrganizationIfNeeded(ApiSourceControlDTO sourceControl) {
    if (sourceControl.provider == null || Strings.isNullOrEmpty(sourceControl.token)) {
      Application application = applicationDAO.getById(sourceControl.ownerId);

      if (application == null) {
        return sourceControl;
      }

      try {
        ApiSourceControlDTO orgSourceControl =
            getSourceControlByOwner(OwnerType.ORGANIZATION, application.getOrganizationId());
        orgSourceControl =
            getSourceControlByOwnerDecrypted(OwnerType.ORGANIZATION, application.getOrganizationId(),
                orgSourceControl.id);
        sourceControl.token = orgSourceControl.token;
        sourceControl.provider = orgSourceControl.provider;
      }
      catch (NotFoundException e) {
        log.error(e.getMessage());
        return sourceControl;
      }
    }
    return sourceControl;
  }

  @VisibleForTesting
  @Authorize(permission = Permission.READ)
  ApiSourceControlDTO getSourceControlByOwnerDecrypted(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      String sourceControlId)
  {
    SourceControl sourceControl = sourceControlDAO.getByIdNotNull(sourceControlId);
    validateOwnerId(ownerType, ownerId, sourceControl);
    decryptToken(sourceControl);
    return apiSourceControlAdapter.convertToDTO(sourceControl);
  }

  private void validateOwnerId(
      final OwnerType ownerType,
      final String ownerId,
      final SourceControl sourceControl)
  {
    if (! sourceControl.getOwnerId().equals(ownerId)) {
      throw new NotFoundException(String.format(
          "Cannot find SourceControl with id: %s for %s with id: %s",
          sourceControl.getId(), ownerType, ownerId));
    }
  }

  private void encryptToken(final SourceControl sourceControl) {
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
    if (!productLicense.hasFeature(LicensedFeature.NOTIFICATIONS)) {
      log.debug("License does not support SourceControl notification features");
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
    }

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL);
    telemetryData.setAttributes(attributes);
    telemetrySender.send(telemetryData);
  }

  private static ApiSourceControlDTO addApplicationId(ApiSourceControlDTO dto) {
    dto.applicationId = dto.ownerId;
    return dto;
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
