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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the executor+semaphore constructor overload added in Step 2 of the unified continuous
 * monitoring queue work (CLM-40039, design Section 3 / Acceptance AT-020):
 * <ul>
 * <li>new constructor accepts an injected {@link ExecutorService} and {@link AdjustableSemaphore};</li>
 * <li>existing constructor remains the default path with no behaviour change;</li>
 * <li>the semaphore enforces the in-flight cap regardless of how many jobs are acquired;</li>
 * <li>permits are released when tasks complete (success or failure) so subsequent polls can
 * dispatch again.</li>
 * </ul>
 */
public class AbstractPollDispatchQueueConsumerInjectionTest
{
  private ExecutorService injectedExecutor;

  @BeforeEach
  public void setUp() {
    TenantTestHelper.setSingleTenant();
  }

  @AfterEach
  public void tearDown() {
    if (injectedExecutor != null) {
      injectedExecutor.shutdownNow();
    }
    TenantTestHelper.resetAfterTest();
  }

  @Test
  public void testRun_injectedConstructorPullsOneRowPerTick() throws Exception {
    AdjustableSemaphore semaphore = new AdjustableSemaphore(3);
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
    AdjustableSemaphore semaphore = new AdjustableSemaphore(1);
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
    AdjustableSemaphore semaphore = new AdjustableSemaphore(2);
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
    AdjustableSemaphore semaphore = new AdjustableSemaphore(1);
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
    AdjustableSemaphore semaphore = new AdjustableSemaphore(1);
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
    AdjustableSemaphore semaphore = new AdjustableSemaphore(1);
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
    AdjustableSemaphore semaphore = new AdjustableSemaphore(1);
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
    AdjustableSemaphore semaphore = new AdjustableSemaphore(2);
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

  // Drain-to-empty tests --------------------------------------------------------------------

  @Test
  public void tickCapacity_clampedToTickBatchSize() throws Exception {
    AdjustableSemaphore semaphore = new AdjustableSemaphore(8);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        Collections.emptyList());
    consumer.tickBatchSize = 4;
    consumer.disableForTesting = true;

    consumer.run();

    assertThat(consumer.lastRequestedCapacity.get()).isEqualTo(4);
  }

  @Test
  public void tickCapacity_clampedToAvailablePermitsWhenSmallerThanBatch() throws Exception {
    AdjustableSemaphore semaphore = new AdjustableSemaphore(2);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        Collections.emptyList());
    consumer.tickBatchSize = 8;
    consumer.disableForTesting = true;

    consumer.run();

