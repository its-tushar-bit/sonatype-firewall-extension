/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.servlet.http.HttpServletRequest;

import org.keycloak.adapters.saml.servlet.FilterSamlSessionStore;
import org.keycloak.adapters.servlet.FilterSessionStore;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.SessionIdMapper;

public class SamlSessionStoreForRedirect
    extends FilterSamlSessionStore
{
  SamlSessionStoreForRedirect(
      HttpServletRequest request,
      HttpFacade facade,
      int maxBuffer,
      SessionIdMapper idMapper)
  {
    super(request, facade, maxBuffer, idMapper);
  }

  @Override
  public void saveRequest() {
    request.getSession(true).setAttribute(FilterSessionStore.REDIRECT_URI, SamlFilter.getDestinationOrDefault(request));
  }

  @Override
  public String getRedirectUri() {
    String redirectUri = super.getRedirectUri();
    return redirectUri == null ? redirectUri : redirectUri.replaceAll("/+$", "/");
  }
}
