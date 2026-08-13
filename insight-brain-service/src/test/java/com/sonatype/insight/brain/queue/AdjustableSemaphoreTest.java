/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.queue;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link AdjustableSemaphore}. Covers the resize contract used by
 * {@code handleConfigurationChanged} on the injected-executor path to live-tune
 * {@code continuousMonitoringWorkerThreads} without restarting the CM consumer.
 */
public class AdjustableSemaphoreTest
{
  @Test
  public void constructor_setsMaxAndAvailableEqual() {
    AdjustableSemaphore s = new AdjustableSemaphore(4);
    assertThat(s.getMaxPermits()).isEqualTo(4);
    assertThat(s.availablePermits()).isEqualTo(4);
  }

  @Test
  public void constructor_rejectsNegativePermits() {
    assertThatThrownBy(() -> new AdjustableSemaphore(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_rejectsZeroPermits() {
    // Symmetric with resize(0): a zero-permit semaphore would silently halt all dispatch with
    // no obvious cause, so reject at construction the same way resize does.
    assertThatThrownBy(() -> new AdjustableSemaphore(0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void resize_increasesAvailablePermitsImmediately() {
    AdjustableSemaphore s = new AdjustableSemaphore(2);
    s.resize(8);
    assertThat(s.getMaxPermits()).isEqualTo(8);
    assertThat(s.availablePermits()).isEqualTo(8);
  }

  @Test
  public void resize_decreaseReducesMaxAndAvailable() {
    AdjustableSemaphore s = new AdjustableSemaphore(8);
    s.resize(2);
    assertThat(s.getMaxPermits()).isEqualTo(2);
    assertThat(s.availablePermits()).isEqualTo(2);
  }

  @Test
  public void resize_decreaseWithHeldPermits_doesNotInterruptHolders() throws Exception {
    AdjustableSemaphore s = new AdjustableSemaphore(4);
    // Simulate 3 in-flight workers.
    s.acquire();
    s.acquire();
    s.acquire();
    assertThat(s.availablePermits()).isEqualTo(1);

    // Decrease to 2: the 3 already-acquired permits stand; only the 1 free permit can be reduced.
    s.resize(2);

    assertThat(s.getMaxPermits()).isEqualTo(2);
    // We had 1 free → after reducePermits(2) the count went to -1; this is the standard
    // "borrowed permits" semantic of Semaphore.reducePermits. availablePermits() returns the
    // raw count, which can be negative until in-flight workers release.
    assertThat(s.availablePermits()).isEqualTo(-1);

    // Release one held permit — count goes from -1 to 0; still no new dispatch allowed.
    s.release();
    assertThat(s.availablePermits()).isEqualTo(0);
    // Release the second — now 1 permit free (within the new cap of 2).
    s.release();
    assertThat(s.availablePermits()).isEqualTo(1);
    // Release the third — 2 permits free (at the new cap).
    s.release();
    assertThat(s.availablePermits()).isEqualTo(2);
  }

  @Test
  public void resize_sameValue_isNoOp() {
    AdjustableSemaphore s = new AdjustableSemaphore(4);
    s.resize(4);
    assertThat(s.getMaxPermits()).isEqualTo(4);
    assertThat(s.availablePermits()).isEqualTo(4);
  }

  @Test
  public void resize_rejectsNegative() {
    AdjustableSemaphore s = new AdjustableSemaphore(4);
    assertThatThrownBy(() -> s.resize(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void resize_rejectsZero() {
    // resize(0) would silently halt dispatch; reject so the caller learns about the bad value
    // instead of debugging mysteriously-stuck workers.
    AdjustableSemaphore s = new AdjustableSemaphore(4);
    assertThatThrownBy(() -> s.resize(0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void tryAcquireAndRelease_roundtrip() {
    AdjustableSemaphore s = new AdjustableSemaphore(2);
    assertThat(s.tryAcquire()).isTrue();
    assertThat(s.tryAcquire()).isTrue();
    assertThat(s.tryAcquire()).isFalse();
    s.release();
    assertThat(s.tryAcquire()).isTrue();
  }
}
