/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.function.Function;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runAsGlobal;

/**
 * Base class for DAOs that support multi-tenant fallback behavior.
 * <p>
 * In MTIQ (multi-tenant) deployments, this class first attempts to retrieve an entity from the current tenant's schema.
 * If the entity does not exist in the per-tenant schema, it falls back to the global tenant to provide default values.
 * </p>
 * <p>
 * In single-tenant (IQ) deployments, this class behaves the same as {@link AbstractOperationalSqlDAO}.
 * </p>
 *
 * @param <T> the entity type
 */
public abstract class AbstractOperationalSqlWithFallbackDAO<T extends HasStringId>
    extends AbstractOperationalSqlDAO<T>
{
  private final TenantUtil tenantUtil = new TenantUtil();

  protected AbstractOperationalSqlWithFallbackDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * MTIQ: First attempt to get the entity from the current tenant. If the entity does not exist in the
   * per-tenant schema then fall back and get the entity from the global tenant. This allows us to provide
   * defaults for all tenants.
   * <p>
   * IQ: Get the entity as normal.
   *
   * @param tx the transaction context
   * @param fetcher function that fetches the entity from the database using jOOQ
   * @return the entity from the current tenant, or from the global tenant if not found
   */
  protected T getWithGlobalFallback(TransactionContext tx, Function<TransactionContext, T> fetcher) {
    return getWithGlobalFallback(tx, fetcher, false);
  }

  /**
   * MTIQ: First attempt to get the entity from the current tenant. If the entity does not exist in the
   * per-tenant schema then fall back and get the entity from the global tenant. This allows us to provide
   * defaults for all tenants.
   * <p>
   * IQ: Get the entity as normal.
   *
   * @param tx the transaction context
   * @param fetcher function that fetches the entity from the database using jOOQ
   * @param fetchForUpdate if true, skip fallback to global (use when fetching to update)
   * @return the entity from the current tenant, or from the global tenant if not found
   */
  protected T getWithGlobalFallback(
      TransactionContext tx,
      Function<TransactionContext, T> fetcher,
      boolean fetchForUpdate)
  {
    T result = fetcher.apply(tx);

    if (fetchForUpdate || result != null || tenantUtil.isSingleTenant() || tenantUtil.isGlobalTenant()) {
      return result;
    }
    else {
      return runAsGlobal(() -> {
        try (TransactionContext globalTx = createTransactionContext()) {
          return fetcher.apply(globalTx);
        }
      });
    }
  }
}
