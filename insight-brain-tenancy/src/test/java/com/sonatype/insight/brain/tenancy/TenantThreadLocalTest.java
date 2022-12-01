/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.junit.Test;
import org.mockito.Mockito;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

public class TenantThreadLocalTest
    extends MultiTenantTest
{
  @Test
  public void shouldPreventWrongTenantUsed_whenThreadReused() throws Exception {
    Tenant tenant1 = new Tenant("tenant1");
    TenantThreadLocal.setTenant(tenant1);

    // Create a thread pool with only one thread so that it gets reused on subsequent calls
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<Tenant> future = executorService.submit(TenantThreadLocal::getTenant);

    // This is a new thread but the tenant should be inherited
    assertThat(future.get()).isEqualTo(tenant1);

    future = executorService.submit(() -> {
      TenantThreadLocal.setTenant(tenant1);

      return TenantThreadLocal.getTenantWithoutValidation();
    });
    assertThat(future.get()).isEqualTo(tenant1);

    // Now simulate the request filter / entry points and invalidate the tenant
    TenantThreadLocal.invalidateTenant();

    // Attempting to get the tenant should now fail
    future = executorService.submit(TenantThreadLocal::getTenant);
    assertThatThrownBy(future::get).hasMessage(
        "java.lang.RuntimeException: Attempting to use a tenant from a previous request/process");
  }

  @Test
  public void shouldAlwaysAllowRunAsGlobalTenant() throws Exception {
    Tenant tenant1 = new Tenant("tenant1");
    TenantThreadLocal.setTenant(tenant1);

    Tenant tenant = TenantThreadLocal.runAs(GLOBAL_TENANT, TenantThreadLocal::getTenantWithoutValidation);
    assertThat(tenant).isEqualTo(GLOBAL_TENANT);
    assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(tenant1);
  }

  @Test
  public void shouldNeverInvalidateGlobalTenant() {
    GLOBAL_TENANT.invalidate();

    assertThat(GLOBAL_TENANT.isInvalid()).isFalse();
  }

  @Test
  public void cloneTenantShouldReturnNewInstance() {
    Tenant tenant = new Tenant("testtenant");

    Tenant clone = TenantThreadLocal.cloneTenant(tenant);

    assertThat(tenant == clone).isFalse();
    assertThat(clone.tenantSlug).isEqualTo(tenant.tenantSlug);
    assertThat(clone.databaseSchema).isEqualTo(tenant.databaseSchema);

    tenant.invalidate();
    assertThat(tenant.isInvalid()).isTrue();
    assertThat(clone.isInvalid()).isFalse();
  }

  @Test
  public void shouldRunAsGlobal() {
    Tenant tenant = new Tenant("testtenant");
    TenantThreadLocal.setTenant(tenant);

    Supplier mockSupplier = Mockito.mock(Supplier.class);
    Mockito.when(mockSupplier.get()).thenAnswer(invocationOnMock -> {
      assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(GLOBAL_TENANT);
      return null;
    });

    TenantThreadLocal.runAsGlobal(mockSupplier);
    assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(tenant);

    // Verify that the runnable did actually run and therefore the assertion also was called
    verify(mockSupplier).get();
  }

  @Test
  public void shouldSetGlobal() {
    TenantThreadLocal.setTenant(new Tenant("testtenant"));
    assertThat(TenantThreadLocal.getTenantWithoutValidation()).isNotEqualTo(GLOBAL_TENANT);

    TenantThreadLocal.setGlobalTenant();
    assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(GLOBAL_TENANT);
  }
}
