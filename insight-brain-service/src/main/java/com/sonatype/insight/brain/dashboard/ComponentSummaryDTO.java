/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

/**
 * Carries counts describing how the non-proprietary components in scope of the dashboard filter are distributed across
 * the various match states.
 */
public class ComponentSummaryDTO
{
  public int total;

  public int exact;

  public int similar;

  public int unknown;
}
