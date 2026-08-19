/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import com.sonatype.insight.brain.common.metering.MeteredThreadPoolExecutor;
import com.sonatype.insight.brain.common.metering.TaggedRunnable;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;

public class MeteredThreadPoolExecutorTest
{
  private MeteredThreadPoolExecutor executor;

  @AfterEach
  public void after() {
    if (executor != null) {
      executor.shutdown();
      executor = null;
    }
  }

  @Test
  public void testMonitoredThreadPoolExecutor_ExecutorIdle() throws Exception {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(Integer.MAX_VALUE), Executors.defaultThreadFactory(), new AbortPolicy(),
        meterRegistry,
        Tags.of("kind", "some_kind", "name", "SomeService"));

    ManagedRunnable managedRunnable = new ManagedRunnable();
    executor.submit(managedRunnable);
    managedRunnable.waitUntilStarted();
    // this next task won't start until the other is stopped because core and max pool sizes are 1
    // meaning idle time is accumulating for it
    executor.submit(() -> {
    });
    Thread.sleep(1000);
    managedRunnable.stop();
    await().atMost(Duration.ofSeconds(5)).until(() -> executor.getActiveCount() == 0);
    Timer idle = meterRegistry.find("executor.idle").timer();
    assertThat(idle).isNotNull();
    assertThat(idle.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(1000);
  }

  @Test
  public void testMonitoredThreadPoolExecutor_Executor() throws Exception {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(Integer.MAX_VALUE), Executors.defaultThreadFactory(), new AbortPolicy(),
        meterRegistry,
        Tags.of("kind", "some_kind", "name", "SomeService"));
    executor.submit(() -> {
    }).get();

    Timer running = meterRegistry.find("executor").timer();
    assertThat(running).isNotNull();
    assertThat(running.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(0);
    ManagedRunnable managedRunnable = new ManagedRunnable();
    executor.submit(managedRunnable);
    managedRunnable.waitUntilStarted();
    Thread.sleep(1000);
    managedRunnable.stop();
    await().atMost(Duration.ofSeconds(5)).until(() -> executor.getActiveCount() == 0);
    assertThat(running.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(1000);
  }

  @Test
  public void testMonitoredThreadPoolExecutor_ExecutorRejected() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredThreadPoolExecutor(0, 1, 0, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1), Executors.defaultThreadFactory(), new AbortPolicy(), meterRegistry,
        Tags.of("kind", "some_kind", "name", "SomeService"));

