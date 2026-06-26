/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import java.util.Set;

/**
 * Request body for dashboard metrics aggregation (CLM-40927).
 * <p>
 * Null or empty id sets mean no additional filter beyond RBAC.
 */
public class DashboardMetricsRequestDTO
{
  public Set<String> organizationIds;

  public Set<String> applicationIds;

  public Set<String> stageIds;

  public Set<String> tagIds;
}
