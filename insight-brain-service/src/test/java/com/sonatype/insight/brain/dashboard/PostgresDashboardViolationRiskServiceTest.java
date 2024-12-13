/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import javax.inject.Inject;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

@PostgresTest
public class PostgresDashboardViolationRiskServiceTest
    extends AbstractDashboardViolationRiskServiceTest
{
  @Inject
  private PostgresDashboardViolationRiskService dashboardViolationRiskService;

  @Override
  protected DashboardViolationRiskService getDashboardViolationRiskService() {
    return dashboardViolationRiskService;
  }
}
