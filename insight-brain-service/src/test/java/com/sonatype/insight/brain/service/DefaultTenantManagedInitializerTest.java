/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableSet;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import org.junit.Test;
import org.mockito.InOrder;

public class DefaultTenantManagedInitializerTest
{
  @Test
  public void shouldCallRegisterOnStart() throws Exception {
    InsightJob job1 = mock(InsightJob.class);
    InsightJob job2 = mock(InsightJob.class);
    TenantManaged tenantManaged = mock(TenantManaged.class);
    TaskScheduler taskScheduler = mock(TaskScheduler.class);

    DefaultTenantManagedInitializer initializer =
        new DefaultTenantManagedInitializer(ImmutableSet.of(job1, job2, tenantManaged), taskScheduler);

    initializer.start();

    InOrder inOrder = inOrder(taskScheduler, job1, job2, tenantManaged);
    inOrder.verify(taskScheduler).start();
    inOrder.verify(job1).register();
    inOrder.verify(job2).register();
    inOrder.verify(tenantManaged).register();
  }

  @Test
  public void shouldCallDeregisterOnStop() throws Exception {
    InsightJob job1 = mock(InsightJob.class);
    InsightJob job2 = mock(InsightJob.class);
    TenantManaged tenantManaged = mock(TenantManaged.class);
    TaskScheduler taskScheduler = mock(TaskScheduler.class);

    DefaultTenantManagedInitializer initializer =
        new DefaultTenantManagedInitializer(ImmutableSet.of(job1, job2, tenantManaged), taskScheduler);

    initializer.start();
    initializer.stop();

    verify(job1).deregister();
    verify(job2).deregister();
    verify(tenantManaged).deregister();
  }
}
