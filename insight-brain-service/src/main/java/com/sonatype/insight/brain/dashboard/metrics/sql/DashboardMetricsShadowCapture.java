/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.time.Instant;
import java.util.Set;

import jakarta.annotation.Nullable;

import com.sonatype.insight.brain.dashboard.metrics.DashboardMetricsDTO;
import com.sonatype.insight.brain.dashboard.metrics.DashboardMetricsRequestDTO;

/**
 * Detached request data used by a dashboard SQL shadow worker.
 */
public record DashboardMetricsShadowCapture(
    @Nullable Set<String> organizationIds,
    @Nullable Set<String> applicationIds,
    @Nullable Set<String> stageIds,
    @Nullable Set<String> tagIds,
    @Nullable Boolean includeHeavyMetrics,
    Instant requestInstant,
    String filterHash,
    @Nullable Long indexSnapshotTime,
    DashboardMetricsDTO servedIndexResponse)
{
  public DashboardMetricsShadowCapture {
    organizationIds = immutableCopy(organizationIds);
    applicationIds = immutableCopy(applicationIds);
    stageIds = immutableCopy(stageIds);
    tagIds = immutableCopy(tagIds);
  }

  public static DashboardMetricsShadowCapture create(
      final DashboardMetricsRequestDTO request,
      final Instant requestInstant,
      @Nullable final Long indexSnapshotTime,
      final DashboardMetricsDTO servedIndexResponse)
  {
    return new DashboardMetricsShadowCapture(
        request == null ? null : request.organizationIds,
        request == null ? null : request.applicationIds,
        request == null ? null : request.stageIds,
        request == null ? null : request.tagIds,
        request == null ? null : request.includeHeavyMetrics,
        requestInstant,
        filterHash(request),
        indexSnapshotTime,
        servedIndexResponse);
  }

  public DashboardMetricsRequestDTO toRequest() {
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = organizationIds;
    request.applicationIds = applicationIds;
    request.stageIds = stageIds;
    request.tagIds = tagIds;
    request.includeHeavyMetrics = includeHeavyMetrics;
    return request;
  }

  @Nullable
  private static Set<String> immutableCopy(@Nullable final Set<String> values) {
    return values == null ? null : Set.copyOf(values);
  }

  private static String filterHash(final DashboardMetricsRequestDTO request) {
    String normalized = DashboardMetricsSqlHashes.normalizeIds(request == null ? null : request.organizationIds)
        + '|' + DashboardMetricsSqlHashes.normalizeIds(request == null ? null : request.applicationIds)
        + '|' + DashboardMetricsSqlHashes.normalizeIds(request == null ? null : request.stageIds)
        + '|' + DashboardMetricsSqlHashes.normalizeIds(request == null ? null : request.tagIds)
        + '|' + (request == null ? null : request.includeHeavyMetrics);
    return DashboardMetricsSqlHashes.sha256Hex(normalized);
  }
}
