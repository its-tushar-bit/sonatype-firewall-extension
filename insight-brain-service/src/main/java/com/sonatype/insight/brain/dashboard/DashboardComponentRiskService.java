/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;

import java.util.Set;

public interface DashboardComponentRiskService
{
  /**
   * Gets the risk per component by rolling up the policy violations matching the specified filter criteria. Empty or
   * null filter criteria generally mean "all available" violations for that aspect. The results are sorted by
   * descending component risk scores.
   */
  DashboardResultsDTO<ComponentRiskDTO> getComponentRisks(
      Set<String> organizationIds,
      Set<String> applicationIds,
      Set<String> stageIds,
      Set<String> tagIds,
      PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyThreatLevelFilter policyThreatLevelFilter,
      PolicyViolationStateFilter policyViolationStateFilter,
      String orderBy,
      int page,
      int pageSize);

  /**
   * Page-scoped Classic component-risk cards for Martha list enrichment. Loads only the given
   * {@code componentHashes} (visible index page). No paging/orderBy — caller merges onto index order
   * then sorts within the page.
   */
  DashboardResultsDTO<ComponentRiskDTO> getComponentRiskCards(
      Set<String> organizationIds,
      Set<String> applicationIds,
      Set<String> componentHashes,
      Set<String> stageIds,
      Set<String> tagIds,
      PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyThreatLevelFilter policyThreatLevelFilter,
      PolicyViolationStateFilter policyViolationStateFilter);
}
