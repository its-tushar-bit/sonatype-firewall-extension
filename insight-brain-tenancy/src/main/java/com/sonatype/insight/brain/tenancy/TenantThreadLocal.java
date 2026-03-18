/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT;

/**
 * Java {@link ThreadLocal} to manage IQ tenants in our IQ multi-tenant saas offering. Application code should always
 * prefer the `TenantManager` class in the insight-brain-service module if possible.
 */
public class TenantThreadLocal
{
  private static final Logger log = LoggerFactory.getLogger(TenantThreadLocal.class);

  private static Tenant defaultTenant = SINGLE_TENANT;

  private static final ThreadLocal<TenantState> tenantThreadLocal = new InheritableThreadLocal<>()
  {
    // NOTE: because this is an InheritableThreadLocal, this method is only actually called to set the value for the
    // `main` thread. All other application threads use `InheritableThreadLocal#childValue()` instead
    @Override
    protected TenantState initialValue() {
      return new TenantState(null, defaultTenant);
    }
  };

  // Visible for testing
  static TenantUtil tenantUtil = new TenantUtil();

  public static void setDefaultTenantToGlobal() {
    defaultTenant = GLOBAL_TENANT;
  }

  /**
   * Returns the tenant from the ThreadLocal. TenantManager in the insight-brain-service module should ALWAYS be
   * preferred. The tenant returned is validated (see {@link TenantUtil#validateTenant(Tenant)}.
   */
  public static Tenant getTenant() {
    // Check the ThreadLocal if a tenant is set yet
    Tenant tenant = tenantUtil.validateTenant(getTenantWithoutValidation());

    checkPermission(tenantThreadLocal.get().current);

    return tenant;
  }

  /**
   * Returns the tenant from the ThreadLocal without doing any validation. This should only be called in circumstances
   * where we don't care about the tenant being set. Ultimately this can be removed when validation is no longer
   * needed.
   */
  static Tenant getTenantWithoutValidation() {
    return tenantThreadLocal.get().current;
  }

  /**
   * Verify that the passed tenant has permission to be used. The goal is to prevent an incorrect tenant being picked up
   * and data leaking between tenants. This is especially true in the case of threads that are shared within ThreadPools
   * Currently, this checks that the tenant has not been invalidates but further checks may well be introduced in the
   * future.
   */
  private static void checkPermission(Tenant tenant) {
    Tenant currentTenant = tenantThreadLocal.get().current;
    Tenant previousTenant = tenantThreadLocal.get().previous;

    // This is initialization, once the tenant has been set to a value it can no longer be nullified.
    if (currentTenant == null) {
      return;
    }

    // Everyone has access to the global tenant.
    if (tenant.equals(GLOBAL_TENANT)) {
      return;
    }

    // Once a tenant has been invalidated it should never be used again
    checkTenantValid(tenant);

    // No need to perform further validation if this is essentially a no-op, can always re-set the current tenant or go
    // back to the previous tenant.
    if (tenant.equals(currentTenant) || tenant.equals(previousTenant)) {
      return;
    }

    // Cannot go from a valid tenant to another valid tenant. Must invalidate the current tenant first to ensure that it
    // cannot be reused.
    if (!GLOBAL_TENANT.equals(currentTenant) && !currentTenant.isInvalid()) {
      throw new InvalidTenantOperationException(
          "Cannot transition from one valid tenant to another. This is to prevent data leakage." + tenantInfo(tenant));
    }

    // Cannot use the global tenant to transition from a valid tenant to another valid tenant. First the valid tenant
    // must be invalidated before changing tenant, to prevent reuse. Alternatively can transition back to the previous
    // tenant.
    if (GLOBAL_TENANT.equals(currentTenant)
        && previousTenant != null
        && !previousTenant.isInvalid())
    {
      throw new InvalidTenantOperationException(
          "Cannot transition from one valid tenant to another via Global. This is to prevent " +
              "data leakage." + tenantInfo(tenant));
    }
  }

