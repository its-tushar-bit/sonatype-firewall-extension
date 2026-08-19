/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import java.util.Optional;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.DbApplicationNameGenerator;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.db.DatabaseConfig;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultOperationalDataStore
    extends AbstractDataStore
    implements OperationalDataStore
{
  private static final Logger log = LoggerFactory.getLogger(DefaultOperationalDataStore.class);

  private DataSource dataSourceForLocks;

  private volatile boolean isInitialized = false;

  private Boolean isDatabaseEmbedded;

  public DefaultOperationalDataStore(final DataSourceProvider dataSourceProvider, final DatabaseConfig databaseConfig) {
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

    isDatabaseEmbedded = DatabaseUtil.isDatabaseEmbedded(databaseConfig);

    if (isDatabaseEmbedded) {
      dataSourceForLocks = null; // H2 doesn't use this, it implements ClusterLock using Java Semaphores
    }
    else {
      // smallest pool size should still support max nesting level of locks
      int maxConnections = Math.max(5, Optional.ofNullable(databaseConfig.getMaxConnections()).orElse(50));
      BasicDataSource dataSourceForLocks = new BasicDataSource();
      dataSourceForLocks.setAutoCommitOnReturn(false);
      dataSourceForLocks.setDefaultAutoCommit(false);
      dataSourceForLocks.setDriverClassName(databaseConfig.getDriverClassName());
      dataSourceForLocks.setUrl(databaseConfig.getUrl());
      dataSourceForLocks.setUsername(databaseConfig.getUsername());
      dataSourceForLocks.setPassword(databaseConfig.getPassword());
      dataSourceForLocks.setMaxTotal(maxConnections);
      dataSourceForLocks.addConnectionProperty("ApplicationName",
          new DbApplicationNameGenerator().generateApplicationNameWithHost("IQ-locks"));

      this.dataSourceForLocks = dataSourceForLocks;
    }
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
  public DataSource getDataSourceWithoutInit() {
    return dataSource;
  }

  @Override
  public boolean isDatabaseInMemory() {
    return DatabaseUtil.isInMemoryDatabase(databaseConfig);
  }

  @Override
  public DataSource getDataSourceForLocks() {
    return dataSourceForLocks;
  }

  @Override
  public boolean isDatabaseEmbedded() {
    return isDatabaseEmbedded;
  }

  @Override
  public void close() throws Exception {
    // Close the dataSourceForLocks BasicDataSource
    if (dataSourceForLocks != null && dataSourceForLocks instanceof BasicDataSource) {
      try {
        ((BasicDataSource) dataSourceForLocks).close();
        log.debug("Closed dataSourceForLocks for {} data store.", getID());
      }
      catch (Exception e) {
        log.warn("Error closing dataSourceForLocks for {} data store: {}", getID(), e.getMessage(), e);
      }
    }

    super.close();
  }

  @Override
  protected void setInitializedFalse() {
    isInitialized = false;
  }
}
