/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.common.cache;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A TTL-based memoizing supplier with reset capability.
 * <p>
 * Based on {@code com.google.common.base.Suppliers.ExpiringMemoizingSupplier} from
 * <a href="https://github.com/google/guava/blob/v33.1.0/guava/src/com/google/common/base/Suppliers.java#L271-L322">
 * Guava</a>, with additional {@link #reset()} and {@link #setMemoizedValue(Object)} methods to support
 * eager cache invalidation and manual value injection.
 * </p>
 * <p>
 * Thread safety is achieved through double-checked locking on {@link #get()} and synchronization on
 * mutating methods.
 * </p>
 *
 * @param <T> the type of the cached value
 */
public class ResettableExpiringMemoizingSupplier<T>
    implements Supplier<T>
{
  private final Supplier<T> delegate;

  private final long durationNanos;

  private final Consumer<T> onChange;

  private volatile long expirationNanos;

  private volatile T value;

  public ResettableExpiringMemoizingSupplier(final Supplier<T> delegate, final Duration duration) {
    this(delegate, duration, null);
  }

  public ResettableExpiringMemoizingSupplier(
      final Supplier<T> delegate,
      final Duration duration,
      final Consumer<T> onChange)
  {
    this.delegate = delegate;
    this.durationNanos = duration.toNanos();
    this.onChange = onChange;
  }

  @Override
  public T get() {
    long nanos = expirationNanos;
    long now = nanoTime();
    boolean changed = false;
    T changedValue = null;
    if (nanos == 0 || now - nanos >= 0) {
      synchronized (this) {
        if (nanos == expirationNanos) { // recheck for lost race
          T oldValue = value;
          T newValue = delegate.get();
          value = newValue;
          if (oldValue != newValue) {
            changed = true;
            changedValue = newValue;
          }
          resetExpiration(now);
        }
      }
    }
    if (onChange != null && changed) {
      onChange.accept(changedValue);
    }
    return value;
  }

  private synchronized void resetExpiration() {
    resetExpiration(nanoTime());
  }

  private synchronized void resetExpiration(final long now) {
    long nanos = now + durationNanos;
    // In the very unlikely event that nanos is 0, set it to 1;
    // no one will notice 1 ns of tardiness.
    expirationNanos = (nanos == 0) ? 1 : nanos;
  }

  // Visible for testing
  long nanoTime() {
    return System.nanoTime();
  }

  /**
   * Resets the cache, causing the next call to {@link #get()} to re-invoke the delegate.
   */
  public synchronized void reset() {
    expirationNanos = 0;
  }

  /**
   * Returns the currently memoized value without triggering a refresh.
   */
  public T getMemoizedValue() {
    return value;
  }

  /**
   * Manually set an updated value that will be returned for the next {@code duration} without ever calling the
   * delegate.
   */
  public synchronized void setMemoizedValue(final T newValue) {
    resetExpiration();
    value = newValue;
  }
}
