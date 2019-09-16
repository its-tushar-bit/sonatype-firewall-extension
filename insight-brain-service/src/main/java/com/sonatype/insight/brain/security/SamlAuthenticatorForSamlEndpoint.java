/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.keycloak.adapters.saml.SamlAuthenticator;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.saml.profile.SamlAuthenticationHandler;
import org.keycloak.adapters.saml.profile.webbrowsersso.SamlEndpoint;
import org.keycloak.adapters.spi.HttpFacade;

public class SamlAuthenticatorForSamlEndpoint
    extends SamlAuthenticator
{
  SamlAuthenticatorForSamlEndpoint(
      final HttpFacade facade,
      final SamlDeployment deployment,
      final SamlSessionStore sessionStore)
  {
    super(facade, deployment, sessionStore);
  }

  @Override
  protected void completeAuthentication(SamlSession samlSession) {
    Subject subject = SecurityUtils.getSubject();
    subject.login(new SamlAuthenticationToken(samlSession.getPrincipal()));
  }

  @Override
  protected SamlAuthenticationHandler createBrowserHandler(
      HttpFacade facade,
      SamlDeployment deployment,
      SamlSessionStore sessionStore)
  {
    return new SamlEndpoint(facade, deployment, sessionStore);
  }
}
