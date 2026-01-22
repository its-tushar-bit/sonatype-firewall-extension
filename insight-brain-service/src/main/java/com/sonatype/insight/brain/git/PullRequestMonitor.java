/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.PrintWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.servlets.tasks.Task;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * This class is the entry point for the Feature Branch Monitoring feature (part of Continuous Risk Profile). It detects
 * changes on feature branches that have associated pull requests, executes policy evaluations on the changed feature
 * branches. The updated pull request policy evaluations may trigger pull requests comment updates, when policy
 * violations are resolved or introduced.
 *
 * @since 1.114
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class PullRequestMonitor
    extends Task
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestMonitor.class);

  public static final String TASK_NAME = "PullRequestMonitor";

  private static final int THREAD_POOL_SIZE = 1;

  private static final long LS_REMOTE_DELAY_MS = 1_000;

  private static final String PULL_REQUEST_DETAILS_UPDATE_ERROR = "Error when updating pull request details";

  private final Configuration configuration;

  private final TaskScheduler taskScheduler;

  private final ApplicationDAO applicationDAO;

  private final SourceControlEventDAO sourceControlEventDAO;

  private final SourceControlPullRequestDAO sourceControlPullRequestDAO;

  private final GitApiFactory gitApiFactory;

  private final SourceControlUtils sourceControlUtils;

  private final SourceControlEventPublisher sourceControlEventPublisher;

  private final IqForScmLicenseChecker licenseChecker;

  private final ApiConfigFeaturesService apiConfigFeaturesService;

  private ExecutorService executorService;

  private final PullRequestCommentingEligibilityValidator pullRequestCommentingEligibilityValidator;

  private final ShutdownHandler shutdownHandler;

  public boolean disableForTesting;

  @Inject
  public PullRequestMonitor(
      Configuration configuration,
      TaskScheduler taskScheduler,
      GitApiFactory gitApiFactory,
      SourceControlUtils sourceControlUtils,
      SourceControlEventPublisher sourceControlEventPublisher,
      IqForScmLicenseChecker licenseChecker,
      ApplicationDAO applicationDAO,
      SourceControlEventDAO sourceControlEventDAO,
      SourceControlPullRequestDAO sourceControlPullRequestDAO,
      PullRequestCommentingEligibilityValidator pullRequestCommentingEligibilityValidator,
      ApiConfigFeaturesService apiConfigFeaturesService,
      ShutdownHandler shutdownHandler)
  {
    super("monitorPRs");
    this.configuration = configuration;
    this.taskScheduler = taskScheduler;
    this.gitApiFactory = gitApiFactory;
    this.sourceControlUtils = sourceControlUtils;
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.licenseChecker = licenseChecker;
    this.applicationDAO = applicationDAO;
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.sourceControlPullRequestDAO = sourceControlPullRequestDAO;
    this.pullRequestCommentingEligibilityValidator = pullRequestCommentingEligibilityValidator;
    this.apiConfigFeaturesService = apiConfigFeaturesService;
    this.shutdownHandler = shutdownHandler;
  }

  // Visible for testing
  ExecutorService getExecutorService() {
    if (executorService == null) {
      ThreadFactory threadFactory =
          new ThreadFactoryBuilder().setDaemon(true).setNameFormat("PullRequestMonitor-%s").build();
      ThreadPoolExecutor threadPoolExecutor =
          new TenantThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 5L, TimeUnit.MINUTES,
              new LinkedBlockingQueue<>(), threadFactory, new AbortPolicy(), "pull_request_monitor",
              "PullRequestMonitor");
      threadPoolExecutor.allowCoreThreadTimeOut(true);
      executorService = threadPoolExecutor;
      shutdownHandler.add(executorService);
    }
    return executorService;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    schedulePullRequestMonitor();
  }

  public void schedulePullRequestMonitor() {
    SourceControlConfiguration sourceControlConfiguration = configuration.getSourceControlConfigurationOrDefault();
    int intervalInSeconds = sourceControlConfiguration.getPullRequestMonitoringIntervalSeconds();
    taskScheduler.schedulePeriodicTask(this, Duration.ofSeconds(intervalInSeconds));
    log.debug("Scheduled PullRequestMonitor, interval={} seconds.", intervalInSeconds);
  }

  @Override
  public void execute(final Map<String, List<String>> map, final PrintWriter output) throws Exception {
    log.debug("Triggering monitoring for all PRs");
    taskScheduler.triggerTaskNow(this, null);
    output.print("Triggered monitoring for all PRs");
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(() -> {
      if (apiConfigFeaturesService.isSaasLifecycleScmEnabled() && licenseChecker.isIqForScmSupported()) {
        updatePullRequestDetails();
      }
    }, log, PULL_REQUEST_DETAILS_UPDATE_ERROR);
  }

  // Visible for tests
  void updatePullRequestDetails() {
    long start = System.currentTimeMillis();
    log.debug("Updating pull request details.");

    List<SourceControlPullRequest> allPullRequests =
        sourceControlPullRequestDAO.getBySources(PullRequestSource.EXTERNAL);

    Map<String, List<SourceControlPullRequest>> pullRequestsByRepositoryUrl =
        allPullRequests.stream().collect(groupingBy(SourceControlPullRequest::getRepositoryUrl, toList()));
    Set<String> repositoryUrls =
        allPullRequests.stream().map(SourceControlPullRequest::getRepositoryUrl).collect(toSet());
    List<PullRequestMonitorTask> tasks = new ArrayList<>();
    for (String repositoryUrl : repositoryUrls) {
      tasks.add(new PullRequestMonitorTask(repositoryUrl, pullRequestsByRepositoryUrl.get(repositoryUrl)));
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
  public void deregister() {
    // Do not unschedule task otherwise it will break MTIQ - SDEV-1312
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }

  private class PullRequestMonitorTask
      implements Callable<Void>
  {
    private final String repositoryUrl;

    private final List<SourceControlPullRequest> pullRequestsForRepository;

    PullRequestMonitorTask(String repositoryUrl, List<SourceControlPullRequest> pullRequestsForRepository) {
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

      Optional<GitRepositoryInfo> prCommentingEnabledRepository = applications.stream()
          .map(application -> sourceControlUtils.getGitRepositoryInfoForApplication(application.getId()))
          .filter(pullRequestCommentingEligibilityValidator::isPullRequestCommentingEnabled)
          .findAny();

      if (!prCommentingEnabledRepository.isPresent()) {
        String applicationPublicIds =
            applications.stream().map(Application::getPublicId).collect(Collectors.joining(","));
        log.debug("None of the applications with public id {} for repository {} has pull requests commenting enabled.",
            applicationPublicIds, repositoryUrl);
        return null;
      }

      List<SourceControlPullRequest> closedPullRequests = new ArrayList<>(pullRequestsForRepository);
      Map<String, List<SourceControlPullRequest>> pullRequestsByBranch =
          pullRequestsForRepository.stream().collect(groupingBy(SourceControlPullRequest::getBranchName, toList()));
      GitRepositoryInfo gitRepositoryInfo = prCommentingEnabledRepository.get();
      GitApi gitApi = gitApiFactory.createGitApi(gitRepositoryInfo);
      List<String> applicationIds = applications.stream().map(Application::getId).collect(toList());
      try {
        // introducing a small delay here to cut down on the perceived load on the scm system (for github.com
        // specifically) to help reduce rate abuse triggering that was experienced during load testing
        Thread.sleep(LS_REMOTE_DELAY_MS);
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
            log.debug("Processing PR# {} (id={}) for SCM repository URL '{}'", pullRequest.getPullRequestId(),
                pullRequest.getId(), pullRequest.getRepositoryUrl());

            closedPullRequests.remove(pullRequest);

            if (!pullRequest.getHeadCommitHash().equals(headCommit)) {
              // The branch for this pull request was updated
              List<SourceControlEvent> existingEvents = sourceControlEventDAO
                  .getPendingOrInProgressUpdatedPullRequestEvents(applicationIds, pullRequest.getPullRequestId());
              if (!existingEvents.isEmpty()) {
                // Since we track PRs independently from apps, if multiple apps are mapped to the same SCM repository,
                // then we should update the PR only if we can send events for all apps.
                log.info(
                    "Pull request updated event for applications with SCM repository URL '{}' "
                        + "with PR# {} and commit {} are already pending or in progress",
                    repositoryUrl, pullRequest.getPullRequestId(), pullRequest.getHeadCommitHash());
                continue;
              }

              pullRequest.setHeadCommitHash(headCommit);
              pullRequest.setLastCheckTime(updateTime);
              pullRequest.setLastDetectedUpdateTime(updateTime);
              sourceControlPullRequestDAO.update(pullRequest);

              for (Application application : applications) {
                gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(application.getId());
                if (pullRequestCommentingEligibilityValidator.isPullRequestCommentingEnabled(gitRepositoryInfo)) {
                  sendUpdatedPullRequestEvent(application, pullRequest);
                  log.trace("Detected change for PR# {} for repository {} and application '{}' with ID {}.",
                      pullRequest.getPullRequestId(), pullRequest.getRepositoryUrl(), application.getName(),
                      application.getId());
                }
                else {
                  log.trace("Pull request commenting is disabled for application '{}'. We will not comment on it.",
                      application.getName());
                }
              }
            }
            else {
              pullRequest.setLastCheckTime(updateTime);
              sourceControlPullRequestDAO.update(pullRequest);
            }

            log.debug("Processed PR# {} (id={}) for SCM repository URL '{}'", pullRequest.getPullRequestId(),
                pullRequest.getId(), pullRequest.getRepositoryUrl());
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

    private void sendUpdatedPullRequestEvent(
        Application application,
        SourceControlPullRequest pullRequest)
    {
      SourceControlEvent event = new SourceControlEvent() //
          .forUpdatedPullRequest() //
          .setApplicationId(application.getId()) //
          .setBranchName(pullRequest.getBranchName()) //
          .setCommitHash(pullRequest.getHeadCommitHash()) //
          .setBaseCommitHash(pullRequest.getBaseCommitHash()) //
          .setBaseBranchName(pullRequest.getBaseBranchName()) //
          .setPullRequestNumber(pullRequest.getPullRequestId()) //
          .setInitiator("polling");
      sourceControlEventPublisher.publishEvent(event);

      log.info("Sent pull request updated event for application '{}' with PR# {} and commit {}", application.getId(),
          pullRequest.getPullRequestId(), pullRequest.getHeadCommitHash());
    }
  }
}
