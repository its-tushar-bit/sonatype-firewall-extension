/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.DatabaseConfigProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TenantLifecycle;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;

/**
 * Exposes setTenant methods to be called by tenant "entry-points" so that a tenant can be correctly provisioned.
 * <p>
 * This class is currently responsible for Tenant onboarding which will eventually be moved to some external process.
 */
@Named
@Singleton
public class TenantManager
{
  private static final Logger log = LoggerFactory.getLogger(TenantManager.class);

  private final Map<Tenant, Boolean> registeredTenants = new ConcurrentHashMap<>();

  private final Collection<TenantManaged> tenantManagedBeans;

  private final InsightConfig insightConfig;

  // This is a provider to prevent circular dependencies between Guice beans
  private final Provider<TenantLifecycle> tenantLifecycle;

  private final DatabaseProvisionUtils databaseProvisionUtils;

  private final DatabaseConfigProvider databaseConfigProvider;

  @Inject
  public TenantManager(
      final Collection<TenantManaged> tenantManagedBeans,
      final InsightConfig insightConfig,
      final Provider<TenantLifecycle> tenantLifecycle,
      final DatabaseProvisionUtils databaseProvisionUtils,
      final DatabaseConfigProvider databaseConfigProvider)
  {
    this.tenantManagedBeans = tenantManagedBeans;
    this.insightConfig = insightConfig;
    this.tenantLifecycle = tenantLifecycle;
    this.databaseProvisionUtils = databaseProvisionUtils;
    this.databaseConfigProvider = databaseConfigProvider;
  }

  public Tenant getTenant() {
    return TenantThreadLocal.getTenant();
  }

  void setTenant(final String tenant) {
    if (tenant == null) {
      throw new IllegalArgumentException("Tenant parameter cannot be null");
    }

    setTenant(new Tenant(tenant));
  }

  void setTenant(final Tenant tenant) {
    if (tenant == null) {
      throw new IllegalArgumentException("Tenant parameter cannot be null.");
    }

    TenantThreadLocal.setTenant(tenant);

    registerTenant(tenant);
  }

  private void registerTenant(final Tenant tenant) {
    // Global tenant does not require tenant registration
    if (Tenant.GLOBAL_TENANT.equals(tenant)) {
      return;
    }

    try {
      if (registeredTenants.putIfAbsent(tenant, true) == null) {
        runAndLogTime("registration", tenant, System.currentTimeMillis(), () -> performRegistration(tenant));
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Perform all registration for a tenant: database init (not migration), tenant jobs, and app lifecycle boot
   */
  private void performRegistration(final Tenant tenant) {
    log.info("Registering tenant {}", tenant.tenantSlug);

    long start = runAndLogTime("database init", tenant, System.currentTimeMillis(),
        () -> databaseProvisionUtils.initializeDatabases(insightConfig, databaseConfigProvider));

    start = runAndLogTime("jobs init", tenant, start, this::setupTenantJobs);

    runAndLogTime("app boot", tenant, start, tenantLifecycle.get()::bootTenant);
  }

  private void setupTenantJobs() {
    List<TenantManaged> prioritizedBeans = tenantManagedBeans.stream()
        .sorted(comparingInt(TenantManaged::registrationPriority))
        .collect(toList());

    for (TenantManaged tenantManaged : prioritizedBeans) {
      if (tenantManaged instanceof GlobalTenantJob) {
        // Global lifecycles are not set here. See QuartzJobInitializer for that.
        continue;
      }

      tenantManaged.register();
    }
  }

  private long runAndLogTime(
      final String name,
      final Tenant tenant,
      final long start,
      final Runnable runnable)
  {
    runnable.run();

    log.info("Tenant {} {} completed in {}ms", tenant.tenantSlug, name, System.currentTimeMillis() - start);

    return System.currentTimeMillis();
  }
}