  private static void checkTenantValid(Tenant tenant) {
    if (tenant.isInvalid()) {
      throw new InvalidTenantOperationException(
          "Attempting to use a tenant from a previous request/process." + tenantInfo(tenant));
    }
  }

  private static String tenantInfo(Tenant tenant) {
    Tenant currentTenant = tenantThreadLocal.get().current;
    Tenant previousTenant = tenantThreadLocal.get().previous;

    String format = " currentTenant=%s, previousTenant=%s, newTenant=%s";

    return String.format(format, currentTenant, previousTenant, tenant);
  }

  /**
   * PACKAGE PRIVATE!!! There are situations where we temporarily need a tenant with its own lifecycle. For example when
   * a request comes in and wants to trigger an async process that will finish after the request has finished. Only
   * tenants that are currently in a valid state can be cloned otherwise checkPermission will fail. This is to prevent
   * this method being used to work around the tenant security.
   */
  static Tenant cloneTenant(Tenant tenant) {
    if (GLOBAL_TENANT.equals(tenant) || SINGLE_TENANT.equals(tenant)) {
      // Never create new instances of the system tenants
      return tenant;
    }

    checkPermission(tenant);

    return new Tenant(tenant.tenantSlug);
  }

  /**
   * PACKAGE PRIVATE!!! This setter is to remain package private. Only the boundaries of the system (e.g.
   * TenantUrlFilter, TenantContextJobListener) should be able to set the tenant. If code needs to run with a particular
   * tenant it should make use of {@link TenantThreadLocal#runAs(Tenant, Supplier)} which correctly handles restoring
   * the tenant when the work is done.
   */
  static void setTenant(final Tenant tenant) {
    checkPermission(tenant);

    if (tenant == null) {
      throw new IllegalArgumentException("Tenant parameter cannot be null");
    }

    if (getTenantWithoutValidation() != null
        && GLOBAL_TENANT == getTenantWithoutValidation()
        && GLOBAL_TENANT == tenant)
    {
      // There is no change in the tenant so setting the tenant should be a no-op
      return;
    }

    setTenantWithoutValidation(tenant);
  }

  static void setTenantWithoutValidation(Tenant tenant) {
    updateLoggingContext(tenant);

    // Note: passing in an exception to get a stacktrace in the log
    log.trace("Setting tenant to {} from {}", tenant, tenantThreadLocal.get().current, new Exception());
    tenantThreadLocal.set(new TenantState(tenant));
  }

  private static void updateLoggingContext(final Tenant tenant) {
    if (SINGLE_TENANT.equals(tenant)) {
      clearLoggingContext();
    }
    else {
      MDC.put("tenant", tenant.tenantSlug);
    }
  }

  private static void clearLoggingContext() {
    MDC.remove("tenant");
  }

  /**
   * PACKAGE PRIVATE!!! This setter is to remain package private. Use the TenantUtil class to init the global tenant
   */
  static void setGlobalTenant() {
    setTenant(GLOBAL_TENANT);
  }

  static void runForAllTenantsOnBatch(List<String> tenants, String taskName, Consumer<Tenant> consumer) {
    if (tenantUtil.isMtiqBatchMode()) {
      runForAllTenants(tenants, taskName, consumer);
    }
  }

  static void runForAllTenantsOnBoot(
      List<String> tenants,
      String taskName,
      Consumer<Tenant> consumer)
  {
    runForAllTenants(tenants, taskName, consumer);
  }

