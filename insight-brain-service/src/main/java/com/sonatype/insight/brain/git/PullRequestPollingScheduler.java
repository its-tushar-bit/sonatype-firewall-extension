/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.Duration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import io.dropwizard.lifecycle.Managed;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class PullRequestPollingScheduler
    implements Managed, Job
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestPollingScheduler.class);

  // Visible for testing
  static final int PULL_REQUEST_MONITORING_INTERVAL_SECONDS = 60;

  // Visible for testing
  static final String NAME = "PullRequestPolling";

  private final PullRequestPollingService pullRequestPollingService;

  private final ProductLicense productLicense;

  private final InsightConfig insightConfig;

  private final TaskScheduler taskScheduler;

  public boolean disableForTesting;

  @Inject
  public PullRequestPollingScheduler(
      PullRequestPollingService pullRequestPollingService,
      ProductLicense productLicense,
      InsightConfig insightConfig,
      TaskScheduler taskScheduler)
  {
    this.pullRequestPollingService = pullRequestPollingService;
    this.productLicense = productLicense;
    this.insightConfig = insightConfig;
    this.taskScheduler = taskScheduler;
  }

  @Override
  public void start() throws Exception {
    if (insightConfig.isFeatureEnabled(Feature.PR_COMMENTING)) {
      startPullRequestMonitoring();
    }
    else {
      log.info("Pull request commenting feature is disabled; Pull request polling scheduler is not started.");
    }
  }

  @Override
  public void stop() {
    // noop
  }

  private void startPullRequestMonitoring() {
    if (disableForTesting) {
      return;
    }
    taskScheduler.schedulePeriodicTask(PullRequestPollingScheduler.class, NAME,
        Duration.ofSeconds(PULL_REQUEST_MONITORING_INTERVAL_SECONDS));
    log.info("Scheduled monitoring of SCM pull requests every {} second(s)", PULL_REQUEST_MONITORING_INTERVAL_SECONDS);
  }

  @Override
  public void execute(JobExecutionContext context) {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      monitorPullRequestsForCommenting();
    }
    catch (Exception e) {
      log.error("Pull request monitoring error: {}", e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(1);
    }
  }

  // Visible for testing
  void monitorPullRequestsForCommenting() {
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
