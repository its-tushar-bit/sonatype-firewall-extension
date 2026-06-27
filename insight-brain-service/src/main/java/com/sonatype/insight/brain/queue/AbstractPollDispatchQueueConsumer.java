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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.sonatype.insight.brain.common.exception.ExceptionHelper;
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
   * Per-tenant reference to the {@link AdjustableSemaphore} created by
   * {@link #createInjectedDispatchStrategy}. Allows {@link #handleConfigurationChanged} to resize
   * the per-tenant permit count on a live workerThreads change. Empty on the legacy
   * (non-injected) constructor path.
   */
  private final TenantReference<AdjustableSemaphore> injectedSemaphores;

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
    this.injectedSemaphores = new TenantReference<>();
  }

  /**
   * Injected-executor constructor: the consumer accepts a pre-built per-tenant
   * {@link ExecutorService} and an {@link AdjustableSemaphore} that caps the number of in-flight
   * drain-workers. Capacity is reported via {@link AdjustableSemaphore#availablePermits()};
   * dispatch acquires a permit before {@code submit()} and the spawned drain-worker releases the
   * permit when it exits (after processing the seed record and draining any additional pending
   * records).
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
   * @param semaphoreSupplier supplies the per-tenant {@link AdjustableSemaphore} whose permit
   *          count equals the maximum number of in-flight drain-workers. The adjustable form is
   *          required so live config changes to {@code workerThreads} can resize without
   *          recreating the executor.
   * @apiNote The semaphore supplier was widened from {@code Supplier<Semaphore>} to
   *          {@code Supplier<AdjustableSemaphore>}. The only production caller is
   *          {@code RepositoryEvaluationQueueConsumer}; out-of-tree subclasses using the
   *          injected-executor constructor must update their supplier to return
   *          {@code AdjustableSemaphore}.
   */
  protected AbstractPollDispatchQueueConsumer(
      final String consumerName,
      final ShutdownHandler shutdownHandler,
      final Supplier<ExecutorService> executorSupplier,
      final Supplier<AdjustableSemaphore> semaphoreSupplier)
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
    this.injectedSemaphores = new TenantReference<>();
  }

  /**
   * Acquire up to {@code limit} pending jobs from the database, marking them IN_PROGRESS.
   * Implementations should use {@code SELECT ... FOR UPDATE SKIP LOCKED} for concurrency safety.
   */
  protected abstract List<T> acquireJobs(int limit);

  /**
   * Acquire exactly one pending job for the per-worker drain loop. Called by each
   * drain-worker after it finishes a record, to fetch the next one without waiting for the
   * scheduler tick. Implementations should use the same {@code SELECT ... FOR UPDATE SKIP LOCKED}
   * pattern as {@link #acquireJobs} with {@code limit=1}.
   * <p>
   * The default delegates to {@code acquireJobs(1)}; subclasses may override for a more direct
   * single-row implementation if it produces better plans.
   *
   * @return an Optional holding the acquired job, or empty if the queue has no pending rows
   */
  protected Optional<T> acquireOneMore() {
    List<T> one = acquireJobs(1);
    return one.isEmpty() ? Optional.empty() : Optional.of(one.get(0));
  }

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
   * <p>
   * <strong>Contract on the injected-executor path:</strong> subclasses constructed via
   * {@link #AbstractPollDispatchQueueConsumer(String, ShutdownHandler, Supplier, Supplier) the
   * injected-executor constructor} should return {@code 0} here. Back-pressure on that path is
   * enforced by the injected {@link AdjustableSemaphore}'s permit count rather than by an executor queue
   * cap, and the injected dispatch strategy ignores this method's return value.
   */
  protected abstract int getMaxQueuedRows();

  /** How often (in milliseconds) the scheduler polls for pending work. */
  protected abstract long getPollIntervalMs();

  /**
   * Maximum number of retry attempts before a job is permanently failed.
   * Returning {@code Integer.MAX_VALUE} means retry indefinitely (no permanent failure).
   */
  protected abstract int getMaxRetries();

  /**
   * Rows acquired in one scheduler tick to seed idle drain-workers in a single round-trip.
   * Only meaningful on the injected-executor path. The tick acquires up to
   * {@code min(availablePermits, getTickBatchSize())} rows, spawning one drain-worker per row.
   * Each spawned worker then runs its own self-poll drain loop (see {@link #acquireOneMore}).
   * <p>
   * Default {@code 1} preserves the original 1-row-per-tick behaviour; subclasses adopting the
   * drain-loop model override to return {@code workerThreads} (or higher) so a cold-start tick
   * saturates all idle workers in one round-trip.
   */
  protected int getTickBatchSize() {
    return 1;
  }

  /**
   * Optional grace period a drain-worker waits when {@link #acquireOneMore} returns empty before
   * exiting the loop. Default {@code 0} means exit immediately on the first empty
   * self-poll. A non-zero value lets a worker linger briefly to absorb a near-empty-queue
   * trickle without paying the re-spawn cost; rarely needed.
   */
  protected long getIdleBackoffMs() {
    return 0L;
  }

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
   * @param newWorkerCount new desired worker thread count. On the legacy path the platform-thread
   *          pool is resized. On the injected-executor path the {@link AdjustableSemaphore} is
   *          resized so the cap on concurrent drain-workers takes effect immediately
   *          for the next dispatch; the executor itself is caller-owned and does not need resizing
   *          because it spawns virtual threads on demand.
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
      // Injected-executor path: resize the AdjustableSemaphore so the new permit count gates the
      // next dispatch. Increasing releases extra permits immediately; decreasing reduces the cap
      // without interrupting running workers (they keep their permits and release on exit). The
      // injected executor is caller-owned and does not need resizing — it spawns virtual threads
      // on demand bounded by the semaphore.
      AdjustableSemaphore semaphore = injectedSemaphores.get();
      if (semaphore != null) {
        semaphore.resize(newWorkerCount);
        log.info("{}: resized semaphore to {} permits (live workerThreads change).",
            getConsumerName(), newWorkerCount);
      }
      else {
        log.debug("{}: workerCount change with no injected semaphore yet (strategy not initialised "
            + "for this tenant); will pick up newWorkerCount={} on first dispatch.",
            getConsumerName(), newWorkerCount);
      }
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
    // Gate recoverStaleJobs() on isEnabled() (CLM-40971): when the feature is off there are no
    // IN_PROGRESS rows to reclaim, and the workerId is freshly generated each boot so the
    // resulting UPDATE matches 0 rows by construction. Without this gate, every disabled-tenant
    // boot pays for one pointless DB round-trip — at fleet scale that is N tenants × every
    // restart. Subclasses that need recovery to run unconditionally should override register()
    // instead of relying on this gate.
    if (isEnabled()) {
      recoverStaleJobs();
    }
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
      final AdjustableSemaphore semaphore)
  {
    Objects.requireNonNull(executor, "injected executor must not be null");
    Objects.requireNonNull(semaphore, "injected semaphore must not be null");
    // Stash the semaphore so handleConfigurationChanged() can resize it on a live worker-count
    // change. The per-tenant TenantReference holds one strategy per tenant; the tenant-scoped
    // lookup happens in handleConfigurationChanged via dispatchStrategies.get().
    injectedSemaphores.set(semaphore);
    return new DispatchStrategy()
    {
      @Override
      public int availableCapacity() {
        // Cold-start tick acquires up to min(permits, tickBatchSize) rows in one round-trip to
        // seed all idle workers. Each spawned worker then runs its own drain loop (see dispatch()
        // below) and continues to pull single rows until the queue empties.
        int permits = semaphore.availablePermits();
        if (permits <= 0) {
          return 0;
        }
        int tickBatch = getTickBatchSize();
        return Math.min(permits, Math.max(1, tickBatch));
      }

      @Override
      public void dispatch(final QueueTask task) {
        // tryAcquire can race a live resize() shrinking the cap mid-tick; the defer block
        // below unacquires this row so it's retried on next tick.
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
          // Drain-worker: process the seed record passed in from the tick, then loop on
          // acquireOneMore() until the queue is empty. The semaphore permit acquired above is
          // held for the lifetime of the worker (many records), not per record. Each record's
          // processing is a single QueueTask.run() pass; on empty self-poll the worker exits and
          // the permit is released in finally so the next scheduler tick can re-spawn workers.
          executor.submit(() -> {
            try {
              // If task.run() throws Error (OOME, StackOverflowError, etc.) processJob rethrows
              // it; drainAdditional() is then skipped — JVM state may be unsafe and a DB call
              // could compound the failure. The finally{} below still runs and releases the
              // permit so the next scheduler tick can re-spawn workers. The stranded
              // IN_PROGRESS row (if any) is reclaimed by recoverStaleJobs on next startup.
              task.run();
              drainAdditional();
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

  /**
   * Drains additional records by repeatedly calling {@link #acquireOneMore} until empty.
   * Called by a drain-worker after it finishes processing the seed record from the
   * tick. Each iteration is a separate {@code acquireOneMore} round-trip; the worker continues
   * until the queue returns empty or the thread is interrupted. Optional {@link #getIdleBackoffMs}
   * grace period before exit lets a worker absorb a near-empty trickle.
   */
  private void drainAdditional() {
    while (!Thread.currentThread().isInterrupted()) {
      Optional<T> next;
      try {
        next = acquireOneMore();
      }
      catch (RuntimeException e) {
        // A DAO failure in the drain loop (DB blip, lock timeout) — log and exit the loop so the
        // permit returns to the semaphore. The next scheduler tick will retry. We do NOT propagate
        // the exception because the seed record has already been processed successfully; failing
        // the drain attempt should not retroactively fail the seed. Preserve any pending interrupt
        // signal so downstream code observes the shutdown.
        if (isShutdownSignal(e)) {
          Thread.currentThread().interrupt();
        }
        log.info("{}: acquireOneMore failed during drain; exiting worker drain loop.",
            getConsumerName(), e);
        return;
      }
      if (next.isEmpty()) {
        long backoff = getIdleBackoffMs();
        if (backoff <= 0) {
          return;
        }
        try {
          Thread.sleep(backoff);
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
        // One retry after the backoff; if still empty, exit.
        try {
          next = acquireOneMore();
        }
        catch (RuntimeException e) {
          if (isShutdownSignal(e)) {
            Thread.currentThread().interrupt();
          }
          log.info("{}: acquireOneMore failed during drain (post-backoff); exiting worker drain loop.",
              getConsumerName(), e);
          return;
        }
        if (next.isEmpty()) {
          return;
        }
      }
      T job = next.get();
      String jobId = getJobId(job);
      Set<String> inflight = queuedItemIds.get();
      inflight.add(jobId);
      try {
        processJob(job);
      }
      catch (RuntimeException e) {
        // processJob wraps InterruptedException in UncheckedInterruptedException (which clears
        // the interrupt flag) and flow processors may wrap interrupts in their own RuntimeExceptions
        // or attach them via addSuppressed. isShutdownSignal walks the full cause and suppressed
        // chains; if the flag is set without an interrupt in the chain (caller interrupted us
        // between iterations) Thread.interrupted() catches that. Either way, exit the drain
        // immediately so we honor the shutdown.
        if (isShutdownSignal(e)) {
          Thread.currentThread().interrupt();
          return;
        }
        // Otherwise log and continue draining: a transient failure in one record should not abort
        // the remaining batch. The failed record's retry count is incremented in processJob via
        // onJobFailure; if retry-exhausted it is deleted.
        log.info("{}: processJob failed for job id={}; continuing drain.", getConsumerName(), jobId, e);
      }
      finally {
        inflight.remove(jobId);
      }
    }
  }

  /**
   * True if {@code e} carries an interrupt signal — either directly (an
   * {@link UncheckedInterruptedException}), nested in its cause or suppressed chain (a flow
   * processor wrapping {@code InterruptedException} in its own {@code RuntimeException}), or
   * indirectly via the current thread's interrupt flag (caller interrupted us between calls but
   * the thrown exception is unrelated). {@link Thread#interrupted()} clears the flag, so the
   * callers must re-set it via {@code Thread.currentThread().interrupt()} before returning.
   */
  private static boolean isShutdownSignal(final RuntimeException e) {
    return e instanceof UncheckedInterruptedException
        || ExceptionHelper.hasCauseOrSuppressedOfType(e, InterruptedException.class)
        || Thread.interrupted();
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
    // Injected executor lifecycle is owned by the caller; we drop the strategy reference and the
    // semaphore tenant-binding only.
    dispatchStrategies.remove();
    injectedSemaphores.remove();
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
