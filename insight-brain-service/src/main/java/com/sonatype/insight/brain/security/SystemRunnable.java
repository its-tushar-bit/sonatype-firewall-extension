/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.tenancy.TenantAwareRunnable;

public class SystemRunnable
    implements Runnable
{
  private final TenantAwareRunnable tenantAwareRunnable;

  public SystemRunnable(Runnable wrapped) {
    this.tenantAwareRunnable = new TenantAwareRunnable(() -> {
      try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
        wrapped.run();
      }
    });
  }

  @Override
  public void run() {
    tenantAwareRunnable.run();
  }
}
