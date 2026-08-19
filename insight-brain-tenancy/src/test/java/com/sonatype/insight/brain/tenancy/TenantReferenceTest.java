/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TenantReferenceTest
    extends MultiTenantTestSupport
{
  static final String VALUE_1 = "value1";

  static final String VALUE_2 = "value2";

  TenantReference<String> underTest;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();

    this.underTest = new TenantReference<>();
  }

  @Test
  public void shouldSetValueDependingOnTenant() {
    Tenant tenant1 = testAsNewTenant(t1 -> {
      assertThat(underTest.get()).isNull();

      underTest.set(VALUE_1);
      assertThat(underTest.get()).isEqualTo(VALUE_1);
    });

    // Set the value for a new tenant
    testAsNewTenant(t2 -> {
      assertThat(underTest.get()).isNull();

      underTest.set(VALUE_2);
      assertThat(underTest.get()).isEqualTo(VALUE_2);
    });

    // Value for original tenant (tenant1) should still be set
    TenantTestHelper.testAsTenant(tenant1, t1 -> assertThat(underTest.get()).isEqualTo(VALUE_1));
  }

  @Test
  public void shouldComputeValue_whenNull() {
    Tenant tenant1 = testAsNewTenant(t1 -> {
      assertThat(underTest.get()).isNull();

      assertThat(underTest.computeIfAbsent(t -> VALUE_1)).isEqualTo(VALUE_1);
    });

    // Set the value for a new tenant
    testAsNewTenant(t2 -> {
      assertThat(underTest.get()).isNull();

      assertThat(underTest.computeIfAbsent(t -> VALUE_2)).isEqualTo(VALUE_2);
      assertThat(underTest.get()).isEqualTo(VALUE_2);
    });

    // Value for original tenant (tenant1) should still be set
    TenantTestHelper.testAsTenant(tenant1, t1 -> assertThat(underTest.get()).isEqualTo(VALUE_1));
  }
}
