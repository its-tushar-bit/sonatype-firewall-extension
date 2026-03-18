/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.google.common.collect.Sets;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.junit.Test;
import org.keycloak.adapters.saml.SamlPrincipal;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.saml.SamlSessionStore.CurrentAction;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.dom.saml.v2.assertion.AssertionType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PersistedUserSessionTest
{
  @Test
  public void testGetSetSession() throws Exception {
    SimpleSession session = new SimpleSession();
    PersistedUserSession persistedUserSession = new PersistedUserSession(session);

    assertThat(persistedUserSession.getSession()).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);

    session = createSession();
    persistedUserSession.setSession(session);

    assertThat(persistedUserSession.getSession()).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .ignoringFields("attributes.org.keycloak.adapters.saml.SamlSession.principal.assertion")
        .isEqualTo(session);

    session.setAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY, createPrincipalCollection());
    persistedUserSession.setSession(session);

    assertThat(persistedUserSession.getSession()).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .ignoringFields("attributes.org.keycloak.adapters.saml.SamlSession.principal.assertion")
        .isEqualTo(session);
  }

  @Test
  public void testGetSession_UnknownProperty() {
    SimpleSession badSession = new SimpleSession()
    {
      private static final long serialVersionUID = 69763569812217857L;

      @SuppressWarnings("unused")
      public String newField;
    };
    PersistedUserSession persistedUserSession = new PersistedUserSession(badSession);

    assertThatExceptionOfType(UnknownSessionException.class).isThrownBy(persistedUserSession::getSession)
        .withMessageContaining("Unrecognized field \"newField\"");
  }

  private SimpleSession createSession() throws Exception {
    SimpleSession simpleSession = new SimpleSession();
    simpleSession.setId(UUID.randomUUID().toString());
    simpleSession.setStartTimestamp(new Date());
    simpleSession.setStopTimestamp(new Date(System.currentTimeMillis() + Duration.ofMinutes(15).toMillis()));
    simpleSession.setLastAccessTime(new Date());
    simpleSession.setTimeout(DefaultSessionManager.DEFAULT_GLOBAL_SESSION_TIMEOUT);
    simpleSession.setExpired(false);
    simpleSession.setHost("127.0.0.1");
    simpleSession.setAttribute("key1", "value1");
    simpleSession.setAttribute("key2", "value2");
    simpleSession.setAttribute(SamlSessionStore.CURRENT_ACTION, CurrentAction.NONE);
    simpleSession.setAttribute(SamlSession.class.getName(), createSamlSession());
    return simpleSession;
  }

  private SamlSession createSamlSession() throws Exception {
    AssertionType assertionType = new AssertionType("id", createXMLGregorianCalendar());
    MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    attributes.put("key1", Arrays.asList("value1", "value2"));
    attributes.putSingle("key2", "value3");
    SamlPrincipal samlPrincipal =
        new SamlPrincipal(assertionType, null, "name", "samlSubject", "nameIDFormat", attributes, attributes);
    Set<String> roles = new HashSet<>(Arrays.asList("role1", "role2"));
    String sessionIndex = "sessionIndex";
    return new SamlSession(samlPrincipal, roles, sessionIndex, createXMLGregorianCalendar());
  }

  private XMLGregorianCalendar createXMLGregorianCalendar() throws Exception {
    return DatatypeFactory.newInstance()
        .newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now(ZoneOffset.UTC)));
  }

  private PrincipalCollection createPrincipalCollection() {
    SimplePrincipalCollection simplePrincipalCollection = new SimplePrincipalCollection();
    UserPrincipal userPrincipal = new UserPrincipal("username1", "displayName1", User.INTERNAL_REALM_ID, Sets
        .newHashSet("group1", "group2"));
    simplePrincipalCollection.add(userPrincipal, userPrincipal.getRealmId());
    return simplePrincipalCollection;
  }
}
