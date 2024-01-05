/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.search;

import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * A no-op implementation of {@link SearchIndexManager}. Most DAOs will use this implementation by default by extending
 * the constructor that does not use/inject the full {@link DefaultSearchIndexManager}.
 */
public class EmptySearchIndexManager
    implements SearchIndexManager
{
  private static final SearchIndexManager INSTANCE = new EmptySearchIndexManager();

  public static SearchIndexManager getInstance() {
    return INSTANCE;
  }

  @Override
  public void insert(final TransactionContext tx, final SearchIndexChange searchIndexChange) {
    // no-op
  }
}
