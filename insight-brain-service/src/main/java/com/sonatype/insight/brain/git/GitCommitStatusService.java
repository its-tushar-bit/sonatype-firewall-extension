/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.Status;
import com.sonatype.nexus.scm.api.model.StatusRequest;

import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles all the logic to create a <strong>Commit Status</strong>
 * <p>
 * With this commit status we can tell the SCM if a particular commit is safe to merge giving a policy
 * evaluation result, by tagging it with a proper state and a description.
 */
@Named
@Singleton
public class GitCommitStatusService
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(GitCommitStatusService.class);

  private final GitClientFactory gitClientFactory;

  private final IqForScmLicenseChecker licenseChecker;

  private final SourceControlUtils sourceControlUtils;

  private final SourceControlEventPublisher sourceControlEventPublisher;

  private final AsyncEventBus asyncEventBus;

  private final ScmStatusHelper scmStatusHelper;

  private final ApiSourceControlService apiSourceControlService;

  @Inject
  public GitCommitStatusService(
      final SourceControlUtils sourceControlUtils,
      final GitClientFactory gitClientFactory,
      final IqForScmLicenseChecker licenseChecker,
      final SourceControlEventPublisher sourceControlEventPublisher,
      final AsyncEventBus asyncEventBus,
      final ScmStatusHelper scmStatusHelper,
      final ApiSourceControlService apiSourceControlService)
  {
    this.gitClientFactory = gitClientFactory;
    this.licenseChecker = licenseChecker;
    this.sourceControlUtils = sourceControlUtils;
    this.scmStatusHelper = scmStatusHelper;
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.asyncEventBus = asyncEventBus;
    this.apiSourceControlService = apiSourceControlService;
  }

  @Subscribe
  public void onApplicationEvaluation(final ApplicationEvaluationEvent event) {
    if (!licenseChecker.isCommitStatusSupported()) {
      log.debug("License does not support source control notification feature");
      return;
    }

    if (Strings.isNullOrEmpty(event.commitHash)) {
      return;
    }

    SourceControl sourceControl = apiSourceControlService.getCompositeSourceControlByOwnerDecrypted(event.ownerId);
    if (Boolean.FALSE.equals(sourceControl.getCommitStatusEnabled())) {
      log.debug("Source control commit status notification feature is disabled");
      return;
    }

    GitRepositoryInfo gitRepositoryInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(sourceControl, event.ownerId);

    if (null == gitRepositoryInfo || null == gitRepositoryInfo.provider ||
        Strings.isNullOrEmpty(gitRepositoryInfo.token))
    {
      log.debug("The git repository information could not be found for application with id {}. " +
          "scm status could not be created.", event.ownerId);
      return;
    }

    sourceControlEventPublisher.publishEvent(
        new SourceControlEvent()
            .forStatusUpdate()
            .setEventPriority(SourceControlEvent.EVENT_PRIORITY_HIGHER)
            .setApplicationId(event.ownerId)
            .setPolicyEvaluationId(event.policyEvaluationId)
            .setPolicyEvaluationOutcome(event.outcome)
            .setCommitHash(event.commitHash)
            .setScanId(event.reportId)
            .withComponentCounts(event.criticalComponentCount, event.severeComponentCount, event.moderateComponentCount)
            .setInitiator(event.initiator)
            .setStageTypeId(event.stageTypeId));
  }

  public void onSendCommitStatus(SourceControlEvent event) {
    GitRepositoryInfo gitRepositoryInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(event.getApplicationId());

    if (null == gitRepositoryInfo || null == gitRepositoryInfo.provider ||
        Strings.isNullOrEmpty(gitRepositoryInfo.token))
    {
      log.debug("The git repository information could not be found for application with id {}, " +
          "scm status could not be created.", event.getApplicationId());
      return;
    }

    GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepositoryInfo);

    StatusRequest statusRequest = scmStatusHelper.createStatusRequestFromSourceControlEvent(event,
        gitApiClient, gitRepositoryInfo.provider);

    log.debug("Creating a {} commit status for repository: {}, commit hash: {}, with outcome: {}, state: {}",
        gitRepositoryInfo.provider, gitApiClient.getProjectUrl().getUrl(),
        event.getCommitHash(), event.getPolicyEvaluationOutcome(), statusRequest.getState());
    try {
      Status status = gitApiClient.createStatus(event.getCommitHash(), statusRequest);
      log.info(
          "Commit status sent for repository: {}, commit hash: {}, evaluation outcome: {}, state: {}, response: {}",
          gitApiClient.getProjectUrl().getUrl(), event.getCommitHash(), event.getPolicyEvaluationOutcome(),
          statusRequest.getState(), status);
    }
    catch (Exception e) {
      String message = String.format(
          "Failed to update status for applicationId: %s, repository: %s, commitHash: %s, " +
              "triggered by policyEvaluationId: %s, reason: %s",
          event.getApplicationId(), gitRepositoryInfo.normalizedRepositoryUrl, event.getCommitHash(),
          event.getPolicyEvaluationId(), e.getMessage());
      throw new SourceControlException(message, e);
    }
  }

  @Override
  public void start() throws Exception {
    asyncEventBus.register(this);
  }

  @Override
  public void stop() throws Exception {
    asyncEventBus.unregister(this);
  }
}
