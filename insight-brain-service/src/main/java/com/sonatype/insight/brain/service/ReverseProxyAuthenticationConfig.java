/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration bean for the REMOTE_USER based SSO integration.
 */
public class ReverseProxyAuthenticationConfig
{
  @NotNull
  @JsonProperty
  private boolean enabled;

  @NotNull
  @JsonProperty
  private String usernameHeader = "REMOTE_USER";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getUsernameHeader() {
    return usernameHeader;
  }

  public void setUsernameHeader(String usernameHeader) {
    this.usernameHeader = usernameHeader;
  }
}
