/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.tenancy.TenantAwareOneTimeRunnable;
import com.sonatype.insight.brain.tenancy.TenantReference;

import com.google.common.annotations.VisibleForTesting;
import datadog.trace.api.Trace;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle the actual scheduling of jobs with the Quartz {@link Scheduler}. This intentionally uses the Quartz method
 * {@link Scheduler#scheduleJobs(Map, boolean)} (plural) to schedule <b>MULTIPLE</b> jobs at once. In a multi-tenant
 * production environment, you can have many jobs for many tenants on many nodes, all clamouring to be scheduled with
 * Quartz and since Quartz has internal locking around this process, it can introduce contention and performance
 * degradation.
 * <p>
 * The approach in this class uses a per-tenant queue backed by a {@link ScheduledExecutorService}. Any time a job is
 * scheduled (for a tenant, even in single-tenant mode), it will store the job in the queue for the tenant, and schedule
 * a Runnable to execute in a few seconds. When additional jobs come in to be scheduled for the same tenant, they are
 * added to the queue and the Runnable reset.
 *
 * @see <a href="https://sonatype.atlassian.net/browse/CLM-34837">CLM-34837</a>
 */
@Named
@Singleton
public class QuartzJobSchedulingService
{
  private static final Logger log = LoggerFactory.getLogger(QuartzJobSchedulingService.class);

  protected static final long DEFAULT_DELAY_MILLIS = 3_000L;

  // Note we allow tests to override this value to reduce test duration. See QuartzJobSchedulingServiceRule.
  protected static long DELAY_MILLIS = DEFAULT_DELAY_MILLIS;

  private final ScheduledExecutorService jobSchedulingExecutor =
      new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "QuartzJobSchedulingServiceThread"));

  // Note all usages of a TenantQuartzJobs instance should be `synchronized`
  private final TenantReference<TenantQuartzJobs> tenantsQuartzJobs = new TenantReference<>(TenantQuartzJobs::new);

  /**
   * Schedules a job with the given scheduler.
   * <p>
   * The job is not scheduled immediately, but is batched with other jobs for the same tenant and scheduled all at once
   * after a delay to reduce contention.
   *
   * @param scheduler the Quartz scheduler to use
   * @param job       the job to schedule
   * @param triggers  the triggers for the job
   * @param jobLogger a log message to run after the job is actually scheduled with quartz. This is so that we can
   *                  determine the real next execution time from Quartz after the job is scheduled.
   */
  void scheduleTask(
      final Scheduler scheduler,
      final JobDetail job,
      final Set<Trigger> triggers,
      final JobLogger jobLogger)
  {
    log.info("Scheduling job {} with triggers {}", job.getKey(), triggers);
    TenantQuartzJobs tenantQuartzJobs = tenantsQuartzJobs.get();
    synchronized (tenantQuartzJobs) {
      tenantQuartzJobs.addJob(scheduler, job, triggers, jobLogger);
    }
  }

  Boolean unscheduleTask(final Scheduler scheduler, final JobKey jobKey) throws SchedulerException {
    TenantQuartzJobs tenantQuartzJobs = tenantsQuartzJobs.get();
    synchronized (tenantQuartzJobs) {
      // first remove any job from the pending queue
      tenantQuartzJobs.removeJob(scheduler, jobKey);

      // then actually remove the job from quartz
      return scheduler.deleteJob(jobKey);
    }
  }

  @VisibleForTesting
  boolean areJobsPending() {
    TenantQuartzJobs tenantQuartzJobs = tenantsQuartzJobs.get();
    synchronized (tenantQuartzJobs) {
      return tenantQuartzJobs.areJobsPending();
    }
  }

  record QuartzJobRecord(Scheduler scheduler, JobDetail jobDetail, Set<Trigger> triggers, JobLogger jobLogger) { }

  /**
   * Encapsulate the jobs for a tenant. Adding a job for a tenant will enqueue it, and a per-tenant timer reset
   * on each addition. When the timer finally executes, it will process all enqueued jobs for that tenant at once.
   * More jobs can be added to the queue at any time.
   */
  class TenantQuartzJobs
  {
    private static final Logger log = LoggerFactory.getLogger(TenantQuartzJobs.class);

    private final ConcurrentLinkedDeque<QuartzJobRecord> jobsDeque = new ConcurrentLinkedDeque<>();

    private volatile ScheduledFuture<?> tenantScheduledFuture;

    void addJob(
        final Scheduler scheduler,
        final JobDetail job,
        final Set<Trigger> triggers,
        final JobLogger jobLogger)
    {
      log.debug("Adding job {}. Total pending tenant job count: {}", job.getKey(), jobsDeque.size() + 1);
      QuartzJobRecord quartzJobRecord = new QuartzJobRecord(scheduler, job, triggers, jobLogger);
      jobsDeque.add(quartzJobRecord);

      // Cancel any existing scheduled future for this tenant
      if (tenantScheduledFuture != null) {
        log.trace("Cancelling 'task scheduling' ScheduledFuture");
        tenantScheduledFuture.cancel(false);
      }

      // Schedule the TenantRunnable into the executor and store the ScheduledFuture
      log.trace("Scheduling ScheduledFuture for tenant {} with wait delay", DELAY_MILLIS);
      TenantAwareOneTimeRunnable tenantRunnable = new TenantAwareOneTimeRunnable(new TenantRunnable());
      tenantScheduledFuture = jobSchedulingExecutor.schedule(tenantRunnable, DELAY_MILLIS, TimeUnit.MILLISECONDS);
    }

    public void removeJob(final Scheduler scheduler, final JobKey jobKey) {
      for (QuartzJobRecord quartzJobRecord : jobsDeque) {
        if (quartzJobRecord.jobDetail.getKey().equals(jobKey) && quartzJobRecord.scheduler.equals(scheduler)) {
          log.debug("Removing job {}. Total pending tenant job count: {}", jobKey, jobsDeque.size() - 1);
          jobsDeque.remove(quartzJobRecord);
          break;
        }
      }
    }

    public boolean areJobsPending() {
      log.info("Checking if any jobs are pending - {} jobs in queue", jobsDeque.size());
      return !jobsDeque.isEmpty();
    }
  }

  class TenantRunnable
      implements Runnable
  {
    @Trace
    @Override
    public void run() {
      TenantQuartzJobs tenantQuartzJobs = tenantsQuartzJobs.get();
      synchronized (tenantQuartzJobs) {

        // Take any jobs that have queued up and split them by their scheduler
        Map<Scheduler, Map<JobDetail, Set<? extends Trigger>>> jobsByScheduler = new HashMap<>();
        List<JobLogger> jobLoggers = new ArrayList<>(tenantQuartzJobs.jobsDeque.size());
        while (tenantQuartzJobs.jobsDeque.peekFirst() != null) {
          QuartzJobRecord quartzJobRecord = tenantQuartzJobs.jobsDeque.removeFirst();
          jobsByScheduler.computeIfAbsent(quartzJobRecord.scheduler, k -> new HashMap<>())
              .put(quartzJobRecord.jobDetail, quartzJobRecord.triggers);
          jobLoggers.add(quartzJobRecord.jobLogger);
        }

        // For each scheduler, schedule the jobs in one batch
        jobsByScheduler.forEach((scheduler, jobs) -> {
          try {
            log.debug("Scheduling {} jobs on scheduler {}", jobs.size(), scheduler.getSchedulerName());
            scheduler.scheduleJobs(jobs, true);
          }
          catch (Exception e) {
            log.error("Error scheduling jobs", e);
          }
          catch (Throwable t) {
            // Try to log to stderr before trying the standard logging because the standard logging may not be
            // operational at this point.
            t.printStackTrace();
            log.error(t.getMessage(), t);
            System.exit(1);
          }
        });

        // After all jobs are successfully scheduled, run the job loggers
        jobLoggers.forEach(JobLogger::log);
      }
    }
  }
}
