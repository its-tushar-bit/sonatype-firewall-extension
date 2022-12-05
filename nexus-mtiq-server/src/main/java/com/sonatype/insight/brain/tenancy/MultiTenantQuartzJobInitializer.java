/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.Collection;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.QuartzJobInitializer;

@Named
@Singleton
public class MultiTenantQuartzJobInitializer
    implements QuartzJobInitializer
{
  private final Collection<TenantJob> tenantLifecycles;

  @Inject
  public MultiTenantQuartzJobInitializer(final Collection<TenantJob> tenantLifecycles) {
    this.tenantLifecycles = tenantLifecycles;
  }

  @Override
  public void start() throws Exception {
    // Only global lifecycle jobs are initialized on startup in multi-tenant mode
    for (TenantJob tenantLifecycle : tenantLifecycles) {
      if (tenantLifecycle instanceof GlobalTenantJob) {
        TenantThreadLocal.runAsGlobal(() -> {
          tenantLifecycle.register();
          return null;
        });
      }
    }
  }

  @Override
  public void stop() throws Exception {
    for (TenantJob tenantLifecycle : tenantLifecycles) {
      if (tenantLifecycle instanceof GlobalTenantJob) {
        TenantThreadLocal.runAsGlobal(() -> {
          tenantLifecycle.deregister();
          return null;
        });
      }
      else {
        tenantLifecycle.deregister();
      }
    }
  }
}
