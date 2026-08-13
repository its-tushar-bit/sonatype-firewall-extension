/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.search.index.RankedGroup;

import org.apache.commons.lang3.StringUtils;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.AggregationRange;
import org.opensearch.client.opensearch._types.aggregations.RangeBucket;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;

/**
 * Reusable helpers for building and parsing OpenSearch aggregation requests/responses
 * in session-scoped (PIT-based) queries.
 */
class OpenSearchSessionAggregations
{
  static final int PRECISION_THRESHOLD = 40_000;

  private OpenSearchSessionAggregations() {
  }

  /**
   * Lowercases the provided group values and returns the unique set plus the include-terms list.
   * Null/blank elements are skipped (Lucene collectors use the same blank filter). Returns null if
   * the input is null/empty or contains only blank values (caller should short-circuit with an empty
   * map).
   */
  static TermsInclude prepareIncludeTerms(final Collection<String> groupValues) {
    if (groupValues == null || groupValues.isEmpty()) {
      return null;
    }
    Set<String> requested = new LinkedHashSet<>();
    for (String groupValue : groupValues) {
      if (StringUtils.isBlank(groupValue)) {
        continue;
      }
      requested.add(groupValue.toLowerCase(Locale.ROOT));
    }
    if (requested.isEmpty()) {
      return null;
    }
    return new TermsInclude(requested, new ArrayList<>(requested));
  }

  /**
   * Builds a terms aggregation with a cardinality sub-aggregation for counting distinct values.
   */
  static Aggregation buildTermsWithCardinality(
      final String groupField,
      final String distinctField,
      final List<String> includeTerms)
  {
    return Aggregation.of(a -> a
        .terms(t -> t.field(groupField)
            .size(includeTerms.size())
            .include(ti -> ti.terms(includeTerms)))
        .aggregations("distinct", sub -> sub
            .cardinality(c -> c.field(distinctField).precisionThreshold(PRECISION_THRESHOLD))));
  }

  /**
   * Builds a terms aggregation with a sum sub-aggregation.
   */
  static Aggregation buildTermsWithSum(
      final String groupField,
      final String sumField,
      final List<String> includeTerms)
  {
    return Aggregation.of(a -> a
        .terms(t -> t.field(groupField)
            .size(includeTerms.size())
            .include(ti -> ti.terms(includeTerms)))
        .aggregations("total", sub -> sub
            .sum(s -> s.field(sumField))));
  }

  /**
   * Groups that have at least one document carrying {@code metricField}, ranked by max metric.
   * Omitting {@code missing} keeps mixed scored/unscored groups honest (a high sentinel would win
   * {@code max} and corrupt them). Unscored-only groups are collected separately via
   * {@link #buildUnscoredRankedGroupsAggregation}.
   */
  static Aggregation buildScoredRankedGroupsAggregation(
      final String groupField,
      final String metricField,
      final int limit,
      final int shardSize,
      final SortOrder metricOrder)
  {
    return Aggregation.of(a -> a
        .filter(f -> f.exists(e -> e.field(metricField)))
        .aggregations("groups", g -> g
            .terms(t -> t
                .field(groupField)
                .size(limit)
                .shardSize(shardSize)
                .order(Map.of("groupMetric", metricOrder), Map.of("_key", SortOrder.Asc)))
            .aggregations("groupMetric", sub -> sub.max(m -> m.field(metricField)))));
  }

  /**
   * Groups whose matching documents never carry {@code metricField}. Used to pad the ranked page so
   * unscored groups sort last regardless of ascending/descending, without a missing sentinel on
   * {@code max}.
   */
  static Aggregation buildUnscoredRankedGroupsAggregation(
      final String groupField,
      final String metricField,
      final int limit)
  {
    return Aggregation.of(a -> a
        .filter(f -> f.bool(b -> b.mustNot(mn -> mn.exists(e -> e.field(metricField)))))
        .aggregations("groups", g -> g
            .terms(t -> t
                .field(groupField)
                .size(limit)
                .order(Map.of("_key", SortOrder.Asc)))));
  }

  /**
   * Scored groups first (already ordered by the terms agg), then unscored-only groups until
   * {@code limit}. One search round-trip; no composite paging.
   */
  static List<RankedGroup> parseRankedGroups(
      final Map<String, Aggregate> aggregations,
      final int limit)
  {
    List<RankedGroup> groups = new ArrayList<>();
    if (limit <= 0 || aggregations == null) {
      return groups;
    }
    Set<String> seen = new LinkedHashSet<>();
    appendScoredRankedGroups(aggregations.get("scoredGroups"), groups, seen, limit);
    if (groups.size() < limit) {
      appendUnscoredRankedGroups(aggregations.get("unscoredGroups"), groups, seen, limit);
    }
    return groups;
  }

