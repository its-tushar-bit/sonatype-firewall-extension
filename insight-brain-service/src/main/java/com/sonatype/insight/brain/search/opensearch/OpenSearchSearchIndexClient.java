/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import javax.inject.Inject;

import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultDTO;

/**
 * OpenSearch support for {@link SearchIndexClient}
 * <p>
 * Note: See {@link com.sonatype.insight.brain.search.SearchModule} for Guice bindings
 */
public class OpenSearchSearchIndexClient
    implements SearchIndexClient
{
  private final SearchConfig searchConfig;

  @Inject
  public OpenSearchSearchIndexClient(final SearchConfig searchConfig) {
    this.searchConfig = searchConfig;
  }

  @Override
  public void createIndex() {
    System.out.println(searchConfig); // avoid PMD unused field for now

    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public void updateIndex() {
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public Long getLastIndexTime() {
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public long getIndexSize() {
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public SearchResultDTO searchIndex(
      final String searchQuery,
      final int pageSize,
      final int page,
      final boolean allComponents,
      final boolean isSbomManagerMode)
  {
    throw new UnsupportedOperationException("not yet implemented");
  }
}
