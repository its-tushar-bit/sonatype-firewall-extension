/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;

import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManagerProvider;
import com.sonatype.insight.brain.dataaccess.lock.PostgresAdvisoryLockDAO;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;

import com.google.common.annotations.VisibleForTesting;

public class DatabaseMigrations
    implements DataStoreProvider
{
  public static final String NXIQ_SCHEMA_MIGRATION = "NXIQ_DATABASE_MIGRATION";

  private static final AtomicBoolean forceEnableMigration = new AtomicBoolean();

  private final OperationalDataStore operationalDataStore;

  private final AggregationDataStore aggregationDataStore;

  private final DataMartDataStore dataMartDataStore;

  private final ThirdPartyScansDataStore thirdPartyScansDataStore;

  private final ClusterLockManager clusterLockManager;

  protected final DatabaseMigrators databaseMigrators;

  public DatabaseMigrations(final DataStoreProvider dataStoreProvider) {
    this.operationalDataStore = dataStoreProvider.getOperationalDataStore();
    this.aggregationDataStore = dataStoreProvider.getAggregationDataStore();
    this.dataMartDataStore = dataStoreProvider.getDataMartDataStore();
    this.thirdPartyScansDataStore = dataStoreProvider.getThirdPartyScansDataStore();
    this.clusterLockManager = getClusterLockManagerProvider(operationalDataStore);
    this.databaseMigrators = createDatabaseMigrators();
  }

  @VisibleForTesting
  public DatabaseMigrations(
      final DataStoreProvider dataStoreProvider,
      final ClusterLockManager clusterLockManager)
  {
    this.operationalDataStore = dataStoreProvider.getOperationalDataStore();
    this.aggregationDataStore = dataStoreProvider.getAggregationDataStore();
    this.dataMartDataStore = dataStoreProvider.getDataMartDataStore();
    this.thirdPartyScansDataStore = dataStoreProvider.getThirdPartyScansDataStore();
    this.clusterLockManager = clusterLockManager;
    this.databaseMigrators = createDatabaseMigrators();
  }

  protected DatabaseMigrators createDatabaseMigrators() {
    return new DatabaseMigrators(this);
  }

  public static void setForceEnableMigration(boolean forceEnableMigration) {
    DatabaseMigrations.forceEnableMigration.set(forceEnableMigration);
  }

  public void migrateDatabase() {
    if (!isMigrationEnabled()) {
      return;
    }

    if (supportsClusterLock()) {
      try (ClusterLock clusterLock = clusterLockManager.createForSchemaMigration()) {
        clusterLock.lock();
        doMigrateDatabases();
      }
    }
    else {
      doMigrateDatabases();
    }
  }

  /**
   * This is a "pre-Guice" class (aka before dependency injection) however the ClusterLockManager is required for schema
   * migration (also pre-Guice) and it has an additional dependency of LockDAO. So for this class we need to manually
   * instantiate both.
   */
  private ClusterLockManager getClusterLockManagerProvider(final OperationalDataStore operationalDataStore) {
    ClusterLockManagerProvider clusterLockManagerProvider = new ClusterLockManagerProvider(
        operationalDataStore,
        new PostgresAdvisoryLockDAO());

    return clusterLockManagerProvider.get();
  }

  /**
   * Is the migration system enabled or disabled. Options include
   * <ul>
   * <li>{@link #setForceEnableMigration(boolean)} was called to forcefully override it</li>
   * <li>The {@link #NXIQ_SCHEMA_MIGRATION} environment variable was set</li>
   * <li>The {@link #NXIQ_SCHEMA_MIGRATION} system configuration property was set in the database</li>
   * </ul>
   */
  public boolean isMigrationEnabled() {
    if (forceEnableMigration.get()) {
      return true;
    }
    Boolean migrationEnabled = isMigrationEnabledFromEnvironmentVariable();
    if (migrationEnabled != null) {
      return migrationEnabled;
    }
    DataSource odsDataSource = operationalDataStore.getDataSource();
    String databaseSchema = operationalDataStore.getDatabaseSchema();
    if (odsDataSource != null && DatabaseUtil.systemConfigurationPropertyTableExists(odsDataSource, databaseSchema)) {
      migrationEnabled =
          parseBoolean(getSchemaMigrationEnabledFromDatabase());
    }
    if (migrationEnabled != null) {
      return migrationEnabled;
    }
    return true;
  }

  @VisibleForTesting
  String getSchemaMigrationEnabledFromDatabase() {
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(
            "SELECT value FROM " + operationalDataStore.getDatabaseSchema() +
                ".system_configuration_property WHERE name = '" + DatabaseMigrator.SCHEMA_MIGRATION_ENABLED + "'"))
    {
      if (resultSet.next()) {
        return resultSet.getString(1);
      }
    }
    catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
    return null;
  }

  private boolean supportsClusterLock() {
    // check if the ODS schema version is newer than the version locking was introduced in
    return DatabaseUtil
        .getLegacyDatabaseSchemaVersion(operationalDataStore) >= OperationalDataStore.LOCK_TABLE_DATABASE_VERSION;
  }

  private void doMigrateDatabases() {
    databaseMigrators.runMigrators();
  }

  public static Boolean isMigrationEnabledFromEnvironmentVariable() {
    return parseBoolean(System.getenv(NXIQ_SCHEMA_MIGRATION));
  }

  private static Boolean parseBoolean(String s) {
    if (s == null) {
      return null;
    }
    if (s.equalsIgnoreCase("true")) {
      return true;
    }
    if (s.equalsIgnoreCase("false")) {
      return false;
    }
    return null;
  }

  /**
   * Verify that the database version matches the application version. Force exit if schema version table does not exist
   * or if migration was needed but didn't happen.
   */
  public void validateMinimumSchemaVersion() {
    if (operationalDataStore.isDatabaseInMemory()) {
      // short-circuit checks for tests
      return;
    }

    databaseMigrators.validateMinimumSchemaVersion();
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
