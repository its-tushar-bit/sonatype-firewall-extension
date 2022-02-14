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

  private final ProprietaryConfigMigrator proprietaryConfigMigrator;

  private final PolicyCoordinatesConditionTypeMigrator policyCoordinatesConditionTypeMigrator;

  private final PolicySecurityVulnerabilityConditionTypeMigrator policySecurityVulnerabilityConditionTypeMigrator;

  private final MarkerFileMigrator markerFileMigrator;

  private final MailConfigurationMigrator mailConfigurationMigrator;

  private final ProxyServerConfigurationMigrator proxyServerConfigurationMigrator;

  private final ProductLicenseMigrator productLicenseMigrator;

  private final PullRequestCommentingConfigMigrator pullRequestCommentingConfigMigrator;

  private final InternalSourceControlPolicyEvaluationsConfigMigrator internalSourceControlEvaluationsConfigMigrator;

  private final SourceControlFileStorageMigrator sourceControlFileStorageMigrator;

  private final AdminInitialPasswordMigrator adminInitialPasswordMigrator;

  @Inject
  public DataMigrator(
      PolicyJsonMigrator policyJsonMigrator,
      PolicyDroolsCodeMigrator policyDroolsCodeMigrator,
      SecurityVulnerabilityOverrideMigrator securityVulnerabilityOverrideMigrator,
      ProprietaryConfigMigrator proprietaryConfigMigrator,
      PolicyCoordinatesConditionTypeMigrator policyCoordinatesConditionTypeMigrator,
      PolicySecurityVulnerabilityConditionTypeMigrator policySecurityVulnerabilityConditionTypeMigrator,
      MarkerFileMigrator markerFileMigrator,
      MailConfigurationMigrator mailConfigurationMigrator,
      ProxyServerConfigurationMigrator proxyServerConfigurationMigrator,
      ProductLicenseMigrator productLicenseMigrator,
      PullRequestCommentingConfigMigrator pullRequestCommentingConfigMigrator,
      InternalSourceControlPolicyEvaluationsConfigMigrator internalSourceControlEvaluationsConfigMigrator,
      SourceControlFileStorageMigrator sourceControlFileStorageMigrator,
      AdminInitialPasswordMigrator adminInitialPasswordMigrator)
  {
    this.policyJsonMigrator = policyJsonMigrator;
    this.policyDroolsCodeMigrator = policyDroolsCodeMigrator;
    this.securityVulnerabilityOverrideMigrator = securityVulnerabilityOverrideMigrator;
    this.proprietaryConfigMigrator = proprietaryConfigMigrator;
    this.policyCoordinatesConditionTypeMigrator = policyCoordinatesConditionTypeMigrator;
    this.policySecurityVulnerabilityConditionTypeMigrator = policySecurityVulnerabilityConditionTypeMigrator;
    this.markerFileMigrator = markerFileMigrator;
    this.mailConfigurationMigrator = mailConfigurationMigrator;
    this.proxyServerConfigurationMigrator = proxyServerConfigurationMigrator;
    this.productLicenseMigrator = productLicenseMigrator;
    this.pullRequestCommentingConfigMigrator = pullRequestCommentingConfigMigrator;
    this.internalSourceControlEvaluationsConfigMigrator = internalSourceControlEvaluationsConfigMigrator;
    this.sourceControlFileStorageMigrator = sourceControlFileStorageMigrator;
    this.adminInitialPasswordMigrator = adminInitialPasswordMigrator;
  }

  /**
   * Runs the data migration steps (if any). Obviously, this is best invoked before the application starts.
   */
  public void migrate() throws IOException {
    markerFileMigrator.migrate();
    policyJsonMigrator.migrate();
    policyDroolsCodeMigrator.migrate();
    securityVulnerabilityOverrideMigrator.migrate();
    proprietaryConfigMigrator.migrate();
    policyCoordinatesConditionTypeMigrator.migrate();
    policySecurityVulnerabilityConditionTypeMigrator.migrate();
    mailConfigurationMigrator.migrate();
    proxyServerConfigurationMigrator.migrate();
    productLicenseMigrator.migrate();
    pullRequestCommentingConfigMigrator.migrate();
    internalSourceControlEvaluationsConfigMigrator.migrate();
    sourceControlFileStorageMigrator.migrate();
    adminInitialPasswordMigrator.migrate();
  }
}
