/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.common.metering;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.micrometer.core.instrument.Tags;

public class TaggedRunnableFuture<V>
    implements RunnableFuture<V>, HasTags
{
  private final RunnableFuture<V> runnableFuture;

  private final Tags tags;

  public TaggedRunnableFuture(final RunnableFuture<V> runnableFuture, final Tags tags) {
    this.runnableFuture = runnableFuture;
    this.tags = tags;
  }

  @Override
  public void run() {
    runnableFuture.run();
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    return runnableFuture.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return runnableFuture.isCancelled();
  }

  @Override
  public boolean isDone() {
    return runnableFuture.isDone();
  }

  @Override
  public V get() throws InterruptedException, ExecutionException {
    return runnableFuture.get();
  }

  @Override
  public V get(
      final long timeout,
      final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
  {
    return runnableFuture.get(timeout, unit);
  }

  @Override
  public Tags getTags() {
    return tags;
  }
}
