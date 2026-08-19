/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datasource.MultiTenantPostgresDataSourceProvider;
import com.sonatype.insight.db.DatabaseConfig;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
public class MultiTenantPostgresDataSourceProviderTest
{
  private DatabaseConfig mainConfig;

  private DatabaseConfig locksConfig;

  @BeforeEach
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
  }

  @Test
  public void testCreateDataSource() throws Exception {
    MultiTenantPostgresDataSourceProvider dataSourceProvider =
        new MultiTenantPostgresDataSourceProvider(mainConfig, locksConfig);
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
      MultiTenantPostgresDataSourceProvider multiTenantPostgresDataSourceProvider =
          new MultiTenantPostgresDataSourceProvider(null, locksConfig);
      multiTenantPostgresDataSourceProvider.getDataSource(mainConfig, null);
    }).withStackTraceContaining("MTIQ-specific database config entry 'mainDatabase' missing from config.yml");
  }

  @Test
  public void testMissingLocksConfig() {
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
      MultiTenantPostgresDataSourceProvider multiTenantPostgresDataSourceProvider =
          new MultiTenantPostgresDataSourceProvider(mainConfig, null);
      multiTenantPostgresDataSourceProvider.getLocksDataSource();
    }).withStackTraceContaining("MTIQ-specific database config entry 'locksDatabase' missing from config.yml");
  }
}
