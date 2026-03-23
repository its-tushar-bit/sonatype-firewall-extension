/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

public abstract class AbstractDatamartSqlDAO<T extends HasStringId>
    extends AbstractSqlDAO<T>
{
  private final DataMartDataStore dataMartDataStore;

  private final SearchIndexManager searchIndexManager;

  protected AbstractDatamartSqlDAO(DataMartDataStore dataMartDataStore) {
    this.dataMartDataStore = dataMartDataStore;
    this.searchIndexManager = null;
  }

  protected AbstractDatamartSqlDAO(
      DataMartDataStore dataMartDataStore,
      SearchIndexManager searchIndexManager)
  {
    super(searchIndexManager);
    this.dataMartDataStore = dataMartDataStore;
    this.searchIndexManager = searchIndexManager;
  }

  @Override
  protected DataMartDataStore getDataStore() {
    return dataMartDataStore;
  }

  /**
   * Override to use non-transactional insert for search index changes.
   * <p>
   * DataMart DAOs use a different database than the Operational database where search_index_change
   * table lives. The passed tx is for DataMart database, so we cannot use it for inserting into
   * the Operational database's search_index_change table. Instead, we use the non-transactional insert
   * which creates its own transaction context for the Operational database.
   * </p>
   */
  @Override
  protected void insertSearchIndexChange(final TransactionContext tx, final SearchIndexChange searchIndexChange) {
    if (searchIndexManager != null) {
      // Use non-transactional insert since search_index_change is in Operational DB, not DataMart DB
      searchIndexManager.insert(searchIndexChange);
    }
  }
}
