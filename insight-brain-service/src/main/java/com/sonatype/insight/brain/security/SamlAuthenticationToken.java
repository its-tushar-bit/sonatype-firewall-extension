/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Objects;

import org.apache.shiro.authc.AuthenticationToken;
import org.keycloak.adapters.saml.SamlPrincipal;

@SuppressWarnings("serial")
public class SamlAuthenticationToken
    implements AuthenticationToken
{
  private final SamlPrincipal samlPrincipal;

  public SamlAuthenticationToken(SamlPrincipal samlPrincipal) {
    this.samlPrincipal = Objects.requireNonNull(samlPrincipal);
  }

  public SamlPrincipal getSamlPrincipal() {
    return samlPrincipal;
  }

  @Override
  public Object getPrincipal() {
    return getSamlPrincipal();
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public String toString() {
    return getClass().getName() + " - " + samlPrincipal.getName();
  }
}
