/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.invalidateTenant;
import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runAs;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.sonatype.insight.brain.api.admin.service.TenantService;
import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.model.tenancy.DeletedTenant;
import com.sonatype.insight.brain.service.TenantLifecycle;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * Exposes setTenant methods to be called by tenant "entry-points" so that a tenant can be correctly provisioned.
 * <p>
 * This class is currently responsible for Tenant onboarding which will eventually be moved to some external process.
 * <p>
 * This class is ordered before MultiTenantTaskScheduler since tenant registration should happen before starting the
 * task schedulers.
 */
@Named
@Singleton
public class TenantManager
    implements SmartInitializingSingleton
{
  private static final Logger log = LoggerFactory.getLogger(TenantManager.class);

  static final String TENANT_PARAMETER_CANNOT_BE_NULL = "Tenant parameter cannot be null";

  private final Map<Tenant, Boolean> registeredTenants = new ConcurrentHashMap<>();

  private volatile boolean tenantsPreRegistered = false;

  private final Provider<Set<TenantManaged>> tenantManagedBeansProvider;

  // This provider avoids a circular dependency between tenant lifecycle collaborators.
  private final Provider<TenantLifecycle> tenantLifecycle;

  private final DatabaseProvisioner databaseProvisioner;

  private final TenantValidator tenantValidator;

  private final DeletedTenantDAO deletedTenantDAO;

  private final TenantService tenantService;

  @Inject
  public TenantManager(
      final Provider<Set<TenantManaged>> tenantManagedBeansProvider,
      final Provider<TenantLifecycle> tenantLifecycle,
      final DatabaseProvisioner databaseProvisioner,
      final TenantValidator tenantValidator,
      final DeletedTenantDAO deletedTenantDAO,
      final TenantService tenantService)
  {
    this.tenantManagedBeansProvider = tenantManagedBeansProvider;
    this.tenantLifecycle = tenantLifecycle;
    this.databaseProvisioner = databaseProvisioner;
    this.tenantValidator = tenantValidator;
    this.deletedTenantDAO = deletedTenantDAO;
    this.tenantService = tenantService;
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

    tagCurrentSpanWithTenant(tenant);

    validateAndRegisterTenant(tenant);
  }

  private static void tagCurrentSpanWithTenant(final Tenant tenant) {
    Span span = Span.current();
    if (span.getSpanContext().isValid()) {
      span.setAttribute("tenant", tenant.tenantSlug);
    }
  }

  @Override
  public void afterSingletonsInstantiated() {
    ensureTenantsPreRegistered();
  }

  @VisibleForTesting
  void preregisterAllTenants() {
    log.info("Pre-registering all tenants");

    final List<String> deletedTenants = deletedTenantDAO.getAllTenantDeletions()
        .stream()
        .map(DeletedTenant::getId)
        .collect(Collectors.toList());

    final List<String> nonDeletedTenants = tenantService
        .getAllTenantsNames()
        .stream()
        .filter(t -> !deletedTenants.contains(t))
        .collect(Collectors.toList());

    registerTenants(nonDeletedTenants);

    TenantThreadLocal.setGlobalTenant();

    tenantsPreRegistered = true;
  }

  public boolean areTenantsPreRegistered() {
    return tenantsPreRegistered;
  }

  public synchronized void ensureTenantsPreRegistered() {
    if (!tenantsPreRegistered) {
      preregisterAllTenants();
    }
  }

  private void registerTenants(List<String> tenants) {
    TenantThreadLocal.runForAllTenantsOnBoot(tenants, "preRegisterAllTenants",
        tenant -> {
          try {
            setTenant(tenant);
          }
          catch (Exception e) {
            log.error("Failed to register tenant {}", tenant, e);
          }
          finally {
            TenantThreadLocal.invalidateTenant();
          }
        });
  }

  /**
   * Validate and then register a tenant. This method is `synchronized` as it is possible for multiple Quartz jobs for a
   * single tenant to execute at the same time on a single node. But registration should only happen once.
   */
  private synchronized void validateAndRegisterTenant(final Tenant tenant) {
    // Global tenant does not require tenant registration
    if (Tenant.GLOBAL_TENANT.equals(tenant)) {
      return;
    }

    try {
      if (registeredTenants.putIfAbsent(tenant, true) == null) {
        log.info("Starting tenant {} registration", tenant.tenantSlug);
        validateTenant(tenant);
        performRegistration();
        log.info("Tenant {} registration fully complete", tenant.tenantSlug);
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
  @WithSpan // OTel agent instruments private methods via bytecode manipulation (verified in CLM-39873)
  private void performRegistration() {
    databaseProvisioner.initializeDatabaseWithoutMigration();
    setupTenantJobs();
    tenantLifecycle.get().bootTenant();
  }

  /**
   * performDatabaseRegistrationAndRun perform only the database init (not migration) for a tenant and run method. This
   * is used for tenant deletion, where the tenant should not be registered as this causes the Quartz jobs to run.
   */
  @WithSpan
  protected <T> T performDatabaseRegistrationAndRunAs(final String tenantSlug, final Supplier<T> supplier) {
    if (StringUtils.isBlank(tenantSlug)) {
      throw new IllegalArgumentException(TENANT_PARAMETER_CANNOT_BE_NULL);
    }
    Tenant tenant = new Tenant(tenantSlug);

    if (!tenantValidator.validateTenantExists(tenant)) {
      log.debug("Tenant doesn't exist: {}", tenant.tenantSlug);
      throw new IllegalArgumentException(TenantUtil.TENANT_DOES_NOT_EXIST);
    }

    return runAs(tenant, () -> {
      tagCurrentSpanWithTenant(tenant);

      log.info("Registering DB for tenant {}", tenant.tenantSlug);

      try {
        databaseProvisioner.initializeDatabaseWithoutMigration();
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
      throw new IllegalArgumentException(TenantUtil.TENANT_DOES_NOT_EXIST);
    }

    if (deletedTenantDAO.isScheduledForDeletion(tenant.tenantSlug)) {
      log.debug("Tenant has been scheduled for deletion and therefore cannot be used: {}", tenant.tenantSlug);
      throw new IllegalArgumentException(TenantUtil.TENANT_DOES_NOT_EXIST);
    }
  }

  private void setupTenantJobs() {
    tenantManagedBeansProvider.get()
        .stream()
        // GlobalTenantJob are initialized on startup by MultiTenantTenantManagedInitializer rather than per tenant.
        .filter(tenantManaged -> !(tenantManaged instanceof GlobalTenantJob))
        .sorted(comparingInt(TenantManaged::registrationPriority))
        .forEach(tenantManaged -> {
          try {
            tenantManaged.register();
          }
          catch (Exception e) {
            log.error(
                "Failed to register bean {} for tenant {}", tenantManaged.getClass(), TenantThreadLocal.getTenant(), e);
          }
        });
  }

  boolean isRegistered() {
    return isTenantRegistered(TenantThreadLocal.getTenant());
  }

  List<String> getRegisteredTenants() {
    return registeredTenants.keySet()
        .stream()
        .filter(this::isTenantRegistered)
        .map(t -> t.tenantSlug)
        .collect(toList());
  }

  boolean isTenantRegistered(Tenant tenant) {
    Boolean registered = registeredTenants.get(tenant);
    return registered != null && registered;
  }

  @WithSpan
  public void deregisterTenant(String tenantSlug) {
    if (StringUtils.isBlank(tenantSlug)) {
      log.warn("There was an attempt to deregister a tenant with blank slug");
      return;
    }

    for (TenantManaged tenantManagedBean : tenantManagedBeansProvider.get()) {
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

    // Flagging the tenant as not registered instead of removing it from the map, so we can avoid some edge scenarios
    // that may add back the tenant to the map before this is flagged by deletion
    registeredTenants.put(new Tenant(tenantSlug), false);
    log.info("Tenant {} deregistered successfully", tenantSlug);
  }
}