    assertThat(consumer.lastRequestedCapacity.get()).isEqualTo(2);
  }

  @Test
  public void drainLoop_processesAllAdditionalJobsThenExits() throws Exception {
    AdjustableSemaphore semaphore = new AdjustableSemaphore(1);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        List.of("seed"));
    consumer.tickBatchSize = 1;
    consumer.additionalDrainJobs.add("after-1");
    consumer.additionalDrainJobs.add("after-2");
    consumer.additionalDrainJobs.add("after-3");
    consumer.disableForTesting = true;

    consumer.run();
    injectedExecutor.shutdown();
    injectedExecutor.awaitTermination(5, TimeUnit.SECONDS);

    assertThat(consumer.dispatched).containsExactly("seed", "after-1", "after-2", "after-3");
    // Permit returned after drain exits.
    assertThat(semaphore.availablePermits()).isEqualTo(1);
  }

  @Test
  public void drainLoop_permitReleasedOnNormalExit() throws Exception {
    AdjustableSemaphore semaphore = new AdjustableSemaphore(1);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        List.of("seed"));
    consumer.disableForTesting = true;

    consumer.run();
    injectedExecutor.shutdown();
    injectedExecutor.awaitTermination(5, TimeUnit.SECONDS);

    assertThat(semaphore.availablePermits()).isEqualTo(1);
  }

  @Test
  public void drainLoop_permitReleasedOnRuntimeException() throws Exception {
    AdjustableSemaphore semaphore = new AdjustableSemaphore(1);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        List.of("seed"))
    {
      @Override
      protected void executeJob(final String job) {
        throw new RuntimeException("forced");
      }
    };
    consumer.disableForTesting = true;

    consumer.run();
    injectedExecutor.shutdown();
    injectedExecutor.awaitTermination(5, TimeUnit.SECONDS);

    // Even though executeJob threw, the permit must be released so the next tick can dispatch.
    assertThat(semaphore.availablePermits()).isEqualTo(1);
  }

  @Test
  public void drainLoop_permitReleasedOnError() throws Exception {
    AdjustableSemaphore semaphore = new AdjustableSemaphore(1);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        List.of("seed"))
    {
      @Override
      protected void executeJob(final String job) {
        throw new Error("forced");
      }
    };
    consumer.disableForTesting = true;

    consumer.run();
    injectedExecutor.shutdown();
    injectedExecutor.awaitTermination(5, TimeUnit.SECONDS);

    // Permit returned even on Error. The Error itself still propagates per existing processJob
    // semantics; we just verify the finally{} release was reached.
    assertThat(semaphore.availablePermits()).isEqualTo(1);
  }

  @Test
  public void semaphoreResize_increasesAvailablePermits() {
    AdjustableSemaphore semaphore = new AdjustableSemaphore(4);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        Collections.emptyList());
    consumer.disableForTesting = true;

    // Force the strategy (and therefore the injectedSemaphores binding) to materialise by running
    // one tick. After this the per-tenant AdjustableSemaphore is registered.
    try {
      consumer.run();
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError(e);
    }

    consumer.handleConfigurationChanged(16, 1000L, true, 1000L, true);

    assertThat(semaphore.availablePermits()).isEqualTo(16);
    assertThat(semaphore.getMaxPermits()).isEqualTo(16);
  }

  @Test
  public void semaphoreResize_decreaseDoesNotInterruptRunningWorkers() throws Exception {
    AdjustableSemaphore semaphore = new AdjustableSemaphore(4);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        Collections.emptyList());
    consumer.disableForTesting = true;
    consumer.run();

    // Simulate 2 workers in-flight by consuming 2 permits.
    semaphore.acquire();
    semaphore.acquire();
    assertThat(semaphore.availablePermits()).isEqualTo(2);

    // Decrease to 2: must NOT interrupt the 2 acquired permits; they remain valid until released.
    consumer.handleConfigurationChanged(2, 1000L, true, 1000L, true);

    // 0 free permits — the 2 we acquired stand, the other 2 were absorbed by reducePermits.
    assertThat(semaphore.availablePermits()).isEqualTo(0);
    assertThat(semaphore.getMaxPermits()).isEqualTo(2);

    // Releasing the held permits brings the available count to the new max.
    semaphore.release();
    semaphore.release();
    assertThat(semaphore.availablePermits()).isEqualTo(2);
  }

  @Test
  public void drainLoop_continuesAfterProcessJobRuntimeException() throws Exception {
    // A transient processJob failure mid-drain must NOT abort the remaining batch. The failed
    // record is recorded as a failure (onJobFailure), the loop continues to subsequent records,
    // and the permit is released exactly once.
    AdjustableSemaphore semaphore = new AdjustableSemaphore(1);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        List.of("seed"))
    {
      @Override
      protected void executeJob(final String job) {
        if ("boom".equals(job)) {
          throw new RuntimeException("forced mid-drain failure");
        }
      }
    };
    consumer.additionalDrainJobs.add("boom");
    consumer.additionalDrainJobs.add("after-boom");
    consumer.disableForTesting = true;

    consumer.run();
    injectedExecutor.shutdown();
    assertThat(injectedExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

    assertThat(consumer.dispatched).containsExactly("seed", "boom", "after-boom");
    assertThat(consumer.failures).containsExactly("boom");
    assertThat(semaphore.availablePermits()).isEqualTo(1);
  }

  @Test
  public void drainLoop_exitsOnInterruptedExceptionWrappedByProcessJob() throws Exception {
    // processJob wraps InterruptedException in UncheckedInterruptedException (clearing the
    // interrupt bit). That wrapped form is a RuntimeException and would otherwise be swallowed
    // by the log-and-continue branch, causing the drain-worker to keep draining after a shutdown
    // signal. The catch block must detect the wrapped interrupt, restore the flag, and exit.
    AdjustableSemaphore semaphore = new AdjustableSemaphore(1);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        List.of("seed"))
    {
      @Override
      protected void executeJob(final String job) throws InterruptedException {
        if ("interruptible".equals(job)) {
          throw new InterruptedException("simulated shutdown mid-drain");
        }
      }
    };
    consumer.additionalDrainJobs.add("interruptible");
    consumer.additionalDrainJobs.add("should-not-be-processed");
    consumer.disableForTesting = true;

    consumer.run();
    injectedExecutor.shutdown();
    assertThat(injectedExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

    // Seed and the interrupted record were processed; the post-interrupt record must NOT be.
    assertThat(consumer.dispatched).containsExactly("seed", "interruptible");
    assertThat(consumer.additionalDrainJobs).containsExactly("should-not-be-processed");
    // onJobFailure was invoked for the interrupted record (processJob path).
    assertThat(consumer.failures).containsExactly("interruptible");
    assertThat(semaphore.availablePermits()).isEqualTo(1);
  }

  @Test
  public void drainLoop_exitsOnDeeplyWrappedInterruptedException() throws Exception {
    // A flow processor may wrap InterruptedException in its own RuntimeException, then jOOQ /
    // another layer wraps THAT in another RuntimeException. The earlier shallow check
    // (`getCause() instanceof InterruptedException`) only looked one level deep, so a
    // RuntimeException → RuntimeException → InterruptedException chain would slip through and
    // the drain-loop would keep going. The fix walks the full cause + suppressed chains.
    AdjustableSemaphore semaphore = new AdjustableSemaphore(1);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        List.of("seed"))
    {
      @Override
      protected void executeJob(final String job) {
        if ("deeply-wrapped".equals(job)) {
          // Two layers of RuntimeException between us and the InterruptedException.
          RuntimeException inner = new RuntimeException(new InterruptedException("simulated"));
          throw new RuntimeException("flow-processor wrapper", inner);
        }
      }
    };
    consumer.additionalDrainJobs.add("deeply-wrapped");
    consumer.additionalDrainJobs.add("should-not-be-processed");
    consumer.disableForTesting = true;

    consumer.run();
    injectedExecutor.shutdown();
    assertThat(injectedExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

    assertThat(consumer.dispatched).containsExactly("seed", "deeply-wrapped");
    assertThat(consumer.additionalDrainJobs).containsExactly("should-not-be-processed");
    assertThat(semaphore.availablePermits()).isEqualTo(1);
  }

  @Test
  public void drainLoop_postBackoffAcquireWithWrappedInterruptExitsCleanly() throws Exception {
    // Coverage for the post-backoff acquireOneMore catch site (previously inconsistent with the
    // other two — no interrupt handling at all). With idleBackoffMs > 0 the first empty self-poll
    // triggers a backoff sleep, then one retry. If that retry throws a RuntimeException whose
    // cause chain contains an InterruptedException, the catch must restore the interrupt flag and
    // exit cleanly with the permit released.
    AdjustableSemaphore sem = new AdjustableSemaphore(1);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> sem,
        List.of("seed"))
    {
      private final AtomicInteger calls = new AtomicInteger();

      @Override
      protected java.util.Optional<String> acquireOneMore() {
        int n = calls.incrementAndGet();
        if (n == 1) {
          return java.util.Optional.empty();
        }
        throw new RuntimeException("post-backoff DAO failure", new InterruptedException("shutdown"));
      }
    };
    consumer.idleBackoffMs = 10L;
    consumer.disableForTesting = true;

    consumer.run();
    injectedExecutor.shutdown();
    assertThat(injectedExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

    assertThat(consumer.dispatched).containsExactly("seed");
    assertThat(sem.availablePermits()).isEqualTo(1);
  }

  @Test
  public void drainLoop_exitsCleanlyWhenAcquireOneMoreThrows() throws Exception {
    // A DAO blip during acquireOneMore must exit the drain loop without propagating; the seed
    // was already processed successfully so failing the drain attempt does not retroactively fail
    // the seed. The permit must be released.
    AdjustableSemaphore semaphore = new AdjustableSemaphore(1);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        List.of("seed"))
    {
      @Override
      protected java.util.Optional<String> acquireOneMore() {
        throw new RuntimeException("forced DAO blip");
      }
    };
    consumer.disableForTesting = true;

    consumer.run();
    injectedExecutor.shutdown();
    assertThat(injectedExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

    assertThat(consumer.dispatched).containsExactly("seed");
    assertThat(consumer.failures).isEmpty();
    assertThat(semaphore.availablePermits()).isEqualTo(1);
  }

  @Test
  public void drainLoop_idleBackoffRetriesOnceThenExitsOnEmpty() throws Exception {
    // With idleBackoffMs > 0, after the first empty self-poll the worker must sleep then retry
    // exactly once. If still empty, exit. Use a counter to verify exactly two acquireOneMore
    // calls happen (initial + post-backoff) when the queue stays empty.
    AdjustableSemaphore semaphore = new AdjustableSemaphore(1);
    injectedExecutor = Executors.newSingleThreadExecutor();
    AtomicInteger acquireOneMoreCalls = new AtomicInteger();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        List.of("seed"))
    {
      @Override
      protected java.util.Optional<String> acquireOneMore() {
        acquireOneMoreCalls.incrementAndGet();
        return java.util.Optional.empty();
      }
    };
    consumer.idleBackoffMs = 10L;
    consumer.disableForTesting = true;

    consumer.run();
    injectedExecutor.shutdown();
    assertThat(injectedExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

    // 2 calls: the initial empty poll → backoff → one retry → still empty → exit.
    assertThat(acquireOneMoreCalls.get()).isEqualTo(2);
    assertThat(semaphore.availablePermits()).isEqualTo(1);
  }

  @Test
  public void drainLoop_idleBackoffPicksUpRecordThatArrivedDuringSleep() throws Exception {
    // Non-zero idleBackoffMs lets a worker absorb a near-empty trickle. If a record appears
    // during the backoff sleep, the post-backoff retry must pick it up and process it.
    AdjustableSemaphore semaphore = new AdjustableSemaphore(1);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        List.of("seed"))
    {
      private final AtomicInteger calls = new AtomicInteger();

      @Override
      protected java.util.Optional<String> acquireOneMore() {
        int n = calls.incrementAndGet();
        if (n == 1) {
          // First call: queue empty; triggers backoff.
          return java.util.Optional.empty();
        }
        if (n == 2) {
          // Post-backoff retry: a producer dropped one record while we slept.
          dispatched.add("trickle-arrival");
          return java.util.Optional.of("trickle-arrival");
        }
        return java.util.Optional.empty();
      }
    };
    consumer.idleBackoffMs = 10L;
    consumer.disableForTesting = true;

    consumer.run();
    injectedExecutor.shutdown();
    assertThat(injectedExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

    assertThat(consumer.dispatched).containsExactly("seed", "trickle-arrival");
    assertThat(semaphore.availablePermits()).isEqualTo(1);
  }

  @Test
  public void dispatch_tryAcquireFailsUnacquiresJobAndDoesNotLeakPermit() throws Exception {
    // Race window: availableCapacity() reports a free permit, but between the capacity check and
    // dispatch's inner tryAcquire(), the permit is consumed (e.g. by a concurrent worker, or a
    // resize). The defer branch must unacquire the job (so it returns to PENDING for retry on
    // the next tick) and must not release a permit it never acquired.
    AdjustableSemaphore semaphore = new AdjustableSemaphore(1);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        List.of("racy"))
    {
      @Override
      protected List<String> acquireJobs(final int limit) {
        List<String> result = super.acquireJobs(limit);
        // Steal the only permit between the capacity check and dispatch's inner tryAcquire().
        // This deterministically reproduces the resize/inflight race.
        try {
          semaphore.acquire();
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
        return result;
      }
    };
    consumer.disableForTesting = true;

    consumer.run();
    injectedExecutor.shutdown();
    assertThat(injectedExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

    // executeJob was never invoked on the deferred job — defer branch is no-op for processing.
    assertThat(consumer.dispatched).containsExactly("racy");
    assertThat(consumer.unacquired).containsExactly("racy");
    // We acquired the permit manually in acquireJobs; release it back. The defer branch must NOT
    // have released anything it didn't own — if it had, availablePermits() would now be 1 not 0.
    assertThat(semaphore.availablePermits()).isEqualTo(0);
    semaphore.release();
    assertThat(semaphore.availablePermits()).isEqualTo(1);
  }

  @Test
  public void drainLoop_interruptedWorkerExitsCleanlyAndReleasesPermit() throws Exception {
    // An interrupted drain-worker must exit the loop on the next iteration, propagate the
    // interrupt bit, and release its permit. No leak even when interrupted mid-drain.
    AdjustableSemaphore semaphore = new AdjustableSemaphore(1);
    injectedExecutor = Executors.newSingleThreadExecutor();
    StubConsumer consumer = new StubConsumer(
        new ShutdownHandler(),
        () -> injectedExecutor,
        () -> semaphore,
        List.of("seed"))
    {
      @Override
      protected void executeJob(final String job) {
        // Self-interrupt after processing the seed — drainAdditional's loop guard
        // (Thread.currentThread().isInterrupted()) must observe this on the next iteration and
        // exit before acquireOneMore() is even called.
        if ("seed".equals(job)) {
          Thread.currentThread().interrupt();
        }
      }
    };
    // If the loop guard fails, drain would dispatch this too.
    consumer.additionalDrainJobs.add("should-not-be-processed");
    consumer.disableForTesting = true;

    consumer.run();
    injectedExecutor.shutdown();
    assertThat(injectedExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

    assertThat(consumer.dispatched).containsExactly("seed");
    assertThat(consumer.additionalDrainJobs).containsExactly("should-not-be-processed");
    assertThat(semaphore.availablePermits()).isEqualTo(1);
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

    /** Used by drain-loop tests: each acquireOneMore call pops one entry. */
    final java.util.Deque<String> additionalDrainJobs = new java.util.concurrent.ConcurrentLinkedDeque<>();

    /** Per-test override for getTickBatchSize. */
    int tickBatchSize = 1;

    /** Per-test override for getIdleBackoffMs. */
    long idleBackoffMs = 0L;

    private List<String> jobsToReturn;

    private boolean drained;

    void armNextBatch(final List<String> nextBatch) {
      this.jobsToReturn = nextBatch;
      this.drained = false;
    }

    StubConsumer(
        final ShutdownHandler shutdownHandler,
        final java.util.function.Supplier<ExecutorService> executorSupplier,
        final java.util.function.Supplier<AdjustableSemaphore> semaphoreSupplier,
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
      // Return at most `limit` rows so tickBatchSize is meaningfully exercised.
      int toReturn = Math.min(limit, jobsToReturn.size());
      List<String> result = new ArrayList<>(jobsToReturn.subList(0, toReturn));
      dispatched.addAll(result);
      return result;
    }

    @Override
    protected java.util.Optional<String> acquireOneMore() {
      String next = additionalDrainJobs.pollFirst();
      if (next == null) {
        return java.util.Optional.empty();
      }
      dispatched.add(next);
      return java.util.Optional.of(next);
    }

    @Override
    protected int getTickBatchSize() {
      return tickBatchSize;
    }

    @Override
    protected long getIdleBackoffMs() {
      return idleBackoffMs;
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
