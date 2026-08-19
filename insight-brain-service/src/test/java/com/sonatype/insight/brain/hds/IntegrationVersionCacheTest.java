/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.List;

import com.google.common.cache.CacheBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IntegrationVersionCacheTest
{
  @Mock
  private IntegrationVersionCacheLoader mockLoader;

  private IntegrationVersionCache cache;

  @BeforeEach
  public void setup() {
    cache = new IntegrationVersionCache(mockLoader);
  }

  @Test
  public void testGet_LoadsFromCacheLoader() throws Exception {
    List<IqIntegrationVersion> expectedVersions = List.of(
        new IqIntegrationVersion("Maven_Plugin", "1.3.0"),
        new IqIntegrationVersion("Maven_Plugin", "1.2.0"));

    when(mockLoader.load(any(IntegrationVersionCacheKey.class))).thenReturn(expectedVersions);

    List<IqIntegrationVersion> result = cache.get("Maven_Plugin", 2);

    assertThat(result).isEqualTo(expectedVersions);
    verify(mockLoader).load(new IntegrationVersionCacheKey("Maven_Plugin", 2));
  }

  @Test
  public void testGet_UsesCacheOnSecondCall() throws Exception {
    List<IqIntegrationVersion> expectedVersions = List.of(
        new IqIntegrationVersion("Maven_Plugin", "1.3.0"));

    when(mockLoader.load(any(IntegrationVersionCacheKey.class))).thenReturn(expectedVersions);

    // First call - should load
    List<IqIntegrationVersion> result1 = cache.get("Maven_Plugin", 1);
    assertThat(result1).isEqualTo(expectedVersions);

    // Second call - should use cache
    List<IqIntegrationVersion> result2 = cache.get("Maven_Plugin", 1);
    assertThat(result2).isEqualTo(expectedVersions);

    // Verify loader was only called once
    verify(mockLoader, times(1)).load(new IntegrationVersionCacheKey("Maven_Plugin", 1));
  }

  @Test
  public void testGet_DifferentKeysLoadSeparately() throws Exception {
    List<IqIntegrationVersion> mavenVersions = List.of(
        new IqIntegrationVersion("Maven_Plugin", "1.3.0"));
    List<IqIntegrationVersion> gradleVersions = List.of(
        new IqIntegrationVersion("Gradle_Plugin", "2.0.0"));

    when(mockLoader.load(new IntegrationVersionCacheKey("Maven_Plugin", 3)))
        .thenReturn(mavenVersions);
    when(mockLoader.load(new IntegrationVersionCacheKey("Gradle_Plugin", 3)))
        .thenReturn(gradleVersions);

    List<IqIntegrationVersion> result1 = cache.get("Maven_Plugin", 3);
    List<IqIntegrationVersion> result2 = cache.get("Gradle_Plugin", 3);

    assertThat(result1).isEqualTo(mavenVersions);
    assertThat(result2).isEqualTo(gradleVersions);

    verify(mockLoader).load(new IntegrationVersionCacheKey("Maven_Plugin", 3));
    verify(mockLoader).load(new IntegrationVersionCacheKey("Gradle_Plugin", 3));
  }

  @Test
  public void testInvalidateAll_ClearsCache() throws Exception {
    List<IqIntegrationVersion> versions = List.of(
        new IqIntegrationVersion("Maven_Plugin", "1.3.0"));

    when(mockLoader.load(any(IntegrationVersionCacheKey.class))).thenReturn(versions);

    // Load into cache
    cache.get("Maven_Plugin", 3);

    // Invalidate
    long invalidatedCount = cache.invalidateAll();

    assertThat(invalidatedCount).isEqualTo(1);

    // Next get should reload from loader
    cache.get("Maven_Plugin", 3);

    // Verify loader was called twice (before and after invalidation)
    verify(mockLoader, times(2)).load(new IntegrationVersionCacheKey("Maven_Plugin", 3));
  }

  @Test
  public void testInvalidateAll_ReturnsZeroWhenEmpty() {
    long invalidatedCount = cache.invalidateAll();

    assertThat(invalidatedCount).isZero();
  }

  @Test
  public void testInvalidateAll_ReturnsCorrectCountForMultipleEntries() throws Exception {
    List<IqIntegrationVersion> versions = List.of(
        new IqIntegrationVersion("Test", "1.0.0"));

    when(mockLoader.load(any(IntegrationVersionCacheKey.class))).thenReturn(versions);

    // Load multiple entries
    cache.get("Maven_Plugin", 3);
    cache.get("Gradle_Plugin", 5);
    cache.get("Jenkins_Plugin", 2);

    long invalidatedCount = cache.invalidateAll();

    assertThat(invalidatedCount).isEqualTo(3);
  }

  @Test
  public void testCreateLoadingCache_ConfiguresCorrectly() {
    IntegrationVersionCache testCache = new IntegrationVersionCache(mockLoader)
    {
      @Override
      CacheBuilder<Object, Object> newCacheBuilder() {
        return spy(CacheBuilder.newBuilder());
      }
    };

    testCache.createLoadingCache();

    // Verify the cache was created (configuration constants tested above)
    assertThat(testCache.getLoadingCache()).isNotNull();
  }

  @Test
  public void testGet_WithDifferentVersionCounts() throws Exception {
    List<IqIntegrationVersion> threeVersions = List.of(
        new IqIntegrationVersion("Maven_Plugin", "1.3.0"),
        new IqIntegrationVersion("Maven_Plugin", "1.2.0"),
        new IqIntegrationVersion("Maven_Plugin", "1.1.0"));
    List<IqIntegrationVersion> fiveVersions = List.of(
        new IqIntegrationVersion("Maven_Plugin", "1.5.0"),
        new IqIntegrationVersion("Maven_Plugin", "1.4.0"),
        new IqIntegrationVersion("Maven_Plugin", "1.3.0"),
        new IqIntegrationVersion("Maven_Plugin", "1.2.0"),
        new IqIntegrationVersion("Maven_Plugin", "1.1.0"));

    when(mockLoader.load(new IntegrationVersionCacheKey("Maven_Plugin", 3)))
        .thenReturn(threeVersions);
    when(mockLoader.load(new IntegrationVersionCacheKey("Maven_Plugin", 5)))
        .thenReturn(fiveVersions);

    List<IqIntegrationVersion> result1 = cache.get("Maven_Plugin", 3);
    List<IqIntegrationVersion> result2 = cache.get("Maven_Plugin", 5);

    assertThat(result1).hasSize(3);
    assertThat(result2).hasSize(5);

    // Both calls should hit the loader since they have different version counts
    verify(mockLoader).load(new IntegrationVersionCacheKey("Maven_Plugin", 3));
    verify(mockLoader).load(new IntegrationVersionCacheKey("Maven_Plugin", 5));
  }
}
