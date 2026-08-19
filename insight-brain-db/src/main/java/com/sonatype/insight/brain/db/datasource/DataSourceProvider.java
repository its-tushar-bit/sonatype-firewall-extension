/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datasource;

import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseConfig;

/**
 * Provide {@link javax.sql.DataSource} objects.
 */
public interface DataSourceProvider
{
  DataSource getDataSource(final DatabaseConfig databaseConfig, final String dataStoreId);

  /**
   * This method should be used only to create the data source(s) at startup and in very limited case where a new
   * DataSource is needed. For all other purposes, use getDataSource.
   */
  DataSource createNewDataSource(DatabaseConfig databaseConfig);
}
