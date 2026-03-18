/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.function.Supplier;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;

/**
 * This is a reusable version of TenantAwareOneTimeRunnable. It does not clone the tenant so the lifecycle of the tenant
 * is managed outside of this class. That means this can be reused up until the tenant is invalidated by something
 * external to this class. If you need a job to run periodically for a tenant then you should make use of quartz which
 * can correctly handle initializing the tenant for each scheduled execution.
 */
public class TenantAwareRunnable
    implements Runnable
{
  private final Runnable wrapped;

  private final Tenant tenant;

  /*
   * In Shiro 2.0.4+ a change removed the InheritableThreadLocal approach from ThreadContext
   * (it was causing nasty classloader/thread-local cleanup problems in app servers when apps spawned threads)
   */
  private final Subject subject;

  public TenantAwareRunnable(Runnable wrapped) {
    this(wrapped, TenantThreadLocal.getTenant());
  }

  TenantAwareRunnable(Runnable wrapped, Tenant tenant) {
    this.wrapped = wrapped;
    this.tenant = tenant;
    this.subject = ThreadContext.getSecurityManager() != null ? SecurityUtils.getSubject() : null;
  }

  @Override
  public void run() {
    TenantThreadLocal.runAsWithoutValidation(tenant, (Supplier<Void>) () -> {
      if (subject != null) {
        subject.associateWith(wrapped).run();
      }
      else {
        wrapped.run();
      }

      return null;
    });
  }
}
