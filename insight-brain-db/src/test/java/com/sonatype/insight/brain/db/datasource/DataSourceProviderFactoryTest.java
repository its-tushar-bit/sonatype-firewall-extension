/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datasource;

import com.sonatype.insight.db.DatabaseException;
import com.sonatype.insight.db.H2DatabaseEngine;
import com.sonatype.insight.db.MysqlDatabaseEngine;
import com.sonatype.insight.db.PostgresDatabaseEngine;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertTrue;

public class DataSourceProviderFactoryTest
{
  @Test
  public void testCreateDataSourceProvider_H2() {
    DataSourceProvider dataSourceProvider =
        DataSourceProviderFactory.createDataSourceProvider(H2DatabaseEngine.INSTANCE);
    assertTrue(dataSourceProvider instanceof H2DiskDataSourceProvider);
  }

  @Test
  public void testCreateDataSourceProvider_Postgres() {
    DataSourceProvider dataSourceProvider =
        DataSourceProviderFactory.createDataSourceProvider(PostgresDatabaseEngine.INSTANCE);
    assertTrue(dataSourceProvider instanceof PostgresDataSourceProvider);
  }

  @Test
  public void testCreateDataSourceProvider_Unknown() {
    // MySQL is a DatabaseEngine that IQ does not support
    assertThatExceptionOfType(DatabaseException.class)
        .isThrownBy(() -> DataSourceProviderFactory.createDataSourceProvider(MysqlDatabaseEngine.INSTANCE));
  }
}
