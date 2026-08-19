/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Request body for {@code POST /api/events/ack}. The relay enforces an upper bound (currently
 * 100) on the number of receipt handles per call; callers must batch above that.
 */
@JsonInclude(Include.NON_NULL)
public class RelayAckRequest
{
  private List<String> receiptHandles;

  public RelayAckRequest() {
  }

  public RelayAckRequest(List<String> receiptHandles) {
    this.receiptHandles = receiptHandles;
  }

  public List<String> getReceiptHandles() {
    return receiptHandles;
  }

  public void setReceiptHandles(List<String> receiptHandles) {
    this.receiptHandles = receiptHandles;
  }
}
