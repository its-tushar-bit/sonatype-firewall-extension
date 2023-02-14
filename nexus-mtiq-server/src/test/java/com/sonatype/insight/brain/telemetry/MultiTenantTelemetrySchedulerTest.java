/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.time.Duration;

import com.sonatype.insight.brain.scheduler.MultiTenantTaskScheduler;
import com.sonatype.insight.brain.tenancy.MultiTenantTest;
import com.sonatype.insight.brain.tenancy.Tenant;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.assertTenantSet;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.createTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.setTenant;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantTelemetrySchedulerTest
    extends MultiTenantTest
{
  @Mock
  private MultiTenantTaskScheduler taskScheduler;

  @Mock
  private MultiTenantTelemetryTask multiTenantTelemetryTask;

  private MultiTenantTelemetryScheduler multiTenantTelemetryScheduler;

  @Before
  public void before() {
    multiTenantTelemetryScheduler =
        new MultiTenantTelemetryScheduler(taskScheduler, multiTenantTelemetryTask);
  }

  @Test
  public void testRegister_Schedule() {
    multiTenantTelemetryScheduler.register();

    verify(taskScheduler).schedulePeriodicTask(eq(multiTenantTelemetryScheduler),
        eq(Duration.ofDays(1)), any());
  }

  @Test
  public void testExecute_Schedule() throws Exception {
    multiTenantTelemetryScheduler.execute(null);

    verify(multiTenantTelemetryTask).execute(eq(null));
    verify(taskScheduler).scheduleOneTimeTaskForAllOtherNodes(multiTenantTelemetryTask);
  }

  @Test
  public void testRegister_ScheduleForMultiTenant() throws Exception {
    Tenant tenantA = createTenant("TenantA");

    setTenant(tenantA);

    doAnswer( unused -> {
      assertTenantSet(tenantA);
      return null;
    }).when(taskScheduler).schedulePeriodicTask(eq(multiTenantTelemetryScheduler), eq(Duration.ofDays(1)), any());

    multiTenantTelemetryScheduler.register();

    verify(taskScheduler).schedulePeriodicTask(eq(multiTenantTelemetryScheduler), eq(Duration.ofDays(1)), any());
  }

  @Test
  public void testExecute_ScheduleForMultiTenant() throws Exception {
    Tenant tenantA = createTenant("TenantA");

    setTenant(tenantA);

    doAnswer( unused -> {
      assertTenantSet(tenantA);
      return null;
    }).when(multiTenantTelemetryTask).execute(null);

    multiTenantTelemetryScheduler.execute(null);

    verify(multiTenantTelemetryTask).execute(null);
  }
}
