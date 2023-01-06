/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.function.Supplier;

import org.slf4j.MDC;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT;

/**
 * Java {@link ThreadLocal} to manage IQ tenants in our IQ multi-tenant saas offering. Application code should always
 * prefer the `TenantManager` class in the insight-brain-service module if possible.
 */
public class TenantThreadLocal
{
  private static final ThreadLocal<Tenant> tenantThreadLocal = new InheritableThreadLocal<>();

  private static final TenantUtil tenantUtil = new TenantUtil();

  static {
    tenantThreadLocal.set(SINGLE_TENANT);
  }

  /**
   * Returns the tenant from the ThreadLocal. TenantManager in the insight-brain-service module should ALWAYS be
   * preferred. The tenant returned is validated (see {@link TenantUtil#validateTenant(Tenant)}.
   */
  public static Tenant getTenant() {
    // Check the ThreadLocal if a tenant is set yet
    Tenant tenant = tenantUtil.validateTenant(getTenantWithoutValidation());

    checkPermission(tenantThreadLocal.get());

    return tenant;
  }

  /**
   * Returns the tenant from the ThreadLocal without doing any validation. This should only be called in circumstances
   * where we don't care about the tenant being set. Ultimately this can be removed when validation is no longer
   * needed.
   */
  static Tenant getTenantWithoutValidation() {
    return tenantThreadLocal.get();
  }

  /**
   * Verify that the passed tenant has permission to be used. The goal is to prevent an incorrect tenant being picked up
   * and data leaking between tenants. This is especially true in the case of threads that are shared within ThreadPools
   * Currently, this checks that the tenant has not been invalidates but further checks may well be introduced in the
   * future.
   */
  private static void checkPermission(Tenant tenant) {
    // This is initialization, once the tenant has been set to a value it can no longer be nullified.
    if (tenantThreadLocal.get() == null) {
      return;
    }

    // Everyone has access to the global tenant.
    if (tenant.equals(GLOBAL_TENANT)) {
      return;
    }

    if (tenant.isInvalid()) {
      throw new RuntimeException("Attempting to use a tenant from a previous request/process");
    }
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

    setTenantWithoutValidation(tenant);
  }

  private static void setTenantWithoutValidation(Tenant tenant) {
    MDC.put("tenant", tenant.tenantSlug);
    tenantThreadLocal.set(tenant);
  }

  /**
   * PACKAGE PRIVATE!!! This setter is to remain package private. Use the TenantUtil class to init the global tenant
   */
  static void setGlobalTenant() {
    setTenant(GLOBAL_TENANT);
  }

  public static <T> T runAsGlobal(Supplier<T> supplier) {
    return runAs(GLOBAL_TENANT, supplier);
  }

  /**
   * PACKAGE PRIVATE!!! Only trusted callers should be able to run code as a specific tenant. Note: Using this method
   * will invalidate the tenant when finished. Use cloneTenant before using this.
   */
  static <T> T runAs(Tenant tenant, Supplier<T> supplier) {
    if (!tenantUtil.isMultiTenant()) {
      return supplier.get();
    }

    // This is likely, called in a new thread, so it is likely the tenant is null. That is not guaranteed though
    // because this thread could be used in a thread pool
    Tenant previous = getTenantWithoutValidation();
    try {
      setTenant(tenant);

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

    getTenantWithoutValidation().invalidate();
  }
}
