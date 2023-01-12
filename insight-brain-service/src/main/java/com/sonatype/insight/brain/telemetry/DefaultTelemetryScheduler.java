/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.security.SystemRunnable;
import com.sonatype.insight.brain.tenancy.TenantScheduledThreadPoolExecutor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.lifecycle.Managed;

@Named
@Singleton
public class DefaultTelemetryScheduler
    extends TelemetryScheduler
    implements Managed
{
  private final ScheduledExecutorService scheduledExecutorService;

  @Inject
  public DefaultTelemetryScheduler(List<TelemetryCollector> telemetryCollectors, TelemetrySender telemetrySender) {
    this(telemetryCollectors, telemetrySender, getScheduledThreadPoolExecutor());
  }

  @VisibleForTesting
  DefaultTelemetryScheduler(
      List<TelemetryCollector> telemetryCollectors,
      TelemetrySender telemetrySender,
      ScheduledExecutorService scheduledExecutorService)
  {
    super(telemetryCollectors, telemetrySender);
    this.scheduledExecutorService = scheduledExecutorService;
  }

  @Override
  public void start() {
    scheduledExecutorService.scheduleAtFixedRate(getTelemetryRunnable(), 0L, 1L, TimeUnit.DAYS);
  }

  @VisibleForTesting
  public Runnable getTelemetryRunnable() {
    return new SystemRunnable(() -> {
      sendTelemetry(telemetryCollectors);
    });
  }

  @Override
  public void stop() {
    scheduledExecutorService.shutdown();
  }

  @VisibleForTesting
  static ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
    return new TenantScheduledThreadPoolExecutor(1,
        new ThreadFactoryBuilder().setNameFormat("TelemetryScheduler-%d").setDaemon(true).build());
  }
}
