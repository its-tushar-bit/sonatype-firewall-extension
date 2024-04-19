/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.quartz.Scheduler;

public class SchedulerShutdownRequest
    extends WeakReferenceShutdownRequest<Scheduler>
{
  public SchedulerShutdownRequest(final WeakReference<Scheduler> item, final int order) {
    super(item, order);
  }

  @Override
  public Future<?> execute(final ExecutorService executorService, final Scheduler item) {
    return executorService.submit(() -> ShutdownHandler.tryCheckedRunnable(() -> item.shutdown(true)));
  }
}
