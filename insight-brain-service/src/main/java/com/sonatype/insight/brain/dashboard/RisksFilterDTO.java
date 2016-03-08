/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Set;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;

/**
 * @since 1.20.0
 */
public class RisksFilterDTO
{

  public Set<String> applicationIds;

  public Set<String> stageIds;

  public Set<String> tagIds;

  public PolicyThreatCategoryFilter policyThreatCategories;

  public PolicyThreatLevelFilter policyThreatLevelRange;

  public int maxResults = 1000;

}
