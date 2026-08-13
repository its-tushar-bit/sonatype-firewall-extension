/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import java.io.File;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.migrations.LegacyDataStoreMigrator;
import com.sonatype.insight.brain.db.migrations.LiquibaseDataStoreMigrator;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractDataStoreTest
    extends AbstractDatabaseTest
{
  protected abstract DataStore getTestDataStore();

  protected void migrateDatabase() {
    new LegacyDataStoreMigrator(getTestDataStore()).migrate();
    new LiquibaseDataStoreMigrator(getTestDataStore()).migrate();
  }

  public void testInit_Migrate() {
    File databaseVersionFile = getDatabaseVersionFile(getDatabasePath(), getTestDataStore().getID());

    migrateDatabase();

    assertThat(databaseVersionFile).doesNotExist();

    int desiredDbVersion = LegacyDataStoreMigrator.determineDesiredVersion(getTestDataStore().getID());
    assertThat(DatabaseUtil.getLegacyDatabaseSchemaVersion(getTestDataStore())).isEqualTo(desiredDbVersion);
    // TODO - liquibase assertions
  }
}
