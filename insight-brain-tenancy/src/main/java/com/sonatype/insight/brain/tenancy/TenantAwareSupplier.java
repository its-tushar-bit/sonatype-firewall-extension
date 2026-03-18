/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.function.Supplier;

public class TenantAwareSupplier<T>
    implements Supplier<T>
{
  private final Supplier<T> wrapped;

  private final Tenant tenant;

  public TenantAwareSupplier(Supplier<T> wrapped) {
    this(wrapped, TenantThreadLocal.getTenant());
  }

  TenantAwareSupplier(Supplier<T> wrapped, Tenant tenant) {
    this.wrapped = wrapped;
    this.tenant = tenant;
  }

  @Override
  public T get() {
    return TenantThreadLocal.runAsWithoutValidation(tenant, wrapped);
  }
}
