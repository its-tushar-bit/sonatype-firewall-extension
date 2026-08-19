/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Auth0Config
{
  @Valid
  @JsonProperty
  private String domain;

  @Valid
  @JsonProperty
  private String customDomain;

  @Valid
  @JsonProperty
  private String clientId;

  @Valid
  @JsonProperty
  private String clientSecret;

  public String getDomain() {
    return domain;
  }

  public void setDomain(final String domain) {
    this.domain = domain;
  }

  public String getCustomDomain() {
    return customDomain;
  }

  public void setCustomDomain(final String customDomain) {
    this.customDomain = customDomain;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(final String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(final String clientSecret) {
    this.clientSecret = clientSecret;
  }
}
