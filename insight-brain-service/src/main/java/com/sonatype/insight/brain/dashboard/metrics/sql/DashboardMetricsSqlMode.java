/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

/**
 * Operator-facing SQL metrics serving mode.
 * <p>
 * {@link #SHADOW} serves index-backed metrics like {@link #OFF} while scheduling sampled,
 * non-blocking SQL dual-run comparison. {@link #ON} serves migrated metrics from SQL when ready.
 */
public enum DashboardMetricsSqlMode
{
  OFF,
  SHADOW,
  ON
}
