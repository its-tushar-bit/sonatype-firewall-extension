/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.git.PullRequestCommentingRemediationService;
import com.sonatype.insight.brain.git.PullRequestRemediationService;
import com.sonatype.insight.brain.git.RemediationBranchNamePrefixGenerator;
import com.sonatype.insight.brain.git.RemediationPullRequestFeatureCheck;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.tenancy.TenantAwareOneTimeRunnable;
import com.sonatype.nexus.git.utils.VersionRemediationTitleGenerator;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to send notifications of policy alerts to Source Code Management
 * systems like github
 */
@Named
@Singleton
public class PolicyAlertScmNotifier
{
  private static final Logger log = LoggerFactory.getLogger(PolicyAlertScmNotifier.class);

  private static final String POLICY_ALERT = "policy alert";

  private final RemediationPullRequestFeatureCheck remediationPullRequestFeatureCheck;

  private final PullRequestCommentingRemediationService remediationService;

  private final PolicyAlertSourceCodeOrganizer policyAlertSourceCodeOrganizer;

  private final RemediationBranchNamePrefixGenerator remediationBranchNamePrefixGenerator =
      new RemediationBranchNamePrefixGenerator();

  private final VersionRemediationTitleGenerator versionRemediationTitleGenerator =
      new VersionRemediationTitleGenerator();

  private final BaseUrl baseUrl;

  private final SourceControlUtils sourceControlUtils;

  private final PullRequestRemediationService pullRequestRemediationService;

  private final SourceControlEventPublisher sourceControlEventPublisher;

  private final OrganizationDAO organizationDAO;

  private final ShutdownHandler shutdownHandler;

  private final StageTypeService stageTypeService;

  private final FeaturesService featuresService;

  @VisibleForTesting
  PullRequestInvoker pullRequestInvoker = new PullRequestInvoker();

  /**
   * notifier for sending to hosted git source control manager service
   *
   * @param remediationPullRequestFeatureCheck service to check if pull request feature is enabled
   * @param remediationService                 service to lookup suggested remediations
   * @param policyAlertSourceCodeOrganizer     service to aggregate policy alerts
   * @param organizationDAO
   * @param featuresService
   */
  @Inject
  public PolicyAlertScmNotifier(
      final RemediationPullRequestFeatureCheck remediationPullRequestFeatureCheck,
      final PullRequestCommentingRemediationService remediationService,
      final PolicyAlertSourceCodeOrganizer policyAlertSourceCodeOrganizer,
      final BaseUrl baseUrl,
      final SourceControlUtils sourceControlUtils,
      final PullRequestRemediationService pullRequestRemediationService,
      final SourceControlEventPublisher sourceControlEventPublisher,
      final OrganizationDAO organizationDAO,
      final ShutdownHandler shutdownHandler,
      final FeaturesService featuresService,
      final StageTypeService stageTypeService)
  {
    this.remediationPullRequestFeatureCheck = remediationPullRequestFeatureCheck;
    this.remediationService = remediationService;
    this.policyAlertSourceCodeOrganizer = policyAlertSourceCodeOrganizer;
    this.baseUrl = baseUrl;
    this.sourceControlUtils = sourceControlUtils;
    this.pullRequestRemediationService = pullRequestRemediationService;
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.organizationDAO = organizationDAO;
    this.shutdownHandler = shutdownHandler;
    this.stageTypeService = stageTypeService;
    this.featuresService = featuresService;
  }

  /**
   * send a notification to git hosting service
   *
   * @param app                 application with policy notifications
   * @param policyNotifications policy notifications
   */
  public void sendNotifications(
      final Application app,
      final String scanId,
      final Stage stage,
      final List<PolicyNotification> policyNotifications)
  {
    final GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(app.getId());

    if (stageTypeService.getLicensedStageTypes(StageTypeService.LIFECYCLE_CONTEXT).stream()
            .noneMatch(stageType -> stageType.getId().equals(stage.getStageTypeId()))
            || Stage.ID_DEVELOP.equals(stage.getStageTypeId())) {
      log.debug("Ignoring Pull Request notification for the stage '{}' for application '{}' and scan '{}'",
          stage.getStageTypeId(), app.getPublicId(), scanId);
      return;
    }

    if (!remediationPullRequestFeatureCheck.isPullRequestFeatureSupported(app, gitRepositoryInfo)) {
      log.debug("Pull Request feature is not supported for application '{}' and scan '{}'", app.getPublicId(), scanId);
      return;
    }

    pullRequestInvoker.execute(scanId, () -> {
      try {
        internalSendNotification(app, scanId, stage, policyNotifications, gitRepositoryInfo);
      }
      catch (final Exception e) {
        log.error("Unable to send PullRequest notification for application {} and scan {} in stage {}",
            app.getPublicId(), scanId, stage, e);
      }
    });
  }

