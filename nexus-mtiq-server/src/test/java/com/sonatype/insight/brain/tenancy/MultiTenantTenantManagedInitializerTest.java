/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantTenantManagedInitializerTest
    extends MultiTenantTestSupport
{
  @Mock
  TenantUtil tenantUtil;

  @Test
  public void shouldCallRegisterOnStart_whenGlobalTenantJob() throws Exception {
    TenantManaged job1 = mock(TenantManaged.class);
    TenantManaged job2 = mock(MockTenantManaged.class);
    TenantManaged job3 = mock(MockTenantManaged.class);

    MultiTenantTenantManagedInitializer initializer =
            new MultiTenantTenantManagedInitializer(ImmutableList.of(job1, job2, job3), tenantUtil);

    initializer.start();

    verify(job1, never()).register();
    verify(job2).register();
    verify(job3).register();
  }

  @Test
  public void shouldRegisterAllTenantsJobs_whenMtiqBatchMode() throws Exception {
    TenantManaged job = mock(AllTenantsJob.class);

    MultiTenantTenantManagedInitializer initializer =
            new MultiTenantTenantManagedInitializer(ImmutableList.of(job), tenantUtil);

    when(tenantUtil.isMtiqBatchMode()).thenReturn(true);

    initializer.start();

    verify(job).register();
  }

  @Test
  public void shouldNotRegisterAllTenantsJobs_whenMtiqBatchMode() throws Exception {
    TenantManaged job = mock(AllTenantsJob.class);

    MultiTenantTenantManagedInitializer initializer =
            new MultiTenantTenantManagedInitializer(ImmutableList.of(job), tenantUtil);

    when(tenantUtil.isMtiqBatchMode()).thenReturn(false);

    initializer.start();

    verify(job, never()).register();
  }

  @Test
  public void shouldCallDeregisterOnStop() throws Exception {
    TenantManaged job1 = mock(TenantManaged.class);
    TenantManaged job2 = mock(TenantManaged.class);

    MultiTenantTenantManagedInitializer
            initializer = new MultiTenantTenantManagedInitializer(ImmutableList.of(job1, job2), tenantUtil);

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
            new MultiTenantTenantManagedInitializer(ImmutableList.of(job), tenantUtil);

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
            new MultiTenantTenantManagedInitializer(ImmutableList.of(job), tenantUtil);

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
            new MultiTenantTenantManagedInitializer(ImmutableList.of(job1, job2), tenantUtil);

    initializer.start();

    verify(job1).register();
    verify(job2, never()).register();
  }

  private static class MockTenantManaged
      implements TenantManaged, GlobalTenantJob
  {
    // We need a mock that implements GlobalTenantJob and TenantManaged
  }
}
