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

import org.mockito.Mockito;
import org.mockito.Spy;

/**
 * Implementation of {@link DatabaseContainer} for testing. The encapsulated {@link DatabaseProvisioner} is a Mockito
 * {@link Spy}
 */
public class TestDatabaseContainer
    implements DatabaseContainer
{
  private final DataSourceProvider dataSourceProvider;

  private final DatabaseProvisioner databaseProvisioner;

  private final DataStoreProvider dataStoreProvider;

  public TestDatabaseContainer(
      final DataSourceProvider dataSourceProvider,
      final DataStoreProvider dataStoreProvider)
  {
    this.dataSourceProvider = dataSourceProvider;
    this.dataStoreProvider = dataStoreProvider;

    this.databaseProvisioner = Mockito.spy(new DatabaseProvisioner(this));
  }

  @Override
  public DataSourceProvider getDataSourceProvider() {
    return dataSourceProvider;
  }

  @Override
  public DatabaseProvisioner getDatabaseProvisioner() {
    return databaseProvisioner;
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
    Mockito.reset(databaseProvisioner);
  }
}