    ManagedRunnable managedRunnable = new ManagedRunnable();
    executor.submit(managedRunnable);
    managedRunnable.waitUntilStarted();
    executor.submit(() -> {
    });
    assertThatExceptionOfType(RejectedExecutionException.class).isThrownBy(() -> executor.submit(() -> {
    }));
    Counter rejected = meterRegistry.find("executor.rejected").counter();
    assertThat(rejected).isNotNull();
    assertThat(rejected.count()).isOne();
  }

  @Test
  public void testMonitoredThreadPoolExecutor_ExecutorRejectedWithEmptyTags() {
    // With a non-null registry but empty tags, meters are off and registeredMeterIds is immutable;
    // a rejection must still surface as RejectedExecutionException, not UnsupportedOperationException.
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredThreadPoolExecutor(0, 1, 0, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1), Executors.defaultThreadFactory(), new AbortPolicy(), meterRegistry,
        Tags.empty());

    ManagedRunnable managedRunnable = new ManagedRunnable();
    executor.submit(managedRunnable);
    managedRunnable.waitUntilStarted();
    executor.submit(() -> {
    });
    assertThatExceptionOfType(RejectedExecutionException.class).isThrownBy(() -> executor.submit(() -> {
    }));
    assertThat(meterRegistry.find("executor.rejected").counter()).isNull();
  }

  @Test
  public void testMonitoredThreadPoolExecutor_ExecutorCompleted() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredThreadPoolExecutor(0, 1, 0, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1), Executors.defaultThreadFactory(), new AbortPolicy(), meterRegistry,
        Tags.of("kind", "some_kind", "name", "SomeService"));

    ManagedRunnable managedRunnable1 = new ManagedRunnable();
    executor.submit(managedRunnable1);
    managedRunnable1.waitUntilStarted();
    ManagedRunnable managedRunnable2 = new ManagedRunnable();
    executor.submit(managedRunnable2);
    assertThatExceptionOfType(RejectedExecutionException.class).isThrownBy(() -> executor.submit(() -> {
    }));
    Counter completed = meterRegistry.find("executor.completed").counter();
    assertThat(completed).isNotNull();
    assertThat(completed.count()).isZero();
    managedRunnable1.stop();
    managedRunnable2.waitUntilStarted();
    assertThat(completed.count()).isOne();
    managedRunnable2.stop();
    await().atMost(Duration.ofSeconds(5)).until(() -> executor.getActiveCount() == 0);
    assertThat(completed.count()).isEqualTo(2);
  }

  @Test
  public void testMonitoredThreadPoolExecutor_NullMeterRegistry() {
    executor = new MeteredThreadPoolExecutor(0, 1, 0, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1), Executors.defaultThreadFactory(), new AbortPolicy(), null,
        Tags.of("kind", "some_kind", "name", "SomeService"));

    ManagedRunnable managedRunnable1 = new ManagedRunnable();
    executor.submit(managedRunnable1);
    managedRunnable1.waitUntilStarted();
    ManagedRunnable managedRunnable2 = new ManagedRunnable();
    executor.submit(managedRunnable2);
    ManagedRunnable managedRunnable3 = new ManagedRunnable();
    assertThatExceptionOfType(RejectedExecutionException.class).isThrownBy(
        () -> executor.submit(managedRunnable3));
    managedRunnable1.stop();
    managedRunnable2.waitUntilStarted();
    managedRunnable2.stop();
    await().atMost(Duration.ofSeconds(5)).until(() -> executor.getActiveCount() == 0);
  }

  @Test
  public void testMonitoredThreadPoolExecutor_NullTags() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredThreadPoolExecutor(0, 1, 0, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(Integer.MAX_VALUE), Executors.defaultThreadFactory(), new AbortPolicy(),
        meterRegistry,
        null);

    executor.submit(() -> {
    });

    await().atMost(Duration.ofSeconds(5)).until(() -> executor.getActiveCount() == 0);

    assertThat(meterRegistry.getMeters()).isEmpty();
  }

  @Test
  public void testMonitoredThreadPoolExecutor_ExecutorQueued() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredThreadPoolExecutor(0, 1, 0, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(Integer.MAX_VALUE), Executors.defaultThreadFactory(), new AbortPolicy(),
        meterRegistry,
        Tags.of("kind", "some_kind", "name", "SomeService"));

    Gauge queued = meterRegistry.find("executor.queued").gauge();
    assertThat(queued).isNotNull();
    assertThat(queued.value()).isZero();
    ManagedRunnable managedRunnable1 = new ManagedRunnable();
    executor.submit(managedRunnable1);
    managedRunnable1.waitUntilStarted();
    ManagedRunnable managedRunnable2 = new ManagedRunnable();
    executor.submit(managedRunnable2);
    assertThat(queued.value()).isOne();
    managedRunnable1.stop();
    managedRunnable2.waitUntilStarted();
    assertThat(queued.value()).isZero();
    managedRunnable2.stop();
  }

  @Test
  public void testMonitoredThreadPoolExecutor_ExecutorQueueRemaining() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredThreadPoolExecutor(0, 1, 0, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(2), Executors.defaultThreadFactory(), new AbortPolicy(), meterRegistry,
        Tags.of("kind", "some_kind", "name", "SomeService"));

    Gauge queueRemaining = meterRegistry.find("executor.queue.remaining").gauge();
    assertThat(queueRemaining).isNotNull();
    assertThat(queueRemaining.value()).isEqualTo(2);
    ManagedRunnable managedRunnable1 = new ManagedRunnable();
    executor.submit(managedRunnable1);
    managedRunnable1.waitUntilStarted();
    ManagedRunnable managedRunnable2 = new ManagedRunnable();
    executor.submit(managedRunnable2);
    assertThat(queueRemaining.value()).isOne();
    managedRunnable1.stop();
    managedRunnable2.waitUntilStarted();
    assertThat(queueRemaining.value()).isEqualTo(2);
    managedRunnable2.stop();
  }

  @Test
  public void testMonitoredThreadPoolExecutor_ExecutorPoolSize() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredThreadPoolExecutor(0, 1, 0, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(Integer.MAX_VALUE), Executors.defaultThreadFactory(), new AbortPolicy(),
        meterRegistry,
        Tags.of("kind", "some_kind", "name", "SomeService"));

    Gauge poolSize = meterRegistry.find("executor.pool.size").gauge();
    assertThat(poolSize).isNotNull();
    assertThat(poolSize.value()).isZero();
    ManagedRunnable managedRunnable = new ManagedRunnable();
    executor.submit(managedRunnable);
    managedRunnable.waitUntilStarted();
    assertThat(poolSize.value()).isOne();
    managedRunnable.stop();
    await().atMost(Duration.ofSeconds(5)).until(() -> executor.getActiveCount() == 0);
    assertThat(poolSize.value()).isZero();
  }

  @Test
  public void testMonitoredThreadPoolExecutor_ExecutorPoolCore() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredThreadPoolExecutor(1, 3, 0, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(Integer.MAX_VALUE), Executors.defaultThreadFactory(), new AbortPolicy(),
        meterRegistry,
        Tags.of("kind", "some_kind", "name", "SomeService"));

    Gauge corePoolSize = meterRegistry.find("executor.pool.core").gauge();
    assertThat(corePoolSize).isNotNull();
    assertThat(corePoolSize.value()).isEqualTo(1);
    executor.setCorePoolSize(2);
    assertThat(corePoolSize.value()).isEqualTo(2);
  }

  @Test
  public void testMonitoredThreadPoolExecutor_ExecutorPoolMax() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredThreadPoolExecutor(1, 3, 0, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(Integer.MAX_VALUE), Executors.defaultThreadFactory(), new AbortPolicy(),
        meterRegistry,
        Tags.of("kind", "some_kind", "name", "SomeService"));

    Gauge maxPoolSize = meterRegistry.find("executor.pool.max").gauge();
    assertThat(maxPoolSize).isNotNull();
    assertThat(maxPoolSize.value()).isEqualTo(3);
    executor.setMaximumPoolSize(4);
    assertThat(maxPoolSize.value()).isEqualTo(4);
  }

  @Test
  public void testExecute_Tags() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1), Executors.defaultThreadFactory(), new AbortPolicy(), meterRegistry,
        Tags.of("kind", "some_kind", "name", "SomeService"));
    ManagedRunnable managedRunnable = new ManagedRunnable();
    executor.submit(managedRunnable);
    managedRunnable.waitUntilStarted();
    executor.submit(() -> {
    });
    assertThatExceptionOfType(RejectedExecutionException.class).isThrownBy(() -> executor.submit(() -> {
    }));
    managedRunnable.stop();

    assertThat(meterRegistry.getMeters())
        .extracting(meter -> Tuple.tuple(
            meter.getId().getName(),
            meter.getId()
                .getTags()
                .stream()
                .collect(Collectors.toMap(Tag::getKey, Tag::getValue))))
        .containsExactlyInAnyOrder(
            Tuple.tuple("executor.active", Map.of("kind", "some_kind", "name", "SomeService")),
            Tuple.tuple("executor.queued", Map.of("kind", "some_kind", "name", "SomeService")),
            Tuple.tuple("executor.queue.remaining", Map.of("kind", "some_kind", "name", "SomeService")),
            Tuple.tuple("executor.pool.size", Map.of("kind", "some_kind", "name", "SomeService")),
            Tuple.tuple("executor.pool.core", Map.of("kind", "some_kind", "name", "SomeService")),
            Tuple.tuple("executor.pool.max", Map.of("kind", "some_kind", "name", "SomeService")),
            Tuple.tuple("executor.rejected", Map.of("kind", "some_kind", "name", "SomeService")),
            Tuple.tuple("executor.idle", Map.of("kind", "some_kind", "name", "SomeService")),
            Tuple.tuple("executor", Map.of("kind", "some_kind", "name", "SomeService")),
            Tuple.tuple("executor.failed", Map.of("kind", "some_kind", "name", "SomeService")),
            Tuple.tuple("executor.completed", Map.of("kind", "some_kind", "name", "SomeService")));
  }

  @Test
  public void testExecute_TaggedRunnable_Tags() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1), Executors.defaultThreadFactory(), new AbortPolicy(), meterRegistry,
        Tags.of("kind", "some_kind", "name", "SomeService"));
    ManagedRunnable managedRunnable = new ManagedRunnable();
    executor.submit(new TaggedRunnable(managedRunnable, Tags.of("tag1", "value1", "tag2", "value2")));
    managedRunnable.waitUntilStarted();
    executor.submit(new TaggedRunnable(() -> {
    }, Tags.of("tag1", "value1", "tag2", "value2")));
    assertThatExceptionOfType(RejectedExecutionException.class).isThrownBy(
        () -> executor.submit(new TaggedRunnable(() -> {
        }, Tags.of("tag1", "value1", "tag2", "value2"))));
    managedRunnable.stop();

    assertThat(meterRegistry.getMeters())
        .extracting(meter -> Tuple.tuple(
            meter.getId().getName(),
            meter.getId()
                .getTags()
                .stream()
                .collect(Collectors.toMap(Tag::getKey, Tag::getValue))))
        .containsExactlyInAnyOrder(
            Tuple.tuple("executor.active", Map.of("kind", "some_kind", "name", "SomeService")),
            Tuple.tuple("executor.queued", Map.of("kind", "some_kind", "name", "SomeService")),
            Tuple.tuple("executor.queue.remaining", Map.of("kind", "some_kind", "name", "SomeService")),
            Tuple.tuple("executor.pool.size", Map.of("kind", "some_kind", "name", "SomeService")),
            Tuple.tuple("executor.pool.core", Map.of("kind", "some_kind", "name", "SomeService")),
            Tuple.tuple("executor.pool.max", Map.of("kind", "some_kind", "name", "SomeService")),
            Tuple.tuple("executor.rejected",
                Map.of("kind", "some_kind", "name", "SomeService", "tag1", "value1", "tag2", "value2")),
            Tuple.tuple("executor.idle",
                Map.of("kind", "some_kind", "name", "SomeService", "tag1", "value1", "tag2", "value2")),
            Tuple.tuple("executor",
                Map.of("kind", "some_kind", "name", "SomeService", "tag1", "value1", "tag2", "value2")),
            Tuple.tuple("executor.failed",
                Map.of("kind", "some_kind", "name", "SomeService", "tag1", "value1", "tag2", "value2")),
            Tuple.tuple("executor.completed",
                Map.of("kind", "some_kind", "name", "SomeService", "tag1", "value1", "tag2", "value2")));
  }

  @Test
  public void testShutdown_RemovesRegisteredMeters() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1), Executors.defaultThreadFactory(), new AbortPolicy(), meterRegistry,
        Tags.of("kind", "some_kind", "name", "SomeService"));
    ManagedRunnable managedRunnable = new ManagedRunnable();
    executor.submit(managedRunnable);
    managedRunnable.waitUntilStarted();
    executor.submit(() -> {
    });
    assertThatExceptionOfType(RejectedExecutionException.class).isThrownBy(() -> executor.submit(() -> {
    }));
    managedRunnable.stop();

    assertThat(meterRegistry.getMeters()).hasSize(11);

    executor.shutdown();

    assertThat(meterRegistry.getMeters()).isEmpty();
  }

  @Test
  public void testShutdownNow_RemovesRegisteredMeters() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    executor = new MeteredThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1), Executors.defaultThreadFactory(), new AbortPolicy(), meterRegistry,
        Tags.of("kind", "some_kind", "name", "SomeService"));
    ManagedRunnable managedRunnable = new ManagedRunnable();
    executor.submit(managedRunnable);
    managedRunnable.waitUntilStarted();
    executor.submit(() -> {
    });
    assertThatExceptionOfType(RejectedExecutionException.class).isThrownBy(() -> executor.submit(() -> {
    }));
    managedRunnable.stop();

    assertThat(meterRegistry.getMeters()).hasSize(11);

    executor.shutdownNow();

    assertThat(meterRegistry.getMeters()).isEmpty();
  }
}
