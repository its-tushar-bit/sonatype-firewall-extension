/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlProvider;
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

  private final ProductLicense productLicense;

  private final TelemetrySender telemetrySender;

  @Inject
  public ApiSourceControlService(final PlexusCipher plexusCipher,
                                 final SourceControlDAO sourceControlDAO,
                                 final ProductLicense productLicense,
                                 final TelemetrySender telemetrySender)
  {
    this.plexusCipher = plexusCipher;
    this.sourceControlDAO = sourceControlDAO;
    this.productLicense = productLicense;
    this.telemetrySender = telemetrySender;
  }

  @Authorize(permission = Permission.READ)
  public List<SourceControl> getAll() {
    checkLicense();
    List<SourceControl> sourceControlDAOAll = sourceControlDAO.getAll();
    sourceControlDAOAll.forEach(this::encryptToken);
    return sourceControlDAOAll;
  }

  @Authorize(permission = Permission.READ)
  public SourceControl getSourceControlByApplicationId(@AuthzContext(Key.APPLICATION_ID) final String applicationId) {
    checkLicense();
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(applicationId);
    if (null == sourceControl) {
      throw new NotFoundException("Cannot find SourceControl for Application with id: " + applicationId);
    }
    sourceControl.setToken(FAKE_SECRET_KEY);
    sendSourceControlTelemetryData(METHOD.GET_BY_APP_ID, applicationId);
    return sourceControl;
  }

  @Authorize(permission = Permission.WRITE)
  public SourceControl addSourceControl(@AuthzContext(Key.APPLICATION_ID) final String applicationId,
      SourceControl sourceControl)
  {
    checkLicense();
    encryptToken(sourceControl);
    sourceControl.setOwnerId(applicationId);
    addDefaultProviderIfNotSpecified(sourceControl);
    sourceControlDAO.insert(sourceControl);
    auditSourceControl(sourceControl);
    sourceControl.setToken(FAKE_SECRET_KEY);
    sendSourceControlTelemetryData(METHOD.ADD, applicationId, sourceControl);
    return sourceControl;
  }

  @Authorize(permission = Permission.WRITE)
  public SourceControl updateSourceControl(@AuthzContext(Key.APPLICATION_ID) final String applicationId,
      SourceControl sourceControl)
  {
    checkLicense();
    // updates may come with our 'fake' token or simply omit it
    if (isEmpty(sourceControl.getToken()) || FAKE_SECRET_KEY.equalsIgnoreCase(sourceControl.getToken())) {
      SourceControl storedSourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
      sourceControl.setToken(storedSourceControl.getToken());
    }
    else {
      encryptToken(sourceControl);
    }
    validateApplicationId(applicationId, sourceControl);
    addDefaultProviderIfNotSpecified(sourceControl);
    sourceControlDAO.update(sourceControl);
    auditSourceControl(sourceControl);
    sourceControl.setToken(FAKE_SECRET_KEY);
    sendSourceControlTelemetryData(METHOD.UPDATE, applicationId, sourceControl);
    return sourceControl;
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteSourceControl(@AuthzContext(Key.APPLICATION_ID) final String applicationId,
      String sourceControlId)
  {
    checkLicense();
    SourceControl sourceControl = sourceControlDAO.getByIdNotNull(sourceControlId);
    validateApplicationId(applicationId, sourceControl);
    sourceControlDAO.delete(sourceControl);
    auditSourceControl(sourceControl);
    sendSourceControlTelemetryData(METHOD.DELETE, applicationId, sourceControl);
  }

  public SourceControl getSourceControlByApplicationIdDecrypted(final String applicationId) {
    SourceControl sourceControl;
    try {
      sourceControl = getSourceControlByApplicationId(applicationId);
    }
    catch (NotFoundException e) {
      return null;
    }
    return getSourceControlDecrypted(sourceControl.getOwnerId(), sourceControl.getId());
  }

  @Authorize(permission = Permission.READ)
  SourceControl getSourceControlDecrypted(@AuthzContext(Key.APPLICATION_ID) final String applicationId,
      String sourceControlId)
  {
    SourceControl sourceControl = sourceControlDAO.getByIdNotNull(sourceControlId);
    validateApplicationId(applicationId, sourceControl);
    decryptToken(sourceControl);
    return sourceControl;
  }

  private void validateApplicationId(final String applicationId, final SourceControl sourceControl) {
    if (! sourceControl.getOwnerId().equals(applicationId)) {
      throw new NotFoundException(
          "Cannot find SourceControl with id: " + sourceControl.getId() + " for Application with id: " + applicationId);
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
        .setData("provider", sourceControl.getProvider())
        .setData("ownerId", sourceControl.getOwnerId());
  }

  private void checkLicense() {
    if (!productLicense.hasFeature(LicensedFeature.NOTIFICATIONS)) {
      log.debug("License does not support SourceControl notification features");
      throw new InvalidLicenseException();
    }
  }

  private void sendSourceControlTelemetryData(final METHOD method, final String applicationId) {
    sendSourceControlTelemetryData(method, applicationId, null);
  }

  private void sendSourceControlTelemetryData(
      final METHOD method,
      final String applicationId,
      final SourceControl sourceControl)
  {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("method", method);
    attributes.put("application_id", HdsClientAnalytics.obfuscate(applicationId));
    if (sourceControl != null) {
      attributes.put("repository_url", HdsClientAnalytics.obfuscate(sourceControl.getRepositoryUrl()));
      attributes.put("provider", sourceControl.getProvider().toString());
    }

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL);
    telemetryData.setAttributes(attributes);
    telemetrySender.send(telemetryData);
  }

  private void addDefaultProviderIfNotSpecified(SourceControl sourceControl) {
    if (sourceControl.getProvider() == null) {
      sourceControl.setProvider(SourceControlProvider.GITHUB);
    }
  }

  enum METHOD
  {
    GET_BY_APP_ID,
    ADD,
    UPDATE,
    DELETE
  }
}
          
