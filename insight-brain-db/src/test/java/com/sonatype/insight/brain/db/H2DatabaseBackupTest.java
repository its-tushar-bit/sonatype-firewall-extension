/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.sonatype.insight.db.DatabaseConfig;

import org.apache.commons.dbcp.BasicDataSource;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class H2DatabaseBackupTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @After
  public void cleanup() {
    DataSourceFactory.clear_ForTestsOnly();
  }

  @Test
  public void testBackup() throws Exception {
    File databaseDir = temporaryFolder.newFolder("db");
    FileUtils.copyDirectory(new File("target/test-classes/H2DatabaseBackupTest/testBackupOperationalDataStore"),
        databaseDir);
    File databaseVersionFile = new File(databaseDir, "ods.ver");
    assertTrue(databaseVersionFile.exists());
    assertEquals("6", FileUtils.fileRead(databaseVersionFile));

    String dbUrl = "jdbc:h2:" + databaseDir.getAbsolutePath()
        + "/ods;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setUrl(dbUrl);

    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setDriverClassName("org.h2.Driver");
    dataSource.setUrl(dbUrl);
    dataSource.setUsername("sa");
    dataSource.setPassword("");

    File dbBackupDir = temporaryFolder.newFolder("backup");
    new H2DatabaseBackup().backup(databaseConfig, dataSource, dbBackupDir);

    File dbBackupFile = new File(dbBackupDir, "ods" + H2DatabaseBackup.BACKUP_FILENAME_SUFFIX);
    try (ZipFile dbBackupZipFile = new ZipFile(dbBackupFile)) {
      ZipEntry zipEntryDb = dbBackupZipFile.getEntry("ods.h2.db");
      assertThat("The db file is missing from the db backup", zipEntryDb, notNullValue());
      assertThat(new String(getZipEntryContent(dbBackupZipFile, zipEntryDb), "UTF-8"),
          is(FileUtils.fileRead(new File(databaseDir, "ods.h2.db"))));

      ZipEntry zipEntryDbVersion = dbBackupZipFile.getEntry("ods.ver");
      assertThat("The db version file is missing from the db backup", zipEntryDbVersion, notNullValue());
      assertThat(new String(getZipEntryContent(dbBackupZipFile, zipEntryDbVersion), "UTF-8"),
          is(FileUtils.fileRead(databaseVersionFile)));
    }

    File restoreIntructionsFile = new File(dbBackupDir, H2DatabaseBackup.RESTORE_FILENAME);
    assertThat("The restore instructions file is missing from the db backup", restoreIntructionsFile.isFile(), is(true));
  }

  private byte[] getZipEntryContent(ZipFile zipFile, ZipEntry zipEntry) throws IOException {
    try (InputStream is = zipFile.getInputStream(zipEntry)) {
      return IOUtil.toByteArray(is);
    }
  }
}
