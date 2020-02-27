/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.GitApiClientUtils;
import com.sonatype.nexus.scm.api.GitGraphQlApiClient;
import com.sonatype.nexus.scm.api.model.CommentResponse;
import com.sonatype.nexus.scm.api.model.Commit;
import com.sonatype.nexus.scm.api.model.CommitInformation;
import com.sonatype.nexus.scm.api.model.ProjectUri;
import com.sonatype.nexus.scm.api.model.PullRequest;
import com.sonatype.nexus.scm.api.model.PullRequestState;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.Subscribe;
import io.dropwizard.lifecycle.Managed;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class PullRequestCommentingService
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestCommentingService.class);

  static final int COMMIT_HISTORY_FETCH_COUNT = 12;

  static final int APPLICATION_PULL_REQUEST_FETCH_COUNT = 10;

  private final SourceControlUtils sourceControlUtils;

  private final GitClientFactory gitClientFactory;

  private final SourceControlPullRequestCommentDAO pullRequestCommentDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PullRequestFeedbackMarkupService pullRequestFeedbackMarkupService;

  private final GitCommitHistoryService gitCommitHistoryService;

  private final PullRequestCommentingMetricsService pullRequestCommentingMetricsService;

  private final AsyncEventBus asyncEventBus;

  private final ProductLicense productLicense;

  private final boolean pullRequestImmediateFlowEnabled;

  @Inject
  public PullRequestCommentingService(
      final SourceControlUtils sourceControlUtils,
      final GitClientFactory gitClientFactory,
      final SourceControlPullRequestCommentDAO pullRequestCommentDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final PullRequestFeedbackMarkupService pullRequestFeedbackMarkupService,
      final GitCommitHistoryService gitCommitHistoryService,
      final PullRequestCommentingMetricsService pullRequestCommentingMetricsService,
      final AsyncEventBus asyncEventBus,
      final ProductLicense productLicense)
  {
    this.sourceControlUtils = sourceControlUtils;
    this.gitClientFactory = gitClientFactory;
    this.pullRequestCommentDAO = pullRequestCommentDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.pullRequestFeedbackMarkupService = pullRequestFeedbackMarkupService;
    this.gitCommitHistoryService = gitCommitHistoryService;
    this.pullRequestCommentingMetricsService = pullRequestCommentingMetricsService;
    this.asyncEventBus = asyncEventBus;
    this.productLicense = productLicense;
    pullRequestImmediateFlowEnabled = null != System.getProperty("enable-pr-immediate");
  }

  @VisibleForTesting
  PullRequestCommentingService(
      final SourceControlUtils sourceControlUtils,
      final GitClientFactory gitClientFactory,
      final SourceControlPullRequestCommentDAO pullRequestCommentDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final PullRequestFeedbackMarkupService pullRequestFeedbackMarkupService,
      final GitCommitHistoryService gitCommitHistoryService,
      final PullRequestCommentingMetricsService pullRequestCommentingMetricsService,
      final AsyncEventBus asyncEventBus,
      final ProductLicense productLicense,
      boolean pullRequestImmediateFlowEnabled)
  {
    this.sourceControlUtils = sourceControlUtils;
    this.gitClientFactory = gitClientFactory;
    this.pullRequestCommentDAO = pullRequestCommentDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.pullRequestFeedbackMarkupService = pullRequestFeedbackMarkupService;
    this.gitCommitHistoryService = gitCommitHistoryService;
    this.pullRequestCommentingMetricsService = pullRequestCommentingMetricsService;
    this.asyncEventBus = asyncEventBus;
    this.productLicense = productLicense;
    this.pullRequestImmediateFlowEnabled = pullRequestImmediateFlowEnabled;
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
   * development branch commit that triggered the policy evaluation (which then issued this event) and the most
   * recently available policy evaluation for the source control configured base branch for the associated application.
   *
   * @param event ApplicationEvaluation event that triggered this call
   */
  @Subscribe
  public void onApplicationEvaluation(ApplicationEvaluationEvent event) {
    if (!pullRequestImmediateFlowEnabled) {
      return;
    }
    if (!checkLicense()) {
      log.debug("License does not support SourceControl automation features");
      return;
    }

    try {
      boolean commentCreated = false;

      if (eventHasCommitHashAndScmIsEnabled(event)) {
        String applicationId = event.ownerId;
        GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);

        if (SourceControlProvider.GITLAB == gitRepositoryInfo.provider) {
          log.debug("GitLab not currently supported for pull request commenting");
        }
        else {
          PolicyEvaluation sourceCommitPolicyEvaluation = policyEvaluationDAO.getById(event.policyEvaluationId);
          CommitInformation commitInfo = getCommitInfoFromScm(gitRepositoryInfo, event.commitHash);

          // the commit info contains not only the pull requests associated with the commit but also some recent commit
          // history for the base branch (optimization that uses GraphQL to fetch multiple pieces of data and cut down
          // on GitHub API load/potential rate limiting);  so, we tuck that info away first so we can make use of it
          // later in this flow
          processBaseBranchCommitHistory(sourceCommitPolicyEvaluation, commitInfo.getCommits());

          for (PullRequest pullRequest : commitInfo.getPullRequests()) {
            if (shouldCommentOnPullRequest(applicationId, pullRequest, gitRepositoryInfo,
                sourceCommitPolicyEvaluation)) {
              // if we've already commented on the PR then we'll skip further processing for now but in the future we'll
              // enter a new PR comment update flow
              SourceControlPullRequestComment existingPullRequestComment =
                  pullRequestCommentDAO.getByApplicationIdAndPullRequestId(applicationId, pullRequest.getNumber());

              if (null != existingPullRequestComment) {
                log.debug(
                    "application '{}' pull request '{}' is already commented, skipping further commenting on this PR",
                    applicationId, pullRequest.getNumber());
                // todo - update existing pull request flow - future - INT-2390
              }
              else {
                Optional<PolicyEvaluation> baseBranchPolicyEvaluation =
                    getLatestPolicyEvaluationReportForBaseBranch(applicationId);

                if (baseBranchPolicyEvaluation.isPresent()) {
                  commentCreated = doPullRequestComment(applicationId, gitRepositoryInfo, pullRequest.getNumber(),
                      sourceCommitPolicyEvaluation, baseBranchPolicyEvaluation.get());
                }
                else {
                  log.warn(
                      "no policy evaluation for base branch, skipping PR commenting for application '{}' " +
                          "pull request '{}'",
                      applicationId, pullRequest.getNumber());
                }
              }
            }
          }
        }
      }

      reportMetrics(commentCreated);
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  @Subscribe
  public void onDiscoveredPullRequest(DiscoveredPullRequestEvent event) {
    String applicationId = event.applicationId;
    GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);
    PolicyEvaluation sourceCommitPolicyEvaluation = policyEvaluationDAO.getById(event.policyEvaluationId);
    boolean commentCreated = false;

    Optional<PolicyEvaluation> baseBranchPolicyEvaluation;

    if (null == event.targetPolicyEvaluationId) {
      // we need to get and process the base branch commit history
      CommitInformation commitInfo = getCommitInfoFromScm(gitRepositoryInfo, event.commitHash);

      // the commit info contains not only the pull requests associated with the commit but also some recent commit
      // history for the base branch (optimization that uses GraphQL to fetch multiple pieces of data and cut down
      // on GitHub API load/potential rate limiting);  so, we tuck that info away first so we can make use of it
      // later in this flow
      processBaseBranchCommitHistory(sourceCommitPolicyEvaluation, commitInfo.getCommits());

      baseBranchPolicyEvaluation = getLatestPolicyEvaluationReportForBaseBranch(applicationId);
    }
    else {
      baseBranchPolicyEvaluation = Optional.ofNullable(policyEvaluationDAO.getById(event.targetPolicyEvaluationId));
    }

    if (baseBranchPolicyEvaluation.isPresent()) {
      // do we already have a comment for this PR?
      if (null == pullRequestCommentDAO.getByApplicationIdAndPullRequestId(applicationId, event.pullRequestNumber)) {
        commentCreated = doPullRequestComment(applicationId, gitRepositoryInfo, event.pullRequestNumber,
            sourceCommitPolicyEvaluation, baseBranchPolicyEvaluation.get());
      }
      else {
        log.debug("Application '{}' pull request '{}' already has a PR comment, skipping further processing",
            applicationId, event.pullRequestNumber);
        // todo - update existing pull request flow - future - INT-2390
      }
    }
    else {
      log.warn(
          "no policy evaluation for base branch, skipping PR commenting for application '{}' pull request '{}'",
          applicationId, event.pullRequestNumber);
    }

    reportMetrics(commentCreated);
  }

  private boolean isPullRequestForBaseBranch(PullRequest pullRequest, GitRepositoryInfo gitRepositoryInfo) {
    return pullRequest.getHead().equalsIgnoreCase(gitRepositoryInfo.baseBranch);
  }

  private Optional<PolicyEvaluation> getLatestPolicyEvaluationReportForBaseBranch(String applicationId) {
    return gitCommitHistoryService.getLatestPolicyEvaluationForApplicationBaseBranch(applicationId);
  }

  /**
   * computes the policy evaluation diff, creates and pushes the comment to the SCM system, records comment metadata in
   * our DB, and pushes metrics
   */
  private boolean doPullRequestComment(
      String applicationId,
      GitRepositoryInfo gitRepositoryInfo,
      int pullRequestNumber,
      PolicyEvaluation sourceCommitPolicyEvaluation,
      PolicyEvaluation baseBranchPolicyEvaluation)
  {
    boolean commentCreated = false;

    try {
      Optional<String> policyEvaluationDiffMarkup = pullRequestFeedbackMarkupService
          .createMarkupIfNewViolationsHaveAppeared(sourceCommitPolicyEvaluation, baseBranchPolicyEvaluation);

      if (policyEvaluationDiffMarkup.isPresent()) {
        Integer commentId =
            createCommentInGitSCM(applicationId, gitRepositoryInfo, pullRequestNumber,
                policyEvaluationDiffMarkup.get());
        recordCommentInDatabase(applicationId, pullRequestNumber, commentId, sourceCommitPolicyEvaluation.getId(),
            baseBranchPolicyEvaluation.getId());
        commentCreated = true;
      }
      else {
        log.info("nothing meaningful in policy eval diff for application '{}' pull request '{}' to comment on",
            applicationId, pullRequestNumber);
      }
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return commentCreated;
  }

  /**
   * creates the pull request comment in GitHub for the given repo and pull request
   */
  private Integer createCommentInGitSCM(
      String applicationId,
      GitRepositoryInfo gitRepositoryInfo,
      int pullRequestNumber,
      String commentText)
      throws IOException
  {
    GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepositoryInfo);
    CommentResponse commentResponse = gitApiClient.createPullRequestComment(pullRequestNumber, commentText);
    log.info("pull request comment '{}' created for application '{}' pull request '{}'",
        commentResponse.getId(), applicationId, pullRequestNumber);
    log.debug("comment text = {}", commentText);
    return commentResponse.getId();
  }

  /**
   * record in the DB that we've created a comment for the given pull request for the given app
   */
  private void recordCommentInDatabase(
      String applicationId,
      int pullRequestNumber,
      Integer commentId,
      String sourcePolicyEvaluationId,
      String basePolicyEvaluationId)
  {
    SourceControlPullRequestComment pullRequestComment =
        new SourceControlPullRequestComment(applicationId, pullRequestNumber, commentId, sourcePolicyEvaluationId,
            basePolicyEvaluationId);
    pullRequestCommentDAO.insert(pullRequestComment);
    log.debug("pull request comment '{}' for application '{}' pull request '{}' recorded in database", commentId,
        applicationId, pullRequestNumber);
  }

  private void reportMetrics(boolean commentCreated) {
    // tbd - future work - INT-2490
    pullRequestCommentingMetricsService.recordEvent(commentCreated);
  }

  @VisibleForTesting
  boolean eventHasCommitHashAndScmIsEnabled(ApplicationEvaluationEvent event) {
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

  /**
   * @return a CommitInformation object as obtained from the scm client or a blank/empty one if there was an exception
   */
  @VisibleForTesting
  CommitInformation getCommitInfoFromScm(GitRepositoryInfo gitRepositoryInfo, String commitHash) {
    CommitInformation result = null;

    GitApiClientUtils gitApiClientUtils = new GitApiClientFactory().getGitApiClientUtils(gitRepositoryInfo.provider);
    ProjectUri projectUri = gitApiClientUtils.createProjectUri(gitRepositoryInfo.repositoryUrl);

    try {
      GitGraphQlApiClient graphqlApiClient = gitClientFactory.createGraphqlApiClient(gitRepositoryInfo);
      result = graphqlApiClient.getCommitInformationForCommit(
          projectUri.getNamespace(),
          projectUri.getProject(),
          commitHash,
          gitRepositoryInfo.baseBranch,
          COMMIT_HISTORY_FETCH_COUNT,
          APPLICATION_PULL_REQUEST_FETCH_COUNT
      );
      log.debug("obtained CommitInfo from SCM for commit '{}' with {} pull request(s) and {} base branch commit(s)",
          commitHash, result.getPullRequests().size(), result.getCommits().size());
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
      result = new CommitInformation();
    }
    return result;
  }

  private void processBaseBranchCommitHistory(
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

  private boolean doesHeadCommitMatchPolicyEvaluationCommit(String policyEvaluationCommitHash, String headCommitHash) {
    return policyEvaluationCommitHash.equals(headCommitHash);
  }

  private boolean isPullRequestOpen(final PullRequest pullRequest) {
    return PullRequestState.OPEN.equals(pullRequest.getState());
  }

  private boolean shouldCommentOnPullRequest(
      String applicationId,
      PullRequest pullRequest,
      GitRepositoryInfo gitRepositoryInfo,
      PolicyEvaluation sourceCommitPolicyEvaluation)
  {
    if (!isPullRequestOpen(pullRequest)) {
      log.debug(
          "application '{}' pull request '{}' state '{}' is not open, skipping commenting for this PR",
          applicationId, pullRequest.getNumber(), pullRequest.getState());
      return false;
    }

    if (isPullRequestForBaseBranch(pullRequest, gitRepositoryInfo)) {
      log.debug(
          "application '{}' pull request '{}' is for the base branch, skipping commenting for this PR",
          applicationId, pullRequest.getNumber());
      return false;
    }

    if (!doesHeadCommitMatchPolicyEvaluationCommit(sourceCommitPolicyEvaluation.getCommitHash(),
        pullRequest.getHeadCommitHash())) {
      log.debug(
          "The head commit hash '{}', for application '{}', PR '{}' does not match the commit on the policy " +
              "evaluation '{}'", pullRequest.getHeadCommitHash(), applicationId, pullRequest.getNumber(),
          sourceCommitPolicyEvaluation.getCommitHash());
      return false;
    }
    return true;
  }

  private boolean checkLicense() {
    return productLicense.hasFeature(LicensedFeature.AUTOMATION);
  }
}
