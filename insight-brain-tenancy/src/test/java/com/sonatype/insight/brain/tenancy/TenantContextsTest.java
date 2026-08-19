/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TenantContextsTest
    extends MultiTenantTestSupport
{
  @Test
  public void runAs_executesActionUnderProvidedTenant() {
    AtomicReference<String> observedSlug = new AtomicReference<>();
    TenantContexts.runAs("tenant-a", () -> observedSlug.set(TenantThreadLocal.getTenant().tenantSlug));
    assertThat(observedSlug.get()).isEqualTo("tenant-a");
  }
}
