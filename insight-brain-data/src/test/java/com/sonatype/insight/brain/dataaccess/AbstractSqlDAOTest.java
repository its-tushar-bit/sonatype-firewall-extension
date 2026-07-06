/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;

import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.MigrationTracker.MIGRATION_TRACKER;
import static org.assertj.core.api.Assertions.assertThat;

public class AbstractSqlDAOTest
    extends AbstractDbDAOTest
{
  private StubbedAbstractSqlDAO stubbedAbstractSqlDAO;

  @Override
  @Before
  public void setup() {
    super.setup();
    this.stubbedAbstractSqlDAO = new StubbedAbstractSqlDAO(databaseRule.getOperationalDataStore());
  }

  @After
  public void tearDown() {
    AbstractSqlDAO.MAX_ALLOWED_DB_RESULTS = AbstractSqlDAO.DEFAULT_MAX_ALLOWED_DB_RESULTS;
  }

  @Test
  public void testEntityLimit() {
    AbstractSqlDAO.MAX_ALLOWED_DB_RESULTS = 3;

    assertThat(stubbedAbstractSqlDAO.getCount()).isGreaterThan(3);
    assertThat(stubbedAbstractSqlDAO.getAll().size()).isEqualTo(3);
  }

  // We use MigrationTracker here because we need an entity and this is a simple table / class
  private static final class StubbedAbstractSqlDAO
      extends AbstractSqlDAO<MigrationTracker>
  {
    private final DataStore dataStore;

    public StubbedAbstractSqlDAO(DataStore dataStore) {
      this.dataStore = dataStore;
    }

    @Override
    protected DataStore getDataStore() {
      return dataStore;
    }

    @Override
    public Table<?> getJooqTable() {
      return MIGRATION_TRACKER;
    }

    @Override
    public Class<MigrationTracker> getEntityClass() {
      return MigrationTracker.class;
    }

    @Override
    public int insert(TransactionContext tx, MigrationTracker entity) {
      throw new UnsupportedOperationException("Not needed for this test");
    }

    @Override
    public int update(TransactionContext tx, MigrationTracker entity) {
      throw new UnsupportedOperationException("Not needed for this test");
    }

    @Override
    public List<MigrationTracker> getAll() {
      try (TransactionContext tx = createTransactionContext()) {
        return tx.dsl()
            .selectFrom(MIGRATION_TRACKER)
            .limit(MAX_ALLOWED_DB_RESULTS)
            .fetchInto(MigrationTracker.class);
      }
    }
  }
}
