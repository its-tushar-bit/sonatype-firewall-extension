/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.db.datastore.DataStoreProvider;

/**
 * Simple encapsulation of the {@link List} of {@link DatabaseMigrator} instances. This is needed as the
 * `DatabaseMigrations` class (which is primary class responsible for migrations) is in the `insight-brain-service`
 * module because of its dependency on `ClusterLockManager`, yet other code in the `insight-brain-db` module here
 * shouldn't have to know about the {@link List} of migrators.
 */
public class DatabaseMigrators
{
  private final DataStoreProvider dataStoreProvider;

  protected final List<DatabaseMigrator> databaseMigrators;

  public DatabaseMigrators(DataStoreProvider dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
    this.databaseMigrators = createDatabaseMigrators();
  }

  protected List<DatabaseMigrator> createDatabaseMigrators() {
    return Arrays.asList(
        // legacy migrations always run first
        new LegacyDatabaseMigrator(dataStoreProvider), new LiquibaseDatabaseMigrator(dataStoreProvider));
  }

  public void runMigrators() {
    for (DatabaseMigrator databaseMigrator : databaseMigrators) {
      databaseMigrator.migrate();
    }
  }

  public void validateMinimumSchemaVersion() {
    for (DatabaseMigrator databaseMigrator : databaseMigrators) {
      databaseMigrator.validateMinimumSchemaVersion();
    }
  }
}
