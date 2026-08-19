/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.apache.shiro.authc.AuthenticationToken;

/**
 * Supports {@link ReverseProxyAuthenticationFilter} and encodes the remote username in a form suitable for login.
 */
@SuppressWarnings("serial")
public class ReverseProxyAuthenticationToken
    implements AuthenticationToken
{
  private final String username;

  public ReverseProxyAuthenticationToken(String username) {
    this.username = username;
  }

  public String getUsername() {
    return username;
  }

  @Override
  public Object getPrincipal() {
    return getUsername();
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public String toString() {
    return getClass().getName() + " - " + getPrincipal();
  }
}
