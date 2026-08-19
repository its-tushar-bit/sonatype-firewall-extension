/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.function.Function;

public class TenantAwareFunction<T, R>
    implements Function<T, R>

{
  private final Function<T, R> wrapped;

  private final Tenant tenant;

  public TenantAwareFunction(Function<T, R> wrapped) {
    this(wrapped, TenantThreadLocal.getTenant());
  }

  TenantAwareFunction(Function<T, R> wrapped, Tenant tenant) {
    this.wrapped = wrapped;
    this.tenant = tenant;
  }

  @Override
  public R apply(T t) {
    return TenantThreadLocal.runAsWithoutValidation(tenant, () -> wrapped.apply(t));
  }
}
