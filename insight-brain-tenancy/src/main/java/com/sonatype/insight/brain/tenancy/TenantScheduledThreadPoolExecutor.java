/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import org.jetbrains.annotations.NotNull;

public class TenantScheduledThreadPoolExecutor
    extends ScheduledThreadPoolExecutor
{
  public TenantScheduledThreadPoolExecutor(
      int corePoolSize,
      @NotNull ThreadFactory threadFactory)
  {
    super(corePoolSize, threadFactory);
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
}
