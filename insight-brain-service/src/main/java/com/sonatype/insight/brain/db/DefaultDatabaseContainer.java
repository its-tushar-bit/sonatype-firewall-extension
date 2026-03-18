/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datasource.DataSourceProviderFactory;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.db.datastore.DefaultAggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultDataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultOperationalDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.service.InsightConfig;

import com.google.common.annotations.VisibleForTesting;

public class DefaultDatabaseContainer
    implements DatabaseContainer
{
  private final DataSourceProvider dataSourceProvider;

  private final DatabaseProvisioner databaseProvisioner;

  private final OperationalDataStore operationalDataStore;

  private final AggregationDataStore aggregationDataStore;

  private final DataMartDataStore dataMartDataStore;

  private final ThirdPartyScansDataStore thirdPartyScansDataStore;

  /**
   * Default constructor which will produce all default database related objects
   */
  public DefaultDatabaseContainer(final InsightConfig insightConfig) {
    DatabaseConfigProvider databaseConfigProvider =
        DatabaseConfigProviderFactory.createDatabaseConfigProvider(insightConfig);

    this.dataSourceProvider =
        DataSourceProviderFactory.createDataSourceProvider(databaseConfigProvider.getDatabaseEngine());

    operationalDataStore =
        new DefaultOperationalDataStore(dataSourceProvider, databaseConfigProvider.getDatabaseConfig(DatabaseName.ods));
    aggregationDataStore = new DefaultAggregationDataStore(dataSourceProvider,
        databaseConfigProvider.getDatabaseConfig(DatabaseName.aggregation));
    dataMartDataStore =
        new DefaultDataMartDataStore(dataSourceProvider, databaseConfigProvider.getDatabaseConfig(DatabaseName.dm));
    thirdPartyScansDataStore = new DefaultThirdPartyScansDataStore(dataSourceProvider,
        databaseConfigProvider.getDatabaseConfig(DatabaseName.third_party_scans));

    this.databaseProvisioner = new DatabaseProvisioner(this);
  }

  @VisibleForTesting
  public DefaultDatabaseContainer(
      final DataSourceProvider dataSourceProvider,
      final DataStoreProvider dataStoreProvider,
      final DatabaseProvisioner databaseProvisioner)
  {
    this.dataSourceProvider = dataSourceProvider;
    this.databaseProvisioner = databaseProvisioner;
    this.operationalDataStore = dataStoreProvider.getOperationalDataStore();
    this.aggregationDataStore = dataStoreProvider.getAggregationDataStore();
    this.dataMartDataStore = dataStoreProvider.getDataMartDataStore();
    this.thirdPartyScansDataStore = dataStoreProvider.getThirdPartyScansDataStore();
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
    return operationalDataStore;
  }

  @Override
  public AggregationDataStore getAggregationDataStore() {
    return aggregationDataStore;
  }

  @Override
  public DataMartDataStore getDataMartDataStore() {
    return dataMartDataStore;
  }

  @Override
  public ThirdPartyScansDataStore getThirdPartyScansDataStore() {
    return thirdPartyScansDataStore;
  }
}
