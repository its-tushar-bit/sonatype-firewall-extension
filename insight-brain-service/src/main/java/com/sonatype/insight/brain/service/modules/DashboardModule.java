/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import com.sonatype.insight.brain.dashboard.DashboardFilterService;
import com.sonatype.insight.brain.dashboard.DashboardUtils;
import com.sonatype.insight.brain.dashboard.H2ApplicationRiskService;
import com.sonatype.insight.brain.dashboard.H2ComponentRiskService;
import com.sonatype.insight.brain.dashboard.H2DashboardViolationRiskService;
import com.sonatype.insight.brain.dashboard.PostgresApplicationRiskService;
import com.sonatype.insight.brain.dashboard.PostgresComponentRiskService;
import com.sonatype.insight.brain.dashboard.PostgresDashboardViolationRiskService;

import com.google.inject.AbstractModule;

/**
 * Guice module providing explicit bindings for Dashboard components. This replaces Sisu's automatic @Named component
 * discovery.
 */
public class DashboardModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(DashboardFilterService.class);
    bind(DashboardUtils.class);
    bind(H2ApplicationRiskService.class);
    bind(H2ComponentRiskService.class);
    bind(H2DashboardViolationRiskService.class);
    bind(PostgresApplicationRiskService.class);
    bind(PostgresComponentRiskService.class);
    bind(PostgresDashboardViolationRiskService.class);
  }
}
