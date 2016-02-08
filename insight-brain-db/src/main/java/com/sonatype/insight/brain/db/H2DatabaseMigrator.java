/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.db.DatabaseConfig;

import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public class H2DatabaseMigrator
{
  private static final Logger log = LoggerFactory.getLogger(H2DatabaseMigrator.class);

  public void migrate(DatabaseConfig databaseConfig,
                      String databaseName,
                      DataSource dataSource,
                      int desiredVersion,
                      int defaultCurrentVersion)
  {
    if (databaseConfig == null) {
      // In memory database, nothing to migrate.
      return;
    }

    File databasePath = H2DatabaseUtil.getDatabasePath(databaseConfig);
    String databaseFilename = databasePath.getName();
    File databaseDir = databasePath.getParentFile();
    File databaseVersionFile = H2DatabaseUtil.getDatabaseVersionFile(databasePath);

    try {
      if (new DataSourceFactory().isNewDataSource(dataSource)) {
        FileUtils.fileWrite(databaseVersionFile, "UTF-8", String.valueOf(desiredVersion));
        return;
      }

      int currentVersion;
      if (databaseVersionFile.exists()) {
        String sCurrentVersion = FileUtils.fileRead(databaseVersionFile, "UTF-8").trim();
        currentVersion = Integer.parseInt(sCurrentVersion);
      }
      else {
        currentVersion = defaultCurrentVersion;
      }

      log.info("Current version of database {}: {}", databaseFilename, currentVersion);
      if (currentVersion >= desiredVersion) {
        return;
      }

      log.info("Migrating database {} to version: {}", databaseFilename, desiredVersion);
      log.info(" Database dir: {}", databaseDir);

      File backupDir = new File(databaseDir, "backup");
      if (backupDir.exists()) {
        throw new IllegalStateException(
            "Cannot migrate database. The backup directory '"
                + backupDir.getAbsolutePath()
                + "' already exists, indicating that a previous migration failed. Please contact support for further assistance.");
      }
      backup(databaseDir, databaseFilename, backupDir);

      String scriptsPath = "/db/" + databaseName + "/";
      for (int i = currentVersion + 1; i <= desiredVersion; i++) {
        String scriptName = scriptsPath + "schema_incremental_" + String.format("%1$04d", i) + ".sql";
        runScript(dataSource, scriptName);
        FileUtils.fileWrite(databaseVersionFile, "UTF-8", String.valueOf(i));
      }

      new FileCleaner().delete(backupDir);
    }
    catch (IOException | SQLException e) {
      throw new RuntimeException(e);
    }
    catch (RuntimeException e) {
      throw enhanceException(e);
    }
  }

  public void runScript(DataSource dataSource, String scriptName) throws SQLException {
    ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
    ResourceLoader resourceLoader = new DefaultResourceLoader();
    resourceDatabasePopulator.addScript(resourceLoader.getResource(scriptName));
    try (Connection conn = dataSource.getConnection()) {
      resourceDatabasePopulator.populate(conn);
    }
  }

  private RuntimeException enhanceException(RuntimeException exception) {
    String reason = createCustomErrorMessage(exception.getMessage());
    if (reason != null) {
      return new RuntimeException(reason, exception);
    }

    return exception;
  }

  private String createCustomErrorMessage(String message) {
    if (message != null && message.contains("db/insight_brain_ods/schema_incremental_0039.sql")) {
      return "Failed to migrate database, exception likely caused by applications without parent organizations. Please contact support for further assistance.";
    }

    return null;
  }

  private void backup(File databaseDir, String databaseName, File backupDir) throws IOException {
    for (File file : databaseDir.listFiles()) {
      if (!file.getName().startsWith(databaseName) || file.getName().equals(databaseName + ".lock.db")) {
        continue;
      }
      FileUtils.copyFile(file, new File(backupDir, file.getName()));
    }
  }
}
