/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

/**
 * Operator-facing SQL metrics serving mode.
 * <p>
 * {@link #SHADOW} is accepted for forward-compatible configuration and readiness passthrough.
 * Dual-run shadow comparison is implemented in CLM-42678; until then the service treats
 * {@code SHADOW} like {@link #OFF} for metric serving.
 */
public enum DashboardMetricsSqlMode
{
  OFF,
  SHADOW,
  ON
}
