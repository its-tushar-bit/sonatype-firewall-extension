/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

import com.sonatype.insight.brain.api.v2.service.ConfigurationListener;
import com.sonatype.insight.brain.common.exception.ExceptionHelper;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.ContinuousMonitoringQueueItemDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;
import com.sonatype.insight.brain.queue.AbstractPollDispatchQueueConsumer;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantVirtualThreadExecutor;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Poll-and-dispatch consumer for the unified continuous monitoring queue (CLM-40039 §6.2).
 * Each tenant gets its own {@link TenantVirtualThreadExecutor} and {@link Semaphore} via the
 * abstract base's {@code TenantReference} machinery; the semaphore's permit count equals
 * {@code continuousMonitoringWorkerThreads} and gates how many jobs are in flight per tenant.
 * <p>
 * Retry budget is read from system-configuration properties on every poll so runtime tuning
 * takes effect without a restart. Worker thread count requires a server restart (the semaphore
 * is constructed once at tenant registration time). On each poll tick the consumer acquires one
 * pending parent row for the {@code HOSTED_REPO} flow per design §6.2, dispatches it to a
 * {@link ContinuousMonitoringFlowProcessor}, and applies the framework retry policy (§7.1) on
 * failure: transient throwables under the retry limit go back to PENDING via {@code markRetry};
 * non-retryable throwables and retry-exhausted items are deleted from the queue.
 */
