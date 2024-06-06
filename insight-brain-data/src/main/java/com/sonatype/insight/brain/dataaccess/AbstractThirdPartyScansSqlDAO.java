/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.sql.Array;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

public abstract class AbstractThirdPartyScansSqlDAO<T extends HasStringId>
    extends AbstractSqlDAO<T>
{
  private final ThirdPartyScansDataStore thirdPartyScansDataStore;

  protected AbstractThirdPartyScansSqlDAO(ThirdPartyScansDataStore thirdPartyScansDataStore) {
    this.thirdPartyScansDataStore = thirdPartyScansDataStore;
  }

  @Override
  public TransactionContext createTransactionContext() {
    return new TransactionContext(thirdPartyScansDataStore.getJPAEntityManagerFactory().createEntityManager());
  }

  protected String getDatabaseSchema() {
    return thirdPartyScansDataStore.getDatabaseSchema();
  }

  protected Array createArrayOf(JDBCType jdbcType, Object[] elements) throws SQLException {
    return thirdPartyScansDataStore.getDataSource().getConnection().createArrayOf(jdbcType.name(), elements);
  }

  protected <E> List<T> getListWithSqlInClause(List<E> inClauseValues, Function<Collection<E>, List<T>> getter) {
    return super.getListWithSqlInClause(inClauseValues, getter, thirdPartyScansDataStore);
  }
}
