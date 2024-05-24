/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TaskSchedulerClusterStateMonitorTest
{
  @Mock
  private MultiTenantTaskScheduler mockMultiTenantTaskScheduler;

  @Mock
  private ScheduledExecutorService mockScheduledExecutorService;

  private TaskSchedulerClusterStateMonitor taskSchedulerClusterStateMonitor;

  @Before
  public void before() {
    taskSchedulerClusterStateMonitor =
        new TaskSchedulerClusterStateMonitor(mockMultiTenantTaskScheduler, mockScheduledExecutorService);
  }

  @Test
  public void testStart() throws Exception {
    taskSchedulerClusterStateMonitor.start();

    ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(mockScheduledExecutorService).scheduleAtFixedRate(
        runnableArgumentCaptor.capture(),
        eq(0L),
        eq(10000L),
        eq(TimeUnit.MILLISECONDS)
    );

    runnableArgumentCaptor.getValue().run();

    verify(mockMultiTenantTaskScheduler).startOrStandbyTaskSchedulers();
  }

  @Test
  public void testStop() {
    taskSchedulerClusterStateMonitor.stop();

    verify(mockScheduledExecutorService).shutdown();
  }
}
