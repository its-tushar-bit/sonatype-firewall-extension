/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TenantVirtualThreadExecutorTest
{
  private TenantVirtualThreadExecutor executor;

  @BeforeEach
  public void setUp() {
    TenantTestHelper.setSingleTenant();
    executor = new TenantVirtualThreadExecutor(null, "test_kind", "TestService");
  }

  @AfterEach
  public void tearDown() {
    executor.shutdown();
    TenantTestHelper.resetAfterTest();
  }

  @Test
  public void testTenantIsPropagatedToVirtualThread() throws Exception {
    Tenant expectedTenant = new Tenant("test-tenant");
    TenantTestHelper.setTenantWithoutValidation(expectedTenant);

    AtomicReference<Tenant> capturedTenant = new AtomicReference<>();
    CountDownLatch done = new CountDownLatch(1);

    executor.execute(() -> {
      capturedTenant.set(TenantThreadLocal.getTenantWithoutValidation());
      done.countDown();
    });

    assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(capturedTenant.get()).isNotNull();
    assertThat(capturedTenant.get().tenantSlug).isEqualTo("test-tenant");
  }

  @Test
  public void testTenantIsPropagatedViaSubmit() throws Exception {
    Tenant expectedTenant = new Tenant("submit-tenant");
    TenantTestHelper.setTenantWithoutValidation(expectedTenant);

    AtomicReference<Tenant> capturedTenant = new AtomicReference<>();

    Future<?> future = executor.submit(() -> {
      capturedTenant.set(TenantThreadLocal.getTenantWithoutValidation());
    });

    future.get(5, TimeUnit.SECONDS);
    assertThat(capturedTenant.get()).isNotNull();
    assertThat(capturedTenant.get().tenantSlug).isEqualTo("submit-tenant");
  }

  @Test
  public void testTenantIsPropagatedViaSubmitCallable() throws Exception {
    Tenant expectedTenant = new Tenant("callable-tenant");
    TenantTestHelper.setTenantWithoutValidation(expectedTenant);

    Future<String> future = executor.submit(() -> {
      Tenant tenant = TenantThreadLocal.getTenantWithoutValidation();
      return tenant != null ? tenant.tenantSlug : null;
    });

    assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo("callable-tenant");
  }

  @Test
  public void testSubmittingThreadTenantIsUnaffected() throws Exception {
    Tenant expectedTenant = new Tenant("invalidated-tenant");
    TenantTestHelper.setTenantWithoutValidation(expectedTenant);

    CountDownLatch done = new CountDownLatch(1);

    executor.execute(done::countDown);
    assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();

    // The submitting thread's tenant should still be set
    assertThat(TenantThreadLocal.getTenantWithoutValidation().tenantSlug).isEqualTo("invalidated-tenant");
  }

  @Test
  public void testRunsOnVirtualThreads() throws Exception {
    TenantTestHelper.setSingleTenant();

    AtomicReference<Boolean> wasVirtual = new AtomicReference<>();
    CountDownLatch done = new CountDownLatch(1);

    executor.execute(() -> {
      wasVirtual.set(Thread.currentThread().isVirtual());
      done.countDown();
    });

    assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(wasVirtual.get()).isTrue();
  }
}
