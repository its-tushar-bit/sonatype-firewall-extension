/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.security.SystemRunnable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.lifecycle.Managed;

@Named
@Singleton
public class TelemetryScheduler
    implements Managed
{
  private final TelemetryCollector telemetryCollector;

  private final TelemetrySender telemetrySender;

  private final ScheduledExecutorService scheduledExecutorService;

  @Inject
  public TelemetryScheduler(TelemetryCollector telemetryCollector, TelemetrySender telemetrySender) {
    this(telemetryCollector, telemetrySender, getScheduledThreadPoolExecutor());
  }

  @VisibleForTesting
  TelemetryScheduler(TelemetryCollector telemetryCollector,
                     TelemetrySender telemetrySender,
                     ScheduledExecutorService scheduledExecutorService)
  {
    this.telemetryCollector = telemetryCollector;
    this.telemetrySender = telemetrySender;
    this.scheduledExecutorService = scheduledExecutorService;
  }

  @Override
  public void start() {
    scheduledExecutorService.scheduleAtFixedRate(getTelemetryRunnable(), 0L, 1L, TimeUnit.DAYS);
  }

  @VisibleForTesting
  Runnable getTelemetryRunnable() {
    return new SystemRunnable(() -> {
      telemetrySender.send(telemetryCollector.collectData());
      telemetrySender.send(telemetryCollector.collectComponentCountsData());
    });
  }

  @Override
  public void stop() {
    scheduledExecutorService.shutdown();
  }

  @VisibleForTesting
  static ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
    return new ScheduledThreadPoolExecutor(1,
        new ThreadFactoryBuilder().setNameFormat("TelemetryScheduler-%d").setDaemon(true).build());
  }
}
