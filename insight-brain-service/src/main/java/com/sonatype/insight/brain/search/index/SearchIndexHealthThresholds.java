/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import com.sonatype.insight.brain.model.searchindex.SearchIndexHealth;

/**
 * Single source of truth for Analyze green/yellow/red bands. FE must not invent divergent thresholds.
 */
public final class SearchIndexHealthThresholds
{
  public static final long LAG_WARNING_SECONDS = 300L;

  public static final long LAG_NOT_HEALTHY_SECONDS = 3600L;

  public static final long PENDING_WARNING = 10_000L;

  public static final long PENDING_NOT_HEALTHY = 100_000L;

  public static final long FAILED_WARNING = 1L;

  public static final long FAILED_POINT_REPAIR_MAX = 25L;

  public static final long FAILED_NOT_HEALTHY = 100L;

  private SearchIndexHealthThresholds() {
  }

  public record DerivedHealth(String healthStatus, String recommendedOp, long queueLagSeconds)
  {
  }

  public static DerivedHealth derive(
      final long queueLagSeconds,
      final long pendingChangeCount,
      final long failedChangeCount,
      final boolean rebuildInProgress)
  {
    if (rebuildInProgress) {
      return new DerivedHealth(SearchIndexHealth.STATUS_REBUILD_IN_PROGRESS, SearchIndexHealth.OP_NONE,
          queueLagSeconds);
    }

    boolean notHealthy = queueLagSeconds >= LAG_NOT_HEALTHY_SECONDS
        || failedChangeCount >= FAILED_NOT_HEALTHY
        || pendingChangeCount >= PENDING_NOT_HEALTHY;
    if (notHealthy) {
      String op = failedChangeCount >= FAILED_NOT_HEALTHY
          ? SearchIndexHealth.OP_FULL_REBUILD
          : SearchIndexHealth.OP_SCOPED_CLEANUP;
      return new DerivedHealth(SearchIndexHealth.STATUS_NOT_HEALTHY, op, queueLagSeconds);
    }

    boolean warning = queueLagSeconds >= LAG_WARNING_SECONDS
        || failedChangeCount >= FAILED_WARNING
        || pendingChangeCount >= PENDING_WARNING;
    if (warning) {
      String op =
          failedChangeCount > 0 && failedChangeCount <= FAILED_POINT_REPAIR_MAX && pendingChangeCount < PENDING_WARNING
              ? SearchIndexHealth.OP_POINT_REPAIR
              : SearchIndexHealth.OP_SCOPED_CLEANUP;
      return new DerivedHealth(SearchIndexHealth.STATUS_WARNING, op, queueLagSeconds);
    }

    return new DerivedHealth(SearchIndexHealth.STATUS_HEALTHY, SearchIndexHealth.OP_NONE, queueLagSeconds);
  }
}
