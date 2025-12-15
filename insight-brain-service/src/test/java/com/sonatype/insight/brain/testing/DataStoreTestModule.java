/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManagerProvider;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.rule.DatabaseContainerRule;

import com.google.inject.Binder;
import com.google.inject.Module;

/**
 * Guice module that binds DataStore instances from a DatabaseContainerRule.
 * <p>
 * Uses providers to ensure we always get the current DataStore instance, which is critical
 * when database fixtures switch types (H2 <-> Postgres) - the old DataStore instances
 * must be released to prevent memory leaks of JDBCConfigurationImpl.
 * </p>
 */
public class DataStoreTestModule
    implements Module
{
  private final DatabaseContainerRule databaseContainerRule;

  public DataStoreTestModule(final DatabaseContainerRule databaseContainerRule) {
    this.databaseContainerRule = databaseContainerRule;
  }

  @Override
  public void configure(final Binder binder) {
    binder.bind(OperationalDataStore.class).toProvider(() -> databaseContainerRule.getOperationalDataStore());
    binder.bind(AggregationDataStore.class).toProvider(() -> databaseContainerRule.getAggregationDataStore());
    binder.bind(DataMartDataStore.class).toProvider(() -> databaseContainerRule.getDataMartDataStore());
    binder.bind(ThirdPartyScansDataStore.class).toProvider(() -> databaseContainerRule.getThirdPartyScansDataStore());
    binder.bind(DataStoreProvider.class).toInstance(databaseContainerRule.getDatabaseContainer());
    binder.bind(ClusterLockManager.class).toProvider(ClusterLockManagerProvider.class);
  }
}
