/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Response body for {@code GET /api/v2/sourceControl/relayWebhookSecret}; returned only when a
 * PAT-mode relay registration exists. Hidden by 404/412 responses otherwise. Carries the
 * decrypted per-customer HMAC signing secret so admins can paste it into the SCM provider's
 * webhook configuration without DB or master-key access.
 */
@JsonInclude(Include.NON_NULL)
public class RelayWebhookSecretResponse
{
  private String webhookSecret;

  public RelayWebhookSecretResponse() {
  }

  public RelayWebhookSecretResponse(String webhookSecret) {
    this.webhookSecret = webhookSecret;
  }

  public String getWebhookSecret() {
    return webhookSecret;
  }

  public void setWebhookSecret(String webhookSecret) {
    this.webhookSecret = webhookSecret;
  }
}
