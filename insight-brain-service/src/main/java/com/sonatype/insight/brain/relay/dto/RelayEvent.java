/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * One normalized event returned by the relay's {@code GET /api/events} endpoint. The
 * {@code receiptHandle} comes back inline on each event (the relay flattens the SQS receipt
 * onto the event for simpler client code) and is the value the IQ poller has to send to
 * {@code /api/events/ack} once the event is durably handled.
 *
 * <p>
 * Provider-specific details are kept in {@link #payload} as a free-form map; mapping logic
 * lives in {@code RelayEventMapper} so this DTO stays decoupled from policy.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelayEvent
{
  /**
   * Standard event types produced by the relay. Anything outside this set is logged and skipped
   * by the mapper rather than failing the whole poll cycle.
   */
  public static final String TYPE_PULL_REQUEST_OPENED = "pull_request_opened";

  public static final String TYPE_PULL_REQUEST_UPDATED = "pull_request_updated";

  public static final String TYPE_PULL_REQUEST_CLOSED = "pull_request_closed";

  public static final String TYPE_PUSH = "push";

  private String eventId;

  private String provider;

  private String eventType;

  private String repositoryUrl;

  private String timestamp;

  private String receiptHandle;

  private Map<String, Object> payload;

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getRepositoryUrl() {
    return repositoryUrl;
  }

  public void setRepositoryUrl(String repositoryUrl) {
    this.repositoryUrl = repositoryUrl;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public String getReceiptHandle() {
    return receiptHandle;
  }

  public void setReceiptHandle(String receiptHandle) {
    this.receiptHandle = receiptHandle;
  }

  public Map<String, Object> getPayload() {
    return payload;
  }

  public void setPayload(Map<String, Object> payload) {
    this.payload = payload;
  }
}
