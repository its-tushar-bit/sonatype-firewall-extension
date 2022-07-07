/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyEvaluationThreadPoolExecutorTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(PolicyEvaluationThreadPoolExecutor.class);

  @Test
  public void testMaxThreadsAreUsed() throws Exception {
    CountDownLatch countDownLatch = new CountDownLatch(PolicyEvaluationThreadPoolExecutor.THREAD_POOL_SIZE);
    ThreadPoolExecutor threadPoolExecutor = new PolicyEvaluationThreadPoolExecutor();

    for (int i = 0; i < PolicyEvaluationThreadPoolExecutor.THREAD_POOL_SIZE; i++) {
      threadPoolExecutor.submit(() -> {
        countDownLatch.countDown();
        sleep(10000);
      });
    }
    countDownLatch.await(5, TimeUnit.SECONDS);
  }

  @Test
  public void testNoWarningIsLoggedWhenThePoolIsNotExhausted() throws Exception {
    ThreadPoolExecutor threadPoolExecutor = new PolicyEvaluationThreadPoolExecutor();

    for (int i = 0; i < PolicyEvaluationThreadPoolExecutor.THREAD_POOL_SIZE; i++) {
      threadPoolExecutor.submit(() -> sleep(5000));
    }
    assertThat(logOutput).atAnyLevel().doesNotContain("All policy evaluation threads are busy");
  }

  @Test
  public void testWarningIsLoggedWhenThePoolIsExhausted() throws Exception {
    ThreadPoolExecutor threadPoolExecutor = new PolicyEvaluationThreadPoolExecutor();
    for (int i = 0; i < PolicyEvaluationThreadPoolExecutor.THREAD_POOL_SIZE + 1; i++) {
      threadPoolExecutor.submit(() -> sleep(5000));
    }
    assertThat(logOutput).atWarnLevel()
        .contains("All policy evaluation threads are busy and there are 1 tasks waiting in the queue.");
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
