/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

class ManagedRunnable
    implements Runnable
{
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

  private final Duration timeout;

  private volatile boolean started;

  private final CountDownLatch stop = new CountDownLatch(1);

  public ManagedRunnable() {
    this(DEFAULT_TIMEOUT);
  }

  public ManagedRunnable(final Duration timeout) {
    this.timeout = timeout;
  }

  @Override
  public void run() {
    started = true;
    try {
      stop.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException e) {
      // noop
    }
  }

  public boolean isStarted() {
    return started;
  }

  public void waitUntilStarted() {
    await().atMost(timeout.toMillis(), TimeUnit.MILLISECONDS).until(this::isStarted);
  }

  public void stop() {
    stop.countDown();
  }
}
