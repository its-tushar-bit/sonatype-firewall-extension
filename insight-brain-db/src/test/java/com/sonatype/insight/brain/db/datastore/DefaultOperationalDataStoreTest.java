/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultOperationalDataStoreTest
    extends AbstractDataStoreTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(DefaultOperationalDataStore.class);

  @Override
  protected DataStore getTestDataStore() {
    return databaseRule.getOperationalDataStore();
  }

  @Test
  @H2DiskTest(suppressMigrations = true, copyExistingDatabase = "DefaultOperationalDataStoreTest/Migrate")
  public void testInit_Migrate() throws Exception {
    migrateDatabase();

    int desiredDbVersion = DataStoreMigrator.determineDesiredVersion(OperationalDataStore.ID);
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(getTestDataStore().getDataSource(), getTestDataStore().getID(),
        getTestDataStore().getDatabaseSchema())).isEqualTo(desiredDbVersion);
  }
}
