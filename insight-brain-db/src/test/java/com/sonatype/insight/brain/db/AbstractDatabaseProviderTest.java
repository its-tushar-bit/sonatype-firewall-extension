/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.db.DatabaseConfig;

import static org.hamcrest.Matchers.is;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

public abstract class AbstractDatabaseProviderTest
{
  @Before
  public void setUp() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
  }

  @After
  public void tearDown() {
    DataSourceFactory.clear_ForTestsOnly();
  }

  protected void verifyDatabaseCreation(DatabaseConfig databaseConfig) throws Exception {
    assertThat(OperationalDataStoreProvider.getDatabaseConfig(), nullValue());

    OperationalDataStoreProvider.init(databaseConfig);
    DataSource dataSource = OperationalDataStoreProvider.getDataSource();
    Assert.assertNotNull(dataSource);
    Connection conn = dataSource.getConnection();
    try {
      exec(conn, "SELECT * FROM test_table");

      String databaseURL = conn.getMetaData().getURL();
      Assert.assertNotNull(databaseURL);
      if (databaseConfig != null) {
        Assert.assertTrue(databaseConfig.getUrl().startsWith(databaseURL + ";"));
      }
      else {
        Assert.assertEquals("jdbc:h2:mem:inMemoryDatabase", databaseURL);
      }
    }
    finally {
      conn.close();
    }

    assertThat(OperationalDataStoreProvider.getDatabaseConfig(), is(databaseConfig));
  }

  protected void verifyDatabaseCreation_OnDisk(DatabaseConfig databaseConfig, File databaseDir) throws Exception {
    new FileCleaner().delete(databaseDir);
    Assert.assertFalse(databaseDir.exists());

    // New database
    verifyDatabaseCreation(databaseConfig);
    Assert.assertTrue(databaseDir.exists());
    Assert.assertTrue(new File(databaseDir, "test.h2.db").exists());
    DataSourceFactory.clear_ForTestsOnly();

    // Existing database
    DataSourceFactory.clear_ForTestsOnly();
    verifyDatabaseCreation(databaseConfig);
    Assert.assertTrue(databaseDir.exists());
    Assert.assertTrue(new File(databaseDir, "test.h2.db").exists());
  }

  private void exec(Connection conn, String sql) throws SQLException {
    Statement stmt = conn.createStatement();
    try {
      stmt.execute(sql);
    }
    finally {
      if (stmt != null) {
        stmt.close();
      }
    }
  }
}