@Named
@Singleton
public class RepositoryEvaluationQueueConsumer
    extends AbstractPollDispatchQueueConsumer<ContinuousMonitoringQueueItem>
    implements ConfigurationListener
{
  public static final String NAME = "RepositoryEvaluationQueueConsumer";

  /**
   * Default poll cadence (5 minutes) — aligned with {@code EvaluationQueueConsumer}, the
   * architecturally identical precedent: same shape (daily Quartz producer + queue + consumer),
   * same use case (background re-evaluation, no human waiting), same MTIQ-per-tenant pattern, in
   * production across hundreds of tenants for years. See
   * {@code EvaluationQueueConfig.DEFAULT_CONSUMER_PERIOD}.
   * <p>
   * On the hosted-repo flow the producer fires once per day, so the queue is empty for ~23h/day.
   * A 5-minute polling cadence puts a ceiling of 5 minutes on dispatch latency from the moment
   * the producer enqueues a batch — invisible against a daily cycle, while keeping the empty-queue
   * probe rate at ~288 polls/tenant/day instead of ~345,600. Once a batch lands, each poll
   * acquires up to {@code continuousMonitoringWorkerThreads} rows and the semaphore-gated executor
   * drains continuously, so the 5-minute cadence affects only first-row latency, not throughput.
   * <p>
   * Compare to {@code HostedComponentScanQueueConsumer}'s 30 s default, chosen for an interactive
   * scan path with very different latency expectations.
   */
  static final long DEFAULT_POLL_INTERVAL_MS = 300_000L;

  private static final Logger log = LoggerFactory.getLogger(RepositoryEvaluationQueueConsumer.class);

  private final ContinuousMonitoringQueueItemDAO queueDAO;

  private final Map<ContinuousMonitoringFlowType, ContinuousMonitoringFlowProcessor> processorsByFlow;

  private final Configuration configuration;

  private final RetryPolicy retryPolicy;

  private final String workerId = UUID.randomUUID().toString();

  /**
   * Per-tenant snapshot of the last poll-interval and enabled value applied via
   * {@link #handleConfigurationChanged}. Mirrors {@code HostedComponentScanQueueConsumer.configs}:
   * lets us hand the real previous values to the base class so reschedules only happen when a
   * watched value actually changed. CLM-40971: replaces the earlier synthetic-delta approach which
   * forced a reschedule on every event and reset the initial jitter delay.
   */
  private final TenantReference<ConfigSnapshot> lastApplied;

  @Inject
  public RepositoryEvaluationQueueConsumer(
      final ShutdownHandler shutdownHandler,
      final ContinuousMonitoringQueueItemDAO queueDAO,
      final Set<ContinuousMonitoringFlowProcessor> flowProcessors,
      final Configuration configuration,
      @Nullable final MeterRegistry meterRegistry)
  {
    super(NAME, shutdownHandler,
        () -> createWorkerExecutor(meterRegistry),
        () -> new Semaphore(readWorkerThreads(configuration)));
    this.queueDAO = queueDAO;
    this.processorsByFlow = indexByFlow(flowProcessors);
    this.configuration = configuration;
    this.retryPolicy = new DefaultRetryPolicy(this::readMaxRetries);
    this.lastApplied = new TenantReference<>(this::loadSnapshot);
  }

  private ConfigSnapshot loadSnapshot() {
    return new ConfigSnapshot(readPollIntervalMs(configuration), isEnabled());
  }

  private record ConfigSnapshot(long pollIntervalMs, boolean enabled)
  {
  }

  private static Map<ContinuousMonitoringFlowType, ContinuousMonitoringFlowProcessor> indexByFlow(
      final Set<ContinuousMonitoringFlowProcessor> flowProcessors)
  {
    Map<ContinuousMonitoringFlowType, ContinuousMonitoringFlowProcessor> map = new HashMap<>();
    for (ContinuousMonitoringFlowProcessor processor : flowProcessors) {
      ContinuousMonitoringFlowProcessor existing = map.put(processor.getFlowType(), processor);
      if (existing != null) {
        throw new IllegalStateException(
            "Duplicate ContinuousMonitoringFlowProcessor for flow " + processor.getFlowType()
                + ": " + existing.getClass().getName() + " and " + processor.getClass().getName());
      }
    }
    return Collections.unmodifiableMap(map);
  }

  private static ExecutorService createWorkerExecutor(@Nullable final MeterRegistry meterRegistry) {
    // Virtual threads, per CLM-40039 §6.2 / reviewer comment #10: each submitted task runs on its
    // own virtual thread, so concurrency is gated by the semaphore (permit count =
    // continuousMonitoringWorkerThreads) rather than by a fixed-size platform-thread pool. This
    // matches the precedent set by PolicyEvaluationVirtualThreadExecutor for the policy-eval path.
    return new TenantVirtualThreadExecutor(meterRegistry, "continuous_monitoring", NAME);
  }

  private static final int DEFAULT_WORKER_THREADS = 4;

  private static final int DEFAULT_MAX_RETRIES = 3;

  /**
   * Reads {@code continuousMonitoringWorkerThreads} from configuration with a defense-in-depth
   * floor (CLM-40971). The REST API enforces {@code [1, 64]} via {@link ConfigurationProperty},
   * but a value sourced directly from the DB (backup restore, manual UPDATE, partial migration)
   * could still be 0 or negative — which would zero out the semaphore permit count and silently
   * stop all dispatch. Falls back to {@link #DEFAULT_WORKER_THREADS} with a WARN if that happens.
   */
  private static int readWorkerThreads(final Configuration configuration) {
    Integer value = configuration.getContinuousMonitoringWorkerThreads();
    if (value == null) {
      return DEFAULT_WORKER_THREADS;
    }
    if (value <= 0) {
      log.warn("continuousMonitoringWorkerThreads={} is invalid (must be > 0); falling back to default {}",
          value, DEFAULT_WORKER_THREADS);
      return DEFAULT_WORKER_THREADS;
    }
    return value;
  }

  /**
   * Reads {@code maxContinuousMonitoringRetries} from configuration with a defense-in-depth
   * floor (CLM-40971). REST validates {@code [1, 100]}; DB-injected 0/negative values would
   * otherwise cause every job to be deleted on first failure (or never deleted, depending on
   * ordering). Falls back to {@link #DEFAULT_MAX_RETRIES} with a WARN.
   */
  private int readMaxRetries() {
    Integer value = configuration.getMaxContinuousMonitoringRetries();
    if (value == null) {
      return DEFAULT_MAX_RETRIES;
    }
    if (value <= 0) {
      log.warn("maxContinuousMonitoringRetries={} is invalid (must be > 0); falling back to default {}",
          value, DEFAULT_MAX_RETRIES);
      return DEFAULT_MAX_RETRIES;
    }
    return value;
  }

  /**
   * Acquires PENDING rows for the {@code HOSTED_REPO} flow only — this is intentional for v1 per
   * the design doc (one consumer per flow type). The Guice/Spring multibinding injects the full
   * {@code Set<ContinuousMonitoringFlowProcessor>} so that {@link #processorsByFlow} could lookup
   * processors for any flow type, but {@code acquireJobs} pulls only HOSTED_REPO rows from the
   * queue, so processors for other flow types are never reached through this consumer. SBOM and
   * LIFECYCLE flows will get their own consumers in follow-up PRs that pass their respective flow
   * types here. The mismatch between the open injection surface and the hardcoded acquire is the
   * v1-shape; the multi-flow extension pattern is in {@link AbstractContinuousMonitoringProducerJob}.
   */
  @Override
  protected List<ContinuousMonitoringQueueItem> acquireJobs(final int limit) {
    return queueDAO.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, workerId, limit);
  }

  @Override
  protected String getJobId(final ContinuousMonitoringQueueItem job) {
    return job.getId();
  }

  @Override
  protected void executeJob(final ContinuousMonitoringQueueItem job) throws Exception {
    ContinuousMonitoringFlowProcessor processor = processorsByFlow.get(job.getFlowType());
    if (processor == null) {
      // Defensive-only: acquireJobs filters to HOSTED_REPO and the constructor rejects duplicate
      // flow-processor bindings, so the lookup will always succeed for any item this consumer
      // dequeues in production. The branch fires only for an artificial test that builds the
      // consumer with Set.of() (no processors) — the IllegalStateException routes through
      // onJobFailure → non-retryable → deleteById, which is the right terminal state for an
      // un-processable row. Kept in place so that adding a new flow type without wiring a
      // processor surfaces explicitly rather than as silent dispatch.
      log.warn("{}: no processor registered for flow {}; discarding queueId={}.",
          getConsumerName(), job.getFlowType(), job.getId());
      throw new IllegalStateException(
          "No processor registered for flow " + job.getFlowType() + "; discarding " + job.getId());
    }
    processor.process(job);
  }

  @Override
  protected void onJobSuccess(final ContinuousMonitoringQueueItem job) {
    disposeOwnedJob(job, "success");
  }

  /**
   * Deletes the queue row only if it is still IN_PROGRESS under {@code workerId} (CLM-40971 M7).
   * A 0-row delete means {@code recoverStaleJobs} reset the row to PENDING and another worker
   * has since claimed it (a normal occurrence under MTIQ rolling restarts); the result of this
   * worker's processing is silently dropped, and the new owner will reprocess. Logged at INFO
   * because this is an expected outcome of the ownership guard, not a fault — operators looking
   * for recurring data-integrity issues should watch the per-reason drop counter on the flow
   * processor instead.
   */
  private void disposeOwnedJob(final ContinuousMonitoringQueueItem job, final String reason) {
    int rows = queueDAO.deleteById(job.getId(), workerId);
    if (rows == 0) {
      log.info("{}: queueId={} ownership transferred during {} (post-stale-recovery reclaim); "
          + "result discarded.",
          getConsumerName(), job.getId(), reason);
    }
  }

  @Override
  protected int incrementRetryCount(final ContinuousMonitoringQueueItem job) {
    // Not used: retry decisions go through onJobFailure which calls markRetry directly so the
    // increment, error_message capture, and unacquire happen atomically.
    return job.getRetryCount();
  }

  @Override
  protected void unacquireJobs(final Set<String> ids) {
    for (String id : ids) {
      queueDAO.unacquire(id);
    }
  }

  /**
   * Defensive-only — unreachable through the normal framework dispatch path.
   * This consumer overrides {@link #onJobFailure} to handle delete/markRetry directly, so the
   * abstract base's default {@code onJobFailure} (which calls {@code permanentlyFailJob}) is
   * never invoked. The override remains in place so that if a future change re-enables the
   * framework's default failure handling, the terminal-state behaviour is still correct.
   */
  @Override
  protected void permanentlyFailJob(final ContinuousMonitoringQueueItem job, final Exception cause) {
    log.error("{}: queueId={} permanently failed; deleting.", getConsumerName(), job.getId(), cause);
    disposeOwnedJob(job, "permanent-failure");
  }

  @Override
  protected void onJobFailure(final ContinuousMonitoringQueueItem job, final Exception e) {
    if (isInterruption(e)) {
      // Per Section 7.2: interrupt is a worker-shutdown signal, not a job failure. Return the
      // job to PENDING WITHOUT incrementing retry_count (CLM-40971 C1) — bumping retry_count
      // here would let 3 rolling restarts during an in-flight job permanently delete it. The
      // abstract base class's processJob will rethrow UncheckedInterruptedException after this
      // returns so the executor can observe the interrupt.
      log.info("{}: queueId={} interrupted; returning to PENDING (retry budget preserved).",
          getConsumerName(), job.getId());
      queueDAO.unacquire(job.getId());
      return;
    }
    if (!retryPolicy.isRetryable(e)) {
      log.warn("{}: queueId={} failed with non-retryable error; deleting.",
          getConsumerName(), job.getId(), e);
      disposeOwnedJob(job, "non-retryable-failure");
      return;
    }
    int currentAttempt = job.getRetryCount() + 1;
    if (currentAttempt >= retryPolicy.maxRetries()) {
      log.error("{}: queueId={} exhausted {} retries; deleting.",
          getConsumerName(), job.getId(), retryPolicy.maxRetries(), e);
      disposeOwnedJob(job, "retry-exhausted");
      return;
    }
    log.warn("{}: queueId={} failed (attempt {}/{}), returning to PENDING.",
        getConsumerName(), job.getId(), currentAttempt, retryPolicy.maxRetries(), e);
    queueDAO.markRetry(job.getId(), formatErrorMessage(e));
  }

  /**
   * Formats an exception for the {@code error_message} column. Defends against
   * {@code e.getMessage() == null} so we don't store the literal string {@code "null"} (CLM-40971
   * M5) — happens for some {@link NullPointerException} chains and library-thrown exceptions that
   * don't supply a message.
   */
  static String formatErrorMessage(final Throwable e) {
    String type = e.getClass().getSimpleName();
    String msg = e.getMessage();
    return msg != null ? type + ": " + msg : type;
  }

  /**
   * True if {@code e} is an {@link InterruptedException} or has one in its cause or suppressed
   * chain (CLM-40971 I3). Some flow processors wrap InterruptedException in RuntimeException
   * (e.g. jOOQ-driven blocking calls) or attach it via {@code addSuppressed} during
   * try-with-resources cleanup, so the bare {@code instanceof} check would miss those and route
   * the interrupt through the retry policy as a regular failure — burning retry budget on a
   * worker-shutdown signal that should leave the job alone.
   * <p>
   * Delegates to {@link ExceptionHelper#hasCauseOrSuppressedOfType} so we get both the cause
   * walk and the suppressed-exception walk plus the shared cycle guard — same idiom used by
   * {@code ApplicationScopeEventProcessingSuspensionRule}. The caller restores the thread's
   * interrupt flag via the {@code UncheckedInterruptedException} rethrown by the abstract base.
   * </p>
   */
  static boolean isInterruption(final Throwable e) {
    return ExceptionHelper.hasCauseOrSuppressedOfType(e, InterruptedException.class);
  }

  @Override
  protected int getWorkerThreadCount() {
    return readWorkerThreads(configuration);
  }

  @Override
  protected int getMaxQueuedRows() {
    // Returns 0 per the injected-executor contract (CLM-40971 N4): this consumer is wired via
    // the injected-executor constructor, so the semaphore permit count
    // (continuousMonitoringWorkerThreads) controls back-pressure. The injected dispatch strategy
    // ignores this value; the abstract contract still requires the override.
    return 0;
  }

  @Override
  protected long getPollIntervalMs() {
    return readPollIntervalMs(configuration);
  }

  /**
   * Reads {@code continuousMonitoringPollIntervalMs} from configuration with a defense-in-depth
   * floor (CLM-40971 I5). REST validates {@code [1_000, 3_600_000]}; DB-injected values out of
   * range fall back to the default. Note this is read fresh on every reschedule (via the
   * scheduler abstract base) so a runtime ConfigurationListener flip propagates.
   */
  private static long readPollIntervalMs(final Configuration configuration) {
    Integer value = configuration.getContinuousMonitoringPollIntervalMs();
    if (value == null) {
      return DEFAULT_POLL_INTERVAL_MS;
    }
    if (value < 1_000 || value > 3_600_000) {
      log.warn("continuousMonitoringPollIntervalMs={} is outside [1_000, 3_600_000]; falling back to default {}ms",
          value, DEFAULT_POLL_INTERVAL_MS);
      return DEFAULT_POLL_INTERVAL_MS;
    }
    return value;
  }

  @Override
  protected int getMaxRetries() {
    return retryPolicy.maxRetries();
  }

  @Override
  protected String getConsumerName() {
    return NAME;
  }

  @Override
  protected String getJitterSeed() {
    // Class name + worker id ALONE leaves all MTIQ tenants on the same node sharing the same
    // jitter seed (workerId is generated once per JVM, not per tenant), so every tenant's first
    // poll lands on the same instant of the cycle (CLM-40971 N3). Including the current tenant
    // slug spreads each tenant's initial delay independently across the poll interval.
    return getClass().getName() + ":" + workerId + ":" + TenantThreadLocal.getTenant().tenantSlug;
  }

  @Override
  protected boolean isEnabled() {
    return SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled();
  }

  /**
   * Picks up live property changes (CLM-40971): when {@code hostedRepositoryEvaluation} is
   * toggled the consumer's scheduled future is cancelled or recreated. Without this, the scheduler
   * would stop firing the producer when the flag flipped off but the consumer would keep polling
   * the queue every 5 minutes — a wasted DB round-trip per tenant per poll for as long as the
   * flag stays off.
   * <p>
   * Worker-thread changes are surfaced via {@link #handleConfigurationChanged} for
   * symmetry with {@link HostedComponentScanQueueConsumer}; on the injected-executor path the
   * abstract base only logs (executor sizing is owned externally), so this is effectively a
   * future-proofing hook.
   */
  @Override
  public void configurationChanged(final Set<String> propertyNames) {
    if (!propertyNames.contains(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION)
        && !propertyNames.contains(SystemConfigurationProperty.CONTINUOUS_MONITORING_WORKER_THREADS)
        && !propertyNames.contains(SystemConfigurationProperty.CONTINUOUS_MONITORING_POLL_INTERVAL_MS))
    {
      return;
    }
    // Mirror HostedComponentScanQueueConsumer: capture the previously-applied values, compute the
    // new ones, then let handleConfigurationChanged decide whether to reschedule. Passing the real
    // old values is what lets the base class's reschedule guard (newPollInterval != oldPollInterval
    // || enabled != wasEnabled) actually fire only on real changes — a no-op write or an unrelated
    // property bundled into the same event no longer resets the initial jitter delay.
    ConfigSnapshot previous = lastApplied.get();
    ConfigSnapshot current = loadSnapshot();
    lastApplied.set(current);
    handleConfigurationChanged(
        readWorkerThreads(configuration),
        current.pollIntervalMs(),
        current.enabled(),
        previous.pollIntervalMs(),
        previous.enabled());
  }

  @Override
  protected void recoverStaleJobs() {
    // Worker-scoped reset (CLM-40039 step1 review): each consumer instance generates a fresh
    // workerId at construction, so on restart it only resets rows that THIS node's previous
    // incarnation left behind. Healthy peer nodes' in-flight rows are untouched, which is the
    // MTIQ rolling-restart safety guarantee called out in PR #16187 review thread on
    // resetInProgressToPending.
    int reset = queueDAO.resetInProgressToPending(workerId);
    if (reset > 0) {
      log.info("{}: reset {} stale IN_PROGRESS rows back to PENDING on register.",
          getConsumerName(), reset);
    }
  }
}
