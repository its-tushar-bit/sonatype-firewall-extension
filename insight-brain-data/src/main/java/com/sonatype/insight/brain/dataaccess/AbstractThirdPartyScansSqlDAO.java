/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.sql.Array;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

public abstract class AbstractThirdPartyScansSqlDAO<T extends HasStringId>
    extends AbstractSqlDAO<T>
{
  private final ThirdPartyScansDataStore thirdPartyScansDataStore;

  private final SearchIndexManager searchIndexManager;

  protected AbstractThirdPartyScansSqlDAO(ThirdPartyScansDataStore thirdPartyScansDataStore) {
    this.thirdPartyScansDataStore = thirdPartyScansDataStore;
    this.searchIndexManager = null;
  }

  protected AbstractThirdPartyScansSqlDAO(
      ThirdPartyScansDataStore thirdPartyScansDataStore,
      SearchIndexManager searchIndexManager)
  {
    super(searchIndexManager);
    this.thirdPartyScansDataStore = thirdPartyScansDataStore;
    this.searchIndexManager = searchIndexManager;
  }

  protected String getDatabaseSchema() {
    return thirdPartyScansDataStore.getDatabaseSchema();
  }

  protected Array createArrayOf(JDBCType jdbcType, Object[] elements) throws SQLException {
    try (Connection connection = thirdPartyScansDataStore.getDataSource().getConnection()) {
      return connection.createArrayOf(jdbcType.name(), elements);
    }
  }

  protected <E, U> List<U> getListWithSqlInClause(
      Collection<E> inClauseValues,
      Function<Collection<E>, List<U>> getter)
  {
    return super.getListWithSqlInClause(inClauseValues, getter, thirdPartyScansDataStore);
  }

  @Override
  protected ThirdPartyScansDataStore getDataStore() {
    return thirdPartyScansDataStore;
  }

  /**
   * Override to use non-transactional insert for search index changes.
   * <p>
   * ThirdPartyScans DAOs use a different database than the Operational database where search_index_change
   * table lives. The passed tx is for ThirdPartyScans database, so we cannot use it for inserting into
   * the Operational database's search_index_change table. Instead, we use the non-transactional insert
   * which creates its own transaction context for the Operational database.
   * </p>
   */
  @Override
  protected void insertSearchIndexChange(final TransactionContext tx, final SearchIndexChange searchIndexChange) {
    if (searchIndexManager != null) {
      // Use non-transactional insert since search_index_change is in Operational DB, not ThirdPartyScans DB
      searchIndexManager.insert(searchIndexChange);
    }
  }
}
