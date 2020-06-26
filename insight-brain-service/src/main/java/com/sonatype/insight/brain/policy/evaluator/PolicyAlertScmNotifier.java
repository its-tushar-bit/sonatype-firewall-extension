/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.git.PullRequestFeatureCheck;
import com.sonatype.insight.brain.git.SourceControlTaskRunner;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
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

  public static final int APP_ID_BRANCH_TRUNCATE_INDEX = 6;

  private static final String VERSION_KEY = "version";

  private final PullRequestFeatureCheck pullRequestFeatureCheck;

  private final GitClientFactory gitClientFactory;

  private final ApiComponentRemediationService remediationService;

  private final PolicyAlertSourceCodeOrganizer policyAlertSourceCodeOrganizer;

  private final VersionRemediationTitleGenerator versionRemediationTitleGenerator =
      new VersionRemediationTitleGenerator();

  private final BaseUrl baseUrl;

  private static final String STAGE_ID = null;

  private static final OwnerType OWNER_TYPE = OwnerType.APPLICATION;

  private final SourceControlUtils sourceControlUtils;

  private final SourceControlTaskRunner sourceControlTaskRunner;

  @VisibleForTesting
  PullRequestInvoker pullRequestInvoker = new PullRequestInvoker();

  /**
   * notifier for sending to hosted git source control manager service
   *
   * @param pullRequestFeatureCheck        service to check if pull request feature is enabled
   * @param remediationService             service to lookup suggested remediations
   * @param policyAlertSourceCodeOrganizer service to aggregate policy alerts
   * @param gitClientFactory               factory to create a connection to git hosting service
   */
  @Inject
  public PolicyAlertScmNotifier(
      final PullRequestFeatureCheck pullRequestFeatureCheck,
      final ApiComponentRemediationService remediationService,
      final PolicyAlertSourceCodeOrganizer policyAlertSourceCodeOrganizer,
      final GitClientFactory gitClientFactory,
      final BaseUrl baseUrl,
      final SourceControlUtils sourceControlUtils,
      final SourceControlTaskRunner sourceControlTaskRunner)
  {
    this.pullRequestFeatureCheck = pullRequestFeatureCheck;
    this.remediationService = remediationService;
    this.policyAlertSourceCodeOrganizer = policyAlertSourceCodeOrganizer;
    this.gitClientFactory = gitClientFactory;
    this.baseUrl = baseUrl;
    this.sourceControlUtils = sourceControlUtils;
    this.sourceControlTaskRunner = sourceControlTaskRunner;
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

    if (!pullRequestFeatureCheck.isPullRequestFeatureSupported(app, gitRepositoryInfo)) {
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
      if (!isFormatSupported(entry.getKey())) {
        log.debug("Format '{}' is not supported for automatic remediation", entry.getKey());
        continue;
      }

      final List<ApiVersionChangeOptionDTO> remediationOptions = getRemediationList(entry.getKey(), app.getId());
      if (remediationOptions.isEmpty()) {
        log.debug("No remediation options found for component [{}]", entry.getKey());
        continue;
      }

      String nextVersion = getNextVersion(remediationOptions);
      final String branchName = getBranchName(app, entry.getKey(), nextVersion);

      if (isBranchOnServer(gitRepositoryInfo, branchName)) {
        log.info("Branch already exists on remote server for remediation [{}]", branchName);
        continue;
      }

      PullRequestRemediationDetails pullRequestRemediationDetails =
          new PullRequestRemediationDetails(entry.getKey(), nextVersion, branchName, entry.getValue(), app, scanId,
              stage.getStageTypeId(), baseUrl.getConfigured(), gitRepositoryInfo.provider);

      sourceControlTaskRunner.doPullRequestRemediation(pullRequestRemediationDetails);
    }
  }

  private boolean isBranchOnServer(
      final GitRepositoryInfo gitRepositoryInfo,
      final String branchName)
      throws IOException
  {
    return gitClientFactory.createApiClient(gitRepositoryInfo).isBranchOnServer(branchName);
  }

  private String getBranchName(
      final Application application,
      final ComponentIdentifier componentIdentifier,
      final String nextVersion)
  {
    String branchPrefix = application.getId().substring(0, APP_ID_BRANCH_TRUNCATE_INDEX);
    return versionRemediationTitleGenerator.generateBranchNameForVersionRemediation(
        branchPrefix, componentIdentifier, nextVersion);
  }

  private String getNextVersion(final List<ApiVersionChangeOptionDTO> remediationOptions) {
    return remediationOptions.get(0).getData()
        .getComponent().componentIdentifier.getCoordinates().get(VERSION_KEY);
  }

  private boolean isFormatSupported(final ComponentIdentifier componentIdentifier) {
    return sourceControlTaskRunner.isFormatSupportedForPullRequestRemediation(componentIdentifier);
  }

  private List<ApiVersionChangeOptionDTO> getRemediationList(
      final ComponentIdentifier componentIdentifier, final String ownerId)
  {
    final ApiComponentDTOV2 componentDto = new ApiComponentDTOV2();
    componentDto.componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);

    return remediationService.getSuggestedRemediationForComponentNoAuth(
        componentDto, OWNER_TYPE, ownerId, STAGE_ID, null, null).remediation.versionChanges;
  }

  /**
   * Invoke the PR runnable in a named thread. Package-private to allow for mocking in tests.
   */
  class PullRequestInvoker
  {
    public void execute(final String scanId, Runnable runnable) {
      new Thread(runnable, "PolicyAlertScmNotifierForScan-" + scanId).start();
    }
  }
}
