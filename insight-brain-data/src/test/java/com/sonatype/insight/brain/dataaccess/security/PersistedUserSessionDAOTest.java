/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.time.Duration;
import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.security.PersistedUserSession;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;

import com.google.common.collect.Sets;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PersistedUserSessionDAOTest
    extends AbstractDbDAOTest
{
  private PersistedUserSessionDAO persistedUserSessionDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    persistedUserSessionDAO = daoFactory.createPersistedUserSessionDAO();
  }

  @Test
  public void testCRUD() {
    SimpleSession session = createSession();
    assertThat(session.getId()).isNull();

    // Create
    PersistedUserSession persistedUserSession = new PersistedUserSession(session);
    persistedUserSessionDAO.insert(persistedUserSession);
    assertThat(persistedUserSession.getId()).isNotNull();
    session = persistedUserSession.getSession();

    // Read
    assertThat(persistedUserSessionDAO.getById(persistedUserSession.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .ignoringCollectionOrder()
        .isEqualTo(persistedUserSession);

    // Update
    session.setAttribute("key3", "value3");
    persistedUserSession.setSession(session);
    persistedUserSessionDAO.update(persistedUserSession);
    assertThat(persistedUserSessionDAO.getById(persistedUserSession.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .ignoringCollectionOrder()
        .isEqualTo(persistedUserSession);

    // Delete
    persistedUserSessionDAO.delete(persistedUserSession);
    assertThat(persistedUserSessionDAO.getById(persistedUserSession.getId())).isNull();
  }

  @Test
  public void testGetAll() {
    PersistedUserSession persistedUserSession1 = new PersistedUserSession(new SimpleSession());
    persistedUserSessionDAO.insert(persistedUserSession1);
    PersistedUserSession persistedUserSession2 = new PersistedUserSession(new SimpleSession());
    persistedUserSessionDAO.insert(persistedUserSession2);

    assertThat(persistedUserSessionDAO.getAll()).extracting(PersistedUserSession::getId)
        .containsExactlyInAnyOrder(persistedUserSession1.getId(), persistedUserSession2.getId());
  }

  @Test
  public void testDeleteById() {
    PersistedUserSession persistedUserSession1 = new PersistedUserSession(new SimpleSession());
    persistedUserSessionDAO.insert(persistedUserSession1);
    PersistedUserSession persistedUserSession2 = new PersistedUserSession(new SimpleSession());
    persistedUserSessionDAO.insert(persistedUserSession2);

    persistedUserSessionDAO.deleteById(persistedUserSession1.getId());

    assertThat(persistedUserSessionDAO.getAll()).extracting(PersistedUserSession::getId)
        .containsExactly(persistedUserSession2.getId());
  }

  private SimpleSession createSession() {
    SimpleSession simpleSession = new SimpleSession();
    simpleSession.setStartTimestamp(new Date());
    simpleSession.setStopTimestamp(new Date(System.currentTimeMillis() + Duration.ofMinutes(15).toMillis()));
    simpleSession.setLastAccessTime(new Date());
    simpleSession.setTimeout(DefaultSessionManager.DEFAULT_GLOBAL_SESSION_TIMEOUT);
    simpleSession.setExpired(false);
    simpleSession.setHost("127.0.0.1");
    simpleSession.setAttribute("key1", "value1");
    simpleSession.setAttribute("key2", "value2");
    simpleSession.setAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY, createPrincipalCollection());
    return simpleSession;
  }

  private PrincipalCollection createPrincipalCollection() {
    SimplePrincipalCollection simplePrincipalCollection = new SimplePrincipalCollection();
    UserPrincipal userPrincipal = new UserPrincipal("username1", "displayName1", User.INTERNAL_REALM_ID, Sets
        .newHashSet("group1", "group2"));
    simplePrincipalCollection.add(userPrincipal, userPrincipal.getRealmId());
    return simplePrincipalCollection;
  }
}
