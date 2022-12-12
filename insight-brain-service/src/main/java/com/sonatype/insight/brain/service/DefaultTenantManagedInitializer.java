/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Collection;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.tenancy.TenantManaged;

/**
 * This class ensure that, in a single tenant deployment, job registration/creation happens at startup and
 * de-registration happens on shutdown.
 * Before the introduction of multi tenancy the dropwizard start() method was used for
 * initialzation of jobs. The problem with that is it can't be used for tenant provisioning because it ties the
 * initialization to the boot of the application (tenants are initialized at some unknown point in time after boot). To
 * solve that we introduced the TenantJob class and register/deregister methods which can be called outside the boot
 * process.
 */
@Named
@Singleton
public class DefaultTenantManagedInitializer
    implements TenantManagedInitializer
{
  private final Collection<TenantManaged> tenantManagedBeans;

  @Inject
  public DefaultTenantManagedInitializer(final Collection<TenantManaged> tenantManagedBeans) {
    this.tenantManagedBeans = tenantManagedBeans;
  }

  @Override
  public void start() throws Exception {
    for (TenantManaged tenantLifecycle : tenantManagedBeans) {
      tenantLifecycle.register();
    }
  }

  @Override
  public void stop() throws Exception {
    for (TenantManaged tenantLifecycle : tenantManagedBeans) {
      tenantLifecycle.deregister();
    }
  }
}
