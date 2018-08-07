/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.50
 */
public class ExecutorThreadPools
{
  private static final Logger log = LoggerFactory.getLogger(ExecutorThreadPools.class);

  private static int DEFAULT_UTILITY_THREADS = 20;

  /**
   * This pool is intended for use ONLY with non-blocking calls and tasks that will not generate long pauses.
   */
  public static final ForkJoinPool GENERAL_UTILITY_THREADS;

  static {
    int utilThreads = Integer.getInteger("insight.threads.utility", DEFAULT_UTILITY_THREADS);
    GENERAL_UTILITY_THREADS = namedForkJoinPool(utilThreads, "insight-thread-utility-");
    log.info("insight.threads.utility pool-size: {}", utilThreads);
  }

  private static ForkJoinPool namedForkJoinPool(int threadCount, String namePrefix) {
    final ForkJoinWorkerThreadFactory factory = (pool) -> {
      final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
      worker.setName(namePrefix + worker.getPoolIndex());
      return worker;
    };

    return new ForkJoinPool(threadCount, factory, null, false);
  }
}
