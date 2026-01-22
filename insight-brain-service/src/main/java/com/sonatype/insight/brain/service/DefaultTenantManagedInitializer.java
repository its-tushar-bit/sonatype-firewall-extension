/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Set;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.tenancy.TenantManaged;

import ru.vyarus.dropwizard.guice.module.installer.order.Order;
import ru.vyarus.dropwizard.guice.module.installer.scanner.InvisibleForScanner;

/**
 * This class ensure that, in a single tenant deployment, job registration/creation happens at startup and
 * de-registration happens on shutdown. Before the introduction of multi tenancy the dropwizard start() method was used
 * for initialization of jobs. The problem with that is it can't be used for tenant provisioning because it ties the
 * initialization to the boot of the application (tenants are initialized at some unknown point in time after boot). To
 * solve that we introduced the TenantJob class and register/deregister methods which can be called outside the boot
 * process.
 * </p>
 * The priority is set to be less than the TaskScheduler to ensure that start() is called on the TaskScheduler before
 * this bean runs so that jobs can be registered correctly and equally so that stop() is called before the TaskScheduler
 * is shutdown. See https://issues.sonatype.org/browse/CLM-24625.
 */
@Named
@Singleton
@Priority(TenantManagedInitializer.PRIORITY)
@Order(Integer.MAX_VALUE - TenantManagedInitializer.PRIORITY)
@InvisibleForScanner
public class DefaultTenantManagedInitializer
    implements TenantManagedInitializer
{
  private final Set<TenantManaged> tenantManagedBeans;

  @Inject
  public DefaultTenantManagedInitializer(final Set<TenantManaged> tenantManagedBeans) {
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
