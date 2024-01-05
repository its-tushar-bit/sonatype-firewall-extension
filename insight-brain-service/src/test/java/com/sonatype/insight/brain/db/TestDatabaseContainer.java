/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;

import org.mockito.Mockito;

/**
 * Implementation of {@link DatabaseContainer} for testing. The encapsulated {@link DatabaseProvisionUtils} is a Mockito
 * @{@link Spy}
 */
public class TestDatabaseContainer
    implements DatabaseContainer
{
  private final DataSourceProvider dataSourceProvider;

  private final DatabaseProvisionUtils databaseProvisionUtils;

  private final DataStoreProvider dataStoreProvider;

  public TestDatabaseContainer(
      final DataSourceProvider dataSourceProvider,
      final DataStoreProvider dataStoreProvider)
  {
    this.dataSourceProvider = dataSourceProvider;
    this.dataStoreProvider = dataStoreProvider;

    this.databaseProvisionUtils = Mockito.spy(
        new DatabaseProvisionUtils(getOperationalDataStore(), getAggregationDataStore(),
            getDataMartDataStore(), getThirdPartyScansDataStore()));
  }

  @Override
  public DataSourceProvider getDataSourceProvider() {
    return dataSourceProvider;
  }

  @Override
  public DatabaseProvisionUtils getDatabaseProvisionUtils() {
    return databaseProvisionUtils;
  }

  @Override
  public OperationalDataStore getOperationalDataStore() {
    return dataStoreProvider.getOperationalDataStore();
  }

  @Override
  public AggregationDataStore getAggregationDataStore() {
    return dataStoreProvider.getAggregationDataStore();
  }

  @Override
  public DataMartDataStore getDataMartDataStore() {
    return dataStoreProvider.getDataMartDataStore();
  }

  @Override
  public ThirdPartyScansDataStore getThirdPartyScansDataStore() {
    return dataStoreProvider.getThirdPartyScansDataStore();
  }

  public void resetMocks() {
    Mockito.reset(databaseProvisionUtils);
  }
}
