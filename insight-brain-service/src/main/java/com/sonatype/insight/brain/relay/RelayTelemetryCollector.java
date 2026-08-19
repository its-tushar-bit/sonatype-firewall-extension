/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.util.Map;

import com.sonatype.insight.brain.telemetry.TelemetryCollector;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * Cluster-scoped telemetry for the SCM webhook relay poller. Drains the counters on each tick
 * and reports them under {@link TelemetryPurpose#SCM_RELAY_INTEGRATION}. When the
 * feature is disabled the counters are still zero so a missing-feature signal is observable.
 */
@Named
@Singleton
public class RelayTelemetryCollector
    implements TelemetryCollector
{
  public static final String RELAY_EVENTS_POLLED = "relay_events_polled";

  public static final String RELAY_EVENTS_PROCESSED = "relay_events_processed";

  public static final String RELAY_EVENTS_UNMATCHED = "relay_events_unmatched";

  public static final String RELAY_EVENTS_DUPLICATE = "relay_events_duplicate";

  public static final String RELAY_POLL_ERRORS = "relay_poll_errors";

  public static final String RELAY_ACK_ERRORS = "relay_ack_errors";

  public static final String RELAY_PROCESSING_ERRORS = "relay_processing_errors";

  public static final String RELAY_FALLBACK_ACTIVE = "relay_fallback_active";

  private final RelayPollerCounters counters;

  @Inject
  public RelayTelemetryCollector(RelayPollerCounters counters) {
    this.counters = counters;
  }

  @Override
  public TelemetryData collectData() {
    RelayPollerCounters.Snapshot snapshot = counters.snapshotAndReset();
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SCM_RELAY_INTEGRATION);
    Map<String, Object> attributes = telemetryData.getAttributes();
    attributes.put(RELAY_EVENTS_POLLED, String.valueOf(snapshot.eventsPolled()));
    attributes.put(RELAY_EVENTS_PROCESSED, String.valueOf(snapshot.eventsProcessed()));
    attributes.put(RELAY_EVENTS_UNMATCHED, String.valueOf(snapshot.eventsUnmatched()));
    attributes.put(RELAY_EVENTS_DUPLICATE, String.valueOf(snapshot.eventsDuplicate()));
    attributes.put(RELAY_POLL_ERRORS, String.valueOf(snapshot.pollErrors()));
    attributes.put(RELAY_ACK_ERRORS, String.valueOf(snapshot.ackErrors()));
    attributes.put(RELAY_PROCESSING_ERRORS, String.valueOf(snapshot.eventsProcessingErrors()));
    attributes.put(RELAY_FALLBACK_ACTIVE, String.valueOf(snapshot.fallbackActive()));
    return telemetryData;
  }

  @Override
  public boolean isClusterTelemetry() {
    return true;
  }
}
