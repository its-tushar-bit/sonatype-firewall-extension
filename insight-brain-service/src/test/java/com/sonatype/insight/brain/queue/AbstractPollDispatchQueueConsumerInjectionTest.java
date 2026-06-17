/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.queue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the executor+semaphore constructor overload added in Step 2 of the unified continuous
 * monitoring queue work (CLM-40039, design Section 3 / Acceptance AT-020):
 * <ul>
 * <li>new constructor accepts an injected {@link ExecutorService} and {@link Semaphore};</li>
 * <li>existing constructor remains the default path with no behaviour change;</li>
 * <li>the semaphore enforces the in-flight cap regardless of how many jobs are acquired;</li>
 * <li>permits are released when tasks complete (success or failure) so subsequent polls can
 * dispatch again.</li>
 * </ul>
 */
public class AbstractPollDispatchQueueConsumerInjectionTest
{
  private ExecutorService injectedExecutor;

  @Before
  public void setUp() {
    TenantTestHelper.setSingleTenant();
  }

  @After
  public void tearDown() {
    if (injectedExecutor != null) {
      injectedExecutor.shutdownNow();
    }
    TenantTestHelper.resetAfterTest();
  }

  @Test
  public void testRun_injectedConstructorPullsOneRowPerTick() throws Exception {
    Semaphore semaphore = new Semaphore(3);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        Collections.emptyList());
    consumer.disableForTesting = true;

    consumer.run();

