/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultAggregationDataStore;
import com.sonatype.insight.db.DatabaseConfig;

/**
 * @since 1.33
 */
public class AggregationDataStoreProvider
{
  private static AggregationDataStore INSTANCE;

  public static AggregationDataStore getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new DefaultAggregationDataStore();
    }
    return INSTANCE;
  }

  public static void setInstance(final AggregationDataStore aggregationDataStore) {
    INSTANCE = aggregationDataStore;
  }

  private AggregationDataStoreProvider() {
    // private ctor
  }

  public static void init(DatabaseConfig databaseConfig) {
    getInstance().initWithMigration(databaseConfig, null);
  }

  public static void initWithoutMigration(DatabaseConfig databaseConfig) {
    getInstance().initWithoutMigration(databaseConfig);
  }

  public static void migrate() {
    getInstance().migrate(false);
  }

  public static DataSource getDataSource() {
    return getInstance().getDataSource();
  }

  public static DatabaseConfig getDatabaseConfig() {
    return getInstance().getDatabaseConfig();
  }

  public static String getDatabaseSchema() {
    return getInstance().getDatabaseSchema();
  }

  public static EntityManagerFactory getJPAEntityManagerFactory() {
    return getInstance().getJPAEntityManagerFactory();
  }

  static synchronized void clear_ForTestsOnly() {
    if (INSTANCE != null) {
      INSTANCE.clear_ForTestsOnly();
    }
  }
}
