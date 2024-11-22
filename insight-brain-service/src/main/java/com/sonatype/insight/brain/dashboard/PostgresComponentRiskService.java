/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class PostgresComponentRiskService
    implements ComponentRiskService
{
  private static final Logger log = LoggerFactory.getLogger(PostgresComponentRiskService.class);

  @Inject
  public PostgresComponentRiskService() {
    // TODO - inject dependencies
    log.info("todo");
  }

  @Override
  public DashboardResultsDTO<ComponentRiskDTO> getComponentRisks(
      final Set<String> organizationIds,
      final Set<String> applicationIds,
      final Set<String> stageIds,
      final Set<String> tagIds,
      final PolicyThreatCategoryFilter policyThreatCategoryFilter,
      final PolicyThreatLevelFilter policyThreatLevelFilter,
      final PolicyViolationStateFilter policyViolationStateFilter,
      final String orderBy,
      final int page,
      final int pageSize)
  {
    // TODO - CLM-32517
    return null;
  }
}
