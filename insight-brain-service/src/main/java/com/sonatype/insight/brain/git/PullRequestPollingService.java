/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.PullRequestInfoProvider;
import com.sonatype.nexus.scm.api.model.ProjectUri;
import com.sonatype.nexus.scm.api.model.PullRequest;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Named
@Singleton
public class PullRequestPollingService
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestPollingService.class);

  @VisibleForTesting
  static final int PULL_REQUESTS_PER_MONITOR_CYCLE = 50;

  private static final int MAX_API_REQUESTS_PER_CYCLE = 50;

  private static final String POLLING = "polling";

  private final ApplicationDAO applicationDAO;

  private final SourceControlDAO sourceControlDAO;

  private final SourceControlPullRequestDAO sourceControlPullRequestDAO;

  private final SourceControlEventPublisher sourceControlEventPublisher;

  private final SourceControlUtils sourceControlUtils;

  private final GitClientFactory gitClientFactory;

  private final PullRequestRepositoryValidator pullRequestRepositoryValidator;

  private final RemediationBranchNamePrefixGenerator remediationBranchNamePrefixGenerator =
      new RemediationBranchNamePrefixGenerator();

  private final SourceControlInstanceManager sourceControlInstanceManager;

  private final IqForScmLicenseChecker licenseChecker;

  private final PullRequestCommentingEligibilityValidator pullRequestCommentingEligibilityValidator;

  @Inject
  public PullRequestPollingService(
      ApplicationDAO applicationDAO,
      SourceControlDAO sourceControlDAO,
      SourceControlPullRequestDAO sourceControlPullRequestDAO,
      SourceControlEventPublisher sourceControlEventPublisher,
      SourceControlUtils sourceControlUtils,
      GitClientFactory gitClientFactory,
      PullRequestRepositoryValidator pullRequestRepositoryValidator,
      SourceControlInstanceManager sourceControlInstanceManager,
      IqForScmLicenseChecker licenseChecker,
      PullRequestCommentingEligibilityValidator pullRequestCommentingEligibilityValidator)
  {
    this.applicationDAO = applicationDAO;
    this.sourceControlDAO = sourceControlDAO;
    this.sourceControlPullRequestDAO = sourceControlPullRequestDAO;
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.sourceControlUtils = sourceControlUtils;
    this.gitClientFactory = gitClientFactory;
    this.pullRequestRepositoryValidator = pullRequestRepositoryValidator;
    this.sourceControlInstanceManager = sourceControlInstanceManager;
    this.licenseChecker = licenseChecker;
    this.pullRequestCommentingEligibilityValidator = pullRequestCommentingEligibilityValidator;
  }

  public void fetchAndSendPullRequestsForCommenting() {
    if (!licenseChecker.isPullRequestCommentingSupported()) {
      log.trace("License does not support source control automation feature");
      return;
    }

    // for now this is a global check;  future plan is to base this on specific tokens/repos/users, in which case
    // we can push this check down into this class' canPoll() method
    if (!sourceControlInstanceManager.canPoll()) {
      log.trace("This instance is not allowed to poll.  Skipping.");
      return;
    }

    PullRequestPollingTracker pollingTracker = new PullRequestPollingTracker(sourceControlDAO);

    // the pull requests we get back can be for any app that the related org and key have access to
    List<PullRequest> pullRequests = getPullRequestsFromScm(pollingTracker);
    for (PullRequest pullRequest : pullRequests) {
      // we'll check all apps associated with the pull request's repository
      List<Application> applications = applicationDAO.getByRepositoryUrl(pullRequest.getRepository());
      boolean pullRequestWasPersisted = false;
      for (Application app : applications) {
        GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(app.getId());

        if (!pullRequestCommentingEligibilityValidator.isPullRequestCommentingEnabled(gitRepositoryInfo)) {
          log.trace("Pull request commenting is disabled for application '{}'. We will not comment on it.",
              app.getName());
        }
        else if (isRemediationPullRequest(pullRequest, app)) {
          log.debug("Pull request {} for branch {} is determined to be an IQ Server generated remediation PR." +
                  "  We will not comment on it.",
              pullRequest.getNumber(), pullRequest.getHead());
        }
        else if (!pullRequestRepositoryValidator.isInternalRepository(gitRepositoryInfo) &&
            !pullRequest.isRepositoryPrivate()) {
          log.debug("Repository is not valid for pull requests, check that it is private: {}",
              gitRepositoryInfo.getRepositoryUrl());
        }
        else if (isPullRequestForBaseBranch(pullRequest, gitRepositoryInfo)) {
          log.debug(
              "Repository '{}' pull request '{}' is for application '{}' base branch, skipping commenting",
              gitRepositoryInfo.getRepositoryUrl(), pullRequest.getNumber(), app.getPublicId());
        }
        else {
          if (!pullRequestWasPersisted) {
            persistPullRequest(pullRequest);
            pullRequestWasPersisted = true;
          }
          createAndSendDiscoveredPullRequestEvent(app.getId(), pullRequest);
        }

        pollingTracker.onPullRequestProcessedForApplication(app.getId(), pullRequest.getCreated());
        log.debug("Pull request polling time updated for '{}' to {}", pullRequest.getRepository(),
            pullRequest.getCreated());
      }
    }
  }

  private void persistPullRequest(PullRequest pullRequest) {
    SourceControlPullRequest sourceControlPullRequest =
        new SourceControlPullRequest(pullRequest.getRepository(), pullRequest.getNumber(),
            pullRequest.getHeadCommitHash(), pullRequest.getBaseCommitHash(),
            pullRequest.getHead(), pullRequest.getBase(),
            pullRequest.getCreated(), new Date(), new Date());
    sourceControlPullRequestDAO.insert(sourceControlPullRequest);
  }

  /**
   * Determines whether or not the given pull request is a remediation PR created by IQ Server
   */
  private boolean isRemediationPullRequest(PullRequest pullRequest, Application application) {
    return pullRequest.getHead()
        .startsWith(remediationBranchNamePrefixGenerator.generatePrefixForApplication(application.getId()));
  }

  /**
   * Does the given pull request represent a merge from the configured default/base branch into some other branch?
   * We don't support those types of PRs with respect to PR commenting
   */
  private boolean isPullRequestForBaseBranch(PullRequest pullRequest, GitRepositoryInfo gitRepositoryInfo) {
    return pullRequest.getHead().equalsIgnoreCase(gitRepositoryInfo.baseBranch);
  }

  private void createAndSendDiscoveredPullRequestEvent(
      String applicationId,
      PullRequest pullRequest)
  {
    SourceControlEvent event = new SourceControlEvent()
        .forDiscoveredPullRequest()
        .setApplicationId(applicationId)
        .setBranchName(pullRequest.getHead())
        .setCommitHash(pullRequest.getHeadCommitHash())
        .setBaseCommitHash(pullRequest.getBaseCommitHash())
        .setBaseBranchName(pullRequest.getBase())
        .setPullRequestNumber(pullRequest.getNumber())
        .setInitiator(POLLING);
    sourceControlEventPublisher.publishEvent(event);
    log.info("Sent pull request discovered event for application '{}' with PR# '{}' and commit '{}'",
        applicationId, pullRequest.getNumber(), pullRequest.getHeadCommitHash());
  }

  /**
   * cycles thru the source control applications in order of pull request poll times and queries the SCM provider for
   * pull requests for the org and api key associated with the given source control entry.
   *
   * @return list of pull requests discovered or an empty list if there are no new pull requests for any of the source
   * control applications
   */
  private List<PullRequest> getPullRequestsFromScm(PullRequestPollingTracker pollingTracker) {
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

      // skip repositories for which PR commenting is disabled
      if (!pullRequestCommentingEligibilityValidator.isPullRequestCommentingEnabled(gitRepositoryInfo)) {
        sourceControl.setPullRequestPollTime(new Date());
        sourceControlDAO.update(sourceControl);
        continue;
      }

      if (canPoll(gitRepositoryInfo)) {
        String org = null;
        String repo = null;
        try {
          GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepositoryInfo);
          ProjectUri projectUri = gitApiClient.getProjectUri();
          org = projectUri.getNamespace();
          // if a provider supports querying across the organization, we do not need a repo in context
          repo =
              gitRepositoryInfo.provider.supportsOrganizationWidePullRequestQueries() ? null : projectUri.getProject();
  
          String token = gitRepositoryInfo.token;
  
          Date currentCutoffTime =
              pollingTracker.getCachedCutoffTime(org, repo, token, sourceControl.getPullRequestPollTime());
  
          if (pollingTracker.visitAndCheckKeyAlreadyUsed(org, repo, token)) {
            // we've already used this key combination and any results for the given repo would have already come back.
            // so, we just need to advance the polling times for this repo
            pollingTracker.onPullRequestProcessed(sourceControl, org, repo, token, currentCutoffTime);
          }
          else {
            PullRequestInfoProvider client = gitClientFactory.createPullRequestInfoClient(gitRepositoryInfo);

            Date now = new Date();

            List<PullRequest> pullRequestResults = client.getPullRequestsSince(
                org,
                currentCutoffTime.toInstant().atOffset(ZoneOffset.UTC),
                PULL_REQUESTS_PER_MONITOR_CYCLE);

            log.debug("Fetched {} pull request(s) for org '{}'{} since {}",
                pullRequestResults.size(),
                org,
                isNotBlank(repo) ? format(" repo '%s'", repo) : "",
                currentCutoffTime);

            if (CollectionUtils.isNotEmpty(pullRequestResults)) {
              pullRequests.addAll(pullRequestResults);
            }

            if (pullRequestResults.isEmpty()) {
              pollingTracker.onPullRequestProcessed(sourceControl, org, repo, token, now);
            }
            else {
              currentCutoffTime = pullRequestResults.stream().map(PullRequest::getCreated).max(Date::compareTo).get();
              pollingTracker.onPullRequestProcessed(sourceControl, org, repo, token, currentCutoffTime);
            }

            apiCallCount++;
          }
        }
        catch (Exception e) {
          String retryDelay = pollingTracker.onErrorProcessingPullRequests(sourceControl);
          if (isNotBlank(repo)) {
            log.warn(
                "Could not fetch pull requests for org '{}' repo '{}'; will retry in {}.  Please check that the" +
                    " configured project url {} is correct, that it is for '{}' and that the API token is valid",
                org, repo, retryDelay, gitRepositoryInfo.normalizedRepositoryUrl, gitRepositoryInfo.provider, e);
          }
          else {
            log.warn(
                "Could not fetch pull requests for org '{}'; will retry in {}.  Please check that the" +
                    " configured project url {} is correct, that it is for '{}' and that the API token is valid",
                org, retryDelay, gitRepositoryInfo.normalizedRepositoryUrl, gitRepositoryInfo.provider, e);
          }
        }
      }
      else {
        pollingTracker.onErrorProcessingPullRequests(sourceControl);
      }
    }

    return pullRequests;
  }

  private boolean canPoll(GitRepositoryInfo gitRepositoryInfo) {
    if (null == gitRepositoryInfo || null == gitRepositoryInfo.provider) {
      return false;
    }
    if (!gitRepositoryInfo.provider.supportsPullRequestCommenting() ||
        sourceControlUtils.isBitbucketCloud(gitRepositoryInfo)) {
      if (log.isDebugEnabled()) {
        log.debug("{} is not currently supported for pull request commenting on repository {}",
            gitRepositoryInfo.provider.toString().toUpperCase(), gitRepositoryInfo.normalizedRepositoryUrl);
      }
      return false;
    }
    return sourceControlUtils.isScmEnabled(gitRepositoryInfo);
  }
}
