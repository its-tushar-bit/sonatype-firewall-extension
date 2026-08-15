/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics.AggregatedSearchStats;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics.SearchCount;
import com.sonatype.insight.brain.tenancy.Tenant;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ComponentH2Test
public class AdvancedSearchTelemetryMetricsTest
    extends AbstractComponentH2Test
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
    metrics.addSearch(new HashSet<>(Collections.singletonList("organizationName")));
    metrics.addSearch(new HashSet<>(Collections.singletonList("itemType")));

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
    metrics.addSearch(new HashSet<>(Collections.singletonList("organizationName")));

    AggregatedSearchStats stats1 = metrics.computeStatsAndReset();
    assertThat(stats1.getSearchCounts()).hasSize(2);
    assertThat(stats1.getTotalSearches()).isEqualTo(2L);
    AggregatedSearchStats stats2 = metrics.computeStatsAndReset();
    assertThat(stats2.getSearchCounts()).hasSize(0);
    assertThat(stats2.getTotalSearches()).isEqualTo(0L);
  }

  @Test
  public void testShouldNotLeakDataBetweenTenants_whenMultiTenantMode() {
    Runnable mockRunnable = mock(Runnable.class);

    Tenant tenant1 = testAsNewTenant(testName, t1 -> {
      metrics.addSearch(new HashSet<>(Arrays.asList("organizationName", "vulnerabilityId")));
      metrics.addSearch(new HashSet<>(Collections.singletonList("itemType")));

      mockRunnable.run();
    });

    Tenant tenant2 = testAsNewTenant(testName, t2 -> {
      metrics.addSearch(new HashSet<>(Collections.singletonList("organizationName")));

      mockRunnable.run();
    });

    testAsTenant(tenant1, t1 -> {
      metrics.addSearch(new HashSet<>(Collections.singletonList("newItemType")));

      assertStatCounts(4, 3L);

      mockRunnable.run();
    });

    testAsTenant(tenant2, t2 -> {
      assertStatCounts(1, 1L);

      mockRunnable.run();
    });

    // Default "SINGLE_TENANT" should remain unaffected
    assertStatCounts(0, 0L);

    // Check test code was actually called
    verify(mockRunnable, times(4)).run();
  }

  private void assertStatCounts(int expectedSearchCounts, long expectedTotalSearches) {
    AggregatedSearchStats stats = metrics.computeStatsAndReset();
    assertThat(stats.getSearchCounts()).hasSize(expectedSearchCounts);
    assertThat(stats.getTotalSearches()).isEqualTo(expectedTotalSearches);
  }
}
