/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.security.PersistedUserSessionDAO;
import com.sonatype.insight.brain.model.security.PersistedUserSession;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.apache.shiro.session.mgt.SimpleSession;
import org.junit.Test;
import org.keycloak.adapters.saml.SamlPrincipal;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.common.util.MultivaluedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class SamlSessionIdMapperTest
    extends AbstractComponentTest
{
  @Inject
  private SamlSessionIdMapper samlSessionIdMapper;

  @Inject
  private PersistedUserSessionDAO persistedUserSessionDAO;

  @Test
  public void testGetUserSessions() {
    String principalName = "john";
    PersistedUserSession persistedUserSession1 =
        new PersistedUserSession(createSession(principalName, TemporaryEntity.uuid()));
    persistedUserSessionDAO.insert(persistedUserSession1);
    PersistedUserSession persistedUserSession2 =
        new PersistedUserSession(createSession(principalName, TemporaryEntity.uuid()));
    persistedUserSessionDAO.insert(persistedUserSession2);
    persistedUserSessionDAO
        .insert(new PersistedUserSession(createSession("not" + principalName, TemporaryEntity.uuid())));
    persistedUserSessionDAO.insert(new PersistedUserSession(new SimpleSession()));

    Set<String> userSessions = samlSessionIdMapper.getUserSessions(principalName);

    assertThat(userSessions).containsExactlyInAnyOrder(persistedUserSession1.getId(), persistedUserSession2.getId());
  }

  @Test
  public void testGetSessionFromSSO() {
    String principalName = "john";
    String sessionIndex = TemporaryEntity.uuid();
    PersistedUserSession persistedUserSession = new PersistedUserSession(createSession(principalName, sessionIndex));
    persistedUserSessionDAO.insert(persistedUserSession);
    persistedUserSessionDAO.insert(new PersistedUserSession(createSession(principalName, TemporaryEntity.uuid())));

    String sessionFromSSO = samlSessionIdMapper.getSessionFromSSO(sessionIndex);

    assertThat(sessionFromSSO).isEqualTo(persistedUserSession.getId());
  }

  private SimpleSession createSession(String principalName, String sessionIndex) {
    SimpleSession simpleSession = new SimpleSession();
    simpleSession.setAttribute(SamlSession.class.getName(), createSamlSession(principalName, sessionIndex));
    return simpleSession;
  }

  private SamlSession createSamlSession(String principalName, String sessionIndex) {
    return new SamlSession(
        new SamlPrincipal(null, null, principalName, "samlSubject", "nameIDFormat", new MultivaluedHashMap<>(),
            new MultivaluedHashMap<>()),
        null, sessionIndex, null);
  }
}
