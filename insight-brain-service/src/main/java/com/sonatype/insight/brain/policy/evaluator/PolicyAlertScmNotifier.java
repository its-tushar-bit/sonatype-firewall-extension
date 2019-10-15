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

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.git.GitApiService;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.git.GitRepositoryInfo;
import com.sonatype.insight.brain.git.PullRequestFeatureCheck;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.nexus.git.utils.VersionRemediationTitleGenerator;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to send notifications of policy alerts to Source Code Management
 * systems like github
 */
public class PolicyAlertScmNotifier
{
  private static final Logger log = LoggerFactory.getLogger(PolicyAlertScmNotifier.class);

  private static final String VERSION_KEY = "version";

  private static final String BRANCH_PREFIX = "";

  private static final List<String> SUPPORTED_FORMATS = ImmutableList.of(ComponentIdentifier.FORMAT_MAVEN);

  private final PullRequestFeatureCheck pullRequestFeatureCheck;

  private final GitClientFactory gitClientFactory;

  private final GitApiService gitApiService;

  private final ApiComponentRemediationService remediationService;

  private final PolicyAlertSourceCodeOrganizer policyAlertSourceCodeOrganizer;

  private final VersionRemediationTitleGenerator versionRemediationTitleGenerator =
      new VersionRemediationTitleGenerator();

  private static final String STAGE_ID = null;

  private static final OwnerType OWNER_TYPE = OwnerType.APPLICATION;

  /**
   * notifier for sending to hosted git source control manager service
   *
   * @param pullRequestFeatureCheck        service to check if pull request feature is enabled
   * @param remediationService             service to lookup suggested remediations
   * @param policyAlertSourceCodeOrganizer service to aggregate policy alerts
   * @param gitClientFactory               factory to create a connection to git hosting service
   * @param gitApiService                  service to find git repository info for an application
   */
  @Inject
  public PolicyAlertScmNotifier(
      final PullRequestFeatureCheck pullRequestFeatureCheck,
      final ApiComponentRemediationService remediationService,
      final PolicyAlertSourceCodeOrganizer policyAlertSourceCodeOrganizer,
      final GitClientFactory gitClientFactory,
      final GitApiService gitApiService)
  {
    this.pullRequestFeatureCheck = pullRequestFeatureCheck;
    this.remediationService = remediationService;
    this.policyAlertSourceCodeOrganizer = policyAlertSourceCodeOrganizer;
    this.gitClientFactory = gitClientFactory;
    this.gitApiService = gitApiService;
  }

  /**
   * send a notification to git hosting service
   *
   * @param app                 application with policy notifications
   * @param policyNotifications policy notifications
   */
  public void sendNotifications(
      final Application app,
      final List<PolicyNotification> policyNotifications)
      throws IOException
  {
    // TODO remove this check for system property feature flag
    if (System.getProperty("enableScmNotification") == null) {
      return;
    }

    final GitRepositoryInfo gitRepositoryInfo =
        gitApiService.getGitRepositoryInfoForApplication(app.getId());

    if (!pullRequestFeatureCheck.isPullRequestFeatureSupported(
        app, gitRepositoryInfo)) {
      return;
    }

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

      final String branchName = getBranchName(entry.getKey(), getNextVersion(remediationOptions));
      if (isBranchOnServer(gitRepositoryInfo, branchName)) {
        log.debug("Branch already exists for remediation [{}]", branchName);
        continue;
      }

      // TODO invoke PR engine
      log.debug("Invoke PR engine to construct a PR for [{}]", entry.getKey());
    }
  }

  private boolean isBranchOnServer(
      final GitRepositoryInfo gitRepositoryInfo,
      final String branchName)
      throws IOException
  {
    return gitClientFactory.create(gitRepositoryInfo).isBranchOnServer(branchName);
  }

  private String getBranchName(
      final ComponentIdentifier componentIdentifier,
      final String nextVersion)
  {
    return versionRemediationTitleGenerator.generateBranchNameForVersionRemediation(
        BRANCH_PREFIX, componentIdentifier, nextVersion);
  }

  private String getNextVersion(final List<ApiVersionChangeOptionDTO> remediationOptions) {
    return remediationOptions.get(0).getData()
        .getComponent().componentIdentifier.getCoordinates().get(VERSION_KEY);
  }

  private boolean isFormatSupported(final ComponentIdentifier componentIdentifier) {
    return SUPPORTED_FORMATS.contains(componentIdentifier.getFormat());
  }

  private List<ApiVersionChangeOptionDTO> getRemediationList(
      final ComponentIdentifier componentIdentifier, final String ownerId)
  {
    final ApiComponentDTOV2 componentDto = new ApiComponentDTOV2();
    componentDto.componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);

    return remediationService.getSuggestedRemediationForComponent(
        componentDto, OWNER_TYPE, ownerId, STAGE_ID).remediation.versionChanges;
  }
}
