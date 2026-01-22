/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.Status;
import com.sonatype.nexus.scm.api.model.StatusRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles all the logic to create a <strong>Pull Request Status</strong>
 * <p>
 * A <strong>Pull Request Status</strong> is similar to a Commit Status, but is not the same. They
 * use different APIs. <strong>Pull Request Status creation is only supported by Azure DevOps</strong>
 * <p>
 * With this pull request status we can tell the SCM if a particular pull request is safe to merge
 * giving a policy evaluation result, by tagging it with a proper state and a description.
 */
@Named
@Singleton
public class PullRequestStatusService
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestStatusService.class);

  private final GitClientFactory gitClientFactory;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final ScmStatusHelper scmStatusHelper;

  @Inject
  public PullRequestStatusService(
      final GitClientFactory gitClientFactory,
      final ScanPolicyEvaluator scanPolicyEvaluator,
      final ScmStatusHelper scmStatusHelper)
  {
    this.gitClientFactory = gitClientFactory;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.scmStatusHelper = scmStatusHelper;
  }

  public void doCreatePullRequestStatus(PullRequestPolicyEvaluationsDTO dto) {
    SourceControlProvider provider = dto.getGitRepositoryInfo().getProvider();
    if (!provider.supportsPullRequestStatusCreation()) {
      return;
    }

    // Getting policy evaluation result to generate proper message for status description
    PolicyEvaluation featureBranchPolicyEvaluation = dto.getFeatureBranchPolicyEvaluation();
    PolicyEvaluationResult policyEvaluationResult = scanPolicyEvaluator.createPolicyEvaluationResult(
        dto.getFeatureBranchPolicyEvaluation(), true);
    createPullRequestStatus(dto.getGitRepositoryInfo(), dto.getPullRequestNumber(),
        featureBranchPolicyEvaluation, policyEvaluationResult);
  }

  private void createPullRequestStatus(
      final GitRepositoryInfo gitRepositoryInfo,
      final Integer pullRequestId,
      final PolicyEvaluation policyEvaluation,
      final PolicyEvaluationResult policyEvaluationResult)
  {
    GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepositoryInfo);
    StatusRequest statusRequest = scmStatusHelper.createStatusRequestFromPolicyEvaluation(
        policyEvaluation,
        policyEvaluationResult,
        gitApiClient,
        gitRepositoryInfo.getProvider());

    try {
      Status status = gitApiClient.createPullRequestStatus(pullRequestId, statusRequest);
      log.info(
          "Pull request status sent for repository: {}, pull request: {} state: {}, response: {}",
          gitRepositoryInfo.normalizedRepositoryUrl, pullRequestId, statusRequest.getState(), status
      );
    }
    catch (IOException e) {
      String message = String.format(
          "Failed to update pull request status for repository: %s, pull request Id: %s reason: %s",
          gitRepositoryInfo.normalizedRepositoryUrl, pullRequestId, e.getMessage()
      );
      throw new SourceControlException(message, e);
    }
  }
}
