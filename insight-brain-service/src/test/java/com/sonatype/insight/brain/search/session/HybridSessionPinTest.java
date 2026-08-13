/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.session;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.RankedGroup;
import com.sonatype.insight.brain.search.index.RankedGroupsResult;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.junit.Test;

import static org.apache.lucene.document.Field.Store.YES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HybridSessionPinTest
{
  @Test
  public void delegatesSuccessfulReadsToPinnedBackend() {
    IndexReadSession pinnedLucene = new StubSession("lucene");
    IndexReadSession session = new HybridIndexReadSession(pinnedLucene);

    IndexPageResult page = session.searchPage(new IndexPageRequest(new MatchAllDocsQuery(), null, 10, List.of()));

    assertThat(session.backendId()).isEqualTo("lucene");
    assertThat(page.docs()).extracting(document -> document.get("value")).containsExactly("one");
    assertThat(session.count(new MatchAllDocsQuery())).isEqualTo(1L);
    assertThat(session.termsAggregation(new MatchAllDocsQuery(), "category", 10))
        .containsExactly(new IndexTermsBucket("alpha", 1L));
  }

  @Test
  public void delegatesRankGroupsByMaxMetricToPinnedBackend() {
    RankedGroupsResult sentinel = new RankedGroupsResult(
        List.of(new RankedGroup("group1", 9.5f)), 1L, true, Map.of("high", 1L), 0L);
    IndexReadSession pinnedLucene = new StubSession("lucene")
    {
      @Override
      public RankedGroupsResult rankGroupsByMaxMetric(
          final Query query,
          final String groupField,
          final String metricField,
          final int limit,
          final boolean ascending,
          final Map<String, float[]> metricBands)
      {
        return sentinel;
      }
    };
    IndexReadSession session = new HybridIndexReadSession(pinnedLucene);

    Map<String, float[]> bands = new LinkedHashMap<>();
    bands.put("high", new float[]{7.0f, 10.0f});
    RankedGroupsResult result = session.rankGroupsByMaxMetric(
        new MatchAllDocsQuery(), "componentHash", "score", 10, false, bands);

    assertThat(result).isSameAs(sentinel);
  }

  @Test
  public void delegatesSumGroupedByToPinnedBackend() {
    Map<String, Long> sentinel = Map.of("hasha", 42L);
    IndexReadSession pinnedLucene = new StubSession("lucene")
    {
      @Override
      public Map<String, Long> sumGroupedBy(
          final Query query,
          final String groupField,
          final String sumField,
          final Collection<String> groupValues)
      {
        return sentinel;
      }
    };
    IndexReadSession session = new HybridIndexReadSession(pinnedLucene);

    Map<String, Long> result = session.sumGroupedBy(
        new MatchAllDocsQuery(), "componentHash", "threatLevel", List.of("hashA"));

    assertThat(result).isSameAs(sentinel);
  }

  @Test
  public void delegatesRemainingAggregationPrimitivesToPinnedBackend() {
    MetricAggregationResult intBands = new MetricAggregationResult(3L, Map.of("critical", 1L));
    MetricAggregationResult floatBands = new MetricAggregationResult(4L, Map.of("high", 2L));
    Map<String, Long> distinct = Map.of("hasha", 2L);
    Map<String, Map<String, Long>> distinctBands = Map.of("hasha", Map.of("critical", 1L));
    Map<String, Map<String, Long>> sumBands = Map.of("hasha", Map.of("critical", 10L));

    IndexReadSession pinnedLucene = new StubSession("lucene")
    {
      @Override
      public MetricAggregationResult aggregateCountByField(
          final Query query,
          final String bucketField,
          final Map<String, int[]> ranges)
      {
        return intBands;
      }

      @Override
      public MetricAggregationResult aggregateCountByFloatField(
          final Query query,
          final String bucketField,
          final Map<String, float[]> ranges,
          final String distinctField)
      {
        return floatBands;
      }

      @Override
      public Map<String, Long> countDistinctGroupedBy(
          final Query query,
          final String groupField,
          final String distinctField,
          final Collection<String> groupValues)
      {
        return distinct;
      }

      @Override
      public Map<String, Map<String, Long>> countDistinctGroupedByBands(
          final Query query,
          final String groupField,
          final String distinctField,
          final Collection<String> groupValues,
          final String bandField,
          final Map<String, int[]> bands)
      {
        return distinctBands;
      }

      @Override
      public Map<String, Map<String, Long>> sumGroupedByBands(
          final Query query,
          final String groupField,
          final String sumField,
          final Collection<String> groupValues,
          final String bandField,
          final Map<String, int[]> bands)
      {
        return sumBands;
      }
    };
    IndexReadSession session = new HybridIndexReadSession(pinnedLucene);

    assertThat(session.aggregateCountByField(new MatchAllDocsQuery(), "threat", Map.of("critical", new int[]{9, 10})))
        .isSameAs(intBands);
    assertThat(session.aggregateCountByFloatField(
        new MatchAllDocsQuery(), "severity", Map.of("high", new float[]{7f, 9f}), "cve"))
            .isSameAs(floatBands);
    assertThat(session.countDistinctGroupedBy(
        new MatchAllDocsQuery(), "componentHash", "applicationId", List.of("hashA")))
            .isSameAs(distinct);
    assertThat(session.countDistinctGroupedByBands(
        new MatchAllDocsQuery(), "componentHash", "applicationId", List.of("hashA"), "threat",
        Map.of("critical", new int[]{9, 10})))
            .isSameAs(distinctBands);
    assertThat(session.sumGroupedByBands(
        new MatchAllDocsQuery(), "componentHash", "threat", List.of("hashA"), "threat",
        Map.of("critical", new int[]{9, 10})))
            .isSameAs(sumBands);
  }

  @Test
  public void searchPage_rejectsCursorFromOtherBackend() {
    IndexReadSession pinnedOpenSearch = new StubSession("opensearch");
    IndexReadSession session = new HybridIndexReadSession(pinnedOpenSearch);

    IndexPageRequest request = new IndexPageRequest(
        new MatchAllDocsQuery(),
        null,
        10,
        IndexSessionCursors.encode("lucene", List.of(1, 1.0F)));

    assertThatThrownBy(() -> session.searchPage(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("lucene")
        .hasMessageContaining("opensearch");
  }

  private static class StubSession
      implements IndexReadSession
  {
    private final String backendId;

    StubSession(final String backendId) {
      this.backendId = backendId;
    }

    @Override
    public String backendId() {
      return backendId;
    }

    @Override
    public Instant lastUpdatedAt() {
      return Instant.EPOCH;
    }

    @Override
    public String snapshotHandle() {
      return "stub";
    }

    @Override
    public IndexPageResult searchPage(final IndexPageRequest request) {
      return new IndexPageResult(List.of(document()), List.of(), false);
    }

    @Override
    public long count(final Query query) {
      return 1;
    }

    @Override
    public List<IndexTermsBucket> termsAggregation(final Query query, final String field, final int maxBuckets) {
      return List.of(new IndexTermsBucket("alpha", 1L));
    }

    @Override
    public RankedGroupsResult rankGroupsByMaxMetric(
        final Query query,
        final String groupField,
        final String metricField,
        final int limit,
        final boolean ascending,
        final Map<String, float[]> metricBands)
    {
      throw new UnsupportedOperationException("stub");
    }

    @Override
    public MetricAggregationResult aggregateCountByField(
        final Query query,
        final String bucketField,
        final Map<String, int[]> ranges)
    {
      throw new UnsupportedOperationException("stub");
    }

    @Override
    public MetricAggregationResult aggregateCountByFloatField(
        final Query query,
        final String bucketField,
        final Map<String, float[]> ranges,
        final String distinctField)
    {
      throw new UnsupportedOperationException("stub");
    }

    @Override
    public Map<String, Long> countDistinctGroupedBy(
        final Query query,
        final String groupField,
        final String distinctField,
        final Collection<String> groupValues)
    {
      throw new UnsupportedOperationException("stub");
    }

    @Override
    public Map<String, Map<String, Long>> countDistinctGroupedByBands(
        final Query query,
        final String groupField,
        final String distinctField,
        final Collection<String> groupValues,
        final String bandField,
        final Map<String, int[]> bands)
    {
      throw new UnsupportedOperationException("stub");
    }

    @Override
    public Map<String, Long> sumGroupedBy(
        final Query query,
        final String groupField,
        final String sumField,
        final Collection<String> groupValues)
    {
      throw new UnsupportedOperationException("stub");
    }

    @Override
    public Map<String, Map<String, Long>> sumGroupedByBands(
        final Query query,
        final String groupField,
        final String sumField,
        final Collection<String> groupValues,
        final String bandField,
        final Map<String, int[]> bands)
    {
      throw new UnsupportedOperationException("stub");
    }

    @Override
    public void close() {
    }

    private Document document() {
      Document document = new Document();
      document.add(new StringField("value", "one", YES));
      document.add(new StringField("category", "alpha", YES));
      return document;
    }
  }
}
