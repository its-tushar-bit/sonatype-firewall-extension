/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class OwnerMaintenanceTelemetryCreatorTest
    extends AbstractComponentTest
{
  @Mock
  private TelemetrySender telemetrySenderMock;

  @Inject
  private OwnerMaintenanceTelemetryCreator telemetryCreator;

  @Inject
  private Configuration configuration;

  @Inject
  private ApiConfigurationService configurationService;

  @Inject
  private TelemetryUtils telemetryUtils;

  private Organization organization;

  private Application application;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
  }

  @Test
  public void testSendOwnerMaintenanceTelemetry_typeADD() {
    Map<String, Object> properties =
        Collections.singletonMap(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED, true);
    configurationService.setConfigurationInDatabaseNoAuthz(properties);
    configuration.configurationChanged(properties.keySet());

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
    Map<String, Object> properties =
        Collections.singletonMap(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED, true);
    configurationService.setConfigurationInDatabaseNoAuthz(properties);
    configuration.configurationChanged(properties.keySet());

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
  public void testSendOwnerMaintenanceTelemetry_typeADD_obfuscatesInformationIfAdvancedReportingDisabled() {
    // Toggle advanced reporting to make sure values are being obfuscated accordingly
    Map<String, Object> properties =
        Collections.singletonMap(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED, false);
    configurationService.setConfigurationInDatabaseNoAuthz(properties);
    configuration.configurationChanged(properties.keySet());

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
  public void testSendOwnerMaintenanceTelemetry_typeUPDATE_obfuscatesInformationIfAdvancedReportingDisabled() {
    // Toggle advanced reporting to make sure values are being obfuscated accordingly
    Map<String, Object> properties =
        Collections.singletonMap(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED, false);
    configurationService.setConfigurationInDatabaseNoAuthz(properties);
    configuration.configurationChanged(properties.keySet());

    // Given
    organization = tempEntity.newOrganization("Some Organization");
    application = tempEntity.newApplication("Other App", "publicIdB", organization.getId());

    // When
    final String maintenanceType = OwnerMaintenanceTelemetry.TYPE_UPDATE;
    telemetryCreator.sendOwnerMaintenanceTelemetry(application, maintenanceType);

    // Then
    assertTelemetry(application, maintenanceType);
  }

  private void assertTelemetry(
      final Application application,
      final String maintenanceType)
  {
    OwnerMaintenanceTelemetry ownerMaintenanceTelemetry =
        new OwnerMaintenanceTelemetry(
            application.getId(),
            application.getName(),
            application.getParentOwnerId(),
            application.getType().toString(),
            maintenanceType);
    if (!configuration.getAdvanceReportingInsightsEnabled()) {
      ownerMaintenanceTelemetry = new OwnerMaintenanceTelemetry(
          telemetryUtils.obfuscate(application.getId()),
          telemetryUtils.obfuscate(application.getName()),
          telemetryUtils.obfuscate(application.getParentOwnerId()),
          application.getType().toString(),
          maintenanceType);
    }

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
    assertThat(actual.getOwnerId()).isEqualTo(expected.getOwnerId());
    assertThat(actual.getOwnerName()).isEqualTo(expected.getOwnerName());
    assertThat(actual.getParentOwnerId()).isEqualTo(expected.getParentOwnerId());
    assertThat(actual.getOwnerType()).isEqualTo(expected.getOwnerType());
    assertThat(actual.getOwnerMaintenanceType()).isEqualTo(expected.getOwnerMaintenanceType());
  }
}
