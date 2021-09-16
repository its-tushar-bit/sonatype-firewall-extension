/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Arrays;
import java.util.HashSet;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics.AggregatedSearchStats;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics.SearchCount;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AdvancedSearchTelemetryMetricsTest
    extends AbstractComponentTest
{
  @Inject
  private AdvancedSearchTelemetryMetrics metrics;

  @Test
  public void testComputeStatsAndReset_NoSearches() {
    AggregatedSearchStats stats = metrics.computeStatsAndReset();
    assertThat(stats.getSearchCounts().isEmpty());
    assertThat(stats.getTotalSearches()).isEqualTo(0L);
  }

  @Test
  public void testComputeStatsAndReset() {
    metrics.addSearch(new HashSet<>(Arrays.asList("organizationName", "vulnerabilityId")));
    metrics.addSearch(new HashSet<>(Arrays.asList("organizationName")));
    metrics.addSearch(new HashSet<>(Arrays.asList("itemType")));

    AggregatedSearchStats stats = metrics.computeStatsAndReset();
    assertThat(stats.getSearchCounts()).hasSize(3);
    assertThat(stats.getSearchCounts()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(new SearchCount("organizationName", 2L), new SearchCount("vulnerabilityId", 1L),
            new SearchCount("itemType", 1L));
    assertThat(stats.getTotalSearches()).isEqualTo(3L);
  }

  @Test
  public void testComputeStatsAndReset_TelemetryTotalsAreReset() {
    metrics.addSearch(new HashSet<>(Arrays.asList("organizationName", "vulnerabilityId")));
    metrics.addSearch(new HashSet<>(Arrays.asList("organizationName")));

    AggregatedSearchStats stats1 = metrics.computeStatsAndReset();
    assertThat(stats1.getSearchCounts()).hasSize(2);
    assertThat(stats1.getTotalSearches()).isEqualTo(2L);
    AggregatedSearchStats stats2 = metrics.computeStatsAndReset();
    assertThat(stats2.getSearchCounts()).hasSize(0);
    assertThat(stats2.getTotalSearches()).isEqualTo(0L);
  }
}
