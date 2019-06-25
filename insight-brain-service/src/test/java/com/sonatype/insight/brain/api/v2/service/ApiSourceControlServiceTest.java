/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.insight.brain.TestLicenseManager;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService.METHOD;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.features.Feature;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.sonatype.plexus.components.cipher.PlexusCipher;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

public class ApiSourceControlServiceTest
    extends AbstractComponentTest
{
  private static final String VALID_URL = "https://example.com/organization/project";

  private static final String TOKEN = "token";

  @Inject
  private ApiSourceControlService sourceControlService;

  @Inject
  private PlexusCipher plexusCipher;

  @Inject
  private TestLicenseManager testLicenseManager;

  @Mock
  private TelemetrySender telemetrySenderMock;

  private SourceControlDAO sourceControlDAO = new SourceControlDAO();

  private Application app;

  @Override
  public void configure(Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    super.configure(binder);
  }

  @Before
  public void setup() {
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testAddSourceControl_TokenEncryption() throws Exception {
    SourceControl validSourceControl = new SourceControl(app.getId(), VALID_URL, TOKEN);
    SourceControl sourceControl = sourceControlService.addSourceControl(app.getId(), validSourceControl);
    assertThat(sourceControl.getToken()).isNotEqualTo(TOKEN);
    assertThat(sourceControl.getToken()).isEqualTo(SourceControl.FAKE_SECRET_KEY);

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());

    String decrypted;
    synchronized (plexusCipher) {
      decrypted = plexusCipher.decrypt(sourceControl.getToken(), "CMMDwoV");
    }
    assertThat(decrypted).isEqualTo(TOKEN);
    assertTelemetry(METHOD.ADD, app.getId(), sourceControl.getRepositoryUrl());
  }

  @Test
  public void testUpdateSourceControl_TokenEncryption() throws Exception {
    SourceControl validSourceControl = new SourceControl(app.getId(), VALID_URL, TOKEN);

    SourceControl sourceControl = sourceControlService.addSourceControl(app.getId(), validSourceControl);
    sourceControl.setToken("updatedToken");
    assertTelemetry(METHOD.ADD, app.getId(), sourceControl.getRepositoryUrl());

    SourceControl updatedScm = sourceControlService.updateSourceControl(app.getId(), sourceControl);
    assertThat(updatedScm.getToken()).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertTelemetry(METHOD.UPDATE, app.getId(), sourceControl.getRepositoryUrl());

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());

    String decrypted;
    synchronized (plexusCipher) {
      decrypted = plexusCipher.decrypt(sourceControl.getToken(), "CMMDwoV");
    }
    assertThat(decrypted).isEqualTo("updatedToken");
  }

  @Test
  public void testGetSourceControlDecrypted() {
    SourceControl validSourceControl = new SourceControl(app.getId(), VALID_URL, TOKEN);
    SourceControl sourceControl = sourceControlService.addSourceControl(app.getId(), validSourceControl);
    assertThat(sourceControlService.getSourceControlDecrypted(app.getId(), sourceControl.getId()).getToken())
        .isEqualTo(TOKEN);
  }

  @Test
  public void testDeleteSourceControl() {
    SourceControl validSourceControl = new SourceControl(app.getId(), VALID_URL, TOKEN);

    SourceControl sourceControl = sourceControlService.addSourceControl(app.getId(), validSourceControl);
    assertThat(sourceControlService.getAll().size()).isEqualTo(1);
    assertTelemetry(METHOD.ADD, app.getId(), sourceControl.getRepositoryUrl());

    sourceControlService.deleteSourceControl(app.getId(), sourceControl.getId());
    assertThat(sourceControlService.getAll().isEmpty()).isTrue();
    assertTelemetry(METHOD.DELETE, app.getId(), sourceControl.getRepositoryUrl());
  }

  @Test
  public void testUpdateSourceControl_WithFakeToken() {
    SourceControl sourceControl = sourceControlService
        .addSourceControl(app.getId(), new SourceControl(app.getId(), VALID_URL, TOKEN));
    sourceControl.setToken(SourceControl.FAKE_SECRET_KEY);
    sourceControlService.updateSourceControl(app.getId(), sourceControl);
    assertThat(sourceControlService.getSourceControlDecrypted(app.getId(), sourceControl.getId()).getToken())
        .isEqualTo(TOKEN);
  }

  @Test
  public void testUpdateSourceControl_WithEmptyToken() {
    SourceControl sourceControl = sourceControlService
        .addSourceControl(app.getId(), new SourceControl(app.getId(), VALID_URL, TOKEN));
    sourceControl.setToken(null);
    sourceControlService.updateSourceControl(app.getId(), sourceControl);
    assertThat(sourceControlService.getSourceControlDecrypted(app.getId(), sourceControl.getId()).getToken())
        .isEqualTo(TOKEN);
  }

  @Test
  public void testUpdateSourceControl_WrongAppId() {
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), VALID_URL, "token");
    sourceControl.setApplicationId("foo");
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        sourceControlService.updateSourceControl(app.getId(), sourceControl)).withMessage(
        "Cannot find SourceControl with id: " + sourceControl.getId() + " for Application with id: " + app.getId());
  }

  @Test
  public void testAddSourceControl_unlicensed() {
    testLicenseManager.setMissingFeatures(Feature.NOTIFICATIONS);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> sourceControlService
            .addSourceControl("foo", new SourceControl(testName.getMethodName(), "bar", "baz")));
  }

  @Test
  public void testUpdateSourceControl_unlicensed() {
    testLicenseManager.setMissingFeatures(Feature.NOTIFICATIONS);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> sourceControlService
            .updateSourceControl("foo", new SourceControl(testName.getMethodName(), "bar", "baz")));
  }

  @Test
  public void testDeleteSourceControl_unlicensed() {
    testLicenseManager.setMissingFeatures(Feature.NOTIFICATIONS);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> sourceControlService.deleteSourceControl("foo", "bar"));
  }

  @Test
  public void testGetAll_unlicensed() {
    testLicenseManager.setMissingFeatures(Feature.NOTIFICATIONS);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> sourceControlService.getAll());
  }

  @Test
  public void testGetSourceControlByApplicationId() {
    tempEntity.newSourceControl(app.getId(), VALID_URL, "token");
    SourceControl sourceControlByApplicationId = sourceControlService.getSourceControlByApplicationId(app.getId());
    assertThat(sourceControlByApplicationId.getId()).isEqualTo(sourceControlByApplicationId.getId());
    assertThat(sourceControlByApplicationId.getRepositoryUrl())
        .isEqualTo(sourceControlByApplicationId.getRepositoryUrl());
    assertThat(sourceControlByApplicationId.getToken()).isEqualTo(SourceControl.FAKE_SECRET_KEY);
  }

  @Test
  public void testGetSourceControlByApplicationId_AppDoesNotExist() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        sourceControlService.getSourceControlByApplicationId(app.getId()));
  }

  @Test
  public void testGetSourceControlByApplicationIdDecrypted_AppDoesNotExist() {
    SourceControl sourceControl = sourceControlService.getSourceControlByApplicationIdDecrypted(app.getId());
    assertThat(sourceControl).isNull();
  }

  private void assertTelemetry(final METHOD method,
                               final String applicationId,
                               final String repositoryUrl)
  {
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("method", method);
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate(applicationId));
    expectedAttributes.put("repository_url", HdsClientAnalytics.obfuscate(repositoryUrl));
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
    reset(telemetrySenderMock);
  }
}
