/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.brain.db.datasource.MultiTenantPostgresDataSourceProvider;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.db.DatabaseConfig;

public class MultiTenantDataMartDataStore
    extends AbstractMultiTenantDataStore
    implements DataMartDataStore
{
  public MultiTenantDataMartDataStore(
      final MultiTenantPostgresDataSourceProvider dataSourceProvider,
      final DatabaseConfig databaseConfig)
  {
    super(dataSourceProvider, databaseConfig);
  }

  @Override
  protected String getFactoryName() {
    return "InsightBrainDM";
  }

  @Override
  public String getDatabaseSchema() {
    // The DataMart resides in the global schema in MTIQ
    return Tenant.GLOBAL_TENANT.databaseSchema;
  }

  @Override
  public boolean isDatabaseEmbedded() {
    // multi-tenant is not compatible with H2
    return false;
  }
}
