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
import java.util.zip.ZipFile;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.db.DatabaseConfig;

import com.google.common.io.Resources;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.springframework.jdbc.datasource.init.ScriptStatementFailedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class DatabaseMigratorTest
    extends AbstractDatabaseTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Test
  public void testMigrate_VersionFileUpdatedWhenMigrationFailsAfterAtLeastOneSuccessfulScript() throws Exception {
    File databaseDir = tempDir.newFolder("db");
    FileUtils.copyDirectory(new File("target/test-classes/DatabaseMigratorTest/"
        + "testMigrate_VersionFileUpdatedWhenMigrationFailsAfterAtLeastOneSuccessfulScript"), databaseDir);
    File databaseVersionFile = new File(databaseDir, "dm.ver");
    assertThat(databaseVersionFile).isFile();
    assertThat(readDatabaseVersion(databaseVersionFile)).isEqualTo("3");

    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, "dm");

    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, DataMartDataStore.ID);

    // The migration should fail because schema_incremental_0007.sql drops the license_category table, but we already
    // removed this table.
    // The version file must be updated to contain the number of the last incremental script applied successfully (in
    // this case, schema_incremental_0006.sql).
    assertThatThrownBy(() -> {
      new DatabaseMigrator().migrate(databaseConfig, DataMartDataStore.ID, dataSource);
    }).isInstanceOf(ScriptStatementFailedException.class);
    assertThat(readDatabaseVersion(databaseVersionFile)).isEqualTo("6");
  }

  @Test
  public void testMigrate_CreatesDatabaseBackup_DatabaseVersionStoredInTheDatabase() throws Exception {
    File databaseDir = tempDir.newFolder("db");
    FileUtils.copyDirectory(new File("target/test-classes/DatabaseMigratorTest/"
        + "testMigrate_CreatesDatabaseBackup_DatabaseVersionStoredInTheDatabase"), databaseDir);

    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, "dm");

    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, DataMartDataStore.ID);

    // The migration should fail because schema_incremental_0007.sql drops the license_category table, but we already
    // removed this table.
    // The version file must be updated to contain the number of the last incremental script applied successfully (in
    // this case, schema_incremental_0006.sql).
    assertThatThrownBy(() -> {
      new DatabaseMigrator().migrate(databaseConfig, DataMartDataStore.ID, dataSource);
    }).isInstanceOf(ScriptStatementFailedException.class);

    File backupFile = new File(databaseDir, "backup/dm.zip");
    assertThat(backupFile).isFile();
  }

  @Test
  public void testMigrate_CreatesDatabaseBackup_DatabaseVersionStoredInSeparateFile() throws Exception {
    File databaseDir = tempDir.newFolder("db");
    FileUtils.copyDirectory(new File("target/test-classes/DatabaseMigratorTest/"
        + "testMigrate_CreatesDatabaseBackup_DatabaseVersionStoredInSeparateFile"), databaseDir);

    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, "dm");

    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, DataMartDataStore.ID);

    // The migration should fail because schema_incremental_0007.sql drops the license_category table, but we already
    // removed this table.
    // The version file must be updated to contain the number of the last incremental script applied successfully (in
    // this case, schema_incremental_0006.sql).
    assertThatThrownBy(() -> {
      new DatabaseMigrator().migrate(databaseConfig, DataMartDataStore.ID, dataSource);
    }).isInstanceOf(ScriptStatementFailedException.class);

    File backupFile = new File(databaseDir, "backup/dm.zip");
    assertThat(backupFile).isFile();
  }

  @Test
  public void testMigrate_VersionUpdatedWhenMigrationFailsAfterAtLeastOneSuccessfulScript() throws Exception {
    File databaseDir = tempDir.newFolder("db");
    FileUtils.copyDirectory(new File("target/test-classes/DatabaseMigratorTest/"
        + "testMigrate_VersionUpdatedWhenMigrationFailsAfterAtLeastOneSuccessfulScript"), databaseDir);
    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, "test");
    String databaseName = "testMigrate_VersionUpdatedWhenMigrationFailsAfterAtLeastOneSuccessfulScript";
    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, databaseName);
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(dataSource, databaseName)).isEqualTo(1);

    // Should fail to upgrade to version 3 because it tries to drop a non-existing table
    assertThatThrownBy(() -> new DatabaseMigrator().migrate(databaseConfig, databaseName, dataSource))
        .isInstanceOf(ScriptStatementFailedException.class);
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(dataSource, databaseName)).isEqualTo(2);
  }

  @Test
  public void testMigrate_RunsPostIncrementalMigrators() throws Exception {
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/PostIncrementalMigrator");
    File databaseVersionFile = getDatabaseVersionFile(databaseDir, "test");
    int desiredVersion = 12;
    assertThat(readDatabaseVersion(databaseVersionFile)).isEqualTo(String.valueOf(desiredVersion - 2));
    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, "test");
    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, "PostIncrementalMigrator");

    new DatabaseMigrator().migrate(databaseConfig, "PostIncrementalMigrator", dataSource);

    assertThat(PostIncrementalMigratorVersionMinus1.invoked).isFalse();
    assertThat(PostIncrementalMigratorVersion.invoked).isFalse();
    assertThat(PostIncrementalMigratorVersionPlus1.invoked).isTrue();
    assertThat(PostIncrementalMigratorVersionDesired.invoked).isTrue();
    assertThat(PostIncrementalMigratorVersionDesiredPlus1.invoked).isFalse();
  }

  @Test
  public void testMigrate_ZipBackups() throws Exception {
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/ZipBackupTest");

    String dbName = "test";
    File backupDir = tempDir.newFolder();

    new DatabaseMigrator().backup(databaseDir, dbName, backupDir);

    File backupZip = new File(backupDir, dbName + ".zip");
    assertThat(backupZip).isFile();

    try (ZipFile zipFile = new ZipFile(backupZip)) {
      assertThat(zipFile.size()).isEqualTo(2);
      assertThat(zipFile.getEntry("test.h2.db")).isNotNull();
      assertThat(zipFile.getEntry("test.ver")).isNotNull();
    }
  }

  @Test
  public void testMigrate_MissingVersion() throws Exception {
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/MissingVersion");
    File databaseVersionFile = getDatabaseVersionFile(databaseDir, "test");
    assertThat(databaseVersionFile).doesNotExist();
    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, "test");
    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, "MissingVersion");
    assertThat(DatabaseUtil.schemaVersionTableExists(dataSource, "MissingVersion")).isFalse();

    assertThatThrownBy(() -> {
      new DatabaseMigrator().migrate(databaseConfig, "MissingVersion", dataSource);
    }).isInstanceOf(IllegalStateException.class).hasMessage(
        "Missing the database schema version either in the database itself or in the database version file " +
            databaseVersionFile + ".");
  }

  @Test
  public void testMigrate_ThrowsExceptionWhenCurrentVersionIsHigherThanDesiredVersion() throws Exception {
    File databaseDir = tempDir.newFolder("db");
    copyDatabase(databaseDir, getClass().getSimpleName() + "/testMigrate_CurrentVersionHigherThanDesiredVersion");
    File databaseVersionFile = new File(databaseDir, "dm.ver");

    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, "dm");
    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, DataMartDataStore.ID);

    assertThatThrownBy(() -> {
      new DatabaseMigrator().migrate(databaseConfig, DataMartDataStore.ID, dataSource);
    }).isInstanceOf(IllegalStateException.class).hasMessageContaining("was created by a newer product version");
    assertThat(readDatabaseVersion(databaseVersionFile)).isEqualTo("9999");
  }

  @Test
  public void testMigrate_OperationalDataStore_ThrowsExceptionDuringExecute() {
    assertThatThrownBy(() -> {
      new DatabaseMigrator().runPostIncrementalMigrator(
          "/DatabaseMigratorTest/"
              + "testMigrate_OperationalDataStore_ThrowsExecuteExceptionMessage/schema_incremental_0089.cls",
          mock(DataSource.class));
    }).isInstanceOf(RuntimeException.class)
        .hasMessage("Failed to execute the PostIncrementalMigrator referenced in "
            + "/DatabaseMigratorTest/testMigrate_OperationalDataStore_ThrowsExecuteExceptionMessage/"
            + "schema_incremental_0089.cls.");
  }

  @Test
  public void testMigrate_OperationalDataStore_ThrowsExceptionDuringLoad() {
    assertThatThrownBy(() -> {
      new DatabaseMigrator().runPostIncrementalMigrator("/DatabaseMigratorTest/"
          + "testMigrate_OperationalDataStore_ThrowsLoadExceptionMessage/schema_incremental_0090.cls", null);
    }).isInstanceOf(RuntimeException.class)
        .hasMessage("Failed to execute the PostIncrementalMigrator referenced in "
            + "/DatabaseMigratorTest/testMigrate_OperationalDataStore_ThrowsLoadExceptionMessage/"
            + "schema_incremental_0090.cls.");
  }

  @Test
  public void testDetermineDesiredVersion() {
    assertThat(DatabaseMigrator.determineDesiredVersion("DetermineDesiredVersion")).isEqualTo(12);
  }

  @Test
  public void testMigrate_NewAggregationDatabase_PopulatesVersion() throws Exception {
    testMigrate_NewDatabase_PopulatesVersion(AggregationDataStore.ID);
  }

  @Test
  public void testMigrate_NewDatamartDatabase_PopulatesVersion() throws Exception {
    testMigrate_NewDatabase_PopulatesVersion(DataMartDataStore.ID);
  }

  @Test
  public void testMigrate_NewOperationalDataStoreDatabase_PopulatesVersion() throws Exception {
    testMigrate_NewDatabase_PopulatesVersion(OperationalDataStore.ID);
  }

  @Test
  public void testMigrate_NewThirdPartyScansDatabase_PopulatesVersion() throws Exception {
    testMigrate_NewDatabase_PopulatesVersion(ThirdPartyScansDataStore.ID);
  }

  @Test
  public void testMigrate_MigrationDisabled_ByEnvironmentVariable() throws Exception {
    environmentVariables.set(DatabaseMigrator.NXIQ_SCHEMA_MIGRATION, "false");
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/testMigrate_ByEnvironmentVariable");
    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, DatabaseName.ods.name());
    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, OperationalDataStore.ID);

    new DatabaseMigrator().migrate(databaseConfig, OperationalDataStore.ID, dataSource);

    File databaseVersionFile = H2DatabaseUtil.getDatabaseVersionFile(H2DatabaseUtil.getDatabasePath(databaseConfig));
    assertThat(databaseVersionFile).exists();
    assertThat(FileUtils.fileRead(databaseVersionFile)).isEqualTo("85");
  }

  @Test
  public void testMigrate_ForceEnableMigration_OverridesEnvironmentVariable() throws Exception {
    try {
      DatabaseMigrator.setForceEnableMigration(true);
      environmentVariables.set(DatabaseMigrator.NXIQ_SCHEMA_MIGRATION, "false");
      File databaseDir = tempDir.newFolder();
      copyDatabase(databaseDir, getClass().getSimpleName() + "/testMigrate_ByEnvironmentVariable");
      DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, DatabaseName.ods.name());
      DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, OperationalDataStore.ID);

      new DatabaseMigrator().migrate(databaseConfig, OperationalDataStore.ID, dataSource);

      File databaseVersionFile = H2DatabaseUtil.getDatabaseVersionFile(H2DatabaseUtil.getDatabasePath(databaseConfig));
      assertThat(databaseVersionFile).doesNotExist();
      assertThat(DatabaseUtil.getDatabaseSchemaVersion(dataSource, OperationalDataStore.ID)).isEqualTo(
          DatabaseMigrator.determineDesiredVersion(OperationalDataStore.ID));
    }
    finally {
      DatabaseMigrator.setForceEnableMigration(false);
    }
  }

  @Test
  public void testMigrate_MigrationEnabled_ByEnvironmentVariable() throws Exception {
    environmentVariables.set(DatabaseMigrator.NXIQ_SCHEMA_MIGRATION, "true");
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/testMigrate_ByEnvironmentVariable");
    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, DatabaseName.ods.name());
    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, OperationalDataStore.ID);

    new DatabaseMigrator().migrate(databaseConfig, OperationalDataStore.ID, dataSource);

    File databaseVersionFile = H2DatabaseUtil.getDatabaseVersionFile(H2DatabaseUtil.getDatabasePath(databaseConfig));
    assertThat(databaseVersionFile).doesNotExist();
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(dataSource, OperationalDataStore.ID)).isEqualTo(
        DatabaseMigrator.determineDesiredVersion(OperationalDataStore.ID));
  }

  @Test
  public void testMigrate_MigrationDisabled_BySystemConfigurationProperty() throws Exception {
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir,
        getClass().getSimpleName() + "/testMigrate_MigrationDisabled_BySystemConfigurationProperty");
    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, DatabaseName.ods.name());
    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, OperationalDataStore.ID);
    OperationalDataStoreProvider.initWithoutMigration(databaseConfig);
    assertThat(DatabaseUtil.systemConfigurationPropertyTableExists(dataSource)).isTrue();
    assertThat(DatabaseUtil.getSchemaMigrationEnabledFromDatabase(dataSource)).isEqualTo("false");

    new DatabaseMigrator().migrate(databaseConfig, OperationalDataStore.ID, dataSource);

    File databaseVersionFile = H2DatabaseUtil.getDatabaseVersionFile(H2DatabaseUtil.getDatabasePath(databaseConfig));
    assertThat(databaseVersionFile).exists();
    assertThat(FileUtils.fileRead(databaseVersionFile)).isEqualTo("110");
  }

  @Test
  public void testMigrate_ForceEnableMigration_OverridesSystemConfigurationProperty() throws Exception {
    try {
      DatabaseMigrator.setForceEnableMigration(true);
      File databaseDir = tempDir.newFolder();
      copyDatabase(databaseDir,
          getClass().getSimpleName() + "/testMigrate_MigrationDisabled_BySystemConfigurationProperty");
      DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, DatabaseName.ods.name());
      DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, OperationalDataStore.ID);
      OperationalDataStoreProvider.initWithoutMigration(databaseConfig);
      assertThat(DatabaseUtil.systemConfigurationPropertyTableExists(dataSource)).isTrue();
      assertThat(DatabaseUtil.getSchemaMigrationEnabledFromDatabase(dataSource)).isEqualTo("false");

      new DatabaseMigrator().migrate(databaseConfig, OperationalDataStore.ID, dataSource);

      File databaseVersionFile = H2DatabaseUtil.getDatabaseVersionFile(H2DatabaseUtil.getDatabasePath(databaseConfig));
      assertThat(databaseVersionFile).doesNotExist();
      assertThat(DatabaseUtil.getDatabaseSchemaVersion(dataSource, OperationalDataStore.ID)).isEqualTo(
          DatabaseMigrator.determineDesiredVersion(OperationalDataStore.ID));
    }
    finally {
      DatabaseMigrator.setForceEnableMigration(false);
    }
  }

  @Test
  public void testMigrate_MigrationEnabled_BySystemConfigurationProperty() throws Exception {
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir,
        getClass().getSimpleName() + "/testMigrate_MigrationEnabled_BySystemConfigurationProperty");
    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, DatabaseName.ods.name());
    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, OperationalDataStore.ID);
    OperationalDataStoreProvider.initWithoutMigration(databaseConfig);
    assertThat(DatabaseUtil.systemConfigurationPropertyTableExists(dataSource)).isTrue();
    assertThat(DatabaseUtil.getSchemaMigrationEnabledFromDatabase(dataSource)).isEqualTo("true");

    new DatabaseMigrator().migrate(databaseConfig, OperationalDataStore.ID, dataSource);

    File databaseVersionFile = H2DatabaseUtil.getDatabaseVersionFile(H2DatabaseUtil.getDatabasePath(databaseConfig));
    assertThat(databaseVersionFile).doesNotExist();
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(dataSource, OperationalDataStore.ID)).isEqualTo(
        DatabaseMigrator.determineDesiredVersion(OperationalDataStore.ID));
  }

  @Test
  public void testNoUniqueIndexesDefined() throws IOException {
    URL url = Resources.getResource("db/insight_brain_ods/schema.sql");
    assertThat(url).isNotNull();

    List<String> filesChecked = new ArrayList<>();
    for (File file : new File(url.getFile()).getParentFile().listFiles()) {
      if (file.isFile()) {
        filesChecked.add(file.getName());
        // we should be using a unique constraint (which will result in an auto-generated unique index) rather than
        // explicitly creating a unique index ourselves;
        assertThat(FileUtils.fileRead(file, StandardCharsets.UTF_8.name())).as("error in %s", file.getName())
            .doesNotContain("CREATE UNIQUE INDEX ");
      }
    }
    assertThat(filesChecked).isNotEmpty();
  }

  private void testMigrate_NewDatabase_PopulatesVersion(String databaseName) throws Exception {
    File databaseDir = tempDir.newFolder();
    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, databaseName);
    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, databaseName);
    assertThat(DatabaseUtil.schemaExists(dataSource, databaseName)).isFalse();

    new DatabaseMigrator().migrate(databaseConfig, databaseName, dataSource);

    assertThat(DatabaseUtil.schemaExists(dataSource, databaseName)).isTrue();
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(dataSource, databaseName)).isEqualTo(
        DatabaseMigrator.determineDesiredVersion(databaseName));
  }

  static class PostIncrementalMigratorVersionMinus1
      implements PostIncrementalMigrator
  {
    static boolean invoked;

    @Override
    public void migrate(DataSource dataSource) {
      invoked = true;
    }
  }

  static class PostIncrementalMigratorVersion
      implements PostIncrementalMigrator
  {
    static boolean invoked;

    @Override
    public void migrate(DataSource dataSource) {
      invoked = true;
    }
  }

  static class PostIncrementalMigratorVersionPlus1
      implements PostIncrementalMigrator
  {
    static boolean invoked;

    @Override
    public void migrate(DataSource dataSource) {
      invoked = true;
    }
  }

  static class PostIncrementalMigratorVersionDesired
      implements PostIncrementalMigrator
  {
    static boolean invoked;

    @Override
    public void migrate(DataSource dataSource) {
      invoked = true;
    }
  }

  static class PostIncrementalMigratorVersionDesiredPlus1
      implements PostIncrementalMigrator
  {
    static boolean invoked;

    @Override
    public void migrate(DataSource dataSource) {
      invoked = true;
    }
  }

  static class PostIncrementalMigratorFail
      implements PostIncrementalMigrator
  {
    @Override
    public void migrate(DataSource dataSource) throws Exception {
      throw new Exception();
    }
  }
}