    // Per design §6.2 / reviewer #24, the injected strategy pulls exactly one row per poll tick
    // whenever any permit is available — proving the injected path was selected (not the legacy
    // thread-pool capacity math) and that batch size is capped at 1.
    assertThat(consumer.lastRequestedCapacity.get()).isEqualTo(1);
  }

  @Test
  public void testRun_zeroCapacityWhenAllSemaphorePermitsHeld() throws Exception {
    Semaphore semaphore = new Semaphore(1);
    semaphore.acquire(); // simulate one in-flight worker
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        Collections.emptyList());
    consumer.disableForTesting = true;

    consumer.run();

    // No permits free → no acquireJobs call (capacity 0 short-circuits before the DAO is touched).
    assertThat(consumer.lastRequestedCapacity.get()).isZero();
    semaphore.release();
  }

  @Test
  public void testRun_semaphoreCapsInFlightDispatchesAcrossTicks() throws Exception {
    // With pull-one-per-tick each run() dispatches at most one job; two ticks fill the 2-permit
    // semaphore, demonstrating the cap holds across ticks.
    Semaphore semaphore = new Semaphore(2);
    injectedExecutor = Executors.newFixedThreadPool(4);

    // No-timeout latch keeps both jobs blocked indefinitely so the assertion below can never race
    // with a job completing and releasing its permit prematurely.
    CountDownLatch hold = new CountDownLatch(1);
    AtomicInteger started = new AtomicInteger();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        List.of("job-1"))
    {
      @Override
      protected void executeJob(final String job) throws Exception {
        started.incrementAndGet();
        hold.await();
      }
    };
    consumer.disableForTesting = true;

    consumer.run(); // tick 1 → dispatches job-1, takes permit 1
    consumer.armNextBatch(List.of("job-2"));
    consumer.run(); // tick 2 → dispatches job-2, takes permit 2

    assertThat(semaphore.availablePermits()).isZero();
    assertThat(consumer.dispatched).containsExactlyInAnyOrder("job-1", "job-2");

    hold.countDown();
    injectedExecutor.shutdown();
    assertThat(injectedExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    assertThat(started.get()).isEqualTo(2);
    assertThat(semaphore.availablePermits()).isEqualTo(2);
  }

  @Test
  public void testDispatch_permitReleasedOnTaskFailure() throws Exception {
    Semaphore semaphore = new Semaphore(1);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        List.of("doomed"))
    {
      @Override
      protected void executeJob(final String job) {
        throw new RuntimeException("boom");
      }
    };
    consumer.disableForTesting = true;

    consumer.run();
    injectedExecutor.shutdown();
    assertThat(injectedExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

    assertThat(semaphore.availablePermits()).isEqualTo(1);
    assertThat(consumer.failures).hasSize(1);
  }

  @Test
  public void testDispatch_permitReleasedOnInterruptedException() throws Exception {
    Semaphore semaphore = new Semaphore(1);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        List.of("interrupted-job"))
    {
      @Override
      protected void executeJob(final String job) throws InterruptedException {
        throw new InterruptedException("simulated interrupt");
      }
    };
    consumer.disableForTesting = true;

    consumer.run();
    injectedExecutor.shutdown();
    assertThat(injectedExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

    // Permit must be released even when the worker throws InterruptedException; otherwise the
    // semaphore would leak permits across consumer ticks and silently throttle the queue.
    assertThat(semaphore.availablePermits()).isEqualTo(1);
    assertThat(consumer.failures).containsExactly("interrupted-job");
  }

  @Test
  public void testDispatch_permitReleasedOnError() throws Exception {
    // processJob() catches Error (OOME, StackOverflowError, AssertionError, ...) and re-throws
    // without calling onJobFailure (the JVM may be unsafe for DB calls). The semaphore.release()
    // in the dispatch lambda's finally{} block must still fire — otherwise an Error would silently
    // throttle the queue to zero throughput. AssertionError is a real Error subtype and avoids
    // simulating an actual OOME.
    Semaphore semaphore = new Semaphore(1);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        List.of("fatal-job"))
    {
      @Override
      protected void executeJob(final String job) {
        throw new AssertionError("simulated fatal error");
      }
    };
    consumer.disableForTesting = true;

    consumer.run();
    injectedExecutor.shutdown();
    assertThat(injectedExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

    // Lambda's finally{semaphore.release()} must fire even on Error.
    assertThat(semaphore.availablePermits()).isEqualTo(1);
    // onJobFailure is intentionally NOT called for Error (see processJob), so failures stays empty.
    assertThat(consumer.failures).isEmpty();
  }

  @Test
  public void testDispatch_rejectedExecutionReleasesPermitAndUnacquiresJob() throws Exception {
    Semaphore semaphore = new Semaphore(1);
    injectedExecutor = Executors.newSingleThreadExecutor();
    // Force RejectedExecutionException by shutting the executor down BEFORE the consumer runs.
    // The dispatch path will succeed at tryAcquire() (permit was free) and then fail at submit().
    injectedExecutor.shutdown();
    assertThat(injectedExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        List.of("rejected-job"));
    consumer.disableForTesting = true;

    consumer.run();

    // Both recovery actions must fire: permit released (so the queue isn't permanently throttled)
    // and unacquireJobs called (so the row returns to PENDING for retry on the next startup).
    assertThat(semaphore.availablePermits()).isEqualTo(1);
    assertThat(consumer.unacquired).containsExactly("rejected-job");
  }

  @Test
  public void testCleanup_unacquiresInflightJobsOnInjectedPath() throws Exception {
    // Setup: dispatch a job that is submitted to the executor but never picked up by a worker.
    // The job sits in queuedItemIds because the worker (and therefore QueueTask.onStart) never
    // ran. cleanup() on the injected path must drain queuedItemIds and call unacquireJobs so
    // any rows still IN_PROGRESS return to PENDING for retry on the next startup.
    //
    // We achieve "submitted but not yet picked up" by saturating a single-thread executor with
    // a long-blocking pre-task: real pool thread blocks on the latch, the next submit() goes
    // into the executor's internal queue (tryAcquire takes the permit but task.run() never
    // fires for the queued job).
    //
    // Note: this test deliberately violates the documented cleanup() ordering contract (caller
    // should drain the executor first). Once cleanup() unacquires the job and we then release
    // holdPreTask, the queued QueueTask eventually runs on the executor — calling onJobSuccess
    // on the stub (a no-op). In production this is harmless because both DAO updates that
    // follow onJobSuccess are guarded on status='IN_PROGRESS' and silently no-op against the
    // already-PENDING row. The test asserts only on the unacquired-list (the actual cleanup
    // contract); future readers should not "fix" the test by draining the executor first.
    Semaphore semaphore = new Semaphore(2);
    injectedExecutor = Executors.newSingleThreadExecutor();
    CountDownLatch holdPreTask = new CountDownLatch(1);
    injectedExecutor.submit(() -> {
      try {
        holdPreTask.await(10, TimeUnit.SECONDS);
      }
      catch (InterruptedException ignored) {
      }
    });

    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        List.of("queued-but-not-started"));
    consumer.disableForTesting = true;

    consumer.run(); // dispatches queued-but-not-started; the executor's worker is busy on
                    // holdPreTask, so QueueTask.onStart for "queued-but-not-started" hasn't fired

    consumer.cleanup();

    // cleanup() must have called unacquireJobs with the in-flight id (Ross pattern 4: assert
    // real state — the unacquired list — not mock interactions).
    assertThat(consumer.unacquired).contains("queued-but-not-started");

    holdPreTask.countDown();
    injectedExecutor.shutdown();
    assertThat(injectedExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void testRun_legacyConstructorUsesThreadPoolCapacityMath() throws Exception {
    // No suppliers passed — legacy path. Capacity should equal workerCount + maxQueuedRows when
    // the pool is idle (3 + 2 = 5), proving the injected strategy was NOT used.
    StubConsumer consumer = new StubConsumer(new ShutdownHandler(), Collections.emptyList());
    consumer.disableForTesting = true;

    consumer.run();

    assertThat(consumer.lastRequestedCapacity.get())
        .isEqualTo(consumer.getWorkerThreadCount() + consumer.getMaxQueuedRows());
    consumer.cleanup();
  }

  // ---------------- helpers ----------------

  /**
   * Minimal subclass exercising both constructors. {@code acquireJobs} returns a fixed list once,
   * then empty; this lets tests assert on dispatched ids without scheduling threads.
   */
  private static class StubConsumer
      extends AbstractPollDispatchQueueConsumer<String>
  {
    final AtomicInteger lastRequestedCapacity = new AtomicInteger();

    final List<String> dispatched = Collections.synchronizedList(new ArrayList<>());

    final List<String> failures = Collections.synchronizedList(new ArrayList<>());

    final List<String> unacquired = Collections.synchronizedList(new ArrayList<>());

    private List<String> jobsToReturn;

    private boolean drained;

    void armNextBatch(final List<String> nextBatch) {
      this.jobsToReturn = nextBatch;
      this.drained = false;
    }

    StubConsumer(
        final ShutdownHandler shutdownHandler,
        final java.util.function.Supplier<ExecutorService> executorSupplier,
        final java.util.function.Supplier<Semaphore> semaphoreSupplier,
        final List<String> jobsToReturn)
    {
      super("StubConsumer", shutdownHandler, executorSupplier, semaphoreSupplier);
      this.jobsToReturn = jobsToReturn;
    }

    StubConsumer(
        final ShutdownHandler shutdownHandler,
        final List<String> jobsToReturn)
    {
      super("StubConsumer", shutdownHandler);
      this.jobsToReturn = jobsToReturn;
    }

    @Override
    protected List<String> acquireJobs(final int limit) {
      lastRequestedCapacity.set(limit);
      if (drained) {
        return List.of();
      }
      drained = true;
      dispatched.addAll(jobsToReturn);
      return new ArrayList<>(jobsToReturn);
    }

    @Override
    protected String getJobId(final String job) {
      return job;
    }

    @Override
    protected void executeJob(final String job) throws Exception {
      // overridden in tests as needed
    }

    @Override
    protected void onJobSuccess(final String job) {
      // no-op
    }

    @Override
    protected int incrementRetryCount(final String job) {
      return 1;
    }

    @Override
    protected void unacquireJobs(final Set<String> ids) {
      unacquired.addAll(ids);
    }

    @Override
    protected void permanentlyFailJob(final String job, final Exception cause) {
      failures.add(job);
    }

    @Override
    protected void onJobFailure(final String job, final Exception e) {
      // Bypass retry-counter dependence: record the failure and stop.
      failures.add(job);
    }

    @Override
    protected int getWorkerThreadCount() {
      return 3;
    }

    @Override
    protected int getMaxQueuedRows() {
      return 2;
    }

    @Override
    protected long getPollIntervalMs() {
      return 1000L;
    }

    @Override
    protected int getMaxRetries() {
      return 3;
    }

    @Override
    protected String getConsumerName() {
      return "StubConsumer";
    }

    @Override
    protected String getJitterSeed() {
      return "stub";
    }
  }
}
