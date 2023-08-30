/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.database.datasource;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseException;
import com.sonatype.insight.db.H2DatabaseEngine;
import com.sonatype.insight.db.PostgresDatabaseEngine;

/**
 * Simple factory to create the needed {@link DataSourceProvider}
 */
public class DataSourceProviderFactory
{
  /**
   * Create the appropriate {@link DataSourceProvider} based on a given {@link DatabaseConfig} object.
   */
  public static DataSourceProvider createDataSourceProvider(final DatabaseConfig databaseConfig) {
    if (DatabaseUtil.getDatabaseEngine(databaseConfig).equals(PostgresDatabaseEngine.INSTANCE)) {
      return new PostgresDataSourceProvider();
    }
    if (DatabaseUtil.getDatabaseEngine(databaseConfig).equals(H2DatabaseEngine.INSTANCE)) {
      return new H2DiskDataSourceProvider();
    }
    throw new DatabaseException("Unable to create DataSourceProvider. Unknown DatabaseConfig.");
  }
}
