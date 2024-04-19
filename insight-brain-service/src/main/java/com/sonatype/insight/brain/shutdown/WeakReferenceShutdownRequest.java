/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.lang.ref.WeakReference;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public abstract class WeakReferenceShutdownRequest<T>
    extends AbstractShutdownRequest<WeakReference<T>>
{
  protected WeakReferenceShutdownRequest(final WeakReference<T> item, final int order) {
    super(item, order);
  }

  @Override
  public Future<?> execute(final ExecutorService executorService) throws Exception {
    T item = getItem().get();
    if (item != null) {
      return execute(executorService, item);
    }
    return CompletableFuture.completedFuture(null);
  }

  /**
   * Convenience method that has already checked the {@link WeakReference} referent is not null.<br/><br/>See
   * {@link ShutdownRequest#execute(ExecutorService)}.
   */
  public Future<?> execute(final ExecutorService executorService, final T item) throws Exception {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public boolean isValid() {
    return getItem().get() != null;
  }
}
