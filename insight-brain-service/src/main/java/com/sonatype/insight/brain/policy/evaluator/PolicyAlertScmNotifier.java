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
import com.sonatype.insight.brain.git.PullRequestCommentingRemediationService;
import com.sonatype.insight.brain.git.PullRequestRemediationService;
import com.sonatype.insight.brain.git.RemediationBranchNamePrefixGenerator;
import com.sonatype.insight.brain.git.RemediationPullRequestFeatureCheck;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
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

  @VisibleForTesting
  PullRequestInvoker pullRequestInvoker = new PullRequestInvoker();

  /**
   * notifier for sending to hosted git source control manager service
   *
   * @param remediationPullRequestFeatureCheck service to check if pull request feature is enabled
   * @param remediationService                 service to lookup suggested remediations
   * @param policyAlertSourceCodeOrganizer     service to aggregate policy alerts
   * @param organizationDAO
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
      final ShutdownHandler shutdownHandler)
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

    if (Stage.ID_DEVELOP.equals(stage.getStageTypeId())) {
      log.debug("Ignoring Pull Request notification for the 'develop' stage for application '{}' and scan '{}'",
          app.getPublicId(), scanId);
      return;
    }

    if (!remediationPullRequestFeatureCheck.isPullRequestFeatureSupported(app, gitRepositoryInfo)) {
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
      if (!isFormatSupported(entry.getKey().getFormat())) {
        log.debug("Format '{}' is not supported for automatic remediation", entry.getKey());
        continue;
      }

      Optional<RemediationVersionDTO> remediationVersion =
          remediationService.getRemediationVersion(entry.getKey(), app.getId());

      if (remediationVersion.isPresent()) {
        String nextVersion = remediationVersion.get().getVersion();
        ApiVersionChangeOptionType remediationType = remediationVersion.get().getRemediationType();
        String stringRemediationType = remediationType.toString();
        final String branchName = getBranchName(app, entry.getKey(), nextVersion);

        if (!sourceControlEventPublisher.doesRemediationEventExistForBranch(app.getId(), branchName)) {
          PullRequestRemediationDetails pullRequestRemediationDetails =
              new PullRequestRemediationDetails(entry.getKey(), nextVersion, stringRemediationType,
                  remediationVersion.get().getBreakingChangesCount(), branchName, entry.getValue(), app,
                  scanId, stage.getStageTypeId(), baseUrl.getConfigured(), gitRepositoryInfo.provider,
                  gitRepositoryInfo.normalizedRepositoryUrl, organizationDAO);
          publishRemediationPullRequestEvent(pullRequestRemediationDetails);
        }
      }
      else {
        log.debug("No remediation options found for component [{}]", entry.getKey());
      }
    }
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
      shutdownHandler.add(scmNotificationThread, 3);
      scmNotificationThread.start();
    }
  }
}
