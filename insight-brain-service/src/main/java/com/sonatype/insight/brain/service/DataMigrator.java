/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Migrates operational data from an earlier schema/format to the latest version.
 * 
 * @since 1.11
 */
@Named
public class DataMigrator
{
  private final LicenseOverrideMigrator licenseOverrideMigrator;
  private final PolicyMigrator policyMigrator;
  private final PolicyEvaluationMigrator policyEvaluationMigrator;
  private final ProcureRemovalMigrator procureRemovalMigrator;
  private final NullHashModifiedMigrator modifiedMigrator;
  private final PolicyDroolsCodeMigrator policyDroolsCodeMigrator;
  private final DashboardFilterAppIdMigrator dashboardFilterAppIdMigrator;

  @Inject
  public DataMigrator(LicenseOverrideMigrator licenseOverrideMigrator, PolicyMigrator policyMigrator,
      PolicyEvaluationMigrator policyEvaluationMigrator, ProcureRemovalMigrator procureRemovalMigrator,
      NullHashModifiedMigrator modifiedMigrator, PolicyDroolsCodeMigrator policyDroolsCodeMigrator,
      DashboardFilterAppIdMigrator dashboardFilterAppIdMigrator)
  {
    this.licenseOverrideMigrator = licenseOverrideMigrator;
    this.policyMigrator = policyMigrator;
    this.policyEvaluationMigrator = policyEvaluationMigrator;
    this.procureRemovalMigrator = procureRemovalMigrator;
    this.modifiedMigrator = modifiedMigrator;
    this.policyDroolsCodeMigrator = policyDroolsCodeMigrator;
    this.dashboardFilterAppIdMigrator = dashboardFilterAppIdMigrator;
  }

  /**
   * Runs the data migration steps (if any). Obviously, this is best invoked before the application starts.
   */
  public void migrate() throws IOException {
    licenseOverrideMigrator.migrate();
    policyMigrator.migrate();
    policyEvaluationMigrator.migrate();
    procureRemovalMigrator.migrate();
    modifiedMigrator.migrate();
    policyDroolsCodeMigrator.migrate();
    dashboardFilterAppIdMigrator.migrate();
  }
}
