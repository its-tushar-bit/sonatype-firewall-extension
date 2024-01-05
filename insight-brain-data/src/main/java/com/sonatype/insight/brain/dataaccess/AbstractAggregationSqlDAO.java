/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.33
 */
public abstract class AbstractAggregationSqlDAO<T extends HasStringId>
    extends AbstractSqlDAO<T>
{
  private final AggregationDataStore aggregationDataStore;

  protected AbstractAggregationSqlDAO(AggregationDataStore aggregationDataStore) {
    this.aggregationDataStore = aggregationDataStore;
  }

  @Override
  public TransactionContext createTransactionContext() {
    return new TransactionContext(aggregationDataStore.getJPAEntityManagerFactory().createEntityManager());
  }
}
