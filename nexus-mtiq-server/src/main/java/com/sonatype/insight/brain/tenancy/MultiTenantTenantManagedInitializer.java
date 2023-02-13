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

import com.sonatype.insight.brain.service.TenantManagedInitializer;

@Named
@Singleton
public class MultiTenantTenantManagedInitializer
    implements TenantManagedInitializer
{
  private final Collection<TenantManaged> tenantLifecycles;

  @Inject
  public MultiTenantTenantManagedInitializer(final Collection<TenantManaged> tenantLifecycles) {
    this.tenantLifecycles = tenantLifecycles;
  }

  @Override
  public void start() throws Exception {
    // Only global lifecycle jobs are initialized on startup in multi-tenant mode
    for (TenantManaged tenantLifecycle : tenantLifecycles) {
      if (tenantLifecycle instanceof GlobalTenantJob || tenantLifecycle.includeGlobalTenantDuringRegistration()) {
        TenantThreadLocal.runAsGlobal(() -> {
          tenantLifecycle.register();
          return null;
        });
      }
    }
  }

  @Override
  public void stop() throws Exception {
    for (TenantManaged tenantLifecycle : tenantLifecycles) {
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
