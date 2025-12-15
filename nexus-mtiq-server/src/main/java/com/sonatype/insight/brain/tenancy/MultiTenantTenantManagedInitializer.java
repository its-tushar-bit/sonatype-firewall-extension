/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.List;
import java.util.Set;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.TenantManagedInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.module.installer.order.Order;
import ru.vyarus.dropwizard.guice.module.installer.scanner.InvisibleForScanner;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;

/**
 * The priority is set to be less than the TaskScheduler to ensure that start() is called on the TaskScheduler before
 * this bean runs so that jobs can be registered correctly and equally so that stop() is called before the TaskScheduler
 * is shutdown. See https://issues.sonatype.org/browse/CLM-24625
 */
@Named
@Singleton
@Priority(TenantManagedInitializer.PRIORITY)
@Order(Integer.MAX_VALUE - TenantManagedInitializer.PRIORITY)
@InvisibleForScanner
public class MultiTenantTenantManagedInitializer
    implements TenantManagedInitializer
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantTenantManagedInitializer.class);

  private final Provider<Set<TenantManaged>> tenantLifecyclesProvider;

  private final TenantUtil tenantUtil;

  @Inject
  public MultiTenantTenantManagedInitializer(final Provider<Set<TenantManaged>> tenantLifecyclesProvider,
                                             final TenantUtil tenantUtil)
  {
    this.tenantLifecyclesProvider = tenantLifecyclesProvider;
    this.tenantUtil = tenantUtil;
  }

  @Override
  public void start() throws Exception {
    List<TenantManaged> prioritizedLifecycles = tenantLifecyclesProvider.get().stream()
        .sorted(comparingInt(TenantManaged::registrationPriority))
        .collect(toList());

    // Global lifecycle jobs are initialized on startup in multi-tenant mode
    for (TenantManaged tenantLifecycle : prioritizedLifecycles) {

      // MtiqBatchJobs must only be created on a server running in Batch Mode
      if (tenantLifecycle instanceof MtiqBatchJob && !tenantUtil.isMtiqBatchMode()) {
        continue;
      }

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
    for (TenantManaged tenantLifecycle : tenantLifecyclesProvider.get()) {
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
