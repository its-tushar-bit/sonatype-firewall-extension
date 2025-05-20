/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestState;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.nexus.scm.api.BatchPullRequestInfoProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.NoSuchPullRequestException;
import com.sonatype.nexus.scm.api.PullRequestInfoProvider;
import com.sonatype.nexus.scm.api.model.PullRequestLifecycleInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor class to handle SourceControlEvents which signal that the status or one or several PRs should
 * be re-queried from SCM and the corresponding SourceControlPullRequests updated. This class does that querying
 * and updating.
 */
@Named
@Singleton
public class PullRequestStateEventHandler
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestStateEventHandler.class);

  private final GitClientFactory gitClientFactory;

  private final SourceControlUtils sourceControlUtils;

  private final SourceControlEventDAO sourceControlEventDAO;

  private final SourceControlPullRequestDAO sourceControlPullRequestDAO;

  @Inject
  public PullRequestStateEventHandler(
      final GitClientFactory gitClientFactory,
      final SourceControlUtils sourceControlUtils,
      final SourceControlEventDAO sourceControlEventDAO,
      final SourceControlPullRequestDAO sourceControlPullRequestDAO)
  {
    this.gitClientFactory = gitClientFactory;
    this.sourceControlUtils = sourceControlUtils;
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.sourceControlPullRequestDAO = sourceControlPullRequestDAO;
  }

  public void handle(final SourceControlEvent event) {
    var gitRepoInfo = sourceControlUtils.getGitRepositoryInfoForApplication(event.getApplicationId());
    PullRequestInfoProvider prInfoProvider = gitClientFactory.createPullRequestInfoClient(gitRepoInfo);
    GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepoInfo);
    String org = gitApiClient.getProjectUrl().getNamespace();
    String repository = gitApiClient.getProjectUrl().getProject();

    switch (event.getEventType()) {
      case SourceControlEvent.PR_STATE_UPDATE_EVENT:
        handleSinglePREvent(prInfoProvider, org, repository, event);
        break;
      case SourceControlEvent.BATCH_PR_STATE_UPDATE_EVENT:
        if (prInfoProvider instanceof BatchPullRequestInfoProvider batchPrInfoProvider) {
          handleBatchPREvent(batchPrInfoProvider, org, repository, event);
        }
        else {
          String message = String.format(
              "SourceControlEvent %s is a BATCH_PR_STATE_UPDATE_EVENT event but is " +
                  "for non-batch-capable SCM provider %s",
              event,
              prInfoProvider.getClass().getName()
          );
          throw new IllegalArgumentException(message);
        }
        break;
      default: // ignore
    }
  }

  private void handleSinglePREvent(
      PullRequestInfoProvider prInfoProvider,
      String org,
      String repository,
      SourceControlEvent event)
  {
    int prNumber = event.getPullRequestNumber();

    PullRequestLifecycleInfo prLifecycleInfo = null;
    boolean ioException = false;
    try {
      prLifecycleInfo =
          prInfoProvider.getPullRequestLifecycleInfoById(org, repository, prNumber);
    }
    catch (IOException e) {
      ioException = true;
      log.error(
          "Failed to obtain PullRequestLifecycleInfo from SCM for org {}, repository {}, pull request {} - reason: {}",
          org, repository, prNumber, e.getMessage());
    }
    catch (NoSuchPullRequestException noSuchPR) {
      prLifecycleInfo = null;
    }

    if (!ioException) {
      processPRLifecycleInfo(event.getApplicationId(), prNumber, prLifecycleInfo);
    }

    sourceControlEventDAO.delete(event);
  }

  private void handleBatchPREvent(
      BatchPullRequestInfoProvider prInfoProvider,
      String org,
      String repository,
      SourceControlEvent event)
  {
    Map<Integer, PullRequestLifecycleInfo> prInfoByNumber = null;
    int[] prNumbers = null;
    boolean ioException = false;
    try {
      prNumbers = JsonUtils.parse(event.getEventStatusDetails(), int[].class);

      if (prNumbers != null) {
        prInfoByNumber = prInfoProvider.getPullRequestLifecycleInfoByIds(org, repository, prNumbers);
      }
    }
    // Note that getPullRequestLifecycleInfoByIds only throws NoSuchPullRequestException if the entire org or repo
    // is missing. If individual PRs are missing they simply won't be in the results.
    catch (IOException e) {
      log.error(
          "Failed to obtain PullRequestLifecycleInfos from SCM for org {}, repository {} - reason: {}",
          org, repository, e.getMessage());

      ioException = true;
    }
    catch (NoSuchPullRequestException noSuchPR) {
      prInfoByNumber = Map.of();
    }

    if (!ioException) {
      for (int prNumber : prNumbers) {
        PullRequestLifecycleInfo prLifecycleInfo = prInfoByNumber.get(prNumber);
        processPRLifecycleInfo(event.getApplicationId(), prNumber, prLifecycleInfo);
      }
    }

    sourceControlEventDAO.delete(event);
  }

  private void processPRLifecycleInfo(
      final String applicationId,
      final int prNumber,
      final PullRequestLifecycleInfo prLifecycleInfo)
  {
    SourceControlPullRequest pullRequest = sourceControlPullRequestDAO.getByApplicationIdAndPullRequestId(
        applicationId,
        prNumber
    );

    if (pullRequest == null) {
      log.warn("Pull request {} not found in database, cannot update state", prNumber);
    }
    else {
      updateSourceControlPullRequest(pullRequest, prLifecycleInfo);
    }
  }

  private void updateSourceControlPullRequest(
      final SourceControlPullRequest pullRequest,
      final PullRequestLifecycleInfo prLifecycleInfo)
  {
    pullRequest.setLastCheckTime(new Date());

    if (prLifecycleInfo == null) {
      pullRequest.setState(PullRequestState.MISSING);
      pullRequest.setLastDetectedUpdateTime(new Date());
    }
    else {
      PullRequestState newState = PullRequestState.fromSCMState(prLifecycleInfo.getState());
      boolean hasChanges = false;

      if (pullRequest.getState() != newState) {
        pullRequest.setState(newState);
        hasChanges = true;
      }

      if (!Objects.equals(pullRequest.getHeadCommitHash(), prLifecycleInfo.getHeadCommitHash())) {
        pullRequest.setHeadCommitHash(prLifecycleInfo.getHeadCommitHash());
        hasChanges = true;
      }

      // Note: gitlab doesn't have this info in the lifecycle response hence the extra null check
      if (prLifecycleInfo.getBaseCommitHash() != null &&
          !Objects.equals(pullRequest.getBaseCommitHash(), prLifecycleInfo.getBaseCommitHash())) {
        pullRequest.setBaseCommitHash(prLifecycleInfo.getBaseCommitHash());
        hasChanges = true;
      }

      if (!Objects.equals(pullRequest.getBranchName(), prLifecycleInfo.getBranchName())) {
        pullRequest.setBranchName(prLifecycleInfo.getBranchName());
        hasChanges = true;
      }

      if (!Objects.equals(pullRequest.getBaseBranchName(), prLifecycleInfo.getBaseBranchName())) {
        pullRequest.setBaseBranchName(prLifecycleInfo.getBaseBranchName());
        hasChanges = true;
      }

      if (hasChanges) {
        pullRequest.setLastDetectedUpdateTime(new Date());
      }
    }

    sourceControlPullRequestDAO.update(pullRequest);
  }
}
