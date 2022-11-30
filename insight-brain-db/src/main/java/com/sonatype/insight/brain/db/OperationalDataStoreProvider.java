/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.util.function.IntConsumer;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datastore.DefaultOperationalDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.db.DatabaseConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.db.datastore.DefaultOperationalDataStore.MINIMUM_DATABASE_VERSION;
import static com.sonatype.insight.brain.db.datastore.DefaultOperationalDataStore.OLD_VIOLATION_MODEL_DATABASE_VERSION;

public class OperationalDataStoreProvider
{
  private static final Logger log = LoggerFactory.getLogger(OperationalDataStoreProvider.class);

  private static OperationalDataStore INSTANCE;

  public static OperationalDataStore getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new DefaultOperationalDataStore();
    }
    return INSTANCE;
  }

  public static void setInstance(OperationalDataStore operationalDataStore) {
    INSTANCE = operationalDataStore;
  }

  private OperationalDataStoreProvider() {
    // private ctor
  }

  public static void init(DatabaseConfig databaseConfig, boolean migrateToNewViolationModel) {
    getInstance().initWithMigration(databaseConfig, migrateToNewViolationModel);
  }

  public static void initWithoutMigration(DatabaseConfig databaseConfig) {
    getInstance().initWithoutMigration(databaseConfig);
  }

  public static void migrate(boolean migrateToNewViolationModel) {
    getInstance().migrate(migrateToNewViolationModel);
  }

  public static DataSource getDataSource() {
    return getInstance().getDataSource();
  }

  public static DataSource getDataSourceWithoutInit() {
    return getInstance().getDataSourceWithoutInit();
  }

  public static DatabaseConfig getDatabaseConfig() {
    return getInstance().getDatabaseConfig();
  }

  public static String getDatabaseSchema() {
    return getInstance().getDatabaseSchema();
  }

  public static boolean isDatabaseInMemory() {
    return getInstance().isDatabaseInMemory();
  }

  public static EntityManagerFactory getJPAEntityManagerFactory() {
    return getInstance().getJPAEntityManagerFactory();
  }

  public static EntityManagerFactory getEntityManagerFactoryForLocks() {
    return getInstance().getEntityManagerFactoryForLocks();
  }

  static synchronized void clear_ForTestsOnly() {
    if (INSTANCE != null) {
      INSTANCE.clear_ForTestsOnly();
    }
  }

  public static boolean isDatabaseEmbedded() {
    return getInstance().isDatabaseEmbedded();
  }

  public static IntConsumer getUpgradeGuard(final Boolean migrateToNewViolationModel) {
    return currentVersion -> {
      if (currentVersion < MINIMUM_DATABASE_VERSION) {
        throw new UnsupportedOperationException(String.format(
            "Cannot migrate %s database, this requires version %s at minimum, but you have version %s.\n"
                + "Please upgrade to Nexus IQ Server version 1.16 before upgrading to this version.",
            OperationalDataStore.ID, MINIMUM_DATABASE_VERSION, currentVersion));
      }
      if (currentVersion <= OLD_VIOLATION_MODEL_DATABASE_VERSION && !migrateToNewViolationModel) {
        log.error("|------------------------------------------");
        log.error("|");
        log.error("| Upgrade requires consent to proceed.");
        log.error("| For detailed instructions, see");
        log.error("| https://links.sonatype.com/products/clm/doc/upgrade/1.45");
        log.error("|");
        log.error("|------------------------------------------");
        throw new UnsupportedOperationException("Consent to upgrade has not been given.");
      }
    };
  }
}
