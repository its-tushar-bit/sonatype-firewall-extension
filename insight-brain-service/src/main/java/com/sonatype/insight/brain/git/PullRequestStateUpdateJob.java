/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A job that runs daily to update the state of pull requests by creating state update events.
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class PullRequestStateUpdateJob
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestStateUpdateJob.class);

  private static final String JOB_ERROR = "Error when running the pull request state update job";

  // Visible for testing
  static final String TASK_NAME = "PullRequestStateUpdateJob";

  // Visible for testing - run once a day
  static final Duration PERIOD = Duration.ofDays(1);

  private final TaskScheduler taskScheduler;

  private final PullRequestStateService pullRequestStateService;

  @Inject
  public PullRequestStateUpdateJob(
      TaskScheduler taskScheduler,
      PullRequestStateService pullRequestStateService)
  {
    this.taskScheduler = taskScheduler;
    this.pullRequestStateService = pullRequestStateService;
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }

  @Override
  public void register() {
    taskScheduler.schedulePeriodicTask(this, PERIOD);
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(this::updatePullRequestState, log, JOB_ERROR);
  }

  private void updatePullRequestState() {
    pullRequestStateService.dispatchPullRequestStateUpdateEvents();
    log.info("{} task executed.", getJobName());
  }

  @Override
  public void deregister() {
    // noop
  }
}
