/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.postgres.PostgresServer;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractDataStoreTest
    extends AbstractDatabaseTest
{
  protected abstract DataStore createTestDataStore();

  protected DataStore dataStore;

  @Before
  public void setup() {
    dataStore = createTestDataStore();
  }

  protected void initDatabase(DatabaseConfig databaseConfig) {
    dataStore.initWithMigration(databaseConfig, true);
  }

  private void verifyDatabaseCreation(DatabaseConfig databaseConfig) throws Exception {
    assertThat(dataStore.getDatabaseConfig()).isNull();

    initDatabase(databaseConfig);
    DataSource dataSource = dataStore.getDataSource();
    assertThat(dataSource).isNotNull();
    try (Connection conn = dataSource.getConnection()) {
      try (Statement stmt = conn.createStatement()) {
        stmt.execute("SELECT * FROM " + dataStore.getID() + ".test_table");
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

    assertThat(dataStore.getDatabaseConfig()).isEqualTo(databaseConfig);
  }

  @Test
  public void testDatabaseCreation_H2_OnDisk() throws Exception {
    File databaseDir = tempDir.newFolder();
    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, "test");

    // New database
    verifyDatabaseCreation(databaseConfig);
    assertThat(databaseDir).exists();
    assertThat(new File(databaseDir, "test.h2.db")).exists();

    // Existing database
    dataStore = createTestDataStore(); // create new datastore instance
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
      DatabaseConfig databaseConfig = postgres.getDatabaseConfig();

      // New database
      verifyDatabaseCreation(databaseConfig);

      // Existing database
      dataStore = createTestDataStore();
      verifyDatabaseCreation(databaseConfig);
    }
  }

  @Test
  public void testInit_Migrate_Postgres() throws Exception {
    try (PostgresServer postgres = new PostgresServer()) {
      DatabaseConfig databaseConfig = postgres.getDatabaseConfig();

      ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
      resourceDatabasePopulator
          .addScript(new ClassPathResource(getClass().getSimpleName() + "/Migrate/postgres-initial-version.sql"));
      try (Connection connection =
               DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
        resourceDatabasePopulator.populate(connection);
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("SELECT * FROM " + dataStore.getID() + ".schema_version")) {
          assertThat(results.next()).isTrue();
        }
      }

      initDatabase(databaseConfig);

      int desiredDbVersion = DatabaseMigrator.determineDesiredVersion(dataStore.getID());
      assertThat(DatabaseUtil.getDatabaseSchemaVersion(dataStore.getDataSource(), dataStore.getID())).isEqualTo(
          desiredDbVersion);
    }
  }
}
