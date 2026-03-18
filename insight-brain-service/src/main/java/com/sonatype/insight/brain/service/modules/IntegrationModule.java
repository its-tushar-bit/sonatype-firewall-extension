/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import java.util.List;

import com.sonatype.insight.brain.git.BitbucketCodeInsightsService;
import com.sonatype.insight.brain.git.DefaultBranchMonitor;
import com.sonatype.insight.brain.git.DefaultBranchMonitorExecutor;
import com.sonatype.insight.brain.git.GitApiFactory;
import com.sonatype.insight.brain.git.GitClientCacheProvider;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.git.GitCommitHistoryService;
import com.sonatype.insight.brain.git.GitCommitStatusService;
import com.sonatype.insight.brain.git.IqForScmLicenseChecker;
import com.sonatype.insight.brain.git.ManualPullRequestFeatureCheck;
import com.sonatype.insight.brain.git.PullRequestCommentCreator;
import com.sonatype.insight.brain.git.PullRequestCommentPurger;
import com.sonatype.insight.brain.git.PullRequestCommentingClient;
import com.sonatype.insight.brain.git.PullRequestCommentingEligibilityValidator;
import com.sonatype.insight.brain.git.PullRequestCommentingEventHandler;
import com.sonatype.insight.brain.git.PullRequestCommentingMetricsService;
import com.sonatype.insight.brain.git.PullRequestCommentingRemediationService;
import com.sonatype.insight.brain.git.PullRequestCommentingService;
import com.sonatype.insight.brain.git.PullRequestDefaultBranchPolicyEvaluationResolver;
import com.sonatype.insight.brain.git.PullRequestEligibilityValidator;
import com.sonatype.insight.brain.git.PullRequestFeedbackMarkupService;
import com.sonatype.insight.brain.git.PullRequestInfoClient;
import com.sonatype.insight.brain.git.PullRequestLineCommentingService;
import com.sonatype.insight.brain.git.PullRequestLocationDiscoveryService;
import com.sonatype.insight.brain.git.PullRequestMonitor;
import com.sonatype.insight.brain.git.PullRequestPolicyEvaluationResolver;
import com.sonatype.insight.brain.git.PullRequestPollingScheduler;
import com.sonatype.insight.brain.git.PullRequestCommentingHashBuilder;
import com.sonatype.insight.brain.git.PullRequestPollingService;
import com.sonatype.insight.brain.git.PullRequestPostCommentAction;
import com.sonatype.insight.brain.git.PullRequestRemediationService;
import com.sonatype.insight.brain.git.PullRequestRepositoryValidator;
import com.sonatype.insight.brain.git.PullRequestTask;
import com.sonatype.insight.brain.git.ManualPullRequestService;
import com.sonatype.insight.brain.git.RemediationBranchNamePrefixGenerator;
import com.sonatype.insight.brain.git.PullRequestStateEventHandler;
import com.sonatype.insight.brain.git.PullRequestStateService;
import com.sonatype.insight.brain.git.PullRequestStateUpdateJob;
import com.sonatype.insight.brain.git.PullRequestStatusService;
import com.sonatype.insight.brain.git.PullRequestTargetCommitPolicyEvaluationResolver;
import com.sonatype.insight.brain.git.RemediationPullRequestEligibilityService;
import com.sonatype.insight.brain.git.RemediationPullRequestFeatureCheck;
import com.sonatype.insight.brain.git.ScmApplicationNameConverter;
import com.sonatype.insight.brain.git.ScmOnboardingService;
import com.sonatype.insight.brain.git.ScmRateLimitMetrics;
import com.sonatype.insight.brain.git.ScmRateLimitProvider;
import com.sonatype.insight.brain.git.ScmReducedSecurityService;
import com.sonatype.insight.brain.git.ScmRepoVisibilityService;
import com.sonatype.insight.brain.git.ScmStatusHelper;
import com.sonatype.insight.brain.git.ScmUserMappingService;
import com.sonatype.insight.brain.git.ScmUserMatchingService;
import com.sonatype.insight.brain.git.SourceControlComponentLoader;
import com.sonatype.insight.brain.git.SourceControlScanService;
import com.sonatype.insight.brain.git.SourceControlService;
import com.sonatype.insight.brain.git.SourceControlSshService;
import com.sonatype.insight.brain.git.event.SourceControlEventFinder;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.git.event.orchestrate.SourceControlEventOrchestrator;
import com.sonatype.insight.brain.git.event.orchestrate.SourceControlEventProcessor;
import com.sonatype.insight.brain.git.pullrequestcreationservice.AutomatedPullRequestCreationService;
import com.sonatype.insight.brain.git.pullrequestcreationservice.ManualPullRequestCreationService;
import com.sonatype.insight.brain.git.render.ComponentFeedbackContextFactory;
import com.sonatype.insight.brain.git.render.SecurityIssueService;
import com.sonatype.insight.brain.git.utils.PullRequestBranchNameGenerator;
import com.sonatype.insight.brain.hds.ComponentCategoryUpdater;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.ComponentRemediationService;
import com.sonatype.insight.brain.hds.DefaultLicenseDataUpdater;
import com.sonatype.insight.brain.hds.FirewallAuditHdsClient;
import com.sonatype.insight.brain.hds.FirewallQuarantineHdsClient;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.HdsPingService;
import com.sonatype.insight.brain.hds.IntegrationVersionCache;
import com.sonatype.insight.brain.hds.IntegrationVersionCacheLoader;
import com.sonatype.insight.brain.hds.PingHdsClient;
import com.sonatype.insight.brain.hds.ReferencePolicyFetcher;
import com.sonatype.insight.brain.hds.ScanHandler;
import com.sonatype.insight.brain.hds.ScanUploadService;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.hds.VersionScoringService;
import com.sonatype.insight.brain.integration.ApplicationForContainerImageFirewallService;
import com.sonatype.insight.brain.integration.ApplicationSummaryService;
import com.sonatype.insight.brain.integration.OrganizationSummaryService;
import com.sonatype.insight.brain.integration.PolicyEvaluationSummaryService;
import com.sonatype.insight.brain.integration.RepositorySummaryService;
import com.sonatype.insight.brain.integration.repository.ArtifactoryRepositoryService;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternService;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternUpdater;
import com.sonatype.insight.brain.integration.repository.FirewallMigrationService;
import com.sonatype.insight.brain.integration.repository.RepositoryContainerImageService;
import com.sonatype.insight.brain.integration.repository.RepositoryService;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Guice module providing explicit bindings for Integration components. This replaces Sisu's automatic @Named component
 * discovery.
 */
