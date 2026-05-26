/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import org.junit.Before;
import org.junit.Test;

public class PropertiesTelemetryCollectorTest
{
  private final InsightConfig insightConfig = new InsightConfig();

  private final Configuration configuration = mock(Configuration.class);

  private PropertiesTelemetryCollector telemetryCollector;

  @Before
  public void setUp() {
    when(configuration.getReportTimeoutInSeconds()).thenReturn(300);
    telemetryCollector = new PropertiesTelemetryCollector(insightConfig, configuration);
  }

  @Test
  public void testCollectData_TelemetryPurpose() {
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.CONFIGURATION_PROPERTIES);
  }

  @Test
  public void testCollectData_ReturnsConfiguredReportTimeout() {
    int configuredTimeout = 600;
    when(configuration.getReportTimeoutInSeconds()).thenReturn(configuredTimeout);

    TelemetryData telemetryData = telemetryCollector.collectData();

    assertThat(telemetryData.getAttributes().get(PropertiesTelemetryCollector.REPORT_TIMEOUT_SECONDS))
        .isEqualTo(configuredTimeout);
  }

  @Test
  public void testCollectData_ConnectorTypes_AppHttpOnly() {
    insightConfig.setApplicationConnectorTypes("http");

    TelemetryData telemetryData = telemetryCollector.collectData();

    assertThat(telemetryData.getAttributes()).containsEntry(PropertiesTelemetryCollector.CONNECTOR_HTTP, true)
        .containsEntry(PropertiesTelemetryCollector.CONNECTOR_HTTPS, false);
  }

  @Test
  public void testCollectData_ConnectorTypes_AppHttpsOnly() {
    insightConfig.setApplicationConnectorTypes("https");

    TelemetryData telemetryData = telemetryCollector.collectData();

    assertThat(telemetryData.getAttributes()).containsEntry(PropertiesTelemetryCollector.CONNECTOR_HTTP, false)
        .containsEntry(PropertiesTelemetryCollector.CONNECTOR_HTTPS, true);
  }

  @Test
  public void testCollectData_ConnectorTypes_AppHttpAndHttps() {
    insightConfig.setApplicationConnectorTypes("http,https");

    TelemetryData telemetryData = telemetryCollector.collectData();

    assertThat(telemetryData.getAttributes()).containsEntry(PropertiesTelemetryCollector.CONNECTOR_HTTP, true)
        .containsEntry(PropertiesTelemetryCollector.CONNECTOR_HTTPS, true);
  }

  @Test
  public void testCollectData_ConnectorTypes_AdminHttpOnly() {
    insightConfig.setAdminConnectorTypes("http");

    TelemetryData telemetryData = telemetryCollector.collectData();

    assertThat(telemetryData.getAttributes()).containsEntry(PropertiesTelemetryCollector.ADMIN_CONNECTOR_HTTP, true)
        .containsEntry(PropertiesTelemetryCollector.ADMIN_CONNECTOR_HTTPS, false);
  }

  @Test
  public void testCollectData_ConnectorTypes_AdminHttpsOnly() {
    insightConfig.setAdminConnectorTypes("https");

    TelemetryData telemetryData = telemetryCollector.collectData();

    assertThat(telemetryData.getAttributes()).containsEntry(PropertiesTelemetryCollector.ADMIN_CONNECTOR_HTTP, false)
        .containsEntry(PropertiesTelemetryCollector.ADMIN_CONNECTOR_HTTPS, true);
  }

  @Test
  public void testCollectData_ConnectorTypes_AdminHttpAndHttps() {
    insightConfig.setAdminConnectorTypes("http,https");

    TelemetryData telemetryData = telemetryCollector.collectData();

    assertThat(telemetryData.getAttributes()).containsEntry(PropertiesTelemetryCollector.ADMIN_CONNECTOR_HTTP, true)
        .containsEntry(PropertiesTelemetryCollector.ADMIN_CONNECTOR_HTTPS, true);
  }

  @Test
  public void testIsClusterTelemetry() {
    assertThat(telemetryCollector.isClusterTelemetry()).isFalse();
  }
}
