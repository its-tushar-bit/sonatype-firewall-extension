/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.db.DatabaseConfig;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.postgresql.Driver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;

public class DatabaseConfigProviderTest
{
  private static final long DEFAULT_CACHE_SIZE_KILOBYTES = 16L * 1024;

  private static final long MAX_CACHE_SIZE_KILOBYTES = 7L * 1024 * 1024;

  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock
  private Runtime runtime;

  private InsightConfig config;

  private DatabaseConfigProvider databaseConfigProvider;

  @Before
  public void init() {
    config = new InsightConfig();
    databaseConfigProvider = new DatabaseConfigProvider(config, runtime);
  }

  @Test
  public void testGetDatabaseConfig_UsesDefaultCacheSize_GivenNullDbCacheSizePercent() {
    assertCacheSize(null, MAX_CACHE_SIZE_KILOBYTES * 1024 - 1, DEFAULT_CACHE_SIZE_KILOBYTES);
    assertCacheSize(null, MAX_CACHE_SIZE_KILOBYTES * 1024, DEFAULT_CACHE_SIZE_KILOBYTES);
    assertCacheSize(null, MAX_CACHE_SIZE_KILOBYTES * 1024 + 1, DEFAULT_CACHE_SIZE_KILOBYTES);
  }

  @Test
  public void testGetDatabaseConfig_UsesCacheSize_IfLessThanMax() {
    assertCacheSize(100, MAX_CACHE_SIZE_KILOBYTES * 1024 - 1, MAX_CACHE_SIZE_KILOBYTES - 1);
  }

  @Test
  public void testGetDatabaseConfig_UsesCacheSize_IfEqualToMax() {
    assertCacheSize(100, MAX_CACHE_SIZE_KILOBYTES * 1024, MAX_CACHE_SIZE_KILOBYTES);
  }

  @Test
  public void testGetDatabaseConfig_UsesMaxCacheSize_IfCacheSizeGreaterThanMax() {
    assertCacheSize(100, MAX_CACHE_SIZE_KILOBYTES * 1024 + 1, MAX_CACHE_SIZE_KILOBYTES);
  }

  private void assertCacheSize(Integer dbCacheSizePercent, long maxMemoryInBytes, long expectedCacheSizeInKilobytes) {
    config.setDbCacheSizePercent(dbCacheSizePercent);
    DatabaseConfigProvider databaseConfigProviderSpy = spy(databaseConfigProvider);
    lenient().when(databaseConfigProviderSpy.getMaxMemory()).thenReturn(maxMemoryInBytes);
    DatabaseConfig databaseConfig = databaseConfigProviderSpy.getDatabaseConfig(DatabaseName.ods);
    Matcher matcher = Pattern.compile("CACHE_SIZE=(\\d*)").matcher(databaseConfig.getUrl());
    matcher.find();
    assertThat(Long.valueOf(matcher.group(1))).isEqualTo(expectedCacheSizeInKilobytes);
  }

  @Test
  public void testGetDatabaseConfig_ExternalDatabase_NoPortOrParameters() {
    com.sonatype.insight.brain.service.DatabaseConfig dbConfig =
        new com.sonatype.insight.brain.service.DatabaseConfig();
    dbConfig.setType("postgresql");
    dbConfig.setHostname("localhost");
    dbConfig.setName("test-db");
    dbConfig.setUsername("testuser");
    dbConfig.setPassword("testpass");
    config.setDatabase(dbConfig);

    DatabaseConfig databaseConfig = databaseConfigProvider.getDatabaseConfig(DatabaseName.ods);

    assertThat(databaseConfig).isNotNull();
    assertThat(databaseConfig.getDriverClassName()).isEqualTo(Driver.class.getName());
    assertThat(databaseConfig.getUsername()).isEqualTo("testuser");
    assertThat(databaseConfig.getPassword()).isEqualTo("testpass");
    assertThat(databaseConfig.getUrl()).isEqualTo("jdbc:postgresql://localhost/test-db");
  }

  @Test
  public void testGetDatabaseConfig_ExternalDatabase_CustomPort() {
    com.sonatype.insight.brain.service.DatabaseConfig dbConfig =
        new com.sonatype.insight.brain.service.DatabaseConfig();
    dbConfig.setType("postgresql");
    dbConfig.setHostname("localhost");
    dbConfig.setPort(6543);
    dbConfig.setName("test-db");
    dbConfig.setUsername("testuser");
    dbConfig.setPassword("");
    config.setDatabase(dbConfig);

    DatabaseConfig databaseConfig = databaseConfigProvider.getDatabaseConfig(DatabaseName.ods);

    assertThat(databaseConfig).isNotNull();
    assertThat(databaseConfig.getDriverClassName()).isEqualTo(Driver.class.getName());
    assertThat(databaseConfig.getUsername()).isEqualTo("testuser");
    assertThat(databaseConfig.getPassword()).isEqualTo("");
    assertThat(databaseConfig.getUrl()).isEqualTo("jdbc:postgresql://localhost:6543/test-db");
  }

  @Test
  public void testGetDatabaseConfig_ExternalDatabase_CustomParameters() {
    Map<String, String> dbParams = new LinkedHashMap<>();
    dbParams.put("user", "paramuser");
    dbParams.put("password", "parampass");
    dbParams.put("key1", "value1");
    dbParams.put("key2", "value2");
    com.sonatype.insight.brain.service.DatabaseConfig dbConfig =
        new com.sonatype.insight.brain.service.DatabaseConfig();
    dbConfig.setType("postgresql");
    dbConfig.setHostname("localhost");
    dbConfig.setName("test-db");
    dbConfig.setUsername("testuser");
    dbConfig.setPassword("testpass");
    dbConfig.setParameters(dbParams);
    config.setDatabase(dbConfig);

    DatabaseConfig databaseConfig = databaseConfigProvider.getDatabaseConfig(DatabaseName.ods);

    assertThat(databaseConfig).isNotNull();
    assertThat(databaseConfig.getDriverClassName()).isEqualTo(Driver.class.getName());
    assertThat(databaseConfig.getUsername()).isEqualTo("testuser");
    assertThat(databaseConfig.getPassword()).isEqualTo("testpass");
    assertThat(databaseConfig.getUrl()).isEqualTo("jdbc:postgresql://localhost/test-db?key1=value1&key2=value2");
  }
}
