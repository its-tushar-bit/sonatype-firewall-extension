/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.scheduler.MultiTenantTaskScheduler;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;

import org.quartz.JobExecutionContext;

@Named
@Singleton
public class MultiTenantTelemetryScheduler
    implements InsightJob
{
  static final String NAME = "MultiTenantTelemetryScheduler";

  private final TaskScheduler taskScheduler;

  private final MultiTenantTelemetryTask multiTenantTelemetryTask;

  @Inject
  public MultiTenantTelemetryScheduler(
      MultiTenantTaskScheduler taskScheduler, final MultiTenantTelemetryTask multiTenantTelemetryTask)
  {
    this.taskScheduler = taskScheduler;
    this.multiTenantTelemetryTask = multiTenantTelemetryTask;
  }

  @Override
  public void register() {
    taskScheduler.schedulePeriodicTask(this, Duration.ofDays(1), randomStartTimeInNext24Hours());
  }

  /**
   * All nodes in the cluster need to send telemetry data.
   * The node that picks up this job sends its own telemetry data and then kicks off a job on all the other nodes.
   * The <code>MultiTenantTelemetryTask</code> is required to avoid the job permanently being rescheduled
   */
  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    multiTenantTelemetryTask.execute(null);
    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(multiTenantTelemetryTask);
  }

  private static Date randomStartTimeInNext24Hours() {
    long now = new Date().getTime();
    long endMillis = new Date(now + TimeUnit.HOURS.toMillis(24)).getTime();
    long randomMillisSinceEpoch = ThreadLocalRandom.current().nextLong(now, endMillis);
    return new Date(randomMillisSinceEpoch);
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
