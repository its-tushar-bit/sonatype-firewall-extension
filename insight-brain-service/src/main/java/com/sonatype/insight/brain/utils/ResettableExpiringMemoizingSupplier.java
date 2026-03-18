/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This is a copy of {@link com.google.common.base.Suppliers.ExpiringMemoizingSupplier}
 * https://github.com/google/guava/blob/v33.1.0/guava/src/com/google/common/base/Suppliers.java#L271-L322 with a reset
 * method to allow us to have a cache with a single value that can be reset
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
    if (nanos == 0 || now - nanos >= 0) {
      synchronized (this) {
        if (nanos == expirationNanos) { // recheck for lost race
          T oldValue = value;
          T newValue = delegate.get();
          value = newValue;
          if (oldValue != newValue) {
            changed = true;
          }
          resetExpiration(now);
        }
      }
    }
    if (onChange != null && changed) {
      onChange.accept(value);
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

  // Visible for testing
  public synchronized void reset() {
    expirationNanos = 0;
  }

  // Visible for testing
  public T getMemoizedValue() {
    return value;
  }

  /**
   * Manually set an updated value that will be returned for the next `duration` without ever calling the `delegate`.
   */
  public synchronized void setMemoizedValue(final T newValue) {
    resetExpiration();
    value = newValue;
  }
}
