/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.google.common.annotations.VisibleForTesting;
import com.sonatype.insight.brain.tenancy.TenantReference;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;

/**
 * Caches integration version data from HDS to reduce load on HDS and improve performance.
 * Cache is tenant-aware and expires entries after 10 minutes.
 */
@Named
@Singleton
public class IntegrationVersionCache
{
  // 10-minute TTL to ensure new releases propagate quickly while maintaining good cache hit rate
  static final Duration MAX_AGE = Duration.ofMinutes(10);

  static final long MAXIMUM_SIZE = 1000L;

  private final IntegrationVersionCacheLoader integrationVersionCacheLoader;

  private final TenantReference<LoadingCache<IntegrationVersionCacheKey, List<IqIntegrationVersion>>> loadingCaches;

  @Inject
  public IntegrationVersionCache(final IntegrationVersionCacheLoader integrationVersionCacheLoader) {
    this.integrationVersionCacheLoader = integrationVersionCacheLoader;
    this.loadingCaches = new TenantReference<>(this::createLoadingCache);
  }

  @VisibleForTesting
  LoadingCache<IntegrationVersionCacheKey, List<IqIntegrationVersion>> createLoadingCache() {
    return newCacheBuilder()
        .expireAfterWrite(MAX_AGE.toMillis(), TimeUnit.MILLISECONDS)
        .maximumSize(MAXIMUM_SIZE)
        .build(integrationVersionCacheLoader);
  }

  @VisibleForTesting
  CacheBuilder<Object, Object> newCacheBuilder() {
    return CacheBuilder.newBuilder();
  }

  /**
   * Get integration versions from cache, loading from HDS if not present.
   *
   * @param name the integration name
   * @param supportedVersionCount the number of versions to retrieve
   * @return list of integration versions, may be empty if none found
   */
  public List<IqIntegrationVersion> get(final String name, final int supportedVersionCount) {
    IntegrationVersionCacheKey key = new IntegrationVersionCacheKey(name, supportedVersionCount);
    return getLoadingCache().getUnchecked(key);
  }

  /**
   * Invalidate all cached entries.
   *
   * @return the number of entries invalidated
   */
  public long invalidateAll() {
    LoadingCache<IntegrationVersionCacheKey, List<IqIntegrationVersion>> loadingCache = getLoadingCache();
    long size = loadingCache.size();
    loadingCache.invalidateAll();
    return size;
  }

  @VisibleForTesting
  LoadingCache<IntegrationVersionCacheKey, List<IqIntegrationVersion>> getLoadingCache() {
    return loadingCaches.get();
  }
}
