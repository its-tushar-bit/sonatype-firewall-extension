/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.concurrent.Callable;

import io.micrometer.core.instrument.Tags;

import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.cloneTenant;
import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.invalidateTenant;

public class TenantAwareOneTimeCallable<T>
    implements Callable<T>, HasTags
{
  private final Callable<T> wrapped;

  private final Tenant tenant;

  private boolean previouslyRun = false;

  public TenantAwareOneTimeCallable(Callable<T> wrapped) {
    this(wrapped, TenantThreadLocal.getTenant());
  }

  TenantAwareOneTimeCallable(Callable<T> wrapped, Tenant tenant) {
    this.wrapped = wrapped;
    this.tenant = cloneTenant(tenant);
  }

  @Override
  public T call() throws Exception {
    if (previouslyRun) {
      /*
        This is to fail fast. The request will fail when the wrapped runnable is called and gets the tenant anyway but
        by failing fast we get a better stack trace, making it easier to find and resolve the problem.
       */
      throw new RuntimeException("TenantAwareOneTimeCallable cannot be reused");
    }

    previouslyRun = true;

    T result = TenantThreadLocal.runAsWithoutValidation(tenant, () -> {
      try {
        return wrapped.call();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
      finally {
        invalidateTenant();
      }
    });

    return result;
  }

  @Override
  public Tags getTags() {
    if (wrapped instanceof HasTags hasTags) {
      return hasTags.getTags();
    }
    return Tags.empty();
  }
}
