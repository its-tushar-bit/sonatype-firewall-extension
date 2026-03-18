/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.brain.tenancy.TenantAwareOneTimeRunnable;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.shiro.util.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

public class LazyInitThreadPoolExecutor
{
  private final int threadPoolSize;

  private final int taskQueueCapacity;

  private final String nameFormat;

  private final long keepAliveTimeInSeconds;

  private final ShutdownHandler shutdownHandler;

  // clear any data that might be in a pooled thread's ThreadContext before the thread is used? by default, Shiro
  // will propagate the security manager and subject to child threads, but only when the child threads are first created
  private boolean shouldClearShiroThreadContextBeforeThreadStart;

  private volatile ThreadPoolExecutor threadPoolExecutor;

  public LazyInitThreadPoolExecutor(
      int threadPoolSize,
      int taskQueueCapacity,
      String nameFormat,
      long keepAliveTimeInSeconds,
      ShutdownHandler shutdownHandler)
  {
    this.threadPoolSize = threadPoolSize;
    this.taskQueueCapacity = taskQueueCapacity;
    this.nameFormat = nameFormat;
    this.keepAliveTimeInSeconds = keepAliveTimeInSeconds;
    this.shutdownHandler = shutdownHandler;
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

  public LazyInitThreadPoolExecutor setShouldClearShiroThreadContextBeforeThreadStart(boolean shouldClearContext) {
    this.shouldClearShiroThreadContextBeforeThreadStart = shouldClearContext;
    return this;
  }

  private ThreadPoolExecutor initThreadPoolExecutor() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setDaemon(true)
        .setNameFormat(nameFormat)
        .build();
    ThreadPoolExecutor localThreadPoolExecutor = new ResettingThreadPoolExecutor(
        threadPoolSize,
        threadPoolSize,
        keepAliveTimeInSeconds,
        TimeUnit.SECONDS,
        shouldClearShiroThreadContextBeforeThreadStart,
        new LinkedBlockingQueue<>(taskQueueCapacity),
        threadFactory);
    localThreadPoolExecutor.allowCoreThreadTimeOut(true);
    shutdownHandler.add(localThreadPoolExecutor, ShutdownPriority.SOURCE_CONTROL_EVENT_PROCESSOR);
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

  private static class ResettingThreadPoolExecutor
      extends TenantThreadPoolExecutor
  {
    private final Logger log = LoggerFactory.getLogger(ResettingThreadPoolExecutor.class);

    private final boolean shouldClearShiroThreadContextBeforeThreadStart;

    public ResettingThreadPoolExecutor(
        int corePoolSize,
        int maximumPoolSize,
        long keepAliveTime,
        TimeUnit unit,
        boolean shouldClearShiroThreadContextBeforeThreadStart,
        BlockingQueue<Runnable> workQueue,
        ThreadFactory threadFactory)
    {
      super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, new AbortPolicy(),
          "source_control_events", "SourceControlEventProcessor");
      this.shouldClearShiroThreadContextBeforeThreadStart = shouldClearShiroThreadContextBeforeThreadStart;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
      if (log.isTraceEnabled()) {
        ThreadContext.getResources().forEach((k, v) -> {
          log.trace("ThreadContext resource '{}' = {}", k, v);
        });
      }
      if (shouldClearShiroThreadContextBeforeThreadStart) {
        ThreadContext.remove();
        // Signal to TenantAwareOneTimeRunnable that Subject should not be propagated.
        // This ensures tasks run as "system" without a user identity.
        TenantAwareOneTimeRunnable.setSkipSubjectPropagation(true);
      }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
      // Clean up the skip flag so it doesn't leak to subsequent tasks on this thread
      if (shouldClearShiroThreadContextBeforeThreadStart) {
        TenantAwareOneTimeRunnable.setSkipSubjectPropagation(false);
      }
    }
  }
}
