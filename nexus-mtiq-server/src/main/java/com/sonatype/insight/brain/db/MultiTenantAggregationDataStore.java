/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;

public class MultiTenantAggregationDataStore
    extends AbstractMultiTenantDataStore
    implements AggregationDataStore
{
  public MultiTenantAggregationDataStore(
      final DataSourceFactory dataSourceFactory,
      final DatabaseMigrator databaseMigrator)
  {
    super(dataSourceFactory, databaseMigrator);
    // Populate the legacy class
    AggregationDataStoreProvider.setInstance(this);
  }

  @Override
  protected String getFactoryName() {
    return "InsightBrainAggregation";
  }
}
