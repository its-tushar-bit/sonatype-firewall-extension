/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.db.DatabaseConfig;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

public class H2DatabaseBackupTest
    extends AbstractDatabaseTest
{
  @TempDir
  public Path tempFolder;

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "H2DatabaseBackupTest/testBackupOperationalDataStore")
  public void testBackup() throws Exception {
    DatabaseConfig databaseConfig = getDatabaseConfig("ods");

    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setDriverClassName(databaseConfig.getDriverClassName());
    dataSource.setUrl(databaseConfig.getUrl());
    dataSource.setUsername(databaseConfig.getUsername());
    dataSource.setPassword(databaseConfig.getPassword());

    File dbBackupDir = Files.createDirectories(tempFolder.resolve("backup")).toFile();
    new H2DatabaseBackup().backup(databaseConfig, dataSource, dbBackupDir);

    File dbBackupFile = new File(dbBackupDir, DatabaseName.ods + H2DatabaseBackup.BACKUP_FILENAME_SUFFIX);
    try (ZipFile dbBackupZipFile = new ZipFile(dbBackupFile)) {
      ZipEntry zipEntryDb = dbBackupZipFile.getEntry("ods.h2.db");
      assertThat(zipEntryDb).as("The db file is missing from the db backup").isNotNull();
      assertThat(getZipEntryContent(dbBackupZipFile, zipEntryDb))
          .isEqualTo(getFileContent(new File(getDatabasePath(), "ods.h2.db")));
    }

    File restoreIntructionsFile = new File(dbBackupDir, H2DatabaseBackup.RESTORE_FILENAME);
    assertThat(restoreIntructionsFile).as("The restore instructions file is missing from the db backup").isFile();
  }

  private byte[] getZipEntryContent(ZipFile zipFile, ZipEntry zipEntry) throws IOException {
    try (InputStream is = zipFile.getInputStream(zipEntry)) {
      return IOUtils.toByteArray(is);
    }
  }

  private byte[] getFileContent(File file) throws IOException {
    try (InputStream is = Files.newInputStream(file.toPath())) {
      return IOUtils.toByteArray(is);
    }
  }
}
