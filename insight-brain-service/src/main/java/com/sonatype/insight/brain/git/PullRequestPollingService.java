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
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.GitGraphQlApiClient;
import com.sonatype.nexus.scm.api.model.ProjectUri;
import com.sonatype.nexus.scm.api.model.PullRequest;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class PullRequestPollingService
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestPollingService.class);

  @VisibleForTesting
  static final int PULL_REQUESTS_PER_MONITOR_CYCLE = 10;

  private static final int MAX_API_REQUESTS_PER_CYCLE = 10;

  private final SourceControlDAO sourceControlDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final GitCommitHistoryService gitCommitHistoryService;

  private final SourceControlUtils sourceControlUtils;

  private final GitClientFactory gitClientFactory;

  private final AsyncEventBus asyncEventBus;

  private final PullRequestUtils pullRequestUtils;

  @Inject
  public PullRequestPollingService(
      SourceControlDAO sourceControlDAO,
      PolicyEvaluationDAO policyEvaluationDAO,
      GitCommitHistoryService gitCommitHistoryService,
      SourceControlUtils sourceControlUtils,
      GitClientFactory gitClientFactory,
      AsyncEventBus asyncEventBus,
      PullRequestUtils pullRequestUtils)
  {
    this.sourceControlDAO = sourceControlDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.gitCommitHistoryService = gitCommitHistoryService;
    this.sourceControlUtils = sourceControlUtils;
    this.gitClientFactory = gitClientFactory;
    this.asyncEventBus = asyncEventBus;
    this.pullRequestUtils = pullRequestUtils;
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
          if (!pullRequestUtils.isEffectivelyPrivate(gitRepositoryInfo, pullRequest.isRepositoryPrivate())) {
            log.debug("Repository is not private: {}", gitRepositoryInfo.repositoryUrl);
          }
          else {
            if (!isPullRequestForBaseBranch(pullRequest, gitRepositoryInfo)) {
              PolicyEvaluation targetPolicyEvaluation = getLatestPolicyEvaluationForBaseBranch(applicationId);
              createAndSendDiscoveredPullRequestEvent(applicationId, pullRequest.getNumber(), sourcePolicyEvaluation,
                  targetPolicyEvaluation);
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
      PolicyEvaluation sourcePolicyEvaluation,
      PolicyEvaluation targetPolicyEvaluation)
  {
    DiscoveredPullRequestEvent event = new DiscoveredPullRequestEvent();
    event.applicationId = applicationId;
    event.commitHash = sourcePolicyEvaluation.getCommitHash();
    event.policyEvaluationId = sourcePolicyEvaluation.getId();
    event.pullRequestNumber = pullRequestNumber;
    if (null != targetPolicyEvaluation) {
      event.targetPolicyEvaluationId = targetPolicyEvaluation.getId();
    }
    event.initiator = PullRequestPollingService.class.getSimpleName();
    asyncEventBus.post(event);
    log.info("Sent pull request discovered event for application '{}' with PR# '{}' and policy evaluation '{}'",
        applicationId, pullRequestNumber, event.policyEvaluationId);
  }

  /**
   * cycles thru the source control applications in order of pull request poll times and queries the SCM provider
   * for pull requests for the org and api key associated with the given source control entry.
   *
   * @return list of pull requests discovered or an empty list if there are no new pull requests for any of the
   * source control applications
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

        if (!pollingTracker.visitAndCheckOrganizationWithToken(projectUri.getNamespace(), gitRepositoryInfo.token)) {
          try {
            GitGraphQlApiClient graphqlApiClient = gitClientFactory.createGraphqlApiClient(gitRepositoryInfo);

            List<PullRequest> pullRequestsForOrg = graphqlApiClient.getPullRequestsSince(
                projectUri.getNamespace(),
                sourceControl.getPullRequestCutoffTime().toInstant().atOffset(ZoneOffset.UTC),
                PULL_REQUESTS_PER_MONITOR_CYCLE);

            pullRequests.addAll(pullRequestsForOrg);

            if (pullRequestsForOrg.isEmpty()) {
              pollingTracker.onPullRequestProcessed(sourceControl.getId(), new Date());
            }
            else {
              Date maxDate = pullRequestsForOrg.stream().map(PullRequest::getCreated).max(Date::compareTo).get();
              pollingTracker.onPullRequestProcessed(sourceControl.getId(), maxDate);
            }

            apiCallCount++;
            log.debug("Fetched {} pull request(s) for org '{}'", pullRequests.size(), projectUri.getNamespace());
          }
          catch (Exception e) {
            String retryDelay = pollingTracker.onErrorProcessingPullRequests(sourceControl.getId());
            log.error(String.format(
                "Error fetching pull requests for org '%s', will retry in %s.  Please check that the" +
                    " configured project url '%s' is correct, that it is for '%s' and that the API token is valid",
                projectUri.getNamespace(),
                retryDelay,
                gitRepositoryInfo.repositoryUrl,
                gitRepositoryInfo.provider), e);
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
    if (null == gitRepositoryInfo) {
      return false;
    }
    if (SourceControlProvider.GITHUB != gitRepositoryInfo.provider) {
      if (log.isDebugEnabled()) {
        log.debug("{} is not currently supported for pull request commenting",
            gitRepositoryInfo.provider.toString().toUpperCase());
      }
      return false;
    }
    return sourceControlUtils.isScmEnabled(gitRepositoryInfo);
  }
}
