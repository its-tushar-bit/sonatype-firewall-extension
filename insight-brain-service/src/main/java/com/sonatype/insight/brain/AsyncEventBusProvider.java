/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.eventbus.AsyncEventBusImpl;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;

@Named
@Singleton
public class AsyncEventBusProvider
    implements Provider<AsyncEventBus>
{
  private final AsyncEventBus asyncEventBus;

  @Inject
  public AsyncEventBusProvider(Configuration configuration, ShutdownHandler shutdownHandler) {
    asyncEventBus = new AsyncEventBusImpl(configuration.getEventBusMaxThreadPoolSize());
    shutdownHandler.add(asyncEventBus.getThreadPoolExecutor(), ShutdownPriority.ASYNC_EVENT_BUS);
  }

  @Override
  public AsyncEventBus get() {
    return asyncEventBus;
  }
}
