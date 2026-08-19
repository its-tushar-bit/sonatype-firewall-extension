/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.H2ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.PostgresAdvisoryLockDAO;
import com.sonatype.insight.brain.dataaccess.lock.PostgresClusterLockManager;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.MultiTenantDatabaseContainer;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MtiqDatabaseConfiguration
{
  private static final Logger log = LoggerFactory.getLogger(MtiqDatabaseConfiguration.class);

  @Bean(name = "databaseContainer")
  @Primary
  public DatabaseContainer databaseContainer(
      MultiTenantInsightConfig insightConfig,
      @Value("${sonatype.database.startup-migrations.enabled:true}") boolean startupMigrationsEnabled)
  {
    log.info("Initializing MTIQ database container");
    MultiTenantDatabaseContainer container = createDatabaseContainer(insightConfig);
    log.info("Initializing MTIQ data stores");
    container.getOperationalDataStore().initialize();
    container.getAggregationDataStore().initialize();
    container.getDataMartDataStore().initialize();
    container.getThirdPartyScansDataStore().initialize();
    log.info("MTIQ data stores initialized");

    if (startupMigrationsEnabled) {
      log.info("Running MTIQ database migrations");
      try {
        DatabaseProvisioner provisioner = container.getDatabaseProvisioner();
        provisioner.initializeDatabaseWithMigration();
        provisioner.validateMinimumSchemaVersion();
        log.info("MTIQ database migrations completed successfully");
      }
      catch (Exception e) {
        log.error("MTIQ database migration failed", e);
        throw new RuntimeException("Failed to run MTIQ database migrations", e);
      }
    }
    else {
      log.info("Skipping MTIQ startup database migrations because command mode disabled them");
    }

    return container;
  }

  protected MultiTenantDatabaseContainer createDatabaseContainer(MultiTenantInsightConfig insightConfig) {
    return new MultiTenantDatabaseContainer(insightConfig);
  }

  @Bean
  public DatabaseProvisioner databaseProvisioner(DatabaseContainer databaseContainer) {
    return databaseContainer.getDatabaseProvisioner();
  }

  @Bean
  public DataSourceProvider dataSourceProvider(DatabaseContainer databaseContainer) {
    return databaseContainer.getDataSourceProvider();
  }

  @Bean
  public OperationalDataStore operationalDataStore(DatabaseContainer databaseContainer) {
    return databaseContainer.getOperationalDataStore();
  }

  @Bean
  public DataSource dataSource(OperationalDataStore operationalDataStore) {
    return operationalDataStore.getDataSource();
  }

  @Bean
  public AggregationDataStore aggregationDataStore(DatabaseContainer databaseContainer) {
    return databaseContainer.getAggregationDataStore();
  }

  @Bean
  public DataMartDataStore dataMartDataStore(DatabaseContainer databaseContainer) {
    return databaseContainer.getDataMartDataStore();
  }

  @Bean
  public ThirdPartyScansDataStore thirdPartyScansDataStore(DatabaseContainer databaseContainer) {
    return databaseContainer.getThirdPartyScansDataStore();
  }

  @Bean
  public TenantMetadataDAO tenantMetadataDAO(OperationalDataStore operationalDataStore) {
    return new TenantMetadataDAO(operationalDataStore);
  }

  @Bean
  public ClusterLockManager clusterLockManager(
      OperationalDataStore operationalDataStore,
      PostgresAdvisoryLockDAO postgresAdvisoryLockDAO)
  {
    if (DatabaseUtil.isDatabaseEmbedded(operationalDataStore.getDatabaseConfig())) {
      log.info("Using H2ClusterLockManager for embedded database");
      return new H2ClusterLockManager();
    }

    log.info("Using PostgresClusterLockManager for PostgreSQL database");
    return new PostgresClusterLockManager(operationalDataStore, postgresAdvisoryLockDAO);
  }
}
