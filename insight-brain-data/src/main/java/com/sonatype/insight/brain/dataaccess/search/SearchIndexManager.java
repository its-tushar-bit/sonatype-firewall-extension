/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.search;

import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.dataaccess.TransactionContext;

public interface SearchIndexManager
{
  void insert(TransactionContext tx, SearchIndexChange searchIndexChange);

  void insert(SearchIndexChange searchIndexChange);
}
