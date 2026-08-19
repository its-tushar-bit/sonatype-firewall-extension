/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class TenantReference<T>
{
  private final Supplier<T> initializer;

  private final ConcurrentMap<Tenant, T> tenantMap = new ConcurrentHashMap<>();

  public TenantReference() {
    this.initializer = null;
  }

  public TenantReference(Supplier<T> initializer) {
    this.initializer = initializer;
  }

  public T get() {
    Tenant tenant = TenantThreadLocal.getTenant();

    if (initializer != null) {
      return tenantMap.computeIfAbsent(tenant, t -> initializer.get());
    }
    else {
      return tenantMap.get(tenant);
    }
  }

  public T remove() {
    Tenant tenant = TenantThreadLocal.getTenant();
    return tenantMap.remove(tenant);
  }

  public void set(T t) {
    Tenant tenant = TenantThreadLocal.getTenant();

    tenantMap.put(tenant, t);
  }

  public T computeIfAbsent(Function<Tenant, T> computation) {
    return tenantMap.computeIfAbsent(TenantThreadLocal.getTenant(), computation);
  }

  /**
   * Returns the value for the current tenant without invoking the initializer.
   */
  public T getIfPresent() {
    return tenantMap.get(TenantThreadLocal.getTenant());
  }
}
