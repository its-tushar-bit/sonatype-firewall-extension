/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.net.URI;

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

  /**
   * @since version 1.26.0
   */
  @NotNull
  @JsonProperty
  private boolean csrfProtectionDisabled;

  /**
   * @since 1.35.0
   */
  @JsonProperty
  private URI logoutUrl;

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

  public boolean isCsrfProtectionDisabled() {
    return csrfProtectionDisabled;
  }

  public void setCsrfProtectionDisabled(final boolean csrfProtectionDisabled) {
    this.csrfProtectionDisabled = csrfProtectionDisabled;
  }

  public URI getLogoutUrl() {
    return logoutUrl;
  }

  public void setLogoutUrl(final URI logoutUrl) {
    this.logoutUrl = logoutUrl;
  }
}
