/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.database.datasource;

import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertTrue;

public class DataSourceProviderFactoryTest
{
  @Test
  public void testCreateDataSourceProvider_H2() {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.h2.Driver");
    DataSourceProvider dataSourceProvider = DataSourceProviderFactory.createDataSourceProvider(databaseConfig);
    assertTrue(dataSourceProvider instanceof H2DiskDataSourceProvider);
  }

  @Test
  public void testCreateDataSourceProvider_Postgres() {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.postgresql.Driver");
    DataSourceProvider dataSourceProvider = DataSourceProviderFactory.createDataSourceProvider(databaseConfig);
    assertTrue(dataSourceProvider instanceof PostgresDataSourceProvider);
  }

  @Test
  public void testCreateDataSourceProvider_Unknown() {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.unknown.Driver");
    assertThatExceptionOfType(DatabaseException.class)
        .isThrownBy(() -> DataSourceProviderFactory.createDataSourceProvider(databaseConfig));
  }
}
