/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

/**
 * This class is intended for tenant-specific thread pools in which only one tenant (the tenant on the thread that
 * created it) will use. This class must not be used for thread pools that would service more than one tenant.
 */
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

  /**
   * scheduleAtFixedRate This creates a new TenantAwareReusableRunnable. That means this tenant can be re-cloned by
   * the scheduler for each use.
   */
  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    return super.scheduleAtFixedRate(new TenantAwareReusableRunnable(command), initialDelay, period, unit);
  }

  /**
   * This is a reusable version of TenantAwareOneTimeRunnable. It clones the tenant so the lifecycle of the
   * tenant is managed here.
   */
  private class TenantAwareReusableRunnable
      implements Runnable
  {
    private final Runnable wrapped;

    private final Tenant reusableTenant;

    public TenantAwareReusableRunnable(Runnable wrapped) {
      this(wrapped, TenantThreadLocal.getTenant());
    }

    TenantAwareReusableRunnable(Runnable wrapped, Tenant tenant) {
      this.wrapped = wrapped;
      // first clone the tenant so that we get the threadlocal checks on it to make sure it's safe to use here
      Tenant clonedTenant = TenantThreadLocal.cloneTenant(tenant);
      reusableTenant = new ReusableTenant(clonedTenant.tenantSlug);
    }

    @Override
    public void run() {
      TenantThreadLocal.runAsWithoutValidation(reusableTenant, (Supplier<Void>) () -> {
        wrapped.run();
        return null;
      });
    }

    /**
     * This class is private as it's only intended to be used in this context where we know that the scheduled
     * executor is ALWAYS invoked in the context of the given tenant. In other contexts where the tenant on a
     * thread can change we would never allow or use a reusable tenant.
     */
    private class ReusableTenant
        extends Tenant
    {
      ReusableTenant(final String tenantSlug) {
        super(tenantSlug);
      }

      @Override
      void invalidate() {
        // no-op: don't want to invalidate the reusable tenant
      }
    }
  }
}
