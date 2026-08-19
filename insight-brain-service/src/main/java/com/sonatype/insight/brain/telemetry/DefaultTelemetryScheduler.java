/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sonatype.insight.brain.security.OneTimeSystemRunnable;
import com.sonatype.insight.brain.tenancy.TenantScheduledThreadPoolExecutor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sonatype.insight.brain.lifecycle.Managed;
import org.springframework.context.annotation.DependsOn;

@Named
@Singleton
@DependsOn("defaultApplicationLifecycle")
public class DefaultTelemetryScheduler
    extends TelemetryScheduler
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(DefaultTelemetryScheduler.class);

  private final ScheduledExecutorService scheduledExecutorService;

  private final ShutdownHandler shutdownHandler;

  @Inject
  public DefaultTelemetryScheduler(
      TelemetryCollectorsProvider telemetryCollectorsProvider,
      TelemetrySender telemetrySender,
      ShutdownHandler shutdownHandler)
  {
    this(telemetryCollectorsProvider, telemetrySender, shutdownHandler, getScheduledThreadPoolExecutor());
  }

  @VisibleForTesting
  DefaultTelemetryScheduler(
      TelemetryCollectorsProvider telemetryCollectorsProvider,
      TelemetrySender telemetrySender,
      ShutdownHandler shutdownHandler,
      ScheduledExecutorService scheduledExecutorService)
  {
    super(telemetryCollectorsProvider, telemetrySender);
    this.shutdownHandler = shutdownHandler;
    this.scheduledExecutorService = scheduledExecutorService;
  }

  @Override
  public void start() {
    scheduledExecutorService.scheduleAtFixedRate(getTelemetryRunnable(), 0L, 1L, TimeUnit.DAYS);
    shutdownHandler.add(scheduledExecutorService, ShutdownPriority.TELEMETRY);
  }

  @VisibleForTesting
  public Runnable getTelemetryRunnable() {
    return () -> {
      OneTimeSystemRunnable oneTimeSystemRunnable = new OneTimeSystemRunnable(() -> sendTelemetry(telemetryCollectors));
      try {
        oneTimeSystemRunnable.run();
      }
      catch (RuntimeException e) {
        log.warn("Unable to run telemetry schedule", e);
      }
    };
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
