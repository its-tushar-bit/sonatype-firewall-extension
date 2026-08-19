/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.time.Duration;
import java.util.Date;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class PersistedPolicyEvaluationPollingResultCleaner
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(PersistedPolicyEvaluationPollingResultCleaner.class);

  // Visible for testing
  static final String TASK_NAME = "PersistedPolicyEvaluationPollingResultCleaner";

  // Visible for testing
  static final Duration PERIOD = Duration.ofHours(1);

  // Visible for testing
  static final Duration LIFESPAN = Duration.ofHours(24);

  public static final String CLEANER_ERROR = "Policy evaluation polling result cleaner error";

  private final TaskScheduler taskScheduler;

  private final PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  public boolean disableForTesting;

  @Inject
  public PersistedPolicyEvaluationPollingResultCleaner(
      TaskScheduler taskScheduler,
      PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO)
  {
    this.taskScheduler = taskScheduler;
    this.persistedPolicyEvaluationPollingResultDAO = persistedPolicyEvaluationPollingResultDAO;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    // Note executing every PERIOD means the oldest a PolicyEvaluationPollingResult can be is PERIOD + LIFESPAN
    taskScheduler.schedulePeriodicTask(this, PERIOD);
  }

  @Override
  public void deregister() {
    // noop
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(this::deleteExpiredPolicyEvaluationPollingResults, log, CLEANER_ERROR);
  }

  private void deleteExpiredPolicyEvaluationPollingResults() {
    persistedPolicyEvaluationPollingResultDAO
        .deleteBeforeOrOn(new Date(System.currentTimeMillis() - LIFESPAN.toMillis()));
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
