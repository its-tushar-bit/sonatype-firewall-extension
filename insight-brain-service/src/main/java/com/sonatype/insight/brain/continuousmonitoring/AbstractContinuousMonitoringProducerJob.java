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

import com.sonatype.insight.brain.dataaccess.continuousmonitoring.EligibilityCursor;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.Page;
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
 * {@link EligibilitySelector} and a flow-specific
 * {@link #enqueueBatch} that inserts the parent queue row + per-flow satellite row in one
 * transaction with dedup.
 * <p>
 * The job is annotated {@link DisallowConcurrentExecution} so a single node never overlaps two
 * cycles; clustered Quartz prevents cross-node overlap via {@code QRTZ_FIRED_TRIGGERS}. The
 * {@link MtiqBatchJob} marker routes execution to the MTIQ batch instance only.
 * <p>
 * Per design Decision G the cycle pages through eligibility with a configurable batch size via
 * keyset cursor (CLM-41005); per Section 7.4 a failure mid-cycle aborts the cycle (does not advance
 * any checkpoint) and the next Quartz fire re-scans from the top. There is no checkpoint-after-page
 * semantics because the natural-key UNIQUE constraint silently dedups already-enqueued rows on the
 * next fire.
 * <p>
 * The cycle runs until the selector reports {@code page.hasMore() == false}. Keyset pagination
 * on {@code (time DESC, proxy_repository_component_id DESC)} makes per-page cost O(limit) regardless of
 * position and prevents row-skip under concurrent inserts, so the previous 1M-row
 * {@code maxCyclePages} ceiling is no longer needed for correctness. A high
 * {@value #DEFAULT_SAFETY_NET_MAX_CYCLE_PAGES}-page safety net remains as a backstop against a
 * buggy selector that perpetually returns {@code hasMore == true} — at that point the cycle
 * aborts and logs a WARN so the bug is operator-visible rather than running forever silently.
 *
 * @param <T> per-flow eligibility candidate type
 */
@DisallowConcurrentExecution
public abstract class AbstractContinuousMonitoringProducerJob<T>
    extends AdminTask
    implements InsightJob, MtiqBatchJob
{
  private static final Logger log = LoggerFactory.getLogger(AbstractContinuousMonitoringProducerJob.class);

  /**
   * Backstop only — not a tenant-size ceiling. At the default {@code pageSize=1000} this is 100M
   * rows per cycle, well above any realistic tenant. Tripping it indicates a selector or DAO bug
   * (e.g. {@code Page.hasMore()} stuck at {@code true}); the cycle aborts and logs a WARN so the
   * bug surfaces in alerts rather than the producer spinning forever. Overridable in tests and
   * production subclasses.
   */
  protected static final int DEFAULT_SAFETY_NET_MAX_CYCLE_PAGES = 100_000;

  protected AbstractContinuousMonitoringProducerJob(final String adminTaskName) {
    super(adminTaskName);
  }

  /** Selector returning eligible candidates for this flow. */
  protected abstract EligibilitySelector<T> getEligibilitySelector();

  /** Number of candidates fetched per page. Operator-tunable; design default 1000. */
  protected abstract int getEligibilityPageSize();

  /** Whether the producer should run; gated on the flow's feature flag. */
  protected abstract boolean isEnabled();

  /** Name of the JVM system property that overrides the safety-net page cap at runtime. */
  static final String SAFETY_NET_MAX_CYCLE_PAGES_PROPERTY = "cm.producer.safetyNetMaxCyclePages";

  /**
   * Backstop page cap. Overridden in tests to exercise the safety-net path without 100K iterations;
   * production subclasses inherit {@link #DEFAULT_SAFETY_NET_MAX_CYCLE_PAGES}.
   * <p>
   * Configurable via system property {@value #SAFETY_NET_MAX_CYCLE_PAGES_PROPERTY} for ops tuning
   * without a rebuild. Unset, non-positive, or unparseable values fall back to
   * {@link #DEFAULT_SAFETY_NET_MAX_CYCLE_PAGES}; the bad-value cases log WARN once per call so
   * operators are not misled into believing their override is in effect (see
   * {@link #parseSafetyNetMaxCyclePages} for the parser).
   */
  protected int getSafetyNetMaxCyclePages() {
    return parseSafetyNetMaxCyclePages(System.getProperty(SAFETY_NET_MAX_CYCLE_PAGES_PROPERTY));
  }

  /**
   * Parses the safety-net override. Separated from {@link #getSafetyNetMaxCyclePages} so unit
   * tests can drive each branch (null / non-positive / unparseable / valid) without setting
   * JVM-wide system properties.
   * <p>
   * Returns {@link #DEFAULT_SAFETY_NET_MAX_CYCLE_PAGES} when {@code rawValue} is null, blank,
   * non-positive, or unparseable. Bad values (non-positive or unparseable) emit a WARN — an
   * operator who set the property explicitly should learn that their override was ignored.
   */
  static int parseSafetyNetMaxCyclePages(final String rawValue) {
    if (rawValue == null) {
      return DEFAULT_SAFETY_NET_MAX_CYCLE_PAGES;
    }
    int parsed;
    try {
      parsed = Integer.parseInt(rawValue.trim());
    }
    catch (NumberFormatException e) {
      log.warn("System property {}={} is not a valid integer; using default {}.",
          SAFETY_NET_MAX_CYCLE_PAGES_PROPERTY, rawValue, DEFAULT_SAFETY_NET_MAX_CYCLE_PAGES);
      return DEFAULT_SAFETY_NET_MAX_CYCLE_PAGES;
    }
    if (parsed <= 0) {
      log.warn("System property {}={} is not positive; using default {}.",
          SAFETY_NET_MAX_CYCLE_PAGES_PROPERTY, rawValue, DEFAULT_SAFETY_NET_MAX_CYCLE_PAGES);
      return DEFAULT_SAFETY_NET_MAX_CYCLE_PAGES;
    }
    return parsed;
  }

  /**
   * Inserts a batch of parent queue rows + per-flow satellite rows in a single transaction with
   * dedup-on-natural-key (subclass owns the DAO call). The framework guarantees strict FIFO
   * ordering at the consumer (acquire orders rows by {@code create_time ASC}), so the producer
   * does not assign per-row priorities — newer pages always land behind older pages, and within
   * a page they tie on {@code create_time} which the database breaks deterministically.
   *
   * @param candidates the page being enqueued (size up to {@link #getEligibilityPageSize()})
   * @param cycleStart cycle anchor — implementations may persist this on the parent row
   * @return number of parent rows actually inserted (post-dedup)
   */
  protected abstract int enqueueBatch(List<T> candidates, Instant cycleStart);

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
    // runCycle's own try blocks: isEnabled, getEligibilitySelector,
    // getEligibilityPageSize) is logged at ERROR before Quartz sees it.
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
   * via {@link #enqueueBatch}. Advances by {@link EligibilityCursor} (CLM-41005) — the selector's
   * {@code Page.hasMore()} signals end-of-stream. Returns a {@link CycleResult} indicating
   * success or abort reason.
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

    int totalEnqueued = 0;
    long totalConsidered = 0;
    int pageCount = 0;
    int safetyNetMaxPages = getSafetyNetMaxCyclePages();
    EligibilityCursor cursor = null;
    while (true) {
      if (pageCount >= safetyNetMaxPages) {
        // Backstop tripped — selector/DAO bug, not a normal big-tenant outcome. WARN (not ERROR)
        // because the cycle's enqueues remain valid; abort the cycle, let the next Quartz fire
        // try again, and surface the bug to ops via the alert pipeline.
        log.warn(
            "Continuous monitoring producer ({}): safety-net cap of {} pages tripped at cursor {} "
                + "(considered={}, enqueued={}); aborting cycle. This indicates a bug in the selector "
                + "or DAO (Page.hasMore() likely stuck true).",
            getFlowLogTag(), safetyNetMaxPages, cursorLogTag(cursor), totalConsidered, totalEnqueued);
        return CycleResult.aborted(totalEnqueued, "safety-net page cap " + safetyNetMaxPages + " tripped");
      }
      Page<T> page;
      try {
        page = selector.fetchPage(cursor, pageSize, cycleStart);
      }
      catch (RuntimeException e) {
        // ERROR (not WARN) so Quartz alerting picks it up — selector failures are unexpected and
        // op-actionable (DB connectivity, schema drift), distinct from the WARN-level expected
        // aborts above (non-positive pageSize). CLM-40971.
        log.error(
            "Continuous monitoring producer ({}): eligibility page fetch failed at cursor {} ({}); aborting cycle.",
            getFlowLogTag(), cursorLogTag(cursor), e.getClass().getSimpleName(), e);
        return CycleResult.aborted(totalEnqueued, "selector failure at cursor " + cursorLogTag(cursor));
      }
      if (page == null || page.rows().isEmpty()) {
        break;
      }
      List<T> rows = page.rows();
      int inserted;
      try {
        inserted = enqueueBatch(rows, cycleStart);
      }
      catch (RuntimeException e) {
        // ERROR for the same reason as the selector catch above (CLM-40971): enqueue failures
        // are unexpected DB-write errors, not expected aborts.
        log.error("Continuous monitoring producer ({}): enqueueBatch failed at cursor {} ({}); aborting cycle.",
            getFlowLogTag(), cursorLogTag(cursor), e.getClass().getSimpleName(), e);
        return CycleResult.aborted(totalEnqueued, "enqueue failure at cursor " + cursorLogTag(cursor));
      }
      totalEnqueued += inserted;
      totalConsidered += rows.size();
      pageCount++;
      if (!page.hasMore()) {
        break;
      }
      cursor = page.nextCursor();
    }
    log.info("Continuous monitoring producer cycle complete ({}): considered={}, enqueued={}.",
        getFlowLogTag(), totalConsidered, totalEnqueued);
    return CycleResult.success(totalEnqueued);
  }

  private static String cursorLogTag(final EligibilityCursor cursor) {
    return cursor == null ? "<start>" : cursor.encode();
  }
}
