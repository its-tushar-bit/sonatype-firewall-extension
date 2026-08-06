/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.security.PersistedUserSession;
import com.sonatype.insight.brain.tenancy.TenantReference;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ShiroSessionDAO
    extends AbstractSessionDAO
{
  private static final Logger log = LoggerFactory.getLogger(ShiroSessionDAO.class);

  // Visible for testing
  static final Duration CACHE_DURATION = Duration.ofSeconds(10);

  // Visible for testing
  static final TenantReference<ConcurrentMap<Serializable, SessionAndStoredJson>> SESSION_CACHE =
      new TenantReference<>(ConcurrentHashMap::new);

  // Visible for testing
  static final long DELAY_BETWEEN_LAST_ACCESS_TIME_UPDATES = Duration.ofSeconds(5).toMillis();

  private final PersistedUserSessionDAO persistedUserSessionDAO;

  @Inject
  public ShiroSessionDAO(PersistedUserSessionDAO persistedUserSessionDAO) {
    this.persistedUserSessionDAO = persistedUserSessionDAO;
  }

  @Override
  protected Serializable doCreate(Session session) {
    checkSimpleSession(session);
    PersistedUserSession persistedUserSession = new PersistedUserSession((SimpleSession) session);
    persistedUserSessionDAO.insert(persistedUserSession);
    ((SimpleSession) session).setId(persistedUserSession.getId());
    cacheSession(session, persistedUserSession);
    return session.getId();
  }

  @Override
  protected Session doReadSession(Serializable sessionId) {
    SessionAndStoredJson cachedSession = SESSION_CACHE.get().get(sessionId);
    if (cachedSession != null &&
        System.currentTimeMillis() - cachedSession.getSession().getLastAccessTime().getTime() <= CACHE_DURATION
            .toMillis())
    {
      return cachedSession.getSession();
    }
    uncacheSession(sessionId);
    PersistedUserSession persistedUserSession = persistedUserSessionDAO.getById(sessionId.toString());
    if (persistedUserSession == null) {
      return null;
    }
    Session session = getSessionOrDeleteIfUnknown(persistedUserSession);
    cacheSession(session, persistedUserSession);
    return session;
  }

  @Override
  public void update(Session session) {
    checkSimpleSession(session);
    checkId(session.getId());
    SessionAndStoredJson cachedSession = SESSION_CACHE.get().get(session.getId());
    if (cachedSession != null &&
        isOnlyLastAccessTimeUpdate((SimpleSession) session, cachedSession.getStoredSessionJson()) &&
        isLastAccessTimeUpdatedRecently(session, cachedSession.getStoredSessionJson()))
    {
      return;
    }
    PersistedUserSession persistedUserSession = new PersistedUserSession((SimpleSession) session);
    try {
      persistedUserSessionDAO.update(persistedUserSession);
    }
    catch (RuntimeException e) {
      // The session may have been deleted on another node e.g. if the user was deleted
      if (e.getMessage() != null && e.getMessage().contains("Entity not found")) {
        throw new UnknownSessionException("There is no session with id [" + session.getId() + "]", e);
      }
      throw e;
    }
    finally {
      uncacheSession(session.getId());
    }
    cacheSession(session, persistedUserSession);
  }

  private boolean isOnlyLastAccessTimeUpdate(SimpleSession session, String storedSessionJson) {
    SimpleSession storedSessionWithUpdatedLastAccessTime =
        PersistedUserSession.simpleSessionFromJson(storedSessionJson);
    storedSessionWithUpdatedLastAccessTime.setLastAccessTime(session.getLastAccessTime());
    // Set the id to null, SimpleSession.equals should ignore ids if either one is null and just compare other fields
    storedSessionWithUpdatedLastAccessTime.setId(null);
    return session.equals(storedSessionWithUpdatedLastAccessTime);
  }

  private boolean isLastAccessTimeUpdatedRecently(Session session, String storedSessionJson) {
    SimpleSession storedSession = PersistedUserSession.simpleSessionFromJson(storedSessionJson);
    return session.getLastAccessTime().getTime()
        - DELAY_BETWEEN_LAST_ACCESS_TIME_UPDATES <= storedSession.getLastAccessTime().getTime();
  }

  @Override
  public void delete(Session session) {
    checkSession(session);
    deleteById(session.getId());
  }

  public void deleteById(Serializable id) {
    if (id != null) {
      persistedUserSessionDAO.deleteById(id.toString());
      uncacheSession(id);
    }
  }

  private void cacheSession(Session session, PersistedUserSession persistedUserSession) {
    SESSION_CACHE.get()
        .put(session.getId(), new SessionAndStoredJson((SimpleSession) session, persistedUserSession.getSessionJson()));
  }

  private void uncacheSession(Serializable id) {
    SESSION_CACHE.get().remove(id);
  }

  @Override
  public Collection<Session> getActiveSessions() {
    Collection<Session> sessions = new ArrayList<>();
    for (PersistedUserSession persistedUserSession : persistedUserSessionDAO.getAll()) {
      try {
        sessions.add(getSessionOrDeleteIfUnknown(persistedUserSession));
      }
      catch (UnknownSessionException e) {
        // noop
      }
    }
    return sessions;
  }

  private Session getSessionOrDeleteIfUnknown(PersistedUserSession persistedUserSession) {
    try {
      return persistedUserSession.getSession();
    }
    catch (UnknownSessionException e) {
      deleteById(persistedUserSession.getId());
      log.debug("Failed to read session {} due to incompatible types and deleted it.", persistedUserSession.getId(), e);
      throw e;
    }
  }

  private void checkId(Serializable id) {
    if (id == null) {
      throw new NullPointerException("id argument cannot be null.");
    }
  }

  private void checkSession(Session session) {
    if (session == null) {
      throw new NullPointerException("session argument cannot be null.");
    }
  }

  private void checkSimpleSession(Session session) {
    if (!(session instanceof SimpleSession)) {
      throw new IllegalArgumentException("session must be an instance of " + SimpleSession.class.getName());
    }
  }

  static class SessionAndStoredJson
  {
    private SimpleSession session;

    private String storedSessionJson;

    public SessionAndStoredJson(SimpleSession session, String storedSessionJson) {
      this.session = session;
      this.storedSessionJson = storedSessionJson;
    }

    public Session getSession() {
      return session;
    }

    public void setSession(SimpleSession session) {
      this.session = session;
    }

    public String getStoredSessionJson() {
      return storedSessionJson;
    }

    public void setStoredSessionJson(String storedSessionJson) {
      this.storedSessionJson = storedSessionJson;
    }
  }
}
