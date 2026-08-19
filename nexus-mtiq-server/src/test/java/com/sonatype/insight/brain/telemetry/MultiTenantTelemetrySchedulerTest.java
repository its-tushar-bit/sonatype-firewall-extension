/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.time.Duration;

import com.sonatype.insight.brain.scheduler.MultiTenantTaskScheduler;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.assertTenantSet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MultiTenantTelemetrySchedulerTest
    extends AbstractMultiTenantTest
{
  @Mock
  private MultiTenantTaskScheduler taskScheduler;

  @Mock
  private MultiTenantTelemetryTask multiTenantTelemetryTask;

  private MultiTenantTelemetryScheduler multiTenantTelemetryScheduler;

  @BeforeEach
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
    testAsNewTenant(t -> {
      doAnswer(unused -> {
        assertTenantSet(t);
        return null;
      }).when(taskScheduler).schedulePeriodicTask(eq(multiTenantTelemetryScheduler), eq(Duration.ofDays(1)), any());

      multiTenantTelemetryScheduler.register();

      verify(taskScheduler).schedulePeriodicTask(eq(multiTenantTelemetryScheduler), eq(Duration.ofDays(1)), any());
    });
  }
}
