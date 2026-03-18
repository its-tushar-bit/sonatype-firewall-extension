/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import java.util.Date;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AutomaticApplicationsConfigurationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private AutomaticApplicationsConfigurationService service;

  @Inject
  private AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
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
  public void testUpdate_TelemetryEventsAreSent() {
    final InvocationOnMock[] invocation = new InvocationOnMock[1];
    doAnswer(x -> invocation[0] = x).when(telemetrySenderMock).send(any(TelemetryData.class));
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();

    // Enable Auto App Creation - a telemetry event should be sent
    Date before = new Date();
    service.update(new AutomaticApplicationsConfiguration(true, org1.getId()));
    Date after = new Date();
    assertTelemetryEvent(invocation[0], TelemetryPurpose.AUTOMATIC_APPLICATION_CREATION,
        AutomaticApplicationsConfigurationService.AUTO_APP_CREATION_ENABLED_TELEMETRY_ATTR,
        before, after, true);
    clearInvocations(telemetrySenderMock);

    // No changes to Auto App Creation enablement - a telemetry event should NOT be sent
    service.update(new AutomaticApplicationsConfiguration(true, org2.getId()));
    verifyNoMoreInteractions(telemetrySenderMock);

    // Disable Auto App Creation - a telemetry event should be sent
    before = new Date();
    service.update(new AutomaticApplicationsConfiguration(false, org2.getId()));
    after = new Date();
    assertTelemetryEvent(invocation[0], TelemetryPurpose.AUTOMATIC_APPLICATION_CREATION,
        AutomaticApplicationsConfigurationService.AUTO_APP_CREATION_ENABLED_TELEMETRY_ATTR,
        before, after, false);
    clearInvocations(telemetrySenderMock);

    // No changes to Auto App Creation enablement - a telemetry event should NOT be sent
    service.update(new AutomaticApplicationsConfiguration(false, org1.getId()));
    verifyNoMoreInteractions(telemetrySenderMock);
  }

  private void assertTelemetryEvent(
      InvocationOnMock invocation,
      TelemetryPurpose telemetryPurpose,
      String telemetryAttr,
      Date before,
      Date after,
      boolean expected)
  {
    TelemetryData telemetryData = (TelemetryData) invocation.getArgument(0);
    assertThat(telemetryData.getPurpose()).isEqualTo(telemetryPurpose);
    assertThat(telemetryData.getAttributes()).hasSize(1).containsEntry(telemetryAttr, String.valueOf(expected));
    assertThat(telemetryData.getTimestamp()).isGreaterThanOrEqualTo(before.getTime())
        .isLessThanOrEqualTo(after.getTime());
  }

  @Test
  public void testUpdate_RootOrganizationId_Enabled() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.update(new AutomaticApplicationsConfiguration(true, Organization.ROOT_ORGANIZATION_ID)))
        .withMessage("Parent cannot be the root organization.");
  }

  @Test
  public void testUpdate_RootOrganizationId_Disabled() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.update(new AutomaticApplicationsConfiguration(false, Organization.ROOT_ORGANIZATION_ID)))
        .withMessage("Parent cannot be the root organization.");
  }

  @Test
  public void testUpdate_InvalidOrganizationId_Enabled() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.update(new AutomaticApplicationsConfiguration(true, "testOrganizationID")))
        .withMessage("Parent organization ID testOrganizationID not found.");
  }

  @Test
  public void testUpdate_InvalidOrganizationId_Disabled() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.update(new AutomaticApplicationsConfiguration(false, "testOrganizationID")))
        .withMessage("Parent organization ID testOrganizationID not found.");
  }

  @Test
  public void testUpdate_EmptyParentOrganizationId_Enabled() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.update(new AutomaticApplicationsConfiguration(true, "")))
        .withMessage("Parent organization ID is required when automatic application creation is enabled.");
  }

  @Test
  public void testUpdate_EmptyParentOrganizationId_Disabled() {
    service.update(new AutomaticApplicationsConfiguration(false, ""));

    assertThat(automaticApplicationsConfigurationDAO.isEnabled()).isFalse();
    assertThat(automaticApplicationsConfigurationDAO.getOrganizationId()).isEqualTo("");
  }

  @Test
  public void testUpdate_NullParentOrganizationId_Enabled() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.update(new AutomaticApplicationsConfiguration(true, null)))
        .withMessage("Parent organization ID is required when automatic application creation is enabled.");
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
