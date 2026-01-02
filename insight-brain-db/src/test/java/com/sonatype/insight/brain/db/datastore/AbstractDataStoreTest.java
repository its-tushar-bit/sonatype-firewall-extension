/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.migrations.LegacyDataStoreMigrator;
import com.sonatype.insight.brain.db.migrations.LiquibaseDataStoreMigrator;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractDataStoreTest
    extends AbstractDatabaseTest
{
  protected abstract DataStore getTestDataStore();

  protected void migrateDatabase() {
    new LegacyDataStoreMigrator(getTestDataStore()).migrate();
    new LiquibaseDataStoreMigrator(getTestDataStore()).migrate();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest(cleanDatabase = true, suppressMigrations = true)
  public void testInit_Migrate_Postgres() throws Exception {
    ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
    resourceDatabasePopulator
        .addScript(new ClassPathResource(getClass().getSimpleName() + "/Migrate/postgres-initial-version.sql"));
    try (Connection connection = databaseRule.getOperationalDataStore().getDataSource().getConnection()) {
      resourceDatabasePopulator.populate(connection);
      try (Statement statement = connection.createStatement();
           ResultSet results = statement.executeQuery(
               "SELECT * FROM " + getTestDataStore().getID() + ".schema_version")) {
        assertThat(results.next()).isTrue();
      }
    }

    migrateDatabase();

    int desiredDbVersion = LegacyDataStoreMigrator.determineDesiredVersion(getTestDataStore().getID());
    assertThat(DatabaseUtil.getLegacyDatabaseSchemaVersion(getTestDataStore())).isEqualTo(desiredDbVersion);

    //TODO - liquibase assertions
  }

  public void testInit_Migrate() {
    File databaseVersionFile = getDatabaseVersionFile(getDatabasePath(), getTestDataStore().getID());

    migrateDatabase();

    assertThat(databaseVersionFile).doesNotExist();

    int desiredDbVersion = LegacyDataStoreMigrator.determineDesiredVersion(getTestDataStore().getID());
    assertThat(DatabaseUtil.getLegacyDatabaseSchemaVersion(getTestDataStore())).isEqualTo(desiredDbVersion);
    //TODO - liquibase assertions
  }
}
