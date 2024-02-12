/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import com.sonatype.insight.brain.db.datastore.DataStore;

import org.junit.Ignore;

// TODO - when Liquibase support is added
@Ignore
public class LiquibaseDatabaseMigratorTest
    extends AbstractDatabaseMigratorTest
{
  @Override
  protected DatabaseMigrator createDatabaseMigratorForTest() {
    return new LiquibaseDatabaseMigrator(databaseRule)
    {
      @Override
      protected DataStoreMigrator createDataStoreMigrator(final DataStore dataStore) {
        return createMockDataStoreMigrator();
      }
    };
  }
}
