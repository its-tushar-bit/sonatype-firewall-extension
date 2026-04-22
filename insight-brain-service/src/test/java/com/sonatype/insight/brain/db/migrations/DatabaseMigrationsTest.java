/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.test.LogOutput;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class DatabaseMigrationsTest
    extends AbstractDatabaseTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Rule
  public LogOutput logOutput = new LogOutput(LegacyDatabaseMigrator.class);

  private DatabaseMigrations databaseMigrations;

  private List<DatabaseMigrator> testDatabaseMigrators;

  @Before
  public void before() {
    databaseMigrations = new TestDatabaseMigrations(databaseRule);
  }

  @Test
  public void testThatBothLegacyAndLiquibaseMigrationsExecute() {
    runDatabaseMigrator();
    assertThat(testDatabaseMigrators.size()).isEqualTo(2);
    assertThat(testDatabaseMigrators.get(0)).isExactlyInstanceOf(LegacyDatabaseMigrator.class);
    assertThat(testDatabaseMigrators.get(1)).isExactlyInstanceOf(LiquibaseDatabaseMigrator.class);
    verify(testDatabaseMigrators.get(0)).migrate();
    verify(testDatabaseMigrators.get(1)).migrate();
  }

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "DatabaseMigrationsTest/testMigrate_ByEnvironmentVariable")
  @Category(SlowTest.class)
  public void testMigrate_MigrationDisabled_ByEnvironmentVariable() throws Exception {
    environmentVariables.set(DatabaseMigrations.NXIQ_SCHEMA_MIGRATION, "false");

    runDatabaseMigrator();

    File databaseVersionFile = new File(getDatabasePath(), "ods.ver");
    assertThat(databaseVersionFile).exists();
    assertThat(FileUtils.readFileToString(databaseVersionFile, StandardCharsets.UTF_8)).isEqualTo("85");
  }

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "DatabaseMigrationsTest/testMigrate_ByEnvironmentVariable")
  @Category(SlowTest.class)
  public void testMigrate_ForceEnableMigration_OverridesEnvironmentVariable() {
    try {
      DatabaseMigrations.setForceEnableMigration(true);
      environmentVariables.set(DatabaseMigrations.NXIQ_SCHEMA_MIGRATION, "false");

      runDatabaseMigrator();

      File databaseVersionFile = new File(getDatabasePath(), "ods.ver");
      assertThat(databaseVersionFile).doesNotExist();
      assertThat(DatabaseUtil.getLegacyDatabaseSchemaVersion(databaseRule.getOperationalDataStore())).isEqualTo(
          LegacyDataStoreMigrator.determineDesiredVersion(OperationalDataStore.ID));
    }
    finally {
      DatabaseMigrations.setForceEnableMigration(false);
    }
  }

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "DatabaseMigrationsTest/testMigrate_ByEnvironmentVariable")
  @Category(SlowTest.class)
  public void testMigrate_MigrationEnabled_ByEnvironmentVariable() {
    environmentVariables.set(DatabaseMigrations.NXIQ_SCHEMA_MIGRATION, "true");

    runDatabaseMigrator();

    File databaseVersionFile = new File(getDatabasePath(), "ods.ver");
    assertThat(databaseVersionFile).doesNotExist();
    assertThat(DatabaseUtil.getLegacyDatabaseSchemaVersion(databaseRule.getOperationalDataStore())).isEqualTo(
        LegacyDataStoreMigrator.determineDesiredVersion(OperationalDataStore.ID));
  }

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "DatabaseMigrationsTest/" +
          "testMigrate_MigrationDisabled_BySystemConfigurationProperty")
  @Category(SlowTest.class)
  public void testMigrate_MigrationDisabled_BySystemConfigurationProperty() throws Exception {
    assertThat(
        DatabaseUtil.systemConfigurationPropertyTableExists(databaseRule.getOperationalDataStore().getDataSource(),
            OperationalDataStore.ID)).isTrue();
    assertThat(databaseMigrations.getSchemaMigrationEnabledFromDatabase())
        .isEqualTo("false");

    runDatabaseMigrator();

    File databaseVersionFile = new File(getDatabasePath(), "ods.ver");
    assertThat(databaseVersionFile).exists();
    assertThat(FileUtils.readFileToString(databaseVersionFile, StandardCharsets.UTF_8)).isEqualTo("110");
  }

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "DatabaseMigrationsTest/" +
          "testMigrate_MigrationDisabled_BySystemConfigurationProperty")
  @Category(SlowTest.class)
  public void testMigrate_ForceEnableMigration_OverridesSystemConfigurationProperty() {
    try {
      DatabaseMigrations.setForceEnableMigration(true);
      DataSource dataSource = databaseRule.getOperationalDataStore().getDataSource();
      assertThat(DatabaseUtil.systemConfigurationPropertyTableExists(dataSource, OperationalDataStore.ID)).isTrue();
      assertThat(databaseMigrations.getSchemaMigrationEnabledFromDatabase())
          .isEqualTo("false");

      runDatabaseMigrator();

      File databaseVersionFile = new File(getDatabasePath(), "ods.ver");
      assertThat(databaseVersionFile).doesNotExist();
      assertThat(DatabaseUtil.getLegacyDatabaseSchemaVersion(databaseRule.getOperationalDataStore())).isEqualTo(
          LegacyDataStoreMigrator.determineDesiredVersion(OperationalDataStore.ID));
    }
    finally {
      DatabaseMigrations.setForceEnableMigration(false);
    }
  }

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "DatabaseMigrationsTest/" +
          "testMigrate_MigrationEnabled_BySystemConfigurationProperty")
  @Category(SlowTest.class)
  public void testMigrate_MigrationEnabled_BySystemConfigurationProperty() {
    DataSource dataSource = databaseRule.getOperationalDataStore().getDataSource();
    assertThat(DatabaseUtil.systemConfigurationPropertyTableExists(dataSource, OperationalDataStore.ID)).isTrue();
    assertThat(databaseMigrations.getSchemaMigrationEnabledFromDatabase())
        .isEqualTo("true");

    runDatabaseMigrator();

    File databaseVersionFile = new File(getDatabasePath(), "ods.ver");
    assertThat(databaseVersionFile).doesNotExist();
    assertThat(DatabaseUtil.getLegacyDatabaseSchemaVersion(databaseRule.getOperationalDataStore())).isEqualTo(
        LegacyDataStoreMigrator.determineDesiredVersion(OperationalDataStore.ID));
  }

  @Test
  @H2DiskTest
  @Category(SlowTest.class)
  public void testDesiredSchemaVersionMet() {
    DatabaseMigrations databaseMigrations = new DatabaseMigrations(databaseRule);

    // @H2DiskTest will create a fully migrated schema so the schema version check will be met
    databaseMigrations.validateMinimumSchemaVersion();

    assertThat(logOutput).doesNotContain("Database migration is required.");
  }

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      // re-use this simple test database that needs migrations
      copyExistingDatabase = "DatabaseMigrationsTest/testMigrate_ByEnvironmentVariable")
  @Category(SlowTest.class)
  public void testDesiredSchemaVersionUnmet() {
    LegacyDatabaseMigrator legacyMigrator = new LegacyDatabaseMigrator(databaseRule);
    LegacyDatabaseMigrator spyMigrator = Mockito.spy(legacyMigrator);
    Mockito.doNothing().when(spyMigrator).exit(Mockito.anyInt());

    spyMigrator.validateMinimumSchemaVersion();

    verify(spyMigrator).exit(1);
    assertThat(logOutput).atErrorLevel()
        .contains("\n\n\t\t\t***** Database migration is required. " +
            "Please migrate the database before starting the application! *****\n");
  }

  @Test
  @H2DiskTest(
      suppressMigrations = true)
  @Category(SlowTest.class)
  public void testDesiredSchemaVersionNoSchema() {
    LegacyDatabaseMigrator legacyMigrator = new LegacyDatabaseMigrator(databaseRule);
    LegacyDatabaseMigrator spyMigrator = Mockito.spy(legacyMigrator);
    Mockito.doNothing().when(spyMigrator).exit(Mockito.anyInt());

    spyMigrator.validateMinimumSchemaVersion();

    verify(spyMigrator).exit(1);
    assertThat(logOutput).atErrorLevel()
        .contains("\n\n\t\t\t***** Database migration is required. " +
            "Please migrate the database before starting the application! *****\n");
  }

  private void runDatabaseMigrator() {
    databaseMigrations.migrateDatabase();
  }

  /**
   * Extend to add a Spy to the inner {@link DatabaseMigrator} instances in the list
   */
  private class TestDatabaseMigrations
      extends DatabaseMigrations
  {
    public TestDatabaseMigrations(final DataStoreProvider dataStoreProvider) {
      super(dataStoreProvider);
    }

    // Override to add a spy to the DatabaseMigrators
    @Override
    protected DatabaseMigrators createDatabaseMigrators() {
      return new DatabaseMigrators(TestDatabaseMigrations.this)
      {
        @Override
        protected List<DatabaseMigrator> createDatabaseMigrators() {
          testDatabaseMigrators = new ArrayList<>();
          for (DatabaseMigrator migrator : super.createDatabaseMigrators()) {
            testDatabaseMigrators.add(Mockito.spy(migrator));
          }
          return testDatabaseMigrators;
        }
      };
    }
  }
}
