/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import java.io.File;
import java.nio.file.Files;
import java.util.zip.ZipFile;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.PostIncrementalMigrator;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datasource.DataSourceProviderFactory;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.H2DatabaseEngine;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.datasource.init.ScriptStatementFailedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class LegacyDataStoreMigratorTest
    extends AbstractDatabaseTest
{
  @TempDir
  public File tempFolder;

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "DataStoreMigratorTest/" +
          "testMigrate_VersionFileUpdatedWhenMigrationFailsAfterAtLeastOneSuccessfulScript")
  public void testMigrate_VersionFileUpdatedWhenMigrationFailsAfterAtLeastOneSuccessfulScript() throws Exception {
    File databaseVersionFile = new File(getDatabasePath(), "dm.ver");
    assertThat(databaseVersionFile).isFile();
    assertThat(readDatabaseVersion(databaseVersionFile)).isEqualTo("3");

    // The migration should fail because schema_incremental_0007.sql drops the license_category table, but we already
    // removed this table.
    // The version file must be updated to contain the number of the last incremental script applied successfully (in
    // this case, schema_incremental_0006.sql).
    assertThatThrownBy(
        () -> runDataStoreMigrator(databaseRule.getDataMartDataStore())).isInstanceOf(
            ScriptStatementFailedException.class);
    assertThat(readDatabaseVersion(databaseVersionFile)).isEqualTo("6");
  }

  @Test
  @H2DiskTest(suppressMigrations = true, copyExistingDatabase = "DataStoreMigratorTest/" +
      "testMigrate_CreatesDatabaseBackup_DatabaseVersionStoredInTheDatabase")
  public void testMigrate_CreatesDatabaseBackup_DatabaseVersionStoredInTheDatabase() throws Exception {
    // The migration should fail because schema_incremental_0007.sql drops the license_category table, but we already
    // removed this table.
    // The version file must be updated to contain the number of the last incremental script applied successfully (in
    // this case, schema_incremental_0006.sql).
    assertThatThrownBy(
        () -> runDataStoreMigrator(databaseRule.getDataMartDataStore())).isInstanceOf(
            ScriptStatementFailedException.class);

    File backupFile = new File(getDatabasePath(), "backup/dm.zip");
    assertThat(backupFile).isFile();
  }

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "DataStoreMigratorTest/" +
          "testMigrate_CreatesDatabaseBackup_DatabaseVersionStoredInSeparateFile")
  public void testMigrate_CreatesDatabaseBackup_DatabaseVersionStoredInSeparateFile() throws Exception {
    // The migration should fail because schema_incremental_0007.sql drops the license_category table, but we already
    // removed this table.
    // The version file must be updated to contain the number of the last incremental script applied successfully (in
    // this case, schema_incremental_0006.sql).
    assertThatThrownBy(
        () -> runDataStoreMigrator(databaseRule.getDataMartDataStore())).isInstanceOf(
            ScriptStatementFailedException.class);

    File backupFile = new File(getDatabasePath(), "backup/dm.zip");
    assertThat(backupFile).isFile();
  }

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "DataStoreMigratorTest/" +
          "testMigrate_VersionUpdatedWhenMigrationFailsAfterAtLeastOneSuccessfulScript")
  public void testMigrate_VersionUpdatedWhenMigrationFailsAfterAtLeastOneSuccessfulScript() throws Exception {
    // Note - this test is an H2 database named `test.h2.db` which contains a schema named this
    DatabaseConfig databaseConfig = getDatabaseConfig("test");
    String schemaName = "testMigrate_VersionUpdatedWhenMigrationFailsAfterAtLeastOneSuccessfulScript";

    DataSource dataSource = DataSourceProviderFactory
        .createDataSourceProvider(H2DatabaseEngine.INSTANCE)
        .getDataSource(databaseConfig, schemaName);

    assertThat(DatabaseUtil.getLegacyDatabaseSchemaVersion(dataSource, schemaName, schemaName)).isEqualTo(1);

    // Should fail to upgrade to version 3 because it tries to drop a non-existing table
    assertThatThrownBy(() -> runDataStoreMigrator(new TestDataStore(databaseConfig, schemaName, schemaName)))
        .isInstanceOf(ScriptStatementFailedException.class);
    assertThat(DatabaseUtil.getLegacyDatabaseSchemaVersion(dataSource, schemaName, schemaName)).isEqualTo(2);
  }

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "DataStoreMigratorTest/PostIncrementalMigrator")
  public void testMigrate_RunsPostIncrementalMigrators() throws Exception {
    DatabaseConfig databaseConfig = getDatabaseConfig("test");
    File databaseVersionFile = getDatabaseVersionFile(getDatabasePath(), "test");
    int desiredVersion = 12;
    assertThat(readDatabaseVersion(databaseVersionFile)).isEqualTo(String.valueOf(desiredVersion - 2));

    runDataStoreMigrator(new TestDataStore(databaseConfig, "PostIncrementalMigrator", "PostIncrementalMigrator"));

    assertThat(PostIncrementalMigratorVersionMinus1.invoked).isFalse();
    assertThat(PostIncrementalMigratorVersion.invoked).isFalse();
    assertThat(PostIncrementalMigratorVersionPlus1.invoked).isTrue();
    assertThat(PostIncrementalMigratorVersionDesired.invoked).isTrue();
    assertThat(PostIncrementalMigratorVersionDesiredPlus1.invoked).isFalse();
  }

  @Test
  @H2DiskTest(suppressMigrations = true, copyExistingDatabase = "DataStoreMigratorTest/ZipBackupTest")
  public void testMigrate_ZipBackups() throws Exception {
    File databaseDir = getDatabasePath();

    String dbName = "test";
    File backupDir = Files.createDirectory(tempFolder.toPath().resolve("backup")).toFile();

    // TODO
    newDataStoreMigrator(new TestDataStore(null, "test", "test")).backup(databaseDir, dbName, backupDir);

    File backupZip = new File(backupDir, dbName + ".zip");
    assertThat(backupZip).isFile();

    try (ZipFile zipFile = new ZipFile(backupZip)) {
      assertThat(zipFile.size()).isEqualTo(2);
      assertThat(zipFile.getEntry("test.h2.db")).isNotNull();
      assertThat(zipFile.getEntry("test.ver")).isNotNull();
    }
  }

  @Test
  @H2DiskTest(suppressMigrations = true, copyExistingDatabase = "DataStoreMigratorTest/MissingVersion")
  public void testMigrate_MissingVersion() throws Exception {
    File databaseDir = getDatabasePath();
    File databaseVersionFile = getDatabaseVersionFile(databaseDir, "test");
    assertThat(databaseVersionFile).doesNotExist();
    DatabaseConfig databaseConfig = getDatabaseConfig("test");
    DataSource dataSource = DataSourceProviderFactory
        .createDataSourceProvider(H2DatabaseEngine.INSTANCE)
        .getDataSource(databaseConfig, "MissingVersion");

    assertThat(DatabaseUtil.legacySchemaVersionTableExists(dataSource, "MissingVersion")).isFalse();

    assertThatThrownBy(
        () -> runDataStoreMigrator(new TestDataStore(databaseConfig, "MissingVersion", "MissingVersion")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "Missing the database schema version either in the database itself or in the database version " +
                    "file " + databaseVersionFile + ".");
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testMigrate_OperationalDataStore_ThrowsExceptionDuringExecute() {
    assertThatThrownBy(() -> newDataStoreMigrator(databaseRule.getOperationalDataStore())
        .runPostIncrementalMigrator("/DataStoreMigratorTest/"
            + "testMigrate_OperationalDataStore_ThrowsExecuteExceptionMessage/schema_incremental_0089.cls",
            mock(DataSource.class), databaseRule.getOperationalDataStore().getDatabaseSchema())).isInstanceOf(
                RuntimeException.class)
                .hasMessage("Failed to execute the PostIncrementalMigrator referenced in "
                    + "/DataStoreMigratorTest/testMigrate_OperationalDataStore_ThrowsExecuteExceptionMessage/"
                    + "schema_incremental_0089.cls.");
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testMigrate_OperationalDataStore_ThrowsExceptionDuringLoad() {
    assertThatThrownBy(() -> newDataStoreMigrator(databaseRule.getOperationalDataStore())
        .runPostIncrementalMigrator("/DataStoreMigratorTest/"
            + "testMigrate_OperationalDataStore_ThrowsLoadExceptionMessage/schema_incremental_0090.cls",
            mock(DataSource.class), databaseRule.getOperationalDataStore().getDatabaseSchema()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to execute the PostIncrementalMigrator referenced in "
                    + "/DataStoreMigratorTest/testMigrate_OperationalDataStore_ThrowsLoadExceptionMessage/"
                    + "schema_incremental_0090.cls.");
  }

  @Test
  public void testDetermineDesiredVersion() {
    assertThat(LegacyDataStoreMigrator.determineDesiredVersion("DetermineDesiredVersion")).isEqualTo(12);
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testMigrate_NewAggregationDatabase_PopulatesVersion() throws Exception {
    testMigrate_NewDatabase_PopulatesVersion(databaseRule.getAggregationDataStore());
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testMigrate_NewDatamartDatabase_PopulatesVersion() throws Exception {
    testMigrate_NewDatabase_PopulatesVersion(databaseRule.getDataMartDataStore());
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testMigrate_NewOperationalDataStoreDatabase_PopulatesVersion() throws Exception {
    testMigrate_NewDatabase_PopulatesVersion(databaseRule.getOperationalDataStore());
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testMigrate_NewThirdPartyScansDatabase_PopulatesVersion() throws Exception {
    testMigrate_NewDatabase_PopulatesVersion(databaseRule.getThirdPartyScansDataStore());
  }

  private void testMigrate_NewDatabase_PopulatesVersion(final DataStore dataStore) throws Exception {
    DataSource dataSource = dataStore.getDataSource();
    assertThat(DatabaseUtil.schemaExists(dataSource, dataStore.getDatabaseSchema())).isFalse();

    newDataStoreMigrator(dataStore).migrate();

    assertThat(DatabaseUtil.schemaExists(dataSource, dataStore.getDatabaseSchema())).isTrue();
    assertThat(
        DatabaseUtil.getLegacyDatabaseSchemaVersion(dataStore)).isEqualTo(
            LegacyDataStoreMigrator.determineDesiredVersion(dataStore.getDatabaseSchema()));
  }

  private LegacyDataStoreMigrator newDataStoreMigrator(final DataStore dataStore) {
    return new LegacyDataStoreMigrator(dataStore);
  }

  private void runDataStoreMigrator(final DataStore dataStore) {
    LegacyDataStoreMigrator legacyDataStoreMigrator = new LegacyDataStoreMigrator(dataStore);
    legacyDataStoreMigrator.migrate();
  }

  private class TestDataStore
      implements DataStore
  {
    private final DatabaseConfig databaseConfig;

    private final String dataStoreId;

    private final String databaseSchema;

    public TestDataStore(final DatabaseConfig databaseConfig, String dataStoreId, final String databaseSchema) {
      this.databaseConfig = databaseConfig;
      this.dataStoreId = dataStoreId;
      this.databaseSchema = databaseSchema;
    }

    @Override
    public String getID() {
      return dataStoreId;
    }

    @Override
    public void initialize() {
      throw new NotImplementedException();
    }

    @Override
    public DataSource getDataSource() {
      return DataSourceProviderFactory
          .createDataSourceProvider(H2DatabaseEngine.INSTANCE)
          .getDataSource(databaseConfig, dataStoreId);
    }

    @Override
    public String getDatabaseSchema() {
      return databaseSchema;
    }

    @Override
    public DatabaseConfig getDatabaseConfig() {
      return databaseConfig;
    }

    @Override
    public DataSourceProvider getDataSourceProvider() {
      return DataSourceProviderFactory
          .createDataSourceProvider(H2DatabaseEngine.INSTANCE);
    }

    @Override
    public boolean isDataStoreNew() {
      return false;
    }

    @Override
    public boolean isDatabaseEmbedded() {
      return true;
    }

    @Override
    public void close() throws Exception {
      // No-op for test data store
    }
  }

  static class PostIncrementalMigratorVersionMinus1
      implements PostIncrementalMigrator
  {
    static boolean invoked;

    @Override
    public void migrate(DataSource dataSource, String databaseSchema) {
      invoked = true;
    }
  }

  static class PostIncrementalMigratorVersion
      implements PostIncrementalMigrator
  {
    static boolean invoked;

    @Override
    public void migrate(DataSource dataSource, String databaseSchema) {
      invoked = true;
    }
  }

  static class PostIncrementalMigratorVersionPlus1
      implements PostIncrementalMigrator
  {
    static boolean invoked;

    @Override
    public void migrate(DataSource dataSource, String databaseSchema) {
      invoked = true;
    }
  }

  static class PostIncrementalMigratorVersionDesired
      implements PostIncrementalMigrator
  {
    static boolean invoked;

    @Override
    public void migrate(DataSource dataSource, String databaseSchema) {
      invoked = true;
    }
  }

  static class PostIncrementalMigratorVersionDesiredPlus1
      implements PostIncrementalMigrator
  {
    static boolean invoked;

    @Override
    public void migrate(DataSource dataSource, String databaseSchema) {
      invoked = true;
    }
  }

  static class PostIncrementalMigratorFail
      implements PostIncrementalMigrator
  {
    @Override
    public void migrate(DataSource dataSource, String databaseSchema) throws Exception {
      throw new Exception();
    }
  }

  @Test
  public void testGetDatabasePath_NonH2DatabaseURL() {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setUrl("jdbc:postgresql://localhost:5432/iq_db");
    databaseConfig.setUsername("postgres");

    assertThatThrownBy(() -> com.sonatype.insight.brain.db.H2DatabaseUtil.getDatabasePath(databaseConfig))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot process non-H2 database")
        .hasMessageContaining("jdbc:postgresql://localhost:5432/iq_db")
        .hasMessageContaining("Check database configuration")
        .hasMessageContaining("properly initialized");
  }
}
