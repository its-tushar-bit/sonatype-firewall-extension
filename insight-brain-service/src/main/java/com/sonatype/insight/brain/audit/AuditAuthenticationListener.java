/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import javax.inject.Named;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationListener;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.PrincipalCollection;

/**
 * Audits the username for explicit authentication attempts. Note that this listener is not notified in case of
 * cookie-based authentication (successful or otherwise) or anonymous access.
 */
@Named
class AuditAuthenticationListener
    implements AuthenticationListener
{
  @Override
  public void onSuccess(AuthenticationToken token, AuthenticationInfo info) {
    auditUsername(token);
  }

  @Override
  public void onFailure(AuthenticationToken token, AuthenticationException ae) {
    auditUsername(token);
    // explicitly mark as authentication failure which ErrorResponseGenerator might report as code 500 and not 401
    AuditData.get().setEvent(AuditEvent.AUTHENTICATION_FAILURE);
  }

  @Override
  public void onLogout(PrincipalCollection principals) {
    // noop, logout is audited by AuditSessionListener
  }

  private void auditUsername(AuthenticationToken token) {
    Object principal = token.getPrincipal();
    AuditData.get().setUsername(principal != null ? principal.toString() : null);
  }
}
