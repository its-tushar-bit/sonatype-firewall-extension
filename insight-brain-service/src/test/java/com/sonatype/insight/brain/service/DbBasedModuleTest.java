/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.dashboard.ApplicationRiskService;
import com.sonatype.insight.brain.dashboard.DashboardComponentRiskService;
import com.sonatype.insight.brain.dashboard.DashboardViolationRiskService;
import com.sonatype.insight.brain.dashboard.H2ApplicationRiskService;
import com.sonatype.insight.brain.dashboard.H2ComponentRiskService;
import com.sonatype.insight.brain.dashboard.H2DashboardViolationRiskService;
import com.sonatype.insight.brain.dashboard.PolicyWaiverService;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.testing.BrainInjectedTest;
import jakarta.inject.Inject;
import org.junit.Test;

@SuppressWarnings("unused")
public class DbBasedModuleTest
    extends BrainInjectedTest
{
  @Inject
  private DashboardViolationRiskService dashboardViolationRiskService;

  @Inject
  private DashboardComponentRiskService dashboardComponentRiskService;

  @Inject
  private ApplicationRiskService applicationRiskService;

  @Inject
  private PolicyWaiverService policyWaiverService;

  @Test
  public void h2DefaultTest() {
    assertThat(dashboardViolationRiskService).isInstanceOf(H2DashboardViolationRiskService.class);
    assertThat(dashboardComponentRiskService).isInstanceOf(H2ComponentRiskService.class);
    assertThat(applicationRiskService).isInstanceOf(H2ApplicationRiskService.class);
  }

  @Test
  @H2DiskTest
  public void h2DiskTest() {
    assertThat(dashboardViolationRiskService).isInstanceOf(H2DashboardViolationRiskService.class);
    assertThat(dashboardComponentRiskService).isInstanceOf(H2ComponentRiskService.class);
    assertThat(applicationRiskService).isInstanceOf(H2ApplicationRiskService.class);
  }
}
