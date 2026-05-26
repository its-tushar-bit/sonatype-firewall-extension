/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExecutorThreadPools
{
  private static final Logger log = LoggerFactory.getLogger(ExecutorThreadPools.class);

  public enum ThreadPools
  {
    DAO,
    GENERAL
  }

  private static volatile ExecutorThreadPools INSTANCE;

  public static void injectInstance(ExecutorThreadPools executorThreadPools) {
    if (INSTANCE == executorThreadPools) {
      return;
    }
    if (INSTANCE != null) {
      log.info("Replacing ExecutorThreadPools instance with Spring-managed bean. Shutting down previous pools.");
      INSTANCE.shutdown();
    }

    INSTANCE = executorThreadPools;
  }

  public static ExecutorThreadPools getInstance() {
    if (INSTANCE == null) {
      /*
       * This class used to be purely static, which made it difficult to extend or modify its behavior.
       * We keep a compatibility fallback here so existing call sites continue to work until the remaining
       * static access patterns can be removed.
       */
      INSTANCE = new DefaultExecutorThreadPools();

      log.debug("Creating fallback ExecutorThreadPools (Spring-managed bean not yet available)");
    }
    return INSTANCE;
  }

  public abstract ForkJoinPool createThreadPool(int minThreads, int maxThreads, int defaultThreads, String configProp);

  public abstract Executor getThreadPool(ThreadPools pool);

  public abstract void shutdown();
}
