/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.common.metering;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.annotation.Nullable;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.BaseUnits;

/**
 * An {@link ExecutorService} backed by virtual threads with Micrometer metrics instrumentation.
 * <p>
 * This is the virtual-thread equivalent of {@link MeteredThreadPoolExecutor}. Since virtual threads
 * have no bounded pool, the following pool-specific metrics are not applicable:
 * <ul>
 * <li>{@code executor.queue.remaining} — no bounded queue capacity</li>
 * <li>{@code executor.pool.size}, {@code executor.pool.core}, {@code executor.pool.max} — no pool</li>
 * <li>{@code executor.rejected} — virtual threads never reject tasks</li>
 * </ul>
 * <p>
 * Metrics that are retained or adapted:
 * <ul>
 * <li>{@code executor.queued} — tasks submitted but not yet running on a carrier thread</li>
 * <li>{@code executor.active} — number of currently-executing tasks (mounted on a carrier)</li>
 * <li>{@code executor.idle} — time tasks spent waiting for a carrier thread before execution</li>
 * <li>{@code executor} (timer) — task execution duration</li>
 * <li>{@code executor.completed} — total completed tasks</li>
 * <li>{@code executor.failed} — total failed tasks</li>
 * </ul>
 */
public class MeteredVirtualThreadExecutor
    extends WrappingForwardingExecutorService
{
  public static final String KIND_TAG = "kind";

  public static final String NAME_TAG = "name";

  private final ExecutorService delegate;

  private final MeterRegistry meterRegistry;

  private final Tags tags;

  private final Set<Id> registeredMeterIds;

  private final AtomicInteger activeTaskCount = new AtomicInteger(0);

  private final AtomicInteger queuedTaskCount = new AtomicInteger(0);

  public MeteredVirtualThreadExecutor(@Nullable final MeterRegistry meterRegistry, final Tags tags) {
    this.delegate = Executors.newVirtualThreadPerTaskExecutor();
    this.meterRegistry = meterRegistry;
    this.tags = tags == null ? Tags.empty() : tags;

    if (this.meterRegistry == null || Tags.empty().equals(this.tags)) {
      registeredMeterIds = Collections.emptySet();
    }
    else {
      Gauge activeThreadsGauge = Gauge.builder("executor.active", this, e -> e.getActiveTaskCount())
          .tags(this.tags)
          .description("The approximate number of tasks that are actively executing on a carrier thread")
          .baseUnit(BaseUnits.TASKS)
          .register(this.meterRegistry);

      Gauge queuedTasksGauge = Gauge.builder("executor.queued", this, e -> e.getQueuedTaskCount())
          .tags(this.tags)
          .description("The approximate number of tasks submitted but waiting for a carrier thread")
          .baseUnit(BaseUnits.TASKS)
          .register(this.meterRegistry);

      // ConcurrentHashMap-backed set because TimedRunnable/TimedCallable constructors
      // add meter IDs from arbitrary virtual threads concurrently with shutdown() iterating.
      registeredMeterIds = ConcurrentHashMap.newKeySet();
      registeredMeterIds.add(activeThreadsGauge.getId());
      registeredMeterIds.add(queuedTasksGauge.getId());
    }
  }

  @Override
  protected ExecutorService delegate() {
    return delegate;
  }

  // Visible for testing and subclasses
  protected int getActiveTaskCount() {
    return activeTaskCount.get();
  }

  // Visible for testing and subclasses
  protected int getQueuedTaskCount() {
    return queuedTaskCount.get();
  }

  @Override
  protected Runnable wrapTask(Runnable task) {
    return new TimedRunnable(task);
  }

  @Override
  protected <T> Callable<T> wrapTask(Callable<T> task) {
    return new TimedCallable<>(task);
  }

  @Override
  public void shutdown() {
    deregisterMeters();
    super.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    deregisterMeters();
    return super.shutdownNow();
  }

  private void deregisterMeters() {
    if (meterRegistry != null) {
      for (Meter.Id id : registeredMeterIds) {
        meterRegistry.remove(id);
      }
    }
  }

  private abstract class InstrumentedTask<R>
  {
    private final long submitTime;

    private final Timer idleTaskTimer;

    private final Timer runningTaskTimer;

    private final Counter failedTaskCounter;

    private final Counter completedTaskCounter;

    InstrumentedTask(final Object task) {
      queuedTaskCount.incrementAndGet();
      this.submitTime = System.nanoTime();
      if (meterRegistry == null || Tags.empty().equals(MeteredVirtualThreadExecutor.this.tags)) {
        idleTaskTimer = null;
        runningTaskTimer = null;
        failedTaskCounter = null;
        completedTaskCounter = null;
      }
      else {
        Tags taskTags = tags;
        if (task instanceof HasTags hasTags) {
          Tags extra = hasTags.getTags();
          if (extra != null) {
            taskTags = Tags.concat(tags, extra);
          }
        }

        idleTaskTimer = Timer.builder("executor.idle")
            .tags(taskTags)
            .description("Time tasks spent waiting for a carrier thread before execution")
            .register(meterRegistry);

        runningTaskTimer = Timer.builder("executor")
            .tags(taskTags)
            .description("Time tasks spent executing")
            .register(meterRegistry);

        failedTaskCounter = Counter.builder("executor.failed")
            .tags(taskTags)
            .description("The total number of tasks that have erroneously completed execution")
            .baseUnit(BaseUnits.TASKS)
            .register(meterRegistry);

        completedTaskCounter = Counter.builder("executor.completed")
            .tags(taskTags)
            .description("The total number of tasks that have completed execution")
            .baseUnit(BaseUnits.TASKS)
            .register(meterRegistry);

        registeredMeterIds.addAll(Set.of(
            idleTaskTimer.getId(),
            runningTaskTimer.getId(),
            failedTaskCounter.getId(),
            completedTaskCounter.getId()));
      }
    }

    private long executionStartTime;

    protected abstract R doExecute() throws Exception;

    protected R execute() throws Exception {
      long idleTime = System.nanoTime() - submitTime;
      queuedTaskCount.decrementAndGet();
      if (idleTaskTimer != null) {
        idleTaskTimer.record(idleTime, TimeUnit.NANOSECONDS);
      }
      activeTaskCount.incrementAndGet();
      executionStartTime = System.nanoTime();
      try {
        return doExecute();
      }
      catch (Throwable t) {
        if (failedTaskCounter != null) {
          failedTaskCounter.increment();
        }
        throw t;
      }
      finally {
        long executionTime = System.nanoTime() - executionStartTime;
        if (runningTaskTimer != null) {
          runningTaskTimer.record(executionTime, TimeUnit.NANOSECONDS);
        }
        if (completedTaskCounter != null) {
          completedTaskCounter.increment();
        }
        activeTaskCount.decrementAndGet();
      }
    }
  }

  private class TimedRunnable
      extends InstrumentedTask<Void>
      implements Runnable
  {
    private final Runnable runnable;

    TimedRunnable(final Runnable runnable) {
      super(runnable);
      this.runnable = runnable;
    }

    @Override
    protected Void doExecute() {
      runnable.run();
      return null;
    }

    @Override
    public void run() {
      try {
        execute();
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private class TimedCallable<T>
      extends InstrumentedTask<T>
      implements Callable<T>
  {
    private final Callable<T> callable;

    TimedCallable(final Callable<T> callable) {
      super(callable);
      this.callable = callable;
    }

    @Override
    protected T doExecute() throws Exception {
      return callable.call();
    }

    @Override
    public T call() throws Exception {
      return execute();
    }
  }
}
