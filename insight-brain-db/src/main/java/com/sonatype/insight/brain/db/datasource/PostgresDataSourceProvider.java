/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datasource;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.db.DatabaseConfig;

/**
 * {@link DataSourceProvider} for Postgres databases. Postgres can maintain a single physical {@link DataSource} for all
 * four {@link DataStore} classes.
 */
public class PostgresDataSourceProvider
    extends AbstractDataSourceProvider
    implements DataSourceProvider, LegacyDataSourceProvider
{
  protected DataSource dataSource;

  @Override
  public DataSource getDataSource(final DatabaseConfig databaseConfig, final String dataStoreId) {
    if (dataSource == null) {
      dataSource = createNewDataSource(databaseConfig);
    }
    return dataSource;
  }
}
