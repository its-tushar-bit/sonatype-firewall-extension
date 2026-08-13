/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantUtil.IS_MTIQ_BATCH;
import static com.sonatype.insight.brain.tenancy.TenantUtil.TENANT_DOES_NOT_EXIST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(MockitoExtension.class)
public class TenantUtilTest
    extends MultiTenantTestSupport
{
  private final TestEnvironmentVariables environmentVariables = new TestEnvironmentVariables();

  @AfterEach
  public void restoreEnvironmentVariables() {
    environmentVariables.restore();
  }

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
  public void shouldThrowAnExceptionIfGlobalTenantNameUsed() {
    assertThatThrownBy(() -> new TenantUtil().getTenantName(GLOBAL_TENANT.tenantSlug + ".")).hasMessage(
        TENANT_DOES_NOT_EXIST);
  }

  @Test
  public void shouldThrowAnExceptionIfSingleTenantNameUsed() {
    assertThatThrownBy(() -> new TenantUtil().getTenantName(SINGLE_TENANT.tenantSlug + ".")).hasMessage(
        TENANT_DOES_NOT_EXIST);
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

  @Test
  public void shouldReturnTrueWhenAllTenantsJob() {
    assertThat(new TenantUtil().isAllTenantsJob(AllTenantsJob.class)).isTrue();
    assertThat(new TenantUtil().isAllTenantsJob(String.class)).isFalse();
  }

  @Test
  public void shouldReturnTrueWhenMtiqBatchJob() {
    assertThat(new TenantUtil().isMtiqBatchJob(MtiqBatchJob.class)).isTrue();
    assertThat(new TenantUtil().isMtiqBatchJob(String.class)).isFalse();
  }

  @Test
  public void shouldReturnBackgroundModeWhenSet() {
    environmentVariables.set(IS_MTIQ_BATCH, "true");

    assertThat(new TenantUtil().isMtiqBatchMode()).isTrue();

    environmentVariables.set(IS_MTIQ_BATCH, "false");

    assertThat(new TenantUtil().isMtiqBatchMode()).isFalse();
  }

  @Test
  public void testValidateThrowsException_whenAllTenantJob_andNotGlobal() {
    Tenant tenant = new Tenant("not-global");

    assertThatThrownBy(() -> new TenantUtil().validateTenantForType(TestAllTenantsJob.class, tenant))
        .isInstanceOf(TenantUtil.InvalidTenantForJobTypeException.class)
        .hasMessage("AllTenantJob(s) cannot be created against a non-global tenant. " +
            "Type=" + TestAllTenantsJob.class.getSimpleName() + ", Tenant=" + tenant);
  }

  @Test
  public void testValidateNoCustomerTenantSet_SingleTenant() {
    TenantThreadLocal.setTenant(SINGLE_TENANT);
    // verifies no exception thrown
    new TenantUtil().validateNoCustomerTenantSet();
  }

  @Test
  public void testValidateNoCustomerTenantSet_GlobalTenant() {
    TenantThreadLocal.setTenant(GLOBAL_TENANT);
    // verifies no exception thrown
    new TenantUtil().validateNoCustomerTenantSet();
  }

  @Test
  public void testValidateNoCustomerTenantSet_RealTenant() {
    TenantThreadLocal.setTenant(new Tenant("foo"));

    assertThatThrownBy(() -> new TenantUtil().validateNoCustomerTenantSet())
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Found tenant 'foo' but was expecting no tenant to be set");
  }

  private interface TestAllTenantsJob
      extends AllTenantsJob
  {
  }
}
