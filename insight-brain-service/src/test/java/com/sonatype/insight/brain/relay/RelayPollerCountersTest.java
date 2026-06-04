/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.time.Instant;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RelayPollerCountersTest
{
  @Test
  public void initialSnapshotIsZeroAndUnset() {
    RelayPollerCounters counters = new RelayPollerCounters();

    RelayPollerCounters.Snapshot snapshot = counters.snapshot();

    assertThat(snapshot.eventsPolled()).isZero();
    assertThat(snapshot.eventsProcessed()).isZero();
    assertThat(snapshot.eventsUnmatched()).isZero();
    assertThat(snapshot.eventsDuplicate()).isZero();
    assertThat(snapshot.pollErrors()).isZero();
    assertThat(snapshot.ackErrors()).isZero();
    assertThat(snapshot.eventsProcessingErrors()).isZero();
    assertThat(snapshot.fallbackActive()).isFalse();
    assertThat(snapshot.lastSuccessfulPollAt()).isNull();
  }

  @Test
  public void incrementsAccumulate() {
    RelayPollerCounters counters = new RelayPollerCounters();

    counters.incEventsPolled(5);
    counters.incEventsPolled(2);
    counters.incEventsProcessed();
    counters.incEventsProcessed();
    counters.incEventsProcessed();
    counters.incEventsUnmatched();
    counters.incEventsDuplicate();
    counters.incPollErrors();
    counters.incAckErrors();
    counters.incAckErrors();
    counters.incEventsProcessingErrors();
    counters.incEventsProcessingErrors();
    counters.incEventsProcessingErrors();

    RelayPollerCounters.Snapshot snapshot = counters.snapshot();

    assertThat(snapshot.eventsPolled()).isEqualTo(7);
    assertThat(snapshot.eventsProcessed()).isEqualTo(3);
    assertThat(snapshot.eventsUnmatched()).isEqualTo(1);
    assertThat(snapshot.eventsDuplicate()).isEqualTo(1);
    assertThat(snapshot.pollErrors()).isEqualTo(1);
    assertThat(snapshot.ackErrors()).isEqualTo(2);
    assertThat(snapshot.eventsProcessingErrors()).isEqualTo(3);
  }

  @Test
  public void incEventsPolledIgnoresNonPositiveDeltas() {
    RelayPollerCounters counters = new RelayPollerCounters();

    counters.incEventsPolled(0);
    counters.incEventsPolled(-3);

    assertThat(counters.snapshot().eventsPolled()).isZero();
  }

  @Test
  public void snapshotIsNonResetting() {
    RelayPollerCounters counters = new RelayPollerCounters();
    counters.incEventsPolled(4);

    counters.snapshot();
    counters.snapshot();

    assertThat(counters.snapshot().eventsPolled()).isEqualTo(4);
  }

  @Test
  public void snapshotAndResetClearsCounters() {
    RelayPollerCounters counters = new RelayPollerCounters();
    counters.incEventsPolled(10);
    counters.incEventsProcessed();
    counters.incEventsUnmatched();
    counters.incEventsDuplicate();
    counters.incPollErrors();
    counters.incAckErrors();
    counters.incEventsProcessingErrors();

    RelayPollerCounters.Snapshot first = counters.snapshotAndReset();
    RelayPollerCounters.Snapshot second = counters.snapshotAndReset();

    assertThat(first.eventsPolled()).isEqualTo(10);
    assertThat(first.eventsProcessed()).isEqualTo(1);
    assertThat(first.eventsProcessingErrors()).isEqualTo(1);
    assertThat(second.eventsPolled()).isZero();
    assertThat(second.eventsProcessed()).isZero();
    assertThat(second.eventsUnmatched()).isZero();
    assertThat(second.eventsDuplicate()).isZero();
    assertThat(second.pollErrors()).isZero();
    assertThat(second.ackErrors()).isZero();
    assertThat(second.eventsProcessingErrors()).isZero();
  }

  @Test
  public void fallbackActiveAndLastPollSurviveResets() {
    RelayPollerCounters counters = new RelayPollerCounters();
    Instant marker = Instant.parse("2024-01-02T03:04:05Z");
    counters.setFallbackActive(true);
    counters.setLastSuccessfulPollAt(marker);
    counters.incEventsPolled(5);

    RelayPollerCounters.Snapshot snapshot = counters.snapshotAndReset();

    assertThat(snapshot.fallbackActive()).isTrue();
    assertThat(snapshot.lastSuccessfulPollAt()).isEqualTo(marker);
    // counters reset, but state fields preserved on the live holder
    RelayPollerCounters.Snapshot after = counters.snapshot();
    assertThat(after.fallbackActive()).isTrue();
    assertThat(after.lastSuccessfulPollAt()).isEqualTo(marker);
    assertThat(after.eventsPolled()).isZero();
  }

  @Test
  public void setFallbackActiveTogglesBackToFalse() {
    RelayPollerCounters counters = new RelayPollerCounters();
    counters.setFallbackActive(true);
    counters.setFallbackActive(false);

    assertThat(counters.snapshot().fallbackActive()).isFalse();
  }
}
