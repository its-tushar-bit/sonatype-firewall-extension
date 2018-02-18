/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseConfig;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class OperationalDataStoreProviderTest
    extends AbstractDatabaseProviderTest
{
  @Override
  protected DatabaseConfig getDatabaseConfig() {
    return OperationalDataStoreProvider.getDatabaseConfig();
  }

  @Override
  protected void initDatabase(DatabaseConfig databaseConfig) {
    OperationalDataStoreProvider.init(databaseConfig);
  }

  @Override
  protected DataSource getDataSource() {
    return OperationalDataStoreProvider.getDataSource();
  }

  @Test
  public void testInit_Migrate() throws Exception {
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/Migrate");
    File databaseVersionFile = getDatabaseVersionFile(databaseDir, "ods");
    assertThat(databaseVersionFile.isFile(), is(true));
    assertThat(readDatabaseVersion(databaseVersionFile),
        is(String.valueOf(OperationalDataStoreProvider.MINIMUM_DATABASE_VERSION)));

    initDatabase(getDatabaseConfig(databaseDir, "ods"));

    assertThat(readDatabaseVersion(databaseVersionFile),
        is(String.valueOf(OperationalDataStoreProvider.DESIRED_DATABASE_VERSION)));
  }

  @Test
  public void testInit_Migrate_CurrentVersionLessThanMinimumVersion() throws Exception {
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/Migrate");
    File databaseVersionFile = new File(databaseDir, "ods.ver");
    String oldVersion = String.valueOf(OperationalDataStoreProvider.MINIMUM_DATABASE_VERSION - 1);
    Files.write(databaseVersionFile.toPath(), oldVersion.getBytes(StandardCharsets.UTF_8));

    try {
      initDatabase(getDatabaseConfig(databaseDir, "ods"));
      fail("Expected exception");
    }
    catch (UnsupportedOperationException e) {
      assertThat(e.getMessage(),
          is("Cannot migrate insight_brain_ods database to version "
              + OperationalDataStoreProvider.DESIRED_DATABASE_VERSION + ", this requires version "
              + OperationalDataStoreProvider.MINIMUM_DATABASE_VERSION + " at minimum, but you have version "
              + oldVersion + ".\nPlease upgrade to Nexus IQ Server version 1.16 before upgrading to this version."));
    }
  }
}
