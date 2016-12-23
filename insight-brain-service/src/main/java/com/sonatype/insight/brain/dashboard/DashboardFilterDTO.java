/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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

  public List<String> organizationFilters;

  public List<String> tagFilters;

  public List<PolicyThreatCategory> policyThreatCategoryFilters;

  public List<String> stageTypeFilters;

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DashboardFilterDTO that = (DashboardFilterDTO) o;

    if (minPolicyThreatLevel != that.minPolicyThreatLevel) {
      return false;
    }
    if (maxPolicyThreatLevel != that.maxPolicyThreatLevel) {
      return false;
    }
    if (!applicationFilters.equals(that.applicationFilters)) {
      return false;
    }
    if (!organizationFilters.equals(that.organizationFilters)) {
      return false;
    }
    if (!tagFilters.equals(that.tagFilters)) {
      return false;
    }
    if (!policyThreatCategoryFilters.equals(that.policyThreatCategoryFilters)) {
      return false;
    }
    return stageTypeFilters.equals(that.stageTypeFilters);
  }

  @Override
  public int hashCode() {
    int result = minPolicyThreatLevel;
    result = 31 * result + maxPolicyThreatLevel;
    result = 31 * result + applicationFilters.hashCode();
    result = 31 * result + organizationFilters.hashCode();
    result = 31 * result + tagFilters.hashCode();
    result = 31 * result + policyThreatCategoryFilters.hashCode();
    result = 31 * result + stageTypeFilters.hashCode();
    return result;
  }
}
