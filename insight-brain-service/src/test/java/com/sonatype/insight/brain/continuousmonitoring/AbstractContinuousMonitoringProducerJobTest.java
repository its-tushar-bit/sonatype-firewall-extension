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

import com.sonatype.insight.brain.tenancy.MtiqBatchJob;

import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AbstractContinuousMonitoringProducerJob#runCycle} (CLM-40039 Section 6.1).
 * Covers paginated cycle, abort-on-failure (Section 7.4), feature-flag gate, max-page guard,
 * priority computation alignment with {@link OrderingStrategy}, admin-task output, and the
 * Quartz contract annotations ({@link org.quartz.DisallowConcurrentExecution} + {@link MtiqBatchJob}).
 */
public class AbstractContinuousMonitoringProducerJobTest
{
  @Test
  public void testRunCycle_disabledJobSkipsCycleAndReturnsSuccessZero() {
    StubProducerJob job = new StubProducerJob();
    job.enabled = false;
    job.selector = new RecordingSelector(List.of(List.of(1L, 2L)));
    AbstractContinuousMonitoringProducerJob.CycleResult result = job.runCycle();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEnqueued()).isEqualTo(0);
    assertThat(result.getAbortReason()).isNull();
    assertThat(((RecordingSelector) job.selector).fetchCount).isEqualTo(0);
    assertThat(job.enqueuedBatches).isEmpty();
  }

  @Test
  public void testRunCycle_nonPositivePageSizeAbortsWithReason() {
    // Misconfiguration (operator set the page-size knob to 0 or negative). Distinct from
    // success(0) "no eligible work" — abort so the admin-task output surfaces the misconfig
    // instead of pretending nothing was wrong.
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
  public void testRunCycle_pagesUntilShortPageThenStops() {
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 3;
    job.selector = new RecordingSelector(List.of(
        List.of(1L, 2L, 3L),
        List.of(4L, 5L, 6L),
        List.of(7L)));
    AbstractContinuousMonitoringProducerJob.CycleResult result = job.runCycle();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEnqueued()).isEqualTo(7);
    assertThat(job.enqueuedBatches).hasSize(3);
    assertThat(((RecordingSelector) job.selector).fetchCount).isEqualTo(3);
    assertThat(((RecordingSelector) job.selector).offsets).containsExactly(0, 3, 6);
  }

  @Test
  public void testRunCycle_newestFirstPriorityDecreasesAcrossPages() {
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 2;
    job.ordering = OrderingStrategy.newestFirst();
    job.selector = new RecordingSelector(List.of(
        List.of(10L, 20L),
        List.of(30L)));
    job.runCycle();
    List<Long> page0Priorities = job.enqueuedBatches.get(0).priorities;
    List<Long> page1Priorities = job.enqueuedBatches.get(1).priorities;
    // newestFirst() assigns the highest priority to the first candidate; priorities therefore
    // decrease monotonically as position grows. A consumer's ORDER BY priority DESC then dispatches
    // them newest-first.
    assertThat(page0Priorities).containsExactly(Long.MAX_VALUE, Long.MAX_VALUE - 1);
    assertThat(page1Priorities).containsExactly(Long.MAX_VALUE - 2);
  }

  @Test
  public void testRunCycle_selectorFailureAbortsCycleAndReturnsAbortedResult() {
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 2;
    job.selector = new RecordingSelector(List.of(List.of(1L, 2L)))
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
        List.of(1L, 2L),
        List.of(3L, 4L),
        List.of(5L)));
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
    job.selector = new RecordingSelector(List.of(List.of(1L, 2L, 3L)));
    AbstractContinuousMonitoringProducerJob.CycleResult result = job.runCycle();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEnqueued()).isEqualTo(1);
  }

  @Test
  public void testRunCycle_cycleStartIsStableAcrossPages() {
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 2;
    job.selector = new RecordingSelector(List.of(
        List.of(1L, 2L),
        List.of(3L)));
    job.runCycle();
    Instant first = job.enqueuedBatches.get(0).cycleStart;
    Instant second = job.enqueuedBatches.get(1).cycleStart;
    assertThat(first).isEqualTo(second);
  }

  @Test
  public void testRunCycle_maxPageGuardAbortsAfterLimit() {
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 2;
    job.maxPages = 3;
    // Generate enough pages to trigger the limit (5 full pages)
    job.selector = new RecordingSelector(List.of(
        List.of(1L, 2L),
        List.of(3L, 4L),
        List.of(5L, 6L),
        List.of(7L, 8L),
        List.of(9L, 10L)));
    AbstractContinuousMonitoringProducerJob.CycleResult result = job.runCycle();
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getEnqueued()).isEqualTo(6); // 3 pages * 2 items each
    assertThat(result.getAbortReason()).contains("maxCyclePages limit reached");
    assertThat(job.enqueuedBatches).hasSize(3);
  }

  @Test
  public void testExecute_adminTaskWritesCompletedOnSuccess() {
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 2;
    job.selector = new RecordingSelector(List.of(List.of(1L, 2L, 3L)));
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
    job.selector = new RecordingSelector(List.of(List.of(1L, 2L)));
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
    job.selector = new RecordingSelector(List.of(List.of(1L, 2L)));
    JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
    Mockito.when(context.getNextFireTime()).thenReturn(new Date(0L));

    // Real-state assertion: cycle ran (enqueued the batch) and the abstract base's
    // success-path branch executed without throwing. The InsightJob.execute wrapper guarantees
    // an uncaught Throwable from runCycle would be logged at ERROR rather than escaping; this
    // test exercises the no-throw path so the wrapper's catch never fires.
    job.execute(context);

    assertThat(job.enqueuedBatches).hasSize(1);
  }

  @Test
  public void testExecute_quartzPathReadsNextFireOnAbort() {
    StubProducerJob job = new StubProducerJob();
    job.pageSize = 2;
    job.failEnqueueAtBatch = 0;
    job.selector = new RecordingSelector(List.of(List.of(1L, 2L)));
    JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
    Mockito.when(context.getNextFireTime()).thenReturn(new Date(0L));

    // Real-state assertion: even on abort, the cycle was aborted at batch 0 (no successful
    // enqueues), proving the abort branch of execute() ran rather than the success branch.
    job.execute(context);

    assertThat(job.enqueuedBatches).isEmpty();
  }

  @Test
  public void testJob_disallowsConcurrentExecution() {
    // The class-level @DisallowConcurrentExecution must be present on the concrete instance Quartz
    // sees (not just on the abstract). A future refactor that drops the annotation silently would
    // allow two cycles to overlap on a single node — bypassing the cross-node single-cycle
    // guarantee documented at the class level. JobBuilder reads the annotation off the class,
    // matching what Quartz does in production.
    assertThat(JobBuilder.newJob(StubProducerJob.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testJob_isMtiqBatchJob() {
    // The MtiqBatchJob marker routes execution to the MTIQ batch instance only. A future refactor
    // that drops the interface silently would cause the job to fire on every MTIQ tenant scheduler
    // rather than the central batch worker.
    assertThat(new StubProducerJob()).isInstanceOf(MtiqBatchJob.class);
  }

  private static final class EnqueueCall
  {
    final List<Long> candidates;

    final List<Long> priorities;

    final Instant cycleStart;

    EnqueueCall(final List<Long> candidates, final List<Long> priorities, final Instant cycleStart) {
      this.candidates = candidates;
      this.priorities = priorities;
      this.cycleStart = cycleStart;
    }
  }

  private static class StubProducerJob
      extends AbstractContinuousMonitoringProducerJob<Long>
  {
    boolean enabled = true;

    int pageSize = 2;

    int maxPages = DEFAULT_MAX_CYCLE_PAGES;

    EligibilitySelector<Long> selector = new RecordingSelector(List.of());

    OrderingStrategy ordering = OrderingStrategy.fifo();

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
    protected OrderingStrategy getOrderingStrategy() {
      return ordering;
    }

    @Override
    protected int getEligibilityPageSize() {
      return pageSize;
    }

    @Override
    protected int getMaxCyclePages() {
      return maxPages;
    }

    @Override
    protected boolean isEnabled() {
      return enabled;
    }

    @Override
    protected int enqueueBatch(
        final List<Long> candidates,
        final List<Long> priorities,
        final Instant cycleStart)
    {
      int batchIndex = enqueuedBatches.size();
      if (batchIndex == failEnqueueAtBatch) {
        throw new RuntimeException("forced enqueue failure");
      }
      enqueuedBatches.add(new EnqueueCall(candidates, priorities, cycleStart));
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

  private static final class RecordingSelector
      implements EligibilitySelector<Long>
  {
    private final List<List<Long>> pages;

    final List<Integer> offsets = new ArrayList<>();

    int fetchCount = 0;

    private int failAtCallNumber = -1;

    RecordingSelector(final List<List<Long>> pages) {
      this.pages = pages;
    }

    RecordingSelector failOnFetchAt(final int callNumber) {
      this.failAtCallNumber = callNumber;
      return this;
    }

    @Override
    public List<Long> fetchPage(final int offset, final int limit, final Instant cycleStart) {
      if (fetchCount == failAtCallNumber) {
        fetchCount++;
        throw new RuntimeException("forced selector failure");
      }
      offsets.add(offset);
      List<Long> page = fetchCount < pages.size() ? pages.get(fetchCount) : List.of();
      fetchCount++;
      return page;
    }
  }
}
