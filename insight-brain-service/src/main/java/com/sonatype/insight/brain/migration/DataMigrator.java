/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ClusterLock;

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

  private final AdminInitialPasswordMigrator adminInitialPasswordMigrator;

  private final ReverseProxyAuthenticationConfigurationMigrator reverseProxyAuthenticationConfigurationMigrator;

  private final BaseUrlConfigurationMigrator baseUrlConfigurationMigrator;

  private final JiraConfigurationMigrator jiraConfigurationMigrator;

  private final SourceControlConfigurationMigrator sourceControlConfigurationMigrator;

  private final SourceControlFileStorageMigrator sourceControlFileStorageMigrator;

  private final SimpleConfigurationMigrator simpleConfigurationMigrator;

  private final PolicyWaiverComponentPurlMigrator policyWaiverComponentPurlMigrator;

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
      AdminInitialPasswordMigrator adminInitialPasswordMigrator,
      ReverseProxyAuthenticationConfigurationMigrator reverseProxyAuthenticationConfigurationMigrator,
      BaseUrlConfigurationMigrator baseUrlConfigurationMigrator,
      JiraConfigurationMigrator jiraConfigurationMigrator,
      SourceControlConfigurationMigrator sourceControlConfigurationMigrator,
      SourceControlFileStorageMigrator sourceControlFileStorageMigrator,
      SimpleConfigurationMigrator simpleConfigurationMigrator,
      PolicyWaiverComponentPurlMigrator policyWaiverComponentPurlMigrator)
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
    this.adminInitialPasswordMigrator = adminInitialPasswordMigrator;
    this.reverseProxyAuthenticationConfigurationMigrator = reverseProxyAuthenticationConfigurationMigrator;
    this.baseUrlConfigurationMigrator = baseUrlConfigurationMigrator;
    this.jiraConfigurationMigrator = jiraConfigurationMigrator;
    this.sourceControlConfigurationMigrator = sourceControlConfigurationMigrator;
    this.sourceControlFileStorageMigrator = sourceControlFileStorageMigrator;
    this.simpleConfigurationMigrator = simpleConfigurationMigrator;
    this.policyWaiverComponentPurlMigrator = policyWaiverComponentPurlMigrator;
  }

  /**
   * Runs the data migration steps (if any). Obviously, this is best invoked before the application starts.
   */
  public void migrate() throws IOException {
    try (ClusterLock clusterLock = ClusterLock.createForDataMigration()) {
      clusterLock.lock();
      runMigrators();
    }
  }

  // Visible for testing
  void runMigrators() throws IOException {
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
    adminInitialPasswordMigrator.migrate();
    reverseProxyAuthenticationConfigurationMigrator.migrate();
    baseUrlConfigurationMigrator.migrate();
    jiraConfigurationMigrator.migrate();
    sourceControlConfigurationMigrator.migrate();
    sourceControlFileStorageMigrator.migrate();
    simpleConfigurationMigrator.migrate();
    policyWaiverComponentPurlMigrator.migrate();
  }
}
