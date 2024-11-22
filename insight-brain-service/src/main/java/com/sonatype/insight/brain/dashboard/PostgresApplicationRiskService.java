/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApplicationTotalRiskDTO;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.CIApplicationFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.StageType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class PostgresApplicationRiskService
    implements ApplicationRiskService
{
  private static final Logger log = LoggerFactory.getLogger(PostgresApplicationRiskService.class);

  @Inject
  public PostgresApplicationRiskService() {
    // TODO - inject dependencies
    log.info("todo");
  }

  @Override
  public DashboardResultsDTO<ApplicationRiskScoreDTO> getApplicationRisks(
      final Set<String> organizationIds,
      final Set<String> applicationIds,
      final Set<String> stageIds,
      final Set<String> tagIds,
      final PolicyThreatCategoryFilter policyThreatCategories,
      final PolicyThreatLevelFilter policyThreatLevelRange,
      final PolicyViolationStateFilter policyViolationStates,
      final String orderBy,
      final int page,
      final int pageSize)
  {
    // TODO - CLM-32520
    return null;
  }

  @Override
  public DashboardResultsDTO<ApplicationTotalRiskDTO> getCIApplicationRisk(final CIApplicationFilter filter) {
    // TODO - CLM-32520
    return null;
  }

  @Override
  public List<ApplicationRiskScoreDTO> getRiskForApplicationsWithReadPermissions() {
    // TODO - CLM-32520
    return List.of();
  }

  @Override
  public ApplicationRiskScoreDTO getRiskForApp(final Application application, final Set<StageType> stageTypes) {
    // TODO - CLM-32520
    return null;
  }

  @Override
  public List<Application> getApplicationsWithReadPermission() {
    // TODO - CLM-32520
    return List.of();
  }
}
