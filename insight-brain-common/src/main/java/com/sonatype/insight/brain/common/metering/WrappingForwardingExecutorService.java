/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.common.metering;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.ForwardingExecutorService;

/**
 * A {@link ForwardingExecutorService} that wraps every submitted task before delegating.
 * <p>
 * Subclasses implement {@link #wrapTask(Runnable)} and {@link #wrapTask(Callable)} to define
 * the wrapping behavior. All {@code execute}, {@code submit}, {@code invokeAll}, and
 * {@code invokeAny} methods route through these two methods.
 */
public abstract class WrappingForwardingExecutorService
    extends ForwardingExecutorService
{
  protected abstract Runnable wrapTask(Runnable task);

  protected abstract <T> Callable<T> wrapTask(Callable<T> task);

  @Override
  public void execute(Runnable command) {
    delegate().execute(wrapTask(command));
  }

  @Override
  public Future<?> submit(Runnable task) {
    return delegate().submit(wrapTask(task));
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return delegate().submit(wrapTask(task), result);
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return delegate().submit(wrapTask(task));
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return delegate().invokeAll(wrapCallables(tasks));
  }

  @Override
  public <T> List<Future<T>> invokeAll(
      Collection<? extends Callable<T>> tasks,
      long timeout,
      TimeUnit unit) throws InterruptedException
  {
    return delegate().invokeAll(wrapCallables(tasks), timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return delegate().invokeAny(wrapCallables(tasks));
  }

  @Override
  public <T> T invokeAny(
      Collection<? extends Callable<T>> tasks,
      long timeout,
      TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
  {
    return delegate().invokeAny(wrapCallables(tasks), timeout, unit);
  }

  private <T> List<Callable<T>> wrapCallables(Collection<? extends Callable<T>> tasks) {
    return tasks.stream().map(this::wrapTask).toList();
  }
}
