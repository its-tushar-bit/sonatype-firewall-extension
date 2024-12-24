/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.function.Supplier;

import com.sonatype.insight.brain.dashboard.ApplicationRiskService;
import com.sonatype.insight.brain.dashboard.DashboardComponentRiskService;
import com.sonatype.insight.brain.dashboard.DashboardViolationRiskService;
import com.sonatype.insight.brain.dashboard.H2ApplicationRiskService;
import com.sonatype.insight.brain.dashboard.H2ComponentRiskService;
import com.sonatype.insight.brain.dashboard.H2DashboardViolationRiskService;
import com.sonatype.insight.brain.dashboard.H2PolicyWaiverService;
import com.sonatype.insight.brain.dashboard.PolicyWaiverService;
import com.sonatype.insight.brain.dashboard.PostgresApplicationRiskService;
import com.sonatype.insight.brain.dashboard.PostgresComponentRiskService;
import com.sonatype.insight.brain.dashboard.PostgresDashboardViolationRiskService;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseUtil;

import com.google.inject.AbstractModule;

/**
 * Guice module to bind the appropriate services based on the database configuration.
 */
public class DbBasedModule
    extends AbstractModule
{
  private final Supplier<DatabaseContainer> databaseContainerSupplier;

  // note: argument is a Supplier as this happens during app boot and DatabaseContainer is not available yet
  public DbBasedModule(final Supplier<DatabaseContainer> databaseContainerSupplier) {
    this.databaseContainerSupplier = databaseContainerSupplier;
  }

  @Override
  public void configure() {
    boolean isDatabaseEmbedded =
        DatabaseUtil.isDatabaseEmbedded(databaseContainerSupplier.get().getOperationalDataStore().getDatabaseConfig());
    if (isDatabaseEmbedded) {
      bind(DashboardViolationRiskService.class).to(H2DashboardViolationRiskService.class);
      bind(DashboardComponentRiskService.class).to(H2ComponentRiskService.class);
      bind(ApplicationRiskService.class).to(H2ApplicationRiskService.class);
      bind(PolicyWaiverService.class).to(H2PolicyWaiverService.class);
    }
    else {
      bind(DashboardComponentRiskService.class).to(PostgresComponentRiskService.class);
      bind(ApplicationRiskService.class).to(PostgresApplicationRiskService.class);
      bind(DashboardViolationRiskService.class).to(PostgresDashboardViolationRiskService.class);
      bind(PolicyWaiverService.class).to(H2PolicyWaiverService.class);
      // TODO - update as tickets of CLM-32515 are completed
      //bind(PolicyWaiverService.class).to(PostgresPolicyWaiverService.class);
    }
  }
}
