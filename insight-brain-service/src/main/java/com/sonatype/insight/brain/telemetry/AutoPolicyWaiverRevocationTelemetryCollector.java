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

import com.sonatype.insight.telemetry.model.TelemetryData;

@Named
@Singleton
public class AutoPolicyWaiverRevocationTelemetryCollector
    implements TelemetryCollector
{
  private final AutoPolicyWaiverRevocationTelemetryMetrics metrics;

  @Inject
  public AutoPolicyWaiverRevocationTelemetryCollector(AutoPolicyWaiverRevocationTelemetryMetrics metrics) {
    this.metrics = metrics;
  }

  @Override
  public List<TelemetryData> collectAllData() {
    return metrics.computeStatsAndReset();
  }

  @Override
  public boolean isClusterTelemetry() {
    return false;
  }
}
