/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantTenantManagedInitializerTest
    extends AbstractMultiTenantTest
{
  @Mock
  TenantUtil tenantUtil;

  @Test
  public void shouldCallRegisterOnStart_whenGlobalTenantJob() throws Exception {
    TenantManaged job1 = mock(TenantManaged.class);
    TenantManaged job2 = mock(MockTenantManaged.class);
    TenantManaged job3 = mock(MockTenantManaged.class);

    MultiTenantTenantManagedInitializer initializer =
            new MultiTenantTenantManagedInitializer(() -> ImmutableSet.of(job1, job2, job3), tenantUtil);

    initializer.start();

    verify(job1, never()).register();
    verify(job2).register();
    verify(job3).register();
  }

  @Test
  public void shouldRegisterAllTenantsJobs_whenMtiqBatchMode() throws Exception {
    TenantManaged job = mock(AllTenantsJob.class);

    MultiTenantTenantManagedInitializer initializer =
            new MultiTenantTenantManagedInitializer(() -> ImmutableSet.of(job), tenantUtil);

    when(tenantUtil.isMtiqBatchMode()).thenReturn(true);

    initializer.start();

    verify(job).register();
  }

  @Test
  public void shouldRegisterAllTenantsJobs_inOrder() throws Exception {
    TenantManaged job1 = mock(MockTenantManaged.class);
    TenantManaged job2 = mock(MockTenantManaged.class);
    TenantManaged job3 = mock(MockTenantManaged.class);
    when(job1.registrationPriority()).thenReturn(3);
    when(job2.registrationPriority()).thenReturn(2);
    when(job3.registrationPriority()).thenReturn(1);

    MultiTenantTenantManagedInitializer initializer =
        new MultiTenantTenantManagedInitializer(() -> ImmutableSet.of(job1, job2, job3), tenantUtil);

    initializer.start();

    InOrder inOrder = inOrder(job3, job2, job1);
    inOrder.verify(job3).register();
    inOrder.verify(job2).register();
    inOrder.verify(job1).register();
  }

  @Test
  public void shouldNotRegisterAllTenantsJobs_whenNotMtiqBatchMode() throws Exception {
    TenantManaged job = mock(AllTenantsJob.class);

    MultiTenantTenantManagedInitializer initializer =
            new MultiTenantTenantManagedInitializer(() -> ImmutableSet.of(job), tenantUtil);

    when(tenantUtil.isMtiqBatchMode()).thenReturn(false);

    initializer.start();

    verify(job, never()).register();
  }

  @Test
  public void shouldCallDeregisterOnStop() throws Exception {
    TenantManaged job1 = mock(TenantManaged.class);
    TenantManaged job2 = mock(TenantManaged.class);

    MultiTenantTenantManagedInitializer
            initializer = new MultiTenantTenantManagedInitializer(() -> ImmutableSet.of(job1, job2), tenantUtil);

    initializer.stop();

    verify(job1).deregister();
    verify(job2).deregister();
  }

  @Test
  public void registerShouldRunAsGlobalTenant() throws Exception {
    TenantThreadLocal.setGlobalTenant();

    TenantManaged job = mock(MockTenantManaged.class);

    doAnswer(invocationOnMock -> {
      assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(Tenant.GLOBAL_TENANT);
      return null;
    }).when(job).register();

    MultiTenantTenantManagedInitializer initializer =
            new MultiTenantTenantManagedInitializer(() -> ImmutableSet.of(job), tenantUtil);

    initializer.start();

    verify(job).register();
  }

  @Test
  public void deregisterShouldRunAsGlobalTenant() throws Exception {
    TenantThreadLocal.setGlobalTenant();

    TenantManaged job = mock(MockTenantManaged.class);

    doAnswer(invocationOnMock -> {
      assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(Tenant.GLOBAL_TENANT);
      return null;
    }).when(job).deregister();

    MultiTenantTenantManagedInitializer initializer =
            new MultiTenantTenantManagedInitializer(() -> ImmutableSet.of(job), tenantUtil);

    initializer.stop();

    verify(job).deregister();
  }

  @Test
  public void shouldCallRegisterOnStart_whenNotGlobalTenantJob_butGlobalTenantRegistration() throws Exception {
    TenantManaged job1 = mock(TenantManaged.class);
    when(job1.includeGlobalTenantDuringRegistration()).thenReturn(true);

    TenantManaged job2 = mock(TenantManaged.class);
    when(job2.includeGlobalTenantDuringRegistration()).thenReturn(false);

    MultiTenantTenantManagedInitializer initializer =
        new MultiTenantTenantManagedInitializer(() -> ImmutableSet.of(job1, job2), tenantUtil);

    initializer.start();

    verify(job1).register();
    verify(job2, never()).register();
  }

  @Test
  public void shouldNotRegister_whenBatchJob_butNotBatchNode() throws Exception {
    when(tenantUtil.isMtiqBatchMode()).thenReturn(false);

    TenantManaged job = mock(MockGlobalBatchTenantManaged.class);
    MultiTenantTenantManagedInitializer initializer =
        new MultiTenantTenantManagedInitializer(() -> ImmutableSet.of(job), tenantUtil);

    initializer.start();

    verify(job, never()).register();

  }

  private static class MockTenantManaged
      implements TenantManaged, GlobalTenantJob
  {
  }

  private static class MockGlobalBatchTenantManaged
      implements TenantManaged, MtiqBatchJob, GlobalTenantJob
  {
  }
}
