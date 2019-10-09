/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import javax.inject.Inject;

import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

public class ReportsTelemetry
{
  public static final String REPORT_TYPE_ATTR = "report_type";

  private final TelemetrySender telemetrySender;

  @Inject
  public ReportsTelemetry(TelemetrySender telemetrySender) {
    this.telemetrySender = telemetrySender;
  }

  public void sendComponentWithWaiversTelemetry() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REPORT_API);
    telemetryData.getAttributes().put(REPORT_TYPE_ATTR, ReportType.COMPONENTS_WITH_WAIVERS.toString());

    telemetrySender.send(telemetryData);
  }

  public void sendAllApplicationsTelemetry() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REPORT_API);
    telemetryData.getAttributes().put(REPORT_TYPE_ATTR, ReportType.ALL_APPLICATIONS.toString());

    telemetrySender.send(telemetryData);
  }

  public void sendSingleApplicationTelemetry() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REPORT_API);
    telemetryData.getAttributes().put(REPORT_TYPE_ATTR, ReportType.SINGLE_APPLICATION.toString());

    telemetrySender.send(telemetryData);
  }

  public void sendMetricsTelemetry() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REPORT_API);
    telemetryData.getAttributes().put(REPORT_TYPE_ATTR, ReportType.METRICS.toString());

    telemetrySender.send(telemetryData);
  }
}
