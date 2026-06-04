/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * Thread-safe accumulators for relay poller stats. Writers are the poller thread(s); readers are
 * telemetry and the support-zip. Counter reads via {@link #snapshot()} are non-resetting; the
 * telemetry path uses {@link #snapshotAndReset()} to drain counters at each tick.
 */
@Named
@Singleton
public class RelayPollerCounters
{
  private final AtomicLong eventsPolled = new AtomicLong();

  private final AtomicLong eventsProcessed = new AtomicLong();

  private final AtomicLong eventsUnmatched = new AtomicLong();

  private final AtomicLong eventsDuplicate = new AtomicLong();

  private final AtomicLong pollErrors = new AtomicLong();

  private final AtomicLong ackErrors = new AtomicLong();

  private final AtomicLong eventsProcessingErrors = new AtomicLong();

  private final AtomicBoolean fallbackActive = new AtomicBoolean();

  private final AtomicReference<Instant> lastSuccessfulPollAt = new AtomicReference<>();

  public void incEventsPolled(long delta) {
    if (delta > 0) {
      eventsPolled.addAndGet(delta);
    }
  }

  public void incEventsProcessed() {
    eventsProcessed.incrementAndGet();
  }

  public void incEventsUnmatched() {
    eventsUnmatched.incrementAndGet();
  }

  public void incEventsDuplicate() {
    eventsDuplicate.incrementAndGet();
  }

  public void incPollErrors() {
    pollErrors.incrementAndGet();
  }

  public void incAckErrors() {
    ackErrors.incrementAndGet();
  }

  public void incEventsProcessingErrors() {
    eventsProcessingErrors.incrementAndGet();
  }

  public void setFallbackActive(boolean active) {
    fallbackActive.set(active);
  }

  public void setLastSuccessfulPollAt(Instant instant) {
    lastSuccessfulPollAt.set(instant);
  }

  /**
   * Returns a consistent counter snapshot without resetting any field.
   */
  public Snapshot snapshot() {
    return new Snapshot(
        eventsPolled.get(),
        eventsProcessed.get(),
        eventsUnmatched.get(),
        eventsDuplicate.get(),
        pollErrors.get(),
        ackErrors.get(),
        eventsProcessingErrors.get(),
        fallbackActive.get(),
        lastSuccessfulPollAt.get());
  }

  /**
   * Reads and resets each incrementing counter. Each {@code getAndSet(0)} is individually
   * atomic, but the snapshot is not globally atomic across all counters: an increment racing
   * with a snapshot read may end up in the next tick. {@code fallbackActive} and
   * {@code lastSuccessfulPollAt} are state, not counters, and are left intact.
   */
  public Snapshot snapshotAndReset() {
    return new Snapshot(
        eventsPolled.getAndSet(0L),
        eventsProcessed.getAndSet(0L),
        eventsUnmatched.getAndSet(0L),
        eventsDuplicate.getAndSet(0L),
        pollErrors.getAndSet(0L),
        ackErrors.getAndSet(0L),
        eventsProcessingErrors.getAndSet(0L),
        fallbackActive.get(),
        lastSuccessfulPollAt.get());
  }

  public record Snapshot(
      long eventsPolled,
      long eventsProcessed,
      long eventsUnmatched,
      long eventsDuplicate,
      long pollErrors,
      long ackErrors,
      long eventsProcessingErrors,
      boolean fallbackActive,
      Instant lastSuccessfulPollAt)
  {
  }
}
