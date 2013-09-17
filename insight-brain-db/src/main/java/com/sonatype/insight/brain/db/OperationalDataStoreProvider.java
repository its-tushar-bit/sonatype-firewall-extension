/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
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

public class OperationalDataStoreProvider
{
  private static final Logger log = LoggerFactory.getLogger(OperationalDataStoreProvider.class);

  public static final int DESIRED_DATABASE_VERSION = 26;

  public static final String ID = "insight_brain_ods";

  private static DataSource dataSource;

  private static EntityManagerFactory entityManagerFactory;

  private static volatile boolean isInitialized = false;

  private OperationalDataStoreProvider() {
  }

  public static synchronized void init(DatabaseConfig databaseConfig) {
    if (isInitialized) {
      return;
    }

    log.info("Initializing the {} data store.", ID);
    long start = System.currentTimeMillis();

    dataSource = new DataSourceFactory().newDataSource(databaseConfig, ID);
    new H2DatabaseMigrator()
        .migrate(databaseConfig, ID, dataSource, DESIRED_DATABASE_VERSION, 6 /* defaultCurrentVersion */);
    Map<String, Object> props = new LinkedHashMap<String, Object>();
    props.put("openjpa.ConnectionFactory", dataSource);
    entityManagerFactory = Persistence.createEntityManagerFactory("InsightBrainODS", props);
    isInitialized = true;

    log.info("Initialized the {} data store in {} ms.", ID, System.currentTimeMillis() - start);
  }

  public static DataSource getDataSource() {
    if (!isInitialized) {
      init(null /* databaseConfig */);
    }
    return dataSource;
  }

  public static EntityManagerFactory getJPAEntityManagerFactory() {
    if (!isInitialized) {
      init(null /* databaseConfig */);
    }
    return entityManagerFactory;
  }

  static synchronized void clear_ForTestsOnly() {
    dataSource = null;
    entityManagerFactory = null;
    isInitialized = false;
  }
}
