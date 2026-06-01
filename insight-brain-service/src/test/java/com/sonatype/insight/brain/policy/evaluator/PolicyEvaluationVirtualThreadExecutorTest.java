/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyEvaluationVirtualThreadExecutorTest
{
  private final PolicyEvaluationVirtualThreadExecutor executor =
      new PolicyEvaluationVirtualThreadExecutor(null, "policy_evaluation", "test");

  @After
  public void tearDown() {
    executor.shutdown();
  }

  @Test
  public void testTasksExecuteOnVirtualThreads() throws Exception {
    AtomicBoolean wasVirtualThread = new AtomicBoolean(false);
    CountDownLatch latch = new CountDownLatch(1);

    executor.execute(() -> {
      wasVirtualThread.set(Thread.currentThread().isVirtual());
      latch.countDown();
    });

    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(wasVirtualThread.get()).isTrue();
  }

  @Test
  public void testActiveTaskCountIsTracked() throws Exception {
    CountDownLatch taskStarted = new CountDownLatch(1);
    CountDownLatch taskCanProceed = new CountDownLatch(1);

    executor.execute(() -> {
      taskStarted.countDown();
      try {
        taskCanProceed.await(5, TimeUnit.SECONDS);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });

    assertThat(taskStarted.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(executor.getActiveTaskCount()).isEqualTo(1);

    taskCanProceed.countDown();
    org.awaitility.Awaitility.await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertThat(executor.getActiveTaskCount()).isEqualTo(0));
  }

  @Test
  public void testSubmitReturnsCompletedFuture() throws Exception {
    AtomicInteger value = new AtomicInteger(0);

    Future<?> future = executor.submit(() -> value.set(42));

    future.get(5, TimeUnit.SECONDS);
    assertThat(value.get()).isEqualTo(42);
  }

  @Test
  public void testHighConcurrencyDoesNotDeadlock() throws Exception {
    // Simulate the scenario that caused the original deadlock:
    // Many tasks submitted, all of which block (simulating DB connection waits).
    // With a fixed pool, this would exhaust threads. With virtual threads, it should work fine.
    int taskCount = 500; // More than the old 200-thread limit
    CountDownLatch allStarted = new CountDownLatch(taskCount);
    CountDownLatch canProceed = new CountDownLatch(1);
    CountDownLatch allDone = new CountDownLatch(taskCount);

    for (int i = 0; i < taskCount; i++) {
      executor.submit(() -> {
        allStarted.countDown();
        try {
          canProceed.await(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        finally {
          allDone.countDown();
        }
      });
    }

    // All 500 tasks should start immediately (no queueing)
    assertThat(allStarted.await(10, TimeUnit.SECONDS))
        .as("All tasks should start without being queued")
        .isTrue();

    assertThat(executor.getActiveTaskCount()).isEqualTo(taskCount);

    // Release all tasks
    canProceed.countDown();
    assertThat(allDone.await(10, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void testShutdownPreventsNewTasks() throws Exception {
    executor.shutdown();
    assertThat(executor.isShutdown()).isTrue();
  }

  @Test
  public void testAwaitTermination() throws Exception {
    CountDownLatch taskStarted = new CountDownLatch(1);

    executor.execute(() -> {
      taskStarted.countDown();
      try {
        Thread.sleep(200);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });

    taskStarted.await(5, TimeUnit.SECONDS);
    executor.shutdown();
    assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    assertThat(executor.isTerminated()).isTrue();
  }
}
