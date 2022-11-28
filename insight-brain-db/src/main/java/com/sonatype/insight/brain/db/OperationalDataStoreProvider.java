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

  private static OperationalDataStore INSTANCE = new DefaultOperationalDataStore();

  public static OperationalDataStore getInstance() {
    return INSTANCE;
  }

  public static void setInstance(OperationalDataStore operationalDataStore) {
    INSTANCE = operationalDataStore;
  }

  private OperationalDataStoreProvider() { }

  public static void init(DatabaseConfig databaseConfig, boolean migrateToNewViolationModel) {
    INSTANCE.initWithMigration(databaseConfig, migrateToNewViolationModel);
  }

  public static void initWithoutMigration(DatabaseConfig databaseConfig) {
    INSTANCE.initWithoutMigration(databaseConfig);
  }

  public static void migrate(boolean migrateToNewViolationModel) {
    INSTANCE.migrate(migrateToNewViolationModel);
  }

  public static DataSource getDataSource() {
    return INSTANCE.getDataSource();
  }

  public static DataSource getDataSourceWithoutInit() {
    return INSTANCE.getDataSourceWithoutInit();
  }

  public static DatabaseConfig getDatabaseConfig() {
    return INSTANCE.getDatabaseConfig();
  }

  public static boolean isDatabaseInMemory() {
    return INSTANCE.isDatabaseInMemory();
  }

  public static EntityManagerFactory getJPAEntityManagerFactory() {
    return INSTANCE.getJPAEntityManagerFactory();
  }

  public static EntityManagerFactory getEntityManagerFactoryForLocks() {
    return INSTANCE.getEntityManagerFactoryForLocks();
  }

  static synchronized void clear_ForTestsOnly() {
    INSTANCE.clear_ForTestsOnly();
  }

  public static boolean isDatabaseEmbedded() {
    return INSTANCE.isDatabaseEmbedded();
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
