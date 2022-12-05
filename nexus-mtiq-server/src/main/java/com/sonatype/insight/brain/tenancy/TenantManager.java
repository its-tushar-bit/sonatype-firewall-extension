/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.ApplicationLifecycle;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages access to tenants
 */
@Named
@Singleton
public class TenantManager
{
  private static final Logger log = LoggerFactory.getLogger(TenantManager.class);

  private final Map<Tenant, Boolean> registeredTenants = new ConcurrentHashMap<>();

  private final Collection<TenantJob> tenantJobs;

  private final InsightConfig insightConfig;

  private final ApplicationLifecycle applicationLifecycle;

  private final DatabaseProvisionUtils databaseProvisionUtils;

  @Inject
  public TenantManager(
      final Collection<TenantJob> tenantJobs,
      final InsightConfig insightConfig,
      final ApplicationLifecycle applicationLifecycle,
      final DatabaseProvisionUtils databaseProvisionUtils)
  {
    this.tenantJobs = tenantJobs;
    this.insightConfig = insightConfig;
    this.applicationLifecycle = applicationLifecycle;
    this.databaseProvisionUtils = databaseProvisionUtils;
  }

  /**
   * Set the global tenant. Should be called at any 'init' point such as application start or schedules. Specifically
   * where there is no tenant set (such as through a URL). Will ensure that the {@link TenantThreadLocal} is properly
   * set to the global tenant.
   */
  public static void initGlobalTenant() {
    TenantUtil.setMultiTenantMode();

    TenantThreadLocal.setGlobalTenant();
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
        performRegistration(tenant);
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
        () -> databaseProvisionUtils.initializeDatabasesWithoutMigration(insightConfig));

    start = runAndLogTime("jobs init", tenant, start, this::setupTenantJobs);

    runAndLogTime("app boot", tenant, start, this::applicationBoot);
  }

  private void applicationBoot() {
    try {
      applicationLifecycle.boot();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void setupTenantJobs() {
    for (TenantJob tenantJob : tenantJobs) {
      if (tenantJob instanceof GlobalTenantJob) {
        // Global lifecycles are not set here. See QuartzJobInitializer for that.
        continue;
      }

      tenantJob.register();
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
