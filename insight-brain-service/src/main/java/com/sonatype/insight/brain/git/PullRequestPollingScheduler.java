/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.ScmNodeProcessor;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.security.OneTimeSystemRunnable;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantScheduledThreadPoolExecutor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import datadog.trace.api.Trace;
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
    implements TenantManaged
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestPollingScheduler.class);

  public static final int PULL_REQUEST_DISCOVERY_INTERVAL_SECONDS = 120;

  private static final int PULL_REQUEST_DISCOVERY_DELAY_SECONDS = 30;

  private static final int PULL_REQUEST_DISCOVERY_TENANT_THREAD_COUNT = 1;

  private final PullRequestPollingService pullRequestPollingService;

  private final IqForScmLicenseChecker licenseChecker;

  private final ApiConfigFeaturesService apiConfigFeaturesService;

  private final TenantReference<ScheduledExecutorService> tenantScheduledExecutorServices;

  private final int pullRequestDiscoveryIntervalSeconds;

  private final int pullRequestDiscoveryDelaySeconds;

  private final ShutdownHandler shutdownHandler;

  private final ScmNodeProcessor scmNodeProcessor;

  public boolean disableForTesting;

  @Inject
  public PullRequestPollingScheduler(
      final PullRequestPollingService pullRequestPollingService,
      final IqForScmLicenseChecker licenseChecker,
      final ApiConfigFeaturesService apiConfigFeaturesService,
      final ShutdownHandler shutdownHandler,
      ScmNodeProcessor scmNodeProcessor)
  {
    this(pullRequestPollingService, licenseChecker, apiConfigFeaturesService,
        PULL_REQUEST_DISCOVERY_DELAY_SECONDS, PULL_REQUEST_DISCOVERY_INTERVAL_SECONDS, shutdownHandler,
        scmNodeProcessor);
  }

  @VisibleForTesting
  PullRequestPollingScheduler(
      PullRequestPollingService pullRequestPollingService,
      IqForScmLicenseChecker licenseChecker,
      final ApiConfigFeaturesService apiConfigFeaturesService,
      int pullRequestDiscoveryDelaySeconds,
      int pullRequestDiscoveryIntervalSeconds,
      ShutdownHandler shutdownHandler,
      ScmNodeProcessor scmNodeProcessor)
  {
    this.pullRequestPollingService = pullRequestPollingService;
    this.licenseChecker = licenseChecker;
    this.apiConfigFeaturesService = apiConfigFeaturesService;
    this.pullRequestDiscoveryDelaySeconds = pullRequestDiscoveryDelaySeconds;
    this.pullRequestDiscoveryIntervalSeconds = pullRequestDiscoveryIntervalSeconds;
    this.tenantScheduledExecutorServices = new TenantReference<>(() -> newExecutor());
    this.shutdownHandler = shutdownHandler;
    this.scmNodeProcessor = scmNodeProcessor;
  }

  @Override
  public void register() {
    if (scmNodeProcessor.shouldRun()) {
      startPullRequestPolling();
    }
  }

  @Override
  public void deregister() {
    if (scmNodeProcessor.shouldRun()) {
      stopPullRequestPolling();
    }
  }

  // Visible for testing
  ScheduledExecutorService newExecutor() {
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("PullRequestPolling-%d").setDaemon(true).build();
    TenantScheduledThreadPoolExecutor tenantScheduledThreadPoolExecutor =
        new TenantScheduledThreadPoolExecutor(PULL_REQUEST_DISCOVERY_TENANT_THREAD_COUNT, threadFactory);
    shutdownHandler.add(tenantScheduledThreadPoolExecutor);
    return tenantScheduledThreadPoolExecutor;
  }

  private void startPullRequestPolling() {
    if (disableForTesting) {
      return;
    }

    ScheduledExecutorService scheduledExecutorService = tenantScheduledExecutorServices.get();
    Runnable pullRequestDiscoveryTask = () -> {
      // The runnable needs to use the runtime tenant from the thread, not the call site tenant
      OneTimeSystemRunnable oneTimeSystemRunnable = new OneTimeSystemRunnable(this::discoverPullRequestsForCommenting);
      try {
        oneTimeSystemRunnable.run();
      }
      catch (RuntimeException e) {
        log.error("Failed to run pull request discovery cycle: {}", e.getMessage(), e);
      }
    };

    scheduledExecutorService.scheduleAtFixedRate(pullRequestDiscoveryTask, pullRequestDiscoveryDelaySeconds,
        pullRequestDiscoveryIntervalSeconds, TimeUnit.SECONDS);
    log.info("Scheduled discovery of SCM pull requests every {} second(s) starting in {} second(s)",
        pullRequestDiscoveryIntervalSeconds, pullRequestDiscoveryDelaySeconds);
  }

  // Visible to tests so they can spy it and count the number of times it runs
  @Trace
  @VisibleForTesting
  void discoverPullRequestsForCommenting() {
    if (isLicensed()) {
      log.debug("Commencing pull request polling cycle");
      pullRequestPollingService.fetchAndSendPullRequestsForCommenting();
      log.debug("Pull request polling cycle complete");
    }
  }

  private void stopPullRequestPolling() {
    if (disableForTesting) {
      return;
    }

    ScheduledExecutorService scheduledExecutorService = tenantScheduledExecutorServices.get();
    scheduledExecutorService.shutdown();
    tenantScheduledExecutorServices.remove();
    log.info("Stopped SCM pull request discovery");
  }

  public boolean isLicensed() {
    return apiConfigFeaturesService.isSaasLifecycleScmEnabled() && licenseChecker.isPullRequestCommentingSupported();
  }
}
