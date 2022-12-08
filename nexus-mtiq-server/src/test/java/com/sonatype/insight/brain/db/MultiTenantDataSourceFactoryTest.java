/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.PostgresDatabaseEngine;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantDataSourceFactoryTest
{
  @Mock
  DataSource testDataSource;

  private final String dataStoreId = "test";

  private final String databaseSchema = "test";

  @Test
  public void testCreateNewDataSource() {
    MultiTenantDataSourceFactory factory = Mockito.spy(new TestMultiTenantDataSourceFactory());
    DatabaseConfig databaseConfig = Mockito.mock(DatabaseConfig.class);

    factory.createNewDataSource(databaseConfig, dataStoreId, databaseSchema);
    Mockito.verify(factory, Mockito.times(1)).loadDataSource(databaseConfig, databaseSchema);

    // Make sure a second invocation does not call loadDataSource a second time
    factory.createNewDataSource(databaseConfig, dataStoreId, databaseSchema);
    Mockito.verify(factory, Mockito.times(1)).loadDataSource(databaseConfig, databaseSchema);
  }

  @Test
  public void testUseCustomPopulator() {
    MultiTenantDataSourceFactory factory = new TestMultiTenantDataSourceFactory();
    assertThat(factory.createDatabaseSchemaPopulator(testDataSource, PostgresDatabaseEngine.INSTANCE, dataStoreId,
        databaseSchema)).isInstanceOf(MultiTenantDatabaseSchemaPopulator.class);
  }

  /**
   * A test version for the factory so we don't actually go creating datasource objects and can use our test mock here
   */
  private class TestMultiTenantDataSourceFactory
      extends MultiTenantDataSourceFactory
  {
    @Override
    protected DataSource loadDataSource(DatabaseConfig databaseConfig, String databaseSchema) {
      return testDataSource;
    }
  }
}
