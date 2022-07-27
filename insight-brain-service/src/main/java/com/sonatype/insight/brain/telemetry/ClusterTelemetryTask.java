/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;

import io.dropwizard.lifecycle.Managed;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class ClusterTelemetryTask
    implements Managed, InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(ClusterTelemetryTask.class);

  // Visible for testing
  static final String NAME = "ClusterTelemetrySender";

  private static final String TELEMETRY_SEND_ERROR = "Cluster telemetry task error";

  private final List<TelemetryCollector> clusterTelemetryCollectors;

  private final TaskScheduler taskScheduler;

  private final TelemetrySender telemetrySender;

  public boolean disableForTesting;

  @Inject
  public ClusterTelemetryTask(
      List<TelemetryCollector> telemetryCollectors,
      TaskScheduler taskScheduler,
      TelemetrySender telemetrySender)
  {
    this.clusterTelemetryCollectors = telemetryCollectors.stream()
        .filter(TelemetryCollector::isClusterTelemetry).collect(Collectors.toList());
    this.taskScheduler = taskScheduler;
    this.telemetrySender = telemetrySender;
  }

  @Override
  public void start() throws Exception {
    if (disableForTesting) {
      return;
    }
    taskScheduler.schedulePeriodicTask(ClusterTelemetryTask.class, NAME, Duration.ofDays(1));
  }

  @Override
  public void stop() {
    // noop
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(() -> {
      for (TelemetryCollector clusterTelemetryCollector : clusterTelemetryCollectors) {
        telemetrySender.send(clusterTelemetryCollector.collectAllData());
      }
    }, log, TELEMETRY_SEND_ERROR);
  }
}
