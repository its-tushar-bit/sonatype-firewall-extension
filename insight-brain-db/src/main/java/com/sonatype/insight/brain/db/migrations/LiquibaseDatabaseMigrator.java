/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;

import com.google.common.annotations.VisibleForTesting;

/**
 * Runs the Liquibase database migrations on each of the data stores
 */
public class LiquibaseDatabaseMigrator
    extends AbstractDatabaseMigrator
    implements DatabaseMigrator
{
  public LiquibaseDatabaseMigrator(final DataStoreProvider dataStoreProvider) {
    super(dataStoreProvider);
  }

  @VisibleForTesting
  public LiquibaseDatabaseMigrator(
      final OperationalDataStore operationalDataStore,
      final AggregationDataStore aggregationDataStore,
      final DataMartDataStore dataMartDataStore,
      final ThirdPartyScansDataStore thirdPartyScansDataStore)
  {
    super(operationalDataStore, aggregationDataStore, dataMartDataStore, thirdPartyScansDataStore);
  }

  @Override
  protected boolean isMigrationNeeded() {
    // TODO
    return true;
  }

  @Override
  protected DataStoreMigrator createDataStoreMigrator(final DataStore dataStore) {
    return new LiquibaseDataStoreMigrator(dataStore);
  }

  @Override
  public void validateMinimumSchemaVersion() {
    // TODO
  }
}
