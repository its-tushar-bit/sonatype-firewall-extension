/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public interface ShutdownRequest<T>
    extends Comparable<ShutdownRequest<?>>
{
  /**
   * @return the item that needs to be shut down.
   */
  T getItem();

  /**
   * @return the order of the shutdown request. Requests are handled in ascending order, and requests with the same
   *         order are handled simultaneously.
   */
  int getOrder();

  /**
   * @return the origin of the shutdown request.
   */
  String getOrigin();

  /**
   * Initiates the item shutdown via non-blocking actions and returns a {@link Future} where {@link Future#get()} will
   * block until the item shutdown is complete. This method should not block. A {@link CompleteOnGetFuture} should be
   * returned when waiting for the item shutdown can happen in the current thread.
   *
   * @param executorService an {@link ExecutorService} that should be used to execute any blocking item shutdown
   *          initiation actions in other threads.
   * @return a {@link Future} to indicate if the item shutdown is complete. {@link Future#get()} should block until the
   *         item shutdown is complete, ideally in the current thread using a {@link CompleteOnGetFuture}.
   */
  Future<?> execute(final ExecutorService executorService) throws Exception;

  /**
   * @return true if the request is valid. An invalid request can be ignored/removed.
   */
  default boolean isValid() {
    return true;
  }

  @Override
  default int compareTo(final ShutdownRequest<?> other) {
    return Long.compare(getOrder(), other.getOrder());
  }

  /**
   * See {@link ShutdownRequest#getItemToString(Object)}.
   */
  default String getItemToString() {
    return getItemToString(getItem());
  }

  /**
   * @param item the item to return a String representation for.
   * @return a basic representation matching {@link Object#toString()} to avoid showing too much information.
   */
  default String getItemToString(final Object item) {
    return item.getClass().getName() + "@" + Integer.toHexString(item.hashCode());
  }
}
