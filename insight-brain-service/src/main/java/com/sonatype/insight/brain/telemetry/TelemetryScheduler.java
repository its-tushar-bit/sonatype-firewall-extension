/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.List;
import java.util.stream.Collectors;

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
    for (TelemetryCollector telemetryCollector : telemetryCollectors) {
      try {
        telemetrySender.send(telemetryCollector.collectAllData());
      }
      catch (Exception e) {
        log.debug("Unable to send telemetry for collector {}", telemetryCollector, e);
      }
    }
  }
}
