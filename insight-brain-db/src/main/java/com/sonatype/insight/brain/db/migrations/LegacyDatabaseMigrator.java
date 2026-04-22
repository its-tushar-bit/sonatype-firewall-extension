/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the legacy database migrations on each of the data stores
 */
public class LegacyDatabaseMigrator
    extends AbstractDatabaseMigrator
    implements DatabaseMigrator
{
  private static final Logger log = LoggerFactory.getLogger(LegacyDatabaseMigrator.class);

  public LegacyDatabaseMigrator(final DataStoreProvider dataStoreProvider) {
    super(dataStoreProvider);
  }

  @Override
  protected boolean isMigrationNeeded() {
    return isLegacyMigrationNeeded(operationalDataStore)
        || isLegacyMigrationNeeded(dataMartDataStore)
        || isLegacyMigrationNeeded(thirdPartyScansDataStore)
        || isLegacyMigrationNeeded(aggregationDataStore);
  }

  @Override
  protected DataStoreMigrator createDataStoreMigrator(final DataStore dataStore) {
    return new LegacyDataStoreMigrator(dataStore);
  }

  @Override
  public void validateMinimumSchemaVersion() {
    if (!DatabaseUtil.legacySchemaVersionTableExists(operationalDataStore) || isMigrationNeeded()) {
      log.error("\n\n\t\t\t***** Database migration is required. " +
          "Please migrate the database before starting the application! *****\n");
      exit(1);
    }
  }

  // Visible for testing
  void exit(int status) {
    System.exit(status);
  }

  private static boolean isLegacyMigrationNeeded(final DataStore dataStore) {
    // if schema doesn't even exist it needs to be migrated
    if (!DatabaseUtil.schemaExists(dataStore.getDataSource(), dataStore.getDatabaseSchema())) {
      return true;
    }
    int currentVersion = DatabaseUtil.getLegacyDatabaseSchemaVersion(dataStore);
    int desiredVersion = LegacyDataStoreMigrator.determineDesiredVersion(dataStore.getID());
    return currentVersion < desiredVersion;
  }
}
