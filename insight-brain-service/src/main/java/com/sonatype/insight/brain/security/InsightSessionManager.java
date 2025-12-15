/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.security.ShiroSessionDAO;
import com.sonatype.insight.brain.tenancy.TenantReference;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionListener;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;

@Named
@Singleton
public class InsightSessionManager
    extends DefaultWebSessionManager
{
  private final TenantReference<Long> tenantSessionTimeout = new TenantReference<>();

  @Inject
  public InsightSessionManager(
      final ShiroSessionDAO shiroSessionDAO,
      final Set<SessionListener> sessionListeners)
  {
    setSessionDAO(shiroSessionDAO);
    setSessionListeners(sessionListeners);

    // Disable Shiro's default ExecutorServiceSessionValidationScheduler to prevent memory leaks in tests.
    // In production, QuartzShiroSessionValidationScheduler is used instead (configured separately).
    setSessionValidationSchedulerEnabled(false);
  }

  public void setTenantSessionTimeout(Long sessionTimeout) {
    if (sessionTimeout == null) {
      tenantSessionTimeout.remove();
    }
    else {
      tenantSessionTimeout.set(sessionTimeout);
    }
  }

  @Override
  public Session start(SessionContext context) {
    Session session = super.start(context);
    Long sessionTimeout = tenantSessionTimeout.get();
    if (sessionTimeout != null) {
      session.setTimeout(sessionTimeout);
    }
    return session;
  }
}
