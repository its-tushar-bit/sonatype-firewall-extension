/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStoreMigrator;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;

import com.google.common.annotations.VisibleForTesting;

/**
 * Migrate the entire IQ database (all four data stores)
 */
public class DatabaseMigrator
{
  public static final String SCHEMA_MIGRATION_ENABLED = "SCHEMA_MIGRATION_ENABLED";

  // Visible for testing
  public static final String NXIQ_SCHEMA_MIGRATION = "NXIQ_DATABASE_MIGRATION";

  private static final AtomicBoolean forceEnableMigration = new AtomicBoolean();

  private final DataStoreProvider dataStoreProvider;

  public DatabaseMigrator(final DataStoreProvider dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @VisibleForTesting
  public DatabaseMigrator(
      final OperationalDataStore operationalDataStore,
      final AggregationDataStore aggregationDataStore,
      final DataMartDataStore dataMartDataStore,
      final ThirdPartyScansDataStore thirdPartyScansDataStore)
  {
    this.dataStoreProvider = new DataStoreProvider()
    {
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
    };
  }

  public void migrate(final Boolean migrateToNewViolationModel) {
    if (!isMigrationEnabled(dataStoreProvider.getOperationalDataStore())) {
      return;
    }

    new DataStoreMigrator(dataStoreProvider.getOperationalDataStore()).migrate(migrateToNewViolationModel);
    new DataStoreMigrator(dataStoreProvider.getAggregationDataStore()).migrate(migrateToNewViolationModel);
    new DataStoreMigrator(dataStoreProvider.getDataMartDataStore()).migrate(migrateToNewViolationModel);
    new DataStoreMigrator(dataStoreProvider.getThirdPartyScansDataStore()).migrate(migrateToNewViolationModel);
  }

  public static void setForceEnableMigration(boolean forceEnableMigration) {
    DatabaseMigrator.forceEnableMigration.set(forceEnableMigration);
  }

  /**
   * Is the migration system enabled or disabled. Options include
   * <ul>
   *   <li>{@link DatabaseMigrator#setForceEnableMigration(boolean)} was called to forcefully override it</li>
   *   <li>The {@link DatabaseMigrator#NXIQ_SCHEMA_MIGRATION} environment variable was set</li>
   *   <li>The {@link DatabaseMigrator#NXIQ_SCHEMA_MIGRATION} system configuration property was set in the database</li>
   * </ul>
   *
   * @param operationalDataStore The {@link OperationalDataStore} used to access the database system config property
   */
  public static boolean isMigrationEnabled(final OperationalDataStore operationalDataStore) {
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
          parseBoolean(DatabaseUtil.getSchemaMigrationEnabledFromDatabase(odsDataSource, databaseSchema));
    }
    if (migrationEnabled != null) {
      return migrationEnabled;
    }
    return true;
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
}
