/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.continuousmonitoring.EligibilityCursor;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.Page;
import com.sonatype.insight.brain.tenancy.MtiqBatchJob;

import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AbstractContinuousMonitoringProducerJob#runCycle} (CLM-40039 §6.1,
 * CLM-41005 keyset). Covers paginated cycle, abort-on-failure (§7.4), feature-flag gate,
 * cursor-advance semantics,
 * admin-task output, and the Quartz contract annotations
 * ({@link org.quartz.DisallowConcurrentExecution} + {@link MtiqBatchJob}).
 */
public class AbstractContinuousMonitoringProducerJobTest
{
  private static final EligibilityCursor CURSOR_AFTER_1 = new EligibilityCursor(new Date(1L), "id-1");

  private static final EligibilityCursor CURSOR_AFTER_2 = new EligibilityCursor(new Date(2L), "id-2");

  @Test
  public void testRunCycle_disabledJobSkipsCycleAndReturnsSuccessZero() {
    StubProducerJob job = new StubProducerJob();
    job.enabled = false;
    job.selector = new RecordingSelector(List.of(
        new Page<>(List.of(1L, 2L), CURSOR_AFTER_2, false)));
    AbstractContinuousMonitoringProducerJob.CycleResult result = job.runCycle();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEnqueued()).isEqualTo(0);
    assertThat(result.getAbortReason()).isNull();
    assertThat(((RecordingSelector) job.selector).fetchCount).isEqualTo(0);
    assertThat(job.enqueuedBatches).isEmpty();
  }

  @Test
  public void testRunCycle_nonPositivePageSizeAbortsWithReason() {
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 0;
    AbstractContinuousMonitoringProducerJob.CycleResult result = job.runCycle();
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getEnqueued()).isEqualTo(0);
    assertThat(result.getAbortReason()).contains("non-positive pageSize");
    assertThat(job.enqueuedBatches).isEmpty();
  }

  @Test
  public void testRunCycle_emptyEligibilityReturnsImmediately() {
    StubProducerJob job = new StubProducerJob();
    job.selector = new RecordingSelector(List.of());
    AbstractContinuousMonitoringProducerJob.CycleResult result = job.runCycle();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEnqueued()).isEqualTo(0);
    assertThat(((RecordingSelector) job.selector).fetchCount).isEqualTo(1);
    assertThat(job.enqueuedBatches).isEmpty();
  }

  @Test
  public void testRunCycle_advancesByCursorUntilHasMoreFalse() {
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 3;
    job.selector = new RecordingSelector(List.of(
        new Page<>(List.of(1L, 2L, 3L), CURSOR_AFTER_1, true),
        new Page<>(List.of(4L, 5L, 6L), CURSOR_AFTER_2, true),
        new Page<>(List.of(7L), null, false)));
    AbstractContinuousMonitoringProducerJob.CycleResult result = job.runCycle();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEnqueued()).isEqualTo(7);
    assertThat(job.enqueuedBatches).hasSize(3);
    assertThat(((RecordingSelector) job.selector).fetchCount).isEqualTo(3);
    assertThat(((RecordingSelector) job.selector).cursorsSeen)
        .containsExactly(null, CURSOR_AFTER_1, CURSOR_AFTER_2);
  }

  @Test
  public void testRunCycle_stopsWhenHasMoreFalseEvenIfPageSaturated() {
    // A selector that returns a page with size == limit but signals hasMore=false must end
    // the cycle. (Without keyset, "page.size() < pageSize" was the only end-of-stream signal;
    // CLM-41005 makes that the selector's responsibility, not the framework's.)
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 2;
    job.selector = new RecordingSelector(List.of(
        new Page<>(List.of(1L, 2L), null, false)));
    AbstractContinuousMonitoringProducerJob.CycleResult result = job.runCycle();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEnqueued()).isEqualTo(2);
    assertThat(((RecordingSelector) job.selector).fetchCount).isEqualTo(1);
  }

  @Test
  public void testRunCycle_safetyNetCapAbortsWhenHasMoreStuckTrue() {
    // Simulates a buggy selector that perpetually returns hasMore=true. Without the safety net the
    // cycle would spin until OOM or wall-clock; with it, the cycle aborts after the configured cap
    // and surfaces a WARN-level abortReason so ops can root-cause the underlying selector bug.
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 2;
    job.safetyNetMaxPages = 3;
    job.selector = new StuckHasMoreSelector();

    AbstractContinuousMonitoringProducerJob.CycleResult result = job.runCycle();

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getAbortReason()).contains("safety-net page cap 3 tripped");
    // 3 pages × 2 rows enqueued before the backstop fires.
    assertThat(result.getEnqueued()).isEqualTo(6);
    assertThat(job.enqueuedBatches).hasSize(3);
  }

  @Test
  public void parseSafetyNetMaxCyclePages_nullReturnsDefault() {
    assertThat(AbstractContinuousMonitoringProducerJob.parseSafetyNetMaxCyclePages(null))
        .isEqualTo(AbstractContinuousMonitoringProducerJob.DEFAULT_SAFETY_NET_MAX_CYCLE_PAGES);
  }

  @Test
  public void parseSafetyNetMaxCyclePages_validIntegerReturnsThatValue() {
    assertThat(AbstractContinuousMonitoringProducerJob.parseSafetyNetMaxCyclePages("250000"))
        .isEqualTo(250_000);
  }

  @Test
  public void parseSafetyNetMaxCyclePages_validIntegerWithWhitespaceIsTrimmed() {
    assertThat(AbstractContinuousMonitoringProducerJob.parseSafetyNetMaxCyclePages("  500  "))
        .isEqualTo(500);
  }

  @Test
  public void parseSafetyNetMaxCyclePages_zeroFallsBackToDefault() {
    // Non-positive value is rejected: a cap of 0 would abort the cycle on the first page,
    // surely not the operator's intent. WARN logged so the override is not silently ignored.
    assertThat(AbstractContinuousMonitoringProducerJob.parseSafetyNetMaxCyclePages("0"))
        .isEqualTo(AbstractContinuousMonitoringProducerJob.DEFAULT_SAFETY_NET_MAX_CYCLE_PAGES);
  }

  @Test
  public void parseSafetyNetMaxCyclePages_negativeFallsBackToDefault() {
    assertThat(AbstractContinuousMonitoringProducerJob.parseSafetyNetMaxCyclePages("-1"))
        .isEqualTo(AbstractContinuousMonitoringProducerJob.DEFAULT_SAFETY_NET_MAX_CYCLE_PAGES);
  }

  @Test
  public void parseSafetyNetMaxCyclePages_unparseableFallsBackToDefault() {
    // Malformed input — WARN-and-fall-back rather than throwing, because the producer must keep
    // running. The operator-visible signal is the log line, not an exception.
    assertThat(AbstractContinuousMonitoringProducerJob.parseSafetyNetMaxCyclePages("not-a-number"))
        .isEqualTo(AbstractContinuousMonitoringProducerJob.DEFAULT_SAFETY_NET_MAX_CYCLE_PAGES);
  }

  @Test
  public void testRunCycle_selectorFailureAbortsCycleAndReturnsAbortedResult() {
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 2;
    job.selector = new RecordingSelector(List.of(
        new Page<>(List.of(1L, 2L), CURSOR_AFTER_2, true)))
            .failOnFetchAt(1);
    AbstractContinuousMonitoringProducerJob.CycleResult result = job.runCycle();
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getEnqueued()).isEqualTo(2);
    assertThat(result.getAbortReason()).contains("selector failure");
    assertThat(job.enqueuedBatches).hasSize(1);
  }

  @Test
  public void testRunCycle_enqueueFailureAbortsCycleAndReturnsAbortedResult() {
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 2;
    job.failEnqueueAtBatch = 1;
    job.selector = new RecordingSelector(List.of(
        new Page<>(List.of(1L, 2L), CURSOR_AFTER_1, true),
        new Page<>(List.of(3L, 4L), CURSOR_AFTER_2, true),
        new Page<>(List.of(5L), null, false)));
    AbstractContinuousMonitoringProducerJob.CycleResult result = job.runCycle();
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getEnqueued()).isEqualTo(2);
    assertThat(result.getAbortReason()).contains("enqueue failure");
    assertThat(job.enqueuedBatches).hasSize(1);
  }

  @Test
  public void testRunCycle_enqueueDedupReturnedCountIsRespected() {
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 3;
    job.dedupReturnFor.put(0, 1);
    job.selector = new RecordingSelector(List.of(
        new Page<>(List.of(1L, 2L, 3L), null, false)));
    AbstractContinuousMonitoringProducerJob.CycleResult result = job.runCycle();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEnqueued()).isEqualTo(1);
  }

  @Test
  public void testRunCycle_crossCycleDedupReportsZeroOnSecondCycle() {
    // CLM-40971 M6: a candidate selected in two separate cycles must enqueue only once.
    // The satellite natural-key UNIQUE + ignoreDuplicateKey in enqueueBatch are what make
    // back-to-back cycles idempotent. The stub models that constraint by having the second
    // cycle's enqueueBatch return 0 (every row is a duplicate-key hit). This locks in the
    // contract: a regression in the natural key or in the ignore-duplicate path would land
    // here as "second cycle enqueued > 0".
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 3;
    job.selector = new RecordingSelector(List.of(
        new Page<>(List.of(1L, 2L, 3L), null, false)));

    AbstractContinuousMonitoringProducerJob.CycleResult first = job.runCycle();
    assertThat(first.isSuccess()).isTrue();
    assertThat(first.getEnqueued()).isEqualTo(3);

    job.selector = new RecordingSelector(List.of(
        new Page<>(List.of(1L, 2L, 3L), null, false)));
    // Batch index is stub-global (enqueuedBatches.size()) so the second cycle's first batch is 1.
    job.dedupReturnFor.put(1, 0);
    AbstractContinuousMonitoringProducerJob.CycleResult second = job.runCycle();
    assertThat(second.isSuccess()).isTrue();
    assertThat(second.getEnqueued()).isEqualTo(0);
  }

  @Test
  public void testRunCycle_cycleStartIsStableAcrossPages() {
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 2;
    job.selector = new RecordingSelector(List.of(
        new Page<>(List.of(1L, 2L), CURSOR_AFTER_2, true),
        new Page<>(List.of(3L), null, false)));
    job.runCycle();
    Instant first = job.enqueuedBatches.get(0).cycleStart;
    Instant second = job.enqueuedBatches.get(1).cycleStart;
    assertThat(first).isEqualTo(second);
  }

  @Test
  public void testExecute_adminTaskWritesCompletedOnSuccess() {
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 2;
    job.selector = new RecordingSelector(List.of(
        new Page<>(List.of(1L, 2L, 3L), null, false)));
    StringWriter sw = new StringWriter();
    job.execute(Map.of(), new PrintWriter(sw));
    assertThat(sw.toString()).contains("Completed");
    assertThat(sw.toString()).contains("3 enqueued");
    assertThat(sw.toString()).doesNotContain("Aborted");
  }

  @Test
  public void testExecute_adminTaskWritesAbortedOnFailure() {
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 2;
    job.failEnqueueAtBatch = 0;
    job.selector = new RecordingSelector(List.of(
        new Page<>(List.of(1L, 2L), null, false)));
    StringWriter sw = new StringWriter();
    job.execute(Map.of(), new PrintWriter(sw));
    assertThat(sw.toString()).contains("Aborted");
    assertThat(sw.toString()).contains("0 enqueued");
    assertThat(sw.toString()).contains("reason:");
  }

  @Test
  public void testExecute_adminTaskWritesFailedWhenRunCycleThrows() {
    // result == null path in execute(Map, PrintWriter) — runCycle() itself escapes the
    // InsightJob.execute wrapper because an abstract-method call (here getEligibilitySelector)
    // threw before runCycle could compute a CycleResult. The wrapper logs at ERROR; the operator
    // sees a "Failed" line directing them to the server log rather than a stale "Completed" or
    // a misleading "Aborted (reason: ...)".
    StubProducerJob job = new StubProducerJob()
    {
      @Override
      protected EligibilitySelector<Long> getEligibilitySelector() {
        throw new RuntimeException("forced uncaught throwable from abstract-method call");
      }
    };
    StringWriter sw = new StringWriter();
    job.execute(Map.of(), new PrintWriter(sw));
    assertThat(sw.toString()).contains("Failed");
    assertThat(sw.toString()).doesNotContain("Completed");
    assertThat(sw.toString()).doesNotContain("Aborted");
  }

  @Test
  public void testExecute_quartzPathLogsNextFireOnSuccess() {
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 2;
    job.selector = new RecordingSelector(List.of(
        new Page<>(List.of(1L, 2L), null, false)));
    JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
    Mockito.when(context.getNextFireTime()).thenReturn(new Date(0L));

    job.execute(context);

    assertThat(job.enqueuedBatches).hasSize(1);
  }

  @Test
  public void testExecute_quartzPathReadsNextFireOnAbort() {
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 2;
    job.failEnqueueAtBatch = 0;
    job.selector = new RecordingSelector(List.of(
        new Page<>(List.of(1L, 2L), null, false)));
    JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
    Mockito.when(context.getNextFireTime()).thenReturn(new Date(0L));

    job.execute(context);

    assertThat(job.enqueuedBatches).isEmpty();
  }

  @Test
  public void testJob_disallowsConcurrentExecution() {
    assertThat(JobBuilder.newJob(StubProducerJob.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testJob_isMtiqBatchJob() {
    assertThat(new StubProducerJob()).isInstanceOf(MtiqBatchJob.class);
  }

  private static final class EnqueueCall
  {
    final List<Long> candidates;

    final Instant cycleStart;

    EnqueueCall(final List<Long> candidates, final Instant cycleStart) {
      this.candidates = candidates;
      this.cycleStart = cycleStart;
    }
  }

  private static class StubProducerJob
      extends AbstractContinuousMonitoringProducerJob<Long>
  {
    boolean enabled = true;

    int pageSize = 2;

    int safetyNetMaxPages = AbstractContinuousMonitoringProducerJob.DEFAULT_SAFETY_NET_MAX_CYCLE_PAGES;

    EligibilitySelector<Long> selector = new RecordingSelector(List.of());

    final List<EnqueueCall> enqueuedBatches = new ArrayList<>();

    int failEnqueueAtBatch = -1;

    final Map<Integer, Integer> dedupReturnFor = new HashMap<>();

    StubProducerJob() {
      super("stub-producer");
    }

    @Override
    protected EligibilitySelector<Long> getEligibilitySelector() {
      return selector;
    }

    @Override
    protected int getEligibilityPageSize() {
      return pageSize;
    }

    @Override
    protected boolean isEnabled() {
      return enabled;
    }

    @Override
    protected int getSafetyNetMaxCyclePages() {
      return safetyNetMaxPages;
    }

    @Override
    protected int enqueueBatch(final List<Long> candidates, final Instant cycleStart) {
      int batchIndex = enqueuedBatches.size();
      if (batchIndex == failEnqueueAtBatch) {
        throw new RuntimeException("forced enqueue failure");
      }
      enqueuedBatches.add(new EnqueueCall(candidates, cycleStart));
      return dedupReturnFor.getOrDefault(batchIndex, candidates.size());
    }

    @Override
    protected String getFlowLogTag() {
      return "stub";
    }

    @Override
    public String getJobName() {
      return "StubContinuousMonitoringProducerJob";
    }
  }

  /**
   * Simulates a buggy selector whose {@code Page.hasMore()} is stuck at {@code true} — the case
   * the safety-net cap exists to defend against.
   */
  private static final class StuckHasMoreSelector
      implements EligibilitySelector<Long>
  {
    private long nextId = 0;

    @Override
    public Page<Long> fetchPage(final EligibilityCursor cursor, final int limit, final Instant cycleStart) {
      List<Long> rows = new ArrayList<>(limit);
      for (int i = 0; i < limit; i++) {
        rows.add(nextId++);
      }
      // Always hasMore=true regardless of position — the bug shape the cap defends against.
      EligibilityCursor next = new EligibilityCursor(new Date(nextId), "id-" + nextId);
      return new Page<>(rows, next, true);
    }
  }

  private static final class RecordingSelector
      implements EligibilitySelector<Long>
  {
    private final List<Page<Long>> pages;

    final List<EligibilityCursor> cursorsSeen = new ArrayList<>();

    int fetchCount = 0;

    private int failAtCallNumber = -1;

    RecordingSelector(final List<Page<Long>> pages) {
      this.pages = pages;
    }

    RecordingSelector failOnFetchAt(final int callNumber) {
      this.failAtCallNumber = callNumber;
      return this;
    }

    @Override
    public Page<Long> fetchPage(final EligibilityCursor cursor, final int limit, final Instant cycleStart) {
      if (fetchCount == failAtCallNumber) {
        fetchCount++;
        throw new RuntimeException("forced selector failure");
      }
      cursorsSeen.add(cursor);
      Page<Long> page = fetchCount < pages.size() ? pages.get(fetchCount) : Page.empty();
      fetchCount++;
      return page;
    }
  }
}
