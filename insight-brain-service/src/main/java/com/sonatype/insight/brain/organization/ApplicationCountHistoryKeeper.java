/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.time.LocalTime;
import java.util.concurrent.ThreadLocalRandom;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class ApplicationCountHistoryKeeper implements InsightJob
{
  public static final String NAME = "ApplicationCountHistoryKeeper";

  private static final Logger log = LoggerFactory.getLogger(ApplicationCountHistoryKeeper.class);

  private final TaskScheduler taskScheduler;

  public boolean disableForTesting;

  @Inject
  public ApplicationCountHistoryKeeper(
      TaskScheduler taskScheduler
  )
  {
    this.taskScheduler = taskScheduler;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    taskScheduler.scheduleDailyTask(this, LocalTime.of(1,
        30 + ThreadLocalRandom.current().nextInt(30))); // randomize minute to avoid coordinated load spike
    log.debug("Scheduled application count history keeping for {}", taskScheduler.getNextExecutionTime(this));
  }

  @Override
  public void deregister() {
    if (!disableForTesting && taskScheduler.unscheduleTask(this)) {
      log.debug("Stopped the scheduled bookkeeping of the application count history.");
    }
  }

  @Override
  public void execute(JobExecutionContext context) {
    log.debug("Skipping execution of job {}", NAME);
    // execute(applicationCountHistoryService::recordApplicationCount, log, JOB_ERROR);
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
