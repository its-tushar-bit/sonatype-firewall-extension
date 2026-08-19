/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TenantScheduledThreadPoolExecutorTest
{
  private final int interval = 1;

  private final int delay = 2;

  @Test
  public void scheduleAtFixedRate_reusesTenant() throws InterruptedException {
    Tenant initialTenant = new Tenant("initial-tenant");
    TenantTestHelper.setTenantWithoutValidation(initialTenant);

    CountDownLatch finished = new CountDownLatch(3);
    Runnable mockRunnableSpy = mock(Runnable.class);
    final Set<Tenant> usedTenants = new HashSet<>();

    doAnswer(invocation -> {
      Tenant invocationTenant = TenantThreadLocal.getTenantWithoutValidation();
      usedTenants.add(invocationTenant);
      assertThat(usedTenants).hasSize(1);
      assertThat(initialTenant.isInvalid()).isTrue();
      assertThat(invocationTenant.isInvalid()).isFalse();
      assertThat(invocationTenant).isEqualTo(initialTenant);
      assertThat(invocationTenant.tenantSlug).isEqualTo(initialTenant.tenantSlug);

      finished.countDown();
      return null;
    }).when(mockRunnableSpy).run();

    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("TenantScheduledThreadPoolExecutorTest-%d").setDaemon(true).build();
    ScheduledExecutorService scheduledExecutorService = new TenantScheduledThreadPoolExecutor(1, threadFactory);
    scheduledExecutorService.scheduleAtFixedRate(mockRunnableSpy, delay, interval,
        SECONDS);

    // The initial tenant is invalidated here to simulate how it would be run as part of the IQ lifetime, this ensures
    // the encapsulated tenant owned by scheduleAtFixedRate is used.
    initialTenant.invalidate();

    boolean ended = finished.await(6, SECONDS);
    assertThat(ended).isTrue();
    verify(mockRunnableSpy, times(3)).run();
  }

  @Test
  public void scheduleAtFixedRate_isCorrectTenantUsed() throws InterruptedException {
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("TenantScheduledThreadPoolExecutorTest-%d").setDaemon(true).build();
    ScheduledExecutorService scheduledExecutorService = new TenantScheduledThreadPoolExecutor(1, threadFactory);

    // Create tenant1 and the callback handler
    Tenant tenant1 = new Tenant("tenant1");
    CountDownLatch finished1 = new CountDownLatch(2);
    Runnable mockRunnable1Spy = mock(Runnable.class);
    doAnswer(invocation -> {
      Tenant invocationTenant = TenantThreadLocal.getTenantWithoutValidation();
      assertThat(tenant1.isInvalid()).isTrue();
      assertThat(invocationTenant.isInvalid()).isFalse();
      assertThat(invocationTenant).isEqualTo(tenant1);
      assertThat(invocationTenant.tenantSlug).isEqualTo(tenant1.tenantSlug);

      finished1.countDown();
      return null;
    }).when(mockRunnable1Spy).run();

    // Create tenant2 and the callback handler
    Tenant tenant2 = new Tenant("tenant2");
    CountDownLatch finished2 = new CountDownLatch(2);
    Runnable mockRunnable2Spy = mock(Runnable.class);
    doAnswer(invocation -> {
      Tenant invocationTenant = TenantThreadLocal.getTenantWithoutValidation();
      assertThat(tenant2.isInvalid()).isTrue();
      assertThat(invocationTenant.isInvalid()).isFalse();
      assertThat(invocationTenant).isEqualTo(tenant2);
      assertThat(invocationTenant.tenantSlug).isEqualTo(tenant2.tenantSlug);

      finished2.countDown();
      return null;
    }).when(mockRunnable2Spy).run();

    // The tenants are invalidated here to simulate how it would be run as part of the IQ lifetime, this ensures
    // the encapsulated tenant owned by scheduleAtFixedRate is used.
    TenantTestHelper.setTenantWithoutValidation(tenant1);
    scheduledExecutorService.scheduleAtFixedRate(mockRunnable1Spy, delay, interval, SECONDS);
    tenant1.invalidate();

    TenantTestHelper.setTenantWithoutValidation(tenant2);
    scheduledExecutorService.scheduleAtFixedRate(mockRunnable2Spy, delay, interval, SECONDS);
    tenant2.invalidate();

    boolean ended1 = finished1.await(5, SECONDS);
    assertThat(ended1).isTrue();

    boolean ended2 = finished2.await(5, SECONDS);
    assertThat(ended2).isTrue();

    verify(mockRunnable1Spy, times(2)).run();
    verify(mockRunnable2Spy, times(2)).run();
  }
}
