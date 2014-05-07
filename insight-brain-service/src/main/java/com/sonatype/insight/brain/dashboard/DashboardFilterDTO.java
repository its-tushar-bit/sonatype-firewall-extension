/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.List;

import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;

/**
 * @since 1.11.0
 */
public class DashboardFilterDTO
{
  public int minPolicyThreatLevel;

  public int maxPolicyThreatLevel;

  public List<String> applicationFilters;

  public List<String> tagFilters;

  public List<PolicyThreatCategory> policyThreatCategoryFilters;

  public List<String> stageTypeFilters;
}
