/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;

import com.google.common.eventbus.Subscribe;
import io.dropwizard.lifecycle.Managed;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class PullRequestCommentingEventHandler
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestCommentingEventHandler.class);

  /**
   * Policy violations with a threat level below this threshold are filleted out of the policy violation diff
   */
  public static final int MINIMUM_THREAT_LEVEL = 2;

  private final PullRequestCommentingService pullRequestCommentingService;

  private final SourceControlUtils sourceControlUtils;

  private final SourceControlEventPublisher sourceControlEventPublisher;

  private final AsyncEventBus asyncEventBus;

  private final IqForScmLicenseChecker licenseChecker;

  private final PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PullRequestStatusService pullRequestStatusService;

  private final PullRequestCommentingEligibilityValidator pullRequestCommentingEligibilityValidator;

  private final GitCommitHistoryService gitCommitHistoryService;

  @Inject
  public PullRequestCommentingEventHandler(
      final PullRequestCommentingService pullRequestCommentingService,
      final SourceControlUtils sourceControlUtils,
      final SourceControlEventPublisher sourceControlEventPublisher,
      final AsyncEventBus asyncEventBus,
      final IqForScmLicenseChecker licenseChecker,
      final PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final PullRequestStatusService pullRequestStatusService,
      final PullRequestCommentingEligibilityValidator pullRequestCommentingEligibilityValidator,
      final GitCommitHistoryService gitCommitHistoryService)
  {
    this.pullRequestCommentingService = pullRequestCommentingService;
    this.sourceControlUtils = sourceControlUtils;
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.asyncEventBus = asyncEventBus;
    this.licenseChecker = licenseChecker;
    this.pullRequestPolicyEvaluationResolver = pullRequestPolicyEvaluationResolver;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.pullRequestStatusService = pullRequestStatusService;
    this.pullRequestCommentingEligibilityValidator = pullRequestCommentingEligibilityValidator;
    this.gitCommitHistoryService = gitCommitHistoryService;
  }

  @Override
  public void start() throws Exception {
    asyncEventBus.register(this);
  }

  @Override
  public void stop() throws Exception {
    asyncEventBus.unregister(this);
  }

  /**
   * This method is for the 'immediate flow' for pull request commenting of policy violation diffs between the
   * development branch commit that triggered the policy evaluation (which then issued this event) and the most recently
   * available policy evaluation for the source control configured base branch for the associated application.
   *
   * @param event ApplicationEvaluation event that triggered this call
   */
  @Subscribe
  public void onApplicationEvaluation(ApplicationEvaluationEvent event) {
    if (!licenseChecker.isPullRequestCommentingSupported()) {
      log.debug("License does not support source control automation feature");
      return;
    }

    if (eventHasCommitHashAndScmIsEnabled(event)) {
      String applicationId = event.ownerId;
      GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);

      if (!pullRequestCommentingEligibilityValidator.isPullRequestCommentingEnabled(gitRepositoryInfo)) {
        return;
      }

      if (!gitRepositoryInfo.provider.supportsPullRequestCommenting() ||
          sourceControlUtils.isBitbucketCloud(gitRepositoryInfo))
      {
        log.debug("'{}' not currently supported for pull request commenting", gitRepositoryInfo.provider.toString());
      }
      else if (wasPolicyEvalInternallyTriggered(event.policyEvaluationId)) {
        log.debug(
            "Ignoring ApplicationEvaluationEvent for application {} because the policy evaluation {} was " +
                "internally triggered",
            applicationId, event.policyEvaluationId);
        // but we want to update the default branch commit history with the new eval. ID, if that's the case
        gitCommitHistoryService.updateCommitHistoryForPolicyEvaluation(event.policyEvaluationId);
      }
      else {
        SourceControlEvent sourceControlEvent = new SourceControlEvent()
            .forApplicationEvaluation()
            .setApplicationId(applicationId)
            .setCommitHash(event.commitHash)
            .setPolicyEvaluationId(event.policyEvaluationId)
            .setInitiator(event.initiator)
            .setCreateTime(new Date());

        sourceControlEventPublisher.publishEvent(sourceControlEvent);
        log.debug("Persisted source control event '{}' for application '{}' and commit '{}'",
            sourceControlEvent.getEventType(), applicationId, event.commitHash);
      }
    }
  }

  /**
   * We don't process pull requests for internally triggered policy evaluations - we let polling take care of that
   */
  private boolean wasPolicyEvalInternallyTriggered(String policyEvaluationId) {
    PolicyEvaluation possibleFeatureBranchPolicyEvaluation = policyEvaluationDAO.getById(policyEvaluationId);

    return null != possibleFeatureBranchPolicyEvaluation
        && possibleFeatureBranchPolicyEvaluation.wasInternallyTriggered();
  }

  public void onApplicationEvaluation(SourceControlEvent event) {
    String applicationId = event.getApplicationId();
    GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);

    List<PullRequestPolicyEvaluationsDTO> pullRequestPolicyEvaluationsDTOs =
        pullRequestPolicyEvaluationResolver.resolveForPolicyEvaluation(applicationId, gitRepositoryInfo,
            event.getPolicyEvaluationId(), event.getCommitHash());

    for (PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO : pullRequestPolicyEvaluationsDTOs) {
      pullRequestCommentingService.doCreateOrUpdatePullRequestComment(pullRequestPolicyEvaluationsDTO);
      // triggering logic for pull request status creation
      pullRequestStatusService.doCreatePullRequestStatus(pullRequestPolicyEvaluationsDTO);
    }
  }

  public void onDiscoveredPullRequest(SourceControlEvent event) {
    String applicationId = event.getApplicationId();
    GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);

    PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO = pullRequestPolicyEvaluationResolver
        .resolveForPullRequest(applicationId, gitRepositoryInfo, event.getPullRequestNumber(), event.getBranchName(),
            event.getBaseBranchName(), event.getCommitHash(), event.getBaseCommitHash());

    if (null != pullRequestPolicyEvaluationsDTO) {
      pullRequestCommentingService.doCreateOrUpdatePullRequestComment(pullRequestPolicyEvaluationsDTO);
      // triggering logic for pull request status creation
      pullRequestStatusService.doCreatePullRequestStatus(pullRequestPolicyEvaluationsDTO);
    }
  }

  public void onUpdatedPullRequest(SourceControlEvent event) {
    String applicationId = event.getApplicationId();
    GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);

    PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO =
        pullRequestPolicyEvaluationResolver.resolveForPullRequest(applicationId, gitRepositoryInfo,
            event.getPullRequestNumber(), event.getBranchName(), event.getBaseBranchName(),
            event.getCommitHash(), event.getBaseCommitHash());

    if (pullRequestPolicyEvaluationsDTO == null) {
      return;
    }

    if (!pullRequestPolicyEvaluationsDTO.getTargetPolicyEvaluation().wasInternallyTriggered()
        || !pullRequestPolicyEvaluationsDTO.getFeatureBranchPolicyEvaluation().wasInternallyTriggered())
    {
      // There is at least one policy evaluation triggered externally for this pull request.
      return;
    }

    pullRequestCommentingService.doCreateOrUpdatePullRequestComment(pullRequestPolicyEvaluationsDTO);
    // triggering logic for pull request status creation
    pullRequestStatusService.doCreatePullRequestStatus(pullRequestPolicyEvaluationsDTO);
  }

  private boolean eventHasCommitHashAndScmIsEnabled(ApplicationEvaluationEvent event) {
    boolean isOk = true;
    String applicationId = event.ownerId;
    if (StringUtils.isBlank(event.commitHash)) {
      log.debug(
          "no commit hash : skipping PR commenting for application '{}' with policy evaluation '{}'",
          applicationId,
          event.policyEvaluationId);
      isOk = false;
    }
    else if (!sourceControlUtils.isScmEnabled(applicationId)) {
      log.debug(
          "scm disabled : skipping PR commenting for application '{}' with policy evaluation '{}'",
          applicationId,
          event.policyEvaluationId);
      isOk = false;
    }
    return isOk;
  }
}
