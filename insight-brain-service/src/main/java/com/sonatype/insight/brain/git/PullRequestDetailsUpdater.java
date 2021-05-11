/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.lifecycle.Managed;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * @since 1.114
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class PullRequestDetailsUpdater
    implements Managed, Job
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestDetailsUpdater.class);

  static final String TASK_NAME = "PullRequestDetailsUpdater";

  private static final int THREAD_POOL_SIZE = 20;

  private final InsightConfig insightConfig;

  private final TaskScheduler taskScheduler;

  private final ApplicationDAO applicationDAO;

  private final SourceControlPullRequestDAO sourceControlPullRequestDAO;

  private final GitApiFactory gitApiFactory;

  private final SourceControlUtils sourceControlUtils;

  private final PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver;

  private final SourceControlEventPublisher sourceControlEventPublisher;

  private ExecutorService executorService;

  public boolean disableForTesting;

  @Inject
  public PullRequestDetailsUpdater(
      InsightConfig insightConfig,
      TaskScheduler taskScheduler,
      GitApiFactory gitApiFactory,
      SourceControlUtils sourceControlUtils,
      PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver,
      SourceControlEventPublisher sourceControlEventPublisher,
      ApplicationDAO applicationDAO,
      SourceControlPullRequestDAO sourceControlPullRequestDAO)
  {
    this.insightConfig = insightConfig;
    this.taskScheduler = taskScheduler;
    this.gitApiFactory = gitApiFactory;
    this.sourceControlUtils = sourceControlUtils;
    this.pullRequestPolicyEvaluationResolver = pullRequestPolicyEvaluationResolver;
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.applicationDAO = applicationDAO;
    this.sourceControlPullRequestDAO = sourceControlPullRequestDAO;
  }

  private ExecutorService getExecutorService() {
    if (executorService == null) {
      ThreadFactory threadFactory =
          new ThreadFactoryBuilder().setDaemon(true).setNameFormat("PullRequestDetailsUpdater-%s").build();
      ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 5L,
          TimeUnit.MINUTES, new LinkedBlockingQueue<>(), threadFactory);
      threadPoolExecutor.allowCoreThreadTimeOut(true);
      executorService = threadPoolExecutor;
    }
    return executorService;
  }

  @Override
  public void start() throws Exception {
    if (disableForTesting) {
      return;
    }

    if (!insightConfig.isExperimentalFeatureEnabled(InsightConfig.Feature.PR_COMMENT_MONITORING)) {
      return;
    }

    int intervalInSeconds = insightConfig.getPullRequestDetailsUpdateIntervalInSeconds();
    taskScheduler.schedulePeriodicTask(PullRequestDetailsUpdater.class, TASK_NAME,
        Duration.ofSeconds(intervalInSeconds));
    log.debug("Scheduled PullRequestDetailsUpdater, interval={} seconds.", intervalInSeconds);
  }

  @Override
  public void execute(JobExecutionContext context) {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      updatePullRequestDetails();
    }
    catch (Exception e) {
      log.error("Error when updating pull request details: {}", e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(1);
    }
  }

  // Visible for tests
  void updatePullRequestDetails() {
    long start = System.currentTimeMillis();
    log.debug("Updating pull request details.");

    List<SourceControlPullRequest> allPullRequests = sourceControlPullRequestDAO.getAll();

    Map<String, List<SourceControlPullRequest>> pullRequestsByRepositoryUrl =
        allPullRequests.stream().collect(groupingBy(SourceControlPullRequest::getRepositoryUrl, toList()));
    Set<String> repositoryUrls =
        allPullRequests.stream().map(SourceControlPullRequest::getRepositoryUrl).collect(toSet());
    List<PullRequestDetailsUpdaterTask> tasks = new ArrayList<>();
    for (String repositoryUrl : repositoryUrls) {
      tasks.add(new PullRequestDetailsUpdaterTask(repositoryUrl, pullRequestsByRepositoryUrl.get(repositoryUrl)));
    }
    try {
      getExecutorService().invokeAll(tasks);
    }
    catch (InterruptedException e) {
      log.error("Interrupted while updating pull request details", e);
      Thread.currentThread().interrupt();
    }

    log.debug("Updated pull request details in {} ms.", System.currentTimeMillis() - start);
  }

  @Override
  public void stop() {
    // no-op
  }

  private class PullRequestDetailsUpdaterTask
      implements Callable<Void>
  {
    private final String repositoryUrl;
    
    private final List<SourceControlPullRequest> pullRequestsForRepository;

    PullRequestDetailsUpdaterTask(String repositoryUrl, List<SourceControlPullRequest> pullRequestsForRepository) {
      this.repositoryUrl = repositoryUrl;
      this.pullRequestsForRepository = pullRequestsForRepository;
    }

    @Override
    public Void call() {
      List<Application> applications = applicationDAO.getByRepositoryUrl(repositoryUrl);
      if (applications.isEmpty()) {
        // Can happen if the corresponding app was deleted or updated after we retrieved all pull requests
        return null;
      }

      List<SourceControlPullRequest> closedPullRequests = new ArrayList<>(pullRequestsForRepository);
      Map<String, List<SourceControlPullRequest>> pullRequestsByBranch =
          pullRequestsForRepository.stream().collect(groupingBy(SourceControlPullRequest::getBranchName, toList()));
      GitRepositoryInfo gitRepositoryInfo =
          sourceControlUtils.getGitRepositoryInfoForApplication(applications.get(0).getId());
      GitApi gitApi = gitApiFactory.createGitApi(gitRepositoryInfo);
      try {
        Map<String, String> headCommitsByBranchName = gitApi.getHeadCommitsForAllBranches(repositoryUrl);
        Date updateTime = new Date();
        for (Entry<String, String> entry : headCommitsByBranchName.entrySet()) {
          String branchName = entry.getKey();
          String headCommit = entry.getValue();
          List<SourceControlPullRequest> pullRequestsForBranch = pullRequestsByBranch.get(branchName);
          if (pullRequestsForBranch == null) {
            continue;
          }

          for (SourceControlPullRequest pullRequest : pullRequestsForBranch) {
            closedPullRequests.remove(pullRequest);

            if (!pullRequest.getHeadCommitHash().equals(headCommit)) {
              // The branch for this pull request was updated
              pullRequest.setHeadCommitHash(headCommit);
              pullRequest.setLastCheckTime(updateTime);
              pullRequest.setLastDetectedUpdateTime(updateTime);
              sourceControlPullRequestDAO.update(pullRequest);

              for (Application application : applications) {
                log.debug("Detected change for PR# {} for repository {} and application '{}' with ID {}.",
                    pullRequest.getPullRequestId(), pullRequest.getRepositoryUrl(), application.getName(),
                    application.getId());
                createAndSendDiscoveredPullRequestEventIfNeeded(application, pullRequest);
              }
            }
            else {
              pullRequest.setLastCheckTime(updateTime);
              sourceControlPullRequestDAO.update(pullRequest);
            }
          }
        }

        // Delete all pull requests for which the branch was removed
        for (SourceControlPullRequest pullRequest : closedPullRequests) {
          sourceControlPullRequestDAO.delete(pullRequest);
          log.debug("Deleted PR# {} for repository {}.", pullRequest.getPullRequestId(),
              pullRequest.getRepositoryUrl());
        }
      }
      catch (GitException e) {
        log.error("Failed to retrieve head commit data for repository URL {}.", repositoryUrl, e);
      }
      catch (Exception e) {
        log.error("Failed to update pull request details for repository URL {}.", repositoryUrl, e);
      }
      catch (Throwable t) {
        // Try to log to stderr before trying the standard logging because the standard logging may not be operational
        // at this point.
        t.printStackTrace();
        log.error(t.getMessage(), t);
        System.exit(1);
      }
      return null;
    }

    private void createAndSendDiscoveredPullRequestEventIfNeeded(
        Application application,
        SourceControlPullRequest pullRequest)
    {
      GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(application.getId());
      PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO =
          pullRequestPolicyEvaluationResolver.resolveForPullRequest(application.getId(), gitRepositoryInfo,
              pullRequest.getPullRequestId(), pullRequest.getBranchName(), pullRequest.getHeadCommitHash());

      if (pullRequestPolicyEvaluationsDTO == null) {
        return;
      }

      if (!pullRequestPolicyEvaluationsDTO.getDefaultBranchPolicyEvaluation().wasInternallyTriggered()
          || !pullRequestPolicyEvaluationsDTO.getFeatureBranchPolicyEvaluation().wasInternallyTriggered()) {
        // There is at least one policy evaluation triggered externally for this pull request.
        return;
      }

      SourceControlEvent event = new SourceControlEvent() //
          .forUpdatedPullRequest() //
          .setApplicationId(application.getId()) //
          .setBranchName(pullRequest.getBranchName()) //
          .setCommitHash(pullRequest.getHeadCommitHash()) //
          .setPullRequestNumber(pullRequest.getPullRequestId()) //
          .setInitiator("polling");
      sourceControlEventPublisher.publishEvent(event);
      log.info("Sent pull request updated event for application '{}' with PR# {} and commit {}", application.getId(),
          pullRequest.getPullRequestId(), pullRequest.getHeadCommitHash());
    }
  }
}
