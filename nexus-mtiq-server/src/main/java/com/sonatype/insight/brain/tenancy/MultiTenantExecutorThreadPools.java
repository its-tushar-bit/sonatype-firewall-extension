/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;

import com.sonatype.insight.brain.utils.DefaultExecutorThreadPools;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.springframework.context.annotation.Primary;

@Named
@Singleton
@Primary
public class MultiTenantExecutorThreadPools
    extends DefaultExecutorThreadPools
{
  @Override
  protected ForkJoinPool namedForkJoinPool(int threadCount, String namePrefix) {
    final ForkJoinWorkerThreadFactory factory = pool -> {
      final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
      worker.setName(namePrefix + worker.getPoolIndex());
      return worker;
    };

    return new TenantForkJoinPool(threadCount, factory);
  }

  /**
   * Handles passing the correct tenant to the executing runnable/callable. Prevents the use of ForkJoinTasks as they
   * can span multiple threads which means the wrong tenant could be picked up from the InheritableThreadLocal that is
   * used to track the current tenant.
   */
  private static class TenantForkJoinPool
      extends ForkJoinPool
  {
    public TenantForkJoinPool(
        int parallelism,
        ForkJoinWorkerThreadFactory factory)
    {
      super(parallelism, factory, null, false);
    }

    @Override
    public <T> ForkJoinTask<T> submit(Callable<T> task) {
      return super.submit(new TenantAwareOneTimeCallable<>(task));
    }

    @Override
    public <T> ForkJoinTask<T> submit(Runnable task, T result) {
      return super.submit(new TenantAwareOneTimeRunnable(task), result);
    }

    @Override
    public ForkJoinTask<?> submit(Runnable task) {
      return super.submit(new TenantAwareOneTimeRunnable(task));
    }

    @Override
    public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task) {
      throw new RuntimeException("ForkJoinTasks are not supported in a multi-tenant environment");
    }

    @Override
    public void execute(Runnable task) {
      super.execute(new TenantAwareOneTimeRunnable(task));
    }

    @Override
    public void execute(ForkJoinTask<?> task) {
      throw new RuntimeException("ForkJoinTasks are not supported in a multi-tenant environment");
    }
  }
}
