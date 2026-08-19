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
 * Response from {@code POST /api/rotate-webhook-secret} on the relay. {@code webhookSecret} is
 * the new HMAC secret plaintext (returned exactly once and meant to be pasted into the SCM
 * provider's webhook secret field); {@code previousSecretExpiresAt} is the ISO-8601 instant at
 * which the previous secret stops being accepted (5-minute grace window during which both
 * signatures verify so SCM-side reconfiguration does not drop deliveries).
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelayRotateWebhookSecretResponse
{
  private String webhookSecret;

  private String previousSecretExpiresAt;

  public String getWebhookSecret() {
    return webhookSecret;
  }

  public void setWebhookSecret(String webhookSecret) {
    this.webhookSecret = webhookSecret;
  }

  public String getPreviousSecretExpiresAt() {
    return previousSecretExpiresAt;
  }

  public void setPreviousSecretExpiresAt(String previousSecretExpiresAt) {
    this.previousSecretExpiresAt = previousSecretExpiresAt;
  }
}
