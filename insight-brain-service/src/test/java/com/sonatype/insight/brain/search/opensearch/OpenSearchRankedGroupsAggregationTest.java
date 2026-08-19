/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.search.index.RankedGroup;
import com.sonatype.insight.brain.search.index.RankedGroupsResult;
import com.sonatype.insight.brain.search.opensearch.OpenSearchRankedGroupsAggregation.GroupMax;

import org.junit.jupiter.api.Test;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Buckets;
import org.opensearch.client.opensearch._types.aggregations.CompositeAggregate;
import org.opensearch.client.opensearch._types.aggregations.CompositeBucket;
import org.opensearch.client.opensearch._types.aggregations.MaxAggregate;
import org.opensearch.client.opensearch.core.SearchResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenSearchRankedGroupsAggregationTest
{
  @Test
  public void reduceGroups_ascending_placesUnscoredLast() {
    List<GroupMax> groups = List.of(
        new GroupMax("scored-low", 4.0f),
        new GroupMax("unscored", Float.NaN),
        new GroupMax("scored-high", 8.0f),
        new GroupMax("mixed-high", 9.5f));

    RankedGroupsResult result = OpenSearchRankedGroupsAggregation.reduceGroups(
        groups, 10, true, cvssBands());

    assertThat(result.groups()).containsExactly(
        new RankedGroup("scored-low", 4.0f),
        new RankedGroup("scored-high", 8.0f),
        new RankedGroup("mixed-high", 9.5f),
        new RankedGroup("unscored", null));
  }

  @Test
  public void reduceGroups_descending_placesUnscoredLast() {
    List<GroupMax> groups = List.of(
        new GroupMax("scored-low", 4.0f),
        new GroupMax("unscored", Float.NaN),
        new GroupMax("scored-high", 8.0f),
        new GroupMax("mixed-high", 9.5f));

    RankedGroupsResult result = OpenSearchRankedGroupsAggregation.reduceGroups(
        groups, 10, false, cvssBands());

    assertThat(result.groups()).containsExactly(
        new RankedGroup("mixed-high", 9.5f),
        new RankedGroup("scored-high", 8.0f),
        new RankedGroup("scored-low", 4.0f),
        new RankedGroup("unscored", null));
  }

  @Test
  public void reduceGroups_bandsEachGroupOnceByMax() {
    List<GroupMax> groups = List.of(
        new GroupMax("scored-low", 4.0f),
        new GroupMax("scored-high", 8.0f),
        new GroupMax("mixed-high", 9.5f),
        new GroupMax("unscored", Float.NaN));

    RankedGroupsResult result = OpenSearchRankedGroupsAggregation.reduceGroups(
        groups, 10, false, cvssBands());

    assertThat(result.distinctGroupCount()).isEqualTo(4);
    assertThat(result.distinctGroupCountExact()).isTrue();
    assertThat(result.bandCounts()).containsOnly(
        Map.entry("medium", 1L),
        Map.entry("high", 1L),
        Map.entry("critical", 1L));
    assertThat(result.unbandedGroupCount()).isEqualTo(1);
  }

  @Test
  public void reduceGroups_bandCountsSumToDistinctMinusUnbanded() {
    List<GroupMax> groups = List.of(
        new GroupMax("a", 1.0f),
        new GroupMax("b", 5.5f),
        new GroupMax("c", 9.0f),
        new GroupMax("d", Float.NaN),
        new GroupMax("e", Float.NaN));

    RankedGroupsResult result = OpenSearchRankedGroupsAggregation.reduceGroups(
        groups, 10, false, cvssBands());

    long banded = result.bandCounts().values().stream().mapToLong(Long::longValue).sum();
    assertThat(banded + result.unbandedGroupCount()).isEqualTo(result.distinctGroupCount());
  }

  @Test
  public void reduceGroups_limit_truncatesResult() {
    List<GroupMax> groups = List.of(
        new GroupMax("a", 9.0f),
        new GroupMax("b", 8.0f),
        new GroupMax("c", 7.0f),
        new GroupMax("d", 6.0f),
        new GroupMax("e", 5.0f));

    RankedGroupsResult result = OpenSearchRankedGroupsAggregation.reduceGroups(
        groups, 3, false, cvssBands());

    assertThat(result.groups()).hasSize(3);
    assertThat(result.groups()).containsExactly(
        new RankedGroup("a", 9.0f),
        new RankedGroup("b", 8.0f),
        new RankedGroup("c", 7.0f));
    assertThat(result.distinctGroupCount()).isEqualTo(5);
  }

  @Test
  public void reduceGroups_tieBreakByGroupKeyAscending() {
    List<GroupMax> groups = List.of(
        new GroupMax("zeta", 7.0f),
        new GroupMax("alpha", 7.0f),
        new GroupMax("mu", 7.0f));

    RankedGroupsResult result = OpenSearchRankedGroupsAggregation.reduceGroups(
        groups, 10, true, cvssBands());

    assertThat(result.groups()).containsExactly(
        new RankedGroup("alpha", 7.0f),
        new RankedGroup("mu", 7.0f),
        new RankedGroup("zeta", 7.0f));
  }

  @Test
  public void reduceGroups_emptyInput_returnsEmpty() {
    RankedGroupsResult result = OpenSearchRankedGroupsAggregation.reduceGroups(
        List.of(), 10, false, cvssBands());

    assertThat(result.groups()).isEmpty();
    assertThat(result.distinctGroupCount()).isEqualTo(0);
    assertThat(result.distinctGroupCountExact()).isTrue();
    assertThat(result.unbandedGroupCount()).isEqualTo(0);
    assertThat(result.bandCounts()).containsOnlyKeys("medium", "high", "critical");
    assertThat(result.bandCounts().values()).allMatch(v -> v == 0L);
  }

  @Test
  public void reduceGroups_allUnscored_sortedByKeyAscending() {
    List<GroupMax> groups = List.of(
        new GroupMax("cherry", Float.NaN),
        new GroupMax("apple", Float.NaN),
        new GroupMax("banana", Float.NaN));

    RankedGroupsResult result = OpenSearchRankedGroupsAggregation.reduceGroups(
        groups, 10, true, cvssBands());

    assertThat(result.groups()).containsExactly(
        new RankedGroup("apple", null),
        new RankedGroup("banana", null),
        new RankedGroup("cherry", null));
    assertThat(result.unbandedGroupCount()).isEqualTo(3);
  }

  @Test
  public void reduceGroups_metricOutsideAllBands_countsAsUnbanded() {
    List<GroupMax> groups = List.of(
        new GroupMax("below-floor", 0.5f),
        new GroupMax("in-band", 5.0f));

    Map<String, float[]> narrowBands = new LinkedHashMap<>();
    narrowBands.put("only", new float[]{4.0f, 7.0f});

    RankedGroupsResult result = OpenSearchRankedGroupsAggregation.reduceGroups(
        groups, 10, false, narrowBands);

    assertThat(result.bandCounts().get("only")).isEqualTo(1L);
    assertThat(result.unbandedGroupCount()).isEqualTo(1L);
  }

  @Test
  public void reduceGroups_multipleUnscoredWithLimit_keepsTopNByKey() {
    List<GroupMax> groups = new ArrayList<>();
    groups.add(new GroupMax("scored", 5.0f));
    groups.add(new GroupMax("z-unscored", Float.NaN));
    groups.add(new GroupMax("a-unscored", Float.NaN));
    groups.add(new GroupMax("m-unscored", Float.NaN));

    RankedGroupsResult result = OpenSearchRankedGroupsAggregation.reduceGroups(
        groups, 3, true, cvssBands());

    assertThat(result.groups()).hasSize(3);
    assertThat(result.groups().get(0)).isEqualTo(new RankedGroup("scored", 5.0f));
    assertThat(result.groups().get(1)).isEqualTo(new RankedGroup("a-unscored", null));
    assertThat(result.groups().get(2)).isEqualTo(new RankedGroup("m-unscored", null));
  }

  @Test
  public void reduceGroups_bandBoundary_sevenIsHighNotMedium() {
    List<GroupMax> groups = List.of(new GroupMax("boundary", 7.0f));

    RankedGroupsResult result = OpenSearchRankedGroupsAggregation.reduceGroups(
        groups, 10, false, cvssBands());

    assertThat(result.bandCounts()).containsOnly(
        Map.entry("medium", 0L),
        Map.entry("high", 1L),
        Map.entry("critical", 0L));
    assertThat(result.unbandedGroupCount()).isEqualTo(0);
  }

  @Test
  public void reduceGroups_singleGroup_noEviction() {
    List<GroupMax> groups = List.of(new GroupMax("only", 8.5f));

    RankedGroupsResult result = OpenSearchRankedGroupsAggregation.reduceGroups(
        groups, 10, false, cvssBands());

    assertThat(result.groups()).containsExactly(new RankedGroup("only", 8.5f));
    assertThat(result.distinctGroupCount()).isEqualTo(1);
    assertThat(result.bandCounts().get("high")).isEqualTo(1L);
  }

  @Test
  public void reduceGroups_equalMetric_tieBreaksByUnsignedUtf8Key() {
    // "~" (0x7E) sorts before "à" (UTF-8 C3 A0) only under unsigned comparison — the signed
    // Arrays.compare path would invert them and diverge from Lucene BytesRef / term ordinals.
    List<GroupMax> groups = List.of(
        new GroupMax("\u00E0-group", 7.0f),
        new GroupMax("~-group", 7.0f));

    RankedGroupsResult result = OpenSearchRankedGroupsAggregation.reduceGroups(
        groups, 10, false, cvssBands());

    assertThat(result.groups()).containsExactly(
        new RankedGroup("~-group", 7.0f),
        new RankedGroup("\u00E0-group", 7.0f));
  }

  @Test
  public void reduceGroups_partialScan_marksDistinctCountInexact() {
    RankedGroupsResult result = OpenSearchRankedGroupsAggregation.reduceGroups(
        List.of(new GroupMax("a", 9.0f)), 10, false, cvssBands(), false);

    assertThat(result.distinctGroupCountExact()).isFalse();
    assertThat(result.distinctGroupCount()).isEqualTo(1L);
  }

  @Test
  public void execute_hitsMaxCompositePages_returnsInexactPartial() throws Exception {
    System.setProperty(OpenSearchRankedGroupsAggregation.PROP_MAX_COMPOSITE_PAGES, "2");
    System.setProperty(OpenSearchRankedGroupsAggregation.PROP_COMPOSITE_PAGE_SIZE, "2");
    try {
      SearchResponse<Map> page = fullPageResponse(List.of(
          stubBucket("g1", 9.0d),
          stubBucket("g2", 8.0d)));
      OpenSearchRankedGroupsAggregation.Searcher searcher = request -> page;

      RankedGroupsResult result = OpenSearchRankedGroupsAggregation.execute(
          searcher,
          () -> new org.opensearch.client.opensearch.core.SearchRequest.Builder(),
          "vulnerabilityId",
          "vulnerabilitySeverity",
          10,
          false,
          cvssBands());

      assertThat(result.distinctGroupCountExact()).isFalse();
      assertThat(result.distinctGroupCount()).isEqualTo(4L);
    }
    finally {
      System.clearProperty(OpenSearchRankedGroupsAggregation.PROP_MAX_COMPOSITE_PAGES);
      System.clearProperty(OpenSearchRankedGroupsAggregation.PROP_COMPOSITE_PAGE_SIZE);
    }
  }

  @SuppressWarnings("unchecked")
  private static SearchResponse<Map> fullPageResponse(final List<CompositeBucket> buckets) {
    Buckets<CompositeBucket> bucketList = mock(Buckets.class);
    when(bucketList.array()).thenReturn(buckets);

    JsonData after = mock(JsonData.class);
    when(after.to(String.class)).thenReturn("cursor");

    CompositeAggregate composite = mock(CompositeAggregate.class);
    when(composite.buckets()).thenReturn(bucketList);
    when(composite.afterKey())
        .thenReturn(Map.of(OpenSearchRankedGroupsAggregation.COMPOSITE_SOURCE_GROUP, after));

    Aggregate ranked = mock(Aggregate.class);
    when(ranked.isComposite()).thenReturn(true);
    when(ranked.composite()).thenReturn(composite);

    SearchResponse<Map> response = mock(SearchResponse.class);
    when(response.aggregations())
        .thenReturn(Map.of(OpenSearchRankedGroupsAggregation.AGG_RANKED, ranked));
    return response;
  }

  private static CompositeBucket stubBucket(final String key, final double max) {
    MaxAggregate maxAgg = mock(MaxAggregate.class);
    when(maxAgg.value()).thenReturn(max);
    Aggregate metric = mock(Aggregate.class);
    when(metric.isMax()).thenReturn(true);
    when(metric.max()).thenReturn(maxAgg);

    JsonData groupData = mock(JsonData.class);
    when(groupData.to(String.class)).thenReturn(key);

    CompositeBucket bucket = mock(CompositeBucket.class);
    when(bucket.key()).thenReturn(Map.of(OpenSearchRankedGroupsAggregation.COMPOSITE_SOURCE_GROUP, groupData));
    when(bucket.aggregations()).thenReturn(Map.of(OpenSearchRankedGroupsAggregation.SUB_AGG_MAX, metric));
    return bucket;
  }

  private static Map<String, float[]> cvssBands() {
    Map<String, float[]> bands = new LinkedHashMap<>();
    bands.put("medium", new float[]{4.0f, 7.0f});
    bands.put("high", new float[]{7.0f, 9.0f});
    bands.put("critical", new float[]{9.0f, 10.1f});
    return bands;
  }
}
