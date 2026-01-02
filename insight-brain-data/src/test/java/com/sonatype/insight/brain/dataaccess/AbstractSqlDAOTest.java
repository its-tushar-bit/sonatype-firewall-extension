/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.List;

import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
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
  private static final class StubbedAbstractSqlDAO extends AbstractSqlDAO<MigrationTracker>
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
    public TransactionContext createTransactionContext() {
      return new TransactionContext(dataStore.getJPAEntityManagerFactory().createEntityManager());
    }

    public List<MigrationTracker> getAll() {
      String sQuery = "SELECT entity FROM MigrationTracker entity";
      return getList(createTransactionContext(), sQuery);
    }

    @Override
    public long getCount() {
      String sQuery = "SELECT COUNT(entity) FROM MigrationTracker entity";
      return getSingle(Long.class, sQuery);
    }
  }
}
