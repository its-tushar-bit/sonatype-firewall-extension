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

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.InsightConfig;

import io.dropwizard.lifecycle.Managed;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private final InsightConfig insightConfig;

  private final TaskScheduler taskScheduler;

  public boolean disableForTesting;

  @Inject
  public PullRequestDetailsUpdater(InsightConfig insightConfig, TaskScheduler taskScheduler) {
    this.insightConfig = insightConfig;
    this.taskScheduler = taskScheduler;
  }

  @Override
  public void start() throws Exception {
    if (disableForTesting) {
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
    // TODO Will be implemented in INT-5039
  }

  @Override
  public void stop() {
    // no-op
  }
}
