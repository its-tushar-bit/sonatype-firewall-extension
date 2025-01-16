/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.List;

/**
 * Wraps the dashboard results and related metadata
 *
 * @since 1.32.0
 */
public class DashboardResultsDTO<T>
{
  public List<T> dashboardResults;

  public boolean hasNextPage = false;
}
