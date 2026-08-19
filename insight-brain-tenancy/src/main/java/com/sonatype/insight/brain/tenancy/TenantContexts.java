/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

/**
 * Trusted entry points for running work under a specific tenant context.
 */
public final class TenantContexts
{
  private TenantContexts() {
  }

  /**
   * Runs {@code action} with {@code tenantSlug} bound on the current thread, then restores the
   * previous tenant context.
   */
  public static void runAs(final String tenantSlug, final Runnable action) {
    TenantThreadLocal.runAsWithoutValidation(new Tenant(tenantSlug), () -> {
      action.run();
      return null;
    });
  }
}
