/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datasource.MultiTenantPostgresDataSourceProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.db.DatabaseConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiTenantOperationalDataStore
    extends AbstractMultiTenantDataStore
    implements OperationalDataStore
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantDataMartDataStore.class);

  private DataSource locksDataSource;

  public MultiTenantOperationalDataStore(
      final MultiTenantPostgresDataSourceProvider dataSourceProvider,
      final DatabaseConfig databaseConfig)
  {
    super(dataSourceProvider, databaseConfig);
  }

  @Override
  public void initialize() {
    // short-circuit if we are already initialized
    if (isInitialized()) {
      return;
    }
    super.initialize();

    // Create database items for locks
    MultiTenantPostgresDataSourceProvider multiTenantPostgresDataSourceProvider =
        (MultiTenantPostgresDataSourceProvider) dataSourceProvider;
    locksDataSource = multiTenantPostgresDataSourceProvider.getLocksDataSource();
  }

  @Override
  public DataSource getDataSourceWithoutInit() {
    return dataSource;
  }

  @Override
  public boolean isDatabaseInMemory() {
    // multi-tenant is not compatible with H2
    return false;
  }

  @Override
  public DataSource getDataSourceForLocks() {
    return locksDataSource;
  }

  @Override
  public boolean isDatabaseEmbedded() {
    // multi-tenant is not compatible with H2
    return false;
  }

  @Override
  public void close() throws Exception {
    log.info("Closing {} data store and releasing resources for all tenants.", getID());

    // Close the locks data source if it exists
    if (locksDataSource != null) {
      try {
        if (locksDataSource instanceof AutoCloseable) {
          ((AutoCloseable) locksDataSource).close();
          log.debug("Closed locksDataSource for {} data store.", getID());
        }
      }
      catch (Exception e) {
        log.warn("Error closing locksDataSource for {} data store: {}", getID(), e.getMessage(), e);
      }
    }

    super.close();
  }
}
