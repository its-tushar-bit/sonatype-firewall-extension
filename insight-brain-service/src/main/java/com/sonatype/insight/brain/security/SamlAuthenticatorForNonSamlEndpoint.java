/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.keycloak.adapters.saml.SamlAuthenticator;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.saml.profile.SamlAuthenticationHandler;
import org.keycloak.adapters.saml.profile.webbrowsersso.BrowserHandler;
import org.keycloak.adapters.spi.HttpFacade;

public class SamlAuthenticatorForNonSamlEndpoint
    extends SamlAuthenticator
{
  SamlAuthenticatorForNonSamlEndpoint(
      final HttpFacade facade,
      final SamlDeployment deployment,
      final SamlSessionStore sessionStore)
  {
    super(facade, deployment, sessionStore);
  }

  @Override
  protected void completeAuthentication(SamlSession samlSession) {
  }

  @Override
  protected SamlAuthenticationHandler createBrowserHandler(
      HttpFacade facade,
      SamlDeployment deployment,
      SamlSessionStore sessionStore)
  {
    return new BrowserHandler(facade, deployment, sessionStore);
  }
}
