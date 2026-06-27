/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.io.PrintWriter;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.service.AdminTask;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.MtiqBatchJob;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quartz-driven producer that runs one continuous monitoring cycle per fire (CLM-40039,
 * Section 6.1). Knows nothing about specific flows: subclasses plug in an
 * {@link EligibilitySelector}, an {@link OrderingStrategy}, and a flow-specific
 * {@link #enqueueBatch} that inserts the parent queue row + per-flow satellite row in one
 * transaction with dedup.
 * <p>
 * The job is annotated {@link DisallowConcurrentExecution} so a single node never overlaps two
 * cycles; clustered Quartz prevents cross-node overlap via {@code QRTZ_FIRED_TRIGGERS}. The
 * {@link MtiqBatchJob} marker routes execution to the MTIQ batch instance only.
 * <p>
 * Per design Decision G the cycle pages through eligibility with a configurable batch size; per
 * Section 7.4 a failure mid-cycle aborts the cycle (does not advance any checkpoint) and the
 * next Quartz fire re-scans from the top. There is no checkpoint-after-page semantics because
 * the natural-key UNIQUE constraint silently dedups already-enqueued rows on the next fire.
 * <p>
 * The cycle is bounded by {@link #getMaxCyclePages()} to prevent unbounded enumeration on large
 * eligibility sets. When the page limit is reached, the cycle aborts with a warning and the next
 * Quartz fire continues from the top (same dedup semantics as abort-on-failure).
 *
 * @param <T> per-flow eligibility candidate type
 */
@DisallowConcurrentExecution
public abstract class AbstractContinuousMonitoringProducerJob<T>
    extends AdminTask
    implements InsightJob, MtiqBatchJob
{
  private static final Logger log = LoggerFactory.getLogger(AbstractContinuousMonitoringProducerJob.class);

  /** Default maximum pages per cycle to prevent unbounded enumeration. */
  protected static final int DEFAULT_MAX_CYCLE_PAGES = 1000;

  protected AbstractContinuousMonitoringProducerJob(final String adminTaskName) {
    super(adminTaskName);
  }

  /** Selector returning eligible candidates for this flow. */
  protected abstract EligibilitySelector<T> getEligibilitySelector();

  /** Ordering strategy for assigning {@code priority} to candidates as they are emitted. */
  protected abstract OrderingStrategy getOrderingStrategy();

  /** Number of candidates fetched per page. Operator-tunable; design default 1000. */
  protected abstract int getEligibilityPageSize();

  /**
   * Maximum number of pages to process in a single cycle before aborting.
   * Prevents unbounded enumeration on very large eligibility sets that would block a single
   * Quartz fire for longer than the schedule interval. Default {@value #DEFAULT_MAX_CYCLE_PAGES}.
   * Subclasses may override to make this configurable.
   */
  protected int getMaxCyclePages() {
    return DEFAULT_MAX_CYCLE_PAGES;
  }

  /** Whether the producer should run; gated on the flow's feature flag. */
  protected abstract boolean isEnabled();

  /**
   * Inserts a batch of parent queue rows + per-flow satellite rows in a single transaction with
   * dedup-on-natural-key (subclass owns the DAO call).
   * <p>
   * <strong>Invariant:</strong> {@code priorities.size() == candidates.size()} and aligned 1:1 —
   * {@code priorities.get(i)} is the priority for {@code candidates.get(i)}. The framework
   * computes priorities from {@code page.size()} via {@link OrderingStrategy} so the alignment
   * always holds; subclasses iterate the two lists in lock-step or zip them into satellite rows
   * and rely on this invariant.
   *
   * @param candidates the page being enqueued (size up to {@link #getEligibilityPageSize()})
   * @param priorities priority assignments aligned 1:1 with {@code candidates} (same size)
   * @param cycleStart cycle anchor — implementations may persist this on the parent row
   * @return number of parent rows actually inserted (post-dedup)
   */
  protected abstract int enqueueBatch(List<T> candidates, List<Long> priorities, Instant cycleStart);

  /**
   * Subclass-supplied descriptive name for log lines and the
   * {@code continuous_monitoring.queue.cycle_duration_ms} metric tag (e.g. {@code "hosted_repo"}).
   * Note: metric recording is deferred to a follow-up; this tag is currently used only for logging.
   */
  protected abstract String getFlowLogTag();

  /**
   * Result of a producer cycle run. Tracks whether the cycle completed successfully or was
   * aborted (due to failure or hitting the page limit), along with the count of rows enqueued
   * and the reason for any abort.
   */
  public static final class CycleResult
  {
    private final boolean success;

    private final int enqueued;

    private final String abortReason;

    private CycleResult(final boolean success, final int enqueued, final String abortReason) {
      this.success = success;
      this.enqueued = enqueued;
      this.abortReason = abortReason;
    }

    /** Returns true if the cycle completed without aborting. */
    public boolean isSuccess() {
      return success;
    }

    /** Returns the number of rows successfully enqueued before completion or abort. */
    public int getEnqueued() {
      return enqueued;
    }

    /** Returns the reason for abort, or null if the cycle completed successfully. */
    public String getAbortReason() {
      return abortReason;
    }

    /** Creates a successful result. */
    public static CycleResult success(final int enqueued) {
      return new CycleResult(true, enqueued, null);
    }

    /** Creates an aborted result. */
    public static CycleResult aborted(final int enqueued, final String reason) {
      return new CycleResult(false, enqueued, reason);
    }
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) {
    log.info("Manual request to run continuous monitoring producer cycle ({}).", getFlowLogTag());
    // Symmetric with the Quartz path below: wrap runCycle() in InsightJob.execute(Runnable,
    // Logger, String) so an admin-port trigger gets the same MDC system-user scope and the same
    // ERROR-level catch on uncaught Throwables from abstract-method calls. The CycleResult is
    // captured in a single-element holder so the output.write call after the wrapper can read
    // it (an admin operator triggering /tasks/... still gets the Completed/Aborted line).
    //
    // Concurrency: @DisallowConcurrentExecution only gates the Quartz path (line 178); two
    // simultaneous admin triggers, or an admin trigger overlapping a Quartz fire, will run
    // runCycle concurrently. This is intentional: the DB natural-key UNIQUE on the satellite
    // table + ignoreDuplicateKey in enqueueBatch make duplicate inserts idempotent. The cost of
    // overlap is doubled eligibility scans and duplicate insert attempts (silently deduped), not
    // data corruption. Admin triggers are operator-driven and rare; a synchronized guard would
    // serialise legitimate "trigger now after fixing config" use-cases for no real safety win.
    CycleResult[] resultHolder = new CycleResult[1];
    execute(() -> resultHolder[0] = runCycle(),
        log, "Continuous monitoring producer cycle error (" + getFlowLogTag() + ")");
    CycleResult result = resultHolder[0];
    if (result == null) {
      // runCycle threw and InsightJob.execute logged at ERROR; tell the operator the run failed.
      output.write("Failed manual continuous monitoring producer cycle (" + getFlowLogTag()
          + "); see server log for details.\n");
      return;
    }
    if (result.isSuccess()) {
      output.write("Completed manual continuous monitoring producer cycle (" + getFlowLogTag() + ", "
          + result.getEnqueued() + " enqueued).\n");
    }
    else {
      output.write("Aborted manual continuous monitoring producer cycle (" + getFlowLogTag() + ", "
          + result.getEnqueued() + " enqueued, reason: " + result.getAbortReason() + ").\n");
    }
  }

  @Override
  public void execute(final JobExecutionContext context) {
    log.info("Continuous monitoring producer fire ({}) for tenant {}.",
        getFlowLogTag(), TenantThreadLocal.getTenant());
    // Use InsightJob.execute(Runnable, Logger, String) so:
    // 1. MDCUsernameScope.forSystem() is on the stack while runCycle runs (consistent with all
    // other Quartz-fired jobs in this codebase — log lines carry the system username MDC).
    // 2. Any Throwable that escapes runCycle (including from the abstract-method calls outside
    // runCycle's own try blocks: isEnabled, getEligibilitySelector, getOrderingStrategy,
    // getMaxCyclePages, getEligibilityPageSize) is logged at ERROR before Quartz sees it.
    execute(() -> {
      CycleResult result = runCycle();
      if (result.isSuccess()) {
        log.info("Next continuous monitoring producer fire ({}) scheduled for {}.",
            getFlowLogTag(), context.getNextFireTime());
      }
      else {
        log.warn("Continuous monitoring producer cycle ({}) aborted: {}. Next fire scheduled for {}.",
            getFlowLogTag(), result.getAbortReason(), context.getNextFireTime());
      }
    }, log, "Continuous monitoring producer cycle error (" + getFlowLogTag() + ")");
  }

  /**
   * Runs one cycle: pages through {@link EligibilitySelector#fetchPage} and enqueues each page
   * via {@link #enqueueBatch}. Returns a {@link CycleResult} indicating success or abort reason.
   */
  protected CycleResult runCycle() {
    if (!isEnabled()) {
      log.debug("Continuous monitoring producer ({}) is disabled; skipping cycle.", getFlowLogTag());
      return CycleResult.success(0);
    }
    Instant cycleStart = Instant.now();
    int pageSize = getEligibilityPageSize();
    if (pageSize <= 0) {
      // Misconfiguration (operator set the page-size knob to 0 or negative). Distinct from
      // success(0) "no eligible work" — return aborted so an admin-task trigger surfaces the
      // misconfig in the output line ("Aborted ... reason: non-positive pageSize") rather than
      // pretending nothing was wrong. The disabled-flag branch above keeps success(0) because
      // disabled is an expected steady-state for some flows.
      log.warn("Continuous monitoring producer ({}): non-positive pageSize {}, aborting cycle.",
          getFlowLogTag(), pageSize);
      return CycleResult.aborted(0, "non-positive pageSize " + pageSize);
    }
    EligibilitySelector<T> selector = getEligibilitySelector();
    OrderingStrategy ordering = getOrderingStrategy();
    int maxPages = getMaxCyclePages();

    int totalEnqueued = 0;
    long totalConsidered = 0;
    int offset = 0;
    int pageCount = 0;
    while (true) {
      if (pageCount >= maxPages) {
        log.warn("Continuous monitoring producer ({}): reached maxCyclePages limit ({}), aborting cycle. "
            + "Considered={}, enqueued={}. Remaining eligible rows will be picked up on next fire.",
            getFlowLogTag(), maxPages, totalConsidered, totalEnqueued);
        return CycleResult.aborted(totalEnqueued,
            "maxCyclePages limit reached (" + maxPages + ")");
      }
      List<T> page;
      try {
        page = selector.fetchPage(offset, pageSize, cycleStart);
      }
      catch (RuntimeException e) {
        // ERROR (not WARN) so Quartz alerting picks it up — selector failures are unexpected and
        // op-actionable (DB connectivity, schema drift), distinct from the WARN-level expected
        // aborts above (non-positive pageSize, maxCyclePages limit reached). CLM-40971.
        log.error(
            "Continuous monitoring producer ({}): eligibility page fetch failed at offset {} ({}); aborting cycle.",
            getFlowLogTag(), offset, e.getClass().getSimpleName(), e);
        return CycleResult.aborted(totalEnqueued, "selector failure at offset " + offset);
      }
      if (page == null || page.isEmpty()) {
        break;
      }
      pageCount++;
      List<Long> priorities = computePriorities(ordering, totalConsidered, page.size());
      int inserted;
      try {
        inserted = enqueueBatch(page, priorities, cycleStart);
      }
      catch (RuntimeException e) {
        // ERROR for the same reason as the selector catch above (CLM-40971): enqueue failures
        // are unexpected DB-write errors, not expected aborts.
        log.error("Continuous monitoring producer ({}): enqueueBatch failed at offset {} ({}); aborting cycle.",
            getFlowLogTag(), offset, e.getClass().getSimpleName(), e);
        return CycleResult.aborted(totalEnqueued, "enqueue failure at offset " + offset);
      }
      totalEnqueued += inserted;
      totalConsidered += page.size();
      if (page.size() < pageSize) {
        break;
      }
      offset += page.size();
    }
    log.info("Continuous monitoring producer cycle complete ({}): considered={}, enqueued={}.",
        getFlowLogTag(), totalConsidered, totalEnqueued);
    return CycleResult.success(totalEnqueued);
  }

  private static List<Long> computePriorities(
      final OrderingStrategy ordering,
      final long positionInCycleStart,
      final int pageSize)
  {
    Long[] result = new Long[pageSize];
    for (int i = 0; i < pageSize; i++) {
      result[i] = ordering.priorityFor(positionInCycleStart + i);
    }
    return List.of(result);
  }
}
