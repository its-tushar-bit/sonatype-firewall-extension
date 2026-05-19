/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.servlet.http.HttpServletRequest;

import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.servlet.FilterSamlSessionStore;
import org.keycloak.adapters.servlet.FilterSessionStore;
import org.keycloak.adapters.spi.HttpFacade;

public class SamlSessionStoreForRedirect
    extends FilterSamlSessionStore
{
  private final String redirect;

  SamlSessionStoreForRedirect(
      HttpServletRequest request,
      HttpFacade facade,
      int maxBuffer,
      SamlSessionIdMapper samlSessionIdMapper,
      SamlDeployment samlDeployment,
      String redirect)
  {
    super(request, facade, maxBuffer, samlSessionIdMapper, samlDeployment);
    this.redirect = redirect;
  }

  void refreshCachedSession() {
    // ShiroHttpServletRequest.getSession(false) clears its stale cached session when no live
    // session exists, which is what we need after stopping the pre-login session.
    request.getSession(false);
  }

  @Override
  public void saveRequest() {
    request.getSession(true).setAttribute(FilterSessionStore.REDIRECT_URI, redirect);
  }

  @Override
  public String getRedirectUri() {
    String redirectUri = super.getRedirectUri();
    return redirectUri == null ? redirectUri : redirectUri.replaceAll("/+$", "/");
  }
}
