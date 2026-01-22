/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
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

  private final IndexService indexService;

  @Inject
  public AdvancedSearchTelemetryCollector(AdvancedSearchTelemetryMetrics metrics, IndexService indexService) {
    this.metrics = metrics;
    this.indexService = indexService;
  }

  @Override
  public List<TelemetryData> collectAllData() {
    List<TelemetryData> allTelemetryData = new ArrayList<>();
    AggregatedSearchStats aggregatedSearchStats = metrics.computeStatsAndReset();
    if (aggregatedSearchStats.getTotalSearches() > 0) {
      TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.ADVANCED_SEARCH);
      telemetryData.put(TOTAL_SEARCHES_BY_FIELD_NAME, aggregatedSearchStats.getSearchCounts());
      telemetryData.put(TOTAL_SEARCHES, aggregatedSearchStats.getTotalSearches());
      allTelemetryData.add(telemetryData);
    }
    long indexSize = indexService.getIndexSize();
    if (indexSize > 0) {
      TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.ADVANCED_SEARCH_INDEXING);
      telemetryData.put(SearchIndexClient.SEARCH_INDEX_SIZE_BYTES, indexSize);
      telemetryData.put(SearchIndexClient.SEARCH_INDEX_REINDEX, false);
      allTelemetryData.add(telemetryData);
    }
    return allTelemetryData;
  }

  @Override
  public boolean isClusterTelemetry() {
    return false;
  }
}
