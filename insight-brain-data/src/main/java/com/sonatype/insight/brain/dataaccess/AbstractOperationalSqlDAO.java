/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

public abstract class AbstractOperationalSqlDAO<T extends HasStringId>
    extends AbstractSqlDAO<T>
{
  @Override
  public TransactionContext createTransactionContext() {
    return new TransactionContext(OperationalDataStoreProvider.getJPAEntityManagerFactory().createEntityManager());
  }

  public boolean isDatabaseEmbedded() {
    return OperationalDataStoreProvider.isDatabaseEmbedded();
  }
}
