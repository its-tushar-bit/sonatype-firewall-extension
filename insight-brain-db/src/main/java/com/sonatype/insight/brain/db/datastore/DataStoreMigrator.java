/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.IntConsumer;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.sql.DataSource;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datasource.LegacyDataSourceProvider;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.H2DatabaseUtil;
import com.sonatype.insight.brain.db.PostIncrementalMigrator;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseEngine;
import com.sonatype.insight.db.H2DatabaseEngine;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * Migrate an individual {@link DataStore}
 */
public class DataStoreMigrator
{
  private static final Logger log = LoggerFactory.getLogger(DataStoreMigrator.class);

  private final DataStore dataStore;

  public DataStoreMigrator(final DataStore dataStore) {
    this.dataStore = dataStore;
  }

  public void migrate(final Boolean migrateToNewViolationModel) {
    DataSource dataSource = dataStore.getDataSource();
    String dataStoreId = dataStore.getID();
    String databaseSchema = dataStore.getDatabaseSchema();
    DatabaseConfig databaseConfig = dataStore.getDatabaseConfig();

    try {
      int desiredVersion = getDesiredVersion(dataStoreId);

      if (isNewDatabase(dataSource, DatabaseUtil.getDatabaseEngine(dataSource), dataStoreId, databaseSchema)) {
        // This is a new database, nothing to migrate here as the population (schema.sql) is the latest
        DatabaseUtil.updateDatabaseSchemaVersion(dataSource, dataStoreId, databaseSchema, desiredVersion);
        return;
      }

      File databaseVersionFile = null;

      // The database exists and it may require migration.
      int currentVersion;
      if (DatabaseUtil.schemaVersionTableExists(dataSource, databaseSchema)) {
        currentVersion = DatabaseUtil.getDatabaseSchemaVersion(dataSource, dataStoreId, databaseSchema);
      }
      else {
        File databasePath = H2DatabaseUtil.getDatabasePath(databaseConfig);
        databaseVersionFile = H2DatabaseUtil.getDatabaseVersionFile(databasePath);
        if (databaseVersionFile.exists()) {
          String sCurrentVersion = FileUtils.readFileToString(databaseVersionFile, StandardCharsets.UTF_8).trim();
          currentVersion = Integer.parseInt(sCurrentVersion);
        }
        else {
          throw new IllegalStateException(
              "Missing the database schema version either in the database itself or in the database version file " +
                  databaseVersionFile + ".");
        }
      }

      log.info("Current version of database schema {}/{}: {}", dataStoreId, databaseSchema, currentVersion);
      if (currentVersion > desiredVersion) {
        throw new IllegalStateException(
            "Database schema " + databaseSchema + " was created by a newer product version. "
                + "Please upgrade your IQ Server or restore a database backup taken by your current version.");
      }

      if (currentVersion == desiredVersion) {
        return;
      }

      IntConsumer upgradeGuard = dataStore.getUpgradeGuard(migrateToNewViolationModel);
      if (upgradeGuard != null) {
        upgradeGuard.accept(currentVersion);
      }

      log.info("Migrating database schema {} from version {} to version: {}", databaseSchema, currentVersion,
          desiredVersion);

      File backupDir = null;
      DatabaseEngine databaseEngine = DatabaseUtil.getDatabaseEngine(dataSource);
      if (H2DatabaseEngine.INSTANCE.equals(databaseEngine)) {
        File databasePath = H2DatabaseUtil.getDatabasePath(databaseConfig);
        File databaseDir = databasePath.getParentFile();
        backupDir = new File(databaseDir, "backup");
        if (backupDir.exists()) {
          throw new IllegalStateException(
              "Cannot migrate database. The backup directory '" + backupDir.getAbsolutePath() + "' already exists"
                  + ", indicating that a previous migration failed. Please contact support for further assistance.");
        }
        log.info("Creating backup of database schema {} in {}", databaseSchema, backupDir);
        backup(databaseDir, databasePath.getName(), backupDir);
      }

      String setSchemaSql = databaseEngine.buildSetSchemaSql(databaseSchema);
      for (int i = currentVersion + 1; i <= desiredVersion; i++) {
        String scriptName = getIncrementalFileName(dataStoreId, "sql", i);
        runScript(setSchemaSql, scriptName);
        String postIncrementalMigratorFileName = getIncrementalFileName(dataStoreId, "cls", i);
        runPostIncrementalMigrator(postIncrementalMigratorFileName, dataSource, databaseSchema);
        if (DatabaseUtil.schemaVersionTableExists(dataSource, databaseSchema)) {
          DatabaseUtil.updateDatabaseSchemaVersion(dataSource, dataStoreId, databaseSchema, i);
        }
        else {
          FileUtils.writeStringToFile(databaseVersionFile, String.valueOf(i), StandardCharsets.UTF_8);
        }
      }

      FileCleaner fileCleaner = new FileCleaner();
      if (databaseVersionFile != null) {
        fileCleaner.delete(databaseVersionFile);
      }
      if (backupDir != null) {
        log.info("Deleting backup of database {} from {}", databaseSchema, backupDir);
        fileCleaner.delete(backupDir);
      }
    }
    catch (IOException | SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isNewDatabase(
      DataSource dataSource,
      DatabaseEngine databaseEngine,
      String dataStoreId,
      String databaseSchema)
  {
    // populateDbSchema returns true if the db is new and populated

    // TODO - currently the new implementations of the new DataSourceProvider all implement LegacyDataSourceProvider
    // Once the new liquibase migrator is in place we can remove this
    DataSourceProvider dataSourceProvider = dataStore.getDataSourceProvider();
    if (dataSourceProvider instanceof LegacyDataSourceProvider) {
      LegacyDataSourceProvider legacyDataSourceProvider = (LegacyDataSourceProvider) dataSourceProvider;
      return legacyDataSourceProvider.populateDbSchema(dataSource, databaseEngine, dataStoreId, databaseSchema);
    }

    throw new RuntimeException("DataStoreMigrator currently only supports LegacyDataSourceProvider");
  }

  // Visible for testing
  public int getDesiredVersion(String dataStoreId) {
    return DataStoreMigrator.determineDesiredVersion(dataStoreId);
  }

  private static String getIncrementalFileName(String dataStoreId, String extension, int scriptIndex) {
    return "/db/" + dataStoreId + "/schema_incremental_" + String.format("%1$04d", scriptIndex) + "." + extension;
  }

  // Public visibility for tests only.
  public static int determineDesiredVersion(String dataStoreId) {
    boolean foundScripts = false;
    for (int version = 1; version < 10000; version++) {
      Resource incrementalScript =
          loadIncrementalScriptResource(getIncrementalFileName(dataStoreId, "sql", version));
      if (incrementalScript.exists()) {
        foundScripts = true;
      }
      else if (foundScripts) {
        return version - 1;
      }
    }
    // There are no incremental scripts.
    return 1;
  }

  void runPostIncrementalMigrator(
      final String postIncrementalMigratorFileName,
      final DataSource dataSource,
      final String databaseSchema)
  {
    try (InputStream is = getClass().getResourceAsStream(postIncrementalMigratorFileName)) {
      if (is != null) {
        Class<?> c = Class.forName(IOUtils.toString(is, StandardCharsets.UTF_8).trim());
        PostIncrementalMigrator migrator = c.asSubclass(PostIncrementalMigrator.class).newInstance();
        migrator.migrate(dataSource, databaseSchema);
      }
    }
    catch (Exception e) {
      throw new RuntimeException(
          "Failed to execute the " + PostIncrementalMigrator.class.getSimpleName() + " referenced in " +
              postIncrementalMigratorFileName + ".", e);
    }
  }

  public void runScript(final String setSchemaSql, final String scriptName)
      throws SQLException
  {
    ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
    resourceDatabasePopulator.addScript(loadIncrementalScriptResource(scriptName));
    try (Connection conn = dataStore.getDataSource().getConnection()) {
      try (Statement statement = conn.createStatement()) {
        statement.execute(setSchemaSql);
      }
      resourceDatabasePopulator.populate(conn);
    }
  }

  // Package visibility for tests only.
  void backup(File databaseDir, String databaseName, File backupDir) throws IOException {
    File[] targets = databaseDir.listFiles(file -> file.isFile() && file.getName().startsWith(databaseName)
        && !file.getName().equals(databaseName + ".lock.db"));

    if (targets.length > 0) {
      Files.createDirectories(backupDir.toPath());
      File dbBackupZip = new File(backupDir, databaseName + ".zip");
      try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(dbBackupZip))) {
        zipOut.setLevel(Deflater.BEST_SPEED);
        for (File file : targets) {
          zipOut.putNextEntry(new ZipEntry(file.getName()));
          Files.copy(file.toPath(), zipOut);
        }
      }
    }
  }

  private static Resource loadIncrementalScriptResource(String scriptName) {
    return new DefaultResourceLoader().getResource(scriptName);
  }
}
