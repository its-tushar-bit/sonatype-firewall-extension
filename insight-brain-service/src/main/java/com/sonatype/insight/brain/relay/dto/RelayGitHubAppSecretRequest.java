/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Body for {@code POST /api/github-app/webhook-secret}. Carries the App-level HMAC secret the
 * customer copied into the GitHub App settings; the relay encrypts it under the customer's KMS
 * key and accepts the previous secret for a short rotation grace window.
 */
@JsonInclude(Include.NON_NULL)
public class RelayGitHubAppSecretRequest
{
  private String webhookSecret;

  public RelayGitHubAppSecretRequest() {
  }

  public RelayGitHubAppSecretRequest(String webhookSecret) {
    this.webhookSecret = webhookSecret;
  }

  public String getWebhookSecret() {
    return webhookSecret;
  }

  public void setWebhookSecret(String webhookSecret) {
    this.webhookSecret = webhookSecret;
  }
}
