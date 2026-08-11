/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.search.index.RankedGroupsResult;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VulnerabilitiesListServicePagingTest
{
  @Test
  public void hasNextPage_falseWhenConsumedReachesMaterializedCapEvenIfEstateTotalLarger() {
    // Estate total 8000, only 5000 materialized; last full page at page 49 (pageSize 100).
    assertThat(VulnerabilitiesListService.hasNextPage(49, 100, 100, 8000, 5000)).isFalse();
    // Empty page past the materialization window must not keep paging.
    assertThat(VulnerabilitiesListService.hasNextPage(50, 100, 0, 8000, 5000)).isFalse();
  }

  @Test
  public void hasNextPage_trueWhileMoreMaterializedRowsRemain() {
    assertThat(VulnerabilitiesListService.hasNextPage(0, 100, 100, 250, 250)).isTrue();
    assertThat(VulnerabilitiesListService.hasNextPage(1, 100, 100, 250, 250)).isTrue();
    assertThat(VulnerabilitiesListService.hasNextPage(2, 100, 50, 250, 250)).isFalse();
  }

  @Test
  public void hasNextPage_usesLongArithmeticForLargePage() {
    assertThat(VulnerabilitiesListService.hasNextPage(21_474_837L, 100, 0, 8000, 5000)).isFalse();
  }

  @Test
  public void severityFacets_foldsUnbandedGroupsIntoNoneSoBucketsSumToTotal() {
    Map<String, Long> bandCounts = new LinkedHashMap<>();
    bandCounts.put("none", 1L);
    bandCounts.put("low", 0L);
    bandCounts.put("medium", 1L);
    bandCounts.put("high", 0L);
    bandCounts.put("critical", 2L);
    RankedGroupsResult ranked = new RankedGroupsResult(List.of(), 6L, true, bandCounts, 2L);

    Map<String, Long> severities = VulnerabilitiesListService.severityFacets(ranked);

    assertThat(severities.get("critical")).isEqualTo(2L);
    assertThat(severities.get("medium")).isEqualTo(1L);
    assertThat(severities.get("high")).isZero();
    assertThat(severities.get("low")).isZero();
    // Unscored and out-of-range vulnerabilities land in none rather than an unknown bucket.
    assertThat(severities.get("none")).isEqualTo(3L);
    assertThat(severities).doesNotContainKey("unknown");
    assertThat(severities.values().stream().mapToLong(Long::longValue).sum())
        .isEqualTo(ranked.distinctGroupCount());
  }

}