  /**
   * PACKAGE PRIVATE!!! Only trusted callers should be able to run code as a specific tenant. Note: Using this method
   * will invalidate the tenant when finished.
   * </p>
   * Running across tenants is only allowed when the node is running in quartz mode or while pre-registering tenants
   * during server startup. This is to prevent any request traffic from being able to iterate through tenants.
   */
  private static void runForAllTenants(
      List<String> tenants,
      String taskName,
      Consumer<Tenant> consumer)
  {
    if (!tenantUtil.isMultiTenant()) {
      consumer.accept(SINGLE_TENANT);

      return;
    }

    log.info("Running task {} for all registered tenants. Tenant count = {}", taskName, tenants.size());

    for (String tenantName : tenants) {

      log.debug("Running task {} for tenant {}", taskName, tenantName);

      try {
        TenantThreadLocal.setGlobalTenant();

        Tenant tenant = new Tenant(tenantName);

        runAs(tenant, () -> {
          try {
            consumer.accept(tenant);
            return null;
          }
          finally {
            invalidateTenant();
          }
        });
      }
      catch (Exception e) {
        log.error("runForAllTenants failed to run consumer for tenant {}, skipping and moving on to next tenant",
            tenantName, e);
      }
    }
  }

  public static <T> T runAsGlobal(Supplier<T> supplier) {
    return runAs(GLOBAL_TENANT, supplier);
  }

  /**
   * PACKAGE PRIVATE!!! Only trusted callers should be able to run code as a specific tenant.
   */
  static <T> T runAs(Tenant tenant, Supplier<T> supplier) {
    checkPermission(tenant);
    return runAsWithoutValidation(tenant, supplier);
  }

  /**
   * PACKAGE PRIVATE!!! Only trusted callers should be able to run code as a specific tenant. This method exists for
   * trusted code that does not need to care what the pre-existing thread-local tenancy situation is, it just
   * knows it needs to run the provided code in the provided tenant and then put things back the way they were
   * afterwards. In particular `TenantAwareRunnable` and similar operate this way.
   * Note: still checks that the new tenant isValid, but skips checks related to the previous tenant
   */
  static <T> T runAsWithoutValidation(Tenant tenant, Supplier<T> supplier) {
    checkTenantValid(tenant);
    if (!tenantUtil.isMultiTenant()) {
      return supplier.get();
    }

    // This is likely, called in a new thread, so it is likely the tenant is null. That is not guaranteed though
    // because this thread could be used in a thread pool
    Tenant previous = getTenantWithoutValidation();
    try {
      setTenantWithoutValidation(tenant);

      return supplier.get();
    }
    finally {
      if (previous == null) {
        previous = GLOBAL_TENANT;
      }

      setTenantWithoutValidation(previous);
    }
  }

  static void invalidateTenant() {
    if (!tenantUtil.isMultiTenant()) {
      return;
    }

    // Note: passing in an exception to get a stacktrace in the log
    log.trace("Invalidating tenant {}", getTenantWithoutValidation(), new Exception());
    getTenantWithoutValidation().invalidate();
    clearLoggingContext();
  }

  // Visible for test
  static void resetTenantForTesting() {
    tenantThreadLocal.remove();
    TenantThreadLocal.setGlobalTenant();
  }

  private static class TenantState
  {
    private final Tenant previous;

    private final Tenant current;

    public TenantState(Tenant current) {
      this(getCurrentTenantOrNull(), current);
    }

    public TenantState(Tenant previous, Tenant current) {
      this.previous = previous;
      this.current = current;
    }

    private static Tenant getCurrentTenantOrNull() {
      if (tenantThreadLocal.get() == null
          || SINGLE_TENANT.equals(tenantThreadLocal.get().current)
          || GLOBAL_TENANT.equals(tenantThreadLocal.get().current))
      {
        return null;
      }

      return tenantThreadLocal.get().current;
    }
  }

  @VisibleForTesting
  static class InvalidTenantOperationException
      extends RuntimeException
  {
    private static final String ERROR_PREFIX = "Tenancy error detected";

    public InvalidTenantOperationException(String message) {
      // This prefix MUST be added to all tenancy error messages as it is this text that appears in the Datadog logs and
      // of which the "Tenancy Errors" Datadog monitor is based off of.
      super(ERROR_PREFIX + ": " + message);
    }
  }
}
