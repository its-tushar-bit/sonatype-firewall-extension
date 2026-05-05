/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.telemetry.model.TelemetryData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TelemetryScheduler
{
  private static final Logger log = LoggerFactory.getLogger(TelemetryScheduler.class);

  final List<TelemetryCollector> telemetryCollectors;

  final TelemetrySender telemetrySender;

  protected TelemetryScheduler(
      TelemetryCollectorsProvider telemetryCollectorsProvider,
      TelemetrySender telemetrySender)
  {
    this.telemetryCollectors = telemetryCollectorsProvider.getTelemetryCollectors()
        .stream()
        .filter(telemetryCollector -> !telemetryCollector.isClusterTelemetry())
        .collect(Collectors.toList());
    this.telemetrySender = telemetrySender;
  }

  protected void sendTelemetry(List<TelemetryCollector> telemetryCollectors) {
    long startMs = System.currentTimeMillis();
    int collectorCount = 0;
    int totalRecords = 0;
    int failedCollectors = 0;

    for (TelemetryCollector telemetryCollector : telemetryCollectors) {
      try {
        List<TelemetryData> data = telemetryCollector.collectAllData();
        telemetrySender.send(data);
        totalRecords += data.size();
        collectorCount++;
      }
      catch (Exception e) {
        failedCollectors++;
        log.warn("Unable to send telemetry for collector {}", telemetryCollector, e);
      }
    }

    long elapsedMs = System.currentTimeMillis() - startMs;
    int totalCollectors = telemetryCollectors.size();
    // Counts reflect successful collection and enqueueing — use rest/telemetry/receipts for delivery confirmation
    log.info("Telemetry schedule completed: {} of {} collectors succeeded, {} failed, {} records queued, {}ms elapsed",
        collectorCount, totalCollectors, failedCollectors, totalRecords, elapsedMs);
  }
}
