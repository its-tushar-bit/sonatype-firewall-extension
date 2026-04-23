/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestState;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlLoadBalancer;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.PullRequestInfoProvider;
import com.sonatype.nexus.scm.api.model.ProjectUrl;
import com.sonatype.nexus.scm.api.model.PullRequest;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Named
@Singleton
public class PullRequestPollingService
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestPollingService.class);

  private static final String SCM_ANONYMOUS_POLLER = "anonymous-poller";

  @VisibleForTesting
  static final int PULL_REQUESTS_PER_MONITOR_CYCLE = 50;

  private static final int MAX_API_REQUESTS_PER_CYCLE = 50;

  // Safety cap on total source_control entries processed per cycle. When all repos share the same
  // org-wide key (common with GitHub App), only 1 SCM API call is made but the loop still iterates
  // through every repo doing DB work. This cap prevents runaway cycles.
  @VisibleForTesting
  static final int MAX_REPOS_PER_CYCLE = 500;

  // After successful processing, repos have their poll time pushed this far into the future so they
  // are not immediately eligible for the next cycle. This matches the scheduler's discovery
  // interval, ensuring each repo is polled at most once per interval.
  @VisibleForTesting
  static final long POLL_INTERVAL_MS = PullRequestPollingScheduler.PULL_REQUEST_DISCOVERY_INTERVAL_SECONDS * 1000L;

  // How long a positive canPollForPullRequests result can be reused from the per-cycle cache
  // before we must consult the load balancer again. This MUST stay comfortably below the
  // partition-reservation safety window used by SelfThrottlingLoadBalancer (60s for SCM), so the
  // cache never reports 'permission granted' after the underlying DB reservation could have
  // lapsed. A long-running cycle (many slow SCM API calls) will cross this boundary and the next
  // lookup will fall through to the load balancer; the load balancer's own renewal cache will
  // usually satisfy that call in-memory without any DB write.
  @VisibleForTesting
  static final long CAN_POLL_CACHE_TTL_MILLIS = 30_000L;

  @VisibleForTesting
  public LongSupplier nowMillisSupplierForTesting = System::currentTimeMillis;

  private static final String POLLING = "polling";

  private final ApplicationDAO applicationDAO;

  private final SourceControlDAO sourceControlDAO;

  private final SourceControlPullRequestDAO sourceControlPullRequestDAO;

  private final SourceControlEventPublisher sourceControlEventPublisher;

  private final SourceControlUtils sourceControlUtils;

  private final GitClientFactory gitClientFactory;

  private final RemediationBranchNamePrefixGenerator remediationBranchNamePrefixGenerator =
      new RemediationBranchNamePrefixGenerator();

  private final SourceControlLoadBalancer sourceControlLoadBalancer;

  private final IqForScmLicenseChecker licenseChecker;

  private final PullRequestCommentingEligibilityValidator pullRequestCommentingEligibilityValidator;

  private final ScmRepoVisibilityService scmRepoVisibilityService;

  @Inject
  public PullRequestPollingService(
      ApplicationDAO applicationDAO,
      SourceControlDAO sourceControlDAO,
      SourceControlPullRequestDAO sourceControlPullRequestDAO,
      SourceControlEventPublisher sourceControlEventPublisher,
      SourceControlUtils sourceControlUtils,
      GitClientFactory gitClientFactory,
      ScmRepoVisibilityService scmRepoVisibilityService,
      SourceControlLoadBalancer sourceControlLoadBalancer,
      IqForScmLicenseChecker licenseChecker,
      PullRequestCommentingEligibilityValidator pullRequestCommentingEligibilityValidator)
  {
    this.applicationDAO = applicationDAO;
    this.sourceControlDAO = sourceControlDAO;
    this.sourceControlPullRequestDAO = sourceControlPullRequestDAO;
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.sourceControlUtils = sourceControlUtils;
    this.gitClientFactory = gitClientFactory;
    this.sourceControlLoadBalancer = sourceControlLoadBalancer;
    this.licenseChecker = licenseChecker;
    this.pullRequestCommentingEligibilityValidator = pullRequestCommentingEligibilityValidator;
    this.scmRepoVisibilityService = scmRepoVisibilityService;
  }

  public void fetchAndSendPullRequestsForCommenting() {
    if (!licenseChecker.isPullRequestCommentingSupported()) {
      log.trace("License does not support source control automation feature");
      return;
    }

    PullRequestPollingTracker pollingTracker =
        new PullRequestPollingTracker(sourceControlDAO, MAX_API_REQUESTS_PER_CYCLE);

    // the pull requests we get back can be for any app that the related org and key have access to
    List<PullRequest> pullRequests = getPullRequestsFromScm(pollingTracker);
    for (PullRequest pullRequest : pullRequests) {
      // we'll check all apps associated with the pull request's repository
      List<Application> applications = applicationDAO.getByRepositoryUrl(pullRequest.getRepository());
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
        else if (!scmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(gitRepositoryInfo)) {
          log.debug("Repository is not valid for pull requests, check that it is private/internal: {}",
              gitRepositoryInfo.getRepositoryUrl());
        }
        else if (isPullRequestForBaseBranch(pullRequest, gitRepositoryInfo)) {
          log.debug(
              "Repository '{}' pull request '{}' is for application '{}' base branch, skipping commenting",
              gitRepositoryInfo.getRepositoryUrl(), pullRequest.getNumber(), app.getPublicId());
        }
        else {
          String repositoryUrl = gitRepositoryInfo.repositoryUrl;
          int pullRequestId = pullRequest.getNumber();
          if (sourceControlPullRequestDAO.getByRepositoryUrlAndPullRequestId(repositoryUrl, pullRequestId) == null) {
            persistPullRequest(pullRequest);
          }
          createAndSendDiscoveredPullRequestEvent(app.getId(), pullRequest);
        }

        // No per-app poll time update needed here: onPullRequestProcessed already advanced
        // poll_request_poll_time for all records sharing this repo URL to now + POLL_INTERVAL_MS,
        // and the SCM query cutoff is tracked separately in the keyCutoffTimes cache.
      }
    }
  }

  private void persistPullRequest(PullRequest pullRequest) {
    SourceControlPullRequest sourceControlPullRequest =
        new SourceControlPullRequest(pullRequest.getRepository(), pullRequest.getNumber(),
            pullRequest.getHeadCommitHash(), pullRequest.getBaseCommitHash(), pullRequest.getHead(),
            pullRequest.getBase(), pullRequest.getCreated(), new Date(), new Date(),
            PullRequestState.fromSCMState(pullRequest.getState()), PullRequestSource.EXTERNAL);
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

  public void createAndSendPullRequestClosingEvent(
      String applicationId,
      SourceControlPullRequest pullRequest,
      String pullRequestContents)
  {
    SourceControlEvent event = new SourceControlEvent()
        .forRemediationPullRequestClosing()
        .setBranchName(pullRequest.getBranchName())
        .setApplicationId(applicationId)
        .setPullRequestNumber(pullRequest.getPullRequestId())
        .setPullRequestContents(pullRequestContents)
        .setInitiator(POLLING);
    sourceControlEventPublisher.publishEvent(event);
    log.info("Sent pull request closing event for application '{}' with PR# '{}'",
        applicationId, pullRequest.getPullRequestId());
  }

  /**
   * cycles thru the source control applications in order of pull request poll times and queries the SCM provider for
   * pull requests for the org and api key associated with the given source control entry.
   *
   * @return list of pull requests discovered or an empty list if there are no new pull requests for any of the source
   *         control applications
   */
  private List<PullRequest> getPullRequestsFromScm(PullRequestPollingTracker pollingTracker) {
    List<PullRequest> pullRequests = new ArrayList<>();
    int apiCallCount = 0;

    // Cache positive canPollForPullRequests results per scmUsername for the duration of this
    // cycle, with a TTL to ensure the cache never outlives the underlying DB partition
    // reservation.
    //
    // The load balancer's canPollForPullRequests does a SELECT FOR UPDATE + UPDATE on the global
    // perpetual_lock table (via SelfThrottlingLoadBalancer.canUsePartition). In MTIQ SaaS the
    // same scmUsername typically maps to many -- sometimes hundreds -- of source_control rows
    // (an org-level SCM token that every inheriting app uses). Without caching, a healthy cohort
    // of active customers drives N-per-repo write churn on the shared lock table every cycle.
    //
    // The TTL matters because a cycle can legitimately run longer than the partition reservation
    // window (MAX_API_REQUESTS_PER_CYCLE = 50 SCM calls, each potentially seconds of latency).
    // If the cache held a positive result past the reservation's expiry, we could hand out
    // 'permission to poll' while another instance had already re-acquired the partition. Values
    // in this map are epoch-millis deadlines; entries are only trusted while now < deadline.
    Map<String, Long> canPollValidUntilMillis = new HashMap<>();

    // make sure all the pull request poll times are as they should be; prevents us from having to put complicated
    // logic in various places to make sure poll times are updated as necessary whenever source control entries are
    // manipulated
    pollingTracker.initializePullRequestPollTimes();

    // cycle thru the repos until we find some new pull requests or run out of repos to check
    int repoCount = 0;
    while (pullRequests.size() < PULL_REQUESTS_PER_MONITOR_CYCLE && apiCallCount < MAX_API_REQUESTS_PER_CYCLE
        && repoCount < MAX_REPOS_PER_CYCLE)
    {
      SourceControl sourceControl = pollingTracker.getNextRepositoryToPoll();
      if (null == sourceControl) {
        break;
      }
      repoCount++;

      GitRepositoryInfo gitRepositoryInfo =
          sourceControlUtils.getGitRepositoryInfoForApplication(sourceControl.getOwnerId());

      // skip repositories for which PR commenting is disabled
      if (!pullRequestCommentingEligibilityValidator.isPullRequestCommentingEnabled(gitRepositoryInfo)) {
        sourceControl.setPullRequestPollTime(new Date());

        // This sourceControl might not be valid if it's for an app inheriting from an org that doesn't have SCM
        // configured. In that case we still want to update its polling time so that the next call to
        // pollingTracker.getNextRepositoryToPoll works, but we need to bypass validation to do so.
        sourceControlDAO.updateWithoutValidation(sourceControl);
        continue;
      }

      if (canPoll(gitRepositoryInfo, canPollValidUntilMillis)) {
        String org = null;
        String repo = null;
        try {
          GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepositoryInfo);
          ProjectUrl projectUri = gitApiClient.getProjectUrl();
          org = projectUri.getNamespace();
          // if a provider supports querying across the organization, we do not need a repo in context
          repo =
              gitRepositoryInfo.provider.supportsOrganizationWidePullRequestQueries() ? null : projectUri.getProject();

          String token = gitRepositoryInfo.token;

          Date currentCutoffTime =
              pollingTracker.getCachedCutoffTime(org, repo, token, sourceControl.getPullRequestPollTime());

          Date nextPollTime = new Date(nowMillisSupplierForTesting.getAsLong() + POLL_INTERVAL_MS);

          if (pollingTracker.visitAndCheckKeyAlreadyUsed(org, repo, token)) {
            // We've already polled this org+token combo — just advance this repo's poll eligibility
            // with a lightweight single-row update, skipping the expensive getByRepositoryUrl fan-out.
            sourceControlDAO.updatePollTimeAndErrorCounts(sourceControl.getId(), nextPollTime, 0);
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
              pollingTracker.onPullRequestProcessed(sourceControl, org, repo, token, now, nextPollTime);
            }
            else {
              currentCutoffTime = pullRequestResults.stream().map(PullRequest::getCreated).max(Date::compareTo).get();
              pollingTracker.onPullRequestProcessed(sourceControl, org, repo, token, currentCutoffTime, nextPollTime);
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

  private boolean canPoll(GitRepositoryInfo gitRepositoryInfo, Map<String, Long> canPollValidUntilMillis) {
    if (!sourceControlUtils.isScmEnabled(gitRepositoryInfo)) {
      log.debug("PR polling will be skipped for given repo due to incomplete gitRepositoryInfo:%n" +
          "  url={}%n  provider={}%n  has token={}%n  has username={}",
          gitRepositoryInfo.repositoryUrl,
          gitRepositoryInfo.provider,
          StringUtils.isNotBlank(gitRepositoryInfo.token) ? "true" : "false",
          StringUtils.isNotBlank(gitRepositoryInfo.username) ? "true" : "false");
      return false;
    }
    if (!gitRepositoryInfo.provider.supportsPullRequestCommenting() ||
        sourceControlUtils.isBitbucketCloud(gitRepositoryInfo))
    {
      if (log.isDebugEnabled()) {
        log.debug("{} is not currently supported for pull request commenting on repository {}",
            gitRepositoryInfo.provider.toString().toUpperCase(), gitRepositoryInfo.normalizedRepositoryUrl);
      }
      return false;
    }

    // Consult the per-cycle cache before paying for a perpetual_lock write. The cache only
    // remembers positive results and only for CAN_POLL_CACHE_TTL_MILLIS, ensuring it never
    // outlives the underlying DB reservation. See getPullRequestsFromScm for the full rationale.
    String scmUsername = getScmUsernameForPolling(gitRepositoryInfo);
    long now = nowMillisSupplierForTesting.getAsLong();
    Long validUntil = canPollValidUntilMillis.get(scmUsername);
    if (validUntil != null && now < validUntil) {
      return true;
    }
    boolean result = sourceControlLoadBalancer.canPollForPullRequests(scmUsername);
    if (result) {
      canPollValidUntilMillis.put(scmUsername, now + CAN_POLL_CACHE_TTL_MILLIS);
    }
    else {
      canPollValidUntilMillis.remove(scmUsername);
    }
    return result;
  }

  private String getScmUsernameForPolling(GitRepositoryInfo gitRepositoryInfo) {
    return StringUtils.isNotBlank(gitRepositoryInfo.getUsername())
        ? gitRepositoryInfo.getUsername()
        : String.format("%s-%s", gitRepositoryInfo.provider, SCM_ANONYMOUS_POLLER);
  }
}
