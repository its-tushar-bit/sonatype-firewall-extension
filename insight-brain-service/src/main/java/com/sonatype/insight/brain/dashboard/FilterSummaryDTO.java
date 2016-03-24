/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

/**
 * Carries counts summarizing how many of the entities accessible/readable to the current user are matched by his
 * dashboard filter.
 */
public class FilterSummaryDTO
{
  public int matchedApplications;

  public int matchedPolicies;

  public int matchedComponents;
}
