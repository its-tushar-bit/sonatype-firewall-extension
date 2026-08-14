/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultExecutorThreadPoolsTest
{
  @Test
  public void testGetThreadCount() {
    // test min
    int min1 = 5;
    int max1 = 10;
    int default1 = 8;
    String property1 = "insight.brain.threadpool.test1";
    System.setProperty(property1, "-1");

    int expected1 = min1;
    assertThat(DefaultExecutorThreadPools.getThreadCount(min1, max1, default1, property1)).isEqualTo(expected1);

    // test max
    int min2 = 5;
    int max2 = 10;
    int default2 = 8;
    String property2 = "insight.brain.threadpool.test2";
    System.setProperty(property2, "1000");

    int expected2 = max2;
    assertThat(DefaultExecutorThreadPools.getThreadCount(min2, max2, default2, property2)).isEqualTo(expected2);

    // test default
    int min3 = 5;
    int max3 = 10;
    int default3 = 8;
    String property3 = "insight.brain.threadpool.test3";
    System.setProperty(property3, "");

    int expected3 = default3;
    assertThat(DefaultExecutorThreadPools.getThreadCount(min3, max3, default3, property3)).isEqualTo(expected3);
  }

  @Test
  public void testThreadPoolUsingDaemonThreads() {
    int min = 5;
    int max = 10;
    int defaultValue = 8;
    String property = "insight.brain.threadpool.test";
    ForkJoinPool testPool;

    testPool = ExecutorThreadPools.getInstance().createThreadPool(min, max, defaultValue, property);

    CompletableFuture<Boolean> daemonCheck =
        CompletableFuture.supplyAsync(() -> Thread.currentThread().isDaemon(), testPool);

    assertThat(daemonCheck.join()).isTrue();
  }
}
