/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.postgres.PostgresServer;

import org.junit.Test;

public class SourceControlUserPostgresqlDAOTest
    extends SourceControlUserDAOTest
{
  @Override
  @Test
  public void testGetByApplicationId() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      super.testGetByApplicationId();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Override
  @Test
  public void testGetUserIdByEmailFilteringByApplicationId() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      super.testGetUserIdByEmailFilteringByApplicationId();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Override
  @Test
  public void testInsertAllIfNew_onlyNewUsers() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      super.testInsertAllIfNew_onlyNewUsers();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Override
  @Test
  public void testInsertAllIfNew_someUserExists_notFailAndIgnore() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      super.testInsertAllIfNew_someUserExists_notFailAndIgnore();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Override
  @Test
  public void testDelete_CascadeToSourceControlUserActivity() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      super.setup();
      super.testDelete_CascadeToSourceControlUserActivity();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }
}