  private void internalSendNotification(final Application app,
                                        final String scanId,
                                        final Stage stage,
                                        final List<PolicyNotification> policyNotifications,
                                        final GitRepositoryInfo gitRepositoryInfo) throws IOException
  {
    // aggregate by component and loop each one
    Map<ComponentIdentifier, List<PolicyNotification>> sortedComponentAlerts =
        policyAlertSourceCodeOrganizer.getNotificationsForScm(policyNotifications);

    for (Map.Entry<ComponentIdentifier, List<PolicyNotification>> entry : sortedComponentAlerts.entrySet()) {
      final ComponentIdentifier componentIdentifier = entry.getKey();
      if (!isFormatSupported(componentIdentifier.getFormat())) {
        log.debug("Format '{}' is not supported for automatic remediation", componentIdentifier);
        continue;
      }

      Optional<RemediationVersionDTO> remediationVersion =
          remediationService.getRemediationVersion(componentIdentifier, app.getId());

      if (remediationVersion.isPresent()) {
        /*
        A 'non-breaking with dependencies versions PR' (aka 'Golden PR') is a PR made with remediation versions of type
        RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES only.
        A 'regular' PR is a PR made with remediation versions of all other types.

        A Golden PR is created for Maven components if the 'developerSuggestNonBreakingVersion' feature flag is enabled
        and if a non-breaking with dependencies version (aka 'Golden version') is available.

        If the component is a Maven component and the feature flag is enabled, but there is no Golden version
        available, no PR is created.

        A regular automated remediation PR is created for non-Maven components or when the feature flag is not enabled.
         */
        if (shouldCreateNonBreakingVersionsPR(componentIdentifier)) {
          final ApiVersionChangeOptionType remediationType = remediationVersion.get().getRemediationType();
          if (!ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES.equals(remediationType)) {
            log.debug("Remediation type for component '{}' is not golden: {}", componentIdentifier, remediationType);
            continue;
          }
          log.debug("Attempt to create golden PR for application '{}' component '{}'",
              app.getPublicId(), componentIdentifier);
        }
        else {
          log.debug("Attempt to create PR for application '{}' component '{}'",
              app.getPublicId(), componentIdentifier);
        }

        String nextVersion = remediationVersion.get().getVersion();
        ApiVersionChangeOptionType remediationType = remediationVersion.get().getRemediationType();
        String stringRemediationType = remediationType.getDisplayName();
        final String branchName = getBranchName(app, componentIdentifier, nextVersion);

        if (!sourceControlEventPublisher.doesRemediationEventExistForBranch(app.getId(), branchName)) {
          final List<PolicyNotification> notifications = entry.getValue();
          PullRequestRemediationDetails pullRequestRemediationDetails =
              new PullRequestRemediationDetails(componentIdentifier, nextVersion, stringRemediationType,
                  remediationVersion.get().getBreakingChangesCount(), branchName, notifications, app,
                  scanId, stage.getStageTypeId(), baseUrl.getConfigured(), gitRepositoryInfo.provider,
                  gitRepositoryInfo.normalizedRepositoryUrl, organizationDAO);
          publishRemediationPullRequestEvent(pullRequestRemediationDetails);
        }
        else {
          log.debug("Remediation pull request already exists for application '{}' component '{}'",
              app.getPublicId(), ComponentDisplayNameUtil.fromIdentifier(componentIdentifier));
        }
      }
      else {
        log.debug("No remediation options found for component [{}]", componentIdentifier);
      }
    }
  }

  private boolean shouldCreateNonBreakingVersionsPR(final ComponentIdentifier componentIdentifier) {
    return componentIdentifier.isMaven() && featuresService.getFeatures()
        .contains(SystemConfigurationPropertyFeature.DEVELOPER_SUGGEST_NON_BREAKING_VERSION);
  }

  private void publishRemediationPullRequestEvent(PullRequestRemediationDetails pullRequestRemediationDetails) {
    SourceControlEvent event = new SourceControlEvent()
        .forRemediationPullRequest()
        .withComponentIdentifier(pullRequestRemediationDetails.getToBeRemediated())
        .setApplicationId(pullRequestRemediationDetails.getApp().getId())
        .setRemediationVersion(pullRequestRemediationDetails.getRemediatedVersion())
        .setScanId(pullRequestRemediationDetails.getScanId())
        .setStageTypeId(pullRequestRemediationDetails.getStage())
        .setBranchName(pullRequestRemediationDetails.getPullRequestBranchName())
        .setPullRequestContents(pullRequestRemediationDetails.getContents())
        .setInitiator(POLICY_ALERT);

    sourceControlEventPublisher.publishEvent(event);

    log.info("Sent remediation pull request event for application '{}' component '{}'",
        pullRequestRemediationDetails.getApp().getId(),
        ComponentDisplayNameUtil.fromIdentifier(pullRequestRemediationDetails.getToBeRemediated()));
  }

  private String getBranchName(
      final Application application,
      final ComponentIdentifier componentIdentifier,
      final String nextVersion)
  {
    String branchPrefix = remediationBranchNamePrefixGenerator.generatePrefixForApplication(application.getId());
    return versionRemediationTitleGenerator.generateBranchNameForVersionRemediation(
        branchPrefix, componentIdentifier, nextVersion);
  }

  private boolean isFormatSupported(final String format) {
    return pullRequestRemediationService.isFormatSupportedForPullRequestRemediation(format);
  }

  /**
   * Invoke the PR runnable in a named thread. Package-private to allow for mocking in tests.
   */
  class PullRequestInvoker
  {
    public void execute(final String scanId, Runnable runnable) {
      Thread scmNotificationThread =
          new Thread(new TenantAwareOneTimeRunnable(runnable), "PolicyAlertScmNotifierForScan-" + scanId);
      shutdownHandler.add(scmNotificationThread, ShutdownPriority.NOTIFICATIONS);
      scmNotificationThread.start();
    }
  }
}
