/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @since GLOBAL_SEARCH
 */
@Named
@Singleton
public class AdvancedSearchTelemetryMetrics
{
  private final Map<String, Long> searchesByFieldNameMap = new HashMap<>();

  private long totalSearches = 0;

  public synchronized void addSearch(Set<String> fieldNames) {
    fieldNames.forEach(fieldName -> searchesByFieldNameMap.merge(fieldName, 1L, Long::sum));
    totalSearches++;
  }

  /**
   * Compute statistics of searches since last call to this method.
   * Results are cleared after computation.
   */
  synchronized AggregatedSearchStats computeStatsAndReset() {
    List<SearchCount> searchCounts = searchesByFieldNameMap.entrySet().stream()
        .map(p -> new SearchCount(p.getKey(), p.getValue())).collect(Collectors.toList());

    AggregatedSearchStats stats = new AggregatedSearchStats(totalSearches, searchCounts);

    searchesByFieldNameMap.clear();
    totalSearches = 0;
    return stats;
  }

  static class AggregatedSearchStats
  {
    private List<SearchCount> searchCounts;

    private long totalSearches;

    AggregatedSearchStats(long totalSearches, List<SearchCount> searchCounts) {
      this.totalSearches = totalSearches;
      this.searchCounts = searchCounts;
    }

    long getTotalSearches() {
      return totalSearches;
    }

    List<SearchCount> getSearchCounts() {
      return searchCounts;
    }
  }

  public static class SearchCount
  {
    public String field;

    public long searches;

    public SearchCount() {
      // for deserialization
    }

    public SearchCount(String field, long searches) {
      this.field = field;
      this.searches = searches;
    }
  }
}
