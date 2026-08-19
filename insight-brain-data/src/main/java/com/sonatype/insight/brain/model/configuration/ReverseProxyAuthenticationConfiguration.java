/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.138
 */
@Entity
@Table(name = "reverse_proxy_authentication_configuration")
public class ReverseProxyAuthenticationConfiguration
    implements HasStringId
{
  public static final String DEFAULT_USERNAME_HEADER = "REMOTE_USER";

  @Id
  @Column(name = "reverse_proxy_authentication_configuration_id")
  private String id;

  @Column(name = "enabled")
  private boolean enabled;

  @Column(name = "username_header")
  private String usernameHeader = DEFAULT_USERNAME_HEADER;

  @Column(name = "csrf_protection_disabled")
  private boolean csrfProtectionDisabled;

  @Column(name = "logout_url")
  private String logoutUrl;

  public ReverseProxyAuthenticationConfiguration() {
  }

  public ReverseProxyAuthenticationConfiguration(
      boolean enabled,
      String usernameHeader,
      boolean csrfProtectionDisabled,
      String logoutUrl)
  {
    this.enabled = enabled;
    this.usernameHeader = usernameHeader;
    this.csrfProtectionDisabled = csrfProtectionDisabled;
    this.logoutUrl = logoutUrl;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

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

  public void setCsrfProtectionDisabled(boolean csrfProtectionDisabled) {
    this.csrfProtectionDisabled = csrfProtectionDisabled;
  }

  public String getLogoutUrl() {
    return logoutUrl;
  }

  public void setLogoutUrl(String logoutUrl) {
    this.logoutUrl = logoutUrl;
  }
}
