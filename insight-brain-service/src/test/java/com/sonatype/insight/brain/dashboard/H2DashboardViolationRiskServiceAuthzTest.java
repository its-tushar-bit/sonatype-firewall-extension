/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import jakarta.inject.Inject;

public class H2DashboardViolationRiskServiceAuthzTest
    extends AbstractDashboardViolationRiskServiceAuthzTest
{
  @Inject
  private H2DashboardViolationRiskService dashboardViolationRiskService;

  @Override
  protected DashboardViolationRiskService getDashboardViolationRiskService() {
    return dashboardViolationRiskService;
  }
}
