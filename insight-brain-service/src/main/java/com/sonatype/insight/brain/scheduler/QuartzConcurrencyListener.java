/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.time.Duration;
import java.util.Date;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class QuartzConcurrencyListener
    extends TriggerListenerSupport
{
  private static final Logger log = LoggerFactory.getLogger(QuartzConcurrencyListener.class);

  public static final String MAX_CONCURRENT = "maxConcurrent";

  public static final String QUEUE_DELAY_MS = "queueDelayMs";

  private static final int DEFAULT_QUEUE_DELAY_MS = (int) Duration.ofMinutes(1).toMillis();

  private final QuartzJobStoreTX quartzJobStoreTX;

  @Inject
  public QuartzConcurrencyListener(final QuartzJobStoreTX quartzJobStoreTX) {
    this.quartzJobStoreTX = quartzJobStoreTX;
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public boolean vetoJobExecution(final Trigger trigger, final JobExecutionContext context) {
    try {
      JobDetail jobDetail = context.getJobDetail();
      JobDataMap jobDataMap = context.getTrigger().getJobDataMap();
      if (!jobDataMap.containsKey(MAX_CONCURRENT)) {
        return false;
      }
      int maxConcurrent = jobDataMap.getIntValue(MAX_CONCURRENT);
      int queueDelayMs = DEFAULT_QUEUE_DELAY_MS;
      if (jobDataMap.containsKey(QUEUE_DELAY_MS)) {
        queueDelayMs = jobDataMap.getIntValue(QUEUE_DELAY_MS);
      }
      Scheduler scheduler = context.getScheduler();
      JobKey jobKey = jobDetail.getKey();
      // Count includes itself
      int runningCount = quartzJobStoreTX.countCurrentlyExecutingJobs(jobKey.getName());
      boolean veto = runningCount > maxConcurrent;
      if (veto) {
        log.debug("Vetoing job execution for job {} due to max concurrency limit reached." +
                " Current running count: {}, max concurrent: {}",
            jobKey.getName(), runningCount, maxConcurrent);

        Trigger newTrigger = trigger.getTriggerBuilder()
            .startAt(new Date(System.currentTimeMillis() + queueDelayMs))
            .usingJobData(jobDataMap)
            .build();

        scheduler.rescheduleJob(trigger.getKey(), newTrigger);
        return true;
      }
      return false;
    }
    catch (Exception e) {
      return false;
    }
  }
}
