/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.db.DatabaseConfig;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared logic that is applicable to all {@link DataStore} implementations.
 */
public abstract class AbstractDataStore
    implements DataStore
{
  private static final Logger log = LoggerFactory.getLogger(AbstractDataStore.class);

  protected DataSource dataSource;

  protected final DatabaseConfig databaseConfig;

  protected final DataSourceProvider dataSourceProvider;

  protected boolean isDataStoreNew;

  public AbstractDataStore(final DataSourceProvider dataSourceProvider, final DatabaseConfig databaseConfig) {
    this.dataSourceProvider = dataSourceProvider;
    this.databaseConfig = databaseConfig;
  }

  @Override
  public DatabaseConfig getDatabaseConfig() {
    return databaseConfig;
  }

  @Override
  public DataSource getDataSource() {
    return dataSource;
  }

  /**
   * Track if the data store is initialized or not. Data store initialization is only allowed once and can be done
   * lazily (for tests only) which will imply a null {@link DatabaseConfig}.
   */
  protected abstract boolean isInitialized();

  @Override
  public DataSourceProvider getDataSourceProvider() {
    return dataSourceProvider;
  }

  @Override
  public boolean isDataStoreNew() {
    return isDataStoreNew;
  }

  @Override
  public void close() throws Exception {
    log.info("Closing {} data store and releasing resources.", getID());

    // Close the EntityManagerFactory which internally cleans up OpenJPA's JDBCConfigurationImpl
    EntityManagerFactory entityManagerFactory = getJPAEntityManagerFactory();
    if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
      try {
        entityManagerFactory.close();
        log.debug("Closed EntityManagerFactory for {} data store.", getID());
      }
      catch (Exception e) {
        log.warn("Error closing EntityManagerFactory for {} data store: {}", getID(), e.getMessage(), e);
      }
    }

    setInitializedFalse();
    log.info("Successfully closed {} data store.", getID());
  }

  protected abstract void setInitializedFalse();
}
