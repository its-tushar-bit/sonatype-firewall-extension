/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.mail.BodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryHeader;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.apache.http.HttpEntity;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AutomaticApplicationsConfigurationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private AutomaticApplicationsConfigurationService service;

  @Inject
  private AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO;

  private HdsClient mockHdsClient = mock(HdsClient.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    super.configure(binder);
  }

  @Test
  public void testUpdate() {
    Organization organization = tempEntity.newOrganization();

    AutomaticApplicationsConfiguration updated = service
        .update(new AutomaticApplicationsConfiguration(true, organization.getId()));

    assertThat(updated.isEnabled()).isTrue();
    assertThat(updated.getParentOrganizationId()).isEqualTo(organization.getId());

    assertThat(automaticApplicationsConfigurationDAO.isEnabled()).isTrue();
    assertThat(automaticApplicationsConfigurationDAO.getOrganizationId()).isEqualTo(organization.getId());
  }

  @Test
  public void testUpdate_TelemetryEventsAreSent() throws Exception {
    final InvocationOnMock[] invocation = new InvocationOnMock[1];
    doAnswer(x -> invocation[0] = x).when(mockHdsClient).post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class),
        eq(null));
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();

    // Enable Auto App Creation - a telemetry event should be sent
    Date before = new Date();
    service.update(new AutomaticApplicationsConfiguration(true, org1.getId()));
    Date after = new Date();
    assertTelemetryEvent(invocation[0], before, after, true);
    clearInvocations(mockHdsClient);

    // No changes to Auto App Creation enablement - a telemetry event should NOT be sent
    service.update(new AutomaticApplicationsConfiguration(true, org2.getId()));
    verifyNoMoreInteractions(mockHdsClient);

    // Disable Auto App Creation - a telemetry event should be sent
    before = new Date();
    service.update(new AutomaticApplicationsConfiguration(false, org2.getId()));
    after = new Date();
    assertTelemetryEvent(invocation[0], before, after, false);
    clearInvocations(mockHdsClient);

    // No changes to Auto App Creation enablement - a telemetry event should NOT be sent
    service.update(new AutomaticApplicationsConfiguration(false, org1.getId()));
    verifyNoMoreInteractions(mockHdsClient);
  }

  private void assertTelemetryEvent(InvocationOnMock invocation, Date before, Date after, boolean expected)
      throws Exception
  {
    assertThat(TelemetrySender.RESOURCE_PATH).isEqualTo(invocation.getArguments()[0]);
    HttpEntity httpEntity = (HttpEntity) invocation.getArguments()[1];
    ByteArrayDataSource multipartDataSource = new ByteArrayDataSource(httpEntity.getContent(), "multipart/form-data");
    MimeMultipart multipart = new MimeMultipart(multipartDataSource);
    BodyPart bodyPart = multipart.getBodyPart(0);
    String filename = bodyPart.getFileName();
    assertThat(TelemetrySender.ZIP_FILENAME).isEqualTo(filename);

    try (ZipInputStream zipInputStream = new ZipInputStream(bodyPart.getInputStream())) {
      byte[] buffer = new byte[1024];

      ZipEntry zipEntryHeader = zipInputStream.getNextEntry();
      assertThat(zipEntryHeader.getName()).isEqualTo(TelemetrySender.HEADER_ENTRY_NAME);
      zipInputStream.read(buffer);
      TelemetryHeader telemetryHeader = JsonUtils.parse(buffer, TelemetryHeader.class);
      assertThat(telemetryHeader.getCreateTime()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);

      ZipEntry zipEntryData = zipInputStream.getNextEntry();
      assertThat(zipEntryData.getName()).isEqualTo(TelemetrySender.DATA_ENTRY_NAME);
      zipInputStream.read(buffer);
      TelemetryData telemetryData = JsonUtils.parse(buffer, TelemetryData.class);
      assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.AUTOMATIC_APPLICATION_CREATION);
      assertThat(telemetryData.getAttributes()).hasSize(1).containsEntry(
          AutomaticApplicationsConfigurationService.AUTO_APP_CREATION_ENABLED_TELEMETRY_ATTR, String.valueOf(expected));
      assertThat(telemetryData.getTimestamp()).isGreaterThanOrEqualTo(before.getTime())
          .isLessThanOrEqualTo(after.getTime());
    }
  }

  @Test
  public void testUpdate_RootOrganizationId_Enabled() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.update(new AutomaticApplicationsConfiguration(true, Organization.ROOT_ORGANIZATION_ID));
    }).withMessage("Parent cannot be the root organization.");
  }

  @Test
  public void testUpdate_RootOrganizationId_Disabled() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.update(new AutomaticApplicationsConfiguration(false, Organization.ROOT_ORGANIZATION_ID));
    }).withMessage("Parent cannot be the root organization.");
  }

  @Test
  public void testUpdate_InvalidOrganizationId_Enabled() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.update(new AutomaticApplicationsConfiguration(true, "testOrganizationID"));
    }).withMessage("Parent organization ID testOrganizationID not found.");
  }

  @Test
  public void testUpdate_InvalidOrganizationId_Disabled() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.update(new AutomaticApplicationsConfiguration(false, "testOrganizationID"));
    }).withMessage("Parent organization ID testOrganizationID not found.");
  }

  @Test
  public void testUpdate_EmptyParentOrganizationId_Enabled() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.update(new AutomaticApplicationsConfiguration(true, ""));
    }).withMessage("Parent organization ID is required when automatic application creation is enabled.");
  }

  @Test
  public void testUpdate_EmptyParentOrganizationId_Disabled() {
    service.update(new AutomaticApplicationsConfiguration(false, ""));

    assertThat(automaticApplicationsConfigurationDAO.isEnabled()).isFalse();
    assertThat(automaticApplicationsConfigurationDAO.getOrganizationId()).isEqualTo("");
  }

  @Test
  public void testUpdate_NullParentOrganizationId_Enabled() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.update(new AutomaticApplicationsConfiguration(true, null));
    }).withMessage("Parent organization ID is required when automatic application creation is enabled.");
  }

  @Test
  public void testUpdate_NullParentOrganizationId_Disabled() {
    service.update(new AutomaticApplicationsConfiguration(false, null));

    assertThat(automaticApplicationsConfigurationDAO.isEnabled()).isFalse();
    assertThat(automaticApplicationsConfigurationDAO.getOrganizationId()).isEqualTo("");
  }

  @Test
  public void testGet() {
    automaticApplicationsConfigurationDAO.setEnabled(true);
    automaticApplicationsConfigurationDAO.setOrganizationId("testGetId");

    AutomaticApplicationsConfiguration configuration = service.get();

    assertThat(configuration.isEnabled()).isTrue();
    assertThat(configuration.getParentOrganizationId()).isEqualTo("testGetId");
  }
}
