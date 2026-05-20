/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import jakarta.annotation.Nullable;

/**
 * Resolves the consumption {@code org_id} from the current thread's {@link Tenant}.
 * Returns {@code null} when the request is not attributable to a specific org
 * (GLOBAL_TENANT cross-tenant admin ops; optionally null tenant) so callers can
 * skip consumption recording for that request.
 *
 * @since 1.204
 */
public final class ConsumptionOrgIdResolver
{
  private ConsumptionOrgIdResolver() {
    // utility
  }

  /**
   * Resolves {@code org_id} for an HTTP request path. Null tenant skips recording
   * (indicates a setup error in MTIQ).
   */
  @Nullable
  public static String resolveForRequest() {
    Tenant tenant = TenantThreadLocal.getTenant();
    if (tenant == null || Tenant.GLOBAL_TENANT.equals(tenant)) {
      return null;
    }
    if (Tenant.SINGLE_TENANT.equals(tenant)) {
      return Organization.ROOT_ORGANIZATION_ID;
    }
    return tenant.tenantSlug;
  }

  /**
   * Resolves {@code org_id} for a background-job context. Null/SINGLE_TENANT map to
   * ROOT_ORGANIZATION_ID (on-prem / boot-time default); GLOBAL_TENANT skips recording.
   */
  @Nullable
  public static String resolveForBackgroundJob() {
    Tenant tenant = TenantThreadLocal.getTenant();
    if (Tenant.GLOBAL_TENANT.equals(tenant)) {
      return null;
    }
    if (tenant == null || Tenant.SINGLE_TENANT.equals(tenant)) {
      return Organization.ROOT_ORGANIZATION_ID;
    }
    return tenant.tenantSlug;
  }
}
