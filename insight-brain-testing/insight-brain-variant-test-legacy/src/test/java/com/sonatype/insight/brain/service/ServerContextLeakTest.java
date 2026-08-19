/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.scheduler.QuartzJobSchedulingService;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.brain.variant.LegacyServerTest;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Regression guard for the QuartzJobSchedulingService context-leak fix. Each IQ server boot creates a Spring
 * ApplicationContext; {@link com.sonatype.insight.brain.scheduler.QuartzJobSchedulingService} owns a
 * ScheduledThreadPoolExecutor whose thread must terminate when the context closes, otherwise every server restart in a
 * single JVM (reuseForks=true, or IntelliJ "run all") leaks a whole context and eventually OOMs.
 *
 * <p>
 * This restarts the server several times in one JVM — a non-reusable {@code Configurator} forces a real
 * {@code applicationContext.close()} + fresh start each time — and asserts the {@code QuartzJobSchedulingServiceThread}
 * does not accumulate (only the current server's thread should be alive). Before the {@code @PreDestroy} fix this grew
 * by one per restart.
 *
 * <p>
 * NOTE: fully collecting a closed context also requires other subsystems (embedded Jetty, logback async appenders,
 * SelfThrottlingLoadBalancer/heartbeat, HTTP clients) to stop their threads on close. That broader "make the IQ server
 * context fully disposable" work is tracked separately; this test guards only the Quartz thread, which was the single
 * largest retainer.
 */
@LegacyServerTest
public class ServerContextLeakTest
    extends AbstractBrainServiceIntegrationTest
{
  private static final String QUARTZ_THREAD_PREFIX = QuartzJobSchedulingService.SCHEDULING_THREAD_NAME;

  private static final int RESTARTS = 4;

  @Test
  @Timeout(value = 3, unit = TimeUnit.MINUTES)
  @ManualIqServerInit
  public void closingTheServerContextTerminatesItsSchedulingThread() throws Exception {
    for (int i = 0; i < RESTARTS; i++) {
      // A custom Configurator is non-reusable by default, so each call stops the previous server
      // (applicationContext.close()) and starts a fresh one.
      startIqTestServer(config -> {
        // no configuration overrides needed; the point is to force a restart
      });
    }

    // Only the current server's scheduling thread should remain; the closed contexts' threads must have terminated.
    Awaitility.await()
        .atMost(Duration.ofSeconds(30))
        .untilAsserted(() -> assertThat(countLiveThreads(QUARTZ_THREAD_PREFIX))
            .as("live %s threads after %d server restarts", QUARTZ_THREAD_PREFIX, RESTARTS)
            .isLessThanOrEqualTo(1));
  }

  private static long countLiveThreads(String namePrefix) {
    return Thread.getAllStackTraces()
        .keySet()
        .stream()
        .filter(Thread::isAlive)
        .filter(thread -> thread.getName().startsWith(namePrefix))
        .count();
  }
}
