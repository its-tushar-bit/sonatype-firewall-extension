/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.50
 */
public class ExecutorThreadPools
{
  private static final Logger log = LoggerFactory.getLogger(ExecutorThreadPools.class);

  public static enum THREAD_POOLS
  {
    DAO, GENERAL
  }

  private static int DEFAULT_UTILITY_THREADS = 20;

  private static int DEFAULT_DAO_THREADS = 20;

  private static final int SINGLE_THREAD_COUNT = 1;

  private static final String SINGLE_THREAD_PREFIX = "insight-thread-";

  /**
   * Fall back to single threaded pools if insight.threads.disabled is set to true.
   */
  private static final boolean THREADING_DISABLED;

  /**
   * This pool is intended for use ONLY with non-blocking calls and tasks that will not generate long pauses.
   */
  private static final ForkJoinPool GENERAL_UTILITY_THREADS;

  /**
   * Shared pool for DAO queries.
   */
  private static final ForkJoinPool DAO_FORK_JOIN_POOL;

  static {
    int utilThreads = Integer.getInteger("insight.threads.utility", DEFAULT_UTILITY_THREADS);
    GENERAL_UTILITY_THREADS = namedForkJoinPool(utilThreads, "insight-thread-utility-");
    log.info("insight.threads.utility pool-size: {}", utilThreads);
  }

  static {
    int daoThreads = Integer.getInteger("insight.threads.dao", DEFAULT_DAO_THREADS);
    DAO_FORK_JOIN_POOL = namedForkJoinPool(daoThreads, "insight-thread-dao-");
    log.info("insight.threads.dao pool-size: {}", daoThreads);
  }

  static {
    THREADING_DISABLED = Boolean.getBoolean("insight.threads.disabled");
    log.info("insight.threads.disabled: {}", THREADING_DISABLED);
  }

  private static ForkJoinPool namedForkJoinPool(int threadCount, String namePrefix) {
    final ForkJoinWorkerThreadFactory factory = (pool) -> {
      final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
      worker.setName(namePrefix + worker.getPoolIndex());
      return worker;
    };

    return new ForkJoinPool(threadCount, factory, null, false);
  }

  public static Executor getThreadPool(THREAD_POOLS pool) {
    if (THREADING_DISABLED) {
      return namedForkJoinPool(SINGLE_THREAD_COUNT, SINGLE_THREAD_PREFIX);
    }

    switch (pool) {
      case DAO:
        return DAO_FORK_JOIN_POOL;
      case GENERAL:
        return GENERAL_UTILITY_THREADS;
      default:
        return namedForkJoinPool(SINGLE_THREAD_COUNT, SINGLE_THREAD_PREFIX);
    }
  }
}
