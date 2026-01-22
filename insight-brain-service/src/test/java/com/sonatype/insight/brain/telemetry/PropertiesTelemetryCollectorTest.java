/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Arrays;
import java.util.Collections;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.jetty.HttpsConnectorFactory;
import io.dropwizard.core.server.DefaultServerFactory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertiesTelemetryCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private PropertiesTelemetryCollector telemetryCollector;

  @Inject
  private InsightConfig insightConfig;

  @Inject
  private ApiConfigurationService configurationService;

  @Test
  public void testCollectData_TelemetryPurpose() {
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.CONFIGURATION_PROPERTIES);
  }

  @Test
  public void testCollectData_ReturnsConfiguredReportTimeout() {
    int configuredTimeout = 600;
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS,
        configuredTimeout);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS);
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getAttributes().get(PropertiesTelemetryCollector.REPORT_TIMEOUT_SECONDS))
        .isEqualTo(configuredTimeout);
  }

  @Test
  public void testCollectData_ConnectorTypes_AppHttpOnly() {
    ((DefaultServerFactory) insightConfig.getServerFactory())
        .setApplicationConnectors(Collections.singletonList(new HttpConnectorFactory()));
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getAttributes()).containsEntry(PropertiesTelemetryCollector.CONNECTOR_HTTP, true)
        .containsEntry(PropertiesTelemetryCollector.CONNECTOR_HTTPS, false);
  }

  @Test
  public void testCollectData_ConnectorTypes_AppHttpsOnly() {
    ((DefaultServerFactory) insightConfig.getServerFactory())
        .setApplicationConnectors(Collections.singletonList(new HttpsConnectorFactory()));
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getAttributes()).containsEntry(PropertiesTelemetryCollector.CONNECTOR_HTTP, false)
        .containsEntry(PropertiesTelemetryCollector.CONNECTOR_HTTPS, true);
  }

  @Test
  public void testCollectData_ConnectorTypes_AppHttpAndHttps() {
    ((DefaultServerFactory) insightConfig.getServerFactory())
        .setApplicationConnectors(Arrays.asList(new HttpConnectorFactory(), new HttpsConnectorFactory()));
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getAttributes()).containsEntry(PropertiesTelemetryCollector.CONNECTOR_HTTP, true)
        .containsEntry(PropertiesTelemetryCollector.CONNECTOR_HTTPS, true);
  }

  @Test
  public void testCollectData_ConnectorTypes_AdminHttpOnly() {
    ((DefaultServerFactory) insightConfig.getServerFactory())
        .setAdminConnectors(Collections.singletonList(new HttpConnectorFactory()));
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getAttributes()).containsEntry(PropertiesTelemetryCollector.ADMIN_CONNECTOR_HTTP, true)
        .containsEntry(PropertiesTelemetryCollector.ADMIN_CONNECTOR_HTTPS, false);
  }

  @Test
  public void testCollectData_ConnectorTypes_AdminHttpsOnly() {
    ((DefaultServerFactory) insightConfig.getServerFactory())
        .setAdminConnectors(Collections.singletonList(new HttpsConnectorFactory()));
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getAttributes()).containsEntry(PropertiesTelemetryCollector.ADMIN_CONNECTOR_HTTP, false)
        .containsEntry(PropertiesTelemetryCollector.ADMIN_CONNECTOR_HTTPS, true);
  }

  @Test
  public void testCollectData_ConnectorTypes_AdminHttpAndHttps() {
    ((DefaultServerFactory) insightConfig.getServerFactory())
        .setAdminConnectors(Arrays.asList(new HttpConnectorFactory(), new HttpsConnectorFactory()));
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getAttributes()).containsEntry(PropertiesTelemetryCollector.ADMIN_CONNECTOR_HTTP, true)
        .containsEntry(PropertiesTelemetryCollector.ADMIN_CONNECTOR_HTTPS, true);
  }

  @Test
  public void testIsClusterTelemetry() {
    assertThat(telemetryCollector.isClusterTelemetry()).isFalse();
  }
}
