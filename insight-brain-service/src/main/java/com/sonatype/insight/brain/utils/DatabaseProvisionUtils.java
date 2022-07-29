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
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.ThirdPartyScansProvider;
import com.sonatype.insight.brain.service.DatabaseConfigProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.db.DatabaseConfig;

public final class DatabaseProvisionUtils
{
  private DatabaseProvisionUtils() {
    // Utility class
  }

  public static void initializeDatabases(InsightConfig insightConfig, DatabaseConfigProvider databaseConfigProvider) {
    initializeDatabasesWithoutMigration(databaseConfigProvider);
    migrateDatabasesIfNeeded(insightConfig);
  }

  public static void initializeDatabasesWithoutMigration(InsightConfig insightConfig) {
    initializeDatabasesWithoutMigration(new DatabaseConfigProvider(insightConfig));
  }

  private static void initializeDatabasesWithoutMigration(DatabaseConfigProvider databaseConfigProvider) {
    DatabaseConfig odsDatabaseConfig = databaseConfigProvider.getDatabaseConfig(DatabaseName.ods);
    OperationalDataStoreProvider.initWithoutMigration(odsDatabaseConfig);

    DatabaseConfig dmDatabaseConfig = databaseConfigProvider.getDatabaseConfig(DatabaseName.dm);
    DatamartProvider.initWithoutMigration(dmDatabaseConfig);

    DatabaseConfig tpsDatabaseConfig = databaseConfigProvider.getDatabaseConfig(DatabaseName.third_party_scans);
    ThirdPartyScansProvider.initWithoutMigration(tpsDatabaseConfig);

    DatabaseConfig aggregationDatabaseConfig = databaseConfigProvider.getDatabaseConfig(DatabaseName.aggregation);
    AggregationDataStoreProvider.initWithoutMigration(aggregationDatabaseConfig);
  }

  public static void migrateDatabasesIfNeeded(InsightConfig insightConfig) {
    int schemaVersion = -2;
    boolean schemaVersionTableExists = isSchemaVersionTableExists();
    if (schemaVersionTableExists) {
      schemaVersion = DatabaseUtil.getDatabaseSchemaVersion(OperationalDataStoreProvider.getDataSource(),
          OperationalDataStoreProvider.ID);
    }
    boolean isMigrationEnabledOrHasNewDataSource = isMigrationEnabledOrHasNewDataSource();
    // -1 indicates a new database which needs to be "migrated" to have its schema version inserted
    if (schemaVersionTableExists &&
        (schemaVersion == -1 || schemaVersion >= OperationalDataStoreProvider.LOCK_TABLE_DATABASE_VERSION)) {
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

  public static boolean isInMemoryDatabase() {
    return OperationalDataStoreProvider.isDatabaseInMemory();
  }

  public static boolean isSchemaVersionTableExists() {
    return DatabaseUtil.schemaVersionTableExists(OperationalDataStoreProvider.getDataSource(),
        OperationalDataStoreProvider.ID);
  }

  public static boolean isMigrationEnabledOrHasNewDataSource() {
    return DatabaseMigrator.isMigrationEnabled() || DataSourceFactory.hasNewDataSource();
  }

  public static boolean isMigrationNeeded() {
    return isMigrationNeeded(OperationalDataStoreProvider.getDataSource(), OperationalDataStoreProvider.ID)
        || isMigrationNeeded(DatamartProvider.getDataSource(), DatamartProvider.ID)
        || isMigrationNeeded(ThirdPartyScansProvider.getDataSource(), ThirdPartyScansProvider.ID)
        || isMigrationNeeded(AggregationDataStoreProvider.getDataSource(), AggregationDataStoreProvider.ID);
  }

  private static boolean isMigrationNeeded(DataSource dataSource, String databaseSchemaName) {
    int currentVersion = DatabaseUtil.getDatabaseSchemaVersion(dataSource, databaseSchemaName);
    int desiredVersion = DatabaseMigrator.determineDesiredVersion(databaseSchemaName);
    return currentVersion < desiredVersion;
  }

  // Visible for testing
  public static void doMigrateDatabases(InsightConfig insightConfig) {
    // NOTE: The ODS can refuse upgrade if the existing schema is too old. So upgrade it first to avoid
    // upgrading the other databases if the ODS fails and a previous server version must be run first instead.
    OperationalDataStoreProvider.migrate(insightConfig.isConsentToUpgradeToVersion_1_45());
    DatamartProvider.migrate();
    ThirdPartyScansProvider.migrate();
    AggregationDataStoreProvider.migrate();
  }
}
