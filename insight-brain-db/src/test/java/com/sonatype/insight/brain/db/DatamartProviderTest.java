/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseConfig;

public class DatamartProviderTest
    extends AbstractDatabaseProviderTest
{
  @Override
  protected DatabaseConfig getDatabaseConfig() {
    return DatamartProvider.getDatabaseConfig();
  }

  @Override
  protected void initDatabase(DatabaseConfig databaseConfig) {
    DatamartProvider.init(databaseConfig);
  }

  @Override
  protected DataSource getDataSource() {
    return DatamartProvider.getDataSource();
  }
}
