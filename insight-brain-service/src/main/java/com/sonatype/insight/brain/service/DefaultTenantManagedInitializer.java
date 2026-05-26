/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Set;
import org.springframework.context.annotation.DependsOn;

/**
 * This class ensure that, in a single tenant deployment, job registration/creation happens at startup and
 * de-registration happens on shutdown. Before the introduction of multi tenancy the dropwizard start() method was used
 * for initialization of jobs. The problem with that is it can't be used for tenant provisioning because it ties the
 * initialization to the boot of the application (tenants are initialized at some unknown point in time after boot). To
 * solve that we introduced the TenantJob class and register/deregister methods which can be called outside the boot
 * process.
 * </p>
 * This bean explicitly depends on application boot and starts the scheduler before registering tenant-managed jobs. The
 * direct scheduler dependency also ensures this bean is destroyed before the scheduler shuts down. See
 * https://issues.sonatype.org/browse/CLM-24625.
 */
@Named("defaultTenantManagedInitializer")
@Singleton
@DependsOn({"staticInjectionInitializer", "defaultApplicationLifecycle", "taskScheduler"})
public class DefaultTenantManagedInitializer
    implements TenantManagedInitializer
{
  private final Set<TenantManaged> tenantManagedBeans;

  private final TaskScheduler taskScheduler;

  private volatile boolean started;

  @Inject
  public DefaultTenantManagedInitializer(
      final Set<TenantManaged> tenantManagedBeans,
      final TaskScheduler taskScheduler)
  {
    this.tenantManagedBeans = tenantManagedBeans;
    this.taskScheduler = taskScheduler;
  }

  @Override
  public void start() throws Exception {
    taskScheduler.start();

    for (TenantManaged tenantLifecycle : tenantManagedBeans) {
      tenantLifecycle.register();
    }
    started = true;
  }

  @Override
  public void stop() throws Exception {
    if (!started) {
      return;
    }
    for (TenantManaged tenantLifecycle : tenantManagedBeans) {
      tenantLifecycle.deregister();
    }
  }
}
