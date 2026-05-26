/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import com.sonatype.insight.brain.common.metering.MeteredVirtualThreadExecutor;

import java.util.concurrent.Callable;
import jakarta.annotation.Nullable;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

/**
 * A virtual-thread {@link java.util.concurrent.ExecutorService} that propagates tenant context and
 * Shiro subject to executing tasks.
 * <p>
 * This is the virtual-thread equivalent of {@link TenantThreadPoolExecutor}. All submitted tasks are
 * wrapped in {@link TenantAwareOneTimeRunnable} or {@link TenantAwareOneTimeCallable} to ensure
 * tenant isolation in multi-tenant deployments.
 */
public class TenantVirtualThreadExecutor
    extends MeteredVirtualThreadExecutor
{
  public TenantVirtualThreadExecutor(@Nullable MeterRegistry meterRegistry, String kind, String name) {
    super(meterRegistry, Tags.of(KIND_TAG, kind, NAME_TAG, name));
  }

  @Override
  protected Runnable wrapTask(Runnable task) {
    return super.wrapTask(new TenantAwareOneTimeRunnable(task));
  }

  @Override
  protected <T> Callable<T> wrapTask(Callable<T> task) {
    return super.wrapTask(new TenantAwareOneTimeCallable<>(task));
  }
}
