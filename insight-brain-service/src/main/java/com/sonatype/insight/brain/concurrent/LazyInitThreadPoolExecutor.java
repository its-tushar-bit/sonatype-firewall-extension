/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.concurrent;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

public class LazyInitThreadPoolExecutor
{
  private final int threadPoolSize;

  private final int taskQueueCapacity;

  private final String nameFormat;

  private final long keepAliveTimeInSeconds;

  private volatile ThreadPoolExecutor threadPoolExecutor;

  public LazyInitThreadPoolExecutor(
      int threadPoolSize,
      int taskQueueCapacity,
      String nameFormat,
      long keepAliveTimeInSeconds)
  {
    this.threadPoolSize = threadPoolSize;
    this.taskQueueCapacity = taskQueueCapacity;
    this.nameFormat = nameFormat;
    this.keepAliveTimeInSeconds = keepAliveTimeInSeconds;
  }

  public ThreadPoolExecutor getThreadPoolExecutor() {
    if (null == threadPoolExecutor) {
      synchronized (this) {
        if (null == threadPoolExecutor) {
          threadPoolExecutor = initThreadPoolExecutor();
        }
      }
    }
    return threadPoolExecutor;
  }

  private ThreadPoolExecutor initThreadPoolExecutor() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setDaemon(true)
        .setNameFormat(nameFormat)
        .build();
    ThreadPoolExecutor localThreadPoolExecutor = new ThreadPoolExecutor(
        threadPoolSize,
        threadPoolSize,
        keepAliveTimeInSeconds,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(taskQueueCapacity),
        threadFactory);
    localThreadPoolExecutor.allowCoreThreadTimeOut(true);
    return localThreadPoolExecutor;
  }

  public void shutdown() {
    if (null != threadPoolExecutor) {
      threadPoolExecutor.shutdownNow();
      while (!threadPoolExecutor.isShutdown()) {
        try {
          sleep(100);
        }
        catch (InterruptedException e) {
          currentThread().interrupt();
          break;
        }
      }
    }
  }
}
