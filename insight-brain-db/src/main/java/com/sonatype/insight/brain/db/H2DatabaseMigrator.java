/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

  private static final String H2_URL_PREFIX = "jdbc:h2:";

  public void migrate(DatabaseConfig databaseConfig, String databaseName, DataSource dataSource, int desiredVersion,
      int defaultCurrentVersion)
  {
    if (databaseConfig == null) {
      // In memory database, nothing to migrate.
      return;
    }

    File databaseDir = getDatabaseDir(databaseConfig);
    String databaseFilename = databaseDir.getName();
    databaseDir = databaseDir.getParentFile();
    File databaseVersionFile = new File(databaseDir, databaseFilename + ".ver");

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
      List<String> scriptNames = new ArrayList<String>();
      for (int i = currentVersion + 1; i <= desiredVersion; i++) {
        String scriptName = scriptsPath + "schema_incremental_" + String.format("%1$04d", i) + ".sql";
        scriptNames.add(scriptName);
      }
      runScripts(dataSource, scriptNames);

      FileUtils.fileWrite(databaseVersionFile, "UTF-8", String.valueOf(desiredVersion));
      new FileCleaner().delete(backupDir);
    }
    catch (IOException | SQLException e) {
      throw new RuntimeException(e);
    }
    catch (RuntimeException e) {
      throw enhanceException(e);
    }
  }

  private void runScripts(DataSource dataSource, List<String> scriptNames) throws SQLException {
    ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
    ResourceLoader resourceLoader = new DefaultResourceLoader();
    for (String scriptName : scriptNames) {
      resourceDatabasePopulator.addScript(resourceLoader.getResource(scriptName));
    }
    Connection conn = dataSource.getConnection();
    try {
      resourceDatabasePopulator.populate(conn);
    }
    finally {
      try {
        conn.close();
      }
      catch (SQLException ignored) {
      }
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

  public void runScript(DataSource dataSource, String scriptName) throws SQLException {
    runScripts(dataSource, Arrays.asList(scriptName));
  }

  private void backup(File databaseDir, String databaseName, File backupDir) throws IOException {
    for (File file : databaseDir.listFiles()) {
      if (!file.getName().startsWith(databaseName) || file.getName().equals(databaseName + ".lock.db")) {
        continue;
      }
      FileUtils.copyFile(file, new File(backupDir, file.getName()));
    }
  }

  private File getDatabaseDir(DatabaseConfig databaseConfig) {
    String url = databaseConfig.getUrl();
    if (!url.startsWith(H2_URL_PREFIX)) {
      throw new IllegalStateException("Cannot upgrade database with URL '" + url + "'");
    }

    String databaseDir = url.substring(H2_URL_PREFIX.length());
    int at = databaseDir.indexOf(';');
    if (at > 0) {
      databaseDir = databaseDir.substring(0, at);
    }
    return new File(databaseDir);
  }
}
