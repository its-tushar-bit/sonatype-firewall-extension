/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import java.util.Set;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import com.sonatype.insight.brain.migration.AbstractAsyncDbMigration;
import com.sonatype.insight.brain.migration.AdminInitialPasswordMigrator;
import com.sonatype.insight.brain.migration.AsyncDbMigrationScheduler;
import com.sonatype.insight.brain.migration.BaseUrlConfigurationMigrator;
import com.sonatype.insight.brain.migration.DataMigrator;
import com.sonatype.insight.brain.migration.DisplayNameForFileCoordinateAsyncDbMigration;
import com.sonatype.insight.brain.migration.FirewallMetricsMigrator;
import com.sonatype.insight.brain.migration.InternalSourceControlPolicyEvaluationsConfigMigrator;
import com.sonatype.insight.brain.migration.JiraConfigurationMigrator;
import com.sonatype.insight.brain.migration.MailConfigurationMigrator;
import com.sonatype.insight.brain.migration.MarkerFileMigrator;
import com.sonatype.insight.brain.migration.PolicyCoordinatesConditionTypeMigrator;
import com.sonatype.insight.brain.migration.PolicyDroolsCodeMigrator;
import com.sonatype.insight.brain.migration.PolicyJsonMigrator;
import com.sonatype.insight.brain.migration.PolicyViolationIndexAsyncDbMigration;
import com.sonatype.insight.brain.migration.PolicySecurityVulnerabilityConditionTypeMigrator;
import com.sonatype.insight.brain.policy.violation.PolicyViolationConstraintFactsJsonAsyncDbMigration;
import com.sonatype.insight.brain.policy.violation.RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration;
import com.sonatype.insight.brain.migration.PolicyWaiverComponentPurlMigrator;
import com.sonatype.insight.brain.migration.ProductLicenseMigrator;
import com.sonatype.insight.brain.migration.ProprietaryConfigMigrator;
import com.sonatype.insight.brain.migration.ProxyServerConfigurationMigrator;
import com.sonatype.insight.brain.migration.PullRequestCommentingConfigMigrator;
import com.sonatype.insight.brain.migration.RepositoryComponentDisplayNameMigrator;
import com.sonatype.insight.brain.migration.ReverseProxyAuthenticationConfigurationMigrator;
import com.sonatype.insight.brain.migration.SamlUserGroupMigrator;
import com.sonatype.insight.brain.migration.ScanFileCleaner;
import com.sonatype.insight.brain.migration.SecurityVulnerabilityOverrideMigrator;
import com.sonatype.insight.brain.migration.SimpleConfigurationMigrator;
import com.sonatype.insight.brain.migration.SourceControlConfigurationMigrator;
import com.sonatype.insight.brain.migration.SourceControlFileStorageMigrator;

/**
 * Guice module providing explicit bindings for Migration components.
 * This replaces Sisu's automatic @Named component discovery.
 */
public class MigrationModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(AdminInitialPasswordMigrator.class).in(Singleton.class);
    bind(AsyncDbMigrationScheduler.class).in(Singleton.class);
    bind(BaseUrlConfigurationMigrator.class).in(Singleton.class);
    bind(DataMigrator.class).in(Singleton.class);
    bind(DisplayNameForFileCoordinateAsyncDbMigration.class).in(Singleton.class);
    bind(FirewallMetricsMigrator.class).in(Singleton.class);
    bind(InternalSourceControlPolicyEvaluationsConfigMigrator.class).in(Singleton.class);
    bind(JiraConfigurationMigrator.class).in(Singleton.class);
    bind(MailConfigurationMigrator.class).in(Singleton.class);
    bind(MarkerFileMigrator.class).in(Singleton.class);
    bind(PolicyCoordinatesConditionTypeMigrator.class).in(Singleton.class);
    bind(PolicyDroolsCodeMigrator.class).in(Singleton.class);
    bind(PolicyJsonMigrator.class).in(Singleton.class);
    bind(PolicyViolationIndexAsyncDbMigration.class).in(Singleton.class);
    bind(PolicySecurityVulnerabilityConditionTypeMigrator.class).in(Singleton.class);
    bind(PolicyWaiverComponentPurlMigrator.class).in(Singleton.class);
    bind(ProductLicenseMigrator.class).in(Singleton.class);
    bind(ProprietaryConfigMigrator.class).in(Singleton.class);
    bind(ProxyServerConfigurationMigrator.class).in(Singleton.class);
    bind(PullRequestCommentingConfigMigrator.class).in(Singleton.class);
    bind(RepositoryComponentDisplayNameMigrator.class).in(Singleton.class);
    bind(ReverseProxyAuthenticationConfigurationMigrator.class).in(Singleton.class);
    bind(SamlUserGroupMigrator.class).in(Singleton.class);
    bind(ScanFileCleaner.class).in(Singleton.class);
    bind(SecurityVulnerabilityOverrideMigrator.class).in(Singleton.class);
    bind(SimpleConfigurationMigrator.class).in(Singleton.class);
    bind(SourceControlConfigurationMigrator.class).in(Singleton.class);
    bind(SourceControlFileStorageMigrator.class).in(Singleton.class);
  }

  /**
   * Provides a set of AbstractAsyncDbMigration instances. This replaces Sisu's automatic collection of @Named
   * AsyncDbMigration components.
   */
  @Provides
  @Singleton
  public Set<AbstractAsyncDbMigration> provideAsyncDbMigrations(
      PolicyViolationConstraintFactsJsonAsyncDbMigration policyViolationConstraintFactsMigration,
      RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration repoPolicyViolationConstraintFactsMigration,
      DisplayNameForFileCoordinateAsyncDbMigration displayNameMigration,
      PolicyViolationIndexAsyncDbMigration policyViolationIndexMigration)
  {
    return Set.of(
        policyViolationConstraintFactsMigration,
        repoPolicyViolationConstraintFactsMigration,
        displayNameMigration,
        policyViolationIndexMigration);
  }
}
