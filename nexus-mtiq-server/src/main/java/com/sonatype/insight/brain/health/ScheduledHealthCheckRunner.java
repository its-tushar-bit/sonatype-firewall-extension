/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.health;

import com.codahale.metrics.health.HealthCheck;
import com.sonatype.insight.brain.health.MtiqHealthConfig.MtiqHealthCheckConfig;
import com.sonatype.insight.brain.health.MtiqHealthConfig.MtiqHealthScheduleConfig;
import com.sonatype.insight.brain.operational.check.AbstractOperationalCheck;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

class ScheduledHealthCheckRunner
{
  private static final Logger log = LoggerFactory.getLogger(ScheduledHealthCheckRunner.class);

  private final AbstractOperationalCheck delegate;

  private final MtiqHealthCheckConfig config;

  private final MtiqHealthScheduleConfig schedule;

  private final AtomicReference<HealthCheck.Result> cachedResult;

  private volatile boolean currentlyHealthy;

  private volatile int consecutiveCount;

  private volatile ScheduledExecutorService executor;

  ScheduledHealthCheckRunner(AbstractOperationalCheck delegate, MtiqHealthCheckConfig config) {
    this.delegate = delegate;
    this.config = config;
    this.schedule = config.getSchedule();
    this.currentlyHealthy = config.isInitialState();
    this.consecutiveCount = 0;
    this.cachedResult = new AtomicReference<>(config.isInitialState()
        ? HealthCheck.Result.healthy()
        : HealthCheck.Result.builder().unhealthy().build());
  }

  HealthCheck.Result getCachedResult() {
    return cachedResult.get();
  }

  void start(ScheduledExecutorService executor) {
    this.executor = executor;
    Duration initialDelay = schedule.getInitialDelay();
    log.info("Scheduling health check '{}' with initialDelay={}s, checkInterval={}s, downtimeInterval={}s, "
        + "failureAttempts={}, successAttempts={}, initialState={}",
        delegate.getName(),
        initialDelay.getSeconds(),
        schedule.getCheckInterval().getSeconds(),
        schedule.getDowntimeInterval().getSeconds(),
        schedule.getFailureAttempts(),
        schedule.getSuccessAttempts(),
        config.isInitialState());
    executor.schedule(this::runCheck, initialDelay.toMillis(), TimeUnit.MILLISECONDS);
  }

  private void runCheck() {
    try {
      Health health = delegate.check();
      boolean healthy = health.getStatus() == Status.UP;
      updateState(healthy, health);
    }
    catch (Exception e) {
      log.debug("Health check '{}' threw exception", delegate.getName(), e);
      updateState(false, Health.down(e).build());
    }

    scheduleNext();
  }

  private void updateState(boolean rawHealthy, Health health) {
    if (rawHealthy == currentlyHealthy) {
      consecutiveCount = 0;
    }
    else {
      consecutiveCount++;
      int threshold = rawHealthy ? schedule.getSuccessAttempts() : schedule.getFailureAttempts();
      if (consecutiveCount >= threshold) {
        boolean previous = currentlyHealthy;
        currentlyHealthy = rawHealthy;
        consecutiveCount = 0;
        log.info("Health check '{}' state changed: {} -> {}",
            delegate.getName(), previous ? "healthy" : "unhealthy", rawHealthy ? "healthy" : "unhealthy");
      }
    }

    cachedResult.set(toResult(currentlyHealthy, health));
  }

  private void scheduleNext() {
    if (executor == null || executor.isShutdown()) {
      return;
    }
    Duration interval = currentlyHealthy ? schedule.getCheckInterval() : schedule.getDowntimeInterval();
    try {
      executor.schedule(this::runCheck, interval.toMillis(), TimeUnit.MILLISECONDS);
    }
    catch (Exception e) {
      log.debug("Failed to schedule next health check for '{}'", delegate.getName(), e);
    }
  }

  private static HealthCheck.Result toResult(boolean healthy, Health health) {
    HealthCheck.ResultBuilder builder = healthy
        ? HealthCheck.Result.builder().healthy()
        : HealthCheck.Result.builder().unhealthy();
    health.getDetails().forEach(builder::withDetail);
    return builder.build();
  }
}
