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

import com.sonatype.insight.brain.dataaccess.continuousmonitoring.ContinuousMonitoringQueueItemDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;
import com.sonatype.insight.brain.queue.AbstractPollDispatchQueueConsumer;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
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

  private static int readWorkerThreads(final Configuration configuration) {
    Integer value = configuration.getContinuousMonitoringWorkerThreads();
    return value != null ? value : 4;
  }

  private int readMaxRetries() {
    Integer value = configuration.getMaxContinuousMonitoringRetries();
    return value != null ? value : 3;
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
    queueDAO.deleteById(job.getId());
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
      queueDAO.markRetry(id, "unacquired on shutdown");
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
    queueDAO.deleteById(job.getId());
  }

  @Override
  protected void onJobFailure(final ContinuousMonitoringQueueItem job, final Exception e) {
    if (e instanceof InterruptedException) {
      // Per Section 7.2: interrupt is a worker-shutdown signal, not a job failure. Return the
      // job to PENDING (without incrementing retry count). The abstract base class's processJob
      // will rethrow UncheckedInterruptedException after this returns so the executor can observe
      // the interrupt. We use markRetry's unacquire side-effect here but accept that it bumps
      // retry_count by one — acceptable trade-off vs. adding a DAO method.
      log.info("{}: queueId={} interrupted; returning to PENDING.", getConsumerName(), job.getId());
      queueDAO.markRetry(job.getId(), "interrupted");
      return;
    }
    if (!retryPolicy.isRetryable(e)) {
      log.warn("{}: queueId={} failed with non-retryable error; deleting.",
          getConsumerName(), job.getId(), e);
      queueDAO.deleteById(job.getId());
      return;
    }
    int currentAttempt = job.getRetryCount() + 1;
    if (currentAttempt >= retryPolicy.maxRetries()) {
      log.error("{}: queueId={} exhausted {} retries; deleting.",
          getConsumerName(), job.getId(), retryPolicy.maxRetries(), e);
      queueDAO.deleteById(job.getId());
      return;
    }
    log.warn("{}: queueId={} failed (attempt {}/{}), returning to PENDING.",
        getConsumerName(), job.getId(), currentAttempt, retryPolicy.maxRetries(), e);
    queueDAO.markRetry(job.getId(), e.getClass().getSimpleName() + ": " + e.getMessage());
  }

  @Override
  protected int getWorkerThreadCount() {
    return readWorkerThreads(configuration);
  }

  @Override
  protected int getMaxQueuedRows() {
    // Injected-executor mode uses semaphore permits for capacity; this value is ignored by the
    // injected dispatch strategy but the abstract contract requires it.
    return 0;
  }

  @Override
  protected long getPollIntervalMs() {
    return DEFAULT_POLL_INTERVAL_MS;
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
    // Combines class name + worker id so different replicas/tenants distribute initial poll across
    // the poll interval rather than thundering on tick zero.
    return getClass().getName() + ":" + workerId;
  }

  @Override
  protected boolean isEnabled() {
    return SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled();
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
