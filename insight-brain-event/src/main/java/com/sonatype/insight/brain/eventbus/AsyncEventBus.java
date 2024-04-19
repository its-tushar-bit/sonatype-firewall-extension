/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.eventbus;

import java.util.concurrent.ThreadPoolExecutor;

public interface AsyncEventBus
{
  int DEFAULT_MAX_POOL_SIZE = 500;

  void register(final Object handler);

  void unregister(final Object handler);

  void post(final Object event);

  int getMaxPoolSize();

  void setMaxPoolSize(int maxPoolSize);

  ThreadPoolExecutor getThreadPoolExecutor();
}
