/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.time.Duration;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Retry
{
  private static final Logger log = LoggerFactory.getLogger(Retry.class);

  public interface RetryableCallable<R, T extends Exception>
  {
    R call() throws T;
  }

  public interface RetryableRunnable<T extends Exception>
  {
    void run() throws T;
  }

  private final String name;

  private int maxRetryCount;

  private final Duration maxRetryDuration;

  private final Predicate<Exception> isRetryableException;

  private final Predicate<Exception> isIgnorableException;

  private final IntFunction<Duration> retryDelay;

  public Retry(
      String name,
      int maxRetryCount,
      Duration maxRetryDuration,
      Predicate<Exception> isRetryableException,
      IntFunction<Duration> retryDelay)
  {
    this(name, maxRetryCount, maxRetryDuration, isRetryableException, e -> false, retryDelay);
  }

  public Retry(
      String name,
      int maxRetryCount,
      Duration maxRetryDuration,
      Predicate<Exception> isRetryableException,
      Predicate<Exception> isIgnorableException,
      IntFunction<Duration> retryDelay)
  {
    this.name = name;
    this.maxRetryCount = maxRetryCount;
    this.maxRetryDuration = maxRetryDuration;
    this.isRetryableException = Objects.requireNonNull(isRetryableException);
    this.isIgnorableException = Objects.requireNonNull(isIgnorableException);
    this.retryDelay = Objects.requireNonNull(retryDelay);
  }

  public <R, T extends Exception> R executeCallable(RetryableCallable<R, T> callable) throws T {
    long start = System.currentTimeMillis();
    int ignored = 0;
    for (int retry = 0;;) {
      try {
        return callable.call();
      }
      catch (Exception e) {
        if (isIgnorableException.test(e)) {
          ignored++;
        }
        if (!shouldRetry(retry, ignored, start, e)) {
          throw e;
        }
        retry++;
        long delayMillis = retryDelay.apply(retry).toMillis();
        log.debug("[{}] Encountered retryable exception, making retry {} in {} ms", name, retry, delayMillis);
        try {
          Thread.sleep(delayMillis);
        }
        catch (InterruptedException interrupt) {
          Thread.currentThread().interrupt();
          e.addSuppressed(interrupt);
          throw e;
        }
      }
    }
  }

  private boolean shouldRetry(int retry, int ignored, long start, Exception e) {
    if (maxRetryCount >= 0 && retry >= maxRetryCount + ignored) {
      log.debug("[{}] Reached maximum retry count {}, {} retries did not count towards the limit", name, maxRetryCount,
          ignored);
      return false;
    }
    if (maxRetryDuration != null && retry > 0 && System.currentTimeMillis() - start >= maxRetryDuration.toMillis()) {
      log.debug("[{}] Reached maximum retry duration {}", name, maxRetryDuration);
      return false;
    }
    if (!isIgnorableException.test(e) && !isRetryableException.test(e)) {
      log.debug("[{}] Encountered non-retryable exception", name);
      return false;
    }
    return true;
  }

  public <T extends Exception> void executeRunnable(RetryableRunnable<T> runnable) throws T {
    executeCallable(() -> {
      runnable.run();
      return null;
    });
  }

  public <R> R executeSupplier(Supplier<R> supplier) {
    return executeCallable(supplier::get);
  }
}
