/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
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
  private final SamlSessionStoreForRedirect samlSessionStore;

  SamlAuthenticatorForSamlEndpoint(
      final HttpFacade facade,
      final SamlDeployment deployment,
      final SamlSessionStoreForRedirect sessionStore)
  {
    super(facade, deployment, sessionStore);
    this.samlSessionStore = sessionStore;
  }

  @Override
  protected void completeAuthentication(SamlSession samlSession) {
    Subject subject = SecurityUtils.getSubject();

    // Workaround for SHIRO-170: Shiro 2.2.0 added session fixation protection that destroys
    // the pre-login session inside Subject.login(). The Keycloak SAML adapter reads session
    // attributes (redirect URI, SAML account) after this method returns, so we use the pattern
    // recommended by Les Hazlewood on SHIRO-170: stop the session before login (making
    // beforeSuccessfulLogin a no-op), then restore attributes to the new post-login session.
    Map<Object, Object> attributes = new LinkedHashMap<>();
    Session oldSession = subject.getSession(false);
    Object oldPrincipal = subject.getPrincipal();
    if (oldSession != null) {
      for (Object key : oldSession.getAttributeKeys()) {
        attributes.put(key, oldSession.getAttribute(key));
      }
      oldSession.stop();
    }

    // Force ShiroHttpServletRequest to drop its stale cached session
    samlSessionStore.refreshCachedSession();

    subject.login(new SamlAuthenticationToken(samlSession.getPrincipal()));

    // Restore pre-login attributes only if the old session was anonymous or belonged to the
    // same principal (per Dmitry Gusev's warning on SHIRO-170: avoid leaking one principal's
    // session attributes into another's). Do not overwrite anything Shiro stored during login.
    Subject newSubject = SecurityUtils.getSubject();
    if (oldPrincipal == null || oldPrincipal.equals(newSubject.getPrincipal())) {
      Session newSession = newSubject.getSession();
      for (Map.Entry<Object, Object> entry : attributes.entrySet()) {
        if (newSession.getAttribute(entry.getKey()) == null) {
          newSession.setAttribute(entry.getKey(), entry.getValue());
        }
      }
    }
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
