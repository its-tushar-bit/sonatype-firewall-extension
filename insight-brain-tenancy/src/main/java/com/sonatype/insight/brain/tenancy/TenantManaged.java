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
    Before the introduction of this class the dropwizard start() method was used for initialzation of jobs. The problem
    with that is it can't be used for tenant provisioning because it ties the initialization to the boot of the
    application (tenants are initialized at some unknown point in time after boot).

    Note: All future Quartz jobs should be TenantManaged.

    Register is still called on start through the use of
    {@link com.sonatype.insight.brain.service.DefaultQuartzJobInitiailizer}
   */
  default void register() {
    // noop
  }

  default void deregister() {
    // noop
  }
}
