/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.HashMap;
import java.util.Map;

import java.util.function.Supplier;

public class TenantReference<T>
{
  private final Supplier<T> initializer;

  private Map<Tenant, T> tenantMap = new HashMap<>();

  public TenantReference() {
    this.initializer = null;
  }

  public TenantReference(Supplier<T> initializer) {
    this.initializer = initializer;
  }

  public T get() {
    Tenant tenant = TenantThreadLocal.getTenant();

    T result = tenantMap.get(tenant);
    if (initializer != null && result == null) {
      T initialValue = initializer.get();
      set(initialValue);
      return initialValue;
    }
    else {
      return result;
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
}
