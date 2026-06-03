/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Response body for {@code GET /api/v2/sourceControl/relayWebhookUrl}; returned only when a
 * relay registration exists. Hidden by 404/412 responses otherwise.
 */
@JsonInclude(Include.NON_NULL)
public class RelayWebhookUrlResponse
{
  private String webhookUrl;

  public RelayWebhookUrlResponse() {
  }

  public RelayWebhookUrlResponse(String webhookUrl) {
    this.webhookUrl = webhookUrl;
  }

  public String getWebhookUrl() {
    return webhookUrl;
  }

  public void setWebhookUrl(String webhookUrl) {
    this.webhookUrl = webhookUrl;
  }
}
