/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

/** Shared tenancy accessor for the Global Search cursor-pinning path. */
public final class GlobalSearchTenancy
{
  private GlobalSearchTenancy() {
  }

  /**
   * Current tenant slug, or the empty string when no tenant is wired (non-MT / test environments). The
   * empty slug still yields a stable, self-consistent cursor token for the single-tenant case.
   */
  public static String currentTenantId() {
    try {
      Tenant t = TenantThreadLocal.getTenant();
      return t == null ? "" : t.tenantSlug;
    }
    catch (RuntimeException e) {
      return "";
    }
  }
}
