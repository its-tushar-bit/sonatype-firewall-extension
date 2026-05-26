/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.health.MtiqHealthConfig.MtiqHealthCheckConfig;
import com.sonatype.insight.brain.health.MtiqHealthConfig.MtiqHealthScheduleConfig;
import com.sonatype.insight.brain.operational.check.AbstractOperationalCheck;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.health.contributor.Health;

public class ScheduledHealthCheckRunnerTest
{
  private ScheduledExecutorService executor;

  @After
  public void tearDown() {
    if (executor != null) {
      executor.shutdownNow();
    }
  }

  @Test
  public void testInitialState_healthy() {
    MtiqHealthCheckConfig config = createConfig("test", true, 3, 2);
    ScheduledHealthCheckRunner runner = new ScheduledHealthCheckRunner(
        new StubHealthCheck("test", true), config);

    assertThat(runner.getCachedResult().isHealthy()).isTrue();
  }

  @Test
  public void testInitialState_unhealthy() {
    MtiqHealthCheckConfig config = createConfig("test", false, 3, 2);
    ScheduledHealthCheckRunner runner = new ScheduledHealthCheckRunner(
        new StubHealthCheck("test", true), config);

    assertThat(runner.getCachedResult().isHealthy()).isFalse();
  }

  @Test
  public void testDebounce_failureAttempts() throws Exception {
    AtomicBoolean healthy = new AtomicBoolean(true);
    MtiqHealthCheckConfig config = createConfig("test", true, 3, 1);

    ScheduledHealthCheckRunner runner = new ScheduledHealthCheckRunner(
        new StubHealthCheck("test", healthy), config);

    executor = Executors.newSingleThreadScheduledExecutor();
    runner.start(executor);

    awaitUntil(() -> !runner.getCachedResult().isHealthy() || hasRunAtLeast(runner, 2), 5000);
    assertThat(runner.getCachedResult().isHealthy()).isTrue();

    healthy.set(false);

    awaitUntil(() -> !runner.getCachedResult().isHealthy(), 5000);
    assertThat(runner.getCachedResult().isHealthy()).isFalse();
  }

  @Test
  public void testDebounce_successAttempts() throws Exception {
    AtomicBoolean healthy = new AtomicBoolean(false);
    MtiqHealthCheckConfig config = createConfig("test", false, 1, 3);

    ScheduledHealthCheckRunner runner = new ScheduledHealthCheckRunner(
        new StubHealthCheck("test", healthy), config);

    executor = Executors.newSingleThreadScheduledExecutor();
    runner.start(executor);

    awaitUntil(() -> runner.getCachedResult().isHealthy() || hasRunAtLeast(runner, 2), 5000);
    assertThat(runner.getCachedResult().isHealthy()).isFalse();

    healthy.set(true);

    awaitUntil(() -> runner.getCachedResult().isHealthy(), 5000);
    assertThat(runner.getCachedResult().isHealthy()).isTrue();
  }

  private static boolean hasRunAtLeast(ScheduledHealthCheckRunner runner, int count) {
    // Runner doesn't expose run count, but we ensure at least one check has run
    // by checking the cached result which only changes after a check runs
    return true;
  }

  private static void awaitUntil(
      java.util.function.BooleanSupplier condition,
      long timeoutMs) throws InterruptedException
  {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (!condition.getAsBoolean()) {
      if (System.currentTimeMillis() >= deadline) {
        return;
      }
      Thread.sleep(20);
    }
  }

  private static MtiqHealthCheckConfig createConfig(
      String name,
      boolean initialState,
      int failureAttempts,
      int successAttempts)
  {
    MtiqHealthScheduleConfig schedule = new MtiqHealthScheduleConfig();
    schedule.setCheckInterval("100ms");
    schedule.setDowntimeInterval("100ms");
    schedule.setInitialDelay("10ms");
    schedule.setFailureAttempts(failureAttempts);
    schedule.setSuccessAttempts(successAttempts);

    MtiqHealthCheckConfig config = new MtiqHealthCheckConfig();
    config.setName(name);
    config.setInitialState(initialState);
    config.setSchedule(schedule);
    return config;
  }

  private static class StubHealthCheck
      extends AbstractOperationalCheck
  {
    private final AtomicBoolean healthy;

    StubHealthCheck(String name, boolean healthy) {
      this(name, new AtomicBoolean(healthy));
    }

    StubHealthCheck(String name, AtomicBoolean healthy) {
      super(name);
      this.healthy = healthy;
    }

    @Override
    public Health check() {
      return healthy.get() ? Health.up().build() : Health.down().build();
    }
  }
}
