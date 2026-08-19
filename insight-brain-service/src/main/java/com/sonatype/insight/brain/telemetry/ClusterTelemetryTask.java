/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.telemetry.model.TelemetryData;

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
      long taskStartMs = System.currentTimeMillis();
      int collectorCount = 0;
      int totalRecords = 0;
      int failedCollectors = 0;

      for (TelemetryCollector clusterTelemetryCollector : clusterTelemetryCollectors) {
        long start = System.currentTimeMillis();

        try {
          if (clusterTelemetryCollector instanceof PaginatedTelemetryCollector paginatedTelemetryCollector) {
            totalRecords += sendPaginatedTelemetry(paginatedTelemetryCollector);
          }
          else {
            List<TelemetryData> data = clusterTelemetryCollector.collectAllData(context);
            telemetrySender.send(data);
            totalRecords += data.size();
          }
          collectorCount++;
          log.info("Sent telemetry for {} in {}ms", clusterTelemetryCollector.getClass().getSimpleName(),
              System.currentTimeMillis() - start);
        }
        catch (Exception e) {
          failedCollectors++;
          log.warn("Failed to send telemetry for {} in {}ms", clusterTelemetryCollector.getClass().getSimpleName(),
              System.currentTimeMillis() - start, e);
        }
      }

      int totalCollectors = clusterTelemetryCollectors.size();
      long taskElapsedMs = System.currentTimeMillis() - taskStartMs;
      // Counts reflect successful collection and enqueueing — use rest/telemetry/receipts for delivery confirmation
      log.info(
          "Cluster telemetry task completed: {} of {} collectors succeeded, {} failed, {} records queued, {}ms elapsed",
          collectorCount, totalCollectors, failedCollectors, totalRecords, taskElapsedMs);
    }, log, TELEMETRY_SEND_ERROR);
  }

  private int sendPaginatedTelemetry(PaginatedTelemetryCollector paginatedTelemetryCollector) {
    int recordCount = 0;
    log.trace("Sending first page of telemetry for {}", paginatedTelemetryCollector.getClass());
    telemetrySender.send(paginatedTelemetryCollector.firstPage());
    recordCount++;
    while (paginatedTelemetryCollector.hasMoreData()) {
      log.trace("Sending next page of telemetry for {}", paginatedTelemetryCollector.getClass());
      telemetrySender.send(paginatedTelemetryCollector.nextPage());
      recordCount++;
    }
    log.trace("All pages of telemetry for {} sent", paginatedTelemetryCollector.getClass());
    return recordCount;
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
