/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OwnerMaintenanceTelemetryCreatorTest
    extends AbstractComponentTest
{
  @Mock
  private TelemetrySender telemetrySenderMock;

  @Inject
  private OwnerMaintenanceTelemetryCreator telemetryCreator;

  private Organization organization;

  private Application application;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    ApiConfigFeaturesService.SystemConfigurationPropertyFeature.INTEGRATED_ENTERPRISE_REPORTING.setEnabled(true);
  }

  @Test
  public void testSendOwnerMaintenanceTelemetry_typeADD() {
    // Given
    organization = tempEntity.newOrganization("Some Organization");
    application = tempEntity.newApplication("Some App", "publicIdA", organization.getId());

    // When
    final String maintenanceType = OwnerMaintenanceTelemetry.TYPE_ADD;
    telemetryCreator.sendOwnerMaintenanceTelemetry(application, maintenanceType);

    // Then
    assertTelemetry(application, maintenanceType);
  }

  @Test
  public void testSendOwnerMaintenanceTelemetry_typeUPDATE() {
    // Given
    organization = tempEntity.newOrganization("Some Organization");
    application = tempEntity.newApplication("Other App", "publicIdB", organization.getId());

    // When
    final String maintenanceType = OwnerMaintenanceTelemetry.TYPE_UPDATE;
    telemetryCreator.sendOwnerMaintenanceTelemetry(application, maintenanceType);

    // Then
    assertTelemetry(application, maintenanceType);
  }

  @Test
  public void testSendOwnerMaintenanceTelemetry_IntegratedEnterpriseReportingDisabled() {
    // Given
    ApiConfigFeaturesService.SystemConfigurationPropertyFeature.INTEGRATED_ENTERPRISE_REPORTING.setEnabled(
        false);
    organization = tempEntity.newOrganization("Some Organization");
    application = tempEntity.newApplication("Other App", "publicIdB", organization.getId());

    // When
    final String maintenanceType = OwnerMaintenanceTelemetry.TYPE_UPDATE;
    telemetryCreator.sendOwnerMaintenanceTelemetry(application, maintenanceType);

    // Then
    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock, times(0)).send(telemetryDataArgumentCaptor.capture());
  }

  private void assertTelemetry(
      final Application application,
      final String maintenanceType)
  {
    final OwnerMaintenanceTelemetry ownerMaintenanceTelemetry =
        new OwnerMaintenanceTelemetry(application.getId(), application.getName(), maintenanceType);

    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
    final TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    final Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put(OwnerMaintenanceTelemetry.OWNER_MAINTENANCE_TELEMETRY, ownerMaintenanceTelemetry);

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REAL_OWNER_IDS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertAttributes(expectedAttributes, telemetryData.getAttributes());
  }

  private void assertAttributes(
      final Map<String, Object> expectedAttributes,
      final Map<String, Object> actualAttributes)
  {
    assertThat(actualAttributes).containsKey(OwnerMaintenanceTelemetry.OWNER_MAINTENANCE_TELEMETRY);
    assertTelemetryData(
        (OwnerMaintenanceTelemetry) expectedAttributes.get(OwnerMaintenanceTelemetry.OWNER_MAINTENANCE_TELEMETRY),
        (OwnerMaintenanceTelemetry) actualAttributes.get(OwnerMaintenanceTelemetry.OWNER_MAINTENANCE_TELEMETRY));
  }

  private void assertTelemetryData(final OwnerMaintenanceTelemetry expected, final OwnerMaintenanceTelemetry actual) {
    assertThat(actual.getApplicationId()).isEqualTo(expected.getApplicationId());
    assertThat(actual.getApplicationName()).isEqualTo(expected.getApplicationName());
    assertThat(actual.getOwnerMaintenanceType()).isEqualTo(expected.getOwnerMaintenanceType());
  }
}
