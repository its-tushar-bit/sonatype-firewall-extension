/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.eventbus.AsyncEventBusImpl;
import com.sonatype.insight.brain.service.Configuration;

@Named
@Singleton
public class AsyncEventBusProvider
    implements Provider<AsyncEventBus>
{
  private final AsyncEventBus asyncEventBus;

  @Inject
  public AsyncEventBusProvider(Configuration configuration) {
    asyncEventBus = new AsyncEventBusImpl(configuration.getEventBusMaxThreadPoolSize());
  }

  @Override
  public AsyncEventBus get() {
    return asyncEventBus;
  }
}
