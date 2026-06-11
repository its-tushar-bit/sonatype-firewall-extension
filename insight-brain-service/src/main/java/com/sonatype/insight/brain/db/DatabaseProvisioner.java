/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.migrations.DatabaseMigrations;

import com.google.common.annotations.VisibleForTesting;
import datadog.trace.api.Trace;
import io.opentelemetry.instrumentation.annotations.WithSpan;

/**
 * Responsible for provisioning the database including initialization (i.e. connecting), populating (new db schema), and
 * migrations.
 */
public class DatabaseProvisioner
    implements DataStoreProvider
{
  private final OperationalDataStore operationalDataStore;

  private final AggregationDataStore aggregationDataStore;

  private final DataMartDataStore dataMartDataStore;

  private final ThirdPartyScansDataStore thirdPartyScansDataStore;

  private final DatabaseMigrations databaseMigrations;

  public DatabaseProvisioner(final DataStoreProvider dataStoreProvider) {
    this.operationalDataStore = dataStoreProvider.getOperationalDataStore();
    this.aggregationDataStore = dataStoreProvider.getAggregationDataStore();
    this.dataMartDataStore = dataStoreProvider.getDataMartDataStore();
    this.thirdPartyScansDataStore = dataStoreProvider.getThirdPartyScansDataStore();
    this.databaseMigrations = new DatabaseMigrations(this);
  }

  @VisibleForTesting
  public DatabaseProvisioner(final DataStoreProvider dataStoreProvider, final DatabaseMigrations databaseMigrations) {
    this.operationalDataStore = dataStoreProvider.getOperationalDataStore();
    this.aggregationDataStore = dataStoreProvider.getAggregationDataStore();
    this.dataMartDataStore = dataStoreProvider.getDataMartDataStore();
    this.thirdPartyScansDataStore = dataStoreProvider.getThirdPartyScansDataStore();
    this.databaseMigrations = databaseMigrations;
  }

  /**
   * Init (connect to) the database, but do not perform migration
   */
  @Trace
  @WithSpan
  public void initializeDatabaseWithoutMigration() {
    operationalDataStore.initialize();
    dataMartDataStore.initialize();
    thirdPartyScansDataStore.initialize();
    aggregationDataStore.initialize();
  }

  /**
   * Init (connect to) the database, and also perform migration
   */
  public void initializeDatabaseWithMigration() {
    initializeDatabaseWithoutMigration();
    migrateDatabase();
  }

  /**
   * Check if migration is enabled, or if this is a brand new database, and execute migrations
   */
  public void migrateDatabase() {
    if (isMigrationEnabledOrHasNewDataSource()) {
      databaseMigrations.migrateDatabase();
    }
  }

  private boolean isMigrationEnabledOrHasNewDataSource() {
    return databaseMigrations.isMigrationEnabled() || hasNewDataSource();
  }

  /**
   * Return true if any of the {@link DataStore} objects were brand new (i.e. new database, no existing schema)
   */
  private boolean hasNewDataSource() {
    return operationalDataStore.isDataStoreNew() ||
        aggregationDataStore.isDataStoreNew() ||
        dataMartDataStore.isDataStoreNew() ||
        thirdPartyScansDataStore.isDataStoreNew();
  }

  public void validateMinimumSchemaVersion() {
    databaseMigrations.validateMinimumSchemaVersion();
  }

  @Override
  public OperationalDataStore getOperationalDataStore() {
    return operationalDataStore;
  }

  @Override
  public AggregationDataStore getAggregationDataStore() {
    return aggregationDataStore;
  }

  @Override
  public DataMartDataStore getDataMartDataStore() {
    return dataMartDataStore;
  }

  @Override
  public ThirdPartyScansDataStore getThirdPartyScansDataStore() {
    return thirdPartyScansDataStore;
  }
}
