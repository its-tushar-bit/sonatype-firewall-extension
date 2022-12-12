/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import org.junit.Test;

import static com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TenantUtilTest
    extends MultiTenantTest
{
  @Test
  public void shouldReturnMultiTenantTrue() {
    assertThat(TenantUtil.isMultiTenant()).isTrue();
  }

  @Test
  public void shouldReturnSingleTenantFalse() {
    assertThat(TenantUtil.isSingleTenant()).isFalse();
  }

  @Test
  public void shouldReturnSingleTenantTrue_whenMultiTenantFalse() {
    TenantThreadLocal.setTenant(SINGLE_TENANT);

    try {
      assertThat(TenantUtil.isMultiTenant()).isFalse();
      assertThat(TenantUtil.isSingleTenant()).isTrue();
    }
    finally {
      TenantThreadLocal.setGlobalTenant();
    }
  }

  @Test
  public void shouldReturnGlobalTrue() {
    assertThat(TenantUtil.isGlobalTenant()).isTrue();
  }

  @Test
  public void shouldReturnGlobalTrue_whenGlobalSlug() {
    assertThat(TenantUtil.isGlobalTenant("global")).isTrue();
    assertThat(TenantUtil.isGlobalTenant("notglobal")).isFalse();
  }

  @Test
  public void shouldExtractTenantNameFromUrl() {
    assertThat(TenantUtil.getTenantName("tenant1.mtiq.cloudy.sonatype.dev")).isEqualTo("tenant1");
    assertThat(TenantUtil.getTenantName("tenant2.staging.mtiq.cloudy.sonatype.dev")).isEqualTo("tenant2");
    assertThat(TenantUtil.getTenantName("tenant3.cloud-dev.sonatype.com")).isEqualTo("tenant3");
    assertThat(TenantUtil.getTenantName("tenant4.nexus.local")).isEqualTo("tenant4");
  }

  @Test
  public void shouldThrowAnExceptionOnLocalhost() {
    assertThatThrownBy(() -> TenantUtil.getTenantName("localhost")).hasMessage(
        "You should not be accessing multi-tenant IQ via localhost. Use a fake vanity URL");
  }

  @Test
  public void shouldThrowRuntimeExceptionForInvalidUrl() {
    assertThatThrownBy(() -> TenantUtil.getTenantName("invalid-url")).isInstanceOf(RuntimeException.class);
  }
}
