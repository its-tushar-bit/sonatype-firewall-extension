/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.brain.db.datasource.MultiTenantPostgresDataSourceProvider;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.db.DatabaseConfig;

public class MultiTenantAggregationDataStore
    extends AbstractMultiTenantDataStore
    implements AggregationDataStore
{
  public MultiTenantAggregationDataStore(
      final MultiTenantPostgresDataSourceProvider dataSourceProvider,
      final DatabaseConfig databaseConfig)
  {
    super(dataSourceProvider, databaseConfig);
  }

  @Override
  public boolean isDatabaseEmbedded() {
    // multi-tenant is not compatible with H2
    return false;
  }
}
