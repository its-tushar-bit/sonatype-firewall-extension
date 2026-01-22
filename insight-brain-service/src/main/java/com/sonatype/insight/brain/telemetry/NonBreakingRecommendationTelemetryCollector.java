/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.telemetry;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.telemetry.model.TelemetryData;

@Named
@Singleton
public class NonBreakingRecommendationTelemetryCollector implements TelemetryCollector
{
  private final NonBreakingRecommendationTelemetryMetrics metrics;

  @Inject
  public NonBreakingRecommendationTelemetryCollector(NonBreakingRecommendationTelemetryMetrics metrics) {
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
