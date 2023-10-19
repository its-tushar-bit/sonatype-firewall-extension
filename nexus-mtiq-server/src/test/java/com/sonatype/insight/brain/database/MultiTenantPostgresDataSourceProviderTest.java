/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.database;

import javax.sql.DataSource;

import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.db.DatabaseConfig;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantPostgresDataSourceProviderTest
{
  @Spy
  private DatabaseConfig mainConfig;

  @Spy
  private DatabaseConfig locksConfig;

  @Spy
  private MultiTenantInsightConfig multiTenantInsightConfig;

  @InjectMocks
  private TestMultiTenantPostgresDataSourceProvider dataSourceProvider;

  @Before
  public void before() {
    mainConfig.setUrl("jdbc:postgresql://hostname:5432/dbname");
    mainConfig.setUsername("foo");
    mainConfig.setPassword("bar");
    mainConfig.setMaxConnections(4);
    mainConfig.setMaxIdleConnections(3);

    locksConfig.setUrl("jdbc:postgresql://hostname:5432/locksdbname");
    locksConfig.setUsername("biz");
    locksConfig.setPassword("baz");
    locksConfig.setMaxConnections(2);
    locksConfig.setMaxIdleConnections(1);

    multiTenantInsightConfig.setMainDatabase(mainConfig);
    multiTenantInsightConfig.setLocksDatabase(locksConfig);
  }

  @Test
  public void testCreateDataSource() throws Exception {
    DataSource mainDataSource = dataSourceProvider.getDataSource(mainConfig, null);
    DataSource locksDataSource = dataSourceProvider.getLocksDataSource();

    assertThat(mainDataSource).isNotEqualTo(locksDataSource);
    assertThat(mainDataSource).isNotEqualTo(locksDataSource);

    assertThat(mainDataSource).isExactlyInstanceOf(BasicDataSource.class);

    try (BasicDataSource mainBasicDataSource = (BasicDataSource) mainDataSource) {
      assertThat(mainBasicDataSource.getUrl()).isEqualTo("jdbc:postgresql://hostname:5432/dbname");
      assertThat(mainBasicDataSource.getUsername()).isEqualTo("foo");
      assertThat(mainBasicDataSource.getPassword()).isEqualTo("bar");
      assertThat(mainBasicDataSource.getMaxTotal()).isEqualTo(4);
      assertThat(mainBasicDataSource.getMaxIdle()).isEqualTo(3);
    }

    try (BasicDataSource locksBasicDataSource = (BasicDataSource) locksDataSource) {
      assertThat(locksBasicDataSource.getUrl()).isEqualTo("jdbc:postgresql://hostname:5432/locksdbname");
      assertThat(locksBasicDataSource.getUsername()).isEqualTo("biz");
      assertThat(locksBasicDataSource.getPassword()).isEqualTo("baz");
      assertThat(locksBasicDataSource.getMaxTotal()).isEqualTo(2);
      assertThat(locksBasicDataSource.getMaxIdle()).isEqualTo(1);
    }

    // Make sure a second invocation does not produce different DataSource objects
    DataSource mainDataSource2 = dataSourceProvider.getDataSource(mainConfig, null);
    DataSource locksDataSource2 = dataSourceProvider.getLocksDataSource();
    assertThat(mainDataSource).isEqualTo(mainDataSource2);
    assertThat(locksDataSource).isEqualTo(locksDataSource2);
  }

  @Test
  public void testMissingMainConfig() {
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
      multiTenantInsightConfig.setMainDatabase(null);
      MultiTenantPostgresDataSourceProvider multiTenantPostgresDataSourceProvider =
          new TestMultiTenantPostgresDataSourceProvider(multiTenantInsightConfig);
      multiTenantPostgresDataSourceProvider.getDataSource(mainConfig, null);
    }).withStackTraceContaining("MTIQ-specific database config entry 'mainDatabase' missing from config.yml");
  }

  @Test
  public void testMissingLocksConfig() {
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
      multiTenantInsightConfig.setLocksDatabase(null);
      MultiTenantPostgresDataSourceProvider multiTenantPostgresDataSourceProvider =
          new TestMultiTenantPostgresDataSourceProvider(multiTenantInsightConfig);
      multiTenantPostgresDataSourceProvider.getLocksDataSource();
    }).withStackTraceContaining("MTIQ-specific database config entry 'locksDatabase' missing from config.yml");
  }

  private static class TestMultiTenantPostgresDataSourceProvider
      extends MultiTenantPostgresDataSourceProvider
  {
    public TestMultiTenantPostgresDataSourceProvider(final MultiTenantInsightConfig multiTenantInsightConfig) {
      super(multiTenantInsightConfig);
    }

    // override just to make public for test
    @Override
    public DataSource createNewDataSource(final DatabaseConfig databaseConfig) {
      return super.createNewDataSource(databaseConfig);
    }
  }
}
