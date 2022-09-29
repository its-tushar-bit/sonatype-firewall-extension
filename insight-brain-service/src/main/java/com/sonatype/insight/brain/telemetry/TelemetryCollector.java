/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.telemetry.model.TelemetryData;

/**
 * @since 1.52
 */
public interface TelemetryCollector
{
  /**
   * Returns the {@code TelemetryData} to send for this particular collector.
   */
  default TelemetryData collectData() {
    return null;
  }

  /**
   * Returns all of the {@code TelemetryData} to send for this particular collector.
   */
  default List<TelemetryData> collectAllData() {
    List<TelemetryData> allTelemetryData = new ArrayList<>();
    TelemetryData telemetryData = collectData();
    if (telemetryData != null) {
      allTelemetryData.add(telemetryData);
    }
    return allTelemetryData;
  }

  /**
   * Returns whether the telemetry will be sent either per cluster or per node.
   * <ul>
   * <li>If true, the telemetry will be sent from one node in the cluster via {@link ClusterTelemetryTask}.</li>
   * <li>If false, the telemetry will be sent from each node in the cluster via {@link TelemetryScheduler}.</li>
   * </ul>
   * In either case the telemetry is sent periodically.
   */
  boolean isClusterTelemetry();
}
