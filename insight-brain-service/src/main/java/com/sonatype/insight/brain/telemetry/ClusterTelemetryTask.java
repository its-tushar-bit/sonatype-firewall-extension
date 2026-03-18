/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class ClusterTelemetryTask
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(ClusterTelemetryTask.class);

  // Visible for testing
  static final String NAME = "ClusterTelemetrySender";

  private static final String TELEMETRY_SEND_ERROR = "Cluster telemetry task error";

  private final Set<TelemetryCollector> clusterTelemetryCollectors;

  private final TaskScheduler taskScheduler;

  private final TelemetrySender telemetrySender;

  public boolean disableForTesting;

  @Inject
  public ClusterTelemetryTask(
      TelemetryCollectorsProvider telemetryCollectorsProvider,
      TaskScheduler taskScheduler,
      TelemetrySender telemetrySender)
  {
    this.clusterTelemetryCollectors = telemetryCollectorsProvider.getTelemetryCollectors()
        .stream()
        .filter(TelemetryCollector::isClusterTelemetry)
        .collect(Collectors.toSet());
    this.taskScheduler = taskScheduler;
    this.telemetrySender = telemetrySender;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    taskScheduler.schedulePeriodicTask(this, Duration.ofDays(1));
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(() -> {
      for (TelemetryCollector clusterTelemetryCollector : clusterTelemetryCollectors) {
        log.debug("Sending telemetry for {}", clusterTelemetryCollector.getClass().getSimpleName());
        long start = System.currentTimeMillis();

        if (clusterTelemetryCollector instanceof PaginatedTelemetryCollector paginatedTelemetryCollector) {
          sendPaginatedTelemetry(paginatedTelemetryCollector);
        }
        else {
          telemetrySender.send(clusterTelemetryCollector.collectAllData(context));
        }

        long stop = System.currentTimeMillis();
        log.debug("Telemetry for {} sent in {}ms", clusterTelemetryCollector.getClass(), stop - start);
      }
    }, log, TELEMETRY_SEND_ERROR);
  }

  private void sendPaginatedTelemetry(PaginatedTelemetryCollector paginatedTelemetryCollector) {
    try {
      log.trace("Sending first page of telemetry for {}", paginatedTelemetryCollector.getClass());
      telemetrySender.send(paginatedTelemetryCollector.firstPage());
      while (paginatedTelemetryCollector.hasMoreData()) {
        log.trace("Sending next page of telemetry for {}", paginatedTelemetryCollector.getClass());
        telemetrySender.send(paginatedTelemetryCollector.nextPage());
      }
      log.trace("All pages of telemetry for {} sent", paginatedTelemetryCollector.getClass());
    }
    catch (Exception e) {
      log.error("Skipping telemetry collection for {} due to an error", paginatedTelemetryCollector.getClass(), e);
    }
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
