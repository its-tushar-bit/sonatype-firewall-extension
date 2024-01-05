/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.search;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * Encapsulates the insertion of search index events to the database via the {@link SearchIndexChangeDAO}. This is
 * because currently the DAO layer (specifically {@link AbstractOperationalSqlDAO}) is responsible for inserting these
 * search events and {@link SearchIndexChangeDAO} is itself a {@link AbstractOperationalSqlDAO}. This class simply puts an
 * abstraction between that.
 */
@Singleton
@Named
public class DefaultSearchIndexManager
    implements SearchIndexManager
{
  private final SearchIndexChangeDAO searchIndexChangeDAO;

  @Inject
  public DefaultSearchIndexManager(final SearchIndexChangeDAO searchIndexChangeDAO) {
    this.searchIndexChangeDAO = searchIndexChangeDAO;
  }

  @Override
  public void insert(final TransactionContext tx, final SearchIndexChange searchIndexChange) {
    if (searchIndexChange != null) {
      searchIndexChangeDAO.insert(tx, searchIndexChange);
    }
  }
}
