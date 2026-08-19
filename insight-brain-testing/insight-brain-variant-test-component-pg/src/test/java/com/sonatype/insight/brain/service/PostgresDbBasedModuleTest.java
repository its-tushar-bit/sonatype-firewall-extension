/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.dashboard.ApplicationRiskService;
import com.sonatype.insight.brain.dashboard.DashboardComponentRiskService;
import com.sonatype.insight.brain.dashboard.DashboardViolationRiskService;
import com.sonatype.insight.brain.dashboard.PostgresApplicationRiskService;
import com.sonatype.insight.brain.dashboard.PostgresComponentRiskService;
import com.sonatype.insight.brain.dashboard.PostgresDashboardViolationRiskService;
import com.sonatype.insight.brain.variant.AbstractComponentPgTest;
import com.sonatype.insight.brain.variant.ComponentPgTest;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL binding assertions relocated from {@code DbBasedModuleTest} (CLM-45235). The H2 / H2-disk coverage
 * stays in the origin {@code DbBasedModuleTest}.
 */
@ComponentPgTest
public class PostgresDbBasedModuleTest
    extends AbstractComponentPgTest
{
  @Inject
  private DashboardViolationRiskService dashboardViolationRiskService;

  @Inject
  private DashboardComponentRiskService dashboardComponentRiskService;

  @Inject
  private ApplicationRiskService applicationRiskService;

  @Test
  public void postgresTest() {
    assertThat(dashboardViolationRiskService).isInstanceOf(PostgresDashboardViolationRiskService.class);
    assertThat(dashboardComponentRiskService).isInstanceOf(PostgresComponentRiskService.class);
    assertThat(applicationRiskService).isInstanceOf(PostgresApplicationRiskService.class);
  }
}
