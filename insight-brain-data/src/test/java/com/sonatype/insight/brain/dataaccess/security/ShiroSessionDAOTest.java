/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.io.Serializable;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
import org.junit.Before;
import org.junit.Test;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.saml.SamlSessionStore.CurrentAction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class ShiroSessionDAOTest
    extends AbstractDbDAOTest
{
  private ShiroSessionDAO shiroSessionDAO;

  private PersistedUserSessionDAO persistedUserSessionDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    shiroSessionDAO = daoFactory.createShiroSessionDAO();
    persistedUserSessionDAO = daoFactory.createPersistedUserSessionDAO();
  }

  @Test
  public void testCRUD() {
    SimpleSession session = createSession();
    assertThat(session.getId()).isNull();

    // Create
    assertThat(shiroSessionDAO.doCreate(session)).isNotNull().isEqualTo(session.getId());

    // Read
    assertThat(shiroSessionDAO.doReadSession(session.getId())).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(getSessionFromCache(session.getId()).getSession()).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(PersistedUserSession.simpleSessionFromJson(getSessionFromCache(session.getId()).getStoredSessionJson()))
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(getSessionFromDatabase(session.getId())).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);

    // Update
    session.setAttribute("key3", "value3");
    shiroSessionDAO.update(session);
    assertThat(shiroSessionDAO.doReadSession(session.getId())).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(getSessionFromCache(session.getId()).getSession()).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(PersistedUserSession.simpleSessionFromJson(getSessionFromCache(session.getId()).getStoredSessionJson()))
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(getSessionFromDatabase(session.getId())).usingRecursiveComparison()
        .ignoringCollectionOrder()
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
    persistedUserSessionDAO.insert(persistedUserSession);
    session.setAttribute("key1", "value2");
    ShiroSessionDAO.SESSION_CACHE.get()
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
    persistedUserSessionDAO.insert(persistedUserSession);
    session.setAttribute("key1", "value2");
    ShiroSessionDAO.SESSION_CACHE.get()
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
    ShiroSessionDAO.SESSION_CACHE.get()
        .put(session.getId(), new SessionAndStoredJson(session, PersistedUserSession.simpleSessionToJson(session)));

    assertThat(shiroSessionDAO.doReadSession(session.getId())).isNull();
    assertThat(getSessionFromCache(session.getId())).isNull();
    assertThat(getSessionFromDatabase(session.getId())).isNull();
  }

  @Test
  public void testDoReadSession_DoesNotExistInDatabase_AndYoung() {
    SimpleSession session = createSession();
    session.setId("doesNotExistInDatabaseAndYoung");
    ShiroSessionDAO.SESSION_CACHE.get()
        .put(session.getId(), new SessionAndStoredJson(session, PersistedUserSession.simpleSessionToJson(session)));

    assertThat(shiroSessionDAO.doReadSession(session.getId())).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(getSessionFromCache(session.getId()).getSession()).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(PersistedUserSession.simpleSessionFromJson(getSessionFromCache(session.getId()).getStoredSessionJson()))
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(getSessionFromDatabase(session.getId())).isNull();
  }

  @Test
  public void testDoReadSession_DoesNotExistInCache_AndOld() {
    SimpleSession session = createSession();
    session.setId("doesNotExistInCacheAndOld");
    session.setLastAccessTime(new Date(System.currentTimeMillis() - ShiroSessionDAO.CACHE_DURATION.toMillis() - 1));
    persistedUserSessionDAO.insert(new PersistedUserSession(session));

    assertThat(shiroSessionDAO.doReadSession(session.getId())).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(getSessionFromCache(session.getId()).getSession()).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(PersistedUserSession.simpleSessionFromJson(getSessionFromCache(session.getId()).getStoredSessionJson()))
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(getSessionFromDatabase(session.getId())).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
  }

  @Test
  public void testDoReadSession_DoesNotExistInCache_AndYoung() {
    SimpleSession session = createSession();
    session.setId("doesNotExistInCacheAndYoung");
    persistedUserSessionDAO.insert(new PersistedUserSession(session));

    assertThat(shiroSessionDAO.doReadSession(session.getId())).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(getSessionFromCache(session.getId()).getSession()).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(PersistedUserSession.simpleSessionFromJson(getSessionFromCache(session.getId()).getStoredSessionJson()))
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(getSessionFromDatabase(session.getId())).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
  }

  @Test
  public void testDoReadSession_BadSession() {
    PersistedUserSession persistedUserSession = new PersistedUserSession(new SimpleSession());
    persistedUserSession.setId("id");
    SimpleSession badSession = new SimpleSession()
    {
      private static final long serialVersionUID = -5966186720431447054L;

      @SuppressWarnings("unused")
      public String newField;
    };
    badSession.setId(persistedUserSession.getId());
    persistedUserSession.setSession(badSession);
    persistedUserSessionDAO.insert(persistedUserSession);

    assertThatExceptionOfType(UnknownSessionException.class)
        .isThrownBy(() -> shiroSessionDAO.doReadSession(badSession.getId()));
    assertThat(ShiroSessionDAO.SESSION_CACHE.get().get(persistedUserSession.getId())).isNull();
    assertThat(persistedUserSessionDAO.getById(persistedUserSession.getId())).isNull();
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
    persistedUserSessionDAO.insert(new PersistedUserSession(session));

    shiroSessionDAO.update(session);

    assertThat(getSessionFromCache(session.getId()).getSession()).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(PersistedUserSession.simpleSessionFromJson(getSessionFromCache(session.getId()).getStoredSessionJson()))
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
    assertThat(getSessionFromDatabase(session.getId())).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(session);
  }

  @Test
  public void testUpdate_DoesNotExistInDatabase() {
    SimpleSession session = createSession();
    session.setId("doesNotExistInDatabase");
    ShiroSessionDAO.SESSION_CACHE.get()
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
      private static final long serialVersionUID = 2775238914868763211L;

      @SuppressWarnings("unused")
      public String newField;
    };
    badSession.setId(persistedUserSession.getId());
    persistedUserSession.setSession(badSession);
    persistedUserSessionDAO.insert(persistedUserSession);
    ShiroSessionDAO.SESSION_CACHE.get()
        .put(persistedUserSession.getId(), new SessionAndStoredJson(badSession, persistedUserSession.getSessionJson()));

    assertThat(shiroSessionDAO.getActiveSessions()).extracting(Session::getId)
        .containsExactly(session1.getId(), session2.getId());
    assertThat(persistedUserSessionDAO.getById(persistedUserSession.getId())).isNull();
    assertThat(ShiroSessionDAO.SESSION_CACHE.get().get(persistedUserSession.getId())).isNull();
  }

  @Test
  public void testDeleteById_ConcurrentlyModified() throws Throwable {
    PersistedUserSessionDAO persistedUserSessionDAOSpy = spy(persistedUserSessionDAO);
    CountDownLatch afterGetLatch = new CountDownLatch(1);
    CountDownLatch beforeDeleteLatch = new CountDownLatch(1);
    // This mocking is only done to protect against the old form i.e.
    // PersistedUserSessionDAO.delete(persistedUserSessionDAO.getById(id.toString()));
    doAnswer(invocationOnMock -> {
      try {
        PersistedUserSession persistedUserSession = (PersistedUserSession) invocationOnMock.callRealMethod();
        afterGetLatch.countDown();
        assertThat(beforeDeleteLatch.await(3, TimeUnit.SECONDS)).isTrue();
        return persistedUserSession;
      }
      catch (Throwable e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }).when(persistedUserSessionDAOSpy).getById(any());
    doAnswer(invocationOnMock -> {
      try {
        afterGetLatch.countDown(); // There is no get used in PersistedUserSessionDAO.deleteById so just count down
        assertThat(beforeDeleteLatch.await(3, TimeUnit.SECONDS)).isTrue();
        invocationOnMock.callRealMethod();
        return null;
      }
      catch (Throwable e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }).when(persistedUserSessionDAOSpy).deleteById(any());
    ShiroSessionDAO shiroSessionDAO = new ShiroSessionDAO(persistedUserSessionDAOSpy);
    SimpleSession session = createSession();
    session.setId("id");
    persistedUserSessionDAOSpy.insert(new PersistedUserSession(session));
    AtomicReference<Throwable> throwable = new AtomicReference<>();
    CountDownLatch endLatch = new CountDownLatch(1);

    new Thread(() -> {
      try {
        shiroSessionDAO.deleteById(session.getId());
      }
      catch (Throwable e) {
        throwable.set(e);
      }
      endLatch.countDown();
    }).start();

    assertThat(afterGetLatch.await(3, TimeUnit.SECONDS)).isTrue();
    persistedUserSessionDAOSpy.delete(new PersistedUserSession(session));
    beforeDeleteLatch.countDown();
    assertThat(endLatch.await(3, TimeUnit.SECONDS)).isTrue();
    if (throwable.get() != null) {
      throw throwable.get();
    }
  }

  @Test
  public void testDoReadSession_Null() {
    testDoReadSession(null, true);
  }

  @Test
  public void testDoReadSession_SamlLoggingIn() {
    testDoReadSession(CurrentAction.LOGGING_IN, false);
  }

  @Test
  public void testDoReadSession_SamlLoggingOut() {
    testDoReadSession(CurrentAction.LOGGING_OUT, false);
  }

  @Test
  public void testDoReadSession_SamlNoAction() {
    testDoReadSession(CurrentAction.NONE, true);
  }

  private void testDoReadSession(CurrentAction currentAction, boolean shouldStoreInCache) {
    SimpleSession session = createSession();
    session.setId("id");
    session.setAttribute(SamlSessionStore.CURRENT_ACTION, currentAction);
    PersistedUserSession persistedUserSession = new PersistedUserSession(session);
    persistedUserSessionDAO.insert(persistedUserSession);

    shiroSessionDAO.doReadSession(session.getId());

    assertThat(getSessionFromCache(session.getId()) != null).isEqualTo(shouldStoreInCache);
    assertThat(getSessionFromDatabase(session.getId())).isNotNull();
  }

  @Test
  public void testUpdateSession_Null() {
    testUpdate(null, true);
  }

  @Test
  public void testUpdateSession_SamlLoggingIn() {
    testUpdate(CurrentAction.LOGGING_IN, false);
  }

  @Test
  public void testUpdateSession_SamlLoggingOut() {
    testUpdate(CurrentAction.LOGGING_OUT, false);
  }

  @Test
  public void testUpdateSession_SamlNoAction() {
    testUpdate(CurrentAction.NONE, true);
  }

  private void testUpdate(CurrentAction currentAction, boolean shouldStoreInCache) {
    SimpleSession session = createSession();
    session.setId("id");
    PersistedUserSession persistedUserSession = new PersistedUserSession(session);
    persistedUserSessionDAO.insert(persistedUserSession);
    if (!shouldStoreInCache) {
      ShiroSessionDAO.SESSION_CACHE.get()
          .put(session.getId(), new SessionAndStoredJson(session, PersistedUserSession.simpleSessionToJson(session)));
    }

    session.setAttribute(SamlSessionStore.CURRENT_ACTION, currentAction);
    shiroSessionDAO.update(session);

    assertThat(getSessionFromCache(session.getId()) != null).isEqualTo(shouldStoreInCache);
    assertThat(getSessionFromDatabase(session.getId())).isNotNull();
  }

  private SessionAndStoredJson getSessionFromCache(Serializable id) {
    return ShiroSessionDAO.SESSION_CACHE.get().get(id);
  }

  private Session getSessionFromDatabase(Serializable id) {
    PersistedUserSession persistedUserSession = persistedUserSessionDAO.getById(id.toString());
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
