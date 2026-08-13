/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.createTenantNameFromTest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TenantThreadLocalTest
    extends MultiTenantTestSupport
{
  @BeforeEach
  public void before() {
    TenantThreadLocal.tenantUtil = new TenantUtil();
  }

  @AfterEach
  public void after() {
    TenantThreadLocal.invalidateTenant();
  }

  @Test
  public void shouldPreventWrongTenantUsed_whenThreadReused() throws Exception {
    Tenant tenant1 = new Tenant("tenant1");
    TenantTestHelper.setTenantWithoutValidation(tenant1);

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
    assertThatThrownBy(future::get).hasMessageContaining("Attempting to use a tenant from a previous request/process");
  }

  @Test
  public void shouldAlwaysAllowRunAsGlobalTenant() {
    Tenant tenant1 = new Tenant("tenant1");
    TenantTestHelper.setTenantWithoutValidation(tenant1);

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
    TenantTestHelper.setTenantWithoutValidation(tenant);

    Supplier mockSupplier = mock(Supplier.class);
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
    TenantTestHelper.setTenantWithoutValidation(new Tenant("testtenant"));
    assertThat(TenantThreadLocal.getTenantWithoutValidation()).isNotEqualTo(GLOBAL_TENANT);

    TenantThreadLocal.setGlobalTenant();
    assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(GLOBAL_TENANT);
  }

  @Test
  public void shouldAllow_whenTransitioningFromInvalidTenantToValidTenant() {
    Tenant tenant1 = new Tenant("tenant1");
    TenantTestHelper.setTenantWithoutValidation(tenant1);
    tenant1.invalidate();

    Tenant tenant2 = new Tenant("tenant2");
    TenantThreadLocal.setTenant(tenant2);
    assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(tenant2);
  }

  @Test
  public void shouldAllow_whenTransitioningFromValidTenantToGlobalAndBack() {
    Tenant tenant1 = new Tenant("tenant1");
    TenantTestHelper.setTenantWithoutValidation(tenant1);

    TenantThreadLocal.setGlobalTenant();

    TenantThreadLocal.setTenant(tenant1);

    assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(tenant1);
  }

  @Test
  public void shouldThrowException_whenTransitioningFromOneValidTenantToAnother() {
    testAsNewTenant(t -> {

      assertThatThrownBy(() -> TenantThreadLocal.setTenant(new Tenant("tenant2")))
          .hasMessageContaining("Tenancy error detected: Cannot transition from one valid tenant to another. " +
              "This is to prevent data leakage");
    });
  }

  @Test
  public void shouldThrowException_whenTransitioningFromOneValidTenantToAnInvalidTenant() {
    testAsNewTenant(t -> {

      Tenant tenant2 = new Tenant("tenant2");
      tenant2.invalidate();

      assertThatThrownBy(() -> TenantThreadLocal.setTenant(tenant2)).hasMessageContaining(
          "Tenancy error detected: Attempting to use a tenant from a previous request/process");
    });
  }

  @Test
  public void shouldThrowException_whenTransitioningFromOneValidTenantToAnother_viaGlobal() {
    testAsNewTenant(t -> {
      TenantThreadLocal.setGlobalTenant();

      assertThatThrownBy(() -> TenantThreadLocal.setTenant(new Tenant("tenant2"))).hasMessageContaining(
          "Tenancy error detected: Cannot transition from one valid tenant to another via Global. " +
              "This is to prevent data leakage");
    });
  }

  @Test
  public void shouldNotCircumventViaGlobalCheck_whenSettingGlobalTenantTwice() {
    testAsNewTenant(t -> {

      TenantThreadLocal.setGlobalTenant();
      TenantThreadLocal.setGlobalTenant();

      assertThatThrownBy(() -> TenantThreadLocal.setTenant(new Tenant("tenant2"))).hasMessageContaining(
          "Tenancy error detected: Cannot transition from one valid tenant to another via Global. " +
              "This is to prevent data leakage");
    });
  }

  @Test
  public void shouldInheritTenant_whenNewThread() throws Exception {
    CountDownLatch lock = new CountDownLatch(1);

    Tenant tenant1 = new Tenant("tenant1");
    TenantTestHelper.setTenantWithoutValidation(tenant1);

    new Thread(() -> {
      assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(tenant1);

      // Invalidating here to guarantee the thread ran successfully
      tenant1.invalidate();

      lock.countDown();
    }).start();

    lock.await(200, TimeUnit.MILLISECONDS);

    assertThat(tenant1.isInvalid()).isTrue();
  }

  @Test
  public void shouldSetLoggingContext_set() {
    TenantThreadLocal.setTenant(new Tenant("testtenant"));
    assertThat(MDC.get("tenant")).isEqualTo("testtenant");
  }

  @Test
  public void shouldSetLoggingContext_remove() {
    TenantThreadLocal.setTenant(SINGLE_TENANT);
    assertThat(MDC.get("tenant")).isNull();
  }

  @Test
  public void shouldSetLoggingContext_cleanup() {
    TenantTestHelper.setTenantWithoutValidation(new Tenant("testtenant"));
    assertThat(MDC.get("tenant")).isEqualTo("testtenant");
    TenantThreadLocal.invalidateTenant();
    assertThat(MDC.get("tenant")).isNull();
  }

  @Test
  public void shouldRunAsAllTenants_whenBatchMode() {
    TenantUtil tenantUtilMock = mock(TenantUtil.class);
    TenantThreadLocal.tenantUtil = tenantUtilMock;
    when(tenantUtilMock.isMtiqBatchMode()).thenReturn(true);
    when(tenantUtilMock.isMultiTenant()).thenReturn(true);

    List<String> tenants =
        ImmutableList.of(createTenantNameFromTest(currentMethodName()), createTenantNameFromTest(currentMethodName()));

    List<Tenant> runAs = new ArrayList<>();
    TenantThreadLocal.runForAllTenantsOnBatch(tenants, currentMethodName(), runAs::add);

    assertThat(runAs).hasSize(2);
    for (int i = 0; i < tenants.size(); i++) {
      assertThat(runAs.get(i).tenantSlug).isEqualTo(tenants.get(i));
    }
  }

  @Test
  public void shouldRunAsSingleTenant_whenSingleTenant_andRunForAllTenantsCalled() {
    TenantUtil tenantUtilMock = mock(TenantUtil.class);
    TenantThreadLocal.tenantUtil = tenantUtilMock;
    when(tenantUtilMock.isMtiqBatchMode()).thenReturn(true);
    when(tenantUtilMock.isMultiTenant()).thenReturn(false);

    List<String> tenants =
        ImmutableList.of(createTenantNameFromTest(currentMethodName()), createTenantNameFromTest(currentMethodName()));

    List<Tenant> runAs = new ArrayList<>();
    TenantThreadLocal.runForAllTenantsOnBatch(tenants, currentMethodName(), runAs::add);

    assertThat(runAs).hasSize(1);
    assertThat(runAs.get(0)).isEqualTo(SINGLE_TENANT);
  }

  @Test
  public void shouldRunAsAllTenants_whenPreRegisteringTenantsOnBoot() {
    TenantUtil tenantUtilMock = mock(TenantUtil.class);
    TenantThreadLocal.tenantUtil = tenantUtilMock;
    when(tenantUtilMock.isMtiqBatchMode()).thenReturn(false);
    when(tenantUtilMock.isMultiTenant()).thenReturn(true);

    List<String> tenants =
        ImmutableList.of(createTenantNameFromTest(currentMethodName()), createTenantNameFromTest(currentMethodName()));

    List<Tenant> runAs = new ArrayList<>();
    TenantThreadLocal.runForAllTenantsOnBoot(tenants, currentMethodName(), runAs::add);

    assertThat(runAs).hasSize(2);
    for (int i = 0; i < tenants.size(); i++) {
      assertThat(runAs.get(i).tenantSlug).isEqualTo(tenants.get(i));
    }
  }

  @Test
  public void runAs_shouldRunInSpecifiedTenant() {
    Tenant tenant1 = new Tenant("tenant1");

    Tenant tenant = TenantThreadLocal.runAs(tenant1, TenantThreadLocal::getTenantWithoutValidation);
    assertThat(tenant).isSameAs(tenant1);
  }

  @Test
  public void runAs_shouldResetPreviousTenantAfter() {
    Tenant tenant1 = new Tenant("tenant1");
    Tenant tenant2 = new Tenant("tenant2");

    tenant1.invalidate();
    TenantTestHelper.setTenantWithoutValidation(tenant1);

    Tenant tenant = TenantThreadLocal.runAs(tenant2, TenantThreadLocal::getTenantWithoutValidation);
    assertThat(tenant).isSameAs(tenant2);
    assertThat(TenantThreadLocal.getTenantWithoutValidation()).isSameAs(tenant1);
  }

  @Test
  public void runAs_shouldDisallowTransitionIfPrevTenantIsValid() {
    Tenant tenant1 = new Tenant("tenant1");
    Tenant tenant2 = new Tenant("tenant2");
    TenantTestHelper.setTenantWithoutValidation(tenant1);

    MutableBoolean closureRan = new MutableBoolean(false);

    assertThatThrownBy(() -> TenantThreadLocal.runAs(tenant2, (Supplier<Void>) () -> {
      closureRan.setTrue();
      return null;
    }))
        .isInstanceOf(TenantThreadLocal.InvalidTenantOperationException.class);

    assertThat(closureRan.booleanValue()).isFalse();
  }

  @Test
  public void runAs_shouldAllowTransitionToSameTenant() {
    Tenant tenant1 = new Tenant("tenant1");
    TenantTestHelper.setTenantWithoutValidation(tenant1);

    Tenant tenant = TenantThreadLocal.runAs(tenant1, TenantThreadLocal::getTenantWithoutValidation);
    assertThat(tenant).isSameAs(tenant1);
    assertThat(TenantThreadLocal.getTenantWithoutValidation()).isSameAs(tenant1);
  }

  @Test
  public void runAsWithoutValidation_shouldRunInSpecifiedTenant() {
    Tenant tenant1 = new Tenant("tenant1");

    Tenant tenant = TenantThreadLocal.runAsWithoutValidation(tenant1, TenantThreadLocal::getTenantWithoutValidation);
    assertThat(tenant).isSameAs(tenant1);
  }

  @Test
  public void runAsWithoutValidation_shouldResetPreviousTenantAfter() {
    Tenant tenant1 = new Tenant("tenant1");
    Tenant tenant2 = new Tenant("tenant2");

    tenant1.invalidate();
    TenantTestHelper.setTenantWithoutValidation(tenant1);

    Tenant tenant = TenantThreadLocal.runAsWithoutValidation(tenant2, TenantThreadLocal::getTenantWithoutValidation);
    assertThat(tenant).isSameAs(tenant2);
    assertThat(TenantThreadLocal.getTenantWithoutValidation()).isSameAs(tenant1);
  }

  @Test
  public void runAsWithoutValidation_shouldAllowTransitionIfPrevTenantIsValid() {
    Tenant tenant1 = new Tenant("tenant1");
    Tenant tenant2 = new Tenant("tenant2");
    TenantTestHelper.setTenantWithoutValidation(tenant1);

    Tenant tenant = TenantThreadLocal.runAsWithoutValidation(tenant2, TenantThreadLocal::getTenantWithoutValidation);
    assertThat(tenant).isSameAs(tenant2);
    assertThat(TenantThreadLocal.getTenantWithoutValidation()).isSameAs(tenant1);
  }

  @Test
  public void runAsWithoutValidation_shouldAllowTransitionToSameTenant() {
    Tenant tenant1 = new Tenant("tenant1");
    TenantTestHelper.setTenantWithoutValidation(tenant1);

    Tenant tenant = TenantThreadLocal.runAsWithoutValidation(tenant1, TenantThreadLocal::getTenantWithoutValidation);
    assertThat(tenant).isSameAs(tenant1);
    assertThat(TenantThreadLocal.getTenantWithoutValidation()).isSameAs(tenant1);
  }
}
