/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.DatabaseUtil;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultAggregationDataStoreTest
    extends AbstractDataStoreTest
{
  @Override
  protected DataStore getTestDataStore() {
    return databaseRule.getAggregationDataStore();
  }

  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "DefaultAggregationDataStoreTest/Migrate"
  )
  public void testInit_Migrate() throws Exception {
    migrateDatabase();

    int desiredDbVersion = DataStoreMigrator.determineDesiredVersion(getTestDataStore().getID());
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(getTestDataStore().getDataSource(), getTestDataStore().getID(),
        getTestDataStore().getDatabaseSchema())).isEqualTo(desiredDbVersion);
  }
}
