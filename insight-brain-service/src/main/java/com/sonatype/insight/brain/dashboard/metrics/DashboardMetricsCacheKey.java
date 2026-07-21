/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import java.util.Set;

import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.CurrentUser;

/**
 * Coalescing-cache key for {@link DashboardMetricsService}. {@link Record#equals} and
 * {@link Record#hashCode} cover principal identity and applied filter dimensions; {@link Set}
 * equality is order-independent.
 */
record DashboardMetricsCacheKey(
    String principalUsername,
    String principalRealmId,
    Set<String> organizationIds,
    Set<String> applicationIds,
    Set<String> stageIds,
    Set<String> tagIds,
    Boolean includeHeavyMetrics)
{
  static DashboardMetricsCacheKey forRequest(UserPrincipal principal, DashboardMetricsRequestDTO request) {
    Boolean includeHeavyMetrics = request == null ? null : request.includeHeavyMetrics;
    if (principal == null) {
      return new DashboardMetricsCacheKey(
          CurrentUser.ANONYMOUS,
          null,
          normalizedIds(request == null ? null : request.organizationIds),
          normalizedIds(request == null ? null : request.applicationIds),
          normalizedIds(request == null ? null : request.stageIds),
          normalizedIds(request == null ? null : request.tagIds),
          includeHeavyMetrics);
    }
    return new DashboardMetricsCacheKey(
        principal.getUsername(),
        principal.getRealmId(),
        normalizedIds(request == null ? null : request.organizationIds),
        normalizedIds(request == null ? null : request.applicationIds),
        normalizedIds(request == null ? null : request.stageIds),
        normalizedIds(request == null ? null : request.tagIds),
        includeHeavyMetrics);
  }

  private static Set<String> normalizedIds(Set<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return Set.of();
    }
    return Set.copyOf(ids);
  }
}
