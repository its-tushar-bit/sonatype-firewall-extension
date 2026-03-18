/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

/**
 * For beans that should run as an individual tenant
 */
public interface TenantManaged
{
  /**
   * Before the introduction of this class, the dropwizard start() method was used for initialization of jobs. The
   * problem with that is it can't be used for tenant provisioning because it ties the initialization to the boot of the
   * application (tenants are initialized at some unknown point in time after boot).
   *
   * Note: All future Quartz jobs should be TenantManaged.
   *
   * Register is still called on start through the use of
   * {@link com.sonatype.insight.brain.service.DefaultTenantManagedInitializer}
   */
  default void register() {
    // noop
  }

  default void deregister() {
    // noop
  }

  /**
   * Start() and stop() are here to clash with the same methods in io.dropwizard.lifecycle.Managed. This prevents
   * a bean implementing both Managed (boot with application) and TenantManaged (boot with tenant) as these are mutually
   * exclusive.
   */
  default String start() throws Exception {
    return "Method not allowed";
  }

  default String stop() throws Exception {
    return "Method not allowed";
  }

  /**
   * Determines the order that TenantManaged beans are called in. The lower the integer the higher the priority (e.g.
   * priority 1 will be registered before priority 20)
   */
  default int registrationPriority() {
    return 99;
  }

  /**
   * In certain circumstances we need the ability to also "register" the global tenant. An example is setting up
   * default configuration, we want that to run for Global and every other tenant.
   */
  default boolean includeGlobalTenantDuringRegistration() {
    return false;
  }
}
