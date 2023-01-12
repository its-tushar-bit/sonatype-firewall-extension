/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.InsightJob;

import org.quartz.JobExecutionContext;

@Named
@Singleton
public class MultiTenantTelemetryTask
    extends TelemetryScheduler
    implements InsightJob
{
  @Inject
  public MultiTenantTelemetryTask(
      List<TelemetryCollector> telemetryCollectors,
      TelemetrySender telemetrySender)
  {
    super(telemetryCollectors, telemetrySender);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    sendTelemetry(telemetryCollectors);
  }
}
