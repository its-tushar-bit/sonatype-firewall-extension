/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManagerProvider;
import com.sonatype.insight.brain.dataaccess.lock.PostgresAdvisoryLockDAO;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.rule.DatabaseContainerRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that binds DataStore instances from a DatabaseContainerRule.
 * <p>
 * Uses method-based beans to ensure we always get the current DataStore instance, which is critical
 * when database fixtures switch types (H2 <-> Postgres) - the old DataStore instances
 * must be released to prevent memory leaks of JDBCConfigurationImpl.
 * </p>
 */
@Configuration
public class DataStoreTestModule
{
  private final DatabaseContainerRule databaseContainerRule;

  public DataStoreTestModule(final DatabaseContainerRule databaseContainerRule) {
    this.databaseContainerRule = databaseContainerRule;
  }

  @Bean
  public OperationalDataStore operationalDataStore() {
    return databaseContainerRule.getOperationalDataStore();
  }

  @Bean
  public AggregationDataStore aggregationDataStore() {
    return databaseContainerRule.getAggregationDataStore();
  }

  @Bean
  public DataMartDataStore dataMartDataStore() {
    return databaseContainerRule.getDataMartDataStore();
  }

  @Bean
  public ThirdPartyScansDataStore thirdPartyScansDataStore() {
    return databaseContainerRule.getThirdPartyScansDataStore();
  }

  @Bean
  public DataStoreProvider dataStoreProvider() {
    return databaseContainerRule.getDatabaseContainer();
  }

  @Bean
  public ClusterLockManagerProvider clusterLockManagerProvider(
      OperationalDataStore operationalDataStore,
      PostgresAdvisoryLockDAO postgresAdvisoryLockDAO)
  {
    return new ClusterLockManagerProvider(operationalDataStore, postgresAdvisoryLockDAO);
  }

  @Bean
  public ClusterLockManager clusterLockManager(ClusterLockManagerProvider provider) {
    return provider.get();
  }
}
