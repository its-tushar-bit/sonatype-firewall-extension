/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.search.global.GlobalSearchRequest;
import com.sonatype.insight.brain.search.global.GlobalSearchResult;
import com.sonatype.insight.brain.search.results.SearchResultDTO;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

/**
 * Client methods for working with the search index
 */
public interface SearchIndexClient
{
  String SEARCH_INDEX_SIZE_BYTES = "search_index_size_bytes";

  String SEARCH_INDEX_REINDEX = "search_index_reindex";

  String SEARCH_INDEX_DURATION_SECONDS = "search_index_duration_seconds";

  // TODO: consider renaming this method to describe its purpose: create the index and re-index the documents
  void populateIndex();

  /**
   * Requests that an in-flight full rebuild abort without cutting over to a partial generation.
   * Default is a no-op for backends that do not support cooperative cancel.
   */
  default void cancelFullRebuild() {
  }

  /**
   * True while a full rebuild is building a new generation (or cutting over). Default {@code false}.
   */
  default boolean isFullRebuildInProgress() {
    return false;
  }

  /**
   * Updates the search index with the given changes, using the provided deletion callback.
   * This allows callers to control how SearchIndexChanges are deleted after processing.
   *
   * @param searchIndexChanges the changes to process
   * @param deletionCallback callback to delete a change after processing (can be no-op)
   */
  void updateIndex(List<SearchIndexChange> searchIndexChanges, Consumer<SearchIndexChange> deletionCallback);

  /**
   * Updates the search index with the given changes.
   * Uses the default deletion behavior (calls deleteSearchIndexChange).
   *
   * @param searchIndexChanges the changes to process
   */
  default void updateIndex(List<SearchIndexChange> searchIndexChanges) {
    updateIndex(searchIndexChanges, this::deleteSearchIndexChange);
  }

  default void updateIndex() {
    updateIndex(getSearchIndexChanges());
  }

  /**
   * Deletes the specified SearchIndexChange from the database.
   * This is separated from updateIndex to allow HybridSearchIndexClient to defer deletion
   * until both primary and secondary clients have processed the changes.
   *
   * @param change the change to delete
   */
  void deleteSearchIndexChange(SearchIndexChange change);

  Long getLastIndexTime();

  long getIndexSize();

  SearchResultDTO searchIndex(
      String searchQuery,
      int pageSize,
      int page,
      boolean allComponents,
      boolean isSbomManagerMode,
      List<String> searchAfter);

