/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import java.io.File;

import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.db.DatabaseUtil;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultDataMartDataStoreTest
    extends AbstractDataStoreTest
{
  @Override
  protected DataStore createTestDataStore() {
    return new DefaultDataMartDataStore();
  }

  @Test
  public void testInit_Migrate() throws Exception {
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/Migrate");
    File databaseVersionFile = getDatabaseVersionFile(databaseDir, "dm");
    assertThat(databaseVersionFile.isFile()).isTrue();
    assertThat(readDatabaseVersion(databaseVersionFile)).isEqualTo("1");

    initDatabase(getDatabaseConfig(databaseDir, "dm"));

    assertThat(databaseVersionFile).doesNotExist();
    int desiredDbVersion = DatabaseMigrator.determineDesiredVersion(dataStore.getID());
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(dataStore.getDataSource(), dataStore.getID()))
        .isEqualTo(desiredDbVersion);
  }
}
