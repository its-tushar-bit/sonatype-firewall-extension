/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;

/**
 * @since 1.11.0
 */
public class DashboardFilterDTO
{
  public static final Integer DEFAULT_MAX_DAYS_OLD = 30;

  public static final PolicyViolationState DEFAULT_POLICY_VIOLATION_STATE = PolicyViolationState.OPEN;

  public int minPolicyThreatLevel;

  public int maxPolicyThreatLevel;

  public List<String> applicationFilters;

  public List<String> organizationFilters;

  public List<String> tagFilters;

  public List<PolicyThreatCategory> policyThreatCategoryFilters;

  public List<String> stageTypeFilters;

  /**
   * The maximum age of risks that pass the filter, in days. When null, no age-based filtering is applied. Note that it
   * is not null by default however, so a null value must be set explicitly if desired.
   *
   * @since 1.27.0
   */
  public Integer maxDaysOld = DEFAULT_MAX_DAYS_OLD;

  public List<String> policyViolationStates = Collections.singletonList(DEFAULT_POLICY_VIOLATION_STATE.name());

  /**
   * @since 1.147
   */
  public ExpirationDate expirationDate = ExpirationDate.ALL;

  /**
   * @since 1.152
   */
  public List<String> repositoryFilters = Collections.emptyList();

  public List<String> policyWaiverReasonIds = Collections.emptyList();

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
    if (!repositoryFilters.equals(that.repositoryFilters)) {
      return false;
    }
    if (!tagFilters.equals(that.tagFilters)) {
      return false;
    }
    if (!policyThreatCategoryFilters.equals(that.policyThreatCategoryFilters)) {
      return false;
    }
    if (!Objects.equals(maxDaysOld, that.maxDaysOld)) {
      return false;
    }
    if (!policyViolationStates.equals(that.policyViolationStates)) {
      return false;
    }
    if (!expirationDate.equals(that.expirationDate)) {
      return false;
    }
    if (!policyWaiverReasonIds.equals(that.policyWaiverReasonIds)) {
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
    result = 31 * result + repositoryFilters.hashCode();
    result = 31 * result + tagFilters.hashCode();
    result = 31 * result + policyThreatCategoryFilters.hashCode();
    result = 31 * result + stageTypeFilters.hashCode();
    result = 31 * result + Objects.hashCode(maxDaysOld);
    result = 31 * result + policyViolationStates.hashCode();
    result = 31 * result + expirationDate.hashCode();
    result = 31 * result + policyWaiverReasonIds.hashCode();
    return result;
  }
}
