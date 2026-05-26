/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Arrays;

/**
 * Collects configuration-related telemetry data.
 *
 * @since 1.69
 */
@Named
@Singleton
public class PropertiesTelemetryCollector
    implements TelemetryCollector
{
  private final InsightConfig config;

  private final Configuration configuration;

  public static final String REPORT_TIMEOUT_SECONDS = "report_timeout_seconds";

  static final String CONNECTOR_HTTP = "connector_http";

  static final String CONNECTOR_HTTPS = "connector_https";

  static final String ADMIN_CONNECTOR_HTTP = "admin_connector_http";

  static final String ADMIN_CONNECTOR_HTTPS = "admin_connector_https";

  @Inject
  public PropertiesTelemetryCollector(InsightConfig config, Configuration configuration) {
    this.config = config;
    this.configuration = configuration;
  }

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.CONFIGURATION_PROPERTIES);
    telemetryData.getAttributes().put(REPORT_TIMEOUT_SECONDS, configuration.getReportTimeoutInSeconds());

    telemetryData.getAttributes().put(CONNECTOR_HTTP, hasConnectorType(config.getApplicationConnectorTypes(), "http"));
    telemetryData.getAttributes()
        .put(CONNECTOR_HTTPS, hasConnectorType(config.getApplicationConnectorTypes(), "https"));
    telemetryData.getAttributes().put(ADMIN_CONNECTOR_HTTP, hasConnectorType(config.getAdminConnectorTypes(), "http"));
    telemetryData.getAttributes()
        .put(ADMIN_CONNECTOR_HTTPS, hasConnectorType(config.getAdminConnectorTypes(), "https"));

    return telemetryData;
  }

  private boolean hasConnectorType(String connectorTypes, String connectorType) {
    return Arrays.stream(connectorTypes.split(","))
        .map(String::trim)
        .anyMatch(type -> connectorType.equalsIgnoreCase(type));
  }

  @Override
  public boolean isClusterTelemetry() {
    return false;
  }
}
