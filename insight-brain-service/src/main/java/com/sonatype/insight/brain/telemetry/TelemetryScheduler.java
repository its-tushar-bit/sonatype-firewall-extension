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
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.security.SystemRunnable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class TelemetryScheduler
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(TelemetryScheduler.class);

  private final List<TelemetryCollector> telemetryCollectors;

  private final TelemetrySender telemetrySender;

  private final ScheduledExecutorService scheduledExecutorService;

  @Inject
  public TelemetryScheduler(List<TelemetryCollector> telemetryCollectors, TelemetrySender telemetrySender) {
    this(telemetryCollectors, telemetrySender, getScheduledThreadPoolExecutor());
  }

  @VisibleForTesting
  TelemetryScheduler(List<TelemetryCollector> telemetryCollectors,
                     TelemetrySender telemetrySender,
                     ScheduledExecutorService scheduledExecutorService)
  {
    this.telemetryCollectors = telemetryCollectors.stream()
        .filter(telemetryCollector -> !telemetryCollector.isClusterTelemetry()).collect(Collectors.toList());
    this.telemetrySender = telemetrySender;
    this.scheduledExecutorService = scheduledExecutorService;
  }

  @Override
  public void start() {
    scheduledExecutorService.scheduleAtFixedRate(getTelemetryRunnable(), 0L, 1L, TimeUnit.DAYS);
  }

  @VisibleForTesting
  public Runnable getTelemetryRunnable() {
    return new SystemRunnable(() -> {
      for (TelemetryCollector telemetryCollector : telemetryCollectors) {
        try {
          telemetrySender.send(telemetryCollector.collectAllData());
        }
        catch (Exception e) {
          log.debug("Unable to send telemetry for collector {}", telemetryCollector, e);
        }
      }
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
