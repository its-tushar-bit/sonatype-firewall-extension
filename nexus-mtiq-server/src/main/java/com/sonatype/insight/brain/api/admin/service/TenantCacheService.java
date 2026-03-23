/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.admin.dto.TenantCacheStatisticsDTO;
import com.sonatype.insight.brain.component.RepositoryIdentifiedComponentCache;

import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;

/**
 * Service for retrieving per-tenant cache statistics.
 *
 * @since 1.185
 */
@Named
@Singleton
public class TenantCacheService
{
  private final RepositoryIdentifiedComponentCache repositoryIdentifiedComponentCache;

  @Inject
  public TenantCacheService(RepositoryIdentifiedComponentCache repositoryIdentifiedComponentCache) {
    this.repositoryIdentifiedComponentCache = repositoryIdentifiedComponentCache;
  }

  /**
   * Gets cache statistics for the current tenant context.
   * The tenant context is set by the AdminTenantFilter based on the URL path parameter.
   *
   * @param tenantSlug the tenant slug (used for logging/validation, actual tenant context is from ThreadLocal)
   * @return cache statistics DTO
   */
  public TenantCacheStatisticsDTO getCacheStatistics(String tenantSlug) {
    // The tenant context should already be set by AdminTenantFilter based on the URL path.
    // We can directly access the tenant-specific cache from the current thread's context.
    LoadingCache<?, ?> cache = repositoryIdentifiedComponentCache.getLoadingCache();
    CacheStats stats = cache.stats();
    return new TenantCacheStatisticsDTO(
        stats.hitCount(),
        stats.missCount(),
        stats.loadCount(),
        stats.evictionCount());
  }
}
