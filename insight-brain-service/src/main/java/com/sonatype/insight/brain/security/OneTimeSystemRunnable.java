/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.tenancy.TenantAwareOneTimeRunnable;

public class OneTimeSystemRunnable
    implements Runnable
{
  private final TenantAwareOneTimeRunnable tenantAwareRunnable;

  public OneTimeSystemRunnable(final Runnable wrapped) {
    this.tenantAwareRunnable = new TenantAwareOneTimeRunnable(() -> {
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
