/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.33
 */
public abstract class AbstractAggregationSqlDAO<T extends HasStringId>
    extends AbstractSqlDAO<T>
{
  private final AggregationDataStore aggregationDataStore;

  private final SearchIndexManager searchIndexManager;

  protected AbstractAggregationSqlDAO(AggregationDataStore aggregationDataStore) {
    this.aggregationDataStore = aggregationDataStore;
    this.searchIndexManager = null;
  }

  protected AbstractAggregationSqlDAO(
      AggregationDataStore aggregationDataStore,
      SearchIndexManager searchIndexManager)
  {
    super(searchIndexManager);
    this.aggregationDataStore = aggregationDataStore;
    this.searchIndexManager = searchIndexManager;
  }

  @Override
  protected AggregationDataStore getDataStore() {
    return aggregationDataStore;
  }

  /**
   * Override to use non-transactional insert for search index changes.
   * <p>
   * Aggregation DAOs use a different database than the Operational database where search_index_change
   * table lives. The passed tx is for Aggregation database, so we cannot use it for inserting into
   * the Operational database's search_index_change table. Instead, we use the non-transactional insert
   * which creates its own transaction context for the Operational database.
   * </p>
   */
  @Override
  protected void insertSearchIndexChange(final TransactionContext tx, final SearchIndexChange searchIndexChange) {
    if (searchIndexManager != null) {
      // Use non-transactional insert since search_index_change is in Operational DB, not Aggregation DB
      searchIndexManager.insert(searchIndexChange);
    }
  }
}
