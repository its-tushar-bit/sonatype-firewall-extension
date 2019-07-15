/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

/**
 * @since 1.69
 */
@Named
@Singleton
public class PropertiesTelemetryCollector
    implements TelemetryCollector
{
  private final InsightConfig config;

  public static final String REPORT_TIMEOUT_SECONDS = "report_timeout_seconds";

  @Inject
  public PropertiesTelemetryCollector(InsightConfig config) {
    this.config = config;
  }

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.CONFIGURATION_PROPERTIES);
    telemetryData.getAttributes().put(REPORT_TIMEOUT_SECONDS, config.getReportTimeoutInSeconds());
    return telemetryData;
  }
}
