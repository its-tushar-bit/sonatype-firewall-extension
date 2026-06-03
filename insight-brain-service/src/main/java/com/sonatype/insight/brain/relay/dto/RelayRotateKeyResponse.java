/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Response from {@code POST /api/rotate-key} on the relay. {@code apiKey} is the new IQ→relay
 * api key plaintext (returned exactly once); {@code previousKeyExpiresAt} is the ISO-8601
 * instant at which the previous key stops being accepted (5-minute grace window).
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelayRotateKeyResponse
{
  private String apiKey;

  private String previousKeyExpiresAt;

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getPreviousKeyExpiresAt() {
    return previousKeyExpiresAt;
  }

  public void setPreviousKeyExpiresAt(String previousKeyExpiresAt) {
    this.previousKeyExpiresAt = previousKeyExpiresAt;
  }
}
