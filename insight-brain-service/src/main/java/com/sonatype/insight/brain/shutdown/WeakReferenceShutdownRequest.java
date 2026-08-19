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
  protected WeakReferenceShutdownRequest(final T item, final int order, final String origin) {
    super(new WeakReference<>(item), order, origin);
  }

  @Override
  public Future<?> execute(final ExecutorService executorService) throws Exception {
    T item = getItem().get();
    return item == null ? CompletableFuture.completedFuture(null) : execute(executorService, item);
  }

  /**
   * Convenience method that has already checked the {@link WeakReference} referent is not null.<br/>
   * <br/>
   * See
   * {@link ShutdownRequest#execute(ExecutorService)}.
   */
  public Future<?> execute(
      @SuppressWarnings("unused") final ExecutorService executorService,
      @SuppressWarnings("unused") final T item) throws Exception
  {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public boolean isValid() {
    return getItem().get() != null;
  }

  @Override
  public String getItemToString() {
    T item = getItem().get();
    return item == null ? null : super.getItemToString(item);
  }
}
