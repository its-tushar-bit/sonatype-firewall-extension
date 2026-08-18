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
import java.util.function.Supplier;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.tenancy.TenantAwareOneTimeRunnable;
import com.sonatype.insight.brain.tenancy.TenantReference;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.instrumentation.annotations.WithSpan;
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
 * scheduled (for a tenant, even in single-tenant mode), a {@link Supplier} that will build the {@link JobDetail} /
 * {@link Trigger}s at flush time is stored in the queue for the tenant, and a Runnable is scheduled to execute after
 * a short delay. When additional jobs come in to be scheduled for the same tenant, they are added to the queue and the
 * Runnable reset.
 * <p>
 * The supplier is invoked at flush time (immediately before the batched {@link Scheduler#scheduleJobs}), not at enqueue
 * time. This matters for triggers whose fire schedule is relative to "now": if the trigger were built at enqueue time,
 * the {@code startTime} it captures via {@code new Date()} would be {@link #DELAY_MILLIS} milliseconds stale by the
 * time Quartz actually persists it. Concretely, {@link NeverPastCalendar} excludes any instant before {@code now -
 * 1500ms} (its safety window against rapid catch-up firing after downtime); an enqueue-time {@code startTime} would
 * fall in that excluded window on every startup and Quartz would advance the first fire by a full repeat interval to
 * find a calendar-included instant — silently shifting daily triggers out by 24h. Building the trigger at flush time
 * keeps {@code startTime} fresh and the first fire lands shortly after registration.
 * See <a href="https://sonatype.atlassian.net/browse/CLM-42076">CLM-42076</a>.
 * <p>
 * The {@link JobKey} passed to {@link #scheduleTask} is held eagerly on the pending record (i.e. captured before the
 * supplier is invoked). This means callers can {@link #unscheduleTask} immediately after {@code scheduleTask} returns,
 * even during the batching window: {@code unscheduleTask} locates and drops the pending record by key without needing
 * to materialize the supplier.
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

  /** Name of the single batching-executor thread; shared with tests that assert on / join it. */
  public static final String SCHEDULING_THREAD_NAME = "QuartzJobSchedulingServiceThread";

  private final ScheduledExecutorService jobSchedulingExecutor =
      new ScheduledThreadPoolExecutor(1, r -> new Thread(r, SCHEDULING_THREAD_NAME));

  // Note all usages of a TenantQuartzJobs instance should be `synchronized`
  private final TenantReference<TenantQuartzJobs> tenantsQuartzJobs = new TenantReference<>(TenantQuartzJobs::new);

  /**
   * Shut down the batching executor when the Spring context closes so its {@link #SCHEDULING_THREAD_NAME} thread
   * does not outlive the context. Without this the thread (and the beans captured by its pending tasks) is retained
   * after {@code applicationContext.close()}, which leaks a full context per server restart in reused-JVM test runs.
   *
   * <p>
   * {@code shutdownNow()} intentionally abandons any jobs still batched in the executor (those enqueued within the
   * last {@link #DELAY_MILLIS} before close) rather than draining them: on context close the server is going away,
   * so flushing those late batches into Quartz against a closing context has no value. Before this hook the leaking
   * thread would eventually have flushed them against a stale context anyway, so no behavior is lost.
   */
  @PreDestroy
  void shutdown() {
    jobSchedulingExecutor.shutdownNow();
  }

  /**
   * Enqueues a job for scheduling with the given scheduler. The job is not scheduled immediately; it is batched with
   * other jobs for the same tenant and scheduled all at once after a delay to reduce Quartz lock contention.
   * <p>
   * The {@code builder} is invoked on the batched scheduling thread just before the {@link Scheduler#scheduleJobs}
   * call, so any {@code new Date()} embedded in the built {@link Trigger}(s) reflects the actual scheduling time.
   *
   * @param scheduler the Quartz scheduler to use
   * @param jobKey the key of the job being scheduled. Held eagerly for identity/lookup (so
   *          {@link #unscheduleTask} can locate the pending entry without materializing the supplier)
   * @param builder produces the {@link JobDetail}, its trigger set, and the {@link JobLogger} that will report the
   *          next execution time after scheduling completes. Invoked once, at flush time.
   */
  void scheduleTask(
      final Scheduler scheduler,
      final JobKey jobKey,
      final Supplier<BuiltJob> builder)
  {
    TenantQuartzJobs tenantQuartzJobs = tenantsQuartzJobs.get();
    synchronized (tenantQuartzJobs) {
      tenantQuartzJobs.addJob(scheduler, jobKey, builder);
    }
  }

  Boolean unscheduleTask(final Scheduler scheduler, final JobKey jobKey) throws SchedulerException {
    TenantQuartzJobs tenantQuartzJobs = tenantsQuartzJobs.get();
    synchronized (tenantQuartzJobs) {
      // first remove any pending record from the queue
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

  /**
   * The output of a {@link Supplier} passed into {@link #scheduleTask}: the JobDetail, the triggers to install for it,
   * and the {@link JobLogger} that will log the actual next execution time after Quartz has persisted the schedule.
   */
  public record BuiltJob(JobDetail jobDetail, Set<? extends Trigger> triggers, JobLogger jobLogger)
  {
  }

  record QuartzJobRecord(Scheduler scheduler, JobKey jobKey, Supplier<BuiltJob> builder)
  {
  }

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
        final JobKey jobKey,
        final Supplier<BuiltJob> builder)
    {
      log.debug("Adding job {}. Total pending tenant job count: {}", jobKey, jobsDeque.size() + 1);
      jobsDeque.add(new QuartzJobRecord(scheduler, jobKey, builder));

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
        if (quartzJobRecord.jobKey.equals(jobKey) && quartzJobRecord.scheduler.equals(scheduler)) {
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
    @WithSpan
    @Override
    public void run() {
      TenantQuartzJobs tenantQuartzJobs = tenantsQuartzJobs.get();
      synchronized (tenantQuartzJobs) {

        // Materialize each pending record via its supplier and split them by their scheduler. Materialization happens
        // here (not at addJob time) so any "now" embedded in the built triggers reflects actual scheduling time.
        //
        // A single throwing supplier is isolated: it's logged and skipped so the rest of the batch still lands. Without
        // this guard, an exception in one supplier would propagate out of TenantRunnable.run(), silently discarding all
        // records already drained from the deque (they've already been removeFirst()-ed) and losing the exception in
        // the ScheduledFuture. See PR 16477 review feedback.
        Map<Scheduler, Map<JobDetail, Set<? extends Trigger>>> jobsByScheduler = new HashMap<>();
        List<JobLogger> jobLoggers = new ArrayList<>(tenantQuartzJobs.jobsDeque.size());
        while (tenantQuartzJobs.jobsDeque.peekFirst() != null) {
          QuartzJobRecord rec = tenantQuartzJobs.jobsDeque.removeFirst();
          try {
            BuiltJob built = rec.builder.get();
            log.info("Scheduling job {} with triggers {}", built.jobDetail.getKey(), built.triggers);
            jobsByScheduler.computeIfAbsent(rec.scheduler, k -> new HashMap<>())
                .put(built.jobDetail, built.triggers);
            jobLoggers.add(built.jobLogger);
          }
          catch (Exception e) {
            log.error("Skipping job {} on scheduler {}: builder threw {}", rec.jobKey, rec.scheduler, e.toString(), e);
          }
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
