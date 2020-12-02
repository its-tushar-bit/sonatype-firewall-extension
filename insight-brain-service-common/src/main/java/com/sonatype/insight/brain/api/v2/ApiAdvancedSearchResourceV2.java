/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;

import com.sonatype.insight.brain.search.results.SearchResultDTO;

/**
 * Resource for API Advanced Searching
 */
public interface ApiAdvancedSearchResourceV2
{
  /**
   * Search request to search the index.
   *
   * @param searchQuery - String holding a query to search for.
   * @param pageSize    - the amount of results per page
   * @param page        - the current page to start from, 0 indexed.
   * @return SearchResultDTO
   * @throws IOException on failing to search the index
   */
  SearchResultDTO searchIndex(
      String searchQuery,
      int pageSize,
      int page) throws IOException;

  /**
   * Request a Search Index to be created asynchronously.
   */
  void createSearchIndexAsync();
}
