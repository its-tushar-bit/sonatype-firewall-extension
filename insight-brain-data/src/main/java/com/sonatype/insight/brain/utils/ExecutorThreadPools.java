/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import jakarta.inject.Inject;

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

  private static ExecutorThreadPools INSTANCE;

  @Inject
  private static void injectInstance(ExecutorThreadPools executorThreadPools) {
    if (INSTANCE != null) {
      log.warn("Replacing existing ExecutorThreadPools instance. Shutting down previous pools.");
      INSTANCE.shutdown();
    }

    INSTANCE = executorThreadPools;
  }

  public static ExecutorThreadPools getInstance() {
    if (INSTANCE == null) {
      /*
       * This class used to be static rather than Guice managed. This makes it difficult to extend / modify
       * functionality. We make use of static injection
       * (https://github.com/google/guice/wiki/Injections#static-injections) which allows us to retain existing
       * behaviour
       * and not have to refactor hundreds of classes while also giving us support for injection.
       */
      INSTANCE = new DefaultExecutorThreadPools();

      log.warn("Injection of ExecutorThreadPools not run, creating an instance to prevent application/test failure");
    }
    return INSTANCE;
  }

  public abstract ForkJoinPool createThreadPool(int minThreads, int maxThreads, int defaultThreads, String configProp);

  public abstract Executor getThreadPool(ThreadPools pool);

  public abstract void shutdown();
}
