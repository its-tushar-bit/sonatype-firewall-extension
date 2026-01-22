/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;
import com.sonatype.insight.brain.common.test.SlowTest;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2InMemoryTest;

import org.junit.experimental.categories.Category;

@H2InMemoryTest
@Category(SlowTest.class)
public class H2DashboardViolationRiskServiceTest
    extends AbstractDashboardViolationRiskServiceTest
{
  @Inject
  private H2DashboardViolationRiskService dashboardViolationRiskService;

  @Override
  protected DashboardViolationRiskService getDashboardViolationRiskService() {
    return dashboardViolationRiskService;
  }
}
