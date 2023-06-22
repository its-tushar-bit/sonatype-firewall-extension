/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.product.license.ProductLicenseListener;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.AllTenantsJob;
import com.sonatype.insight.brain.tenancy.Tenant;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.servlets.tasks.Task;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the entry point for the pull request discovery flow, part of PR Comments feature.
 * It runs periodically and detects if there are any new open pull requests (PRs) for any repository associated with an
 * SCM configured application, for which we could create PR comments i.e.
 * - there is a policy evaluation run against the source control configured default branch, and
 * - there is a policy evaluation run against the feature/development branch head commit that the PR pertains to.
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class PullRequestPollingTask
    extends Task
    implements InsightJob, ProductLicenseListener, AllTenantsJob
{
  public static final String TASK_NAME = "PullRequestPollingTask";

  private static final Logger log = LoggerFactory.getLogger(PullRequestPollingTask.class);

  public static final int PULL_REQUEST_MONITORING_INTERVAL_SECONDS = 60;

  private static final int PULL_REQUEST_MONITORING_DELAY_SECONDS = 30;

  private final PullRequestPollingService pullRequestPollingService;

  private final IqForScmLicenseChecker licenseChecker;

  private final TaskScheduler taskScheduler;

  private final int pullRequestMonitoringIntervalSeconds;

  private final int pullRequestMonitoringDelaySeconds;

  public boolean disableForTesting;

  @Inject
  public PullRequestPollingTask(
      TaskScheduler taskScheduler,
      final PullRequestPollingService pullRequestPollingService,
      final IqForScmLicenseChecker licenseChecker)
  {
    this(taskScheduler, pullRequestPollingService, licenseChecker, PULL_REQUEST_MONITORING_DELAY_SECONDS,
        PULL_REQUEST_MONITORING_INTERVAL_SECONDS);
  }

  @VisibleForTesting
  PullRequestPollingTask(
      TaskScheduler taskScheduler,
      PullRequestPollingService pullRequestPollingService,
      IqForScmLicenseChecker licenseChecker,
      int pullRequestMonitoringDelaySeconds,
      int pullRequestMonitoringIntervalSeconds)
  {
    super("pollPRs");
    this.taskScheduler = taskScheduler;
    this.pullRequestPollingService = pullRequestPollingService;
    this.licenseChecker = licenseChecker;
    this.pullRequestMonitoringDelaySeconds = pullRequestMonitoringDelaySeconds;
    this.pullRequestMonitoringIntervalSeconds = pullRequestMonitoringIntervalSeconds;
  }

  @Override
  public void register() {
    startPullRequestMonitoring();
  }

  @Override
  public void deregister() {
    stopPullRequestMonitoring();
  }

  private void startPullRequestMonitoring() {
    if (disableForTesting || !isLicensed()) {
      return;
    }
    Date startTime = Date.from(
        LocalDateTime.now().plusSeconds(pullRequestMonitoringDelaySeconds).atZone(ZoneId.systemDefault()).toInstant());
    taskScheduler.schedulePeriodicTask(this, Duration.ofSeconds(pullRequestMonitoringIntervalSeconds), startTime);
    log.info("Scheduled SCM pull request polling every {} second(s) starting in {} second(s)",
        pullRequestMonitoringIntervalSeconds, pullRequestMonitoringDelaySeconds);
  }

  @Override
  public void execute(final Map<String, List<String>> map, final PrintWriter output) throws Exception {
    log.debug("Triggering polling for all PRs");
    taskScheduler.triggerTaskNow(this, null);
    output.print("Triggered polling for all PRs");
  }

  @Override
  public void executeForTenant(JobExecutionContext context, Tenant tenant) {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      fetchAndSendPullRequestsForCommenting();
    }
    catch (Exception e) {
      log.error("Failed to start pull request polling: {}", e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(2);
    }
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }

  private void stopPullRequestMonitoring() {
    if (!disableForTesting) {
      log.info("Stopped SCM pull request polling");
      taskScheduler.unscheduleTask(this);
    }
  }

  private void fetchAndSendPullRequestsForCommenting() {
    log.debug("Commencing pull request polling cycle");

    try {
      pullRequestPollingService.fetchAndSendPullRequestsForCommenting();
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    log.debug("Pull request polling cycle complete");
  }

  @Override
  public void productLicenseChanged() {
    if (tenantUtil.isSingleTenant() || tenantUtil.isGlobalTenant()) {
      if (isLicensed()) {
        log.debug("Pull request polling (automation) is licensed");
        startPullRequestMonitoring();
      }
      else {
        log.debug("Pull request polling (automation) is not licensed");
        stopPullRequestMonitoring();
      }
    }
  }

  @Override
  public boolean isLicensed() {
    return licenseChecker.isPullRequestCommentingSupported();
  }
}
