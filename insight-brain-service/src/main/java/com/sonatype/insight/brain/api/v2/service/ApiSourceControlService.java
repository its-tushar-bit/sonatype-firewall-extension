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
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.google.common.annotations.VisibleForTesting;
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
      sourceControl =
          new SourceControl.Builder().setOwnerId(application.getId()).setRepositoryUrl(repositoryUrl).build();
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
    return apiSourceControlAdapter.convertToDTO(sourceControl);
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
    sourceControlDTO.ownerId = ownerId;
    checkLicense();

    SourceControl sourceControl = apiSourceControlAdapter.convertFromDTO(sourceControlDTO);
    encryptToken(sourceControl);

    // fail if there's already a sourcecontrol in place for the owner
    if (null != sourceControlDAO.getByOwnerId(ownerId)) {
      throw new BadRequestException(String.format(
          "SourceControl already exists for %s with id: %s", ownerType, ownerId));
    }

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
    sourceControlDTO.ownerId = ownerId;
    checkLicense();

    SourceControl storedSourceControl = sourceControlDAO.getByOwnerId(sourceControlDTO.ownerId);
    if (null == storedSourceControl) {
      throw new NotFoundException(String.format(
          "Cannot find SourceControl for %s with id: %s", ownerType, ownerId));
    }

    SourceControl sourceControl = apiSourceControlAdapter.convertFromDTO(sourceControlDTO);
    sourceControl.setId(storedSourceControl.getId());

    // updates may come with our 'fake' token or simply omit it
    if (isEmpty(sourceControl.getToken())
        || FAKE_SECRET_KEY.equalsIgnoreCase(sourceControl.getToken())) {
      sourceControl.setToken(storedSourceControl.getToken());
    }
    else {
      encryptToken(sourceControl);
    }

    sourceControlDAO.update(sourceControl);
    auditSourceControl(sourceControl);
    sourceControl.setToken(FAKE_SECRET_KEY);
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
          "Cannot find SourceControl for %s with id: %s", ownerType, ownerId));
    }
    sourceControlDAO.delete(sourceControl);
    auditSourceControl(sourceControl);
    sendSourceControlTelemetryData(METHOD.DELETE, ownerId, sourceControl);
  }

  @Authorize(permission = Permission.READ)
  public SourceControl getSourceControlByOwnerDecrypted(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId)
  {
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(ownerId);
    if (sourceControl == null) {
      return null;
    }
    decryptToken(sourceControl);
    return sourceControl;
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
      attributes.put("enable_pull_requests", sourceControl.getEnablePullRequests());
      attributes.put("enable_status_checks", sourceControl.getEnableStatusChecks());
      attributes.put("base_branch", sourceControl.getBaseBranch());
    }

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL);
    telemetryData.setAttributes(attributes);
    telemetrySender.send(telemetryData);
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
