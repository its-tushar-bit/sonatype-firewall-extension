/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TelemetrySchedulerTest
    extends AbstractComponentTest
{
  @Mock
  private TelemetryCollector telemetryCollector;

  @Mock
  private TelemetrySender telemetrySender;

  @Mock
  private ScheduledExecutorService scheduledExecutorService;
  
  private TelemetryScheduler telemetryScheduler;

  @Before
  public void before() {
    telemetryScheduler = new TelemetryScheduler(telemetryCollector, telemetrySender, scheduledExecutorService);
  }

  @Test
  public void testGetScheduledThreadPoolExecutor_ReturnsCorrectExecutor() {
    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = TelemetryScheduler.getScheduledThreadPoolExecutor();
    ThreadFactory threadFactory = scheduledThreadPoolExecutor.getThreadFactory();
    Thread thread = threadFactory.newThread(() -> {
    });
    assertThat(thread.getName(), is("TelemetryScheduler-0"));
    assertThat(thread.isDaemon(), is(true));
  }

  @Test
  public void testStart_Schedule() {
    TelemetryScheduler telemetrySchedulerSpy = spy(telemetryScheduler);
    Runnable telemetryRunnable = mock(Runnable.class);
    when(telemetrySchedulerSpy.getTelemetryRunnable()).thenReturn(telemetryRunnable);

    telemetrySchedulerSpy.start();

    verify(scheduledExecutorService).scheduleAtFixedRate(eq(telemetryRunnable), eq(0L), eq(1L), eq(TimeUnit.DAYS));
  }

  @Test
  public void testGetTelemetryRunnableRun_SendSuccess() throws Exception {
    TelemetryData telemetryData = mock(TelemetryData.class);
    when(telemetryCollector.collectData()).thenReturn(telemetryData);

    telemetryScheduler.getTelemetryRunnable().run();

    verify(telemetryCollector).collectData();
    verify(telemetrySender).send(telemetryData);
  }

  @Test
  public void testStop_ShutdownExecutor() {
    telemetryScheduler.stop();
    
    verify(scheduledExecutorService).shutdown();
  }
}
