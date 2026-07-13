/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.sonatype.insight.brain.model.SearchIndexChange;
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

  List<SearchIndexChange> getSearchIndexChanges();

  /**
   * RBAC-scoped count of documents matching {@code metricQuery}. Fails closed: callers with no
   * allowed contexts get 0 (never an unscoped count). {@code metricQuery} is a small, server-built
   * field query (e.g. {@code itemType:APPLICATION}); the RBAC filter is applied internally and
   * programmatically (not string-concatenated). Implemented in CLM-40927 PR1.
   */
  long count(String metricQuery);

  /**
   * RBAC-scoped bucketed count. {@code bucketField} is a numeric field (e.g.
   * policyViolationThreatLevel); {@code ranges} maps a bucket label to an [minInclusive, maxInclusive]
   * int pair. Implemented in CLM-40927 PR1.
   */
  MetricAggregationResult aggregateCountByField(String metricQuery, String bucketField, Map<String, int[]> ranges);

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
}
