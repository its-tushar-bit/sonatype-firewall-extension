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
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantUtil;

/**
 * @since 1.88
 */
@Named
@Singleton
public class AdvancedSearchTelemetryMetrics
{
  /**
   * This same bean is used for both IQ and MTIQ. Wrapping the in-memory state in a "TenantReference" means that the
   * telemetry will be stored and accessed per tenant and in the case of on-prem there will only be a single tenant.
   */
  private final TenantReference<Map<String, Long>> searchesByFieldNameMap = new TenantReference<>(HashMap::new);

  private final TenantReference<Long> totalSearches = new TenantReference<>(() -> 0L);

  private final TenantUtil tenantUtil;

  @Inject
  public AdvancedSearchTelemetryMetrics(TenantUtil tenantUtil) {
    this.tenantUtil = tenantUtil;
  }

  public void addSearch(Set<String> fieldNames) {
    // Synchronize by tenant so that the counts and searches do not get out of sync
    synchronized (tenantUtil.getTenantSlugForSynchronization()) {
      fieldNames.forEach(fieldName -> searchesByFieldNameMap.get().merge(fieldName, 1L, Long::sum));
      totalSearches.set(totalSearches.get() + 1);
    }
  }

  /**
   * Compute statistics of searches since last call to this method. Results are cleared after computation.
   */
  AggregatedSearchStats computeStatsAndReset() {
    synchronized (tenantUtil.getTenantSlugForSynchronization()) {
      List<SearchCount> searchCounts = searchesByFieldNameMap.get().entrySet().stream()
          .map(p -> new SearchCount(p.getKey(), p.getValue())).collect(Collectors.toList());

      AggregatedSearchStats stats = new AggregatedSearchStats(totalSearches.get(), searchCounts);

      searchesByFieldNameMap.get().clear();
      totalSearches.set(0L);
      return stats;
    }
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
