/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseConfig;

import org.apache.openjpa.lib.jdbc.JDBCListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperationalDataStoreProvider
{
  private static final Logger log = LoggerFactory.getLogger(OperationalDataStoreProvider.class);

  public static final String ID = "insight_brain_ods";

  static final int MINIMUM_DATABASE_VERSION = 85;

  static final int OLD_VIOLATION_MODEL_DATABASE_VERSION = 114;

  private static DataSource dataSource;

  private static DatabaseConfig databaseConfig;

  private static EntityManagerFactory entityManagerFactory;

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
    if (migrateDatabase) {
      new H2DatabaseMigrator().migrate(databaseConfig, ID, dataSource, currentVersion -> {
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
      });
    }
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("openjpa.ConnectionFactory", dataSource);

    if (SqlCallCounterMetrics.getInstance().getJDBCListener() != null) {
      props.put("openjpa.jdbc.JDBCListeners",
          new JDBCListener[] {SqlCallCounterMetrics.getInstance().getJDBCListener()});
      log.info("Enabled JPA JDBC listener for performance testing.");
    }

    entityManagerFactory = Persistence.createEntityManagerFactory("InsightBrainODS", props);
    isInitialized = true;

    log.info("Initialized the {} data store in {} ms.", ID, System.currentTimeMillis() - start);
  }

  public static DataSource getDataSource() {
    if (!isInitialized) {
      init(null /* databaseConfig */, false);
    }
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

  static synchronized void clear_ForTestsOnly() {
    dataSource = null;
    entityManagerFactory = null;
    databaseConfig = null;
    isInitialized = false;
  }
}
