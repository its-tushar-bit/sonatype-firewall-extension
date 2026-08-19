/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datasource;

import com.sonatype.insight.db.DatabaseEngine;
import com.sonatype.insight.db.DatabaseException;
import com.sonatype.insight.db.H2DatabaseEngine;
import com.sonatype.insight.db.PostgresDatabaseEngine;

/**
 * Simple factory to create the needed {@link DataSourceProvider}
 */
public class DataSourceProviderFactory
{
  /**
   * Create the appropriate {@link DataSourceProvider} based on a given {@link DatabaseEngine} object.
   */
  public static DataSourceProvider createDataSourceProvider(final DatabaseEngine databaseEngine) {
    if (PostgresDatabaseEngine.INSTANCE.equals(databaseEngine)) {
      return new PostgresDataSourceProvider();
    }
    if (H2DatabaseEngine.INSTANCE.equals(databaseEngine)) {
      return new H2DiskDataSourceProvider();
    }
    throw new DatabaseException("Unable to create DataSourceProvider. Unknown DatabaseConfig.");
  }
}
