/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.datasource.DataSourceProviderFactory;
import com.sonatype.insight.brain.db.datastore.DataStoreMigrator;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.H2DatabaseEngine;

import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseMigratorTest
    extends AbstractDatabaseTest
{
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "DatabaseMigratorTest/testMigrate_ByEnvironmentVariable"
  )
  public void testMigrate_MigrationDisabled_ByEnvironmentVariable() throws Exception {
    environmentVariables.set(DatabaseMigrator.NXIQ_SCHEMA_MIGRATION, "false");

    runDatabaseMigrator();

    File databaseVersionFile = new File(getDatabasePath(), "ods.ver");
    assertThat(databaseVersionFile).exists();
    assertThat(FileUtils.readFileToString(databaseVersionFile, StandardCharsets.UTF_8)).isEqualTo("85");
  }

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "DatabaseMigratorTest/testMigrate_ByEnvironmentVariable"
  )
  public void testMigrate_ForceEnableMigration_OverridesEnvironmentVariable() throws Exception {
    try {
      DatabaseMigrator.setForceEnableMigration(true);
      environmentVariables.set(DatabaseMigrator.NXIQ_SCHEMA_MIGRATION, "false");

      runDatabaseMigrator();

      File databaseDir = getDatabasePath();
      DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, DatabaseName.ods.name());
      DataSource dataSource = DataSourceProviderFactory
          .createDataSourceProvider(H2DatabaseEngine.INSTANCE)
          .getDataSource(databaseConfig, OperationalDataStore.ID);

      File databaseVersionFile = new File(getDatabasePath(), "ods.ver");
      assertThat(databaseVersionFile).doesNotExist();
      assertThat(DatabaseUtil.getDatabaseSchemaVersion(dataSource, OperationalDataStore.ID,
          OperationalDataStore.ID)).isEqualTo(
          DataStoreMigrator.determineDesiredVersion(OperationalDataStore.ID));
    }
    finally {
      DatabaseMigrator.setForceEnableMigration(false);
    }
  }

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "DatabaseMigratorTest/testMigrate_ByEnvironmentVariable"
  )
  public void testMigrate_MigrationEnabled_ByEnvironmentVariable() throws Exception {
    environmentVariables.set(DatabaseMigrator.NXIQ_SCHEMA_MIGRATION, "true");

    runDatabaseMigrator();

    File databaseDir = getDatabasePath();
    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, DatabaseName.ods.name());
    DataSource dataSource = DataSourceProviderFactory
        .createDataSourceProvider(H2DatabaseEngine.INSTANCE)
        .getDataSource(databaseConfig, OperationalDataStore.ID);

    File databaseVersionFile = new File(getDatabasePath(), "ods.ver");
    assertThat(databaseVersionFile).doesNotExist();
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(dataSource, OperationalDataStore.ID,
        OperationalDataStore.ID)).isEqualTo(
        DataStoreMigrator.determineDesiredVersion(OperationalDataStore.ID));
  }

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "DatabaseMigratorTest/" +
          "testMigrate_MigrationDisabled_BySystemConfigurationProperty"
  )
  public void testMigrate_MigrationDisabled_BySystemConfigurationProperty() throws Exception {
    File databaseDir = getDatabasePath();
    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, DatabaseName.ods.name());
    DataSource dataSource = DataSourceProviderFactory
        .createDataSourceProvider(H2DatabaseEngine.INSTANCE)
        .getDataSource(databaseConfig, OperationalDataStore.ID);
    assertThat(DatabaseUtil.systemConfigurationPropertyTableExists(dataSource, OperationalDataStore.ID)).isTrue();
    assertThat(DatabaseUtil.getSchemaMigrationEnabledFromDatabase(dataSource, OperationalDataStore.ID)).isEqualTo(
        "false");

    runDatabaseMigrator();

    File databaseVersionFile = new File(getDatabasePath(), "ods.ver");
    assertThat(databaseVersionFile).exists();
    assertThat(FileUtils.readFileToString(databaseVersionFile, StandardCharsets.UTF_8)).isEqualTo("110");
  }

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "DatabaseMigratorTest/" +
          "testMigrate_MigrationDisabled_BySystemConfigurationProperty"
  )
  public void testMigrate_ForceEnableMigration_OverridesSystemConfigurationProperty() throws Exception {
    try {
      DatabaseMigrator.setForceEnableMigration(true);
      File databaseDir = getDatabasePath();
      DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, DatabaseName.ods.name());
      DataSource dataSource = DataSourceProviderFactory
          .createDataSourceProvider(H2DatabaseEngine.INSTANCE)
          .getDataSource(databaseConfig, OperationalDataStore.ID);
      assertThat(DatabaseUtil.systemConfigurationPropertyTableExists(dataSource, OperationalDataStore.ID)).isTrue();
      assertThat(DatabaseUtil.getSchemaMigrationEnabledFromDatabase(dataSource, OperationalDataStore.ID)).isEqualTo(
          "false");

      runDatabaseMigrator();

      File databaseVersionFile = new File(getDatabasePath(), "ods.ver");
      assertThat(databaseVersionFile).doesNotExist();
      assertThat(DatabaseUtil.getDatabaseSchemaVersion(dataSource, OperationalDataStore.ID,
          OperationalDataStore.ID)).isEqualTo(
          DataStoreMigrator.determineDesiredVersion(OperationalDataStore.ID));
    }
    finally {
      DatabaseMigrator.setForceEnableMigration(false);
    }
  }

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "DatabaseMigratorTest/" +
          "testMigrate_MigrationEnabled_BySystemConfigurationProperty"
  )
  public void testMigrate_MigrationEnabled_BySystemConfigurationProperty() throws Exception {
    File databaseDir = getDatabasePath();
    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, DatabaseName.ods.name());
    DataSource dataSource = DataSourceProviderFactory
        .createDataSourceProvider(H2DatabaseEngine.INSTANCE)
        .getDataSource(databaseConfig, OperationalDataStore.ID);
    assertThat(DatabaseUtil.systemConfigurationPropertyTableExists(dataSource, OperationalDataStore.ID)).isTrue();
    assertThat(DatabaseUtil.getSchemaMigrationEnabledFromDatabase(dataSource, OperationalDataStore.ID)).isEqualTo(
        "true");

    runDatabaseMigrator();

    File databaseVersionFile = new File(getDatabasePath(), "ods.ver");
    assertThat(databaseVersionFile).doesNotExist();
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(dataSource, OperationalDataStore.ID,
        OperationalDataStore.ID)).isEqualTo(
        DataStoreMigrator.determineDesiredVersion(OperationalDataStore.ID));
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testNoUniqueIndexesDefined() throws IOException {
    URL url = Resources.getResource("db/insight_brain_ods/schema.sql");
    assertThat(url).isNotNull();

    List<String> filesChecked = new ArrayList<>();
    for (File file : new File(url.getFile()).getParentFile().listFiles()) {
      if (file.isFile()) {
        filesChecked.add(file.getName());
        // we should be using a unique constraint (which will result in an auto-generated unique index) rather than
        // explicitly creating a unique index ourselves;
        assertThat(FileUtils.readFileToString(file, StandardCharsets.UTF_8)).as("error in %s", file.getName())
            .doesNotContain("CREATE UNIQUE INDEX ");
      }
    }
    assertThat(filesChecked).isNotEmpty();
  }

  private void runDatabaseMigrator() {
    new DatabaseMigrator(databaseRule).migrate(true);
  }
}
