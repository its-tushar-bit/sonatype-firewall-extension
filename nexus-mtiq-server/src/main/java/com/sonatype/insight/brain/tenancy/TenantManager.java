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
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.db.MultiTenantDatabaseConfigProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TenantLifecycle;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;

import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.invalidateTenant;
import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runAs;
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

  static final String TENANT_PARAMETER_CANNOT_BE_NULL = "Tenant parameter cannot be null";

  static final String TENANT_DOES_NOT_EXIST = "Tenant does not exist";

  private final Map<Tenant, Boolean> registeredTenants = new ConcurrentHashMap<>();

  private final Collection<TenantManaged> tenantManagedBeans;

  private final MultiTenantDatabaseConfigProvider multiTenantDatabaseConfigProvider;

  // This is a provider to prevent circular dependencies between Guice beans
  private final Provider<TenantLifecycle> tenantLifecycle;

  private final DatabaseProvisionUtils databaseProvisionUtils;

  private final TenantValidator tenantValidator;

  private final DeletedTenantDAO deletedTenantDAO;

  @Inject
  public TenantManager(
      final Collection<TenantManaged> tenantManagedBeans,
      final InsightConfig insightConfig,
      final Provider<TenantLifecycle> tenantLifecycle,
      final DatabaseProvisionUtils databaseProvisionUtils,
      final TenantValidator tenantValidator,
      final DeletedTenantDAO deletedTenantDAO)
  {
    this.tenantManagedBeans = tenantManagedBeans;
    this.tenantLifecycle = tenantLifecycle;
    this.databaseProvisionUtils = databaseProvisionUtils;
    this.tenantValidator = tenantValidator;
    this.deletedTenantDAO = deletedTenantDAO;

    multiTenantDatabaseConfigProvider = new MultiTenantDatabaseConfigProvider(insightConfig);
  }

  public Tenant getTenant() {
    return TenantThreadLocal.getTenant();
  }

  void setTenant(final String tenant) {
    if (StringUtils.isBlank(tenant)) {
      throw new IllegalArgumentException(TENANT_PARAMETER_CANNOT_BE_NULL);
    }

    setTenant(new Tenant(tenant));
  }

  void setTenantForAdminRequest(final String tenant) {
    if (tenant == null) {
      throw new IllegalArgumentException(TENANT_PARAMETER_CANNOT_BE_NULL);
    }

    TenantThreadLocal.setTenant(new Tenant(tenant));
  }

  void setTenant(final Tenant tenant) {
    if (tenant == null) {
      throw new IllegalArgumentException(TENANT_PARAMETER_CANNOT_BE_NULL);
    }

    TenantThreadLocal.setTenant(tenant);

    final Span span = GlobalTracer.get().activeSpan();
    if (span != null) {
      span.setTag("tenant", tenant.tenantSlug);
    }

    registerTenant(tenant);
  }

  private void registerTenant(final Tenant tenant) {
    // Global tenant does not require tenant registration
    if (Tenant.GLOBAL_TENANT.equals(tenant)) {
      return;
    }

    try {
      if (registeredTenants.putIfAbsent(tenant, true) == null) {
        long start = runAndLogTime("validate tenant", tenant, System.currentTimeMillis(),
            () -> validateTenant(tenant));

        runAndLogTime("registration", tenant, start, () -> performRegistration(tenant));
      }
    }
    catch (IllegalArgumentException e) {
      registeredTenants.remove(tenant);
      throw e;
    }
    catch (Exception e) {
      registeredTenants.remove(tenant);
      throw new RuntimeException(e);
    }
  }

  /**
   * Perform all registration for a tenant: database init (not migration), tenant jobs, and app lifecycle boot
   */
  private void performRegistration(final Tenant tenant) {
    log.info("Registering tenant {}", tenant.tenantSlug);

    long start = runAndLogTime("database init", tenant, System.currentTimeMillis(),
        () -> databaseProvisionUtils.initializeDatabasesWithoutMigration(multiTenantDatabaseConfigProvider));

    start = runAndLogTime("jobs init", tenant, start, this::setupTenantJobs);

    runAndLogTime("app boot", tenant, start, tenantLifecycle.get()::bootTenant);
  }

  /**
   * performDatabaseRegistrationAndRun perform only the database init (not migration) for a tenant and run method
   * This is used for tenant deletion, where the tenant should not be registered as this causes the Quartz jobs to run.
   */
  protected  <T> T performDatabaseRegistrationAndRunAs(final String tenantSlug, final Supplier<T> supplier) {
    if (StringUtils.isBlank(tenantSlug)) {
      throw new IllegalArgumentException(TENANT_PARAMETER_CANNOT_BE_NULL);
    }
    Tenant tenant = new Tenant(tenantSlug);

    if (!tenantValidator.validateTenantExists(tenant)) {
      log.debug("Tenant doesn't exist: {}", tenant.tenantSlug);
      throw new IllegalArgumentException(TENANT_DOES_NOT_EXIST);
    }

    return runAs(tenant, () -> {
      final Span span = GlobalTracer.get().activeSpan();
      if (span != null) {
        span.setTag("tenant", tenant.tenantSlug);
      }

      log.info("Registering DB for tenant {}", tenant.tenantSlug);

      try {
        databaseProvisionUtils.initializeDatabasesWithoutMigration(multiTenantDatabaseConfigProvider);
        return supplier.get();
      }
      finally {
        invalidateTenant();
      }
    });
  }

  /**
   * Validates a tenant exists and is not deleted before registration
   */
  private void validateTenant(final Tenant tenant) {
    if (!tenantValidator.validateTenantExists(tenant)) {
      log.debug("Tenant doesn't exist: {}", tenant.tenantSlug);
      throw new IllegalArgumentException(TENANT_DOES_NOT_EXIST);
    }

    if (deletedTenantDAO.isScheduledForDeletion(tenant.tenantSlug)) {
      log.debug("Tenant has been scheduled for deletion and therefore cannot be used: {}", tenant.tenantSlug);
      throw new IllegalArgumentException(TENANT_DOES_NOT_EXIST);
    }
  }

  private void setupTenantJobs() {
    List<TenantManaged> prioritizedBeans = tenantManagedBeans.stream()
        .sorted(comparingInt(TenantManaged::registrationPriority))
        .collect(toList());

    for (TenantManaged tenantManaged : prioritizedBeans) {
      if (tenantManaged instanceof GlobalTenantJob) {
        /*
          GlobalTenantJob are initialized on startup by MultiTenantTenantManagedInitializer rather than per tenant.
         */
        continue;
      }

      try {
        tenantManaged.register();
      }
      catch (Exception e) {
        log.error("Failed to load bean {} for tenant {}", tenantManaged.getClass(), TenantThreadLocal.getTenant(), e);
      }
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

  boolean isRegistered() {
    Boolean registered = registeredTenants.get(TenantThreadLocal.getTenant());

    return registered != null && registered;
  }

  List<String> getRegisteredTenants() {
    return registeredTenants.keySet().stream().map(t -> t.tenantSlug).collect(toList());
  }

  public void deregisterTenant(String tenantSlug) {
    if (StringUtils.isNotBlank(tenantSlug)) {
      for (TenantManaged tenantManagedBean : tenantManagedBeans) {
        runAs(new Tenant(tenantSlug), () -> {
          try {
            tenantManagedBean.deregister();
          }
          catch (Exception e) {
            log.error("Failed to deregister managed bean {} for tenant {}", tenantManagedBean.getClass(), tenantSlug,
                e);
          }
          return null;
        });
      }

      registeredTenants.remove(new Tenant(tenantSlug));
    }
  }
}
