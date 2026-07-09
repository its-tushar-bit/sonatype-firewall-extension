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
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.StageType;

public interface ApplicationRiskService
{
  DashboardResultsDTO<ApplicationRiskScoreDTO> getApplicationRisks(
      Set<String> organizationIds,
      Set<String> applicationIds,
      Set<String> stageIds,
      Set<String> tagIds,
      PolicyThreatCategoryFilter policyThreatCategories,
      PolicyThreatLevelFilter policyThreatLevelRange,
      PolicyViolationStateFilter policyViolationStates,
      String orderBy,
      int page,
      int pageSize);

  /**
   * Evaluation cards for a fixed application id set. Unlike {@link #getApplicationRisks}, retains
   * evaluated apps with zero violations so {@link ApplicationRiskScoreDTO#lastEvaluationTime} is
   * available on Martha list cards.
   */
  DashboardResultsDTO<ApplicationRiskScoreDTO> getApplicationRiskCards(
      Set<String> organizationIds,
      Set<String> applicationIds,
      Set<String> stageIds,
      Set<String> tagIds,
      PolicyThreatCategoryFilter policyThreatCategories,
      PolicyThreatLevelFilter policyThreatLevelRange,
      PolicyViolationStateFilter policyViolationStates);

  ApplicationRiskScoreDTO getRiskForApp(final Application application, final Set<StageType> stageTypes);
}
