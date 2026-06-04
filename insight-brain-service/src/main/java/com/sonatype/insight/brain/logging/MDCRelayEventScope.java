/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.logging;

import org.slf4j.MDC;

/**
 * Try-with-resources MDC scope that attaches a relay event id to log lines emitted while
 * processing one event. Restores any prior value on close so nested scopes are well-behaved.
 */
public class MDCRelayEventScope
    implements AutoCloseable
{
  public static final String RELAY_EVENT_ID = "relayEventId";

  private final String previousValue;

  public static MDCRelayEventScope forEventId(final String eventId) {
    return new MDCRelayEventScope(eventId);
  }

  private MDCRelayEventScope(final String eventId) {
    this.previousValue = MDC.get(RELAY_EVENT_ID);
    if (eventId == null) {
      MDC.remove(RELAY_EVENT_ID);
    }
    else {
      MDC.put(RELAY_EVENT_ID, eventId);
    }
  }

  @Override
  public void close() {
    if (previousValue == null) {
      MDC.remove(RELAY_EVENT_ID);
    }
    else {
      MDC.put(RELAY_EVENT_ID, previousValue);
    }
  }
}
