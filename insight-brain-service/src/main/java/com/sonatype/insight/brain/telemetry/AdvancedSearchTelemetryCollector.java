/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics.AggregatedSearchStats;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

/**
 * @since 1.88
 */
@Named
@Singleton
public class AdvancedSearchTelemetryCollector
    implements TelemetryCollector
{
  public static final String TOTAL_SEARCHES_BY_FIELD_NAME = "total_searches_by_field_name";

  public static final String TOTAL_SEARCHES = "total_searches";

  private final AdvancedSearchTelemetryMetrics metrics;

  @Inject
  public AdvancedSearchTelemetryCollector(AdvancedSearchTelemetryMetrics metrics) {
    this.metrics = metrics;
  }

  @Override
  public TelemetryData collectData() {
    AggregatedSearchStats aggregatedSearchStats = metrics.computeStatsAndReset();
    if (aggregatedSearchStats.getTotalSearches() == 0) {
      return null;
    }
    else {
      TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.ADVANCED_SEARCH);
      Map<String, Object> attributes = telemetryData.getAttributes();
      attributes.put(TOTAL_SEARCHES_BY_FIELD_NAME, aggregatedSearchStats.getSearchCounts());
      attributes.put(TOTAL_SEARCHES, aggregatedSearchStats.getTotalSearches());
      return telemetryData;
    }
  }
}
