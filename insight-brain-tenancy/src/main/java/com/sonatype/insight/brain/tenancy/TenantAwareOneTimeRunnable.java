/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.function.Supplier;

import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.cloneTenant;

public class TenantAwareOneTimeRunnable
    implements Runnable
{
  private final Runnable wrapped;

  private final Tenant tenant;

  private boolean previouslyRun = false;

  public TenantAwareOneTimeRunnable(Runnable wrapped) {
    this(wrapped, TenantThreadLocal.getTenant());
  }

  TenantAwareOneTimeRunnable(Runnable wrapped, Tenant tenant) {
    this.wrapped = wrapped;
    this.tenant = cloneTenant(tenant);
  }

  @Override
  public void run() {
    if (previouslyRun) {
      /*
        This is to fail fast. The request will fail when the wrapped runnable is called and gets the tenant anyway but
        by failing fast we get a better stack trace, making it easier to find and resolve the problem.
       */
      throw new RuntimeException("TenantAwareOneTimeRunnable cannot be reused");
    }

    previouslyRun = true;

    TenantThreadLocal.runAs(tenant, (Supplier<Void>) () -> {
      try {
        wrapped.run();

        return null;
      }
      finally {
        TenantThreadLocal.invalidateTenant();
      }
    });
  }
}
