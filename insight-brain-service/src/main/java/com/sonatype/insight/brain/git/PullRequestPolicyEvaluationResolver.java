/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.git.utils.api.GitException;
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

  private static final Stage DEVELOP_STAGE = new Stage(Stage.ID_DEVELOP);

  private final GitCommitHistoryService gitCommitHistoryService;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PullRequestDefaultBranchPolicyEvaluationResolver defaultBranchPolicyEvaluationResolver;

  private final PullRequestEligibilityValidator pullRequestEligibilityValidator;

  private final PullRequestInfoClient pullRequestInfoClient;

  private final SourceControlScanService sourceControlScanService;

  @Inject
  public PullRequestPolicyEvaluationResolver(
      GitCommitHistoryService gitCommitHistoryService,
      PolicyEvaluationDAO policyEvaluationDAO,
      PullRequestDefaultBranchPolicyEvaluationResolver defaultBranchPolicyEvaluationResolver,
      PullRequestEligibilityValidator pullRequestEligibilityValidator,
      PullRequestInfoClient pullRequestInfoClient,
      SourceControlScanService sourceControlScanService)
  {
    this.gitCommitHistoryService = gitCommitHistoryService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.defaultBranchPolicyEvaluationResolver = defaultBranchPolicyEvaluationResolver;
    this.pullRequestEligibilityValidator = pullRequestEligibilityValidator;
    this.pullRequestInfoClient = pullRequestInfoClient;
    this.sourceControlScanService = sourceControlScanService;
  }

  /**
   * given a policy evaluation for an application this method determines if there are any open, internal/private pull
   * requests for the commit associated with that policy evaluation;  if there are, this method then tries to determine
   * the policy evaluations to associate with each one, if able;  in some cases, we can/will initiate those policy
   * evaluations;
   *
   * @return list of zero or more {@link PullRequestPolicyEvaluationsDTO} objects that represent any pull requests that
   * successfully resolved
   */
  public List<PullRequestPolicyEvaluationsDTO> resolveForPolicyEvaluation(
      String applicationId,
      GitRepositoryInfo gitRepositoryInfo,
      String policyEvaluationId,
      String commitHash)
  {
    List<PullRequestPolicyEvaluationsDTO> pullRequestPolicyEvaluationsResults = new ArrayList<>();

    PolicyEvaluation possibleFeatureBranchPolicyEvaluation = policyEvaluationDAO.getById(policyEvaluationId);
    // we don't process pull requests for internally triggered policy evaluations - we let polling take care of that
    if (null == possibleFeatureBranchPolicyEvaluation
        || possibleFeatureBranchPolicyEvaluation.wasInternallyTriggered()) {
      return pullRequestPolicyEvaluationsResults;
    }

    CommitInformation commitInfo = pullRequestInfoClient.getCommitInfoFromScm(gitRepositoryInfo, commitHash);

    // the commit info contains not only the pull requests associated with the commit but also some recent
    // commit history for the base branch
    processDefaultBranchCommitHistory(applicationId, possibleFeatureBranchPolicyEvaluation, commitInfo.getCommits());

    PolicyEvaluation defaultBranchPolicyEvaluation = null;

    for (PullRequest pullRequest : commitInfo.getPullRequests()) {
      if (pullRequestEligibilityValidator.isPullRequestEligibleForCommenting(applicationId, pullRequest,
          gitRepositoryInfo, possibleFeatureBranchPolicyEvaluation)) {

        if (null == defaultBranchPolicyEvaluation) {
          defaultBranchPolicyEvaluation = getLatestPolicyEvaluationReportForBaseBranch(applicationId);
        }

        if (null != defaultBranchPolicyEvaluation) {
          PullRequestPolicyEvaluationsDTO dto = new PullRequestPolicyEvaluationsDTO()
              .setApplicationId(applicationId)
              .setFeatureBranchName(pullRequest.getHead())
              .setDefaultBranchPolicyEvaluation(defaultBranchPolicyEvaluation)
              .setFeatureBranchPolicyEvaluation(possibleFeatureBranchPolicyEvaluation)
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
   * @return {@link PullRequestPolicyEvaluationsDTO}
   */
  public PullRequestPolicyEvaluationsDTO resolveForPullRequest(
      String applicationId,
      GitRepositoryInfo gitRepositoryInfo,
      int pullRequestNumber,
      String featureBranchName,
      String pullRequestHeadCommitHash)
  {
    PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO = null;

    Application application = new ApplicationDAO().getById(applicationId);
    if (null == application) {
      return null;
    }

    try {
      PolicyEvaluation defaultBranchPolicyEvaluation = defaultBranchPolicyEvaluationResolver
          .getOrPerformDefaultBranchPolicyEvaluation(applicationId, gitRepositoryInfo, pullRequestHeadCommitHash);

      if (null != defaultBranchPolicyEvaluation) {
        PolicyEvaluation featureBranchPolicyEvaluation =
            getOrPerformFeatureBranchPolicyEvaluation(applicationId, pullRequestHeadCommitHash, featureBranchName,
                defaultBranchPolicyEvaluation.wasInternallyTriggered());

        if (null != featureBranchPolicyEvaluation) {
          if (defaultBranchPolicyEvaluation.wasInternallyTriggered()
              == featureBranchPolicyEvaluation.wasInternallyTriggered()) {
            pullRequestPolicyEvaluationsDTO = new PullRequestPolicyEvaluationsDTO()
                .setApplicationId(applicationId)
                .setFeatureBranchName(featureBranchName)
                .setDefaultBranchPolicyEvaluation(defaultBranchPolicyEvaluation)
                .setFeatureBranchPolicyEvaluation(featureBranchPolicyEvaluation)
                .setGitRepositoryInfo(gitRepositoryInfo)
                .setPullRequestHeadCommit(pullRequestHeadCommitHash)
                .setPullRequestNumber(pullRequestNumber);
          }
          else {
            log.debug("Cannot comment - internal/external policy evaluation mismatch for application {} repository {}",
                application.getPublicId(), gitRepositoryInfo.getRepositoryUrl());
          }
        }
        else {
          log.debug("Cannot comment - missing feature branch policy evaluation for application {} repository {}",
              application.getPublicId(), gitRepositoryInfo.getRepositoryUrl());
        }
      }
      else {
        log.debug("Cannot comment - missing default branch policy evaluation for application {} repository {}",
            application.getPublicId(), gitRepositoryInfo.getRepositoryUrl());
      }
    }
    catch (Exception e) {
      log.error(
          "Cannot comment - unable to resolve policy evaluations for application {} repository {} pull request {} : {}",
          application.getPublicId(), gitRepositoryInfo.getRepositoryUrl(), pullRequestNumber, e.getMessage());
    }

    return pullRequestPolicyEvaluationsDTO;
  }

  private PolicyEvaluation getOrPerformFeatureBranchPolicyEvaluation(
      String applicationId,
      String pullRequestHeadCommitHash,
      String featureBranchName,
      boolean allowInternalSourceControlScans) throws GitException, IOException
  {
    PolicyEvaluation featureBranchPolicyEvaluation =
        policyEvaluationDAO.getLastByApplicationAndCommitHash(applicationId, pullRequestHeadCommitHash);

    if (null == featureBranchPolicyEvaluation && allowInternalSourceControlScans) {
      featureBranchPolicyEvaluation =
          sourceControlScanService.doSynchronousSourceControlScan(applicationId, DEVELOP_STAGE, featureBranchName);
    }

    return featureBranchPolicyEvaluation;
  }

  private PolicyEvaluation getLatestPolicyEvaluationReportForBaseBranch(String applicationId) {
    Optional<PolicyEvaluation> policyEvaluation =
        gitCommitHistoryService.getLatestPolicyEvaluationForApplicationBaseBranch(applicationId);
    return policyEvaluation.isPresent() ? policyEvaluation.get() : null;
  }

  private void processDefaultBranchCommitHistory(
      String applicationId,
      PolicyEvaluation policyEvaluation,
      List<Commit> commits)
  {
    // this call is for the specific policy eval that was run and if it happened to be for the base branch then
    // the associated commit history will be updated
    gitCommitHistoryService.updateCommitHistoryForPolicyEvaluation(policyEvaluation);

    // this call is for the list of base branch commits we got back from SCM
    gitCommitHistoryService.updateCommitHistoryForCommits(applicationId, commits);
    log.debug("{} base branch commits to process for application '{}'", commits.size(), applicationId);
  }
}
