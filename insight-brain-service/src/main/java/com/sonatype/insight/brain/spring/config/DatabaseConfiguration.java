/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.H2ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.PostgresAdvisoryLockDAO;
import com.sonatype.insight.brain.dataaccess.lock.PostgresClusterLockManager;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.DefaultDatabaseContainer;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.service.InsightConfig;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Database configuration for Spring Boot.
 * <p>
 * Creates and initializes the custom database layer (DatabaseContainer, DataStores)
 * that the application uses. This preserves the early database initialization required by the server startup flow.
 * <p>
 * This configuration is excluded from the "test" profile, where
 * {@link com.sonatype.insight.brain.testing.TestDatabaseConfiguration} provides
 * a test-specific DatabaseContainer instead.
 */
@Configuration
@Profile("!test")
public class DatabaseConfiguration
{

  private static final Logger log = LoggerFactory.getLogger(DatabaseConfiguration.class);

  /**
   * Creates the DatabaseContainer which encapsulates all database layer objects.
   * <p>
   * The container creates:
   * <ul>
   * <li>DataSourceProvider - manages database connections</li>
   * <li>OperationalDataStore - main operational database</li>
   * <li>AggregationDataStore - aggregated metrics data</li>
   * <li>DataMartDataStore - data warehouse functions</li>
   * <li>ThirdPartyScansDataStore - third party scan data</li>
   * </ul>
   */
  @Bean
  public DatabaseContainer databaseContainer(
      InsightConfig insightConfig,
      @Value("${sonatype.database.startup-migrations.enabled:true}") boolean startupMigrationsEnabled)
  {
    log.info("Initializing database container");
    DatabaseContainer container = createDatabaseContainer(insightConfig);
    // Initialize the data stores immediately so their DataSources are available
    // This is needed because beans like QuartzJobStoreTX access the data source in their constructor
    log.info("Initializing data stores");
    container.getOperationalDataStore().initialize();
    container.getAggregationDataStore().initialize();
    container.getDataMartDataStore().initialize();
    container.getThirdPartyScansDataStore().initialize();
    log.info("Data stores initialized");

    if (startupMigrationsEnabled) {
      // Run migrations immediately - other beans (like Configuration) query the database in their constructors
      log.info("Running database migrations");
      try {
        DatabaseProvisioner provisioner = container.getDatabaseProvisioner();
        provisioner.initializeDatabaseWithMigration();
        provisioner.validateMinimumSchemaVersion();
        log.info("Database migrations completed successfully");
      }
      catch (Exception e) {
        log.error("Database migration failed", e);
        throw new RuntimeException("Failed to run database migrations", e);
      }
    }
    else {
      log.info("Skipping startup database migrations because command mode disabled them");
    }

    return container;
  }

  protected DatabaseContainer createDatabaseContainer(InsightConfig insightConfig) {
    return new DefaultDatabaseContainer(insightConfig);
  }

  /**
   * Expose the DatabaseProvisioner for database migrations.
   */
  @Bean
  public DatabaseProvisioner databaseProvisioner(DatabaseContainer databaseContainer) {
    return databaseContainer.getDatabaseProvisioner();
  }

  /**
   * Expose the DataSourceProvider.
   */
  @Bean
  public DataSourceProvider dataSourceProvider(DatabaseContainer databaseContainer) {
    return databaseContainer.getDataSourceProvider();
  }

  /**
   * Expose OperationalDataStore.
   */
  @Bean
  public OperationalDataStore operationalDataStore(DatabaseContainer databaseContainer) {
    return databaseContainer.getOperationalDataStore();
  }

  /**
   * Expose the primary DataSource from OperationalDataStore.
   * Many beans (like DatabaseHealthIndicator, JooqConfiguration) require a javax.sql.DataSource.
   */
  @Bean
  public DataSource dataSource(OperationalDataStore operationalDataStore) {
    return operationalDataStore.getDataSource();
  }

  /**
   * Expose AggregationDataStore.
   */
  @Bean
  public AggregationDataStore aggregationDataStore(DatabaseContainer databaseContainer) {
    return databaseContainer.getAggregationDataStore();
  }

  /**
   * Expose DataMartDataStore.
   */
  @Bean
  public DataMartDataStore dataMartDataStore(DatabaseContainer databaseContainer) {
    return databaseContainer.getDataMartDataStore();
  }

  /**
   * Expose ThirdPartyScansDataStore.
   */
  @Bean
  public ThirdPartyScansDataStore thirdPartyScansDataStore(DatabaseContainer databaseContainer) {
    return databaseContainer.getThirdPartyScansDataStore();
  }

  /**
   * Provides the appropriate ClusterLockManager implementation based on database type.
   * Uses H2ClusterLockManager for embedded H2 databases, PostgresClusterLockManager for PostgreSQL.
   */
  @Bean
  public ClusterLockManager clusterLockManager(
      OperationalDataStore operationalDataStore,
      PostgresAdvisoryLockDAO postgresAdvisoryLockDAO)
  {
    if (DatabaseUtil.isDatabaseEmbedded(operationalDataStore.getDatabaseConfig())) {
      log.info("Using H2ClusterLockManager for embedded database");
      return new H2ClusterLockManager();
    }
    else {
      log.info("Using PostgresClusterLockManager for PostgreSQL database");
      return new PostgresClusterLockManager(operationalDataStore, postgresAdvisoryLockDAO);
    }
  }

}
