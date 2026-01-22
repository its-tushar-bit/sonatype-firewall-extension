/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.health;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.operational.check.AbstractOperationalCheck;
import com.sonatype.insight.brain.tenancy.TenantManager;

/**
 * A 'READY' health check to ensure all tenants are registered before the application is considered ready.
 */
@Named
@Singleton
public class TenantRegistrationHealthCheck
    extends AbstractOperationalCheck
{
  private final TenantManager tenantManager;

  @Inject
  public TenantRegistrationHealthCheck(final TenantManager tenantManager) {
    super("tenant-registration");
    this.tenantManager = tenantManager;
  }

  @Override
  protected Result check() throws Exception {
    ResultBuilder resultBuilder = Result.builder();
    if (!tenantManager.areTenantsPreRegistered()) {
      resultBuilder.unhealthy();
    }
    return resultBuilder.build();
  }
}
