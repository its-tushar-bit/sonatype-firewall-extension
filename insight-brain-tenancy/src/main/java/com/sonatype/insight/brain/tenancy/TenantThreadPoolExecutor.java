/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import com.sonatype.insight.brain.common.metering.MeteredThreadPoolExecutor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Tags;
import org.jetbrains.annotations.NotNull;

public class TenantThreadPoolExecutor
    extends MeteredThreadPoolExecutor
{
  public TenantThreadPoolExecutor(
      int corePoolSize,
      int maximumPoolSize,
      long keepAliveTime,
      TimeUnit unit,
      BlockingQueue<Runnable> workQueue,
      ThreadFactory threadFactory,
      RejectedExecutionHandler handler,
      String kind,
      String name)
  {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler,
        Tags.of(KIND_TAG, kind, NAME_TAG, name));
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return super.submit(new TenantAwareOneTimeCallable<>(task));
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return super.submit(new TenantAwareOneTimeRunnable(task), result);
  }

  @Override
  public Future<?> submit(Runnable task) {
    return super.submit(new TenantAwareOneTimeRunnable(task));
  }

  @Override
  public void execute(Runnable task) {
    super.execute(new TenantAwareOneTimeRunnable(task));
  }

  @Override
  public <T> List<Future<T>> invokeAll(
      @NotNull final Collection<? extends Callable<T>> tasks) throws InterruptedException
  {
    List<TenantAwareOneTimeCallable<T>> wrappedTasks = tasks.stream()
        .map(TenantAwareOneTimeCallable::new)
        .collect(Collectors.toList());
    return super.invokeAll(wrappedTasks);
  }
}
