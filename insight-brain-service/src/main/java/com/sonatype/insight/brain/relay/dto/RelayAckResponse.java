/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay.dto;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Response from {@code POST /api/events/ack}; reports which receipt handles the relay was able
 * to delete from SQS and which it could not. Failed entries are logged by the poller but do not
 * abort the polling cycle.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelayAckResponse
{
  private List<String> acknowledged;

  private List<String> failed;

  public List<String> getAcknowledged() {
    return acknowledged != null ? acknowledged : Collections.emptyList();
  }

  public void setAcknowledged(List<String> acknowledged) {
    this.acknowledged = acknowledged;
  }

  public List<String> getFailed() {
    return failed != null ? failed : Collections.emptyList();
  }

  public void setFailed(List<String> failed) {
    this.failed = failed;
  }
}
