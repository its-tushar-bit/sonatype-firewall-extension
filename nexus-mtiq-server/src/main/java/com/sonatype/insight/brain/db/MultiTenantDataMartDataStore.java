/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import com.sonatype.insight.brain.db.datastore.AbstractDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.db.DatabaseConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO - multi-tenant implementation
 */
public class MultiTenantDataMartDataStore
    extends AbstractDataStore
    implements DataMartDataStore
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantDataMartDataStore.class);

  private final Map<Tenant, EntityManagerFactory> entityManagerFactoryMap = new ConcurrentHashMap<>();

  private final Map<Tenant, Boolean> isInitializedMap = new ConcurrentHashMap<>();

  public MultiTenantDataMartDataStore(
      final MultiTenantDataSourceFactory dataSourceFactory,
      final DatabaseMigrator databaseMigrator)
  {
    super(dataSourceFactory, databaseMigrator);
  }

  @Override
  protected void init(
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
    props.put("openjpa.jdbc.Schema", getDatabaseSchema());

    entityManagerFactoryMap.put(TenantThreadLocal.getTenant(),
        Persistence.createEntityManagerFactory("InsightBrainDM", props));
    isInitializedMap.put(TenantThreadLocal.getTenant(), true);

    log.info("Initialized the {} data store in {} ms.", getID(), System.currentTimeMillis() - start);
  }

  @Override
  protected boolean isInitialized() {
    return Boolean.TRUE.equals(isInitializedMap.get(TenantThreadLocal.getTenant()));
  }

  @Override
  public String getDatabaseSchema() {
    return TenantThreadLocal.getTenant().databaseSchema;
  }

  @Override
  public EntityManagerFactory getJPAEntityManagerFactory() {
    return entityManagerFactoryMap.get(TenantThreadLocal.getTenant());
  }
}
