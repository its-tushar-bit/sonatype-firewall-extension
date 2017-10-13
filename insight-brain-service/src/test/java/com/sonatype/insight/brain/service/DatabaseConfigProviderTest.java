/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.db.DatabaseConfig;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseConfigProviderTest
{
  private static final long DEFAULT_CACHE_SIZE_KILOBYTES = 16L * 1024;

  private static final long MAX_CACHE_SIZE_KILOBYTES = 7L * 1024 * 1024;

  @Test
  public void testGetDatabaseConfig_UsesDefaultCacheSize_GivenNullDbCacheSizePercent() throws Exception {
    assertCacheSize(null, MAX_CACHE_SIZE_KILOBYTES * 1024 - 1, DEFAULT_CACHE_SIZE_KILOBYTES);
    assertCacheSize(null, MAX_CACHE_SIZE_KILOBYTES * 1024, DEFAULT_CACHE_SIZE_KILOBYTES);
    assertCacheSize(null, MAX_CACHE_SIZE_KILOBYTES * 1024 + 1, DEFAULT_CACHE_SIZE_KILOBYTES);
  }

  @Test
  public void testGetDatabaseConfig_UsesCacheSize_IfLessThanMax() throws Exception {
    assertCacheSize(100, MAX_CACHE_SIZE_KILOBYTES * 1024 - 1, MAX_CACHE_SIZE_KILOBYTES - 1);
  }

  @Test
  public void testGetDatabaseConfig_UsesCacheSize_IfEqualToMax() throws Exception {
    assertCacheSize(100, MAX_CACHE_SIZE_KILOBYTES * 1024, MAX_CACHE_SIZE_KILOBYTES);
  }

  @Test
  public void testGetDatabaseConfig_UsesMaxCacheSize_IfCacheSizeGreaterThanMax() throws Exception {
    assertCacheSize(100, MAX_CACHE_SIZE_KILOBYTES * 1024 + 1, MAX_CACHE_SIZE_KILOBYTES);
  }

  private void assertCacheSize(Integer dbCacheSizePercent, long maxMemoryInBytes, long expectedCacheSizeInKilobytes) {
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setDbCacheSizePercent(dbCacheSizePercent);
    Runtime mockRuntime = mock(Runtime.class);
    when(mockRuntime.maxMemory()).thenReturn(maxMemoryInBytes);
    DatabaseConfig databaseConfig = new DatabaseConfigProvider(insightConfig, mockRuntime)
        .getDatabaseConfig(DatabaseName.ods);
    Matcher matcher = Pattern.compile("CACHE_SIZE=(\\d*)").matcher(databaseConfig.getUrl());
    matcher.find();
    assertThat(Long.valueOf(matcher.group(1)), is(expectedCacheSizeInKilobytes));
  }
}
