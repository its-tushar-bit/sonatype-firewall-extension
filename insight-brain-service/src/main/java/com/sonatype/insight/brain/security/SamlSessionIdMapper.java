/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.security.PersistedUserSessionDAO;
import com.sonatype.insight.brain.model.security.PersistedUserSession;

import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.spi.SessionIdMapper;

@Named
@Singleton
public class SamlSessionIdMapper
    implements SessionIdMapper
{
  private final PersistedUserSessionDAO persistedUserSessionDAO;

  @Inject
  public SamlSessionIdMapper(PersistedUserSessionDAO persistedUserSessionDAO) {
    this.persistedUserSessionDAO = persistedUserSessionDAO;
  }

  @Override
  public boolean hasSession(String id) {
    return persistedUserSessionDAO.getById(id) != null;
  }

  @Override
  public void clear() {
    // noop
  }

  @Override
  public Set<String> getUserSessions(String principal) {
    Set<String> userSessions = new HashSet<>();
    for (PersistedUserSession persistedUserSession : persistedUserSessionDAO.getAll()) {
      Object samlSessionAttribute = persistedUserSession.getSession().getAttribute(SamlSession.class.getName());
      if (samlSessionAttribute instanceof SamlSession) {
        SamlSession samlSession = (SamlSession) samlSessionAttribute;
        if (samlSession.getPrincipal().getName().equals(principal)) {
          userSessions.add(persistedUserSession.getId());
        }
      }
    }
    return userSessions;
  }

  @Override
  public String getSessionFromSSO(String sso) {
    for (PersistedUserSession persistedUserSession : persistedUserSessionDAO.getAll()) {
      Object samlSessionAttribute = persistedUserSession.getSession().getAttribute(SamlSession.class.getName());
      if (samlSessionAttribute instanceof SamlSession) {
        SamlSession samlSession = (SamlSession) samlSessionAttribute;
        if (samlSession.getSessionIndex().equals(sso)) {
          return persistedUserSession.getId();
        }
      }
    }
    return null;
  }

  @Override
  public void map(String sso, String principal, String session) {
    // noop
  }

  @Override
  public void removeSession(String session) {
    // noop
  }
}
