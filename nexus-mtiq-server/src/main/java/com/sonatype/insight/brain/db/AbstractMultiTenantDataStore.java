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

import com.sonatype.insight.brain.db.datasource.MultiTenantPostgresDataSourceProvider;
import com.sonatype.insight.brain.db.datastore.AbstractDataStore;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.db.DatabaseConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMultiTenantDataStore
    extends AbstractDataStore
{
  private static final Logger log = LoggerFactory.getLogger(AbstractMultiTenantDataStore.class);

  private final Map<Tenant, EntityManagerFactory> entityManagerFactoryMap = new ConcurrentHashMap<>();

  private final Map<Tenant, Boolean> isInitializedMap = new ConcurrentHashMap<>();

  public AbstractMultiTenantDataStore(
      final MultiTenantPostgresDataSourceProvider dataSourceProvider,
      final DatabaseConfig databaseConfig)
  {
    super(dataSourceProvider, databaseConfig);
  }

  @Override
  public void initialize() {
    if (isInitialized()) {
      return;
    }

    log.info("Initializing the '{}' data store for tenant schema '{}'.", getID(), getDatabaseSchema());
    long start = System.currentTimeMillis();

    dataSource = dataSourceProvider.getDataSource(databaseConfig, getID());

    Map<String, Object> props = new LinkedHashMap<>();
    props.put("openjpa.ConnectionFactory", dataSource);
    props.put("openjpa.jdbc.Schema", getDatabaseSchema());
    addAdditionalProps(props);

    entityManagerFactoryMap.put(TenantThreadLocal.getTenant(),
        Persistence.createEntityManagerFactory(getFactoryName(), props));
    isInitializedMap.put(TenantThreadLocal.getTenant(), true);

    log.info("Initialized the '{}' data store for tenant schema '{}' in {} ms.", getID(), getDatabaseSchema(),
        System.currentTimeMillis() - start);
  }

  /**
   * Opportunity for implementers to add additional properties for the EntityManagerFactory
   */
  protected void addAdditionalProps(final Map<String, Object> props) {
    // no-op
  }

  /**
   * @return the factory name for JPA defined in the persistence.xml
   */
  protected abstract String getFactoryName();

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
