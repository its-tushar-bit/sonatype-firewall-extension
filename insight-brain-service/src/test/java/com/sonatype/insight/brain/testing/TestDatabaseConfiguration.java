/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.H2ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.PostgresAdvisoryLockDAO;
import com.sonatype.insight.brain.dataaccess.lock.PostgresClusterLockManager;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test-specific database configuration.
 * <p>
 * This configuration is active when the "test" Spring profile is set.
 * It provides beans from the test's pre-existing DatabaseContainer (created by DatabaseContainerRule)
 * rather than creating a new one from scratch.
 * <p>
 * This replaces the production {@link com.sonatype.insight.brain.spring.config.DatabaseConfiguration}
 * in the test context.
 */
@Configuration
@Profile("test")
public class TestDatabaseConfiguration
{
  private static final Logger log = LoggerFactory.getLogger(TestDatabaseConfiguration.class);

  /**
   * Provides the DatabaseContainer from the test holder.
   * <p>
   * The container must have been set via {@link TestDatabaseContainerHolder#set(DatabaseContainer)}
   * before the Spring context is started (typically in @Before or via a JUnit Rule).
   *
   * @throws IllegalStateException if no container has been set
   */
  @Bean(destroyMethod = "")
  @Primary
  public static DatabaseContainer databaseContainer() {
    DatabaseContainer container = TestDatabaseContainerHolder.get();
    if (container == null) {
      throw new IllegalStateException(
          "DatabaseContainer not set in TestDatabaseContainerHolder. "
              + "Ensure setDatabaseContainer() is called before starting the Spring context.");
    }

    log.info("Using test DatabaseContainer from holder");
    DatabaseProvisioner provisioner = container.getDatabaseProvisioner();
    provisioner.initializeDatabaseWithMigration();
    provisioner.validateMinimumSchemaVersion();
    return container;
  }

  /**
   * Expose the DatabaseProvisioner.
   */
  @Bean(destroyMethod = "")
  @Primary
  public static DatabaseProvisioner databaseProvisioner(DatabaseContainer databaseContainer) {
    return databaseContainer.getDatabaseProvisioner();
  }

  /**
   * Expose the DataSourceProvider.
   */
  @Bean(destroyMethod = "")
  @Primary
  public static DataSourceProvider dataSourceProvider(DatabaseContainer databaseContainer) {
    return databaseContainer.getDataSourceProvider();
  }

  /**
   * Expose OperationalDataStore.
   */
  @Bean(destroyMethod = "")
  @Primary
  public static OperationalDataStore operationalDataStore(DatabaseContainer databaseContainer) {
    return databaseContainer.getOperationalDataStore();
  }

  /**
   * Expose the primary DataSource from OperationalDataStore.
   */
  @Bean(destroyMethod = "")
  @Primary
  public static DataSource dataSource(OperationalDataStore operationalDataStore) {
    return operationalDataStore.getDataSource();
  }

  /**
   * Expose AggregationDataStore.
   */
  @Bean(destroyMethod = "")
  @Primary
  public static AggregationDataStore aggregationDataStore(DatabaseContainer databaseContainer) {
    return databaseContainer.getAggregationDataStore();
  }

  /**
   * Expose DataMartDataStore.
   */
  @Bean(destroyMethod = "")
  @Primary
  public static DataMartDataStore dataMartDataStore(DatabaseContainer databaseContainer) {
    return databaseContainer.getDataMartDataStore();
  }

  /**
   * Expose ThirdPartyScansDataStore.
   */
  @Bean(destroyMethod = "")
  @Primary
  public static ThirdPartyScansDataStore thirdPartyScansDataStore(DatabaseContainer databaseContainer) {
    return databaseContainer.getThirdPartyScansDataStore();
  }

  /**
   * Provide the same cluster-lock implementation selection as the production database configuration.
   */
  @Bean
  @Primary
  public static ClusterLockManager clusterLockManager(
      OperationalDataStore operationalDataStore,
      PostgresAdvisoryLockDAO postgresAdvisoryLockDAO)
  {
    if (DatabaseUtil.isDatabaseEmbedded(operationalDataStore.getDatabaseConfig())) {
      return new H2ClusterLockManager();
    }
    return new PostgresClusterLockManager(operationalDataStore, postgresAdvisoryLockDAO);
  }
}
