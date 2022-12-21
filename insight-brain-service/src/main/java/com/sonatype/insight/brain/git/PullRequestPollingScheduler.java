/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.security.SystemRunnable;
import com.sonatype.insight.brain.tenancy.TenantScheduledThreadPoolExecutor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.lifecycle.Managed;
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
public class PullRequestPollingScheduler
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestPollingScheduler.class);

  public static final int PULL_REQUEST_MONITORING_INTERVAL_SECONDS = 60;

  private static final int PULL_REQUEST_MONITORING_DELAY_SECONDS = 30;

  private final PullRequestPollingService pullRequestPollingService;

  private final IqForScmLicenseChecker licenseChecker;

  private ScheduledExecutorService scheduledExecutorService;

  private final int pullRequestMonitoringIntervalSeconds;

  private final int pullRequestMonitoringDelaySeconds;

  public boolean disableForTesting;

  @Inject
  public PullRequestPollingScheduler(
      final PullRequestPollingService pullRequestPollingService,
      final IqForScmLicenseChecker licenseChecker)
  {
    this(pullRequestPollingService, licenseChecker, PULL_REQUEST_MONITORING_DELAY_SECONDS,
        PULL_REQUEST_MONITORING_INTERVAL_SECONDS);
  }

  @VisibleForTesting
  PullRequestPollingScheduler(
      PullRequestPollingService pullRequestPollingService,
      IqForScmLicenseChecker licenseChecker,
      int pullRequestMonitoringDelaySeconds,
      int pullRequestMonitoringIntervalSeconds)
  {
    this.pullRequestPollingService = pullRequestPollingService;
    this.licenseChecker = licenseChecker;
    this.pullRequestMonitoringDelaySeconds = pullRequestMonitoringDelaySeconds;
    this.pullRequestMonitoringIntervalSeconds = pullRequestMonitoringIntervalSeconds;
  }

  @Override
  public void start() throws Exception {
    startPullRequestMonitoring();
  }

  @Override
  public void stop() throws Exception {
    stopPullRequestMonitoring();
  }

  private ScheduledExecutorService newExecutor() {
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("PullRequestMonitor-%d").setDaemon(true).build();
    return new TenantScheduledThreadPoolExecutor(1, threadFactory);
  }

  private void startPullRequestMonitoring() {
    if (scheduledExecutorService != null || disableForTesting) {
      return;
    }
    scheduledExecutorService = newExecutor();
    Runnable pullRequestMonitoringTask = new SystemRunnable(() -> {
      try {
        monitorPullRequestsForCommenting();
      }
      catch (RuntimeException e) {
        log.warn("Failed to monitor pull requests", e);
      }
    });
    scheduledExecutorService.scheduleAtFixedRate(pullRequestMonitoringTask, pullRequestMonitoringDelaySeconds,
        pullRequestMonitoringIntervalSeconds, TimeUnit.SECONDS);
    log.info("Scheduled monitoring of SCM pull requests every {} second(s) starting in {} second(s)",
        pullRequestMonitoringIntervalSeconds, pullRequestMonitoringDelaySeconds);
  }

  private void stopPullRequestMonitoring() {
    if (scheduledExecutorService != null) {
      scheduledExecutorService.shutdown();
      scheduledExecutorService = null;
      log.info("Stopped SCM pull request monitoring");
    }
  }

  private void monitorPullRequestsForCommenting() {
    if (licenseChecker.isPullRequestCommentingSupported()) {
      log.debug("Commencing pull request polling cycle");

      try {
        pullRequestPollingService.fetchAndSendPullRequestsForCommenting();
      }
      catch (Exception e) {
        log.error(e.getMessage(), e);
      }
      log.debug("Pull request polling cycle complete");
    }
  }
}
