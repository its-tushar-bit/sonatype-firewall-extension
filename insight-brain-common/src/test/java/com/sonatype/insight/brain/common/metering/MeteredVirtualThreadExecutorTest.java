/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.common.metering;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class MeteredVirtualThreadExecutorTest
{
  private MeteredVirtualThreadExecutor executor;

  @After
  public void after() {
    if (executor != null) {
      executor.shutdown();
      executor = null;
    }
  }

  @Test
  public void testActiveGaugeTracksRunningTasks() throws Exception {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredVirtualThreadExecutor(meterRegistry, Tags.of("kind", "test_kind", "name", "TestService"));

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

    Gauge activeGauge = meterRegistry.find("executor.active")
        .tag("kind", "test_kind")
        .tag("name", "TestService")
        .gauge();
    assertThat(activeGauge).isNotNull();
    assertThat(activeGauge.value()).isEqualTo(1.0);

    taskCanProceed.countDown();

    await().atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertThat(activeGauge.value()).isEqualTo(0.0));
  }

  @Test
  public void testCompletedCounterIncrementsOnSuccess() throws Exception {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredVirtualThreadExecutor(meterRegistry, Tags.of("kind", "test_kind", "name", "TestService"));

    CountDownLatch done = new CountDownLatch(1);
    executor.execute(done::countDown);
    assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();

    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      Counter completedCounter = meterRegistry.find("executor.completed")
          .tag("kind", "test_kind")
          .tag("name", "TestService")
          .counter();
      assertThat(completedCounter).isNotNull();
      assertThat(completedCounter.count()).isEqualTo(1.0);
    });
  }

  @Test
  public void testFailedCounterIncrementsOnFailure() throws Exception {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredVirtualThreadExecutor(meterRegistry, Tags.of("kind", "test_kind", "name", "TestService"));

    CountDownLatch taskRan = new CountDownLatch(1);
    // Use submit() so the exception is captured in the Future rather than
    // propagating to the virtual thread's uncaught exception handler
    executor.submit(() -> {
      taskRan.countDown();
      throw new RuntimeException("deliberate failure");
    });
    assertThat(taskRan.await(5, TimeUnit.SECONDS)).isTrue();

    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      Counter failedCounter = meterRegistry.find("executor.failed")
          .tag("kind", "test_kind")
          .tag("name", "TestService")
          .counter();
      assertThat(failedCounter).isNotNull();
      assertThat(failedCounter.count()).isEqualTo(1.0);
    });
  }

  @Test
  public void testExecutionTimerRecordsTime() throws Exception {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredVirtualThreadExecutor(meterRegistry, Tags.of("kind", "test_kind", "name", "TestService"));

    CountDownLatch done = new CountDownLatch(1);
    executor.execute(() -> {
      try {
        Thread.sleep(50);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      done.countDown();
    });
    assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();

    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      Timer runningTimer = meterRegistry.find("executor")
          .tag("kind", "test_kind")
          .tag("name", "TestService")
          .timer();
      assertThat(runningTimer).isNotNull();
      assertThat(runningTimer.count()).isEqualTo(1);
      assertThat(runningTimer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(50);
    });
  }

  @Test
  public void testMetersRemovedOnShutdown() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredVirtualThreadExecutor(meterRegistry, Tags.of("kind", "test_kind", "name", "TestService"));

    assertThat(meterRegistry.find("executor.active").tag("kind", "test_kind").gauge()).isNotNull();

    executor.shutdown();

    assertThat(meterRegistry.find("executor.active").tag("kind", "test_kind").gauge()).isNull();
    executor = null; // prevent double-shutdown in @After
  }

  @Test
  public void testNoMetersRegisteredWithNullRegistry() {
    executor = new MeteredVirtualThreadExecutor(null, Tags.of("kind", "test_kind", "name", "TestService"));

    CountDownLatch done = new CountDownLatch(1);
    executor.execute(done::countDown);

    // Should not throw — just runs without metrics
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(done.getCount()).isEqualTo(0));
  }

  @Test
  public void testNoMetersRegisteredWithEmptyTags() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredVirtualThreadExecutor(meterRegistry, Tags.empty());

    CountDownLatch done = new CountDownLatch(1);
    executor.execute(done::countDown);

    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(done.getCount()).isEqualTo(0));

    // No meters should be registered
    assertThat(meterRegistry.find("executor.active").gauge()).isNull();
  }

  @Test
  public void testTasksRunOnVirtualThreads() throws Exception {
    executor = new MeteredVirtualThreadExecutor(null, Tags.of("kind", "test_kind", "name", "TestService"));

    CountDownLatch done = new CountDownLatch(1);
    boolean[] wasVirtual = {false};
    executor.execute(() -> {
      wasVirtual[0] = Thread.currentThread().isVirtual();
      done.countDown();
    });

    assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(wasVirtual[0]).isTrue();
  }

  @Test
  public void testQueuedGaugeTracksSubmittedButNotYetRunning() throws Exception {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredVirtualThreadExecutor(meterRegistry, Tags.of("kind", "test_kind", "name", "TestService"));

    // Saturate carrier threads with CPU-bound work so new virtual threads can't start immediately.
    // The carrier pool defaults to availableProcessors() threads.
    int carrierCount = Runtime.getRuntime().availableProcessors();
    CountDownLatch saturatorsStarted = new CountDownLatch(carrierCount);
    CountDownLatch saturatorsCanStop = new CountDownLatch(1);

    // Submit CPU-busy tasks that pin carriers (busy-wait, no unmount points)
    for (int i = 0; i < carrierCount; i++) {
      executor.execute(() -> {
        saturatorsStarted.countDown();
        // Busy-wait to keep the carrier occupied without unmounting
        while (saturatorsCanStop.getCount() > 0) {
          Thread.onSpinWait();
        }
      });
    }
    assertThat(saturatorsStarted.await(5, TimeUnit.SECONDS)).isTrue();

    // Now submit additional tasks — these should be "queued" waiting for a carrier
    int extraTasks = 5;
    CountDownLatch extraStarted = new CountDownLatch(extraTasks);
    for (int i = 0; i < extraTasks; i++) {
      executor.execute(extraStarted::countDown);
    }

    Gauge queuedGauge = meterRegistry.find("executor.queued")
        .tag("kind", "test_kind")
        .tag("name", "TestService")
        .gauge();
    assertThat(queuedGauge).isNotNull();
    // The extra tasks should be queued (waiting for carrier). Use await() because the
    // queuedTaskCount is incremented in InstrumentedTask constructor (on submitting thread)
    // but a carrier could theoretically become available and start running a task before we assert.
    await().atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertThat(queuedGauge.value()).isGreaterThanOrEqualTo(1.0));

    // Release the saturators
    saturatorsCanStop.countDown();

    // All extra tasks should now run
    assertThat(extraStarted.await(5, TimeUnit.SECONDS)).isTrue();

    await().atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertThat(queuedGauge.value()).isEqualTo(0.0));
  }

  @Test
  public void testIdleTimerRecordsWaitForCarrier() throws Exception {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredVirtualThreadExecutor(meterRegistry, Tags.of("kind", "test_kind", "name", "TestService"));

    // Saturate carrier threads so submitted tasks must wait
    int carrierCount = Runtime.getRuntime().availableProcessors();
    CountDownLatch saturatorsStarted = new CountDownLatch(carrierCount);
    CountDownLatch saturatorsCanStop = new CountDownLatch(1);

    for (int i = 0; i < carrierCount; i++) {
      executor.execute(() -> {
        saturatorsStarted.countDown();
        while (saturatorsCanStop.getCount() > 0) {
          Thread.onSpinWait();
        }
      });
    }
    assertThat(saturatorsStarted.await(5, TimeUnit.SECONDS)).isTrue();

    // Submit a task that will be delayed waiting for a carrier
    CountDownLatch taskDone = new CountDownLatch(1);
    executor.execute(taskDone::countDown);

    // Hold carriers busy for a measurable period
    Thread.sleep(100);
    saturatorsCanStop.countDown();

    assertThat(taskDone.await(5, TimeUnit.SECONDS)).isTrue();

    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      Timer idleTimer = meterRegistry.find("executor.idle")
          .tag("kind", "test_kind")
          .tag("name", "TestService")
          .timer();
      assertThat(idleTimer).isNotNull();
      // carrierCount saturator tasks + 1 delayed task
      assertThat(idleTimer.count()).isEqualTo(carrierCount + 1);
      // The delayed task waited at least ~100ms for a carrier
      assertThat(idleTimer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(50);
    });
  }

  @Test
  public void testPerTaskTagsArePropagated() throws Exception {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredVirtualThreadExecutor(meterRegistry, Tags.of("kind", "test_kind", "name", "TestService"));

    CountDownLatch done = new CountDownLatch(1);
    TaggedRunnable taggedTask = new TaggedRunnable(done::countDown, Tags.of("task", "special"));
    executor.execute(taggedTask);

    assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();

    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      Counter completedCounter = meterRegistry.find("executor.completed")
          .tag("kind", "test_kind")
          .tag("name", "TestService")
          .tag("task", "special")
          .counter();
      assertThat(completedCounter).isNotNull();
      assertThat(completedCounter.count()).isEqualTo(1.0);
    });
  }
}
