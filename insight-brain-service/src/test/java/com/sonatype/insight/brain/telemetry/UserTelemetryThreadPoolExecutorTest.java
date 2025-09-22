/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.sonatype.insight.test.LogOutput;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTelemetryThreadPoolExecutorTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(UserTelemetryThreadPoolExecutor.class);

  @Test
  public void testMaxThreadsAreUsed() throws Exception {
    CountDownLatch countDownLatch = new CountDownLatch(UserTelemetryThreadPoolExecutor.DEFAULT_MAX_THREAD_POOL_SIZE);
    ThreadPoolExecutor threadPoolExecutor = new UserTelemetryThreadPoolExecutor(UserTelemetryThreadPoolExecutor
        .DEFAULT_MAX_THREAD_POOL_SIZE);

    for (int i = 0; i < UserTelemetryThreadPoolExecutor.DEFAULT_MAX_THREAD_POOL_SIZE; i++) {
      threadPoolExecutor.submit(() -> {
        countDownLatch.countDown();
        sleep(10000);
      });
    }
    countDownLatch.await(5, TimeUnit.SECONDS);
    threadPoolExecutor.shutdown();
  }

  @Test
  public void testNoWarningIsLoggedWhenThePoolIsNotExhausted() {
    ThreadPoolExecutor threadPoolExecutor = new UserTelemetryThreadPoolExecutor(UserTelemetryThreadPoolExecutor
        .DEFAULT_MAX_THREAD_POOL_SIZE);

    for (int i = 0; i < UserTelemetryThreadPoolExecutor.DEFAULT_MAX_THREAD_POOL_SIZE; i++) {
      threadPoolExecutor.submit(() -> sleep(5000));
    }
    assertThat(logOutput).atAnyLevel().doesNotContain("All User Telemetry threads are busy");
    threadPoolExecutor.shutdown();
  }

  @Test
  public void testWarningIsLoggedWhenThePoolIsExhausted() {
    ThreadPoolExecutor threadPoolExecutor = new UserTelemetryThreadPoolExecutor(UserTelemetryThreadPoolExecutor
        .DEFAULT_MAX_THREAD_POOL_SIZE);

    for (int i = 0; i < UserTelemetryThreadPoolExecutor.DEFAULT_MAX_THREAD_POOL_SIZE + 1; i++) {
      threadPoolExecutor.submit(() -> sleep(5000));
    }
    assertThat(logOutput).atAnyLevel().contains("All User Telemetry threads are busy");
    threadPoolExecutor.shutdown();
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
