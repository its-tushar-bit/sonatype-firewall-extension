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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.33
 */
public class AggregationDataStoreProvider
{
  private static final Logger log = LoggerFactory.getLogger(AggregationDataStoreProvider.class);

  public static final String ID = "insight_brain_aggregation";

  private static DataSource dataSource;

  private static DatabaseConfig databaseConfig;

  private static EntityManagerFactory entityManagerFactory;

  private static volatile boolean isInitialized = false;

  private AggregationDataStoreProvider() {
  }

  public static void init(DatabaseConfig databaseConfig) {
    init(databaseConfig, true);
  }

  public static void initWithoutMigration(DatabaseConfig databaseConfig) {
    init(databaseConfig, false);
  }

  private static synchronized void init(DatabaseConfig databaseConfig, boolean migrateDatabase) {
    if (isInitialized) {
      return;
    }

    log.info("Initializing the {} data store.", ID);
    long start = System.currentTimeMillis();

    AggregationDataStoreProvider.databaseConfig = databaseConfig;
    dataSource = new DataSourceFactory().newDataSource(databaseConfig, ID);
    if (migrateDatabase) {
      migrate();
    }
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("openjpa.ConnectionFactory", dataSource);
    entityManagerFactory = Persistence.createEntityManagerFactory("InsightBrainAggregation", props);
    isInitialized = true;

    log.info("Initialized the {} data store in {} ms.", ID, System.currentTimeMillis() - start);
  }

  public static void migrate() {
    new DatabaseMigrator().migrate(databaseConfig, ID, dataSource);
  }

  public static DataSource getDataSource() {
    if (!isInitialized) {
      init(null /* databaseConfig */);
    }
    return dataSource;
  }

  public static DatabaseConfig getDatabaseConfig() {
    return databaseConfig;
  }

  public static EntityManagerFactory getJPAEntityManagerFactory() {
    if (!isInitialized) {
      init(null /* databaseConfig */);
    }
    return entityManagerFactory;
  }

  static synchronized void clear_ForTestsOnly() {
    databaseConfig = null;
    dataSource = null;
    entityManagerFactory = null;
    isInitialized = false;
  }
}
