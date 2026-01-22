/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import jakarta.inject.Named;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionListenerAdapter;

/**
 * Audits start and stop of Shiro sessions. When using reverse proxy authentication (RUT) users do not log into the app
 * using our login modal/resource. We therefore track session events to deduce login and logout activity (as a best
 * effort for RUT).
 */
@Named
public class AuditSessionListener
    extends SessionListenerAdapter
{
  @Override
  public void onStart(Session session) {
    audit(AuditEvent.LOGIN);
  }

  @Override
  public void onStop(Session session) {
    audit(AuditEvent.LOGOUT);
  }

  private void audit(AuditEvent event) {
    try (AuditSession auditSession = AuditData.get().recordSubEvent(event, true)) {
      // no additional data to collect, just close the session and commit the audit data
    }
  }
}
