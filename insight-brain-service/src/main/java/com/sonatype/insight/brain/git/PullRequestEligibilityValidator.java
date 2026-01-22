/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.scm.api.model.PullRequest;
import com.sonatype.nexus.scm.api.model.PullRequestState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class PullRequestEligibilityValidator
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestEligibilityValidator.class);

  private final ScmRepoVisibilityService scmRepoVisibilityService;

  @Inject
  public PullRequestEligibilityValidator(ScmRepoVisibilityService scmRepoVisibilityService) {
    this.scmRepoVisibilityService = scmRepoVisibilityService;
  }

  /**
   * determines whether or not the given pull request is eligible for PR commenting
   *
   * @param applicationId IQ application the pull request relates to
   * @param pullRequest git #PullRequest to validate
   * @param gitRepositoryInfo info about the git repository; used to determine if the given pull request is for the
   *                          configured default branch
   * @param featureBranchPolicyEvaluation evaluation associated with the application;  this needs to be for the
   *                                     head commit for the PR in order to pass validation
   *
   * @return true if:
   *   (a) the PR is for a private or internal repository (or allowed via license flag)
   *   (b) the PR is open
   *   (c) the PR is not for the default branch
   *   (d) the given policy evaluation is for the head commit of the PR
   */
  public boolean isPullRequestEligibleForCommenting(
      String applicationId,
      PullRequest pullRequest,
      GitRepositoryInfo gitRepositoryInfo,
      PolicyEvaluation featureBranchPolicyEvaluation)
  {
    if (!scmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(gitRepositoryInfo)) {
      log.debug("Repository is not valid for pull requests, ensure that it is private or internal: {}",
          gitRepositoryInfo.getRepositoryUrl());
      return false;
    }

    if (!isPullRequestOpen(pullRequest)) {
      log.debug("application '{}' pull request '{}' state '{}' is not open", applicationId, pullRequest.getNumber(),
          pullRequest.getState());
      return false;
    }

    if (isPullRequestForBaseBranch(pullRequest, gitRepositoryInfo)) {
      log.debug("application '{}' pull request '{}' is for the default branch", applicationId, pullRequest.getNumber());
      return false;
    }

    if (!doesHeadCommitMatchPolicyEvaluationCommit(featureBranchPolicyEvaluation.getCommitHash(),
        pullRequest.getHeadCommitHash())) {
      log.debug(
          "The head commit hash '{}', for application '{}', PR '{}' does not match the commit on the policy " +
              "evaluation '{}'", pullRequest.getHeadCommitHash(), applicationId, pullRequest.getNumber(),
          featureBranchPolicyEvaluation.getCommitHash());
      return false;
    }

    return true;
  }

  private boolean doesHeadCommitMatchPolicyEvaluationCommit(String policyEvaluationCommitHash, String headCommitHash) {
    return policyEvaluationCommitHash.equals(headCommitHash);
  }

  private boolean isPullRequestOpen(final PullRequest pullRequest) {
    return PullRequestState.OPEN.equals(pullRequest.getState());
  }

  private boolean isPullRequestForBaseBranch(PullRequest pullRequest, GitRepositoryInfo gitRepositoryInfo) {
    return pullRequest.getHead().equalsIgnoreCase(gitRepositoryInfo.getBaseBranch());
  }
}
