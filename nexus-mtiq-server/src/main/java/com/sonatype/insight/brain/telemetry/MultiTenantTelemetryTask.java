/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.InsightJob;

import org.quartz.JobExecutionContext;

@Named
@Singleton
public class MultiTenantTelemetryTask
    extends TelemetryScheduler
    implements InsightJob
{
  static final String NAME = "MultiTenantTelemetryTask";

  @Inject
  public MultiTenantTelemetryTask(
      TelemetryCollectorsProvider telemetryCollectorsProvider,
      TelemetrySender telemetrySender)
  {
    super(telemetryCollectorsProvider, telemetrySender);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    sendTelemetry(telemetryCollectors);
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
