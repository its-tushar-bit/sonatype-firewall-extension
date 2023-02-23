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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class TenantUtilTest
    extends MultiTenantTestSupport
{
  @Test
  public void shouldReturnMultiTenantTrue() {
    assertThat(new TenantUtil().isMultiTenant()).isTrue();
  }

  @Test
  public void shouldReturnSingleTenantFalse() {
    assertThat(new TenantUtil().isSingleTenant()).isFalse();
  }

  @Test
  public void shouldReturnSingleTenantTrue_whenMultiTenantFalse() {
    TenantThreadLocal.setTenant(SINGLE_TENANT);

    try {
      assertThat(new TenantUtil().isMultiTenant()).isFalse();
      assertThat(new TenantUtil().isSingleTenant()).isTrue();
    }
    finally {
      TenantThreadLocal.setGlobalTenant();
    }
  }

  @Test
  public void shouldReturnGlobalTrue() {
    assertThat(new TenantUtil().isGlobalTenant()).isTrue();
  }

  @Test
  public void shouldReturnGlobalTrue_whenGlobalSlug() {
    assertThat(new TenantUtil().isGlobalTenant("global")).isTrue();
    assertThat(new TenantUtil().isGlobalTenant("notglobal")).isFalse();
  }

  @Test
  public void shouldExtractTenantNameFromUrl() {
    assertThat(new TenantUtil().getTenantName("tenant1.mtiq.cloudy.sonatype.dev")).isEqualTo("tenant1");
    assertThat(new TenantUtil().getTenantName("tenant2.staging.mtiq.cloudy.sonatype.dev")).isEqualTo("tenant2");
    assertThat(new TenantUtil().getTenantName("tenant3.cloud-dev.sonatype.com")).isEqualTo("tenant3");
    assertThat(new TenantUtil().getTenantName("tenant4.nexus.local")).isEqualTo("tenant4");
  }

  @Test
  public void shouldThrowAnExceptionOnLocalhost() {
    assertThatThrownBy(() -> new TenantUtil().getTenantName("localhost")).hasMessage(
        "You should not be accessing multi-tenant IQ via localhost. Use a fake vanity URL");
  }

  @Test
  public void shouldThrowRuntimeExceptionForInvalidUrl() {
    assertThatThrownBy(() -> new TenantUtil().getTenantName("invalid-url")).isInstanceOf(RuntimeException.class);
  }

  @Test
  public void shouldUseInternedTenantString_forSynchronization() {
    char[] tenantNameCharArray = {'t', '-', '1'};

    // Strings initialized from char arrays do not get interned
    TenantThreadLocal.setTenant(new Tenant(new String(tenantNameCharArray)));

    assertNotSame(new String(tenantNameCharArray), new TenantUtil().getTenantSlugForSynchronization());
    assertSame(new String(tenantNameCharArray).intern(), new TenantUtil().getTenantSlugForSynchronization());
  }
}
