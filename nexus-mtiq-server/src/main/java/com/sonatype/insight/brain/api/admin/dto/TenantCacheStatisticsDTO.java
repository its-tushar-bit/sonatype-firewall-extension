/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.dto;

/**
 * DTO for tenant cache statistics.
 *
 * @since 1.185
 */
public class TenantCacheStatisticsDTO
{
  public long totalHitCount;

  public long totalMissCount;

  public long totalLoadCount;

  public long totalEvictionCount;

  public TenantCacheStatisticsDTO() {
    // no-op for Jackson
  }

  public TenantCacheStatisticsDTO(
      long totalHitCount,
      long totalMissCount,
      long totalLoadCount,
      long totalEvictionCount)
  {
    this.totalHitCount = totalHitCount;
    this.totalMissCount = totalMissCount;
    this.totalLoadCount = totalLoadCount;
    this.totalEvictionCount = totalEvictionCount;
  }
}
