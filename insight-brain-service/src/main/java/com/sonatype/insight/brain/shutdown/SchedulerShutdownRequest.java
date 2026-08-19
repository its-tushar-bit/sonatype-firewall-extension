/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.quartz.Scheduler;

public class SchedulerShutdownRequest
    extends WeakReferenceShutdownRequest<Scheduler>
{
  private final ShutdownHandler shutdownHandler;

  public SchedulerShutdownRequest(
      final Scheduler item,
      final int order,
      final String origin,
      final ShutdownHandler shutdownHandler)
  {
    super(item, order, origin);
    this.shutdownHandler = shutdownHandler;
  }

  @Override
  public Future<?> execute(final ExecutorService executorService, final Scheduler item) {
    return executorService.submit(() -> shutdownHandler.tryCheckedRunnable(() -> item.shutdown(true)));
  }
}
