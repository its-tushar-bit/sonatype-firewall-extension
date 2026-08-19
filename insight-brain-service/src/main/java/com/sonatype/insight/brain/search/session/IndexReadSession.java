/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.session;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.RankedGroupsResult;

import org.apache.lucene.search.Query;

public interface IndexReadSession
    extends AutoCloseable
{
  String backendId();

  Instant lastUpdatedAt();

  /** Diagnostic only — not a cross-request consistency token. */
  String snapshotHandle();

  IndexPageResult searchPage(IndexPageRequest request);

  long count(Query query);

  /**
   * Top {@code maxBuckets} values of {@code field} by document count, over the RBAC-scoped documents
   * matching {@code query}. Lucene reads the field's facet doc-values column and OpenSearch uses a terms
   * aggregation; a field the current index cannot serve as a facet yields an empty list rather than an
   * error, so a rail degrades instead of failing the page.
   * <p>
   * Only for bounded vocabularies (organization, application, category, stage, enum). The Lucene
   * implementation builds index-wide doc-values ordinals for the field, not ordinals for the matching
   * documents alone, so an unbounded column such as {@code componentHash} or a CVE id would size that
   * state to the whole corpus however selective the query is. Counting distinct values of an unbounded
   * column is what {@link #countDistinctGroupedBy} is for.
   *
   * @return the buckets, never null and never containing null, in descending count order; an empty list when
   *         there is nothing to aggregate, so callers need no null checks
   */
  List<IndexTermsBucket> termsAggregation(Query query, String field, int maxBuckets);

  /**
   * Groups the RBAC-scoped documents matching {@code query} by {@code groupField}, reduces each
   * group to the maximum value of {@code metricField}, and returns the highest-ranked groups together
   * with the distinct group count and per-band distinct group counts.
   * <p>
   * Groups are ordered by metric, then by {@code groupValue} ascending. Groups whose documents carry no
   * metric value sort last regardless of {@code ascending}. Group values are returned lower-cased.
   * {@code metricField} must be a float/sortable-int metric (e.g. {@code vulnerabilitySeverity}): Lucene
   * always decodes maxes via {@code NumericUtils.sortableIntToFloat}. Integral int fields are not
   * supported and will disagree across backends. Metrics are assumed non-negative. OpenSearch enumerates
   * groups via a paged composite aggregation, then ranks and bands in Java so unscored groups sort last
   * and band counts match Lucene's per-group max semantics.
   * <p>
   * Field names are trusted caller input (no {@code checkFieldNames} validation). Result map iteration
   * order is unspecified across backends. The session applies {@code withRbac} to {@code query} and
   * runs against the pinned reader or PIT.
   *
   * @param limit maximum number of ranked groups returned
   * @param ascending true to rank by lowest metric first
   * @param metricBands half-open {@code [minInclusive, maxExclusive)} bands, counted as distinct groups
   */
  RankedGroupsResult rankGroupsByMaxMetric(
      Query query,
      String groupField,
      String metricField,
      int limit,
      boolean ascending,
      Map<String, float[]> metricBands);

  /**
   * RBAC-scoped bucketed count. {@code bucketField} is a numeric field (e.g.
   * policyViolationThreatLevel); {@code ranges} maps a bucket label to an [minInclusive, maxInclusive]
   * int pair. The session applies {@code withRbac} to {@code query} and runs against the pinned
   * reader or PIT. Fails closed identically to {@link #count(Query)}: callers with no readable
   * contexts get all-zero buckets.
   */
  MetricAggregationResult aggregateCountByField(
      Query query,
      String bucketField,
      Map<String, int[]> ranges);

  /**
   * RBAC-scoped bucketed count over a <em>float</em> point field (the float sibling of
   * {@link #aggregateCountByField(Query, String, Map)}). {@code bucketField} is a numeric
   * {@code FloatPoint} field (e.g. {@code vulnerabilitySeverity}, a CVSS score); {@code ranges} maps a
   * bucket label to a {@code float[2]} {@code [minInclusive, maxExclusive)} half-open pair. The upper
   * bound is <em>exclusive</em> (unlike the int overload, whose upper bound is inclusive) so adjacent
   * CVSS bands cannot double-count a boundary value. This is a raw document count per band; callers
   * needing distinct-entity counts must use the
   * {@link #aggregateCountByFloatField(Query, String, Map, String) distinct-field overload}. Fails
   * closed identically to {@link #count(Query)}. The session applies {@code withRbac} to {@code query}.
   */
  default MetricAggregationResult aggregateCountByFloatField(
      Query query,
      String bucketField,
      Map<String, float[]> ranges)
  {
    return aggregateCountByFloatField(query, bucketField, ranges, null);
  }

  /**
   * Half-open float-band aggregation with an optional {@code distinctField}. When {@code distinctField}
   * is {@code null} this is the raw per-document band count of
   * {@link #aggregateCountByFloatField(Query, String, Map)}. When {@code distinctField} is non-null each
   * band's count is the number of <em>distinct</em> {@code distinctField} values among the documents whose
   * {@code bucketField} value falls in that band. The {@code total} on the result is the raw document total
   * (unaffected by {@code distinctField}); only the per-band bucket counts become distinct counts. Bands
   * are half-open {@code [minInclusive, maxExclusive)} on both backends. Fails closed identically to
   * {@link #count(Query)}. The session applies {@code withRbac} to {@code query}.
   * <p>
   * Lucene currently implements bands as one filtered scan per band (interim stored-field distinct when
   * {@code distinctField} is set). OpenSearch uses a single range aggregation. Lucene resolves every
   * band from the metric's doc-values column in one pass when that column and any distinct column carry
   * doc values, and falls back to one filtered search per band for an index written before they existed.
   * <p>
   * When {@code distinctField} is non-null, Lucene counts exactly; OpenSearch uses HyperLogLog++
   * cardinality estimates ({@code precisionThreshold=40_000}) with no exactness flag on
   * {@link MetricAggregationResult}.
   */
  MetricAggregationResult aggregateCountByFloatField(
      Query query,
      String bucketField,
      Map<String, float[]> ranges,
      String distinctField);

  /**
   * RBAC-scoped, page-level distinct count: for the documents matching {@code query}, counts distinct
   * {@code distinctField} values grouped by {@code groupField}, restricted to {@code groupValues}. Returns a
   * map from group value to distinct count; a group with no matching documents (or only blank field values)
   * is absent from the map (callers treat absence as zero). This lets a whole result page's affected-app /
   * affected-component counts be computed in one index read instead of one distinct-count query per row.
   * Fails closed identically to {@link #count(Query)}: callers with no readable contexts get an
   * empty map. The session applies {@code withRbac} to {@code query} and runs against the pinned reader or PIT.
   * <p>
   * Lucene returns exact distinct counts. OpenSearch returns HyperLogLog++ estimates
   * ({@code precisionThreshold=40_000}); there is no exactness flag on this {@code Map} return type
   * (unlike {@link #rankGroupsByMaxMetric}'s {@code distinctGroupCountExact}).
   */
  Map<String, Long> countDistinctGroupedBy(
      Query query,
      String groupField,
      String distinctField,
      Collection<String> groupValues);

  /**
   * RBAC-scoped, page-level distinct count split into numeric <em>bands</em>: for the documents matching
   * {@code query}, counts distinct {@code distinctField} values grouped by {@code groupField}
   * (restricted to {@code groupValues}) <em>within each</em> {@code [minInclusive, maxInclusive]} band of
   * {@code bandField}. Returns a map from group value to a per-band count map (band label to distinct
   * count); a (group, band) pair with no matching documents is absent (callers treat absence as zero).
   * Band ranges are built programmatically on both backends, never string-interpolated into a re-parsed
   * query. Fails closed identically to {@link #countDistinctGroupedBy(Query, String, String, Collection)}:
   * callers with no readable contexts get an empty map. The session applies {@code withRbac} to
   * {@code query}.
   * <p>
   * Lucene currently runs one filtered collect per band (interim stored-field path). OpenSearch uses a
   * single nested range aggregation. Lucene runs one grouped-distinct collect per band, each reading
   * doc-values columns when they are present.
   * <p>
   * Lucene returns exact distinct counts. OpenSearch banded distincts are HyperLogLog++ estimates
   * ({@code precisionThreshold=40_000}) with no exactness flag on the returned map.
   */
  Map<String, Map<String, Long>> countDistinctGroupedByBands(
      Query query,
      String groupField,
      String distinctField,
      Collection<String> groupValues,
      String bandField,
      Map<String, int[]> bands);

  /**
   * RBAC-scoped plain sum of an <em>integral</em> numeric field grouped by {@code groupField},
   * restricted to {@code groupValues}. Returns a map from group value to sum; a group with no matching
   * documents is absent from the map (callers treat absence as zero). This is a plain sum over the
   * docValues column (not a distinct sum); callers are responsible for stage scoping. Empty
   * {@code groupValues} returns an empty map. Fails closed identically to {@link #count(Query)}. The
   * session applies {@code withRbac} to {@code query}.
   * <p>
   * {@code sumField} must be an integral (int/long) docValues field. Float/sortable-int fields (e.g.
   * {@code vulnerabilitySeverity}) produce meaningless sums if cast to {@code long}. Implementations
   * reject known float sum fields with {@link IllegalArgumentException}; there is no general
   * field-type registry check — callers must not pass other float-encoded fields.
   * <p>
   * Field names are trusted caller input (no {@code checkFieldNames} validation). Result map iteration
   * order is unspecified across backends.
   */
  Map<String, Long> sumGroupedBy(
      Query query,
      String groupField,
      String sumField,
      Collection<String> groupValues);

  /**
   * RBAC-scoped plain sum of an <em>integral</em> numeric field grouped by {@code groupField},
   * restricted to {@code groupValues}, within each {@code [minInclusive, maxInclusive]} band of
   * {@code bandField}. Returns a map from group value to a per-band sum map (band label to sum); a
   * (group, band) pair with no matching documents is absent (callers treat absence as zero). Empty
   * {@code groupValues} or empty bands returns an empty map. Fails closed identically to
   * {@link #sumGroupedBy(Query, String, String, Collection)}. The session applies {@code withRbac} to
   * {@code query}.
   * <p>
   * {@code sumField} must be backed by integral (int/long) docValues — see
   * {@link #sumGroupedBy(Query, String, String, Collection)} for the integral-only contract.
   * <p>
   * Field names are trusted caller input. Result map iteration order is unspecified across backends.
   */
  Map<String, Map<String, Long>> sumGroupedByBands(
      Query query,
      String groupField,
      String sumField,
      Collection<String> groupValues,
      String bandField,
      Map<String, int[]> bands);

  @Override
  void close();
}
