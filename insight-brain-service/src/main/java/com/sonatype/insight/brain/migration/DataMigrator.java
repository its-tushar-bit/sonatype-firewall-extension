/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

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
  private final SecurityVulnerabilityOverrideMigrator securityVulnerabilityOverrideMigrator;

  private final PolicyJsonMigrator policyJsonMigrator;

  private final PolicyDroolsCodeMigrator policyDroolsCodeMigrator;

  private final RootOrganizationConfigMigrator rootOrganizationConfigMigrator;

  private final ProprietaryConfigMigrator proprietaryConfigMigrator;

  private final PolicyCoordinatesConditionTypeMigrator policyCoordinatesConditionTypeMigrator;

  private final PolicySecurityVulnerabilityConditionTypeMigrator policySecurityVulnerabilityConditionTypeMigrator;

  @Inject
  public DataMigrator(PolicyJsonMigrator policyJsonMigrator,
                      PolicyDroolsCodeMigrator policyDroolsCodeMigrator,
                      RootOrganizationConfigMigrator rootOrganizationConfigMigrator,
                      SecurityVulnerabilityOverrideMigrator securityVulnerabilityOverrideMigrator,
                      ProprietaryConfigMigrator proprietaryConfigMigrator,
                      PolicyCoordinatesConditionTypeMigrator policyCoordinatesConditionTypeMigrator,
                      PolicySecurityVulnerabilityConditionTypeMigrator policySecurityVulnerabilityConditionTypeMigrator)
  {
    this.policyJsonMigrator = policyJsonMigrator;
    this.policyDroolsCodeMigrator = policyDroolsCodeMigrator;
    this.rootOrganizationConfigMigrator = rootOrganizationConfigMigrator;
    this.securityVulnerabilityOverrideMigrator = securityVulnerabilityOverrideMigrator;
    this.proprietaryConfigMigrator = proprietaryConfigMigrator;
    this.policyCoordinatesConditionTypeMigrator = policyCoordinatesConditionTypeMigrator;
    this.policySecurityVulnerabilityConditionTypeMigrator = policySecurityVulnerabilityConditionTypeMigrator;
  }

  /**
   * Runs the data migration steps (if any). Obviously, this is best invoked before the application starts.
   */
  public void migrate() throws IOException {
    policyJsonMigrator.migrate();
    policyDroolsCodeMigrator.migrate();
    rootOrganizationConfigMigrator.migrate();
    securityVulnerabilityOverrideMigrator.migrate();
    proprietaryConfigMigrator.migrate();
    policyCoordinatesConditionTypeMigrator.migrate();
    policySecurityVulnerabilityConditionTypeMigrator.migrate();
  }
}