  private static void appendScoredRankedGroups(
      final Aggregate scoredFilter,
      final List<RankedGroup> groups,
      final Set<String> seen,
      final int limit)
  {
    Aggregate terms = nestedGroups(scoredFilter);
    if (terms == null || !terms.isSterms()) {
      return;
    }
    for (StringTermsBucket bucket : terms.sterms().buckets().array()) {
      if (groups.size() >= limit) {
        return;
      }
      String key = bucket.key();
      if (key == null || !seen.add(key)) {
        continue;
      }
      Float metricValue = null;
      Aggregate metric = bucket.aggregations() == null ? null : bucket.aggregations().get("groupMetric");
      if (metric != null && metric.isMax()) {
        double value = metric.max().value();
        if (!Double.isNaN(value)) {
          metricValue = (float) value;
        }
      }
      groups.add(new RankedGroup(key, metricValue));
    }
  }

  private static void appendUnscoredRankedGroups(
      final Aggregate unscoredFilter,
      final List<RankedGroup> groups,
      final Set<String> seen,
      final int limit)
  {
    Aggregate terms = nestedGroups(unscoredFilter);
    if (terms == null || !terms.isSterms()) {
      return;
    }
    for (StringTermsBucket bucket : terms.sterms().buckets().array()) {
      if (groups.size() >= limit) {
        return;
      }
      String key = bucket.key();
      if (key == null || !seen.add(key)) {
        continue;
      }
      groups.add(new RankedGroup(key, null));
    }
  }

  private static Aggregate nestedGroups(final Aggregate filterAgg) {
    if (filterAgg == null || !filterAgg.isFilter() || filterAgg.filter().aggregations() == null) {
      return null;
    }
    return filterAgg.filter().aggregations().get("groups");
  }

  /**
   * Builds integer range aggregation ranges from inclusive-upper int bands.
   * Adds 1 to the upper bound (long arithmetic) for OpenSearch's exclusive upper.
   * When the upper bound is {@link Integer#MAX_VALUE}, omits the {@code to} clause so the range is
   * unbounded above — adding 1 would overflow the int domain and OpenSearch would silently exclude
   * the MAX_VALUE documents.
   */
  static List<AggregationRange> buildIntRanges(final Map<String, int[]> bands) {
    List<AggregationRange> ranges = new ArrayList<>();
    bands.forEach((label, bounds) -> ranges.add(AggregationRange.of(r -> {
      r.key(label).from(String.valueOf(bounds[0]));
      if (bounds[1] != Integer.MAX_VALUE) {
        r.to(String.valueOf((long) bounds[1] + 1));
      }
      return r;
    })));
    return ranges;
  }

  /**
   * Builds float range aggregation ranges (half-open, bounds pass through verbatim).
   */
  static List<AggregationRange> buildFloatRanges(final Map<String, float[]> ranges) {
    List<AggregationRange> result = new ArrayList<>();
    ranges.forEach((label, bounds) -> result.add(AggregationRange.of(r -> r
        .key(label)
        .from(String.valueOf(bounds[0]))
        .to(String.valueOf(bounds[1])))));
    return result;
  }

  /**
   * Parses a terms+cardinality aggregation response into a map of group-key to distinct count.
   * Only includes entries where the key is in the requested set and the count is &gt; 0.
   */
  static Map<String, Long> parseTermsCardinality(
      final Map<String, Aggregate> aggregations,
      final String aggName,
      final Set<String> requestedKeys)
  {
    if (aggregations == null) {
      return Map.of();
    }
    Aggregate aggregate = aggregations.get(aggName);
    if (aggregate == null || !aggregate.isSterms()) {
      return Map.of();
    }
    Map<String, Long> counts = new LinkedHashMap<>();
    for (StringTermsBucket bucket : aggregate.sterms().buckets().array()) {
      String key = bucket.key();
      if (!requestedKeys.contains(key)) {
        continue;
      }
      Aggregate distinct = bucket.aggregations() == null ? null : bucket.aggregations().get("distinct");
      long value = distinct != null && distinct.isCardinality() ? distinct.cardinality().value() : 0L;
      if (value > 0) {
        counts.put(key, value);
      }
    }
    return counts;
  }

