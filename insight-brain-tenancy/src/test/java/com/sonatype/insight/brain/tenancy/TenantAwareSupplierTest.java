/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TenantAwareSupplierTest
    extends MultiTenantTestSupport
{
  @Test
  public void shouldCallWrappedSupplier_usingTenantSetAtCreationTime() {
    String resultString = "complete";

    Tenant expectedTenant = new Tenant("correcttenant");
    TenantTestHelper.setTenantWithoutValidation(expectedTenant);
    TenantAwareSupplier<String> supplier = new TenantAwareSupplier<>(() -> {
      assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(expectedTenant);
      return resultString;
    });

    // Swap the tenant before running the supplier
    TenantTestHelper.setTenantWithoutValidation(new Tenant("wrongtenant"));

    String result = supplier.get();

    // Verify that the supplier did actually run and therefore the assertion also was called
    assertThat(result).isEqualTo(resultString);
  }
}
