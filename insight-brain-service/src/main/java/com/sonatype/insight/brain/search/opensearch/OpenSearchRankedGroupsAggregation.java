/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.sonatype.insight.brain.search.index.RankedGroup;
import com.sonatype.insight.brain.search.index.RankedGroupsRanking;
import com.sonatype.insight.brain.search.index.RankedGroupsResult;
import com.sonatype.insight.brain.search.index.SearchIndexException;

import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.CompositeAggregationSource;
import org.opensearch.client.opensearch._types.aggregations.CompositeBucket;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared helper that ranks groups by per-group maximum metric using a paged composite aggregation,
 * matching the Lucene {@code RankedGroupsReduction} semantics via {@link RankedGroupsRanking}:
 * <ul>
 * <li>Unscored groups sort last regardless of ascending/descending direction</li>
 * <li>Each group contributes to at most one band via its max metric (half-open {@code [min, max)})</li>
 * <li>Tie-break: group key ascending in unsigned UTF-8 byte order via {@link RankedGroupsRanking}
 * (keyword values are lowercased by the normalizer; matches Lucene {@code BytesRef} / term-ordinal
 * order, including non-ASCII)</li>
 * <li>Collects all distinct groups via paged composite aggregation ({@code O(cardinality)} memory by
 * design — callers that only need a top-N still pay a full scan). Soft-warns at
 * {@link #collectedGroupsWarnThreshold()} and stops (graceful degrade, inexact result) after
 * {@link #maxCompositePages()}</li>
 * </ul>
 * <p>
 * OpenSearch uses {@code -1.0} as an internal unscored sentinel on the max sub-aggregation
 * ({@code missing}); any max {@code <= -1.0} is treated as unscored. Production metric fields
 * (e.g. CVSS) are non-negative — see {@code IndexReadSession} Javadoc. The Java client deserializes
 * JSON {@code null} max values as {@code 0.0}, so the sentinel check is the production contract.
 * <p>
 * Limits are tunable per cell via system properties (defaults match prior compile-time constants):
 * {@code nexusOne.rankedGroups.compositePageSize}, {@code nexusOne.rankedGroups.maxCompositePages},
 * {@code nexusOne.rankedGroups.collectedGroupsWarnThreshold}.
 */
final class OpenSearchRankedGroupsAggregation
{
  private static final Logger log = LoggerFactory.getLogger(OpenSearchRankedGroupsAggregation.class);

  static final String PROP_COMPOSITE_PAGE_SIZE = "nexusOne.rankedGroups.compositePageSize";

  static final String PROP_MAX_COMPOSITE_PAGES = "nexusOne.rankedGroups.maxCompositePages";

  static final String PROP_COLLECTED_GROUPS_WARN = "nexusOne.rankedGroups.collectedGroupsWarnThreshold";

  private static final int DEFAULT_COMPOSITE_PAGE_SIZE = 10_000;

  private static final int DEFAULT_MAX_COMPOSITE_PAGES = 100;

  private static final int DEFAULT_COLLECTED_GROUPS_WARN = 50_000;

  /**
   * OpenSearch max-agg missing value and unscored threshold. Values at or below this become
   * {@link Float#NaN} (unscored). Not a valid production CVSS / severity score.
   */
  private static final double UNSCORED_METRIC = -1.0d;

  static final String AGG_RANKED = "rankedComposite";

  static final String SUB_AGG_MAX = "groupMetric";

  static final String COMPOSITE_SOURCE_GROUP = "group";

  private OpenSearchRankedGroupsAggregation() {
  }

  /** Composite page size; tests assert against this for after-key round-trips. */
  static int compositePageSize() {
    return positiveInt(PROP_COMPOSITE_PAGE_SIZE, DEFAULT_COMPOSITE_PAGE_SIZE);
  }

  /** Soft warn when collected distinct groups exceed this. */
  static int collectedGroupsWarnThreshold() {
    return positiveInt(PROP_COLLECTED_GROUPS_WARN, DEFAULT_COLLECTED_GROUPS_WARN);
  }

  /**
   * Ceiling on composite pages. At default page size this is one million distinct groups — beyond
   * that the scan stops and returns an inexact partial result rather than failing the dashboard.
   */
  static int maxCompositePages() {
    return positiveInt(PROP_MAX_COMPOSITE_PAGES, DEFAULT_MAX_COMPOSITE_PAGES);
  }

  private static int positiveInt(final String property, final int defaultValue) {
    int configured = Integer.getInteger(property, defaultValue);
    return configured > 0 ? configured : defaultValue;
  }

  /**
   * Executes a paged composite aggregation to enumerate all groups with their per-group max metric,
   * then sorts and bands them in Java to match Lucene's ranked-groups contract.
   *
   * @param searcher executes a search request and returns the response; caller controls index/PIT
   * @param requestFactory creates a fresh SearchRequest.Builder pre-configured with index or PIT and
   *          query; called once per composite page since builders are single-use after build()
   * @param groupField the keyword field to group on (already resolved to its aggregation-ready label)
   * @param metricField the numeric field whose per-group max determines rank and band
   * @param limit maximum number of groups to return
   * @param ascending true for lowest-metric-first ordering
   * @param metricBands half-open {@code [min, max)} bands for severity classification
   * @return ranked groups result with band counts derived from per-group max
   */
  static RankedGroupsResult execute(
      final Searcher searcher,
      final RequestFactory requestFactory,
      final String groupField,
      final String metricField,
      final int limit,
      final boolean ascending,
      final Map<String, float[]> metricBands) throws IOException
  {
    long startedNanos = System.nanoTime();
    CollectionOutcome collected = collectAllGroups(searcher, requestFactory, groupField, metricField);
    RankedGroupsResult result = reduceGroups(
        collected.groups(), limit, ascending, metricBands, collected.complete());
    long durationMs = (System.nanoTime() - startedNanos) / 1_000_000L;
    log.info(
        "OpenSearch ranked-groups composite scan finished: pages={} groups={} complete={} durationMs={}",
        collected.pages(), collected.groups().size(), collected.complete(), durationMs);
    return result;
  }

  /**
   * Pure reduction: given a list of (group, maxMetric) pairs, produces the ranked/banded result.
   * Extracted for unit testing without an OpenSearch cluster.
   */
  static RankedGroupsResult reduceGroups(
      final List<GroupMax> allGroups,
      final int limit,
      final boolean ascending,
      final Map<String, float[]> metricBands)
  {
    return reduceGroups(allGroups, limit, ascending, metricBands, true);
  }

  static RankedGroupsResult reduceGroups(
      final List<GroupMax> allGroups,
      final int limit,
      final boolean ascending,
      final Map<String, float[]> metricBands,
      final boolean exact)
  {
    Map<String, Long> bandCounts = new LinkedHashMap<>();
    metricBands.keySet().forEach(band -> bandCounts.put(band, 0L));

    Comparator<GroupMax> worstFirst = rankedGroupComparator(ascending).reversed();
    // Match Lucene RankedGroupsReduction: default capacity, grow as needed — do not allocate to
    // caller-supplied limit up front (IndexReadSession limit is uncapped).
    PriorityQueue<GroupMax> heap = new PriorityQueue<>(worstFirst);

    long distinct = 0;
    long unbanded = 0;

    for (GroupMax group : allGroups) {
      distinct++;
      String band = RankedGroupsRanking.bandFor(metricBands, group.maxMetric);
      if (band == null) {
        unbanded++;
      }
      else {
        bandCounts.merge(band, 1L, Long::sum);
      }
      heap.add(group);
      if (heap.size() > limit) {
        heap.poll();
      }
    }

    List<GroupMax> ordered = new ArrayList<>(heap);
    ordered.sort(rankedGroupComparator(ascending));
    List<RankedGroup> groups = new ArrayList<>(ordered.size());
    for (GroupMax gm : ordered) {
      groups.add(new RankedGroup(gm.groupKey, Float.isNaN(gm.maxMetric) ? null : gm.maxMetric));
    }

    return new RankedGroupsResult(groups, distinct, exact, bandCounts, unbanded);
  }

  private static CollectionOutcome collectAllGroups(
      final Searcher searcher,
      final RequestFactory requestFactory,
      final String groupField,
      final String metricField) throws IOException
  {
    List<GroupMax> allGroups = new ArrayList<>();
    Map<String, String> afterKey = null;
    boolean warned = false;
    int pages = 0;
    int pageSize = compositePageSize();
    int maxPages = maxCompositePages();
    int warnThreshold = collectedGroupsWarnThreshold();

    while (true) {
      if (pages >= maxPages) {
        log.error(
            "OpenSearch ranked-groups composite scan hit maxCompositePages={} (pageSize={}); "
                + "returning partial inexact result ({} groups). Raise {} to scan further.",
            maxPages, pageSize, allGroups.size(), PROP_MAX_COMPOSITE_PAGES);
        return new CollectionOutcome(allGroups, false, pages);
      }
      pages++;

      SearchRequest request = requestFactory.create()
          .size(0)
          .aggregations(AGG_RANKED, buildCompositeAggregation(groupField, metricField, afterKey, pageSize))
          .build();

      SearchResponse<Map> response = searcher.search(request);

      Map<String, Aggregate> aggs = response.aggregations();
      if (aggs == null) {
        throw new SearchIndexException(
            "OpenSearch ranked-groups response missing aggregations", null);
      }
      Aggregate compositeAgg = aggs.get(AGG_RANKED);
      if (compositeAgg == null || !compositeAgg.isComposite()) {
        throw new SearchIndexException(
            "OpenSearch ranked-groups response missing composite aggregation '" + AGG_RANKED + "'",
            null);
      }

      List<CompositeBucket> buckets = compositeAgg.composite().buckets().array();
      if (buckets.isEmpty()) {
        break;
      }

      for (CompositeBucket bucket : buckets) {
        String groupKey = extractGroupKey(bucket);
        float maxMetric = extractMaxMetric(bucket);
        allGroups.add(new GroupMax(groupKey, maxMetric));
      }

      if (!warned && allGroups.size() >= warnThreshold) {
        warned = true;
        log.warn(
            "OpenSearch ranked-groups composite scan collected {} distinct groups (warn threshold {}); "
                + "full scan cost is independent of the requested limit",
            allGroups.size(), warnThreshold);
      }

      // A short page is the last page — avoid a wasted empty terminator round-trip and keep the
      // maxPages ceiling meaning "one million groups" rather than failing at exactly that size.
      if (buckets.size() < pageSize) {
        break;
      }

      Map<String, JsonData> responseAfterKey = compositeAgg.composite().afterKey();
      if (responseAfterKey == null || responseAfterKey.isEmpty()) {
        break;
      }
      afterKey = toStringMap(responseAfterKey);
    }

    return new CollectionOutcome(allGroups, true, pages);
  }

  private static Aggregation buildCompositeAggregation(
      final String groupField,
      final String metricField,
      final Map<String, String> afterKey,
      final int pageSize)
  {
    CompositeAggregationSource source = CompositeAggregationSource.of(s -> s
        .terms(t -> t.field(groupField)));

    return Aggregation.of(a -> a
        .composite(c -> {
          c.size(pageSize);
          c.sources(List.of(Map.of(COMPOSITE_SOURCE_GROUP, source)));
          if (afterKey != null) {
            c.after(afterKey);
          }
          return c;
        })
        .aggregations(SUB_AGG_MAX, Aggregation.of(sub -> sub
            .max(m -> m.field(metricField).missing(FieldValue.of(UNSCORED_METRIC))))));
  }

  private static Map<String, String> toStringMap(final Map<String, JsonData> jsonDataMap) {
    Map<String, String> result = new LinkedHashMap<>();
    for (Map.Entry<String, JsonData> entry : jsonDataMap.entrySet()) {
      JsonData value = entry.getValue();
      result.put(entry.getKey(), value.to(String.class));
    }
    return result;
  }

  private static String extractGroupKey(final CompositeBucket bucket) {
    Map<String, JsonData> key = bucket.key();
    JsonData groupValue = key.get(COMPOSITE_SOURCE_GROUP);
    if (groupValue == null) {
      throw new SearchIndexException(
          "OpenSearch ranked-groups composite bucket missing source '" + COMPOSITE_SOURCE_GROUP + "'",
          null);
    }
    return groupValue.to(String.class);
  }

  private static float extractMaxMetric(final CompositeBucket bucket) {
    Aggregate metric = bucket.aggregations().get(SUB_AGG_MAX);
    if (metric == null || !metric.isMax()) {
      throw new SearchIndexException(
          "OpenSearch ranked-groups composite bucket missing max sub-aggregation '" + SUB_AGG_MAX + "'",
          null);
    }
    double value = metric.max().value();
    // Production contract: missing metrics arrive via the -1.0 sentinel (JSON null deserializes as 0.0).
    if (value <= UNSCORED_METRIC) {
      return Float.NaN;
    }
    return (float) value;
  }

  /** Best-first: metric-less groups always last, then metric by direction, then key ascending. */
  private static Comparator<GroupMax> rankedGroupComparator(final boolean ascending) {
    return (left, right) -> RankedGroupsRanking.compareMetricThenKey(
        left.maxMetric, left.groupKey, right.maxMetric, right.groupKey, ascending);
  }

  record GroupMax(String groupKey, float maxMetric)
  {
  }

  private record CollectionOutcome(List<GroupMax> groups, boolean complete, int pages)
  {
  }

  @FunctionalInterface
  interface Searcher
  {
    @SuppressWarnings("rawtypes")
    SearchResponse<Map> search(SearchRequest request) throws IOException;
  }

  @FunctionalInterface
  interface RequestFactory
  {
    SearchRequest.Builder create();
  }
}
