/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datastore.DefaultThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.db.DatabaseConfig;

public class ThirdPartyScansProvider
{
  private static ThirdPartyScansDataStore INSTANCE;

  public static ThirdPartyScansDataStore getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new DefaultThirdPartyScansDataStore();
    }
    return INSTANCE;
  }

  public static void setInstance(final ThirdPartyScansDataStore thirdPartyScansDataStore) {
    INSTANCE = thirdPartyScansDataStore;
  }

  private ThirdPartyScansProvider() {
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

  public static EntityManagerFactory getJPAEntityManagerFactory() {
    return getInstance().getJPAEntityManagerFactory();
  }

  static synchronized void clear_ForTestsOnly() {
    if (INSTANCE != null) {
      INSTANCE.clear_ForTestsOnly();
    }
  }
}
