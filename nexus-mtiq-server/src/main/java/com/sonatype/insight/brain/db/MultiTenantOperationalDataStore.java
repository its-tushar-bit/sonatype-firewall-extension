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
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datasource.MultiTenantPostgresDataSourceProvider;
import com.sonatype.insight.brain.db.cache.MultiTenantQueryCache;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.db.DatabaseConfig;

import org.apache.openjpa.datacache.DataCacheMode;
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
    DataSource locksDataSource = multiTenantPostgresDataSourceProvider.getLocksDataSource();
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("openjpa.ConnectionFactory", locksDataSource);
    props.put("openjpa.jdbc.Schema", getDatabaseSchema());
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

    props.put("openjpa.DataCache", "true(CacheSize=8192, SoftReferenceSize=0, EnableStatistics=true)");
    props.put("openjpa.QueryCache", MultiTenantQueryCache.class.getName() + "(CacheSize=1000, SoftReferenceSize=0)");
    props.put("javax.persistence.sharedCache.mode", DataCacheMode.ENABLE_SELECTIVE.name());
    props.put("openjpa.RemoteCommitProvider", "sjvm");
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
