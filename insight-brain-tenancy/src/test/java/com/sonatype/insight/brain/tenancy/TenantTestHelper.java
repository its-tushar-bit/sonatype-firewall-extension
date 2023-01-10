/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class TenantTestHelper
{
  public static void initMultiTenantMode() {
    TenantThreadLocal.setGlobalTenant();
  }

  public static void setTenant(final Tenant tenant) {
    TenantThreadLocal.setTenantWithoutValidation(tenant);
  }

  public static void assertTenantSet(Tenant tenant) {
    assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(tenant);
  }

  public static void testAs(Tenant tenant, Consumer<Tenant> test) {
    Tenant currentTenant = TenantThreadLocal.getTenantWithoutValidation();
    try {
      setTenant(tenant);

      test.accept(tenant);
    }
    finally {
      setTenant(currentTenant);
    }
  }
}
