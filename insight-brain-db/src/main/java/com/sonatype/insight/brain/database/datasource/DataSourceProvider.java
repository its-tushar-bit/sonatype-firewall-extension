/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.database.datasource;

import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseConfig;

/**
 * Provide {@link javax.sql.DataSource} objects.
 */
public interface DataSourceProvider
{
  DataSource getDataSource(final DatabaseConfig databaseConfig, final String dataStoreId);
}
