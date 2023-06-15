/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.error.exception.BadRequestException;

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

  protected final SamlUserDAO samlUserDAO;

  public AbstractUserService(
      final SessionDAO sessionDAO,
      final DefaultWebSessionManager defaultWebSessionManager,
      final CurrentUser currentUser, final SamlUserDAO samlUserDAO)
  {
    this.sessionDAO = sessionDAO;
    this.defaultWebSessionManager = defaultWebSessionManager;
    this.currentUser = currentUser;
    this.samlUserDAO = samlUserDAO;
  }

  protected void deleteUser(SamlUser samlUser) {
    samlUserDAO.delete(samlUser);
    auditUser(samlUser);
    logoutUser(SamlUser.SAML_REALM_ID, samlUser.getUsername());
  }

  protected boolean areUsernamesEqual(String realmId, String username1, String username2) {
    if (SamlUser.SAML_REALM_ID.equals(realmId)) {
      return username1.equals(username2);
    }
    return username1.equalsIgnoreCase(username2);
  }

  protected void logoutUser(String realmId, String username) {
    for (Session session : sessionDAO.getActiveSessions()) {
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
  }

  protected void validateUserToDeleteIsNotCurrentlyLoggedIn(String realmId, String username) {
    if (areUsernamesEqual(realmId, currentUser.getUsername(), username)) {
      throw new BadRequestException("A user who is logged in cannot delete themself.");
    }
  }

  void auditUser(SamlUser user) {
    auditUser(SamlUser.SAML_REALM_ID, user.getUsername(), user.getFirstName(), user.getLastName(), user.getEmail());
  }

  protected void auditUser(String realmId, String username, String firstName, String lastName, String email) {
    AuditData.get() //
        .setData("username", username) //
        .setData("firstName", firstName).setData("lastName", lastName) //
        .setData("emailAddress", email)
        .setData("realm", realmId);
  }
}

