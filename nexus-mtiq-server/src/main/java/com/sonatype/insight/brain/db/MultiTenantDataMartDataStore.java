/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.util.LinkedHashMap;
import java.util.Map;

import com.sonatype.insight.brain.db.datasource.MultiTenantPostgresDataSourceProvider;
import com.sonatype.insight.brain.db.datastore.AbstractDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.db.DatabaseConfig;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiTenantDataMartDataStore
    extends AbstractDataStore
    implements DataMartDataStore
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantDataMartDataStore.class);

  boolean isGlobalDataMartInitialized = false;

  EntityManagerFactory globalDataMartEntityManagerFactory;

  public MultiTenantDataMartDataStore(
      final MultiTenantPostgresDataSourceProvider dataSourceProvider,
      final DatabaseConfig databaseConfig)
  {
    super(dataSourceProvider, databaseConfig);
  }

  /**
   * Initialize the DataMart data store. This is a singleton data store that is shared across all tenants. The DataMart
   * resides in the global tenant schema in MTIQ.
   */
  @Override
  public void initialize() {
    if (isInitialized()) {
      return;
    }

    log.info("Initializing the single '{}' data store for all tenants in schema '{}'.", getID(), getDatabaseSchema());
    long start = System.currentTimeMillis();

    dataSource = dataSourceProvider.getDataSource(databaseConfig, getID());

    Map<String, Object> props = new LinkedHashMap<>();
    props.put("openjpa.ConnectionFactory", dataSource);
    props.put("openjpa.jdbc.Schema", getDatabaseSchema());

    globalDataMartEntityManagerFactory = Persistence.createEntityManagerFactory(getFactoryName(), props);
    isGlobalDataMartInitialized = true;

    log.info("Initialized the single '{}' data store for all tenants in schema '{}' in {} ms.", getID(),
        getDatabaseSchema(), System.currentTimeMillis() - start);
  }

  private String getFactoryName() {
    return "InsightBrainDM";
  }

  @Override
  public String getDatabaseSchema() {
    // The DataMart resides in the global schema in MTIQ
    return Tenant.GLOBAL_TENANT.databaseSchema;
  }

  @Override
  protected boolean isInitialized() {
    // The DataMart reuses the global DataStore in MTIQ
    return isGlobalDataMartInitialized;
  }

  @Override
  public EntityManagerFactory getJPAEntityManagerFactory() {
    // The DataMart reuses the global DataStore in MTIQ
    return globalDataMartEntityManagerFactory;
  }

  @Override
  public boolean isDatabaseEmbedded() {
    // multi-tenant is not compatible with H2
    return false;
  }

  @Override
  protected void setInitializedFalse() {
    isGlobalDataMartInitialized = false;
  }
}
