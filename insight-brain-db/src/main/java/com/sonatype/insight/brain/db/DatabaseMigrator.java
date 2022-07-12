/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseEngine;
import com.sonatype.insight.db.H2DatabaseEngine;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public class DatabaseMigrator
{
  private static final Logger log = LoggerFactory.getLogger(DatabaseMigrator.class);

  public void migrate(DatabaseConfig databaseConfig, String databaseName, DataSource dataSource) {
    migrate(databaseConfig, databaseName, dataSource, null /* upgradeGuard */);
  }

  public void migrate(DatabaseConfig databaseConfig,
                      String databaseName,
                      DataSource dataSource,
                      IntConsumer upgradeGuard)
  {
    if (databaseConfig == null) {
      // In memory database, nothing to migrate.
      return;
    }

    try {
      int desiredVersion = getDesiredVersion(databaseName);

      if (new DataSourceFactory().isNewDataSource(dataSource)) {
        // This is a new database, nothing to migrate here.
        DatabaseUtil.updateDatabaseSchemaVersion(dataSource, databaseName, desiredVersion);
        return;
      }

      File databaseVersionFile = null;

      // The database exists and it may require migration.
      int currentVersion;
      if (DatabaseUtil.schemaVersionTableExists(dataSource, databaseName)) {
        currentVersion = DatabaseUtil.getDatabaseSchemaVersion(dataSource, databaseName);
      }
      else {
        File databasePath = H2DatabaseUtil.getDatabasePath(databaseConfig);
        databaseVersionFile = H2DatabaseUtil.getDatabaseVersionFile(databasePath);
        if (databaseVersionFile.exists()) {
          String sCurrentVersion = FileUtils.fileRead(databaseVersionFile, "UTF-8").trim();
          currentVersion = Integer.parseInt(sCurrentVersion);
        }
        else {
          throw new IllegalStateException(
              "Missing the database schema version either in the database itself or in the database version file " +
                  databaseVersionFile + ".");
        }
      }

      log.info("Current version of database {}: {}", databaseName, currentVersion);
      if (currentVersion > desiredVersion) {
        throw new IllegalStateException("Database schema " + databaseName + " was created by a newer product version. "
            + "Please upgrade your IQ Server or restore a database backup taken by your current version.");
      }

      if (currentVersion == desiredVersion) {
        return;
      }

      if (upgradeGuard != null) {
        upgradeGuard.accept(currentVersion);
      }

      log.info("Migrating database schema {} to version: {}", databaseName, desiredVersion);

      File backupDir = null;
      DatabaseEngine databaseEngine = DataSourceFactory.getDatabaseEngine(dataSource);
      if (H2DatabaseEngine.INSTANCE.equals(databaseEngine)) {
        File databasePath = H2DatabaseUtil.getDatabasePath(databaseConfig);
        File databaseDir = databasePath.getParentFile();
        backupDir = new File(databaseDir, "backup");
        if (backupDir.exists()) {
          throw new IllegalStateException(
              "Cannot migrate database. The backup directory '" + backupDir.getAbsolutePath() + "' already exists"
                  + ", indicating that a previous migration failed. Please contact support for further assistance.");
        }
        log.info("Creating backup of database {} in {}", databaseName, backupDir);
        backup(databaseDir, databasePath.getName(), backupDir);
      }

      String setSchemaSql = databaseEngine.buildSetSchemaSql(databaseName);
      for (int i = currentVersion + 1; i <= desiredVersion; i++) {
        String scriptName = getIncrementalFileName(databaseName, "sql", i);
        runScript(dataSource, setSchemaSql, scriptName);
        String postIncrementalMigratorFileName = getIncrementalFileName(databaseName, "cls", i);
        runPostIncrementalMigrator(postIncrementalMigratorFileName, dataSource);
        if (DatabaseUtil.schemaVersionTableExists(dataSource, databaseName)) {
          DatabaseUtil.updateDatabaseSchemaVersion(dataSource, databaseName, i);
        }
        else {
          FileUtils.fileWrite(databaseVersionFile, "UTF-8", String.valueOf(i));
        }
      }

      FileCleaner fileCleaner = new FileCleaner();
      if (databaseVersionFile != null) {
        fileCleaner.delete(databaseVersionFile);
      }
      if (backupDir != null) {
        log.info("Deleting backup of database {} from {}", databaseName, backupDir);
        fileCleaner.delete(backupDir);
      }
    }
    catch (IOException | SQLException e) {
      throw new RuntimeException(e);
    }
  }

  // Visible for testing
  public int getDesiredVersion(String databaseName) {
    return DatabaseMigrator.determineDesiredVersion(databaseName);
  }

  private static String getIncrementalFileName(String databaseName, String extension, int scriptIndex) {
    return "/db/" + databaseName + "/schema_incremental_" + String.format("%1$04d", scriptIndex) + "." + extension;
  }

  // Public visibility for tests only.
  public static int determineDesiredVersion(String databaseName) {
    boolean foundScripts = false;
    for (int version = 1; version < 10000; version++) {
      Resource incrementalScript = loadIncrementalScriptResource(getIncrementalFileName(databaseName, "sql", version));
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

  void runPostIncrementalMigrator(String postIncrementalMigratorFileName, DataSource dataSource) {
    try (InputStream is = getClass().getResourceAsStream(postIncrementalMigratorFileName)) {
      if (is != null) {
        Class<?> c = Class.forName(IOUtil.toString(is, "UTF-8").trim());
        PostIncrementalMigrator migrator = c.asSubclass(PostIncrementalMigrator.class).newInstance();
        migrator.migrate(dataSource);
      }
    }
    catch (Exception e) {
      throw new RuntimeException(
          "Failed to execute the " + PostIncrementalMigrator.class.getSimpleName() + " referenced in " +
              postIncrementalMigratorFileName + ".", e);
    }
  }

  public void runScript(DataSource dataSource, String setSchemaSql, String scriptName) throws SQLException {
    ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
    resourceDatabasePopulator.addScript(loadIncrementalScriptResource(scriptName));
    try (Connection conn = dataSource.getConnection()) {
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
