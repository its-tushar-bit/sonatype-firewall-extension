/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

/**
 * @since 1.24.0
 */
public class NamedDashboardFilterDTO
{
  public String name;

  public String basedOnFilterName;

  /**
   * If true, the user needs to acknowledge the filter before being able to see any data in the dashboard.
   *
   * @since 1.29
   */
  public boolean needsAcknowledgement;

  public DashboardFilterDTO filter;
}
