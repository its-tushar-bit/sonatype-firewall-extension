/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.sonatype.insight.brain.service.InsightJob;

import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs job scheduling details for {@link TaskScheduler}. Ensures a consistent logging format for job scheduling.
 * <p>
 * Note in older versions of IQ that this type of logging was inside the job class itself, however with the introduction
 * of {@link QuartzJobSchedulingService} where we schedule multiple jobs at once, it was no longer possible to do that.
 */
public class JobLogger
{
  private static final Logger log = LoggerFactory.getLogger(JobLogger.class);

  private final Function<InsightJob, Date> nextExecutionTime;

  private final InsightJob insightJob;

  private final String message;

  private final List<Object> loggerArgs;

  private JobLogger(
      final Function<InsightJob, Date> nextExecutionTime,
      final InsightJob insightJob,
      final JobKey jobKey,
      final String message,
      final Object... loggerArgs)
  {
    this.nextExecutionTime = nextExecutionTime;
    this.insightJob = insightJob;
    this.message = message;

    this.loggerArgs = new ArrayList<>();
    this.loggerArgs.add(insightJob.getJobName());
    this.loggerArgs.add(jobKey);
    this.loggerArgs.addAll(Arrays.asList(loggerArgs));
  }

  static JobLogger daily(final Function<InsightJob, Date> nextExecutionTime, InsightJob insightJob, JobKey jobKey) {
    return new JobLogger(nextExecutionTime, insightJob, jobKey,
        "Scheduling daily task for job {}, jobKey '{}', next execution time: {}.");
  }

  static JobLogger weekly(final Function<InsightJob, Date> nextExecutionTime, InsightJob insightJob, JobKey jobKey) {
    return new JobLogger(nextExecutionTime, insightJob, jobKey,
        "Scheduling weekly task for job {}, jobKey '{}', next execution time: {}.");
  }

  static JobLogger oneTime(
      final Function<InsightJob, Date> nextExecutionTime,
      final InsightJob insightJob,
      final JobKey jobKey)
  {
    return new JobLogger(nextExecutionTime, insightJob, jobKey,
        "Scheduling one-time task for job {}, jobKey '{}', next execution time: {}.");
  }

  static JobLogger periodic(
      final Function<InsightJob, Date> nextExecutionTime,
      final InsightJob insightJob,
      final JobKey jobKey,
      final Duration interval)
  {
    return new JobLogger(nextExecutionTime, insightJob, jobKey,
        "Scheduling periodic task for job {}, jobKey '{}', to run every {}, next execution time {}",
        interval);
  }

  static JobLogger onOtherNodes(final InsightJob insightJob, final JobKey jobKey, final Set<String> otherNodes) {
    // No next execution time for this case as it occurs on other nodes and not this one
    Function<InsightJob, Date> func = job -> null;
    return new JobLogger(func, insightJob, jobKey,
        "Scheduling one-time task for job {}, jobKey '{}', to run on {} nodes.", otherNodes);
  }

  public void log() {
    try {
      Date result = nextExecutionTime.apply(insightJob);
      if (result != null) {
        loggerArgs.add(result);
      }
    }
    catch (NullPointerException e) {
      // `nextExecutionTime` is generally always `TaskScheduler#getNextExecutionTime` which actually asks Quartz what
      // the real next execution time is. If it happens that Quartz has not yet actually scheduled the job then it will
      // unfortunately throw a NullPointerException. This is unexpected, but not a fail state.
      log.warn("Failed to determine next execution time for job {}", insightJob.getJobName(), e);
    }
    log.debug(message, loggerArgs.toArray());
  }
}
