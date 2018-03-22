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
import com.sonatype.insight.brain.telemetry.TelemetryData;
import com.sonatype.insight.brain.telemetry.TelemetryHeader;
import com.sonatype.insight.brain.telemetry.TelemetryPurpose;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.inject.Binder;
import org.apache.http.HttpEntity;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.fail;
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
    super.configure(binder);
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
  }

  @Test
  public void testUpdate() {
    Organization organization = tempEntity.newOrganization();

    AutomaticApplicationsConfiguration updated = service
        .update(new AutomaticApplicationsConfiguration(true, organization.getId()));

    assertThat(updated.isEnabled(), is(true));
    assertThat(updated.getParentOrganizationId(), is(organization.getId()));

    assertThat(automaticApplicationsConfigurationDAO.isEnabled(), is(true));
    assertThat(automaticApplicationsConfigurationDAO.getOrganizationId(), is(organization.getId()));
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
    assertThat(TelemetrySender.RESOURCE_PATH, is(invocation.getArguments()[0]));
    HttpEntity httpEntity = (HttpEntity) invocation.getArguments()[1];
    ByteArrayDataSource multipartDataSource = new ByteArrayDataSource(httpEntity.getContent(), "multipart/form-data");
    MimeMultipart multipart = new MimeMultipart(multipartDataSource);
    BodyPart bodyPart = multipart.getBodyPart(0);
    String filename = bodyPart.getFileName();
    assertThat(TelemetrySender.ZIP_FILENAME, is(filename));

    try (ZipInputStream zipInputStream = new ZipInputStream(bodyPart.getInputStream())) {
      byte[] buffer = new byte[1024];

      ZipEntry zipEntryHeader = zipInputStream.getNextEntry();
      assertThat(zipEntryHeader.getName(), is(TelemetrySender.HEADER_ENTRY_NAME));
      zipInputStream.read(buffer);
      TelemetryHeader telemetryHeader = JsonUtils.parse(buffer, TelemetryHeader.class);
      assertThat(telemetryHeader.getCreateTime(), greaterThanOrEqualTo(before));
      assertThat(telemetryHeader.getCreateTime(), lessThanOrEqualTo(after));

      ZipEntry zipEntryData = zipInputStream.getNextEntry();
      assertThat(zipEntryData.getName(), is(TelemetrySender.DATA_ENTRY_NAME));
      zipInputStream.read(buffer);
      TelemetryData telemetryData = JsonUtils.parse(buffer, TelemetryData.class);
      assertThat(telemetryData.getPurpose(), is(TelemetryPurpose.AUTOMATIC_APPLICATION_CREATION));
      assertThat(
          telemetryData.getAttributes()
              .get(AutomaticApplicationsConfigurationService.AUTO_APP_CREATION_ENABLED_TELEMETRY_ATTR),
          is(String.valueOf(expected)));
      assertThat(telemetryData.getAttributes().size(), is(1));
      assertThat(telemetryData.getTimestamp(), greaterThanOrEqualTo(before.getTime()));
      assertThat(telemetryData.getTimestamp(), lessThanOrEqualTo(after.getTime()));
    }
  }

  @Test
  public void testUpdate_RootOrganizationId_Enabled() {
    try {
      service.update(new AutomaticApplicationsConfiguration(true, Organization.ROOT_ORGANIZATION_ID));
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Parent cannot be the root organization."));
    }
  }

  @Test
  public void testUpdate_RootOrganizationId_Disabled() {
    try {
      service.update(new AutomaticApplicationsConfiguration(false, Organization.ROOT_ORGANIZATION_ID));
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Parent cannot be the root organization."));
    }
  }

  @Test
  public void testUpdate_InvalidOrganizationId_Enabled() {
    try {
      service.update(new AutomaticApplicationsConfiguration(true, "testOrganizationID"));
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Parent organization ID testOrganizationID not found."));
    }
  }

  @Test
  public void testUpdate_InvalidOrganizationId_Disabled() {
    try {
      service.update(new AutomaticApplicationsConfiguration(false, "testOrganizationID"));
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Parent organization ID testOrganizationID not found."));
    }
  }

  @Test
  public void testUpdate_EmptyParentOrganizationId_Enabled() {
    try {
      service.update(new AutomaticApplicationsConfiguration(true, ""));
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          is("Parent organization ID is required when automatic application creation is enabled."));
    }
  }

  @Test
  public void testUpdate_EmptyParentOrganizationId_Disabled() {
    service.update(new AutomaticApplicationsConfiguration(false, ""));

    assertThat(automaticApplicationsConfigurationDAO.isEnabled(), is(false));
    assertThat(automaticApplicationsConfigurationDAO.getOrganizationId(), is(""));
  }

  @Test
  public void testUpdate_NullParentOrganizationId_Enabled() {
    try {
      service.update(new AutomaticApplicationsConfiguration(true, null));
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          is("Parent organization ID is required when automatic application creation is enabled."));
    }
  }

  @Test
  public void testUpdate_NullParentOrganizationId_Disabled() {
    service.update(new AutomaticApplicationsConfiguration(false, null));

    assertThat(automaticApplicationsConfigurationDAO.isEnabled(), is(false));
    assertThat(automaticApplicationsConfigurationDAO.getOrganizationId(), is(""));
  }

  @Test
  public void testGet() {
    automaticApplicationsConfigurationDAO.setEnabled(true);
    automaticApplicationsConfigurationDAO.setOrganizationId("testGetId");

    AutomaticApplicationsConfiguration configuration = service.get();

    assertThat(configuration.isEnabled(), is(true));
    assertThat(configuration.getParentOrganizationId(), is("testGetId"));
  }
}
