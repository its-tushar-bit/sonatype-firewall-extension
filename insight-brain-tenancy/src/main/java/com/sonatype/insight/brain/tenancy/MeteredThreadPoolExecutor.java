/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.BaseUnits;

public class MeteredThreadPoolExecutor
    extends ThreadPoolExecutor
{
  @Inject
  @Nullable
  private static MeterRegistry injectedMeterRegistry;

  public static final String KIND_TAG = "kind";

  public static final String NAME_TAG = "name";

  private final MeterRegistry meterRegistry;

  private final Tags tags;

  private final Set<Id> registeredMeterIds;

  public MeteredThreadPoolExecutor(
      final int corePoolSize,
      final int maximumPoolSize,
      final long keepAliveTime,
      final TimeUnit unit,
      final BlockingQueue<Runnable> workQueue,
      final ThreadFactory threadFactory,
      final RejectedExecutionHandler handler,
      final Tags tags)
  {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler, injectedMeterRegistry,
        tags);
  }

  public MeteredThreadPoolExecutor(
      final int corePoolSize,
      final int maximumPoolSize,
      final long keepAliveTime,
      final TimeUnit unit,
      final BlockingQueue<Runnable> workQueue,
      final ThreadFactory threadFactory,
      final RejectedExecutionHandler handler,
      final MeterRegistry meterRegistry,
      final Tags tags)
  {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    this.meterRegistry = meterRegistry;
    this.tags = tags == null ? Tags.empty() : tags;

    if (this.meterRegistry == null || Tags.empty().equals(this.tags)) {
      registeredMeterIds = Collections.emptySet();
    }
    else {
      Gauge activeThreadsGauge = Gauge.builder("executor.active", this, ThreadPoolExecutor::getActiveCount)
          .tags(this.tags)
          .description("The approximate number of threads that are actively executing tasks")
          .baseUnit(BaseUnits.THREADS)
          .register(this.meterRegistry);

      Gauge queuedTasksGauge =
          Gauge.builder("executor.queued", this, threadPoolExecutor -> threadPoolExecutor.getQueue().size())
              .tags(this.tags)
              .description("The approximate number of tasks that are queued for execution")
              .baseUnit(BaseUnits.TASKS)
              .register(this.meterRegistry);

      Gauge queueRemainingForTasksGauge = Gauge
          .builder("executor.queue.remaining", this,
              threadPoolExecutor -> threadPoolExecutor.getQueue().remainingCapacity())
          .tags(this.tags)
          .description("The number of additional elements that this queue can ideally accept without blocking")
          .baseUnit(BaseUnits.TASKS)
          .register(this.meterRegistry);

      Gauge executorPoolSize = Gauge.builder("executor.pool.size", this, ThreadPoolExecutor::getPoolSize)
          .tags(this.tags)
          .description("The current number of threads in the pool")
          .baseUnit(BaseUnits.THREADS)
          .register(this.meterRegistry);

      Gauge executorPoolCore = Gauge.builder("executor.pool.core", this, ThreadPoolExecutor::getCorePoolSize)
          .tags(this.tags)
          .description("The core number of threads in the pool")
          .baseUnit(BaseUnits.THREADS)
          .register(this.meterRegistry);

      Gauge executorPoolMax = Gauge.builder("executor.pool.max", this, ThreadPoolExecutor::getMaximumPoolSize)
          .tags(this.tags)
          .description("The maximum allowed number of threads in the pool")
          .baseUnit(BaseUnits.THREADS)
          .register(this.meterRegistry);

      registeredMeterIds = new HashSet<>(
          Set.of(
              activeThreadsGauge.getId(),
              queuedTasksGauge.getId(),
              queueRemainingForTasksGauge.getId(),
              executorPoolSize.getId(),
              executorPoolCore.getId(),
              executorPoolMax.getId()));
    }
  }

  @Override
  public void shutdown() {
    for (Meter.Id id : registeredMeterIds) {
      meterRegistry.remove(id);
    }
    super.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    for (Meter.Id id : registeredMeterIds) {
      meterRegistry.remove(id);
    }
    return super.shutdownNow();
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable, final T value) {
    RunnableFuture<T> result = super.newTaskFor(runnable, value);
    if (runnable instanceof HasTags hasTags) {
      return new TaggedRunnableFuture<>(result, hasTags.getTags());
    }
    return result;
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(final Callable<T> callable) {
    RunnableFuture<T> result = super.newTaskFor(callable);
    if (callable instanceof HasTags hasTags) {
      return new TaggedRunnableFuture<>(result, hasTags.getTags());
    }
    return result;
  }

  @Override
  public void execute(final Runnable runnable) {
    Tags tags = Tags.concat(this.tags, getTags(runnable));
    try {
      super.execute(new TimedRunnable(runnable, tags));
    }
    catch (RejectedExecutionException e) {
      Optional.ofNullable(meterRegistry).ifPresent(meterRegistry -> {
        Counter rejectedTaskCounter = Counter.builder("executor.rejected")
            .tags(tags)
            .description("The total number of tasks that have been rejected by the executor")
            .baseUnit(BaseUnits.TASKS)
            .register(meterRegistry);
        registeredMeterIds.add(rejectedTaskCounter.getId());
        rejectedTaskCounter.increment();
      });
      throw e;
    }
  }

  private class TimedRunnable
      implements Runnable
  {
    private final Runnable runnable;

    private final long submitTime;

    private final Timer idleTaskTimer;

    private final Timer runningTaskTimer;

    private final Counter failedTaskCounter;

    private final Counter completedTaskCounter;

    TimedRunnable(final Runnable runnable, final Tags tags) {
      this.runnable = runnable;
      submitTime = System.nanoTime();
      if (meterRegistry == null || Tags.empty().equals(MeteredThreadPoolExecutor.this.tags)) {
        idleTaskTimer = null;
        runningTaskTimer = null;
        failedTaskCounter = null;
        completedTaskCounter = null;
      }
      else {
        idleTaskTimer = Timer.builder("executor.idle")
            .tags(tags)
            .description("Time tasks spent waiting in the queue before execution")
            .register(meterRegistry);

        runningTaskTimer = Timer.builder("executor")
            .tags(tags)
            .description("Time tasks spent executing")
            .register(meterRegistry);

        failedTaskCounter = Counter.builder("executor.failed")
            .tags(tags)
            .description("The total number of tasks that have erroneously completed execution")
            .baseUnit(BaseUnits.TASKS)
            .register(meterRegistry);

        completedTaskCounter = Counter.builder("executor.completed")
            .tags(tags)
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

    @Override
    public void run() {
      long idleTime = System.nanoTime() - submitTime;
      Optional.ofNullable(idleTaskTimer).ifPresent(timer -> timer.record(idleTime, TimeUnit.NANOSECONDS));
      long executionStartTime = System.nanoTime();
      try {
        runnable.run();
      }
      catch (Throwable t) {
        Optional.ofNullable(failedTaskCounter).ifPresent(Counter::increment);
        throw t;
      }
      finally {
        long executionTime = System.nanoTime() - executionStartTime;
        Optional.ofNullable(runningTaskTimer).ifPresent(timer -> timer.record(executionTime, TimeUnit.NANOSECONDS));
        Optional.ofNullable(completedTaskCounter).ifPresent(Counter::increment);
      }
    }
  }

  private Tags getTags(final Runnable runnable) {
    if (runnable instanceof HasTags hasTags) {
      Tags tags = hasTags.getTags();
      if (tags != null) {
        return tags;
      }
    }
    return Tags.empty();
  }
}
