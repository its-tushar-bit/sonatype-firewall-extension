/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import javax.sql.DataSource;

import com.sonatype.insight.brain.dataaccess.ClusterLock;
import com.sonatype.insight.brain.db.AggregationDataStoreProvider;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.DatamartProvider;
import com.sonatype.insight.brain.db.ThirdPartyScansProvider;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.service.DatabaseConfigProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.db.DatabaseConfig;

public class DatabaseProvisionUtils
{
  private final OperationalDataStore operationalDataStore;

  private final AggregationDataStore aggregationDataStore;

  private final DataMartDataStore dataMartDataStore;

  private final ThirdPartyScansDataStore thirdPartyScansDataStore;

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
  }

  public void initializeDatabases(InsightConfig insightConfig, DatabaseConfigProvider databaseConfigProvider) {
    initializeDatabasesWithoutMigration(databaseConfigProvider);
    migrateDatabasesIfNeeded(insightConfig);
  }

  public void initializeDatabasesWithoutMigration(InsightConfig insightConfig) {
    initializeDatabasesWithoutMigration(new DatabaseConfigProvider(insightConfig));
  }

  private void initializeDatabasesWithoutMigration(DatabaseConfigProvider databaseConfigProvider) {
    DatabaseConfig odsDatabaseConfig = databaseConfigProvider.getDatabaseConfig(DatabaseName.ods);
    operationalDataStore.initWithoutMigration(odsDatabaseConfig);

    DatabaseConfig dmDatabaseConfig = databaseConfigProvider.getDatabaseConfig(DatabaseName.dm);
    dataMartDataStore.initWithoutMigration(dmDatabaseConfig);

    DatabaseConfig tpsDatabaseConfig = databaseConfigProvider.getDatabaseConfig(DatabaseName.third_party_scans);
    thirdPartyScansDataStore.initWithoutMigration(tpsDatabaseConfig);

    DatabaseConfig aggregationDatabaseConfig = databaseConfigProvider.getDatabaseConfig(DatabaseName.aggregation);
    aggregationDataStore.initWithoutMigration(aggregationDatabaseConfig);
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
      try (ClusterLock clusterLock = ClusterLock.createForSchemaMigration()) {
        clusterLock.lock();
        if (isMigrationEnabledOrHasNewDataSource && isMigrationNeeded()) {
          ClusterLock.createForSchemaMigrationInProgress();
          doMigrateDatabases(insightConfig);
          ClusterLock.deleteForSchemaMigrationInProgress();
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
    return DatabaseMigrator.isMigrationEnabled() || DataSourceFactory.hasNewDataSource();
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

  private boolean isMigrationNeeded(DataSource dataSource, String dataStoreId, String databaseSchemaName) {
    if (!DatabaseUtil.schemaExists(dataSource, databaseSchemaName)) {
      return true;
    }
    int currentVersion = DatabaseUtil.getDatabaseSchemaVersion(dataSource, dataStoreId, databaseSchemaName);
    int desiredVersion = DatabaseMigrator.determineDesiredVersion(databaseSchemaName);
    return currentVersion < desiredVersion;
  }

  // Visible for testing
  public void doMigrateDatabases(InsightConfig insightConfig) {
    // NOTE: The ODS can refuse upgrade if the existing schema is too old. So upgrade it first to avoid
    // upgrading the other databases if the ODS fails and a previous server version must be run first instead.
    operationalDataStore.migrate(insightConfig.isConsentToUpgradeToVersion_1_45());
    DatamartProvider.migrate();
    ThirdPartyScansProvider.migrate();
    AggregationDataStoreProvider.migrate();
  }
}
