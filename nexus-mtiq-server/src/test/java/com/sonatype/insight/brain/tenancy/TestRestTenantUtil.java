/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import jakarta.servlet.ServletRequest;

/**
 * This class is bound instead of the default <code>TenantUtil</code> to allow overriding of the TenantUrlFilter
 * behaviour. On receipt of any rest request the <code>TenantUrlFilter</code> will now always return the currently set
 * Tenant as the tenantSlug. Without this override tests run on localhost only and therefore the default TenantUrlFilter
 * is unable to determine a valid tenantSlug typically derived from the address.
 */
public class TestRestTenantUtil
    extends TenantUtil
{
  private String tenantSlug;

  @Override
  public String getTenantName(final String serverName) {
    if (tenantSlug == null) {
      return super.getTenantName(serverName);
    }
    // Whatever the current tenant has been set to will be the assumed tenant
    return tenantSlug;
  }

  public void setTenantSlug(String tenantSlug) {
    this.tenantSlug = tenantSlug;
  }

  public String getTenantSlug() {
    return tenantSlug;
  }

  public void clearTenantSlug() {
    tenantSlug = null;
  }

  /**
   * The real impl assumes IP-address hostnames are for the admin API. Tests cannot assume that as the dockerized
   * Chrome instance in the functional tests uses IP-address hostnames to contact the IQ server
   */
  @Override
  public boolean requestShouldUseGlobalTenant(ServletRequest request) {
    return isAdminApiRequest(request) || tenantSlug == null;
  }
}