  /**
   * Parses a terms+sum aggregation response into a map of group-key to sum.
   * Only includes entries where the key is in the requested set and the sum is non-zero
   * (negative sums are kept, matching the Lucene backend for signed sum fields).
   */
  static Map<String, Long> parseTermsSum(
      final Map<String, Aggregate> aggregations,
      final String aggName,
      final Set<String> requestedKeys)
  {
    if (aggregations == null) {
      return Map.of();
    }
    Aggregate aggregate = aggregations.get(aggName);
    if (aggregate == null || !aggregate.isSterms()) {
      return Map.of();
    }
    Map<String, Long> sums = new LinkedHashMap<>();
    for (StringTermsBucket bucket : aggregate.sterms().buckets().array()) {
      String key = bucket.key();
      if (!requestedKeys.contains(key)) {
        continue;
      }
      Aggregate sumAgg = bucket.aggregations() == null ? null : bucket.aggregations().get("total");
      // Cast to long is safe for integral sumFields; requireIntegralSumField rejects known float
      // fields, so OpenSearch returns an exact integer sum (no fractional part to truncate).
      long value = sumAgg != null && sumAgg.isSum() ? (long) sumAgg.sum().value() : 0L;
      if (value != 0) {
        sums.put(key, value);
      }
    }
    return sums;
  }

  /**
   * Parses range→terms→cardinality nested aggregation into group→band→count map.
   */
  static Map<String, Map<String, Long>> parseRangeTermsCardinality(
      final Map<String, Aggregate> aggregations,
      final String rangeAggName,
      final Set<String> requestedKeys)
  {
    if (aggregations == null) {
      return Map.of();
    }
    Aggregate bandsAgg = aggregations.get(rangeAggName);
    if (bandsAgg == null || !bandsAgg.isRange()) {
      return Map.of();
    }
    Map<String, Map<String, Long>> byGroup = new LinkedHashMap<>();
    for (RangeBucket bandBucket : bandsAgg.range().buckets().array()) {
      String bandLabel = bandBucket.key();
      if (bandLabel == null) {
        continue;
      }
      Aggregate groupsAgg =
          bandBucket.aggregations() == null ? null : bandBucket.aggregations().get("groups");
      if (groupsAgg == null || !groupsAgg.isSterms()) {
        continue;
      }
      for (StringTermsBucket groupBucket : groupsAgg.sterms().buckets().array()) {
        String group = groupBucket.key();
        if (!requestedKeys.contains(group)) {
          continue;
        }
        Aggregate distinct =
            groupBucket.aggregations() == null ? null : groupBucket.aggregations().get("distinct");
        long value = distinct != null && distinct.isCardinality() ? distinct.cardinality().value() : 0L;
        if (value > 0) {
          byGroup.computeIfAbsent(group, k -> new LinkedHashMap<>()).put(bandLabel, value);
        }
      }
    }
    return byGroup;
  }

  /**
   * Parses range→terms→sum nested aggregation into group→band→sum map.
   * Sums that are non-zero are kept (negative sums included), matching the Lucene backend
   * for signed sum fields.
   */
  static Map<String, Map<String, Long>> parseRangeTermsSum(
      final Map<String, Aggregate> aggregations,
      final String rangeAggName,
      final Set<String> requestedKeys)
  {
    if (aggregations == null) {
      return Map.of();
    }
    Aggregate bandsAgg = aggregations.get(rangeAggName);
    if (bandsAgg == null || !bandsAgg.isRange()) {
      return Map.of();
    }
    Map<String, Map<String, Long>> byGroup = new LinkedHashMap<>();
    for (RangeBucket bandBucket : bandsAgg.range().buckets().array()) {
      String bandLabel = bandBucket.key();
      if (bandLabel == null) {
        continue;
      }
      Aggregate groupsAgg =
          bandBucket.aggregations() == null ? null : bandBucket.aggregations().get("groups");
      if (groupsAgg == null || !groupsAgg.isSterms()) {
        continue;
      }
      for (StringTermsBucket groupBucket : groupsAgg.sterms().buckets().array()) {
        String group = groupBucket.key();
        if (!requestedKeys.contains(group)) {
          continue;
        }
        Aggregate sumAgg =
            groupBucket.aggregations() == null ? null : groupBucket.aggregations().get("total");
        // Cast to long is safe under the integral-only sum contract (see parseTermsSum).
        long value = sumAgg != null && sumAgg.isSum() ? (long) sumAgg.sum().value() : 0L;
        if (value != 0) {
          byGroup.computeIfAbsent(group, k -> new LinkedHashMap<>()).put(bandLabel, value);
        }
      }
    }
    return byGroup;
  }

  record TermsInclude(Set<String> requestedKeys, List<String> includeTerms)
  {
  }
}
