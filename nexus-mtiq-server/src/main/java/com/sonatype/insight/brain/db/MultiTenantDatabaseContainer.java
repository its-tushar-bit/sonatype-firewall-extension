/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datasource.MultiTenantPostgresDataSourceProvider;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;

import com.google.common.annotations.VisibleForTesting;

public class MultiTenantDatabaseContainer
    implements DatabaseContainer
{
  private final MultiTenantPostgresDataSourceProvider multiTenantPostgresDataSourceProvider;

  private final DatabaseProvisioner databaseProvisioner;

  private final OperationalDataStore operationalDataStore;

  private final AggregationDataStore aggregationDataStore;

  private final DataMartDataStore dataMartDataStore;

  private final ThirdPartyScansDataStore thirdPartyScansDataStore;

  public MultiTenantDatabaseContainer(final MultiTenantInsightConfig insightConfig) {
    MultiTenantDatabaseConfigProvider multiTenantDatabaseConfigProvider =
        new MultiTenantDatabaseConfigProvider(insightConfig);

    this.multiTenantPostgresDataSourceProvider =
        new MultiTenantPostgresDataSourceProvider(insightConfig.getMainDatabase(), insightConfig.getLocksDatabase());

    this.operationalDataStore = new MultiTenantOperationalDataStore(multiTenantPostgresDataSourceProvider,
        multiTenantDatabaseConfigProvider.getDatabaseConfig(DatabaseName.ods));
    this.aggregationDataStore = new MultiTenantAggregationDataStore(multiTenantPostgresDataSourceProvider,
        multiTenantDatabaseConfigProvider.getDatabaseConfig(DatabaseName.aggregation));
    this.dataMartDataStore = new MultiTenantDataMartDataStore(multiTenantPostgresDataSourceProvider,
        multiTenantDatabaseConfigProvider.getDatabaseConfig(DatabaseName.dm));
    this.thirdPartyScansDataStore = new MultiTenantThirdPartyScansDataStore(multiTenantPostgresDataSourceProvider,
        multiTenantDatabaseConfigProvider.getDatabaseConfig(DatabaseName.third_party_scans));

    this.databaseProvisioner = new DatabaseProvisioner(this);
  }

  @VisibleForTesting
  public MultiTenantDatabaseContainer(
      final MultiTenantPostgresDataSourceProvider multiTenantPostgresDataSourceProvider,
      final DatabaseProvisioner databaseProvisioner,
      final OperationalDataStore operationalDataStore,
      final AggregationDataStore aggregationDataStore,
      final DataMartDataStore dataMartDataStore,
      final ThirdPartyScansDataStore thirdPartyScansDataStore)
  {
    this.multiTenantPostgresDataSourceProvider = multiTenantPostgresDataSourceProvider;
    this.databaseProvisioner = databaseProvisioner;
    this.operationalDataStore = operationalDataStore;
    this.aggregationDataStore = aggregationDataStore;
    this.dataMartDataStore = dataMartDataStore;
    this.thirdPartyScansDataStore = thirdPartyScansDataStore;
  }

  @Override
  public DataSourceProvider getDataSourceProvider() {
    return multiTenantPostgresDataSourceProvider;
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
