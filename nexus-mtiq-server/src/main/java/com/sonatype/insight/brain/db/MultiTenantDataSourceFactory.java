/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import javax.sql.DataSource;

import com.sonatype.insight.db.AbstractDatabaseSchemaPopulator;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseEngine;

public class MultiTenantDataSourceFactory
    extends DataSourceFactory
{
  private DataSource multiTenantDataSource;

  @Override
  public DataSource createNewDataSource(
      final DatabaseConfig databaseConfig,
      final String dataStoreId,
      final String databaseSchema)
  {
    // For MTIQ we only create one data source (i.e. connection pool)
    if (multiTenantDataSource == null) {
      // Note: We directly are calling super.loadDataSource here, we do not need the logic in super.createNewDataSource
      // See CLM-23241 which aims to refactor the *DataSourceFactory classes
      multiTenantDataSource = loadDataSource(databaseConfig, databaseSchema);
    }
    return multiTenantDataSource;
  }

  @Override
  protected AbstractDatabaseSchemaPopulator createDatabaseSchemaPopulator(
      final DataSource dataSource,
      final DatabaseEngine databaseEngine,
      final String dataStoreId,
      final String databaseSchema)
  {
    return new MultiTenantDatabaseSchemaPopulator(dataSource, dataStoreId, databaseSchema);
  }
}
