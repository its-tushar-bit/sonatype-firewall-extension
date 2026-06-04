/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.util.Map;

import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RelayTelemetryCollectorTest
{
  @Test
  public void isClusterTelemetry() {
    RelayTelemetryCollector collector = new RelayTelemetryCollector(new RelayPollerCounters());

    assertThat(collector.isClusterTelemetry()).isTrue();
  }

  @Test
  public void zeroStateProducesAllZeroAttributes() {
    RelayTelemetryCollector collector = new RelayTelemetryCollector(new RelayPollerCounters());

    TelemetryData data = collector.collectData();

    assertThat(data.getPurpose()).isEqualTo(TelemetryPurpose.SCM_RELAY_INTEGRATION);
    Map<String, Object> attributes = data.getAttributes();
    assertThat(attributes).containsEntry(RelayTelemetryCollector.RELAY_EVENTS_POLLED, "0")
        .containsEntry(RelayTelemetryCollector.RELAY_EVENTS_PROCESSED, "0")
        .containsEntry(RelayTelemetryCollector.RELAY_EVENTS_UNMATCHED, "0")
        .containsEntry(RelayTelemetryCollector.RELAY_EVENTS_DUPLICATE, "0")
        .containsEntry(RelayTelemetryCollector.RELAY_POLL_ERRORS, "0")
        .containsEntry(RelayTelemetryCollector.RELAY_ACK_ERRORS, "0")
        .containsEntry(RelayTelemetryCollector.RELAY_PROCESSING_ERRORS, "0")
        .containsEntry(RelayTelemetryCollector.RELAY_FALLBACK_ACTIVE, "false");
  }

  @Test
  public void collectReadsCountersAndResetsThem() {
    RelayPollerCounters counters = new RelayPollerCounters();
    counters.incEventsPolled(7);
    counters.incEventsProcessed();
    counters.incEventsProcessed();
    counters.incEventsUnmatched();
    counters.incEventsDuplicate();
    counters.incPollErrors();
    counters.incAckErrors();
    counters.incEventsProcessingErrors();
    counters.setFallbackActive(true);
    RelayTelemetryCollector collector = new RelayTelemetryCollector(counters);

    TelemetryData first = collector.collectData();
    TelemetryData second = collector.collectData();

    assertThat(first.getAttributes())
        .containsEntry(RelayTelemetryCollector.RELAY_EVENTS_POLLED, "7")
        .containsEntry(RelayTelemetryCollector.RELAY_EVENTS_PROCESSED, "2")
        .containsEntry(RelayTelemetryCollector.RELAY_EVENTS_UNMATCHED, "1")
        .containsEntry(RelayTelemetryCollector.RELAY_EVENTS_DUPLICATE, "1")
        .containsEntry(RelayTelemetryCollector.RELAY_POLL_ERRORS, "1")
        .containsEntry(RelayTelemetryCollector.RELAY_ACK_ERRORS, "1")
        .containsEntry(RelayTelemetryCollector.RELAY_PROCESSING_ERRORS, "1")
        .containsEntry(RelayTelemetryCollector.RELAY_FALLBACK_ACTIVE, "true");
    // Second read must show every counter reset; fallback flag is state, not a counter, so it persists.
    assertThat(second.getAttributes())
        .containsEntry(RelayTelemetryCollector.RELAY_EVENTS_POLLED, "0")
        .containsEntry(RelayTelemetryCollector.RELAY_EVENTS_PROCESSED, "0")
        .containsEntry(RelayTelemetryCollector.RELAY_EVENTS_UNMATCHED, "0")
        .containsEntry(RelayTelemetryCollector.RELAY_EVENTS_DUPLICATE, "0")
        .containsEntry(RelayTelemetryCollector.RELAY_POLL_ERRORS, "0")
        .containsEntry(RelayTelemetryCollector.RELAY_ACK_ERRORS, "0")
        .containsEntry(RelayTelemetryCollector.RELAY_PROCESSING_ERRORS, "0")
        .containsEntry(RelayTelemetryCollector.RELAY_FALLBACK_ACTIVE, "true");
  }
}
