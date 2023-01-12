/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

/**
 * {@link TenantManager#setTenant(String)} and {@link TenantManager#setTenant(Tenant)} are package-private by design to
 * protect the ability to set a tenant. In tests however we may need to register a tenant outside of this package. So
 * this class exists to expose that functionality.
 */
public class TenantManagerTestHelper
{
  public static void setTestTenant(final TenantManager tenantManager, final String tenant) {
    tenantManager.setTenant(tenant);
  }

  public static void setTestTenant(final TenantManager tenantManager, final Tenant tenant) {
    tenantManager.setTenant(tenant);
  }
}
