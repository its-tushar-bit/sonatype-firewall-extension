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
import com.sonatype.insight.postgres.PostgresServer;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractDatabaseProviderTest
    extends AbstractDatabaseTest
{
  protected abstract DatabaseConfig getDatabaseConfig();

  protected abstract void initDatabase(DatabaseConfig databaseConfig);

  protected abstract DataSource getDataSource();

  protected abstract String getSchemaName();

  private void verifyDatabaseCreation(DatabaseConfig databaseConfig) throws Exception {
    assertThat(getDatabaseConfig()).isNull();

    initDatabase(databaseConfig);
    DataSource dataSource = getDataSource();
    assertThat(dataSource).isNotNull();
    try (Connection conn = dataSource.getConnection()) {
      try (Statement stmt = conn.createStatement()) {
        stmt.execute("SELECT * FROM " + getSchemaName() + ".test_table");
      }

      String databaseURL = conn.getMetaData().getURL();
      assertThat(databaseURL).isNotNull();
      if (databaseConfig != null) {
        assertThat(databaseConfig.getUrl()).startsWith(databaseURL);
      }
      else {
        assertThat(databaseURL).isEqualTo("jdbc:h2:mem:inMemoryDatabase");
      }
    }

    assertThat(getDatabaseConfig()).isEqualTo(databaseConfig);
  }

  @Test
  public void testDatabaseCreation_H2_OnDisk() throws Exception {
    File databaseDir = tempDir.newFolder();
    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, "test");

    // New database
    verifyDatabaseCreation(databaseConfig);
    assertThat(databaseDir).exists();
    assertThat(new File(databaseDir, "test.h2.db")).exists();
    DataSourceFactory.clear_ForTestsOnly();

    // Existing database
    DataSourceFactory.clear_ForTestsOnly();
    verifyDatabaseCreation(databaseConfig);
    assertThat(databaseDir).exists();
    assertThat(new File(databaseDir, "test.h2.db")).exists();
  }

  @Test
  public void testDatabaseCreation_H2_InMemory() throws Exception {
    verifyDatabaseCreation(null);
  }

  @Test
  public void testDatabaseCreation_Postgres() throws Exception {
    try (PostgresServer postgres = new PostgresServer()) {
      DatabaseConfig databaseConfig = getDatabaseConfig(postgres);

      // New database
      verifyDatabaseCreation(databaseConfig);

      // Existing database
      DataSourceFactory.clear_ForTestsOnly();
      verifyDatabaseCreation(databaseConfig);
    }
  }
}
