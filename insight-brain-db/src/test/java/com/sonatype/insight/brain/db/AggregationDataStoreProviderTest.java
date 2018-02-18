/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseConfig;

public class AggregationDataStoreProviderTest
    extends AbstractDatabaseProviderTest
{
  @Override
  protected DatabaseConfig getDatabaseConfig() {
    return AggregationDataStoreProvider.getDatabaseConfig();
  }

  @Override
  protected void initDatabase(DatabaseConfig databaseConfig) {
    AggregationDataStoreProvider.init(databaseConfig);
  }

  @Override
  protected DataSource getDataSource() {
    return AggregationDataStoreProvider.getDataSource();
  }
}
