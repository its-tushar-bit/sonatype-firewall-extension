/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Response from {@code GET /api/events}. The relay returns each {@link RelayEvent} with its SQS
 * receipt handle attached so the client can ack drained events without an extra lookup.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelayEventsResponse
{
  private List<RelayEvent> events;

  public RelayEventsResponse() {
  }

  public RelayEventsResponse(List<RelayEvent> events) {
    this.events = events;
  }

  public List<RelayEvent> getEvents() {
    return events;
  }

  public void setEvents(List<RelayEvent> events) {
    this.events = events;
  }
}
