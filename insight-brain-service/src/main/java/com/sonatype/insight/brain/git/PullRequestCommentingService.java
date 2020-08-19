/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.policy.PolicyEvaluationDiffService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.GitApiClientUtils;
import com.sonatype.nexus.scm.api.PullRequestInfoProvider;
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
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry.ACTION_CREATED;
import static com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry.ACTION_UPDATED;

@Named
@Singleton
public class PullRequestCommentingService
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestCommentingService.class);

  /**
   * Policy violations with a threat level below this threshold are filleted out of the policy violation diff
   */
  public static final int MINIMUM_THREAT_LEVEL = 2;

  static final int COMMIT_HISTORY_FETCH_COUNT = 12;

  static final int APPLICATION_PULL_REQUEST_FETCH_COUNT = 10;

  private final SourceControlUtils sourceControlUtils;

  private final GitClientFactory gitClientFactory;

  private final SourceControlPullRequestCommentDAO pullRequestCommentDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final SourceControlEventDAO sourceControlEventDAO;

  private final PullRequestFeedbackMarkupService pullRequestFeedbackMarkupService;

  private final GitCommitHistoryService gitCommitHistoryService;

  private final PullRequestCommentingMetricsService prCommentingMetricsService;

  private final AsyncEventBus asyncEventBus;

  private final ProductLicense productLicense;

  private final PullRequestRepositoryValidator pullRequestRepositoryValidator;

  private final PolicyEvaluationDiffService policyEvaluationDiffService;

  private final InsightConfig insightConfig;

  private final PullRequestCommentingRemediationService commentingRemediationService;

  private final PullRequestLineCommentingService pullRequestLineCommentingService;

  private final Provider<PullRequestCommentingHashBuilder> hashBuilderProvider;

  private final List<PullRequestPostCommentAction> pullRequestPostCommentActionList;

  private final PullRequestLocationDiscoveryService locationDiscoveryService;

  @Inject
  public PullRequestCommentingService(
      final SourceControlUtils sourceControlUtils,
      final GitClientFactory gitClientFactory,
      final SourceControlPullRequestCommentDAO pullRequestCommentDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final SourceControlEventDAO sourceControlEventDAO,
      final PullRequestFeedbackMarkupService pullRequestFeedbackMarkupService,
      final GitCommitHistoryService gitCommitHistoryService,
      final PullRequestCommentingMetricsService prCommentingMetricsService,
      final PullRequestCommentingRemediationService commentingRemediationService,
      final AsyncEventBus asyncEventBus,
      final ProductLicense productLicense,
      final PullRequestRepositoryValidator pullRequestRepositoryValidator,
      final PolicyEvaluationDiffService policyEvaluationDiffService,
      final InsightConfig insightConfig,
      final PullRequestLineCommentingService pullRequestLineCommentingService,
      final Provider<PullRequestCommentingHashBuilder> hashBuilderProvider,
      final List<PullRequestPostCommentAction> pullRequestPostCommentActionList,
      final PullRequestLocationDiscoveryService locationDiscoveryService)
  {
    this.sourceControlUtils = sourceControlUtils;
    this.gitClientFactory = gitClientFactory;
    this.pullRequestCommentDAO = pullRequestCommentDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.pullRequestFeedbackMarkupService = pullRequestFeedbackMarkupService;
    this.gitCommitHistoryService = gitCommitHistoryService;
    this.prCommentingMetricsService = prCommentingMetricsService;
    this.commentingRemediationService = commentingRemediationService;
    this.asyncEventBus = asyncEventBus;
    this.productLicense = productLicense;
    this.pullRequestRepositoryValidator = pullRequestRepositoryValidator;
    this.policyEvaluationDiffService = policyEvaluationDiffService;
    this.insightConfig = insightConfig;
    this.pullRequestLineCommentingService = pullRequestLineCommentingService;
    this.hashBuilderProvider = hashBuilderProvider;
    this.pullRequestPostCommentActionList = pullRequestPostCommentActionList;
    this.locationDiscoveryService = locationDiscoveryService;
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
    if (!insightConfig.isFeatureEnabled(Feature.PR_COMMENTING)) {
      return;
    }
    if (!checkLicense()) {
      log.debug("License does not support SourceControl automation features");
      return;
    }

    if (eventHasCommitHashAndScmIsEnabled(event)) {
      String applicationId = event.ownerId;
      GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);

      if (!gitRepositoryInfo.provider.supportsPullRequestCommenting() ||
          (gitRepositoryInfo.provider == SourceControlProvider.GITLAB && // will be removed when MR commenting is ready
              !insightConfig.isExperimentalFeatureEnabled("mrCommenting"))) {
        log.debug("'{}' not currently supported for pull request commenting", gitRepositoryInfo.provider.toString());
      }
      else {
        SourceControlEvent sourceControlEvent = new SourceControlEvent()
            .setApplicationId(applicationId)
            .setCommitHash(event.commitHash)
            .setEventType(SourceControlEvent.APPLICATION_EVALUATION_EVENT)
            .setPolicyEvaluationId(event.policyEvaluationId)
            .setInitiator(event.initiator)
            .setCreateTime(new Date());

        sourceControlEventDAO.insert(sourceControlEvent);
        log.debug("Persisted source control event '{}' for application '{}' and commit '{}'",
            sourceControlEvent.getEventType(), applicationId, event.commitHash);
      }
    }
  }

  public void onApplicationEvaluation(SourceControlEvent event) {
    try {
      String applicationId = event.getApplicationId();
      GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);

      PolicyEvaluation sourceCommitPolicyEvaluation = policyEvaluationDAO.getById(event.getPolicyEvaluationId());
      CommitInformation commitInfo = getCommitInfoFromScm(gitRepositoryInfo, event.getCommitHash());

      // the commit info contains not only the pull requests associated with the commit but also some recent
      // commit history for the base branch
      processBaseBranchCommitHistory(sourceCommitPolicyEvaluation, commitInfo.getCommits());

      for (PullRequest pullRequest : commitInfo.getPullRequests()) {
        if (shouldCommentOnPullRequest(applicationId, pullRequest, gitRepositoryInfo,
            sourceCommitPolicyEvaluation)) {
          SourceControlPullRequestComment existingPullRequestComment = pullRequestCommentDAO
              .getByApplicationIdAndPullRequestIdWithoutComponent(applicationId, pullRequest.getNumber());

          Optional<PolicyEvaluation> baseBranchPolicyEvaluation =
              getLatestPolicyEvaluationReportForBaseBranch(applicationId);

          if (baseBranchPolicyEvaluation.isPresent()) {
            doCreateOrUpdatePullRequestComment(applicationId, gitRepositoryInfo,
                pullRequest.getNumber(), pullRequest.getHead(),
                sourceCommitPolicyEvaluation, baseBranchPolicyEvaluation.get(), existingPullRequestComment);
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
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  public void onDiscoveredPullRequest(SourceControlEvent event) {
    String applicationId = event.getApplicationId();
    GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);
    PolicyEvaluation sourceCommitPolicyEvaluation = policyEvaluationDAO.getById(event.getPolicyEvaluationId());

    Optional<PolicyEvaluation> baseBranchPolicyEvaluation = Optional.empty();

    if (null == event.getTargetPolicyEvaluationId()) {
      // we need to get and process the base branch commit history
      CommitInformation commitInfo = getCommitInfoFromScm(gitRepositoryInfo, event.getCommitHash());
      // the commit info contains not only the pull requests associated with the commit but also some recent commit
      // history for the base branch
      processBaseBranchCommitHistory(sourceCommitPolicyEvaluation, commitInfo.getCommits());
      baseBranchPolicyEvaluation = getLatestPolicyEvaluationReportForBaseBranch(applicationId);
    }
    else {
      baseBranchPolicyEvaluation =
          Optional.ofNullable(policyEvaluationDAO.getById(event.getTargetPolicyEvaluationId()));
    }

    if (baseBranchPolicyEvaluation.isPresent()) {
      // do we already have a comment for this PR?
      SourceControlPullRequestComment existingPullRequestComment =
          pullRequestCommentDAO
              .getByApplicationIdAndPullRequestIdWithoutComponent(applicationId, event.getPullRequestNumber());

      doCreateOrUpdatePullRequestComment(applicationId, gitRepositoryInfo, event.getPullRequestNumber(),
          event.getBranchName(), sourceCommitPolicyEvaluation, baseBranchPolicyEvaluation.get(),
          existingPullRequestComment);
    }
    else {
      log.warn(
          "no policy evaluation for base branch, skipping PR commenting for application '{}' pull request '{}'",
          applicationId, event.getPullRequestNumber());
    }
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
  private void doCreateOrUpdatePullRequestComment(
      final String applicationId,
      final GitRepositoryInfo gitRepositoryInfo,
      final int pullRequestNumber,
      final String branchName,
      final PolicyEvaluation sourceCommitPolicyEvaluation,
      final PolicyEvaluation baseBranchPolicyEvaluation,
      final SourceControlPullRequestComment existingPullRequestComment)
  {
    try {
      Optional<PolicyViolationDiff<PolicyViolation>> policyViolationDiff = policyEvaluationDiffService
          .createPolicyViolationDiff(baseBranchPolicyEvaluation, sourceCommitPolicyEvaluation, MINIMUM_THREAT_LEVEL);

      if (policyViolationDiff.isPresent()) {
        // retrieve suggested remediation map for components in the appeared violation list
        SortedMap<ComponentIdentifier, String> remediationVersionMap = commentingRemediationService
            .getRemediationVersionMap(policyViolationDiff.get().getAppeared(), applicationId);

        // calculate comment content hash
        String contentHash = hashBuilderProvider.get().withPolicyViolationDiff(policyViolationDiff.get())
            .withRemediationVersionMap(remediationVersionMap).generateHash();

        if (existingPullRequestComment == null) { // new PR comment
          if (policyViolationDiff.get().hasAppeared() || policyViolationDiff.get().hasCleared()) {
            doCreateOrUpdateComments(applicationId, gitRepositoryInfo, pullRequestNumber, branchName,
                sourceCommitPolicyEvaluation,
                baseBranchPolicyEvaluation, existingPullRequestComment, policyViolationDiff, remediationVersionMap,
                contentHash);
          }
          else {
            log.info("no added or cleared violations in policy evaluation diff, and no previous PR comments for " +
                "application '{}' pull request '{}'.", applicationId, pullRequestNumber);
          }
        }
        else { // existing PR comment
          if (!contentHash.equals(existingPullRequestComment.getContentHash())) {
            doCreateOrUpdateComments(applicationId, gitRepositoryInfo, pullRequestNumber, branchName,
                sourceCommitPolicyEvaluation,
                baseBranchPolicyEvaluation, existingPullRequestComment, policyViolationDiff, remediationVersionMap,
                contentHash);
          }
          else {
            log.info("policy evaluations have not changed for application '{}' pull request '{}'.",
                applicationId, pullRequestNumber);
          }
        }
      }
      else {
        log.info("unable to get the policy evaluation diff for application '{}' pull request '{}'.",
            applicationId, pullRequestNumber);
      }
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  private void doCreateOrUpdateComments(
      final String applicationId,
      final GitRepositoryInfo gitRepositoryInfo,
      final int pullRequestNumber,
      final String branchName,
      final PolicyEvaluation sourceCommitPolicyEvaluation,
      final PolicyEvaluation baseBranchPolicyEvaluation,
      final SourceControlPullRequestComment existingPullRequestComment,
      final Optional<PolicyViolationDiff<PolicyViolation>> policyViolationDiff,
      final Map<ComponentIdentifier, String> remediationVersionMap,
      final String contentHash)
      throws IOException
  {
    PullRequestCommentTelemetry telemetry = new PullRequestCommentTelemetry(applicationId, pullRequestNumber);

    LocationDiscoveryResult locationDiscoveryResult = new LocationDiscoveryResult();
    if (isLocationDiscoveryRequired(gitRepositoryInfo, policyViolationDiff)) {
      // Find all potential source locations to comment on
      List<PolicyViolation> violationList = policyViolationDiff.get().getAppeared();
      locationDiscoveryResult = locationDiscoveryService.doLocationDiscovery(
          violationList, gitRepositoryInfo, branchName, applicationId);
    }

    // line comment sub-flow
    List<PullRequestLineCommentDTO> pullRequestLineComments = pullRequestLineCommentingService
        .createPullRequestLineComments(policyViolationDiff.get().getAppeared(), gitRepositoryInfo,
            remediationVersionMap, pullRequestNumber, sourceCommitPolicyEvaluation.getCommitHash(),
            applicationId, sourceCommitPolicyEvaluation.getId(), baseBranchPolicyEvaluation.getId(),
            locationDiscoveryResult);
    telemetry.lineCommentCount = pullRequestLineComments.size();

    Optional<String> policyEvaluationDiffMarkup = pullRequestFeedbackMarkupService.createMarkup(
        policyViolationDiff.get(), remediationVersionMap, pullRequestLineComments, gitRepositoryInfo, pullRequestNumber,
        sourceCommitPolicyEvaluation, baseBranchPolicyEvaluation, telemetry);

    if (policyEvaluationDiffMarkup.isPresent()) {
      Optional<CommentResponse> response =
          createOrUpdateCommentInGitSCM(applicationId, gitRepositoryInfo, pullRequestNumber,
              policyEvaluationDiffMarkup.get(), existingPullRequestComment, telemetry);
      if (response.isPresent()) {
        CommentResponse commentResponse = response.get();
        recordCommentInDatabase(applicationId, pullRequestNumber, commentResponse.getId(), commentResponse.getVersion(),
            contentHash, sourceCommitPolicyEvaluation.getId(), baseBranchPolicyEvaluation.getId(),
            existingPullRequestComment);
        invokePostCommentActions(gitRepositoryInfo, policyViolationDiff.get(),
            sourceCommitPolicyEvaluation, baseBranchPolicyEvaluation, branchName, locationDiscoveryResult);

        prCommentingMetricsService.sendTelemetry(telemetry);

        AuditEvent auditEvent = existingPullRequestComment == null
            ? AuditEvent.CREATE_PULL_REQUEST_COMMENT : AuditEvent.UPDATE_PULL_REQUEST_COMMENT;
        prCommentingMetricsService.addAuditRecord(
            auditEvent, applicationId, gitRepositoryInfo.repositoryUrl, pullRequestNumber);
      }
    }
    else {
      log.info("generated feedback markup was empty for application '{}' pull request '{}'",
          applicationId, pullRequestNumber);
    }
  }

  /**
   * Check if we will need to do location discovery
   */
  private boolean isLocationDiscoveryRequired(final GitRepositoryInfo gitRepositoryInfo,
                                              final Optional<PolicyViolationDiff<PolicyViolation>> policyViolationDiff)
  {
    if ((insightConfig.isFeatureEnabled(Feature.PR_LINE_COMMENTING) &&
        gitRepositoryInfo.getProvider().supportsPullRequestLineCommenting()) ||
        gitRepositoryInfo.getProvider().supportsCodeInsights()) {
      return policyViolationDiff.isPresent() && !policyViolationDiff.get().getAppeared().isEmpty();
    }
    return false;
  }

  /**
   * creates or updates the pull request comment in GitHub for the given repo and pull request
   */
  private Optional<CommentResponse> createOrUpdateCommentInGitSCM(
      String applicationId,
      GitRepositoryInfo gitRepositoryInfo,
      int pullRequestNumber,
      String commentText,
      SourceControlPullRequestComment existingPullRequestComment,
      PullRequestCommentTelemetry telemetry)
      throws IOException
  {
    Optional<CommentResponse> response;

    GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepositoryInfo);
    if (existingPullRequestComment == null) {
      CommentResponse commentResponse = gitApiClient.createPullRequestComment(pullRequestNumber, commentText);
      log.info("pull request comment '{}' created for application '{}' pull request '{}'",
          commentResponse.getId(), applicationId, pullRequestNumber);
      telemetry.action = ACTION_CREATED;
      response = Optional.of(commentResponse);
    }
    else {
      response = updateCommentInGitSCM(applicationId, pullRequestNumber, commentText, existingPullRequestComment,
          gitApiClient);
      telemetry.action = ACTION_UPDATED;
    }
    response.ifPresent(commentResponse -> telemetry.commentId = commentResponse.getId());
    return response;
  }

  private Optional<CommentResponse> updateCommentInGitSCM(
      final String applicationId,
      final int pullRequestNumber,
      final String commentText,
      final SourceControlPullRequestComment existingPullRequestComment,
      final GitApiClient gitApiClient) throws IOException
  {
    CommentResponse commentResponse = null;
    try {
      commentResponse = gitApiClient
          .updatePullRequestComment(existingPullRequestComment.getPullRequestCommentId(), pullRequestNumber,
              existingPullRequestComment.getPullRequestCommentVersion(), commentText);
      if (commentResponse.getVersion() == null) {
        log.info("pull request comment '{}' updated for application '{}' pull request '{}'",
            commentResponse.getId(), applicationId, pullRequestNumber);
      }
      else {
        log.info("pull request comment '{}' with version '{}' updated for application '{}' pull request '{}'",
            commentResponse.getId(), commentResponse.getVersion(), applicationId, pullRequestNumber);
      }
    }
    catch (HttpResponseException e) {
      if (HttpStatus.SC_NOT_FOUND == e.getStatusCode()) {
        log.warn("Updating pull request comment '{}' for application '{}' pull request '{}' returned 404 NOT FOUND",
            existingPullRequestComment.getPullRequestCommentId(), applicationId, pullRequestNumber);
      }
      else {
        throw e;
      }
    }
    return Optional.ofNullable(commentResponse);
  }

  /**
   * record in the DB that we've created or updated a comment for the given pull request for the given app
   */
  private void recordCommentInDatabase(
      String applicationId,
      int pullRequestNumber,
      Integer commentId,
      Integer commentVersion,
      String contentHash,
      String sourcePolicyEvaluationId,
      String basePolicyEvaluationId,
      SourceControlPullRequestComment existingPullRequestComment)
  {
    if (existingPullRequestComment == null) {
      SourceControlPullRequestComment pullRequestComment =
          new SourceControlPullRequestComment(applicationId, pullRequestNumber, commentId, commentVersion, contentHash,
              sourcePolicyEvaluationId, basePolicyEvaluationId);
      pullRequestCommentDAO.insert(pullRequestComment);
    }
    else {
      existingPullRequestComment.setPullRequestCommentId(commentId);
      existingPullRequestComment.setPullRequestCommentVersion(commentVersion);
      existingPullRequestComment.setContentHash(contentHash);
      existingPullRequestComment.setSourcePolicyEvaluationId(sourcePolicyEvaluationId);
      existingPullRequestComment.setTargetPolicyEvaluationId(basePolicyEvaluationId);
      pullRequestCommentDAO.update(existingPullRequestComment);
    }
    log.debug("pull request comment '{}' for application '{}' pull request '{}' recorded in database", commentId,
        applicationId, pullRequestNumber);
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
      PullRequestInfoProvider client = gitClientFactory.createPullRequestInfoClient(gitRepositoryInfo);
      result = client.getCommitInformationForCommit(
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
    if (!pullRequestRepositoryValidator.isInternalRepository(gitRepositoryInfo) &&
        !pullRequest.isRepositoryPrivate()) {
      log.debug("Repository is not valid for pull requests, ensure that it is private: {}",
          gitRepositoryInfo.repositoryUrl);
      return false;
    }

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

  /**
   * Invoked after a comment has been created or updated
   */
  private void invokePostCommentActions(
      final GitRepositoryInfo gitRepositoryInfo,
      final PolicyViolationDiff<PolicyViolation> policyViolationDiff,
      final PolicyEvaluation sourceCommitPolicyEvaluation,
      final PolicyEvaluation baseBranchPolicyEvaluation,
      final String branch,
      final LocationDiscoveryResult locationDiscoveryResult)
  {
    pullRequestPostCommentActionList.forEach(pullRequestPostCommentAction -> pullRequestPostCommentAction
        .invokeAction(gitClientFactory, gitRepositoryInfo, policyViolationDiff, sourceCommitPolicyEvaluation,
            baseBranchPolicyEvaluation, branch, locationDiscoveryResult));
  }

  private boolean checkLicense() {
    return productLicense.hasFeature(LicensedFeature.AUTOMATION);
  }
}
