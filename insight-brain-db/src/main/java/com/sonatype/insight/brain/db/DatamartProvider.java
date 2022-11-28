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
  private static DataMartDataStore INSTANCE = new DefaultDataMartDataStore();

  public static DataMartDataStore getInstance() {

    return INSTANCE;
  }

  public static void setInstance(final DataMartDataStore dataMartDataStore) {
    INSTANCE = dataMartDataStore;
  }

  private DatamartProvider() {
  }

  public static void init(DatabaseConfig databaseConfig) {
    INSTANCE.initWithMigration(databaseConfig, null);
  }

  public static void initWithoutMigration(DatabaseConfig databaseConfig) {
    INSTANCE.initWithoutMigration(databaseConfig);
  }

  public static void migrate() {
    INSTANCE.migrate(false);
  }

  public static DataSource getDataSource() {
    return INSTANCE.getDataSource();
  }

  public static DatabaseConfig getDatabaseConfig() {
    return INSTANCE.getDatabaseConfig();
  }

  public static EntityManagerFactory getJPAEntityManagerFactory() {
    return INSTANCE.getJPAEntityManagerFactory();
  }

  static synchronized void clear_ForTestsOnly() {
    INSTANCE.clear_ForTestsOnly();
  }
}
