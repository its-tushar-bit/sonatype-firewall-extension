/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.quartz.Scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SchedulerShutdownRequestTest
{
  @Test
  public void testExecute() throws Exception {
    ExecutorService mockExecutorService = mock(ExecutorService.class);
    Future<?> future = CompletableFuture.completedFuture(null);
    doReturn(future).when(mockExecutorService).submit(any(Runnable.class));
    Scheduler mockScheduler = mock(Scheduler.class);
    ShutdownHandler shutdownHandler = new ShutdownHandler();
    SchedulerShutdownRequest schedulerShutdownRequest =
        new SchedulerShutdownRequest(mockScheduler, 0, null, shutdownHandler);

    Future<?> result = schedulerShutdownRequest.execute(mockExecutorService);

    assertThat(result).isEqualTo(future);
    ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(mockExecutorService).submit(runnableArgumentCaptor.capture());
    Runnable task = runnableArgumentCaptor.getValue();
    task.run();
    verify(mockScheduler).shutdown(true);
  }
}
