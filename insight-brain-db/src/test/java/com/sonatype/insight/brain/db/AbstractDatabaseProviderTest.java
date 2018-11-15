/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseConfig;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractDatabaseProviderTest
    extends AbstractDatabaseTest
{
  protected abstract DatabaseConfig getDatabaseConfig();

  protected abstract void initDatabase(DatabaseConfig databaseConfig);

  protected abstract DataSource getDataSource();

  private void verifyDatabaseCreation(DatabaseConfig databaseConfig) throws Exception {
    assertThat(getDatabaseConfig(), nullValue());

    initDatabase(databaseConfig);
    DataSource dataSource = getDataSource();
    assertNotNull(dataSource);
    try (Connection conn = dataSource.getConnection()) {
      try (Statement stmt = conn.createStatement()) {
        stmt.execute("SELECT * FROM test_table");
      }

      String databaseURL = conn.getMetaData().getURL();
      assertNotNull(databaseURL);
      if (databaseConfig != null) {
        assertTrue(databaseConfig.getUrl().startsWith(databaseURL + ";"));
      }
      else {
        assertEquals("jdbc:h2:mem:inMemoryDatabase", databaseURL);
      }
    }

    assertThat(getDatabaseConfig(), is(databaseConfig));
  }

  @Test
  public void testDatabaseCreation_OnDisk() throws Exception {
    File databaseDir = tempDir.newFolder();
    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, "test");

    // New database
    verifyDatabaseCreation(databaseConfig);
    assertTrue(databaseDir.exists());
    assertTrue(new File(databaseDir, "test.h2.db").exists());
    DataSourceFactory.clear_ForTestsOnly();

    // Existing database
    DataSourceFactory.clear_ForTestsOnly();
    verifyDatabaseCreation(databaseConfig);
    assertTrue(databaseDir.exists());
    assertTrue(new File(databaseDir, "test.h2.db").exists());
  }

  @Test
  public void testDatabaseCreation_InMemory() throws Exception {
    verifyDatabaseCreation(null);
  }
}
