/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.tenancy.TenantManaged;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultTenantManagedInitializerTest
{
  @Test
  public void shouldCallRegisterOnStart() throws Exception {
    InsightJob job1 = mock(InsightJob.class);
    InsightJob job2 = mock(InsightJob.class);
    TenantManaged tenantManaged = mock(TenantManaged.class);

    DefaultTenantManagedInitializer initializer =
        new DefaultTenantManagedInitializer(ImmutableSet.of(job1, job2, tenantManaged));

    initializer.start();

    verify(job1).register();
    verify(job2).register();
    verify(tenantManaged).register();
  }

  @Test
  public void shouldCallDeregisterOnStop() throws Exception {
    InsightJob job1 = mock(InsightJob.class);
    InsightJob job2 = mock(InsightJob.class);
    TenantManaged tenantManaged = mock(TenantManaged.class);

    DefaultTenantManagedInitializer initializer =
        new DefaultTenantManagedInitializer(ImmutableSet.of(job1, job2, tenantManaged));

    initializer.stop();

    verify(job1).deregister();
    verify(job2).deregister();
    verify(tenantManaged).deregister();
  }
}
