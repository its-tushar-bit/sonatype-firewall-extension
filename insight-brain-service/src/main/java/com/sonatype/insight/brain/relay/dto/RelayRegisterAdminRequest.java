/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Optional request body for the admin re-register endpoint. When both fields are present,
 * the request is treated as a GitHub App registration; otherwise the PAT path is used.
 */
public class RelayRegisterAdminRequest
{
  private String installationId;

  private String webhookSecret;

  public RelayRegisterAdminRequest() {
  }

  @JsonProperty
  public String getInstallationId() {
    return installationId;
  }

  public void setInstallationId(String installationId) {
    this.installationId = installationId;
  }

  @JsonProperty
  public String getWebhookSecret() {
    return webhookSecret;
  }

  public void setWebhookSecret(String webhookSecret) {
    this.webhookSecret = webhookSecret;
  }
}
