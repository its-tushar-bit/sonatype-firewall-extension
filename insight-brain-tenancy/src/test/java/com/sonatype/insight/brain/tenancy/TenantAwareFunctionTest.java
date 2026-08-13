/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TenantAwareFunctionTest
    extends MultiTenantTestSupport
{
  @Test
  public void shouldCallWrappedFunction_usingTenantSetAtCreationTime() {
    String resultString = "complete";

    Tenant expectedTenant = new Tenant("correcttenant");
    TenantTestHelper.setTenantWithoutValidation(expectedTenant);
    TenantAwareFunction<String, String> function = new TenantAwareFunction<>(input -> {
      assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(expectedTenant);
      return input;
    });

    // Swap the tenant before running the function
    TenantTestHelper.setTenantWithoutValidation(new Tenant("wrongtenant"));

    String result = function.apply(resultString);

    // Verify that the function did actually run and therefore the assertion also was called
    assertThat(result).isEqualTo(resultString);
  }
}
