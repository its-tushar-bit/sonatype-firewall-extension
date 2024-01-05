/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datasource;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseEngine;

/**
 * TODO: This class will be removed at the end of the liquibase move - CLM-26741
 * Implementation of {@link DataSourceProvider} which wraps the legacy {@link DataSourceFactory}.
 */
@Deprecated
public class LegacyDataSourceFactory
    implements DataSourceProvider, LegacyDataSourceProvider
{
  private final DataSourceFactory dataSourceFactory;

  public LegacyDataSourceFactory() {
    dataSourceFactory = new DataSourceFactory();
  }

  @Override
  public DataSource getDataSource(final DatabaseConfig databaseConfig, final String dataStoreId) {
    return dataSourceFactory.createNewDataSource(databaseConfig, dataStoreId, dataStoreId);
  }

  @Override
  public boolean populateDbSchema(
      final DataSource dataSource,
      final DatabaseEngine databaseEngine,
      final String dataStoreId,
      final String databaseSchema)
  {
    return dataSourceFactory.populateDbSchema(dataSource, databaseEngine, dataStoreId, databaseSchema);
  }
}
