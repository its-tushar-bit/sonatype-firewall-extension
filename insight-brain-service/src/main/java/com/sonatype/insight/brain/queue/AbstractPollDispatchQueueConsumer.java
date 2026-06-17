/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.queue;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.sonatype.insight.brain.service.AdminTask;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantScheduledThreadPoolExecutor;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang3.exception.UncheckedInterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class implementing the Poll-and-Dispatch queue consumer pattern.
 * <p>
 * A single scheduler thread polls the database at a fixed rate ({@code scheduleAtFixedRate}).
 * On each tick it calculates available worker-pool capacity and acquires exactly that many jobs,
 * dispatching each to an independent worker thread. One job failure never blocks others.
 * <p>
 * Subclasses provide domain-specific plug-in points via abstract methods, and may optionally
 * participate in the {@link com.sonatype.insight.brain.service.AdminTask AdminTask} admin-port
 * framework and the live-configuration ConfigurationListener framework by overriding the
 * relevant hook methods.
 * <p>
 * Per-tenant isolation is achieved via {@link TenantReference}: each tenant gets its own
 * scheduler, worker pool, and in-flight ID set.
 * <p>
 * <b>Retry behaviour:</b> On job failure, {@link #onJobFailure} is called. The default
 * implementation increments a persistent retry counter. If the counter is below
 * {@link #getMaxRetries()}, the job is unacquired back to PENDING for automatic retry on the next
 * poll tick. Once the limit is reached the job is marked FAILED permanently. Subclasses may
 * override {@link #onJobFailure} to change this behaviour (e.g. dead-letter queuing).
 * <p>
 * <b>AdminTask / ConfigurationListener:</b> Concrete subclasses that also extend
 * AdminTask and implement ConfigurationListener should call
 * {@link #handleConfigurationChanged(int, long, boolean)} from their {@code configurationChanged()}
 * implementation. This keeps the framework logic centralised here while giving subclasses full control.
 *
 * @param <T> the queue job type
 */
public abstract class AbstractPollDispatchQueueConsumer<T>
    extends AdminTask
    implements TenantManaged
{
  private static final Logger log = LoggerFactory.getLogger(AbstractPollDispatchQueueConsumer.class);

  private final ShutdownHandler shutdownHandler;

  private final TenantReference<TenantScheduledThreadPoolExecutor> scheduleExecutors;

  private final TenantReference<TenantThreadPoolExecutor> executors;

  private final TenantReference<DispatchStrategy> dispatchStrategies;

  private final TenantReference<ScheduledFuture<?>> scheduledFutures;

  private final TenantReference<AtomicBoolean> running;

  private final TenantReference<Set<String>> queuedItemIds;

  /**
   * Set {@code true} in tests to prevent thread creation during unit tests.
   * When {@code true}, {@link #register()} returns immediately without scheduling.
   */
  public boolean disableForTesting = false;

  /**
   * Legacy constructor: the consumer owns a per-tenant {@link TenantThreadPoolExecutor} sized by
   * {@link #getWorkerThreadCount()}. Capacity is computed from the pool's core/active counts and
   * its internal queue size. Preserved bit-for-bit for existing subclasses (e.g.
   * {@code HostedComponentScanQueueConsumer}).
   */
  protected AbstractPollDispatchQueueConsumer(final String consumerName, final ShutdownHandler shutdownHandler) {
    super(consumerName);
    this.shutdownHandler = shutdownHandler;
    this.scheduleExecutors = new TenantReference<>(this::createScheduledExecutorService);
    this.executors = new TenantReference<>(this::createExecutorService);
    this.dispatchStrategies = new TenantReference<>(this::createLegacyDispatchStrategy);
    this.scheduledFutures = new TenantReference<>();
    this.running = new TenantReference<>(AtomicBoolean::new);
    this.queuedItemIds = new TenantReference<>(ConcurrentHashMap::newKeySet);
  }

  /**
   * Injected-executor constructor: the consumer accepts a pre-built per-tenant
   * {@link ExecutorService} and a {@link Semaphore} that caps the number of in-flight jobs.
   * Capacity is reported as {@link Semaphore#availablePermits()}; dispatch acquires a permit
   * before {@code submit()} and releases it when the task finishes (success or failure).
   * <p>
   * This path enables virtual-thread-based execution where the legacy capacity math
   * ({@code corePoolSize - activeCount + maxQueuedRows - queue.size()}) does not apply, and is
   * the model used by the unified continuous monitoring queue (Hosted Repo v1 design).
   * <p>
   * The suppliers are invoked once per tenant via {@link TenantReference} and must return
   * tenant-specific instances. Lifecycle of the supplied executor is managed by the caller — the
   * consumer does not register it with {@link ShutdownHandler} or call {@code shutdown()} on it.
   * The consumer still owns its own scheduler and continues to use {@link ShutdownHandler} for
   * that.
   * <p>
   * <strong>Graceful shutdown limitation:</strong> Jobs that were acquired (status IN_PROGRESS
   * in the database) but not yet started by the executor are NOT automatically unacquired on
   * shutdown. The caller's subclass must implement {@code recoverStaleJobs()} to reset these
   * on startup. This differs from the legacy path where {@code clearQueueAndUnacquireJobs()}
   * handles this automatically.
   * <p>
   * <strong>cleanup() ordering contract:</strong> Callers MUST shut down the injected executor
   * and await its termination before invoking {@link #cleanup()}. Calling cleanup() while a
   * worker is mid-execution opens a TOCTOU window in which cleanup() unacquires a row
   * ({@code IN_PROGRESS → PENDING}) that the worker is about to mark complete; both DAO updates
   * are guarded on {@code status='IN_PROGRESS'} so the worker's update silently no-ops and the
   * row is picked up again on the next poll, causing duplicate execution. Drain-then-cleanup
   * avoids the race.
   *
   * @param executorSupplier supplies the per-tenant executor that will run job tasks
   * @param semaphoreSupplier supplies the per-tenant semaphore whose permit count equals the
   *          maximum number of in-flight jobs
   */
  protected AbstractPollDispatchQueueConsumer(
      final String consumerName,
      final ShutdownHandler shutdownHandler,
      final Supplier<ExecutorService> executorSupplier,
      final Supplier<Semaphore> semaphoreSupplier)
  {
    super(consumerName);
    Objects.requireNonNull(executorSupplier, "executorSupplier must not be null");
    Objects.requireNonNull(semaphoreSupplier, "semaphoreSupplier must not be null");
    this.shutdownHandler = shutdownHandler;
    this.scheduleExecutors = new TenantReference<>(this::createScheduledExecutorService);
    this.executors = new TenantReference<>();
    this.dispatchStrategies = new TenantReference<>(
        () -> createInjectedDispatchStrategy(executorSupplier.get(), semaphoreSupplier.get()));
    this.scheduledFutures = new TenantReference<>();
    this.running = new TenantReference<>(AtomicBoolean::new);
    this.queuedItemIds = new TenantReference<>(ConcurrentHashMap::newKeySet);
  }

  /**
   * Acquire up to {@code limit} pending jobs from the database, marking them IN_PROGRESS.
   * Implementations should use {@code SELECT ... FOR UPDATE SKIP LOCKED} for concurrency safety.
   */
  protected abstract List<T> acquireJobs(int limit);

  /** Returns the stable string ID of a job (used for in-flight tracking and unacquire). */
  protected abstract String getJobId(T job);

  /** Executes the job. Should throw on failure so the framework can apply retry logic. */
  protected abstract void executeJob(T job) throws Exception;

  /** Called after a job executes successfully (e.g. mark COMPLETE in DB). */
  protected abstract void onJobSuccess(T job);

  /**
   * Increments the retry counter for the given job and returns the new count.
   * Used by {@link #onJobFailure} to decide whether to retry or permanently fail the job.
   */
  protected abstract int incrementRetryCount(T job);

  /**
   * Unacquires a set of job IDs back to PENDING so they can be retried on the next poll.
   * Called when a job fails but has not exceeded {@link #getMaxRetries()}, and also on
   * graceful shutdown for jobs that were queued but not yet started.
   */
  protected abstract void unacquireJobs(Set<String> ids);

  /**
   * Permanently marks a job as failed after exhausting all retry attempts.
   * Called by {@link #onJobFailure} when {@link #incrementRetryCount} reaches {@link #getMaxRetries()}.
   */
  protected abstract void permanentlyFailJob(T job, Exception cause);

  /** Number of worker threads in the pool per tenant. */
  protected abstract int getWorkerThreadCount();

  /**
   * Maximum number of jobs that may sit in the executor's internal queue waiting for a free
   * worker. Combined with {@link #getWorkerThreadCount()} to calculate rows to acquire per tick.
   */
  protected abstract int getMaxQueuedRows();

  /** How often (in milliseconds) the scheduler polls for pending work. */
  protected abstract long getPollIntervalMs();

  /**
   * Maximum number of retry attempts before a job is permanently failed.
   * Returning {@code Integer.MAX_VALUE} means retry indefinitely (no permanent failure).
   */
  protected abstract int getMaxRetries();

  /** Short identifier used as thread-name prefix (e.g. {@code "HostedComponentScanQueue"}). */
  protected abstract String getConsumerName();

  /**
   * Jitter seed string for computing the initial scheduler delay.
   * Typically combines the application instance ID and tenant slug to spread DB contention
   * across nodes and tenants on restart.
   */
  protected abstract String getJitterSeed();

  /**
   * Returns whether this consumer is currently enabled.
   * When {@code false}, {@link #run()} is a no-op regardless of how it is triggered
   * (scheduled poll or direct call via {@link #triggerProcessing()}).
   * Default is {@code true}. Override to wire in live configuration.
   */
  protected boolean isEnabled() {
    return true;
  }

  /**
   * Called once in {@link #register()} before scheduling begins.
   * Override to reset stale IN_PROGRESS rows to PENDING after a crash.
   * Default is a no-op.
   * <p>
   * <strong>Injected-executor path requirement:</strong> Subclasses constructed via the
   * {@link #AbstractPollDispatchQueueConsumer(String, ShutdownHandler, Supplier, Supplier)
   * injected-executor constructor} MUST override this method. The injected path does not
   * automatically unacquire rows on shutdown (see that constructor's Javadoc); this method is
   * the only recovery mechanism for IN_PROGRESS rows from a previous unclean shutdown.
   */
  protected void recoverStaleJobs() {
    // no-op by default
  }

  /**
   * Called when scheduling is enabled or the poll interval changes.
   * Override if the subclass needs to react to a reschedule (e.g. log the new interval).
   */
  protected void onReschedule(final long pollIntervalMs, final long initialDelayMs) {
    // no-op by default
  }

  /**
   * Triggered via {@code POST /tasks/{consumerName}} on the admin port.
   * Forces an immediate processing run for the current tenant.
   * <p>
   * Note: {@link #triggerProcessing()} only <em>submits</em> jobs asynchronously — when this
   * method returns, the dispatched jobs have not necessarily completed. The response wording
   * reflects that.
   */
  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) throws Exception {
    log.info("Manual request to run {}.", getConsumerName());
    triggerProcessing();
    output.write("Triggered manual execution of " + getConsumerName() + ".\n");
  }

  /**
   * Call from the subclass {@code configurationChanged()} to handle a live configuration update.
   * Resizes the thread pool if the worker count changed, and reschedules only when the enabled
   * flag or poll interval actually changed — avoiding unnecessary cancellation and recreation of
   * the scheduled future when unrelated fields (e.g. maxRetries, maxQueuedRows) are updated.
   *
   * <pre>{@code
   * @Override
   * public void configurationChanged(Set<String> propertyNames) {
   *   if (propertyNames.contains(MY_PROPERTY_KEY)) {
   *     HostedComponentScanQueueConfig old = configs.get();
   *     reloadConfig();
   *     handleConfigurationChanged(newWorkerCount, newPollIntervalMs, enabled,
   *         old.pollIntervalMilliseconds(), old.enabled());
   *   }
   * }
   * }</pre>
   *
   * @param newWorkerCount new desired worker thread count (ignored for injected-executor path;
   *          the caller owns executor sizing and must resize externally)
   * @param newPollIntervalMs new poll interval in milliseconds
   * @param enabled whether the consumer should be scheduled
   * @param oldPollIntervalMs previous poll interval — used to detect whether rescheduling is needed
   * @param wasEnabled previous enabled state — used to detect whether rescheduling is needed
   */
  protected void handleConfigurationChanged(
      final int newWorkerCount,
      final long newPollIntervalMs,
      final boolean enabled,
      final long oldPollIntervalMs,
      final boolean wasEnabled)
  {
    ThreadPoolExecutor executor = executors.get();
    if (executor != null) {
      if (newWorkerCount > executor.getCorePoolSize()) {
        executor.setMaximumPoolSize(newWorkerCount);
        executor.setCorePoolSize(newWorkerCount);
      }
      else if (newWorkerCount < executor.getCorePoolSize()) {
        executor.setCorePoolSize(newWorkerCount);
        executor.setMaximumPoolSize(newWorkerCount);
      }
    }
    else {
      // Injected-executor path: caller owns sizing of the executor and the semaphore. Live
      // reconfiguration is out of scope here; emit a visible signal so operators tuning the
      // worker count via live config see that an additional step (caller-side resize) is needed.
      log.warn("{}: ignoring newWorkerCount={} on injected-executor path — resize the executor "
          + "and semaphore externally.", getConsumerName(), newWorkerCount);
    }

    if (enabled != wasEnabled || newPollIntervalMs != oldPollIntervalMs) {
      reschedule(enabled);
    }
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    recoverStaleJobs();
    reschedule(isEnabled());
  }

  /**
   * Triggers an immediate processing run for the current tenant.
   * Used by the REST endpoint after enqueueing a new job to reduce latency.
   * If the consumer is already running for this tenant the call is a no-op.
   */
  public void triggerProcessing() {
    tryRun();
  }

  private void reschedule(final boolean enabled) {
    ScheduledFuture<?> existing = scheduledFutures.get();
    if (existing != null) {
      existing.cancel(false);
    }

    if (enabled) {
      long initialDelay = getInitialDelay(getJitterSeed(), getPollIntervalMs());
      onReschedule(getPollIntervalMs(), initialDelay);
      scheduledFutures.set(
          scheduleExecutors.get()
              .scheduleAtFixedRate(
                  this::tryRun,
                  initialDelay,
                  getPollIntervalMs(),
                  TimeUnit.MILLISECONDS));
    }
    else {
      log.debug("{}: unscheduled — disabled by configuration.", getConsumerName());
      scheduledFutures.remove();
    }
  }

  /**
   * Computes a hash-based initial delay in {@code [0, periodInMilliseconds)}.
   * Spreading initial delays across nodes and tenants avoids thundering-herd DB contention
   * on restart.
   */
  public static long getInitialDelay(final String jitterSeed, final long periodInMilliseconds) {
    if (periodInMilliseconds <= 0) {
      return 0L;
    }
    return Integer.toUnsignedLong(jitterSeed.hashCode()) % periodInMilliseconds;
  }

  private void tryRun() {
    try {
      run();
    }
    catch (InterruptedException e) {
      throw new UncheckedInterruptedException(e);
    }
  }

  public void run() throws InterruptedException {
    if (!isEnabled()) {
      log.debug("{}: consumer is disabled, skipping poll.", getConsumerName());
      return;
    }
    if (running.get().getAndSet(true)) {
      log.debug("{}: consumer already running, skipping poll.", getConsumerName());
      return;
    }
    try {
      DispatchStrategy strategy = dispatchStrategies.get();
      int rowsToAcquire = strategy.availableCapacity();
      if (rowsToAcquire <= 0) {
        log.debug("{}: no capacity for new jobs.", getConsumerName());
        return;
      }

      List<T> acquired = acquireJobs(rowsToAcquire);
      if (!acquired.isEmpty()) {
        Set<String> inflight = queuedItemIds.get();
        acquired.forEach(job -> {
          String jobId = getJobId(job);
          inflight.add(jobId);
          strategy.dispatch(new QueueTask(jobId, () -> processJob(job), inflight::remove));
        });
        log.debug("{}: acquired {} jobs for processing.", getConsumerName(), acquired.size());
      }
    }
    finally {
      running.get().set(false);
    }
  }

  private void processJob(final T job) {
    try {
      executeJob(job);
      onJobSuccess(job);
    }
    catch (InterruptedException e) {
      onJobFailure(job, e);
      throw new UncheckedInterruptedException(e);
    }
    catch (RuntimeException e) {
      onJobFailure(job, e);
      throw e;
    }
    catch (Exception e) {
      onJobFailure(job, e);
      throw new RuntimeException(e);
    }
    catch (Error e) {
      // Don't call onJobFailure here — the JVM may be unsafe (OOME, StackOverflowError, etc.) and
      // a DB call could compound the failure. Don't swallow either: operators must see fatal
      // failures. On the injected-executor path, the semaphore permit is released by the dispatch
      // lambda's finally{} (the legacy path has no semaphore); on either path the row stays
      // IN_PROGRESS and is reclaimed by recoverStaleJobs() on next startup.
      log.error("{}: job id={} threw fatal Error.", getConsumerName(), getJobId(job), e);
      throw e;
    }
  }

  /**
   * Handles a job failure. Increments the persistent retry counter; if below
   * {@link #getMaxRetries()} the job is returned to PENDING for automatic retry on the next
   * poll tick. Once the limit is reached the job is permanently failed.
   * <p>
   * Override to change retry semantics (e.g. add exponential back-off, dead-letter queuing).
   */
  protected void onJobFailure(final T job, final Exception e) {
    int attempts = incrementRetryCount(job);
    if (attempts < getMaxRetries()) {
      log.warn("{}: job id={} failed (attempt {}/{}), returning to PENDING for retry.",
          getConsumerName(), getJobId(job), attempts, getMaxRetries(), e);
      unacquireJobs(Set.of(getJobId(job)));
    }
    else {
      log.error("{}: job id={} exhausted {} retries, marking as permanently FAILED.",
          getConsumerName(), getJobId(job), getMaxRetries(), e);
      permanentlyFailJob(job, e);
    }
  }

  private TenantThreadPoolExecutor createExecutorService() {
    int threadCount = getWorkerThreadCount();
    TenantThreadPoolExecutor executor = new TenantThreadPoolExecutor(
        threadCount,
        threadCount,
        5L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        new ThreadFactoryBuilder().setNameFormat(getConsumerName() + "-%d").build(),
        new ThreadPoolExecutor.AbortPolicy(),
        getConsumerName().toLowerCase(),
        getClass().getSimpleName())
    {
      @Override
      public Future<?> submit(final Runnable task) {
        return super.submit(task);
      }

      @Override
      public void shutdown() {
        super.shutdown();
        clearQueueAndUnacquireJobs();
      }

      @Override
      public java.util.List<Runnable> shutdownNow() {
        java.util.List<Runnable> result = super.shutdownNow();
        clearQueueAndUnacquireJobs();
        return result;
      }

      private void clearQueueAndUnacquireJobs() {
        getQueue().clear();
        Set<String> ids = queuedItemIds.remove();
        if (ids != null && !ids.isEmpty()) {
          unacquireJobs(ids);
        }
      }
    };
    executor.allowCoreThreadTimeOut(true);
    shutdownHandler.add(executor);
    return executor;
  }

  private TenantScheduledThreadPoolExecutor createScheduledExecutorService() {
    TenantScheduledThreadPoolExecutor scheduler = new TenantScheduledThreadPoolExecutor(
        1,
        new ThreadFactoryBuilder()
            .setNameFormat(getConsumerName() + "Scheduler-%d")
            .setDaemon(true)
            .build());
    shutdownHandler.add(scheduler);
    return scheduler;
  }

  /**
   * Per-tenant dispatch seam. Two implementations: legacy {@link TenantThreadPoolExecutor} (used
   * by existing callers) and an injected {@link ExecutorService} + {@link Semaphore} (used by the
   * unified continuous monitoring queue). The strategy decides both how much capacity is
   * available on each poll tick and how a {@link QueueTask} is submitted to a worker.
   */
  private interface DispatchStrategy
  {
    int availableCapacity();

    void dispatch(QueueTask task);
  }

  private DispatchStrategy createLegacyDispatchStrategy() {
    final TenantThreadPoolExecutor executor = executors.get();
    return new DispatchStrategy()
    {
      @Override
      public int availableCapacity() {
        return (executor.getCorePoolSize() - executor.getActiveCount())
            + getMaxQueuedRows() - executor.getQueue().size();
      }

      @Override
      public void dispatch(final QueueTask task) {
        executor.submit(task);
      }
    };
  }

  private DispatchStrategy createInjectedDispatchStrategy(
      final ExecutorService executor,
      final Semaphore semaphore)
  {
    Objects.requireNonNull(executor, "injected executor must not be null");
    Objects.requireNonNull(semaphore, "injected semaphore must not be null");
    return new DispatchStrategy()
    {
      @Override
      public int availableCapacity() {
        // Pull at most one row per poll tick (CLM-40039 design §6.2). The semaphore's permit
        // count still gates whether we have any worker capacity at all; if no permits are free,
        // we report zero so the tick is skipped.
        return semaphore.availablePermits() > 0 ? 1 : 0;
      }

      @Override
      public void dispatch(final QueueTask task) {
        // tryAcquire is non-blocking: capacity was checked just above under the running latch (the
        // 'running' AtomicBoolean prevents concurrent dispatch() calls for the same tenant), so
        // a permit should always be available. This branch handles the theoretical race where a
        // permit is consumed between availableCapacity() and tryAcquire() — it should never fire
        // under normal operation but provides defensive recovery if it does.
        if (!semaphore.tryAcquire()) {
          log.debug("{}: semaphore drained between capacity check and dispatch; deferring job id={}.",
              getConsumerName(), task.getJobId());
          // The job is still in queuedItemIds and IN_PROGRESS in the DB; reset so it is retried.
          Set<String> inflight = queuedItemIds.get();
          inflight.remove(task.getJobId());
          unacquireJobs(Set.of(task.getJobId()));
          return;
        }
        try {
          executor.submit(() -> {
            try {
              task.run();
            }
            finally {
              semaphore.release();
            }
          });
        }
        catch (java.util.concurrent.RejectedExecutionException e) {
          // Executor was shut down between tryAcquire() and submit() — release the permit
          // and unacquire the job so it can be retried on next startup.
          semaphore.release();
          queuedItemIds.get().remove(task.getJobId());
          unacquireJobs(Set.of(task.getJobId()));
          log.warn("{}: executor rejected job id={}; releasing permit and deferring.",
              getConsumerName(), task.getJobId());
        }
      }
    };
  }

  public void cleanup() {
    ScheduledFuture<?> future = scheduledFutures.remove();
    if (future != null) {
      future.cancel(true);
    }
    TenantScheduledThreadPoolExecutor scheduler = scheduleExecutors.remove();
    if (scheduler != null) {
      scheduler.shutdownNow();
    }
    TenantThreadPoolExecutor executor = executors.remove();
    if (executor != null) {
      executor.shutdownNow();
    }
    // Injected executor lifecycle is owned by the caller; we drop the strategy reference only.
    dispatchStrategies.remove();
    // On the injected-executor path, queuedItemIds may still contain a job that was submitted
    // to the executor but never started (its QueueTask.onStart callback never fired). Unacquire
    // those rows so they return to PENDING for startup recovery.
    // On the legacy path, clearQueueAndUnacquireJobs() inside executor.shutdownNow() already
    // drained and removed queuedItemIds, so remove() returns null here and the block is skipped.
    Set<String> inflight = queuedItemIds.remove();
    if (inflight != null && !inflight.isEmpty()) {
      unacquireJobs(inflight);
    }
    running.remove();
  }
}
