/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.Collection;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.TenantManagedInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The priority is set to be less than the TaskScheduler to ensure that start() is called on the TaskScheduler before
 * this bean runs so that jobs can be registered correctly and equally so that stop() is called before the TaskScheduler
 * is shutdown. See https://issues.sonatype.org/browse/CLM-24625
 */
@Named
@Singleton
@Priority(TaskScheduler.TASK_SCHEDULER_BEAN_PRIORITY - 1)
public class MultiTenantTenantManagedInitializer
    implements TenantManagedInitializer
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantTenantManagedInitializer.class);

  private final Collection<TenantManaged> tenantLifecycles;

  private final TenantUtil tenantUtil;

  @Inject
  public MultiTenantTenantManagedInitializer(final Collection<TenantManaged> tenantLifecycles,
                                             final TenantUtil tenantUtil)
  {
    this.tenantLifecycles = tenantLifecycles;
    this.tenantUtil = tenantUtil;
  }

  @Override
  public void start() throws Exception {
    // Global lifecycle jobs and jobs that are intended to run for all tenants (AllTenantsJob) are initialized on
    // startup in multi-tenant mode
    for (TenantManaged tenantLifecycle : tenantLifecycles) {
      if (tenantLifecycle instanceof GlobalTenantJob
          || (tenantLifecycle instanceof AllTenantsJob && tenantUtil.isMtiqBatchMode())
          || tenantLifecycle.includeGlobalTenantDuringRegistration()) {
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
          try {
            tenantLifecycle.deregister();
          }
          catch (Exception e) {
            log.error("Failed to deregister job {} during shutdown ", tenantLifecycle.getClass(), e);
          }
          return null;
        });
      }
      else {
        tenantLifecycle.deregister();
      }
    }
  }
}
