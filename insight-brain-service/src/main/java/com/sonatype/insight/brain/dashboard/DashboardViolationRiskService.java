/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Set;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;

public interface DashboardViolationRiskService
{
  /**
   * Gets the violation risk matching the specified filter criteria. Empty or null filter criteria generally means
   * "all available" violations for that filter.
   */
  DashboardResultsDTO<DashboardViolationRiskDTO> get(
      Set<String> organizationIds,
      Set<String> applicationIds,
      Set<String> stageIds,
      Set<String> tagIds,
      PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyThreatLevelFilter policyThreatLevelFilter,
      PolicyViolationStateFilter policyViolationStateFilter,
      String orderBy,
      Integer maxDaysOld,
      int page,
      int pageSize);
}
