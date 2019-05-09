/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;
import java.util.zip.ZipFile;

import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseConfig;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import org.springframework.jdbc.datasource.init.ScriptStatementFailedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class DatabaseMigratorTest
    extends AbstractDatabaseTest
{
  @Test
  public void testMigrate_VersionFileUpdatedWhenMigrationFailsAfterAtLeastOneSuccessfulScript() throws Exception {
    File databaseDir = tempDir.newFolder("db");
    FileUtils.copyDirectory(new File("target/test-classes/DatabaseMigratorTest/"
        + "testMigrate_VersionFileUpdatedWhenMigrationFailsAfterAtLeastOneSuccessfulScript"), databaseDir);
    File databaseVersionFile = new File(databaseDir, "dm.ver");
    assertThat(databaseVersionFile).isFile();
    assertThat(readDatabaseVersion(databaseVersionFile)).isEqualTo("3");

    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, "dm");

    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, DatamartProvider.ID);

    // The migration should fail because schema_incremental_0007.sql drops the license_category table, but we already
    // removed this table.
    // The version file must be updated to contain the number of the last incremental script applied successfully (in
    // this case, schema_incremental_0006.sql).
    assertThatThrownBy(() -> {
      new DatabaseMigrator().migrate(databaseConfig, DatamartProvider.ID, dataSource);
    }).isInstanceOf(ScriptStatementFailedException.class);
    assertThat(readDatabaseVersion(databaseVersionFile)).isEqualTo("6");
  }

  @Test
  public void testMigrate_CreatesDatabaseBackup_DatabaseVersionStoredInTheDatabase() throws Exception {
    File databaseDir = tempDir.newFolder("db");
    FileUtils.copyDirectory(new File("target/test-classes/DatabaseMigratorTest/"
        + "testMigrate_CreatesDatabaseBackup_DatabaseVersionStoredInTheDatabase"), databaseDir);

    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, "dm");

    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, DatamartProvider.ID);

    // The migration should fail because schema_incremental_0007.sql drops the license_category table, but we already
    // removed this table.
    // The version file must be updated to contain the number of the last incremental script applied successfully (in
    // this case, schema_incremental_0006.sql).
    assertThatThrownBy(() -> {
      new DatabaseMigrator().migrate(databaseConfig, DatamartProvider.ID, dataSource);
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

    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, DatamartProvider.ID);

    // The migration should fail because schema_incremental_0007.sql drops the license_category table, but we already
    // removed this table.
    // The version file must be updated to contain the number of the last incremental script applied successfully (in
    // this case, schema_incremental_0006.sql).
    assertThatThrownBy(() -> {
      new DatabaseMigrator().migrate(databaseConfig, DatamartProvider.ID, dataSource);
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
    copyDatabase(databaseDir,  getClass().getSimpleName() + "/PostIncrementalMigrator");
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
    copyDatabase(databaseDir,  getClass().getSimpleName() + "/ZipBackupTest");

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
    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, DatamartProvider.ID);

    assertThatThrownBy(() -> {
      new DatabaseMigrator().migrate(databaseConfig, DatamartProvider.ID, dataSource);
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
    testMigrate_NewDatabase_PopulatesVersion(AggregationDataStoreProvider.ID);
  }

  @Test
  public void testMigrate_NewDatamartDatabase_PopulatesVersion() throws Exception {
    testMigrate_NewDatabase_PopulatesVersion(DatamartProvider.ID);
  }

  @Test
  public void testMigrate_NewOperationalDataStoreDatabase_PopulatesVersion() throws Exception {
    testMigrate_NewDatabase_PopulatesVersion(OperationalDataStoreProvider.ID);
  }

  private void testMigrate_NewDatabase_PopulatesVersion(String databaseName) throws Exception {
    File databaseDir = tempDir.newFolder();
    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, databaseName);
    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, databaseName);
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(dataSource, databaseName)).isEqualTo(-1);

    new DatabaseMigrator().migrate(databaseConfig, databaseName, dataSource);

    assertThat(DatabaseUtil.getDatabaseSchemaVersion(dataSource, databaseName)).isNotEqualTo(-1);
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
