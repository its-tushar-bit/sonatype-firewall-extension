/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultDataMartDataStore;
import com.sonatype.insight.db.DatabaseConfig;

public class DatamartProvider
{
  private static DataMartDataStore INSTANCE;

  public static DataMartDataStore getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new DefaultDataMartDataStore(new DataSourceFactory(), new DatabaseMigrator());
    }
    return INSTANCE;
  }

  public static void setInstance(final DataMartDataStore dataMartDataStore) {
    INSTANCE = dataMartDataStore;
  }

  private DatamartProvider() {
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
