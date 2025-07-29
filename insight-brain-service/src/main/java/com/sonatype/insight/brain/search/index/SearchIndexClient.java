/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import com.sonatype.insight.brain.search.results.SearchResultDTO;

/**
 * Client methods for working with the search index
 */
public interface SearchIndexClient
{
  String SEARCH_INDEX_SIZE_BYTES = "search_index_size_bytes";

  String SEARCH_INDEX_REINDEX = "search_index_reindex";

  String SEARCH_INDEX_DURATION_SECONDS = "search_index_duration_seconds";

  //TODO: consider renaming this method to describe its purpose: create the index and re-index the documents
  void createIndex();

  void updateIndex();

  Long getLastIndexTime();

  long getIndexSize();

  SearchResultDTO searchIndex(
      String searchQuery,
      int pageSize,
      int page,
      boolean allComponents,
      boolean isSbomManagerMode);
}
