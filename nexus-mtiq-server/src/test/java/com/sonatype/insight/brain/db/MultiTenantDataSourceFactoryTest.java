/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import javax.sql.DataSource;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.PostgresDatabaseEngine;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantDataSourceFactoryTest
{
  @Mock
  private DataSource testMainDataSource;

  private MultiTenantInsightConfig multiTenantInsightConfig;

  private final String dataStoreId = "test";

  private final String databaseSchema = "test";

  private DatabaseConfig mainConfig;

  private DatabaseConfig locksConfig;

  @Spy
  private TestMultiTenantDataSourceFactory factory;

  @Before
  public void before() {
    mainConfig = new DatabaseConfig();
    mainConfig.setUrl("jdbc:postgresql://hostname:5432/dbname");
    mainConfig.setUsername("foo");
    mainConfig.setPassword("bar");
    mainConfig.setMaxConnections(4);
    mainConfig.setMaxIdleConnections(3);

    locksConfig = new DatabaseConfig();
    locksConfig.setUrl("jdbc:postgresql://hostname:5432/locksdbname");
    locksConfig.setUsername("biz");
    locksConfig.setPassword("baz");
    locksConfig.setMaxConnections(2);
    locksConfig.setMaxIdleConnections(1);

    multiTenantInsightConfig = new MultiTenantInsightConfig();
    multiTenantInsightConfig.setMainDatabase(mainConfig);
    multiTenantInsightConfig.setLocksDatabase(locksConfig);

    factory.setInsightConfig(multiTenantInsightConfig);
  }

  @Test
  public void testCreateDataSource() {
    factory.createNewDataSource(mainConfig, dataStoreId, databaseSchema);
    verify(factory, Mockito.times(1)).createNewDataSourceFromConfig(mainConfig);
    factory.createLocksDataSource();
    verify(factory, Mockito.times(1)).createNewDataSourceFromConfig(locksConfig);

    // Make sure a second invocation does not call methods a second time
    factory.createNewDataSource(mainConfig, dataStoreId, databaseSchema);
    verify(factory, Mockito.times(1)).createNewDataSourceFromConfig(mainConfig);
    factory.createLocksDataSource();
    verify(factory, Mockito.times(1)).createNewDataSourceFromConfig(locksConfig);
  }

  @Test
  @Category(PostgresTestCategory.class)
  public void testUseCustomPopulator() {
    MultiTenantDataSourceFactory factory = new TestMultiTenantDataSourceFactory();
    assertThat(factory.createDatabaseSchemaPopulator(testMainDataSource, PostgresDatabaseEngine.INSTANCE, dataStoreId,
        databaseSchema)).isInstanceOf(MultiTenantDatabaseSchemaPopulator.class);
  }

  @Test
  public void testMissingMainConfig() {
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
      multiTenantInsightConfig.setMainDatabase(null);
      factory.createNewDataSource(null, dataStoreId, databaseSchema);
    }).withStackTraceContaining("MTIQ-specific database config entry 'mainDatabase' missing from config.yml");
  }

  @Test
  public void testMissingLocksConfig() {
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
      multiTenantInsightConfig.setLocksDatabase(null);
      factory.createLocksDataSource();
    }).withStackTraceContaining("MTIQ-specific database config entry 'locksDatabase' missing from config.yml");
  }

  private static class TestMultiTenantDataSourceFactory
      extends MultiTenantDataSourceFactory
  {
    // override just to make public for test
    @Override
    public DataSource createNewDataSourceFromConfig(final DatabaseConfig databaseConfig) {
      return super.createNewDataSourceFromConfig(databaseConfig);
    }
  }
}
