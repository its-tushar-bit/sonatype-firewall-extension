/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.search.results.SearchResultDTO;

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
}
