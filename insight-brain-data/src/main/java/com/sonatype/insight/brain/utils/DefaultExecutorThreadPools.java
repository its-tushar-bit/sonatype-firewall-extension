/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.50
 */
@Named
@Singleton
public class DefaultExecutorThreadPools
    extends ExecutorThreadPools
{
  private static final Logger log = LoggerFactory.getLogger(DefaultExecutorThreadPools.class);

  private static final int DEFAULT_UTILITY_THREADS = 20;

  private static final int DEFAULT_DAO_THREADS = 20;

  private static final int SINGLE_THREAD_COUNT = 1;

  private static final String SINGLE_THREAD_PREFIX = "insight-thread-";

  /**
   * Fall back to single threaded pools if insight.threads.disabled is set to true.
   */
  private static final boolean THREADING_DISABLED;

  /**
   * This pool is intended for use ONLY with non-blocking calls and tasks that will not generate long pauses.
   */
  private final ForkJoinPool generalUtilityThreads;

  /**
   * Shared pool for DAO queries.
   */
  private final ForkJoinPool daoForkJoinPool;

  static {
    THREADING_DISABLED = Boolean.getBoolean("insight.threads.disabled");
    log.info("insight.threads.disabled: {}", THREADING_DISABLED);
  }

  public DefaultExecutorThreadPools() {
    generalUtilityThreads = initGeneralUtilThreads();
    daoForkJoinPool = initDaoForkJoinPool();
  }

  private ForkJoinPool initGeneralUtilThreads() {
    int utilThreads =
        Math.max(SINGLE_THREAD_COUNT, Integer.getInteger("insight.threads.utility", DEFAULT_UTILITY_THREADS));
    ForkJoinPool pool = namedForkJoinPool(utilThreads, "insight-thread-utility-");

    log.info("insight.threads.utility pool-size: {}", utilThreads);

    return pool;
  }

  private ForkJoinPool initDaoForkJoinPool() {
    int daoThreads = Math.max(SINGLE_THREAD_COUNT, Integer.getInteger("insight.threads.dao", DEFAULT_DAO_THREADS));
    ForkJoinPool pool = namedForkJoinPool(daoThreads, "insight-thread-dao-");

    log.info("insight.threads.dao pool-size: {}", daoThreads);

    return pool;
  }

  private LoadingCache<Thread, ForkJoinPool> singleThreadedPoolCache = CacheBuilder.newBuilder().weakKeys()
      .build(CacheLoader.from(key -> namedForkJoinPool(SINGLE_THREAD_COUNT, SINGLE_THREAD_PREFIX)));

  protected ForkJoinPool namedForkJoinPool(int threadCount, String namePrefix) {
    final ForkJoinWorkerThreadFactory factory = pool -> {
      final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
      worker.setName(namePrefix + worker.getPoolIndex());
      return worker;
    };

    return new ForkJoinPool(threadCount, factory, null, false);
  }

  public static int getThreadCount(int minThreads, int maxThreads, int defaultThreads, String configProp) {
    /*
     * get value based on configProp, if not set, fall back to defaultThreads
     * if value is less than minThreads, fall back to minThreads
     * if value is greater than maxThreads, fall back to maxThreads
     */
    int threadCount = Integer.getInteger(configProp, defaultThreads);
    threadCount = Math.max(minThreads, threadCount);
    threadCount = Math.min(maxThreads, threadCount);
    return threadCount;
  }

  @Override
  public ForkJoinPool createThreadPool(int minThreads, int maxThreads, int defaultThreads, String configProp) {
    String namePrefix = configProp.replace(".", "-").replace("threads", "thread") + "-";
    int threadCount = getThreadCount(minThreads, maxThreads, defaultThreads, configProp);
    return namedForkJoinPool(threadCount, namePrefix);
  }

  @Override
  public Executor getThreadPool(ThreadPools pool) {
    if (THREADING_DISABLED) {
      return singleThreadedPoolCache.getUnchecked(Thread.currentThread());
    }

    switch (pool) {
      case DAO:
        return daoForkJoinPool;
      case GENERAL:
        return generalUtilityThreads;
      default:
        return singleThreadedPoolCache.getUnchecked(Thread.currentThread());
    }
  }
}
