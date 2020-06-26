/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventService;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.PullRequestInfoProvider;
import com.sonatype.nexus.scm.api.model.ProjectUri;
import com.sonatype.nexus.scm.api.model.PullRequest;
import com.sonatype.nexus.scm.bitbucket.BitbucketApiClientUtils;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;

@Named
@Singleton
public class PullRequestPollingService
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestPollingService.class);

  @VisibleForTesting
  static final int PULL_REQUESTS_PER_MONITOR_CYCLE = 50;

  private static final int MAX_API_REQUESTS_PER_CYCLE = 50;

  private static final String POLLING = "polling";

  private final SourceControlDAO sourceControlDAO;

  private final SourceControlEventService sourceControlEventService;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final GitCommitHistoryService gitCommitHistoryService;

  private final SourceControlUtils sourceControlUtils;

  private final GitClientFactory gitClientFactory;

  private final PullRequestRepositoryValidator pullRequestRepositoryValidator;

  @Inject
  public PullRequestPollingService(
      SourceControlDAO sourceControlDAO,
      SourceControlEventService sourceControlEventService,
      PolicyEvaluationDAO policyEvaluationDAO,
      GitCommitHistoryService gitCommitHistoryService,
      SourceControlUtils sourceControlUtils,
      GitClientFactory gitClientFactory,
      PullRequestRepositoryValidator pullRequestRepositoryValidator)
  {
    this.sourceControlDAO = sourceControlDAO;
    this.sourceControlEventService = sourceControlEventService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.gitCommitHistoryService = gitCommitHistoryService;
    this.sourceControlUtils = sourceControlUtils;
    this.gitClientFactory = gitClientFactory;
    this.pullRequestRepositoryValidator = pullRequestRepositoryValidator;
  }

  public void fetchAndSendPullRequestsForCommenting() throws IOException {
    PullRequestPollingTracker pollingTracker = new PullRequestPollingTracker(sourceControlDAO);

    // the pull requests we get back can be for any app that the related org and key have access to
    for (PullRequest pullRequest : getPullRequestsFromScm(pollingTracker)) {
      // a given commit could be associated with multiple applications
      List<PolicyEvaluation> sourcePolicyEvaluations =
          policyEvaluationDAO.getLastByCommitHashPerApplication(pullRequest.getHeadCommitHash());

      if (CollectionUtils.isEmpty(sourcePolicyEvaluations)) {
        log.debug("Policy evaluation not yet available for '{}' pull request '{}'", pullRequest.getRepository(),
            pullRequest.getNumber());
        if (pollingTracker.onPullRequestProcessed(pullRequest)) {
          log.debug("Pull request polling time updated for '{}'", pullRequest.getRepository());
        }
      }

      for (PolicyEvaluation sourcePolicyEvaluation : sourcePolicyEvaluations) {
        String applicationId = sourcePolicyEvaluation.getApplicationId();

        GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);

        if (null != gitRepositoryInfo) {
          if (!pullRequestRepositoryValidator.isInternalRepository(gitRepositoryInfo) &&
              !pullRequest.isRepositoryPrivate()) {
            log.debug("Repository is not valid for pull requests, check that it is private: {}",
                gitRepositoryInfo.repositoryUrl);
          }
          else {
            if (!isPullRequestForBaseBranch(pullRequest, gitRepositoryInfo)) {
              PolicyEvaluation targetPolicyEvaluation = getLatestPolicyEvaluationForBaseBranch(applicationId);
              createAndSendDiscoveredPullRequestEvent(applicationId, pullRequest.getNumber(), pullRequest.getHead(),
                  sourcePolicyEvaluation, targetPolicyEvaluation);
            }
            else {
              log.debug(
                  "application '{}' pull request '{}' is for the base branch, skipping commenting for this PR",
                  applicationId, pullRequest.getNumber());
            }
          }
        }
        pollingTracker.onPullRequestProcessedForApplication(applicationId, pullRequest.getCreated());
        log.debug("Pull request polling time updated for '{}'", pullRequest.getRepository());
      }
    }
  }

  private boolean isPullRequestForBaseBranch(PullRequest pullRequest, GitRepositoryInfo gitRepositoryInfo) {
    return pullRequest.getHead().equalsIgnoreCase(gitRepositoryInfo.baseBranch);
  }

  private PolicyEvaluation getLatestPolicyEvaluationForBaseBranch(String applicationId) {
    Optional<PolicyEvaluation> policyEvaluation =
        gitCommitHistoryService.getLatestPolicyEvaluationForApplicationBaseBranch(applicationId);
    return policyEvaluation.orElse(null);
  }

  private void createAndSendDiscoveredPullRequestEvent(
      String applicationId,
      int pullRequestNumber,
      String branchName,
      PolicyEvaluation sourcePolicyEvaluation,
      PolicyEvaluation targetPolicyEvaluation)
  {
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId(applicationId)
        .setBranchName(branchName)
        .setCommitHash(sourcePolicyEvaluation.getCommitHash())
        .setEventType(SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT)
        .setPolicyEvaluationId(sourcePolicyEvaluation.getId())
        .setPullRequestNumber(pullRequestNumber)
        .setInitiator(POLLING);
    if (null != targetPolicyEvaluation) {
      event.setTargetPolicyEvaluationId(targetPolicyEvaluation.getId());
    }
    sourceControlEventService.publishEvent(event);
    log.info("Sent pull request discovered event for application '{}' with PR# '{}' and policy evaluation '{}'",
        applicationId, pullRequestNumber, event.getPolicyEvaluationId());
  }

  /**
   * cycles thru the source control applications in order of pull request poll times and queries the SCM provider for
   * pull requests for the org and api key associated with the given source control entry.
   *
   * @return list of pull requests discovered or an empty list if there are no new pull requests for any of the source
   * control applications
   * @throws IOException thrown if there is a problem fetching pull requests from the SCM provider
   */
  private List<PullRequest> getPullRequestsFromScm(PullRequestPollingTracker pollingTracker)
      throws IOException
  {
    List<PullRequest> pullRequests = new ArrayList<>();
    int apiCallCount = 0;

    // make sure all the pull request poll times are as they should be; prevents us from having to put complicated
    // logic in various places to make sure poll times are updated as necessary whenever source control entries are
    // manipulated
    pollingTracker.initializePullRequestPollTimes();

    // cycle thru the repos until we find some new pull requests or run out of repos to check
    while (pullRequests.size() < PULL_REQUESTS_PER_MONITOR_CYCLE && apiCallCount < MAX_API_REQUESTS_PER_CYCLE) {
      SourceControl sourceControl = pollingTracker.getNextRepositoryToPoll();
      if (null == sourceControl) {
        break;
      }

      GitRepositoryInfo gitRepositoryInfo =
          sourceControlUtils.getGitRepositoryInfoForApplication(sourceControl.getOwnerId());

      if (canPoll(gitRepositoryInfo)) {
        GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepositoryInfo);
        ProjectUri projectUri = gitApiClient.getProjectUri();
        String org = projectUri.getNamespace();

        // if a provider supports querying across the organization, we do not need a repo in context
        String repo =
            gitRepositoryInfo.provider.supportsOrganizationWidePullRequestQueries() ? null : projectUri.getProject();

        String token = gitRepositoryInfo.token;

        Date currentCutoffTime = pollingTracker.getCachedCutoffTime(org, repo, gitRepositoryInfo.token,
            sourceControl.getPullRequestPollTime());

        if (pollingTracker.visitAndCheckKeyAlreadyUsed(org, repo, token)) {
          // we've already used this key combination and any results for the given repo would have already come back;
          // so, we just need to advance the polling times for this repo
          pollingTracker.onPullRequestProcessed(sourceControl.getId(), org, repo, token, currentCutoffTime);
        }
        else {
          try {
            PullRequestInfoProvider client = gitClientFactory.createPullRequestInfoClient(gitRepositoryInfo);

            Date now = new Date();

            List<PullRequest> pullRequestResults = client.getPullRequestsSince(
                org,
                currentCutoffTime.toInstant().atOffset(ZoneOffset.UTC),
                PULL_REQUESTS_PER_MONITOR_CYCLE);

            pullRequests.addAll(pullRequestResults);

            if (pullRequestResults.isEmpty()) {
              pollingTracker.onPullRequestProcessed(sourceControl.getId(), org, repo, token, now);
            }
            else {
              currentCutoffTime = pullRequestResults.stream().map(PullRequest::getCreated).max(Date::compareTo).get();
              pollingTracker.onPullRequestProcessed(sourceControl.getId(), org, repo, token, currentCutoffTime);
            }

            apiCallCount++;
            log.debug("Fetched {} pull request(s) for org '{}' and repo '{}' since {}", pullRequests.size(),
                org, null == repo ? "none specified" : repo, currentCutoffTime);
          }
          catch (Exception e) {
            String retryDelay = pollingTracker.onErrorProcessingPullRequests(sourceControl.getId());
            log.error(
                "Error fetching pull requests for org '{}' and repo '{}'; will retry in {}.  Please check that the" +
                    " configured project url {} is correct, that it is for '{}' and that the API token is valid",
                org,
                null == repo ? "none specified" : repo,
                retryDelay,
                gitRepositoryInfo.repositoryUrl,
                gitRepositoryInfo.provider, e);
          }
        }
      }
      else {
        pollingTracker.onErrorProcessingPullRequests(sourceControl.getId());
      }
    }

    return pullRequests;
  }

  private boolean canPoll(GitRepositoryInfo gitRepositoryInfo) {
    if (null == gitRepositoryInfo || null == gitRepositoryInfo.provider) {
      return false;
    }
    if (!gitRepositoryInfo.provider.supportsPullRequestCommenting() || isBitbucketCloud(gitRepositoryInfo)) {
      if (log.isDebugEnabled()) {
        log.debug("{} is not currently supported for pull request commenting on repository {}",
            gitRepositoryInfo.provider.toString().toUpperCase(), gitRepositoryInfo.repositoryUrl);
      }
      return false;
    }
    return sourceControlUtils.isScmEnabled(gitRepositoryInfo);
  }

  /**
   * Pull request commenting features are not yet supported for Bitbucket cloud so provide logic to recognize any
   * repositories in that SCM.
   */
  private boolean isBitbucketCloud(GitRepositoryInfo gitRepositoryInfo) {
    return gitRepositoryInfo.provider.equals(BITBUCKET) &&
        BitbucketApiClientUtils.isCloudHosted(gitRepositoryInfo.repositoryUrl);
  }
}