public class IntegrationModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(ApplicationForContainerImageFirewallService.class);
    bind(ApplicationSummaryService.class);
    bind(ArtifactoryRepositoryService.class);
    bind(AutomatedPullRequestCreationService.class);
    bind(BitbucketCodeInsightsService.class);
    bind(ComponentCategoryUpdater.class);
    bind(ComponentDetailsLoaderFactory.class);
    bind(ComponentFeedbackContextFactory.class);
    bind(ComponentInfoService.class);
    bind(ComponentRemediationService.class);
    bind(DefaultBranchMonitor.class);
    bind(DefaultBranchMonitorExecutor.class);
    bind(DefaultLicenseDataUpdater.class);
    bind(FirewallAuditHdsClient.class);
    bind(FirewallIgnorePatternService.class);
    bind(FirewallIgnorePatternUpdater.class);
    bind(FirewallMigrationService.class);
    bind(FirewallQuarantineHdsClient.class);
    bind(GitApiFactory.class);
    bind(GitClientCacheProvider.class);
    bind(GitClientFactory.class);
    bind(GitCommitHistoryService.class);
    bind(GitCommitStatusService.class);
    bind(HdsClient.class);
    bind(HdsPingService.class);
    bind(IntegrationVersionCache.class);
    bind(IntegrationVersionCacheLoader.class);
    bind(IqForScmLicenseChecker.class);
    bind(ManualPullRequestCreationService.class);
    bind(ManualPullRequestFeatureCheck.class);
    bind(OrganizationSummaryService.class);
    bind(PingHdsClient.class);
    bind(PolicyEvaluationSummaryService.class);
    bind(PullRequestBranchNameGenerator.class);
    bind(PullRequestCommentCreator.class);
    bind(PullRequestCommentPurger.class);
    bind(PullRequestCommentingClient.class);
    bind(PullRequestCommentingEligibilityValidator.class);
    bind(PullRequestCommentingEventHandler.class);
    bind(PullRequestCommentingMetricsService.class);
    bind(PullRequestCommentingRemediationService.class);
    bind(PullRequestCommentingService.class);
    bind(PullRequestDefaultBranchPolicyEvaluationResolver.class);
    bind(PullRequestEligibilityValidator.class);
    bind(PullRequestFeedbackMarkupService.class);
    bind(PullRequestInfoClient.class);
    bind(PullRequestLineCommentingService.class);
    bind(PullRequestLocationDiscoveryService.class);
    bind(PullRequestMonitor.class);
    bind(PullRequestPolicyEvaluationResolver.class);
    bind(PullRequestPollingScheduler.class);
    bind(PullRequestPollingService.class);
    bind(PullRequestRemediationService.class);
    bind(PullRequestRepositoryValidator.class);
    bind(PullRequestStateEventHandler.class);
    bind(PullRequestStateService.class);
    bind(PullRequestStateUpdateJob.class);
    bind(PullRequestStatusService.class);
    bind(PullRequestTargetCommitPolicyEvaluationResolver.class);
    bind(ReferencePolicyFetcher.class);
    bind(RemediationPullRequestEligibilityService.class);
    bind(RemediationPullRequestFeatureCheck.class);
    bind(RepositoryContainerImageService.class);
    bind(RepositoryService.class);
    bind(RepositorySummaryService.class);
    bind(ScanHandler.class);
    bind(ScanUploadService.class);
    bind(ScanUploader.class);
    bind(ScmApplicationNameConverter.class);
    bind(ScmOnboardingService.class);
    bind(ScmRateLimitMetrics.class);
    bind(ScmRateLimitProvider.class);
    bind(ScmReducedSecurityService.class);
    bind(ScmRepoVisibilityService.class);
    bind(ScmStatusHelper.class);
    bind(ScmUserMappingService.class);
    bind(ScmUserMatchingService.class);
    bind(SecurityIssueService.class);
    bind(SourceControlComponentLoader.class);
    bind(SourceControlEventFinder.class);
    bind(SourceControlEventOrchestrator.class);
    bind(SourceControlEventProcessor.class);
    bind(SourceControlEventPublisher.class);
    bind(SourceControlScanService.class);
    bind(SourceControlService.class);
    bind(SourceControlSshService.class);
    bind(TelemetryId.class).in(Singleton.class);
    bind(VersionScoringService.class);

    bind(PullRequestCommentingHashBuilder.class);
    bind(PullRequestTask.class);
    bind(ManualPullRequestService.class);
    bind(RemediationBranchNamePrefixGenerator.class);

    // External dependency bindings (from com.sonatype.nexus packages)
    bind(com.sonatype.nexus.git.utils.VersionRemediationTitleGenerator.class);
    bind(com.sonatype.nexus.iq.location.discovery.LocationDiscoveryExecutor.class);
    bind(com.sonatype.nexus.iq.location.discovery.PositionDiscoveryExecutor.class);
    bind(com.sonatype.nexus.iq.manager.PullRequestExecutor.class);
  }

  /**
   * Provides a list of all HdsClient instances for dependency injection. This replaces Sisu's automatic collection of
   *
   * @Named HdsClient components.
   */
  @Provides
  @Singleton
  public List<HdsClient> provideHdsClients(
      HdsClient hdsClient,
      FirewallAuditHdsClient firewallAuditHdsClient,
      FirewallQuarantineHdsClient firewallQuarantineHdsClient,
      PingHdsClient pingHdsClient)
  {
    return List.of(hdsClient, firewallAuditHdsClient, firewallQuarantineHdsClient, pingHdsClient);
  }

  /**
   * Provides a list of all PullRequestPostCommentAction instances for dependency injection. This replaces Sisu's
   * automatic collection of @Named PullRequestPostCommentAction components.
   */
  @Provides
  @Singleton
  public List<PullRequestPostCommentAction> providePullRequestPostCommentActions(
      BitbucketCodeInsightsService bitbucketCodeInsightsService)
  {
    return List.of(bitbucketCodeInsightsService);
  }
}
