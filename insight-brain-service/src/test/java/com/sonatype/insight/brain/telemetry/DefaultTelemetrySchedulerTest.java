/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultTelemetrySchedulerTest
    extends AbstractComponentTest
{
  @Spy
  private TelemetryCollector telemetryCollector1;

  @Spy
  private TelemetryCollector telemetryCollector2;

  @Mock
  private TelemetrySender telemetrySender;

  @Mock
  private ScheduledExecutorService scheduledExecutorService;

  private DefaultTelemetryScheduler defaultTelemetryScheduler;

  @Before
  public void before() {
    defaultTelemetryScheduler = new DefaultTelemetryScheduler(
        new DefaultTelemetryCollectorsProvider(Set.of(telemetryCollector1, telemetryCollector2)),
        telemetrySender, scheduledExecutorService);
  }

  @Test
  public void testGetScheduledThreadPoolExecutor_ReturnsCorrectExecutor() {
    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
        DefaultTelemetryScheduler.getScheduledThreadPoolExecutor();
    ThreadFactory threadFactory = scheduledThreadPoolExecutor.getThreadFactory();
    Thread thread = threadFactory.newThread(() -> {
    });
    assertThat(thread.getName()).isEqualTo("TelemetryScheduler-0");
    assertThat(thread.isDaemon()).isTrue();
  }

  @Test
  public void testStart_Schedule() {
    DefaultTelemetryScheduler defaultTelemetrySchedulerSpy = spy(defaultTelemetryScheduler);
    Runnable telemetryRunnable = mock(Runnable.class);
    when(defaultTelemetrySchedulerSpy.getTelemetryRunnable()).thenReturn(telemetryRunnable);

    defaultTelemetrySchedulerSpy.start();

    verify(scheduledExecutorService).scheduleAtFixedRate(eq(telemetryRunnable), eq(0L), eq(1L), eq(TimeUnit.DAYS));
  }

  @Test
  public void testGetTelemetryRunnableRun_SendSuccess() {
    TelemetryData telemetryData1 = mock(TelemetryData.class);
    when(telemetryCollector1.collectData()).thenReturn(telemetryData1);

    TelemetryData telemetryData2 = mock(TelemetryData.class);
    when(telemetryCollector2.collectData()).thenReturn(telemetryData2);

    defaultTelemetryScheduler.getTelemetryRunnable().run();

    verify(telemetryCollector1).collectAllData();
    verify(telemetryCollector2).collectAllData();
    verify(telemetrySender).send(Collections.singletonList(telemetryData1));
    verify(telemetrySender).send(Collections.singletonList(telemetryData2));
  }

  @Test
  public void testGetTelemetryRunnableRun_SendFailure() {
    when(telemetryCollector1.collectData()).thenThrow(new RuntimeException("Failure"));

    TelemetryData telemetryData2 = mock(TelemetryData.class);
    when(telemetryCollector2.collectData()).thenReturn(telemetryData2);

    defaultTelemetryScheduler.getTelemetryRunnable().run();

    verify(telemetryCollector1).collectAllData();
    verify(telemetryCollector2).collectAllData();
    verify(telemetrySender).send(Collections.singletonList(telemetryData2));
  }

  @Test
  public void testStop_ShutdownExecutor() {
    defaultTelemetryScheduler.stop();

    verify(scheduledExecutorService).shutdown();
  }
}
