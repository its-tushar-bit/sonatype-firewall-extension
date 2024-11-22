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
public class PostgresNewestRiskService
    implements NewestRiskService
{
  private static final Logger log = LoggerFactory.getLogger(PostgresNewestRiskService.class);

  @Inject
  public PostgresNewestRiskService() {
    // TODO - inject dependencies
    log.info("todo");
  }

  @Override
  public DashboardResultsDTO<NewestRiskDTO> getNewestRisks(
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
      int pageSize)
  {
    // TODO - CLM-32516
    return null;
  }
}
