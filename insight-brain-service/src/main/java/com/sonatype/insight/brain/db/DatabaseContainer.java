/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.brain.database.MtiqTempUtils;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;

/**
 * The application performs all database connection and initialization before the Guice injection occurs. We need some
 * similar control over the instances created of database classes so this class can be considered to be a simple way to
 * track and manage those classes.
 */
public class DatabaseContainer
{
  private final DataSourceFactory dataSourceFactory;

  private final DatabaseProvisionUtils databaseProvisionUtils;

  public DatabaseContainer(
      final DataSourceFactory dataSourceFactory,
      final DatabaseProvisionUtils databaseProvisionUtils)
  {
    MtiqTempUtils.logTodo("Constructor to be replaced with a single parameter InsightConfig");
    this.dataSourceFactory = dataSourceFactory;
    this.databaseProvisionUtils = databaseProvisionUtils;
  }

  public DataSourceFactory getDataSourceFactory() {
    return dataSourceFactory;
  }

  public DatabaseProvisionUtils getDatabaseProvisionUtils() {
    return databaseProvisionUtils;
  }
}
