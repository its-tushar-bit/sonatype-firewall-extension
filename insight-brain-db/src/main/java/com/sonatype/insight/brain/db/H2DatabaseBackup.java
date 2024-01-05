/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseConfig;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a backup of an H2 database. The backup includes instructions for restoring the database from the backup.
 *
 * @since 1.15.0
 */
public class H2DatabaseBackup
{
  private static final Logger log = LoggerFactory.getLogger(H2DatabaseBackup.class);

  public static final String BACKUP_FILENAME_SUFFIX = "-db-backup.zip";

  static final String RESTORE_FILENAME = "restore-instructions.txt";

  private static final String NEW_LINE = "\n";

  public void backup(DatabaseConfig databaseConfig, DataSource dataSource, File dbBackupDir) {
    long start = System.currentTimeMillis();

    log.debug("Creating database backup '{}'...", dbBackupDir.getAbsolutePath());

    if (DatabaseUtil.isInMemoryDatabase(databaseConfig)) {
      throw new IllegalArgumentException("Cannot backup an in-memory H2 database.");
    }

    File databasePath = H2DatabaseUtil.getDatabasePath(databaseConfig);
    String databaseName = databasePath.getName();
    File dbBackupFile = new File(dbBackupDir, databaseName + BACKUP_FILENAME_SUFFIX);

    createDbBackup(dataSource, dbBackupFile);
    createDbRestoreIntructions(databasePath, dbBackupDir);

    log.debug("Created database backup '{}' in {} ms.", dbBackupDir.getAbsolutePath(), System.currentTimeMillis()
        - start);
  }

  private void createDbBackup(DataSource dataSource, File dbBackupFile) {
    try (Connection connection = dataSource.getConnection()) {
      try (Statement statement = connection.createStatement()) {
        statement.execute("BACKUP TO '" + dbBackupFile.getAbsolutePath() + "'");
      }
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private void createDbRestoreIntructions(File databasePath, File dbBackupDir) {
    File restoreInstructionsFile = new File(dbBackupDir, RESTORE_FILENAME);
    try {
      StringBuilder instructions = new StringBuilder();
      instructions.append("Backup for database at: ").append(databasePath).append(NEW_LINE).append(NEW_LINE);
      instructions.append(
          "To restore the database from this backup, unzip the backup zip file into the desired database location.")
          .append(NEW_LINE);
      instructions.append("The Nexus IQ Server must be stopped before the database is restored.").append(NEW_LINE);
      FileUtils.writeStringToFile(restoreInstructionsFile, instructions.toString(), StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
