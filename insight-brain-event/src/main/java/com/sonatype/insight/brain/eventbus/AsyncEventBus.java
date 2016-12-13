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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class AsyncEventBus
{
  private static final Logger log = LoggerFactory.getLogger(AsyncEventBus.class);

  private final com.google.common.eventbus.AsyncEventBus delegate;

  @Inject
  public AsyncEventBus(final EventBusConfig eventBusConfig) {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("AsyncEventBusThread-%d")
        .build();

    ThreadPoolExecutor threadPool = new ThreadPoolExecutor(0, eventBusConfig.getMaxPoolSize(), 60L, TimeUnit.SECONDS,
        new SynchronousQueue<Runnable>(), threadFactory, new AsyncEventBusDiscardPolicy());

    delegate = new com.google.common.eventbus.AsyncEventBus(threadPool, new AsyncEventBusExceptionHandler());
  }

  public void register(final Object handler) {
    delegate.register(handler);
    log.debug("Registered async handler: {}", handler);
  }

  public void unregister(final Object handler) {
    delegate.unregister(handler);
    log.debug("Unregistered async handler: {}", handler);
  }

  public void post(final Object event) {
    log.debug("AsyncEvent '{}' fired", event);
    delegate.post(event);
  }
}
