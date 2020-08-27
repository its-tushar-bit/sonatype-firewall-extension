/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.io.Serializable;
import java.time.Duration;
import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.security.ShiroSessionDAO.SessionAndStoredJson;
import com.sonatype.insight.brain.model.security.PersistedUserSession;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;

import com.google.common.collect.Sets;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

public class ShiroSessionDAOTest
    extends AbstractDbDAOTest
{
  private final ShiroSessionDAO shiroSessionDAO = new ShiroSessionDAO();

  @Test
  public void testCRUD() {
    SimpleSession session = createSession();
    assertThat(session.getId()).isNull();

    // Create
    assertThat(shiroSessionDAO.doCreate(session)).isNotNull().isEqualTo(session.getId());

    // Read
    assertThat(shiroSessionDAO.doReadSession(session.getId())).usingRecursiveComparison().ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(getSessionFromCache(session.getId()).getSession()).usingRecursiveComparison().ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(PersistedUserSession.simpleSessionFromJson(getSessionFromCache(session.getId()).getStoredSessionJson()))
        .usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(session);
    assertThat(getSessionFromDatabase(session.getId())).usingRecursiveComparison().ignoringCollectionOrder()
        .isEqualTo(session);

    // Update
    session.setAttribute("key3", "value3");
    shiroSessionDAO.update(session);
    assertThat(shiroSessionDAO.doReadSession(session.getId())).usingRecursiveComparison().ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(getSessionFromCache(session.getId()).getSession()).usingRecursiveComparison().ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(PersistedUserSession.simpleSessionFromJson(getSessionFromCache(session.getId()).getStoredSessionJson()))
        .usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(session);
    assertThat(getSessionFromDatabase(session.getId())).usingRecursiveComparison().ignoringCollectionOrder()
        .isEqualTo(session);

    // Delete
    shiroSessionDAO.delete(session);
    assertThat(shiroSessionDAO.doReadSession(session.getId())).isNull();
    assertThat(getSessionFromCache(session.getId())).isNull();
    assertThat(getSessionFromDatabase(session.getId())).isNull();
  }

  @Test
  public void testDoCreate_NotSimpleSession() {
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> shiroSessionDAO.doCreate(mock(Session.class)))
        .withMessageContaining("session must be an instance of " + SimpleSession.class.getName());
  }

  @Test
  public void testDoReadSession_ReadsFromCache_IfYoung() {
    SimpleSession session = createSession();
    session.setId("id");
    session.setAttribute("key1", "value1");
    PersistedUserSession persistedUserSession = new PersistedUserSession(session);
    new PersistedUserSessionDAO().insert(persistedUserSession);
    session.setAttribute("key1", "value2");
    ShiroSessionDAO.SESSION_CACHE
        .put(session.getId(), new SessionAndStoredJson(session, persistedUserSession.getSessionJson()));

    assertThat(shiroSessionDAO.doReadSession(session.getId()).getAttribute("key1")).isEqualTo("value2");
    assertThat(getSessionFromCache(session.getId()).getSession().getAttribute("key1")).isEqualTo("value2");
    assertThat(PersistedUserSession.simpleSessionFromJson(getSessionFromCache(session.getId()).getStoredSessionJson())
        .getAttribute("key1")).isEqualTo("value1");
    assertThat(getSessionFromDatabase(session.getId()).getAttribute("key1")).isEqualTo("value1");
  }

  @Test
  public void testDoReadSession_ReadsFromDatabase_IfOld() {
    SimpleSession session = createSession();
    session.setId("id");
    session.setLastAccessTime(new Date(System.currentTimeMillis() - ShiroSessionDAO.CACHE_DURATION.toMillis() - 1));
    session.setAttribute("key1", "value1");
    PersistedUserSession persistedUserSession = new PersistedUserSession(session);
    new PersistedUserSessionDAO().insert(persistedUserSession);
    session.setAttribute("key1", "value2");
    ShiroSessionDAO.SESSION_CACHE
        .put(session.getId(), new SessionAndStoredJson(session, persistedUserSession.getSessionJson()));

    assertThat(shiroSessionDAO.doReadSession(session.getId()).getAttribute("key1")).isEqualTo("value1");
    assertThat(getSessionFromCache(session.getId()).getSession().getAttribute("key1")).isEqualTo("value1");
    assertThat(PersistedUserSession.simpleSessionFromJson(getSessionFromCache(session.getId()).getStoredSessionJson())
        .getAttribute("key1")).isEqualTo("value1");
    assertThat(getSessionFromDatabase(session.getId()).getAttribute("key1")).isEqualTo("value1");
  }

  @Test
  public void testDoReadSession_DoesNotExistInCacheOrDatabase() {
    String id = "doesNotExistInCacheOrDatabase";

    assertThat(shiroSessionDAO.doReadSession(id)).isNull();
    assertThat(getSessionFromCache(id)).isNull();
    assertThat(getSessionFromDatabase(id)).isNull();
  }

  @Test
  public void testDoReadSession_DoesNotExistInDatabase_AndOld() {
    SimpleSession session = createSession();
    session.setId("doesNotExistInDatabaseAndOld");
    session.setLastAccessTime(new Date(System.currentTimeMillis() - ShiroSessionDAO.CACHE_DURATION.toMillis() - 1));
    ShiroSessionDAO.SESSION_CACHE
        .put(session.getId(), new SessionAndStoredJson(session, PersistedUserSession.simpleSessionToJson(session)));

    assertThat(shiroSessionDAO.doReadSession(session.getId())).isNull();
    assertThat(getSessionFromCache(session.getId())).isNull();
    assertThat(getSessionFromDatabase(session.getId())).isNull();
  }

  @Test
  public void testDoReadSession_DoesNotExistInDatabase_AndYoung() {
    SimpleSession session = createSession();
    session.setId("doesNotExistInDatabaseAndYoung");
    ShiroSessionDAO.SESSION_CACHE
        .put(session.getId(), new SessionAndStoredJson(session, PersistedUserSession.simpleSessionToJson(session)));

    assertThat(shiroSessionDAO.doReadSession(session.getId())).usingRecursiveComparison().ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(getSessionFromCache(session.getId()).getSession()).usingRecursiveComparison().ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(PersistedUserSession.simpleSessionFromJson(getSessionFromCache(session.getId()).getStoredSessionJson()))
        .usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(session);
    assertThat(getSessionFromDatabase(session.getId())).isNull();
  }

  @Test
  public void testDoReadSession_DoesNotExistInCache_AndOld() {
    SimpleSession session = createSession();
    session.setId("doesNotExistInCacheAndOld");
    session.setLastAccessTime(new Date(System.currentTimeMillis() - ShiroSessionDAO.CACHE_DURATION.toMillis() - 1));
    new PersistedUserSessionDAO().insert(new PersistedUserSession(session));

    assertThat(shiroSessionDAO.doReadSession(session.getId())).usingRecursiveComparison().ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(getSessionFromCache(session.getId()).getSession()).usingRecursiveComparison().ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(PersistedUserSession.simpleSessionFromJson(getSessionFromCache(session.getId()).getStoredSessionJson()))
        .usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(session);
    assertThat(getSessionFromDatabase(session.getId())).usingRecursiveComparison().ignoringCollectionOrder()
        .isEqualTo(session);
  }

  @Test
  public void testDoReadSession_DoesNotExistInCache_AndYoung() {
    SimpleSession session = createSession();
    session.setId("doesNotExistInCacheAndYoung");
    new PersistedUserSessionDAO().insert(new PersistedUserSession(session));

    assertThat(shiroSessionDAO.doReadSession(session.getId())).usingRecursiveComparison().ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(getSessionFromCache(session.getId()).getSession()).usingRecursiveComparison().ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(PersistedUserSession.simpleSessionFromJson(getSessionFromCache(session.getId()).getStoredSessionJson()))
        .usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(session);
    assertThat(getSessionFromDatabase(session.getId())).usingRecursiveComparison().ignoringCollectionOrder()
        .isEqualTo(session);
  }

  @Test
  public void testDoReadSession_BadSession() {
    PersistedUserSession persistedUserSession = new PersistedUserSession(new SimpleSession());
    persistedUserSession.setId("id");
    SimpleSession badSession = new SimpleSession()
    {
      public String newField;
    };
    badSession.setId(persistedUserSession.getId());
    persistedUserSession.setSession(badSession);
    new PersistedUserSessionDAO().insert(persistedUserSession);

    assertThatExceptionOfType(UnknownSessionException.class)
        .isThrownBy(() -> shiroSessionDAO.doReadSession(badSession.getId()));
    assertThat(ShiroSessionDAO.SESSION_CACHE.get(persistedUserSession.getId())).isNull();
    assertThat(new PersistedUserSessionDAO().getById(persistedUserSession.getId())).isNull();
  }

  @Test
  public void testUpdate_NotSimpleSession() {
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> shiroSessionDAO.update(mock(Session.class)))
        .withMessageContaining("session must be an instance of " + SimpleSession.class.getName());
  }

  @Test
  public void testUpdate_NullId() {
    SimpleSession session = createSession();
    assertThat(session.getId()).isNull();

    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> shiroSessionDAO.update(session))
        .withMessageContaining("id argument cannot be null");
  }

  @Test
  public void testUpdate_DoesNotExistInCacheOrDatabase() {
    SimpleSession session = createSession();
    session.setId("doesNotExistInCacheOrDatabase");

    assertThatExceptionOfType(UnknownSessionException.class).isThrownBy(() -> shiroSessionDAO.update(session));
    assertThat(getSessionFromCache(session.getId())).isNull();
    assertThat(getSessionFromDatabase(session.getId())).isNull();
  }

  @Test
  public void testUpdate_DoesNotExistInCache() {
    SimpleSession session = createSession();
    session.setId("doesNotExistInCache");
    new PersistedUserSessionDAO().insert(new PersistedUserSession(session));

    shiroSessionDAO.update(session);

    assertThat(getSessionFromCache(session.getId()).getSession()).usingRecursiveComparison().ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(PersistedUserSession.simpleSessionFromJson(getSessionFromCache(session.getId()).getStoredSessionJson()))
        .usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(session);
    assertThat(getSessionFromDatabase(session.getId())).usingRecursiveComparison().ignoringCollectionOrder()
        .isEqualTo(session);
  }

  @Test
  public void testUpdate_DoesNotExistInDatabase() {
    SimpleSession session = createSession();
    session.setId("doesNotExistInDatabase");
    ShiroSessionDAO.SESSION_CACHE
        .put(session.getId(), new SessionAndStoredJson(session, PersistedUserSession.simpleSessionToJson(session)));
    session.setAttribute("key", "value");

    assertThatExceptionOfType(UnknownSessionException.class).isThrownBy(() -> shiroSessionDAO.update(session));
    assertThat(getSessionFromCache(session.getId())).isNull();
    assertThat(getSessionFromDatabase(session.getId())).isNull();
  }

  @Test
  public void testUpdate_IgnoredIfOnlyLastAccessTimeAndItWasRecentlyUpdated() {
    // Initial lastAccessTime set
    SimpleSession session = new SimpleSession();
    session.setId("id");
    Date lastAccessTime = new Date(System.currentTimeMillis() - 1);
    session.setLastAccessTime(lastAccessTime);
    shiroSessionDAO.doCreate(session);

    // Doesn't update if it updated recently
    session = PersistedUserSession.simpleSessionFromJson(PersistedUserSession.simpleSessionToJson(session));
    session.setId("id");
    session
        .setLastAccessTime(new Date(lastAccessTime.getTime() + ShiroSessionDAO.DELAY_BETWEEN_LAST_ACCESS_TIME_UPDATES));
    shiroSessionDAO.update(session);
    assertThat(getSessionFromCache(session.getId()).getSession().getLastAccessTime()).isEqualTo(lastAccessTime);
    assertThat(PersistedUserSession.simpleSessionFromJson(getSessionFromCache(session.getId()).getStoredSessionJson())
        .getLastAccessTime()).isEqualTo(lastAccessTime);
    assertThat(getSessionFromDatabase(session.getId()).getLastAccessTime()).isEqualTo(lastAccessTime);

    // Updates if it didn't update recently
    session.setLastAccessTime(
        new Date(lastAccessTime.getTime() + ShiroSessionDAO.DELAY_BETWEEN_LAST_ACCESS_TIME_UPDATES + 1));
    shiroSessionDAO.update(session);
    assertThat(getSessionFromCache(session.getId()).getSession().getLastAccessTime())
        .isEqualTo(session.getLastAccessTime());
    assertThat(PersistedUserSession.simpleSessionFromJson(getSessionFromCache(session.getId()).getStoredSessionJson())
        .getLastAccessTime()).isEqualTo(session.getLastAccessTime());
    assertThat(getSessionFromDatabase(session.getId()).getLastAccessTime()).isEqualTo(session.getLastAccessTime());
  }

  @Test
  public void testDelete_Null() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> shiroSessionDAO.delete(null))
        .withMessageContaining("session argument cannot be null");
  }

  @Test
  public void testDelete_NullId() {
    SimpleSession doesNotExist = createSession();
    shiroSessionDAO.delete(doesNotExist);
  }

  @Test
  public void testDelete_NotFound() {
    SimpleSession doesNotExist = createSession();
    doesNotExist.setId("doesNotExist");
    shiroSessionDAO.delete(doesNotExist);
  }

  @Test
  public void testGetActiveSessions() {
    SimpleSession session1 = new SimpleSession();
    shiroSessionDAO.doCreate(session1);
    SimpleSession session2 = new SimpleSession();
    shiroSessionDAO.doCreate(session2);
    PersistedUserSession persistedUserSession = new PersistedUserSession(new SimpleSession());
    persistedUserSession.setId("id");
    SimpleSession badSession = new SimpleSession()
    {
      public String newField;
    };
    badSession.setId(persistedUserSession.getId());
    persistedUserSession.setSession(badSession);
    new PersistedUserSessionDAO().insert(persistedUserSession);
    ShiroSessionDAO.SESSION_CACHE
        .put(persistedUserSession.getId(), new SessionAndStoredJson(badSession, persistedUserSession.getSessionJson()));

    assertThat(shiroSessionDAO.getActiveSessions()).extracting(Session::getId)
        .containsExactly(session1.getId(), session2.getId());
    assertThat(new PersistedUserSessionDAO().getById(persistedUserSession.getId())).isNull();
    assertThat(ShiroSessionDAO.SESSION_CACHE.get(persistedUserSession.getId())).isNull();
  }

  private SessionAndStoredJson getSessionFromCache(Serializable id) {
    return ShiroSessionDAO.SESSION_CACHE.get(id);
  }

  private Session getSessionFromDatabase(Serializable id) {
    PersistedUserSession persistedUserSession = new PersistedUserSessionDAO().getById(id.toString());
    return persistedUserSession == null ? null : persistedUserSession.getSession();
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
