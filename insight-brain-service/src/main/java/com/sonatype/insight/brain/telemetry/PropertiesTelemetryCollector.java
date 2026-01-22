/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpsConnectorFactory;
import io.dropwizard.core.server.DefaultServerFactory;

/**
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
    List<ConnectorFactory> connectorFactories =
        ((DefaultServerFactory) config.getServerFactory()).getApplicationConnectors();
    boolean http = connectorFactories.stream().anyMatch(factory -> !(factory instanceof HttpsConnectorFactory));
    boolean https = connectorFactories.stream().anyMatch(factory -> factory instanceof HttpsConnectorFactory);
    telemetryData.getAttributes().put(CONNECTOR_HTTP, http);
    telemetryData.getAttributes().put(CONNECTOR_HTTPS, https);
    connectorFactories = ((DefaultServerFactory) config.getServerFactory()).getAdminConnectors();
    http = connectorFactories.stream().anyMatch(factory -> !(factory instanceof HttpsConnectorFactory));
    https = connectorFactories.stream().anyMatch(factory -> factory instanceof HttpsConnectorFactory);
    telemetryData.getAttributes().put(ADMIN_CONNECTOR_HTTP, http);
    telemetryData.getAttributes().put(ADMIN_CONNECTOR_HTTPS, https);
    return telemetryData;
  }

  @Override
  public boolean isClusterTelemetry() {
    return false;
  }
}
