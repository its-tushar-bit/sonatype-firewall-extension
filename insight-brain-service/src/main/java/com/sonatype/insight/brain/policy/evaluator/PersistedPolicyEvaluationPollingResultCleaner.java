/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.time.Duration;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;

import io.dropwizard.lifecycle.Managed;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class PersistedPolicyEvaluationPollingResultCleaner
    implements Managed, Job
{
  private static final Logger log = LoggerFactory.getLogger(PersistedPolicyEvaluationPollingResultCleaner.class);

  // Visible for testing
  static final String TASK_NAME = "PersistedPolicyEvaluationPollingResultCleaner";

  // Visible for testing
  static final Duration PERIOD = Duration.ofHours(1);

  // Visible for testing
  static final Duration LIFESPAN = Duration.ofHours(2);

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
  public void start() {
    if (disableForTesting) {
      return;
    }
    // Note executing every PERIOD means the oldest a PolicyEvaluationPollingResult can be is PERIOD + LIFESPAN
    taskScheduler.schedulePeriodicTask(PersistedPolicyEvaluationPollingResultCleaner.class, TASK_NAME, PERIOD);
  }

  @Override
  public void stop() {
    // noop
  }

  @Override
  public void execute(JobExecutionContext context) {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      deleteExpiredPolicyEvaluationPollingResults();
    }
    catch (Exception e) {
      log.error("Policy evaluation polling result cleaner error: {}", e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(1);
    }
  }

  private void deleteExpiredPolicyEvaluationPollingResults() {
    persistedPolicyEvaluationPollingResultDAO
        .deleteBeforeOrOn(new Date(System.currentTimeMillis() - LIFESPAN.toMillis()));
  }
}
