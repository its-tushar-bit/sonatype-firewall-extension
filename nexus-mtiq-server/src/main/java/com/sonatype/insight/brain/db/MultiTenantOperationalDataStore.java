/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.db.DatabaseConfig;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.openjpa.lib.jdbc.JDBCListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiTenantOperationalDataStore
    extends AbstractMultiTenantDataStore
    implements OperationalDataStore
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantDataMartDataStore.class);

  private final Map<Tenant, EntityManagerFactory> entityManagerFactoryForLocks = new ConcurrentHashMap<>();

  public MultiTenantOperationalDataStore(
      final MultiTenantDataSourceFactory dataSourceFactory,
      final DatabaseMigrator databaseMigrator)
  {
    super(dataSourceFactory, databaseMigrator);
    // Populate the legacy class
    OperationalDataStoreProvider.setInstance(this);
  }

  @Override
  protected void init(
      final DatabaseConfig databaseConfig,
      final boolean migrateDatabase,
      final Boolean migrateToNewViolationModel)
  {
    super.init(databaseConfig, migrateDatabase, migrateToNewViolationModel);

    Map<String, Object> props = new LinkedHashMap<>();
    props.put("openjpa.ConnectionFactory", dataSource);
    props.put("openjpa.jdbc.Schema", getDatabaseSchema());

    // smallest pool size should still support max nesting level of locks
    int maxConnections = Math.max(5, Optional.ofNullable(databaseConfig.getMaxConnections()).orElse(50));
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setDriverClassName(databaseConfig.getDriverClassName());
    dataSource.setUrl(databaseConfig.getUrl());
    dataSource.setUsername(databaseConfig.getUsername());
    dataSource.setPassword(databaseConfig.getPassword());
    dataSource.setMaxTotal(maxConnections);
    props.put("openjpa.ConnectionFactory", dataSource);
    entityManagerFactoryForLocks.put(TenantThreadLocal.getTenant(),
        Persistence.createEntityManagerFactory("InsightBrainODS", props));
  }

  @Override
  protected void addAdditionalProps(final Map<String, Object> props) {
    // Add JDBC listeners for performance test framework
    if (SqlCallCounterMetrics.getInstance().getJDBCListener() != null) {
      props.put("openjpa.jdbc.JDBCListeners",
          new JDBCListener[]{SqlCallCounterMetrics.getInstance().getJDBCListener()});
      log.info("Enabled JPA JDBC listener for performance testing.");
    }
  }

  @Override
  protected String getFactoryName() {
    return "InsightBrainODS";
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
  public EntityManagerFactory getEntityManagerFactoryForLocks() {
    return entityManagerFactoryForLocks.get(TenantThreadLocal.getTenant());
  }

  @Override
  public boolean isDatabaseEmbedded() {
    // multi-tenant is not compatible with H2
    return false;
  }
}
