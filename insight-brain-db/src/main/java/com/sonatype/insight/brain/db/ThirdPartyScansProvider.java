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
  private static ThirdPartyScansDataStore INSTANCE = new DefaultThirdPartyScansDataStore();

  public static ThirdPartyScansDataStore getInstance() {
    return INSTANCE;
  }

  public static void setInstance(final ThirdPartyScansDataStore thirdPartyScansDataStore) {
    INSTANCE = thirdPartyScansDataStore;
  }

  private ThirdPartyScansProvider() {
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
