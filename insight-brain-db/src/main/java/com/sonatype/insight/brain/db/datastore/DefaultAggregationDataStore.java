/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.db.DatabaseConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.33
 */
public class DefaultAggregationDataStore
    extends AbstractDataStore
    implements AggregationDataStore
{
  private static final Logger log = LoggerFactory.getLogger(DefaultAggregationDataStore.class);

  private EntityManagerFactory entityManagerFactory;

  private volatile boolean isInitialized = false;

  public DefaultAggregationDataStore(
      final DataSourceFactory dataSourceFactory,
      final DatabaseMigrator databaseMigrator)
  {
    super(dataSourceFactory, databaseMigrator);
  }

  @Override
  protected synchronized void init(
      final DatabaseConfig databaseConfig,
      final boolean migrateDatabase,
      final Boolean migrateToNewViolationModel)
  {
    if (isInitialized()) {
      return;
    }

    log.info("Initializing the {} data store.", getID());
    long start = System.currentTimeMillis();

    this.databaseConfig = databaseConfig;
    dataSource = dataSourceFactory.createNewDataSource(databaseConfig, getID(), getDatabaseSchema());
    if (migrateDatabase) {
      migrate(migrateToNewViolationModel);
    }
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("openjpa.ConnectionFactory", dataSource);
    entityManagerFactory = Persistence.createEntityManagerFactory("InsightBrainAggregation", props);
    isInitialized = true;

    log.info("Initialized the {} data store in {} ms.", getID(), System.currentTimeMillis() - start);
  }

  @Override
  public String getDatabaseSchema() {
    return ID;
  }

  @Override
  protected boolean isInitialized() {
    return isInitialized;
  }

  @Override
  public EntityManagerFactory getJPAEntityManagerFactory() {
    if (!isInitialized()) {
      initWithMigration(null /* databaseConfig */, false);
    }
    return entityManagerFactory;
  }

  @Override
  public void clear_ForTestsOnly() {
    super.clear_ForTestsOnly();
    entityManagerFactory = null;
    isInitialized = false;
  }
}
