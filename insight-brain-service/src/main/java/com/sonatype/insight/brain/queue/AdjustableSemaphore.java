/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.queue;

import java.util.concurrent.Semaphore;

/**
 * A {@link Semaphore} whose maximum permit count can be changed at runtime.
 * <p>
 * The CM consumer's injected-executor path uses a semaphore as the concurrency cap on worker
 * drain-loops. Operators can resize {@code continuousMonitoringWorkerThreads} per tenant; the
 * change must take effect without restarting the JVM or recreating the executor. Standard
 * {@link Semaphore} has no resize hook; this class adds one.
 * <p>
 * <strong>Resize semantics:</strong>
 * <ul>
 * <li><strong>Increase</strong> — {@code release(delta)} extra permits. Already-running workers
 * are unaffected; the next {@code dispatch} that asks for a permit will see the new count.</li>
 * <li><strong>Decrease</strong> — call {@link Semaphore#reducePermits(int)} for {@code -delta}.
 * Running workers are not interrupted; they keep their permits and release normally when they
 * exit. New worker spawns are gated at the lower count. Eventual permit count converges
 * correctly because every worker's {@code release()} pairs with the {@code tryAcquire()} that
 * spawned it.</li>
 * </ul>
 * <p>
 * Thread-safety: {@link #resize} is synchronized so two concurrent config-change callbacks cannot
 * race on the delta calculation. The {@code acquire/release/tryAcquire/availablePermits} methods
 * delegate to {@link Semaphore} and are already thread-safe.
 */
public class AdjustableSemaphore
{
  /**
   * Subclass that exposes {@link Semaphore#reducePermits(int)} to {@link AdjustableSemaphore}.
   * The base method is {@code protected}; this lets the enclosing class call it on resize.
   */
  private static final class ResizableSemaphore
      extends Semaphore
  {
    ResizableSemaphore(final int permits) {
      super(permits);
    }

    @Override
    protected void reducePermits(final int reduction) {
      super.reducePermits(reduction);
    }
  }

  private final ResizableSemaphore inner;

  private int maxPermits;

  public AdjustableSemaphore(final int permits) {
    if (permits < 1) {
      throw new IllegalArgumentException("permits must be >= 1: " + permits);
    }
    this.inner = new ResizableSemaphore(permits);
    this.maxPermits = permits;
  }

  /**
   * Returns the current maximum permit count (as last set by the constructor or {@link #resize}).
   * Note this is NOT the same as {@link #availablePermits()} which reflects free permits only.
   */
  public synchronized int getMaxPermits() {
    return maxPermits;
  }

  /**
   * Resizes the maximum permit count to {@code newPermits}. A larger value releases extra permits;
   * a smaller value reduces the cap. Running workers are never interrupted; a decrease simply
   * gates new dispatches at the lower count and converges as running workers finish.
   *
   * @throws IllegalArgumentException if {@code newPermits} is less than 1
   */
  public synchronized void resize(final int newPermits) {
    if (newPermits < 1) {
      throw new IllegalArgumentException("newPermits must be >= 1: " + newPermits);
    }
    int delta = newPermits - maxPermits;
    if (delta > 0) {
      inner.release(delta);
    }
    else if (delta < 0) {
      inner.reducePermits(-delta);
    }
    maxPermits = newPermits;
  }

  public boolean tryAcquire() {
    return inner.tryAcquire();
  }

  /**
   * Blocks until a permit is available. Used by tests and callers that explicitly want to wait
   * (production dispatch uses {@link #tryAcquire()} non-blocking).
   *
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  public void acquire() throws InterruptedException {
    inner.acquire();
  }

  public void release() {
    inner.release();
  }

  public int availablePermits() {
    return inner.availablePermits();
  }
}
