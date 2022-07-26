/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntConsumer;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.H2DatabaseEngine;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.openjpa.lib.jdbc.JDBCListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperationalDataStoreProvider
{
  private static final Logger log = LoggerFactory.getLogger(OperationalDataStoreProvider.class);

  public static final String ID = "insight_brain_ods";

  // Visible for testing
  public static final int MINIMUM_DATABASE_VERSION = 85;

  public static final int LOCK_TABLE_DATABASE_VERSION = 181;

  static final int OLD_VIOLATION_MODEL_DATABASE_VERSION = 114;

  private static DataSource dataSource;

  private static DatabaseConfig databaseConfig;

  private static Boolean isDatabaseEmbedded;

  private static EntityManagerFactory entityManagerFactory;

  private static EntityManagerFactory entityManagerFactoryForLocks;

  private static volatile boolean isInitialized = false;

  private OperationalDataStoreProvider() {
  }

  public static void init(DatabaseConfig databaseConfig, boolean migrateToNewViolationModel) {
    init(databaseConfig, true /* migrateDatabase */, migrateToNewViolationModel);
  }

  public static void initWithoutMigration(DatabaseConfig databaseConfig) {
    init(databaseConfig, false /* migrateDatabase */, false);
  }

  private static synchronized void init(DatabaseConfig databaseConfig,
                                        boolean migrateDatabase,
                                        boolean migrateToNewViolationModel)
  {
    if (isInitialized) {
      return;
    }

    log.info("Initializing the {} data store.", ID);
    long start = System.currentTimeMillis();

    OperationalDataStoreProvider.databaseConfig = databaseConfig;
    dataSource = new DataSourceFactory().newDataSource(databaseConfig, ID);
    isDatabaseEmbedded = H2DatabaseEngine.class.equals(DataSourceFactory.getDatabaseEngine(dataSource).getClass());

    if (migrateDatabase) {
      migrate(migrateToNewViolationModel);
    }
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("openjpa.ConnectionFactory", dataSource);

    if (SqlCallCounterMetrics.getInstance().getJDBCListener() != null) {
      props.put("openjpa.jdbc.JDBCListeners",
          new JDBCListener[] {SqlCallCounterMetrics.getInstance().getJDBCListener()});
      log.info("Enabled JPA JDBC listener for performance testing.");
    }

    entityManagerFactory = Persistence.createEntityManagerFactory("InsightBrainODS", props);
    if (isDatabaseEmbedded) {
      entityManagerFactoryForLocks = entityManagerFactory;
    }
    else {
      // smallest pool size should still support max nesting level of locks
      int maxConnections = Math.max(5, Optional.ofNullable(databaseConfig.getMaxConnections()).orElse(50));
      BasicDataSource dataSource = new BasicDataSource();
      dataSource.setDriverClassName(databaseConfig.getDriverClassName());
      dataSource.setUrl(databaseConfig.getUrl());
      dataSource.setUsername(databaseConfig.getUsername());
      dataSource.setPassword(databaseConfig.getPassword());
      dataSource.setMaxTotal(maxConnections);
      props.put("openjpa.ConnectionFactory", dataSource);
      entityManagerFactoryForLocks = Persistence.createEntityManagerFactory("InsightBrainODS", props);
    }
    isInitialized = true;

    log.info("Initialized the {} data store in {} ms.", ID, System.currentTimeMillis() - start);
  }

  public static void migrate(boolean migrateToNewViolationModel) {
    new DatabaseMigrator().migrate(databaseConfig, ID, dataSource, getUpgradeGuard(migrateToNewViolationModel));
  }

  public static IntConsumer getUpgradeGuard(boolean migrateToNewViolationModel) {
    return currentVersion -> {
      if (currentVersion < MINIMUM_DATABASE_VERSION) {
        throw new UnsupportedOperationException(String.format(
            "Cannot migrate %s database, this requires version %s at minimum, but you have version %s.\n"
                + "Please upgrade to Nexus IQ Server version 1.16 before upgrading to this version.",
            ID, MINIMUM_DATABASE_VERSION, currentVersion));
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

  public static DataSource getDataSource() {
    if (!isInitialized) {
      init(null /* databaseConfig */, false);
    }
    return dataSource;
  }

  public static DataSource getDataSourceWithoutInit() {
    return dataSource;
  }

  public static DatabaseConfig getDatabaseConfig() {
    return databaseConfig;
  }

  public static boolean isDatabaseInMemory() {
    return databaseConfig == null;
  }

  public static EntityManagerFactory getJPAEntityManagerFactory() {
    if (!isInitialized) {
      init(null /* databaseConfig */, false);
    }
    return entityManagerFactory;
  }

  public static EntityManagerFactory getEntityManagerFactoryForLocks() {
    if (!isInitialized) {
      init(null /* databaseConfig */, false);
    }
    return entityManagerFactoryForLocks;
  }

  static synchronized void clear_ForTestsOnly() {
    dataSource = null;
    entityManagerFactory = null;
    entityManagerFactoryForLocks = null;
    databaseConfig = null;
    isDatabaseEmbedded = null;
    isInitialized = false;
  }

  public static boolean isDatabaseEmbedded() {
    return isDatabaseEmbedded;
  }
}
