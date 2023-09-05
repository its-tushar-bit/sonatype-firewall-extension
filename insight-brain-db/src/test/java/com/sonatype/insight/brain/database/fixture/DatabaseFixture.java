/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.database.fixture;

import com.sonatype.insight.brain.database.datasource.DataSourceProvider;
import com.sonatype.insight.db.DatabaseConfig;

public interface DatabaseFixture
    extends AutoCloseable
{
  DatabaseConfig getDatabaseConfig();

  DataSourceProvider getDataSourceProvider();
}
