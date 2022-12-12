/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class MultiTenantTenantManagedInitializerTest
    extends MultiTenantTest
{
  @Test
  public void shouldCallRegisterOnStart_whenGlobalTenantJob() throws Exception {
    TenantManaged job1 = mock(TenantManaged.class);
    TenantManaged job2 = mock(GlobalTenantManaged.class);
    TenantManaged job3 = mock(GlobalTenantManaged.class);

    MultiTenantTenantManagedInitializer initializer =
        new MultiTenantTenantManagedInitializer(ImmutableList.of(job1, job2, job3));

    initializer.start();

    verify(job1, never()).register();
    verify(job2).register();
    verify(job3).register();
  }

  @Test
  public void shouldCallDeregisterOnStop() throws Exception {
    TenantManaged job1 = mock(TenantManaged.class);
    TenantManaged job2 = mock(TenantManaged.class);

    MultiTenantTenantManagedInitializer
        initializer = new MultiTenantTenantManagedInitializer(ImmutableList.of(job1, job2));

    initializer.stop();

    verify(job1).deregister();
    verify(job2).deregister();
  }

  @Test
  public void registerShouldRunAsGlobalTenant() throws Exception {
    TenantThreadLocal.setGlobalTenant();

    TenantManaged job = mock(GlobalTenantManaged.class);

    doAnswer(invocationOnMock -> {
      assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(Tenant.GLOBAL_TENANT);
      return null;
    }).when(job).register();

    MultiTenantTenantManagedInitializer initializer = new MultiTenantTenantManagedInitializer(ImmutableList.of(job));

    initializer.start();

    verify(job).register();
  }

  @Test
  public void deregisterShouldRunAsGlobalTenant() throws Exception {
    TenantThreadLocal.setGlobalTenant();

    TenantManaged job = mock(GlobalTenantManaged.class);

    doAnswer(invocationOnMock -> {
      assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(Tenant.GLOBAL_TENANT);
      return null;
    }).when(job).deregister();

    MultiTenantTenantManagedInitializer initializer = new MultiTenantTenantManagedInitializer(ImmutableList.of(job));

    initializer.stop();

    verify(job).deregister();
  }
}
