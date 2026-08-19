/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.quartz.JobExecutionContext;

import com.sonatype.insight.telemetry.model.TelemetryData;

/**
 * Implementing classes should override ONE of the collectData or collectAllData methods. Overriding multiple methods
 * may result in duplicate telemetry collection
 *
 * @since 1.52
 */
public interface TelemetryCollector
{
  /**
   * TelemetryCollectors whose logic relies on the JobExecutionContext can override this method.
   * Returns the {@code TelemetryData} to send for this particular collector.
   */
  default TelemetryData collectData(JobExecutionContext jobExecutionContext) {
    return null;
  }

  /**
   * TelemetryCollectors whose logic doesn't rely on the JobExecutionContext can override this method.
   */
  default TelemetryData collectData() {
    return null;
  }

  /**
   * TelemetryCollectors whose logic relies on the JobExecutionContext can override this method.
   * Returns all of the {@code TelemetryData} to send for this particular collector.
   */
  default List<TelemetryData> collectAllData(JobExecutionContext jobExecutionContext) {
    return Stream.concat(Optional.ofNullable(collectData(jobExecutionContext)).stream(), collectAllData().stream())
        .collect(Collectors.toList());
  }

  /**
   * TelemetryCollectors whose logic doesn't rely on the JobExecutionContext can override this method.
   */
  default List<TelemetryData> collectAllData() {
    TelemetryData telemetryData = collectData();
    return telemetryData == null ? List.of() : List.of(telemetryData);
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
