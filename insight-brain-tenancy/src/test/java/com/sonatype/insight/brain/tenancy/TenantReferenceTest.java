/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TenantReferenceTest
{
  @Test
  public void shouldSetValueDependingOnTenant() {
    TenantTestHelper.setMultiTenantModeForTest(true);

    Tenant tenant1 = new Tenant("tenant1");
    Tenant tenant2 = new Tenant("tenant2");
    String value1 = "value1";
    String value2 = "value2";

    TenantReference<String> underTest = new TenantReference<>();

    TenantTestHelper.setTenant(tenant1);
    assertThat(underTest.get()).isNull();
    underTest.set(value1);
    assertThat(underTest.get()).isEqualTo(value1);

    // Set the value for a new tenant
    TenantTestHelper.setTenant(tenant2);
    assertThat(underTest.get()).isNull();
    underTest.set(value2);
    assertThat(underTest.get()).isEqualTo(value2);

    // Value for original tenant (tenant1) should still be set
    TenantTestHelper.setTenant(tenant1);
    assertThat(underTest.get()).isEqualTo(value1);
  }
}