  /**
   * Same as {@link #searchIndex(String, int, int, boolean, boolean, List)} but ANDs
   * boolean-clause-budget-exempt term-set restrictions (Lucene {@code TermInSetQuery} / OpenSearch
   * {@code terms}). Null or empty {@code termSetRestrictions} means no extra filters. An empty id
   * set on a restriction matches nothing (fail closed). Ids are matched lower-cased. CLM-44783.
   */
  default SearchResultDTO searchIndex(
      String searchQuery,
      int pageSize,
      int page,
      boolean allComponents,
      boolean isSbomManagerMode,
      List<String> searchAfter,
      List<? extends IndexFilterRestriction> termSetRestrictions)
  {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " does not implement term-set restricted searchIndex()");
  }

  List<SearchIndexChange> getSearchIndexChanges();

  /**
   * RBAC-scoped count of documents matching {@code metricQuery}. Fails closed: callers with no
   * allowed contexts get 0 (never an unscoped count). {@code metricQuery} is a small, server-built
   * field query (e.g. {@code itemType:APPLICATION}); the RBAC filter is applied internally and
   * programmatically (not string-concatenated). Implemented in CLM-40927 PR1.
   */
  long count(String metricQuery);

  /**
   * Same as {@link #count(String)} with budget-exempt term-set restrictions. CLM-44783.
   */
  default long count(String metricQuery, List<? extends IndexFilterRestriction> termSetRestrictions) {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " does not implement term-set restricted count()");
  }

  /**
   * RBAC-scoped bucketed count. {@code bucketField} is a numeric field (e.g.
   * policyViolationThreatLevel); {@code ranges} maps a bucket label to an [minInclusive, maxInclusive]
   * int pair. Implemented in CLM-40927 PR1.
   */
  MetricAggregationResult aggregateCountByField(String metricQuery, String bucketField, Map<String, int[]> ranges);

  /**
   * Same as {@link #aggregateCountByField(String, String, Map)} with budget-exempt term-set
   * restrictions. CLM-44783.
   */
  default MetricAggregationResult aggregateCountByField(
      String metricQuery,
      String bucketField,
      Map<String, int[]> ranges,
      List<? extends IndexFilterRestriction> termSetRestrictions)
  {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " does not implement term-set restricted aggregateCountByField()");
  }

  /**
   * RBAC-scoped bucketed count over a <em>float</em> point field (the float sibling of
   * {@link #aggregateCountByField(String, String, Map)}). {@code bucketField} is a numeric
   * {@code FloatPoint} field (e.g. {@code vulnerabilitySeverity}, a CVSS score); {@code ranges} maps a
   * bucket label to a {@code float[2]} {@code [minInclusive, maxExclusive)} half-open pair. The upper
   * bound is <em>exclusive</em> (unlike the int overload, whose upper bound is inclusive) so adjacent
   * CVSS bands cannot double-count a boundary value: a score of {@code 7.0} lands in {@code [7.0, 9.0)}
   * (High) and never in {@code [4.0, 7.0)} (Medium). This is a raw document count per band (a document
   * whose bucket value is in the range counts once per band); callers needing distinct-entity counts
   * (e.g. distinct CVEs across per-app-per-stage docs) must use the
   * {@link #aggregateCountByFloatField(String, String, Map, String) distinct-field overload}. Fails
   * closed identically to {@link #count(String)}: callers with no readable contexts get all-zero
   * buckets. The same programmatic RBAC filter is applied internally (never string-concatenated).
   */
  default MetricAggregationResult aggregateCountByFloatField(
      String metricQuery,
      String bucketField,
      Map<String, float[]> ranges)
  {
    return aggregateCountByFloatField(metricQuery, bucketField, ranges, null);
  }

  /**
   * Half-open float-band aggregation with an optional {@code distinctField}. When {@code distinctField}
   * is {@code null} this is the raw per-document band count of
   * {@link #aggregateCountByFloatField(String, String, Map)}. When {@code distinctField} is non-null each
   * band's count is the number of <em>distinct</em> {@code distinctField} values among the documents whose
   * {@code bucketField} value falls in that band — the float-band sibling of the {@code countDistinct}
   * machinery. This powers the CVSS severity-band facet, where a single CVE recurs across many
   * per-app-per-stage documents and must count once in its band, in a single aggregation pass instead of
   * one {@code countDistinct} per band. The {@code total} on the result is the raw document total
   * (unaffected by {@code distinctField}); only the per-band bucket counts become distinct counts. Bands
   * are half-open {@code [minInclusive, maxExclusive)} on both backends, so a boundary value (e.g. CVSS
   * {@code 7.0}) lands in exactly one band. Fails closed identically to {@link #count(String)}. The same
   * programmatic RBAC filter is applied internally (never string-concatenated).
   */
  MetricAggregationResult aggregateCountByFloatField(
      String metricQuery,
      String bucketField,
      Map<String, float[]> ranges,
      String distinctField);

  /**
   * Same as {@link #aggregateCountByFloatField(String, String, Map, String)} with budget-exempt
   * term-set restrictions. CLM-44783.
   */
  default MetricAggregationResult aggregateCountByFloatField(
      String metricQuery,
      String bucketField,
      Map<String, float[]> ranges,
      String distinctField,
      List<? extends IndexFilterRestriction> termSetRestrictions)
  {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " does not implement term-set restricted aggregateCountByFloatField()");
  }

  /**
   * RBAC-scoped count of <em>distinct</em> composite keys among the documents matching {@code metricQuery}.
   * The composite key is the tuple of values of {@code compositeKeyFields} (e.g.
   * {@code [applicationId, componentHash]}); documents sharing the same tuple count once. This powers the
   * "Scanned Components" KPI, where a vulnerable component is indexed as one
   * {@link ItemType#SECURITY_VULNERABILITY} document per CVE, so a naive {@link #count(String)} over-counts.
   * Fails closed identically to {@link #count(String)}: callers with no readable contexts get 0. The same
   * programmatic RBAC filter is applied internally (never string-concatenated). Implemented in CLM-40927 PR4.
   * <p>
   * Backend semantics differ at scale: the Lucene implementation counts exactly (in-memory {@code HashSet}); the
   * OpenSearch implementation uses a {@code cardinality} aggregation (HyperLogLog++), which is approximate above
   * {@code precision_threshold} (configured to 40000 for tighter error bounds). Dashboard KPIs tolerate this
   * approximation; {@link HybridSearchIndexClient} falls back to the exact Lucene count when OpenSearch fails.
   */
  long countDistinct(String metricQuery, List<String> compositeKeyFields);

  /**
   * Same as {@link #countDistinct(String, List)} with budget-exempt term-set restrictions. CLM-44783.
   */
  default long countDistinct(
      String metricQuery,
      List<String> compositeKeyFields,
      List<? extends IndexFilterRestriction> termSetRestrictions)
  {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " does not implement term-set restricted countDistinct()");
  }

  /**
   * Several {@link #countDistinct(String, List)} composites over the same {@code metricQuery}. The default
   * walks each named key independently. Backends that can count several named keys in one index pass
   * should override so legal/KPI breakdowns share that pass.
   */
  default Map<String, Long> countDistinctNamed(
      String metricQuery,
      Map<String, List<String>> namedCompositeKeyFields)
  {
    return countDistinctNamed(metricQuery, namedCompositeKeyFields, null);
  }

  /**
   * Same as {@link #countDistinctNamed(String, Map)} with budget-exempt term-set restrictions.
   */
  default Map<String, Long> countDistinctNamed(
      String metricQuery,
      Map<String, List<String>> namedCompositeKeyFields,
      List<? extends IndexFilterRestriction> termSetRestrictions)
  {
    Map<String, Long> counts = new LinkedHashMap<>();
    if (namedCompositeKeyFields == null || namedCompositeKeyFields.isEmpty()) {
      return counts;
    }
    namedCompositeKeyFields.forEach(
        (name, fields) -> counts.put(name, countDistinct(metricQuery, fields, termSetRestrictions)));
    return counts;
  }

  /**
   * Distinct {@code compositeKeyFields} total plus per-band distinct counts on {@code bucketField} for
   * the same matching documents. {@code compositeKeyFields} must contain exactly one field — a
   * multi-field list throws. Default is {@link #countDistinct} then
   * {@link #aggregateCountByFloatField(String, String, Map, String)}. {@code total} is the overall
   * distinct count (including unscored docs that sit in no band); buckets are distinct-per-band only.
   * An empty or null list returns a zero total and no buckets.
   */
  default MetricAggregationResult countDistinctAndFloatBands(
      String metricQuery,
      List<String> compositeKeyFields,
      String bucketField,
      Map<String, float[]> ranges)
  {
    return countDistinctAndFloatBands(metricQuery, compositeKeyFields, bucketField, ranges, null);
  }

  /**
   * Same as {@link #countDistinctAndFloatBands(String, List, String, Map)} with budget-exempt
   * term-set restrictions.
   */
  default MetricAggregationResult countDistinctAndFloatBands(
      String metricQuery,
      List<String> compositeKeyFields,
      String bucketField,
      Map<String, float[]> ranges,
      List<? extends IndexFilterRestriction> termSetRestrictions)
  {
    if (!hasSingleDistinctAndFloatBandsKey(compositeKeyFields)) {
      return new MetricAggregationResult(0L, Map.of());
    }
    long total = countDistinct(metricQuery, compositeKeyFields, termSetRestrictions);
    String distinctField = compositeKeyFields.get(0);
    MetricAggregationResult bands = aggregateCountByFloatField(
        metricQuery, bucketField, ranges, distinctField, termSetRestrictions);
    return new MetricAggregationResult(total, bands.buckets);
  }

  /**
   * Empty/null and single-field contract for {@link #countDistinctAndFloatBands}.
   *
   * @return {@code false} when {@code compositeKeyFields} is null or empty (callers return a zero
   *         total and no buckets)
   * @throws IllegalArgumentException when more than one field is supplied
   */
  static boolean hasSingleDistinctAndFloatBandsKey(List<String> compositeKeyFields) {
    if (compositeKeyFields == null || compositeKeyFields.isEmpty()) {
      return false;
    }
    if (compositeKeyFields.size() > 1) {
      throw new IllegalArgumentException(
          "countDistinctAndFloatBands supports a single distinct field; got " + compositeKeyFields);
    }
    return true;
  }

  /**
   * RBAC-scoped, page-level distinct count: for the documents matching {@code metricQuery}, counts distinct
   * {@code distinctField} values grouped by {@code groupField}, restricted to {@code groupValues}. Returns a
   * map from group value to distinct count; a group with no matching documents (or only blank field values)
   * is absent from the map (callers treat absence as zero). This lets a whole result page's affected-app /
   * affected-component counts be computed in one index read instead of one distinct-count query per row.
   * Fails closed identically to {@link #countDistinct(String, List)}: callers with no readable contexts get an
   * empty map. The same programmatic RBAC filter is applied internally (never string-concatenated).
   */
  Map<String, Long> countDistinctGroupedBy(
      String metricQuery,
      String groupField,
      String distinctField,
      Collection<String> groupValues);

  /**
   * Same as {@link #countDistinctGroupedBy(String, String, String, Collection)} with budget-exempt
   * term-set restrictions. CLM-44783.
   */
  default Map<String, Long> countDistinctGroupedBy(
      String metricQuery,
      String groupField,
      String distinctField,
      Collection<String> groupValues,
      List<? extends IndexFilterRestriction> termSetRestrictions)
  {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " does not implement term-set restricted countDistinctGroupedBy()");
  }

  /**
   * Same as {@link #countDistinctGroupedBy} but reports whether the producing backend's counts are
   * exact. Prefer this over combining {@link #countDistinctGroupedBy} with {@link #backendId()}:
   * hybrid failover can return secondary results while {@code backendId()} still names the primary.
   */
  default GroupedDistinctCounts countDistinctGroupedByWithExactness(
      final String metricQuery,
      final String groupField,
      final String distinctField,
      final Collection<String> groupValues)
  {
    return new GroupedDistinctCounts(
        countDistinctGroupedBy(metricQuery, groupField, distinctField, groupValues),
        isDistinctAggregationExact());
  }

  /**
   * Same as {@link #countDistinctGroupedByWithExactness(String, String, String, Collection)} with
   * budget-exempt term-set restrictions. CLM-44783.
   */
  default GroupedDistinctCounts countDistinctGroupedByWithExactness(
      final String metricQuery,
      final String groupField,
      final String distinctField,
      final Collection<String> groupValues,
      final List<? extends IndexFilterRestriction> termSetRestrictions)
  {
    return new GroupedDistinctCounts(
        countDistinctGroupedBy(metricQuery, groupField, distinctField, groupValues, termSetRestrictions),
        isDistinctAggregationExact());
  }

  /**
   * Whether {@link #countDistinctGroupedBy} returns exact distinct counts on this backend (Lucene
   * yes; OpenSearch cardinality is HLL). Default {@code false} (fail closed / approximate).
   */
  default boolean isDistinctAggregationExact() {
    return false;
  }

  /**
   * Groups the RBAC-filtered documents matching {@code metricQuery} by {@code groupField}, reduces each
   * group to the maximum value of {@code metricField}, and returns the highest-ranked groups together
   * with the distinct group count and per-band distinct group counts.
   * <p>
   * Groups are ordered by metric, then by {@code groupValue} ascending. Groups whose documents carry no
   * metric value sort last regardless of {@code ascending}. Group values are returned lower-cased.
   *
   * @param limit maximum number of ranked groups returned
   * @param ascending true to rank by lowest metric first
   * @param metricBands half-open {@code [minInclusive, maxExclusive)} bands, counted as distinct groups
   */
  default RankedGroupsResult rankGroupsByMaxMetric(
      final String metricQuery,
      final String groupField,
      final String metricField,
      final int limit,
      final boolean ascending,
      final Map<String, float[]> metricBands)
  {
    throw new UnsupportedOperationException("rankGroupsByMaxMetric is not supported by " + backendId());
  }

  /**
   * Same as {@link #rankGroupsByMaxMetric(String, String, String, int, boolean, Map)} with budget-exempt
   * term-set restrictions. CLM-44783.
   */
  default RankedGroupsResult rankGroupsByMaxMetric(
      String metricQuery,
      String groupField,
      String metricField,
      int limit,
      boolean ascending,
      Map<String, float[]> metricBands,
      List<? extends IndexFilterRestriction> termSetRestrictions)
  {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " does not implement term-set restricted rankGroupsByMaxMetric()");
  }

  /**
   * RBAC-scoped, page-level distinct count split into numeric <em>bands</em>: for the documents matching
   * {@code metricQuery}, counts distinct {@code distinctField} values grouped by {@code groupField}
   * (restricted to {@code groupValues}) <em>within each</em> {@code [minInclusive, maxInclusive]} band of
   * {@code bandField}. Returns a map from group value to a per-band count map (band label to distinct
   * count); a (group, band) pair with no matching documents is absent (callers treat absence as zero).
   * <p>
   * This powers the Components leg per-severity policy-violation counts (critical/high/medium/low), where
   * the group is the component hash, the distinct entity is the policy violation id (so the same violation
   * re-indexed across per-(app, stage) docs counts once), and the bands are the {@link ItemType#POLICY_VIOLATION}
   * threat-level severity bands. The whole page's four counts are computed in one index read per band
   * (a small constant, not one query per row). Band ranges are built programmatically on both backends
   * (an {@code IntPoint} range on Lucene, a range aggregation on OpenSearch), never string-interpolated
   * into a re-parsed query. Fails
   * closed identically to {@link #countDistinctGroupedBy(String, String, String, Collection)}: callers with
   * no readable contexts get an empty map.
   */
  Map<String, Map<String, Long>> countDistinctGroupedByBands(
      String metricQuery,
      String groupField,
      String distinctField,
      Collection<String> groupValues,
      String bandField,
      Map<String, int[]> bands);

  /**
   * Same as {@link #countDistinctGroupedByBands(String, String, String, Collection, String, Map)} with
   * budget-exempt term-set restrictions. CLM-44783.
   */
  default Map<String, Map<String, Long>> countDistinctGroupedByBands(
      String metricQuery,
      String groupField,
      String distinctField,
      Collection<String> groupValues,
      String bandField,
      Map<String, int[]> bands,
      List<? extends IndexFilterRestriction> termSetRestrictions)
  {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " does not implement term-set restricted countDistinctGroupedByBands()");
  }

  /**
   * Permission-filters {@code baseQuery}: looks up the caller's READ contexts, builds the filter,
   * and wraps. Prefer this over calling the three steps below by hand.
   *
   * <p>
   * For global/root-access callers the permission filter is {@code null} (no filtering needed), so
   * the base query is returned unchanged. If such a caller also passes a {@code null} base query, a
   * {@link MatchAllDocsQuery} is returned rather than a bare {@code null} that a searcher would NPE
   * on. A non-null base is always returned intact; this method never yields {@code null}.
   *
   * <p>
   * For a non-global (restricted) caller passing a {@code null} base query, the permission filter
   * alone is returned — semantically "all documents I'm permitted to see", i.e. equivalent to
   * {@link MatchAllDocsQuery} AND the permission filter.
   *
   * <p>
   * Backward-compat contract (requires prior reindex): the permission filter matches on the
   * denormalized {@code allowedContextIds} field. Documents indexed before this field existed
   * (pre-upgrade docs) do not carry it until the one-time backfill/reindex has run, so a non-global
   * caller's filtered query matches nothing on those un-backfilled docs and returns empty results
   * (fail-closed/secure, but surprising). A consumer of this permission filter therefore MUST NOT
   * be enabled in production until the {@code allowedContextIds} backfill/reindex has completed for
   * the tenant. That backfill is gated behind the reindex feature flag (default off) and ships with
   * the first consuming feature, not in this foundations change.
   */
  default Query buildPermittedQuery(Query baseQuery) {
    Query filter = buildAllowedContextIdsFilter(getCurrentUserContextIdsWithReadPermission());
    Query permitted = wrapWithPermissionFilter(baseQuery, filter);
    return permitted != null ? permitted : new MatchAllDocsQuery();
  }

  /**
   * Context IDs (org and/or app) on which the current user has READ. Default impl throws so
   * unimplemented backends fail loudly rather than returning an unsafe empty set.
   *
   * @apiNote Low-level step of the permission-filter pipeline; prefer the composed
   *          {@link #buildPermittedQuery(Query)}, which cannot forget the lookup+build+wrap order.
   */
  default Set<String> getCurrentUserContextIdsWithReadPermission() {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " does not implement permission lookup");
  }

  /**
   * Builds the permission-filter clause from the caller's READ contexts. See
   * {@link com.sonatype.insight.brain.search.index.AbstractSearchIndexClient#buildAllowedContextIdsFilter(Set)}.
   *
   * @apiNote Low-level step of the permission-filter pipeline; prefer the composed
   *          {@link #buildPermittedQuery(Query)}.
   */
  default Query buildAllowedContextIdsFilter(Set<String> userPermittedContextIds) {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " does not implement permission filter");
  }

  /**
   * ANDs a base query with a permission filter. See
   * {@link com.sonatype.insight.brain.search.index.AbstractSearchIndexClient#wrapWithPermissionFilter(Query, Query)}.
   *
   * <p>
   * Contract: a {@code null} {@code baseQuery} combined with a {@code null} {@code permissionFilter}
   * yields {@code null}. A searcher NPEs on a {@code null} query, so direct callers must pass a
   * non-null {@code baseQuery}. The safe composed entry point is {@link #buildPermittedQuery(Query)},
   * which substitutes a {@link MatchAllDocsQuery} instead of ever returning {@code null}.
   *
   * @apiNote Low-level step of the permission-filter pipeline; prefer the composed
   *          {@link #buildPermittedQuery(Query)}.
   */
  default Query wrapWithPermissionFilter(Query baseQuery, Query permissionFilter) {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " does not implement permission wrap");
  }

  /** Default {@code false} so clients fail closed: an unwired feature flag reads as disabled. */
  default boolean isSearchPreviewEnabled() {
    return false;
  }

  /**
   * Global Search read path. Implementations must enforce the {@code track_total_hits} cap and
   * honour the supplied {@link Query} verbatim. Default throws so hybrid impls can't accidentally
   * serve this surface.
   */
  default GlobalSearchResult searchGlobal(GlobalSearchRequest request) {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " does not implement Global Search searchGlobal()");
  }

  /**
   * Backend identifier embedded in the {@code GlobalSearchCursor} generation token so a cursor
   * minted by one backend cannot be decoded by another. Default throws so impls fail loudly rather
   * than return a falsely-shared id.
   */
  default String backendId() {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " does not implement Global Search backendId()");
  }

  /**
   * Validate SBOM-Manager-vs-default mode against the license. Default throws so an unimplemented
   * backend fails loudly rather than silently skipping the license/mode gate, consistent with
   * {@link #searchGlobal(GlobalSearchRequest)} and {@link #backendId()}.
   */
  default void checkGlobalSearchMode(boolean isSbomManagerMode) {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " does not implement Global Search mode check");
  }
}
