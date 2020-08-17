/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

public abstract class AbstractOperationalSqlDAO<T extends HasStringId>
    extends AbstractSqlDAO<T>
{
  public static final int H2_IN_OPERATOR_THRESHOLD = 2000;

  @Override
  public TransactionContext createTransactionContext() {
    return new TransactionContext(OperationalDataStoreProvider.getJPAEntityManagerFactory().createEntityManager());
  }

  public boolean isDatabaseEmbedded() {
    return OperationalDataStoreProvider.isDatabaseEmbedded();
  }

  @Override
  public void insert(TransactionContext tx, T entity) {
    super.insert(tx, entity);
    insertSearchIndexChange(tx, newSearchIndexChangeForInsert(entity));
  }

  @Override
  public void update(TransactionContext tx, T entity) {
    super.update(tx, entity);
    insertSearchIndexChange(tx, newSearchIndexChangeForUpdate(entity));
  }

  @Override
  public void delete(TransactionContext tx, T entity) {
    super.delete(tx, entity);
    insertSearchIndexChange(tx, newSearchIndexChangeForDelete(entity));
  }

  protected void insertSearchIndexChange(TransactionContext tx, SearchIndexChange searchIndexChange) {
    if (searchIndexChange != null) {
      new SearchIndexChangeDAO().insert(tx, searchIndexChange);
    }
  }

  protected SearchIndexChange newSearchIndexChangeForInsert(T entity) {
    return newSearchIndexChange(entity);
  }

  protected SearchIndexChange newSearchIndexChangeForUpdate(T entity) {
    return newSearchIndexChange(entity);
  }

  protected SearchIndexChange newSearchIndexChangeForDelete(T entity) {
    return newSearchIndexChange(entity);
  }

  protected SearchIndexChange newSearchIndexChange(T entity) {
    // by default, no contribution to the search index
    return null;
  }
}
