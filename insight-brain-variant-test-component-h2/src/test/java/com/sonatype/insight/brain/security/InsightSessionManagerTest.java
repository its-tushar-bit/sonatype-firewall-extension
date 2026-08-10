/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collection;
import java.util.List;

import com.sonatype.insight.brain.audit.AuditSessionListener;
import com.sonatype.insight.brain.dataaccess.security.PersistedUserSessionDAO;
import com.sonatype.insight.brain.dataaccess.security.ShiroSessionDAO;
import com.sonatype.insight.brain.model.security.PersistedUserSession;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import jakarta.inject.Inject;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionListener;
import org.apache.shiro.session.mgt.AbstractSessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class InsightSessionManagerTest
    extends AbstractComponentH2Test
{
  @Inject
  private PersistedUserSessionDAO persistedUserSessionDAO;

  @Inject
  private InsightSessionManager insightSessionManager;

  @AfterEach
  public void after() {
    insightSessionManager.setGlobalSessionTimeout(AbstractSessionManager.DEFAULT_GLOBAL_SESSION_TIMEOUT);
    insightSessionManager.setTenantSessionTimeout(null);
  }

  @Test
  public void testInsightSessionManager_SetsCorrectSessionDAO() {
    assertThat(insightSessionManager.getSessionDAO()).isInstanceOf(ShiroSessionDAO.class);
  }

  @Test
  public void testInsightSessionManager_SetsCorrectSessionListeners() {
    Collection<SessionListener> sessionListeners = insightSessionManager.getSessionListeners();
    assertThat(sessionListeners).hasSize(1);
    assertThat(sessionListeners.iterator().next()).isInstanceOf(AuditSessionListener.class);
  }

  @Test
  public void testApplyGlobalSessionTimeout_NoSessionTimeoutSet() {
    long globalSessionTimeout = insightSessionManager.getGlobalSessionTimeout() + 1;
    insightSessionManager.setGlobalSessionTimeout(globalSessionTimeout);

    Session session = insightSessionManager.start(null);

    assertThat(session.getTimeout()).isEqualTo(globalSessionTimeout);
    List<PersistedUserSession> persistedUserSessions = persistedUserSessionDAO.getAll();
    assertThat(persistedUserSessions).hasSize(1);
    Session storedSession = persistedUserSessions.get(0).getSession();
    assertThat(storedSession.getId()).isEqualTo(session.getId());
    assertThat(storedSession.getTimeout()).isEqualTo(session.getTimeout());
  }

  @Test
  public void testStart_SessionTimeoutSet() {
    long sessionTimeout = insightSessionManager.getGlobalSessionTimeout() + 2;
    insightSessionManager.setTenantSessionTimeout(sessionTimeout);

    Session session = insightSessionManager.start(null);

    assertThat(session.getTimeout()).isEqualTo(sessionTimeout);
    List<PersistedUserSession> persistedUserSessions = persistedUserSessionDAO.getAll();
    assertThat(persistedUserSessions).hasSize(1);
    Session storedSession = persistedUserSessions.get(0).getSession();
    assertThat(storedSession.getId()).isEqualTo(session.getId());
    assertThat(storedSession.getTimeout()).isEqualTo(session.getTimeout());
  }
}
