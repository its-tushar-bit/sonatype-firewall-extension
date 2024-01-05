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

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
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

  public DefaultAggregationDataStore(final DataSourceProvider dataSourceProvider, final DatabaseConfig databaseConfig) {
    super(dataSourceProvider, databaseConfig);
  }

  @Override
  public synchronized void initialize() {
    if (isInitialized()) {
      return;
    }

    log.info("Initializing the {} data store.", getID());
    long start = System.currentTimeMillis();

    dataSource = dataSourceProvider.getDataSource(databaseConfig, getID());
    isDataStoreNew = !DatabaseUtil.schemaExists(dataSource, getDatabaseSchema());
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
    return entityManagerFactory;
  }
}
