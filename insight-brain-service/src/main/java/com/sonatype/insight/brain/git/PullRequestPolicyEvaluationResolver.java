/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.scm.api.model.Commit;
import com.sonatype.nexus.scm.api.model.CommitInformation;
import com.sonatype.nexus.scm.api.model.PullRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class PullRequestPolicyEvaluationResolver
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestPolicyEvaluationResolver.class);

  private final GitCommitHistoryService gitCommitHistoryService;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PullRequestEligibilityValidator pullRequestEligibilityValidator;

  private final PullRequestInfoClient pullRequestInfoClient;

  @Inject
  public PullRequestPolicyEvaluationResolver(
      GitCommitHistoryService gitCommitHistoryService,
      PolicyEvaluationDAO policyEvaluationDAO,
      PullRequestEligibilityValidator pullRequestEligibilityValidator,
      PullRequestInfoClient pullRequestInfoClient)
  {
    this.gitCommitHistoryService = gitCommitHistoryService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.pullRequestEligibilityValidator = pullRequestEligibilityValidator;
    this.pullRequestInfoClient = pullRequestInfoClient;
  }

  /**
   * given a policy evaluation for an application this method determines if there are any open, internal/private pull
   * requests for the commit associated with that policy evaluation;  if there are, this method then tries to determine
   * the policy evaluations to associate with each one, if able;  in some cases, we can/will generate those policy
   * evaluations;
   *
   * @return list of zero or more #PullRequestPolicyEvaluationsDTO objects that represent any pull requests that
   * successfully resolved
   */
  public List<PullRequestPolicyEvaluationsDTO> resolveForPolicyEvaluation(
      String applicationId,
      GitRepositoryInfo gitRepositoryInfo,
      String featureBranchPolicyEvaluationId,
      String commitHash)
  {
    List<PullRequestPolicyEvaluationsDTO> pullRequestPolicyEvaluationsResults = new ArrayList<>();

    PolicyEvaluation featureBranchPolicyEvaluation = policyEvaluationDAO.getById(featureBranchPolicyEvaluationId);
    if (null == featureBranchPolicyEvaluation) {
      return pullRequestPolicyEvaluationsResults;
    }

    CommitInformation commitInfo = pullRequestInfoClient.getCommitInfoFromScm(gitRepositoryInfo, commitHash);

    // the commit info contains not only the pull requests associated with the commit but also some recent
    // commit history for the base branch
    processDefaultBranchCommitHistory(featureBranchPolicyEvaluation, commitInfo.getCommits());

    for (PullRequest pullRequest : commitInfo.getPullRequests()) {
      if (pullRequestEligibilityValidator
          .isPullRequestEligibleForCommenting(applicationId, pullRequest, gitRepositoryInfo,
              featureBranchPolicyEvaluation)) {

        Optional<PolicyEvaluation> defaultBranchPolicyEvaluation =
            getLatestPolicyEvaluationReportForBaseBranch(applicationId);

        if (defaultBranchPolicyEvaluation.isPresent()) {
          PullRequestPolicyEvaluationsDTO dto = new PullRequestPolicyEvaluationsDTO()
              .setApplicationId(applicationId)
              .setFeatureBranchName(pullRequest.getHead())
              .setDefaultBranchPolicyEvaluation(defaultBranchPolicyEvaluation.get())
              .setFeatureBranchPolicyEvaluation(featureBranchPolicyEvaluation)
              .setGitRepositoryInfo(gitRepositoryInfo)
              .setPullRequestHeadCommit(pullRequest.getHeadCommitHash())
              .setPullRequestNumber(pullRequest.getNumber());
          pullRequestPolicyEvaluationsResults.add(dto);
        }
        else {
          log.warn(
              "no policy evaluation for base branch, skipping PR commenting for application '{}' pull request '{}'",
              applicationId, pullRequest.getNumber());
        }
      }
    }

    return pullRequestPolicyEvaluationsResults;
  }

  /**
   * given a particular open, internal/private pull request, determines the policy evaluations that should be associated
   * with the head and target branches/commits for that PR;  in some cases we can/will generate those policy evaluations
   *
   * @return a list with zero or one #PullRequestPolicyEvaluationsDTO entries, depending on whether or not we were
   * able to resolve the policy evaluations to use
   */
  public PullRequestPolicyEvaluationsDTO resolveForPullRequest(
      String applicationId,
      GitRepositoryInfo gitRepositoryInfo,
      int pullRequestNumber,
      String featureBranchName,
      String pullRequestHeadCommitHash)
  {
    PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO = null;

    PolicyEvaluation featureBranchPolicyEvaluation =
        policyEvaluationDAO.getLastByApplicationAndCommitHash(applicationId, pullRequestHeadCommitHash);

    if (null == featureBranchPolicyEvaluation) {
      return null;
    }

    Optional<PolicyEvaluation> defaultBranchPolicyEvaluation =
        getLatestPolicyEvaluationReportForBaseBranch(applicationId);

    if (!defaultBranchPolicyEvaluation.isPresent()) {
      // we need to get and process the base branch commit history
      CommitInformation commitInfo =
          pullRequestInfoClient.getCommitInfoFromScm(gitRepositoryInfo, pullRequestHeadCommitHash);

      // the commit info contains not only the pull requests associated with the commit but also some recent commit
      // history for the base branch
      processDefaultBranchCommitHistory(featureBranchPolicyEvaluation, commitInfo.getCommits());
      defaultBranchPolicyEvaluation = getLatestPolicyEvaluationReportForBaseBranch(applicationId);
    }

    if (defaultBranchPolicyEvaluation.isPresent()) {
      pullRequestPolicyEvaluationsDTO = new PullRequestPolicyEvaluationsDTO()
          .setApplicationId(applicationId)
          .setFeatureBranchName(featureBranchName)
          .setDefaultBranchPolicyEvaluation(defaultBranchPolicyEvaluation.get())
          .setFeatureBranchPolicyEvaluation(featureBranchPolicyEvaluation)
          .setGitRepositoryInfo(gitRepositoryInfo)
          .setPullRequestHeadCommit(pullRequestHeadCommitHash)
          .setPullRequestNumber(pullRequestNumber);
    }
    else {
      log.warn(
          "no policy evaluation for base branch, skipping PR commenting for application '{}' pull request '{}'",
          applicationId, pullRequestNumber);
    }

    return pullRequestPolicyEvaluationsDTO;
  }

  private Optional<PolicyEvaluation> getLatestPolicyEvaluationReportForBaseBranch(String applicationId) {
    return gitCommitHistoryService.getLatestPolicyEvaluationForApplicationBaseBranch(applicationId);
  }

  private void processDefaultBranchCommitHistory(
      PolicyEvaluation policyEvaluation,
      List<Commit> commits)
  {
    String applicationId = policyEvaluation.getApplicationId();
    // this call is for the specific policy eval that was run and if it happened to be for the base branch then
    // the associated commit history will be updated
    gitCommitHistoryService.updateCommitHistoryForPolicyEvaluation(policyEvaluation);

    // this call is for the list of base branch commits we got back from SCM
    gitCommitHistoryService.updateCommitHistoryForCommits(applicationId, commits);
    log.debug("{} base branch commits to process for application '{}'", commits.size(), applicationId);
  }
}
