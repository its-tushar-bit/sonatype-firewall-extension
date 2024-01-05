/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

public abstract class AbstractDatamartSqlDAO<T extends HasStringId>
    extends AbstractSqlDAO<T>
{
  private final DataMartDataStore dataMartDataStore;

  protected AbstractDatamartSqlDAO(DataMartDataStore dataMartDataStore) {
    this.dataMartDataStore = dataMartDataStore;
  }

  @Override
  public TransactionContext createTransactionContext() {
    return new TransactionContext(dataMartDataStore.getJPAEntityManagerFactory().createEntityManager());
  }

  protected DataMartDataStore getDataMartDataStore() {
    return dataMartDataStore;
  }
}
