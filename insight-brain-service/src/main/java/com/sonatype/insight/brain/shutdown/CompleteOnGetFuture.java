/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.utils.CheckedRunnable;

/**
 * A CompletableFuture that, when {@link Future#get()} is called, executes some synchronous logic resulting in the
 * Future's completion.
 */
public class CompleteOnGetFuture<T>
    extends CompletableFuture<T>
{
  private final CheckedRunnable checkedRunnable;

  public CompleteOnGetFuture(final CheckedRunnable checkedRunnable) {
    this.checkedRunnable = checkedRunnable;
  }

  @Override
  public T get() throws InterruptedException, CancellationException, ExecutionException {
    if (isCancelled()) {
      throw new CancellationException();
    }

    if (!isDone()) {
      try {
        checkedRunnable.run();
      }
      catch (Exception e) {
        completeExceptionally(e);
      }

      // note: no-op if already completed by the completeExceptionally block above
      complete(null);
    }

    return super.get();
  }

  @Override
  public T get(final long timeout, final TimeUnit timeUnit) {
    throw new UnsupportedOperationException();
  }
}
