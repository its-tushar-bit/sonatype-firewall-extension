/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.session.ExpiredSessionException;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.DelegatingSession;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;

public class AbstractUserService
{
  private final SessionDAO sessionDAO;

  private final DefaultWebSessionManager defaultWebSessionManager;

  protected final CurrentUser currentUser;

  protected final SsoUserService ssoUserService;

  public AbstractUserService(
      final SessionDAO sessionDAO,
      final DefaultWebSessionManager defaultWebSessionManager,
      final CurrentUser currentUser,
      final SsoUserService ssoUserService)
  {
    this.sessionDAO = sessionDAO;
    this.defaultWebSessionManager = defaultWebSessionManager;
    this.currentUser = currentUser;
    this.ssoUserService = ssoUserService;
  }

  protected void deleteUser(SsoUser ssoUser) {
    ssoUserService.deleteSsoUser(ssoUser);
    auditUser(ssoUser);
    logoutUser(ssoUser.getRealmId(), ssoUser.getUsername());
  }

  protected boolean areUsernamesEqual(String realmId, String username1, String username2) {
    if (ssoUserService.isSsoRealm(realmId)) {
      return username1.equals(username2);
    }
    return username1.equalsIgnoreCase(username2);
  }

  protected void logoutUser(String realmId, String username) {
    // Shiro validates sessions periodically (see DefaultWebSessionManager.setSessionValidationInterval).
    // This means sessionDAO.getActiveSessions() may return sessions that are already expired, but were not effectively
    // expired by Shiro.
    // If we try to logout a subject with an expired session, we get an ExpiredSessionException.
    for (Session session : sessionDAO.getActiveSessions()) {
      try {
        // Use a delegating session to ensure the session manager handles and persists session changes
        DelegatingSession delegatingSession =
            new DelegatingSession(defaultWebSessionManager, new DefaultSessionKey(session.getId()));
        Subject subject = new Subject.Builder().session(delegatingSession).buildSubject();
        Object principal = subject.getPrincipal();
        // if the principal is null, then session either has an anonymous Subject,
        // or the subject has already been invalidated by shiro
        if (principal != null && areUsernamesEqual(realmId, username, principal.toString())) {
          subject.logout();
        }
      }
      catch (ExpiredSessionException e) {
        // The session is already expired, which is what we ultimately want.
      }
    }
  }

  protected void validateUserToDeleteIsNotCurrentlyLoggedIn(String realmId, String username) {
    if (areUsernamesEqual(realmId, currentUser.getUsername(), username)) {
      throw new BadRequestException("A user who is logged in cannot delete themself.");
    }
  }

  void auditUser(SsoUser user) {
    auditUser(user.getRealmId(), user.getUsername(), user.getFirstName(), user.getLastName(), user.getEmail());
  }

  protected void auditUser(String realmId, String username, String firstName, String lastName, String email) {
    AuditData.get() //
        .setData("username", username) //
        .setData("firstName", firstName)
        .setData("lastName", lastName) //
        .setData("emailAddress", email)
        .setData("realm", realmId);
  }
}
