/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.SystemRunnable;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class PullRequestPollingScheduler
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestPollingScheduler.class);

  private static final int PULL_REQUEST_MONITORING_DELAY_SECONDS = 30;

  private static final int PULL_REQUEST_MONITORING_INTERVAL_SECONDS = 60;

  private final PullRequestPollingService pullRequestPollingService;

  private final ProductLicense productLicense;

  private ScheduledExecutorService scheduledExecutorService;

  private final int pullRequestMonitoringIntervalSeconds;

  private final int pullRequestMonitoringDelaySeconds;

  private final boolean pullRequestPollingEnabled;

  @Inject
  public PullRequestPollingScheduler(
      PullRequestPollingService pullRequestPollingService,
      ProductLicense productLicense)
  {
    this.pullRequestPollingService = pullRequestPollingService;
    this.productLicense = productLicense;
    pullRequestMonitoringDelaySeconds = PULL_REQUEST_MONITORING_DELAY_SECONDS;
    pullRequestMonitoringIntervalSeconds = PULL_REQUEST_MONITORING_INTERVAL_SECONDS;
    pullRequestPollingEnabled = null != System.getProperty("enable-pr-polling");
  }

  @VisibleForTesting
  PullRequestPollingScheduler(
      PullRequestPollingService pullRequestPollingService,
      ProductLicense productLicense,
      int pullRequestMonitoringDelaySeconds,
      int pullRequestMonitoringIntervalSeconds,
      boolean pullRequestPollingEnabled)
  {
    this.pullRequestPollingService = pullRequestPollingService;
    this.productLicense = productLicense;
    this.pullRequestMonitoringDelaySeconds = pullRequestMonitoringDelaySeconds;
    this.pullRequestMonitoringIntervalSeconds = pullRequestMonitoringIntervalSeconds;
    this.pullRequestPollingEnabled = pullRequestPollingEnabled;
  }

  @Override
  public void start() throws Exception {
    if (pullRequestPollingEnabled) {
      startPullRequestMonitoring();
    }
  }

  @Override
  public void stop() throws Exception {
    stopPullRequestMonitoring();
  }

  private ScheduledExecutorService newExecutor() {
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("PullRequestMonitor-%d").setDaemon(true).build();
    return new ScheduledThreadPoolExecutor(1, threadFactory);
  }

  private void startPullRequestMonitoring() {
    if (null == scheduledExecutorService) {
      scheduledExecutorService = newExecutor();
    }
    Duration initialDelay = Duration.ofSeconds(pullRequestMonitoringDelaySeconds);
    Duration period = Duration.ofSeconds(pullRequestMonitoringIntervalSeconds);
    Runnable pullRequestMonitoringTask = new SystemRunnable(() -> {
      try {
        monitorPullRequestsForCommenting();
      }
      catch (RuntimeException e) {
        log.warn("Failed to monitor pull requests", e);
      }
    });
    scheduledExecutorService.scheduleAtFixedRate(pullRequestMonitoringTask, initialDelay.toMillis(), period.toMillis(),
        TimeUnit.MILLISECONDS);
    log.info("Scheduled monitoring of SCM pull requests every {} second(s) starting in {} second(s)",
        period.toMillis() / 1000, initialDelay.toMillis() / 1000);
  }

  private void stopPullRequestMonitoring() {
    if (scheduledExecutorService != null) {
      scheduledExecutorService.shutdown();
      scheduledExecutorService = null;
      log.info("Stopped SCM pull request monitoring");
    }
  }

  private void monitorPullRequestsForCommenting() {
    if (checkLicense()) {
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

  private boolean checkLicense() {
    return productLicense.hasFeature(LicensedFeature.AUTOMATION);
  }
}
