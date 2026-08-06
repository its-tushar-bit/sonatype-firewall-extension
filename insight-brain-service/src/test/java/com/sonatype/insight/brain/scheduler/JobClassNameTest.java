/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightJob;

import org.junit.Test;
import org.quartz.Job;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class JobClassNameTest
    extends AbstractComponentTest
{
  private static final Map<String, String> insightJobClassNameToExpectedJobName = new HashMap<>();

  static {
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.git.DefaultBranchMonitor",
        "DefaultBranchMonitor");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.hds.DefaultLicenseDataUpdater",
        "LoadLicenses");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.scheduler.NonConcurrentTestJob",
        "NonConcurrentTestJob");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.scheduler.TestJob", "TestJob");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.service.AdminServletTest$TestBlockJob",
        "TestBlockJob");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.component.RepositoryIdentifiedComponentPurger",
        "RepositoryIdentifiedComponentPurger");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.git.PullRequestCommentPurger",
        "PullRequestCommentPurger");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.sourcecontrol.SourceControlStaleEventResetJob",
        "SourceControlStaleEventResetJob");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.git.PullRequestMonitor", "PullRequestMonitor");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.hds.ComponentCategoryUpdater",
        "LoadComponentCategories");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.migration.ScanFileCleaner", "ScanFileCleaner");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.policy.PolicyMonitoringTask",
        "PolicyMonitoringTask");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.relay.RelayEventLogCleanupTask",
        "RelayEventLogCleanupTask");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.report.ReportPurger", "ReportPurger");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.repository.IgnoredRepositoryComponentCleaner",
        "IgnoredRepositoryComponentCleaner");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.scan.PersistedScanTicketCleaner",
        "PersistedScanTicketCleaner");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.security.ClearRolePermissionCache",
        "ClearRolePermissionCache");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.security.SamlConfigurationCache",
        "SamlConfigurationCache");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.successmetrics.SuccessMetricsPurger",
        "SuccessMetricsPurger");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.telemetry.ClusterTelemetryTask",
        "ClusterTelemetrySender");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternUpdater",
        "FirewallIgnorePatternUpdater");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.service.githubapp.GitHubAppCleanupTask",
        "GitHubAppCleanupTask");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.policy.evaluator.PersistedPolicyEvaluationPollingResultCleaner",
        "PersistedPolicyEvaluationPollingResultCleaner");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.policy.evaluator.queue.EvaluationQueueProducer",
        "EvaluationQueueProducer");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.product.license.CLMLicenseManager",
        "ProductLicenseLoad");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.repository.autorelease.AutomaticQuarantineReleaseTask",
        "AutomaticQuarantineReleaseTask");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.repository.component.QuarantinedComponentAccessPurger",
        "QuarantinedComponentAccessPurger");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.search.index.IndexService",
        "SearchIndexUpdate");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.search.index.IndexCreationScheduler",
        "SearchIndexCreate");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.api.v2.service.ApiConfigurationService",
        "Configuration");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.api.v2.service.ApiJiraConfigurationService",
        "JiraConfiguration");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.api.v2.service.ApiProxyServerConfigurationService",
        "ProxyServerConfiguration");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.api.v2.service.ApiRepositoryIdentifiedComponentService",
        "DeleteRepositoryIdentifiedComponent");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.api.v2.service.ApiReverseProxyAuthenticationConfigurationService",
        "ReverseProxyAuthenticationConfiguration");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.api.v2.service.ApiSourceControlConfigurationService", "SourceControlConfiguration");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.policy.waiver.WaivedComponentUpgradeTask", "WaivedComponentUpgradeTask");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.policy.waiver.WaiverExpirationDetectionTask", "WaiverExpirationDetectionTask");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.repository.ProprietaryComponentNameDetector", "InvalidateComponentNameMatchers");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.organization.ApplicationCountHistoryKeeper", "ApplicationCountHistoryKeeper");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.firewall.metrics.FirewallMetricsComponentWaivedConsolidatorCronJob",
        "FirewallMetricsComponentWaivedConsolidatorCronJob");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.firewall.metrics.FirewallMetricsComponentQuarantinedConsolidatorCronJob",
        "FirewallMetricsComponentQuarantinedConsolidatorCronJob");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.firewall.metrics.FirewallMetricsComponentsAutoReleasedConsolidatorCronJob",
        "FirewallMetricsComponentsAutoReleasedConsolidatorCronJob");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.firewall.metrics.DeleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob",
        "DeleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService",
        "UpdateEnterpriseDashboardLocalCache");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.sbom.PendingSbomMetadataCleaner",
        "PendingSbomMetadataCleanerJob");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.migration.AsyncDbMigrationScheduler",
        "AsyncDbMigrationScheduler");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.telemetry.HistoricalPolicyViolationTelemetryTask",
        "HistoricalPolicyViolationTelemetryTask");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.malware.defense.changedetection.ComponentChangeDetectionTask",
        "ComponentChangeDetectionTask");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryBackfillTask",
        "PolicyWaiverTelemetryBackfillTask");
    insightJobClassNameToExpectedJobName.put("com.sonatype.insight.brain.zscaler.ZScalerUpdater", "ZScalerUpdater");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.git.PullRequestStateUpdateJob",
        "PullRequestStateUpdateJob");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.service.CopyStorageTask",
        "CopyStorageTask");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.repository.ReevaluateCascadeRequestCleaner", "ReevaluateCascadeRequestCleaner");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.service.SystemConfigurationPropertyCacheInvalidationJob",
        "SystemConfigurationPropertyCacheInvalidation");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.relay.RelayLinkRetrySweepTask", "RelayLinkRetrySweepTask");
    insightJobClassNameToExpectedJobName.put(
        "com.sonatype.insight.brain.continuousmonitoring.RepositoryEvaluationQueueProducerJob",
        "RepositoryEvaluationQueueProducerJob");
  }

  @Inject
  private Set<Job> jobs;

  @Inject
  private Set<InsightJob> insightJobs;

  @Test
  public void testJobShouldImplementInsightJob() {
    for (Job job : jobs) {
      if (!(job instanceof InsightJob)) {
        fail("All jobs should implement InsightJob but " + job.getClass().getName() + " does not.");
      }
    }
  }

  @Test
  public void testInsightJobHasExpectedJobName() {
    for (InsightJob insightJob : insightJobs) {
      Class<?> userClass = ClassUtils.getUserClass(insightJob);
      String insightJobClassName = userClass.getName();
      String expectedJobName = insightJobClassNameToExpectedJobName.get(insightJobClassName);
      if (expectedJobName == null) {
        fail("InsightJob " + insightJobClassName + " has no expected job name, " +
            "if this is a new class update this test with its fixed class/job names, " +
            "otherwise if this is a renamed class a migration script may be needed as well as updating this test " +
            "see https://issues.sonatype.org/browse/CLM-24241.");
      }
      if (!expectedJobName.equals(insightJob.getJobName())) {
        assertThat(insightJob.getJobName()).as("If job names have been changed a migration script may be " +
            "needed as well as updating this test.").isEqualTo(expectedJobName);
      }
    }
  }

  @Test
  public void testInsightJobExists() {
    for (String insightJobClassName : insightJobClassNameToExpectedJobName.keySet()) {
      try {
        Class.forName(insightJobClassName);
      }
      catch (ClassNotFoundException e) {
        fail("InsightJob " + insightJobClassName + " no longer exists, if this class has been removed or updated a " +
            "migration script may be needed as well as updating this test.");
      }
    }
  }
}
