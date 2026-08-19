/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.H2DatabaseEngine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
public class H2DiskDatabaseConfigProviderTest
{
  private static final long DEFAULT_CACHE_SIZE_KILOBYTES = 16L * 1024;

  private static final long MAX_CACHE_SIZE_KILOBYTES = 7L * 1024 * 1024;

  @Mock
  private Runtime runtime;

  private InsightConfig insightConfig;

  private H2DiskDatabaseConfigProvider h2DiskDatabaseConfigProvider;

  @BeforeEach
  public void init() {
    insightConfig = new InsightConfig();
    h2DiskDatabaseConfigProvider = new H2DiskDatabaseConfigProvider(insightConfig, runtime);
  }

  @Test
  public void testGetDatabaseEngine() {
    assertThat(h2DiskDatabaseConfigProvider.getDatabaseEngine().equals(H2DatabaseEngine.INSTANCE));
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
    insightConfig.setDbCacheSizePercent(dbCacheSizePercent);
    H2DiskDatabaseConfigProvider databaseConfigProviderSpy = spy(h2DiskDatabaseConfigProvider);
    lenient().when(databaseConfigProviderSpy.getMaxMemory()).thenReturn(maxMemoryInBytes);
    DatabaseConfig databaseConfig = databaseConfigProviderSpy.getDatabaseConfig(DatabaseName.ods);
    Matcher matcher = Pattern.compile("CACHE_SIZE=(\\d*)").matcher(databaseConfig.getUrl());
    matcher.find();
    assertThat(Long.valueOf(matcher.group(1))).isEqualTo(expectedCacheSizeInKilobytes);
  }
}
