/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import javax.sql.DataSource;

import com.sonatype.insight.brain.dataaccess.LockDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManagerProvider;
import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStoreMigrator;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.service.InsightConfig;

import com.google.common.annotations.VisibleForTesting;

public class DatabaseProvisionUtils
    implements DataStoreProvider
{
  private final OperationalDataStore operationalDataStore;

  private final AggregationDataStore aggregationDataStore;

  private final DataMartDataStore dataMartDataStore;

  private final ThirdPartyScansDataStore thirdPartyScansDataStore;

  private final ClusterLockManager clusterLockManager;

  public DatabaseProvisionUtils(
      final OperationalDataStore operationalDataStore,
      final AggregationDataStore aggregationDataStore,
      final DataMartDataStore dataMartDataStore,
      final ThirdPartyScansDataStore thirdPartyScansDataStore)
  {
    this.operationalDataStore = operationalDataStore;
    this.aggregationDataStore = aggregationDataStore;
    this.dataMartDataStore = dataMartDataStore;
    this.thirdPartyScansDataStore = thirdPartyScansDataStore;

    this.clusterLockManager = getClusterLockManagerProvider(operationalDataStore);
  }

  @VisibleForTesting
  public DatabaseProvisionUtils(
      final OperationalDataStore operationalDataStore,
      final AggregationDataStore aggregationDataStore,
      final DataMartDataStore dataMartDataStore,
      final ThirdPartyScansDataStore thirdPartyScansDataStore,
      final ClusterLockManager clusterLockManager)
  {
    this.operationalDataStore = operationalDataStore;
    this.aggregationDataStore = aggregationDataStore;
    this.dataMartDataStore = dataMartDataStore;
    this.thirdPartyScansDataStore = thirdPartyScansDataStore;
    this.clusterLockManager = clusterLockManager;
  }

  /**
   * {@link DatabaseProvisionUtils} is a "pre-Guice" class (aka before dependency injection) however the
   * ClusterLockManager is required for schema migration (also pre-Guice) and it has an additional dependency of
   * LockDAO. So for this class we need to manually instantiate both.
   */
  private ClusterLockManager getClusterLockManagerProvider(final OperationalDataStore operationalDataStore) {
    ClusterLockManagerProvider clusterLockManagerProvider =
        new ClusterLockManagerProvider(operationalDataStore, new LockDAO(operationalDataStore));
    return clusterLockManagerProvider.get();
  }

  public void initializeDatabasesWithMigration(InsightConfig insightConfig) {
    initializeDatabasesWithoutMigration();
    migrateDatabasesIfNeeded(insightConfig);
  }

  public void initializeDatabasesWithoutMigration() {
    operationalDataStore.initialize();
    dataMartDataStore.initialize();
    thirdPartyScansDataStore.initialize();
    aggregationDataStore.initialize();
  }

  public void migrateDatabasesIfNeeded(InsightConfig insightConfig) {
    int schemaVersion = -2;
    boolean schemaVersionTableExists = isSchemaVersionTableExists();
    if (schemaVersionTableExists) {
      schemaVersion =
          DatabaseUtil.getDatabaseSchemaVersion(operationalDataStore.getDataSource(), operationalDataStore.getID(),
              operationalDataStore.getDatabaseSchema());
    }
    boolean isMigrationEnabledOrHasNewDataSource = isMigrationEnabledOrHasNewDataSource();
    // -1 indicates a new database which needs to be "migrated" to have its schema version inserted
    if (schemaVersionTableExists &&
        (schemaVersion == -1 || schemaVersion >= OperationalDataStore.LOCK_TABLE_DATABASE_VERSION)) {
      try (ClusterLock clusterLock = clusterLockManager.createForSchemaMigration()) {
        clusterLock.lock();
        if (isMigrationEnabledOrHasNewDataSource && isMigrationNeeded()) {
          clusterLockManager.createForSchemaMigrationInProgress();
          doMigrateDatabases(insightConfig);
          clusterLockManager.deleteForSchemaMigrationInProgress();
        }
      }
    }
    else {
      if (isMigrationEnabledOrHasNewDataSource) {
        doMigrateDatabases(insightConfig);
      }
    }
  }

  public boolean isInMemoryDatabase() {
    return operationalDataStore.isDatabaseInMemory();
  }

  public boolean isSchemaVersionTableExists() {
    return DatabaseUtil.schemaVersionTableExists(operationalDataStore.getDataSource(),
        operationalDataStore.getDatabaseSchema());
  }

  private boolean isMigrationEnabledOrHasNewDataSource() {
    return DatabaseMigrator.isMigrationEnabled(operationalDataStore) || hasNewDataSource();
  }

  public boolean isMigrationNeeded() {
    return isMigrationNeeded(operationalDataStore.getDataSource(), operationalDataStore.getID(),
        operationalDataStore.getDatabaseSchema())
        || isMigrationNeeded(dataMartDataStore.getDataSource(), dataMartDataStore.getID(),
        dataMartDataStore.getDatabaseSchema())
        || isMigrationNeeded(thirdPartyScansDataStore.getDataSource(), thirdPartyScansDataStore.getID(),
        thirdPartyScansDataStore.getDatabaseSchema())
        || isMigrationNeeded(aggregationDataStore.getDataSource(), aggregationDataStore.getID(),
        aggregationDataStore.getDatabaseSchema());
  }

  public boolean hasNewDataSource() {
    return operationalDataStore.isDataStoreNew() ||
        aggregationDataStore.isDataStoreNew() ||
        dataMartDataStore.isDataStoreNew() ||
        thirdPartyScansDataStore.isDataStoreNew();
  }

  private boolean isMigrationNeeded(DataSource dataSource, String dataStoreId, String databaseSchemaName) {
    if (!DatabaseUtil.schemaExists(dataSource, databaseSchemaName)) {
      return true;
    }
    int currentVersion = DatabaseUtil.getDatabaseSchemaVersion(dataSource, dataStoreId, databaseSchemaName);
    int desiredVersion = DataStoreMigrator.determineDesiredVersion(dataStoreId);
    return currentVersion < desiredVersion;
  }

  // Visible for testing
  public void doMigrateDatabases(InsightConfig insightConfig) {
    // NOTE: The ODS can refuse upgrade if the existing schema is too old. So upgrade it first to avoid
    // upgrading the other databases if the ODS fails and a previous server version must be run first instead.
    (new DatabaseMigrator(this)).migrate(insightConfig.isConsentToUpgradeToVersion_1_45());
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
