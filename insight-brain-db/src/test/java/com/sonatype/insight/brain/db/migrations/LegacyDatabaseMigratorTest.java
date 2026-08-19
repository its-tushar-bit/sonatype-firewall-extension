/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import com.sonatype.insight.brain.db.datastore.DataStore;

public class LegacyDatabaseMigratorTest
    extends AbstractDatabaseMigratorTest
{
  @Override
  protected DatabaseMigrator createDatabaseMigratorForTest() {
    return new LegacyDatabaseMigrator(databaseRule)
    {
      @Override
      protected DataStoreMigrator createDataStoreMigrator(final DataStore dataStore) {
        return createMockDataStoreMigrator();
      }
    };
  }
}
