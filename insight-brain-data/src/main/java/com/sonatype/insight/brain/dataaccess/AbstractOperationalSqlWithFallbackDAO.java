/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runAsGlobal;

public abstract class AbstractOperationalSqlWithFallbackDAO<T extends HasStringId>
    extends AbstractOperationalSqlDAO<T>
{
  private final TenantUtil tenantUtil = new TenantUtil();

  @Override
  protected T get(
      TransactionContext tx,
      String sQuery,
      Object... parameters)
  {
    return getWithGlobalFallback(tx, sQuery, false, parameters);
  }

  /**
   * MTIQ: First attempt to get the configuration from the current tenant. If the configuration does not exist in the
   * per-tenant schema then fall back and get the config from the global tenant. This allows us to provide configuration
   * defaults for all tenants.
   * <p>
   * IQ: Get the configuration as normal.
   *
   * @param tx
   * @param sQuery
   * @param parameters
   * @param fetchForUpdate - If fetching config to then update the config, should NOT fall back and use global
   * @return
   */
  protected T getWithGlobalFallback(
      TransactionContext tx,
      String sQuery,
      boolean fetchForUpdate,
      Object... parameters)
  {
    T result = super.get(tx, sQuery, null, parameters);

    if (fetchForUpdate || result != null || tenantUtil.isSingleTenant() || tenantUtil.isGlobalTenant()) {
      return result;
    }
    else {
      return runAsGlobal(() -> {
        try (TransactionContext globalTx = createTransactionContext()) {
          return super.get(globalTx, sQuery, null, parameters);
        }
      });
    }
  }
}
