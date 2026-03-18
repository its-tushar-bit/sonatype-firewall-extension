/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.eventbus;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;

import com.google.common.eventbus.TenantAwareEventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncEventBusImpl
    implements AsyncEventBus
{
  private static final Logger log = LoggerFactory.getLogger(AsyncEventBusImpl.class);

  private final ThreadPoolExecutor threadPoolExecutor;

  private final com.google.common.eventbus.EventBus delegate;

  public AsyncEventBusImpl(int maxPoolSize) {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true)
        .setNameFormat("AsyncEventBusThread-%d")
        .build();

    threadPoolExecutor =
        new TenantThreadPoolExecutor(0, maxPoolSize, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory,
            new AsyncEventBusDiscardPolicy(), "async_event_bus", "AsyncEventBusImpl");

    delegate = new TenantAwareEventBus(threadPoolExecutor, new AsyncEventBusExceptionHandler());
  }

  @Override
  public void register(final Object handler) {
    delegate.register(handler);
    log.debug("Registered async handler: {}", handler);
  }

  @Override
  public void unregister(final Object handler) {
    delegate.unregister(handler);
    log.debug("Unregistered async handler: {}", handler);
  }

  @Override
  public void post(final Object event) {
    log.debug("AsyncEvent '{}' fired", event);
    delegate.post(event);
  }

  @Override
  public int getMaxPoolSize() {
    return threadPoolExecutor.getMaximumPoolSize();
  }

  @Override
  public void setMaxPoolSize(int maxPoolSize) {
    threadPoolExecutor.setMaximumPoolSize(maxPoolSize);
  }

  @Override
  public ThreadPoolExecutor getThreadPoolExecutor() {
    return threadPoolExecutor;
  }
}
