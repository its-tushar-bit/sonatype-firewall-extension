/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import static com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryBackfillTask.TASK_STARTUP_DELAY_MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobExecutionContext;

@RunWith(MockitoJUnitRunner.class)
public class PolicyWaiverTelemetryBackfillTaskTest
    extends AbstractComponentTest
{
  @Mock
  private PolicyWaiverTelemetryBackfillService policyWaiverTelemetryBackfillService;

  @Mock
  private JobExecutionContext jobExecutionContext;

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private TenantUtil tenantUtil;

  private PolicyWaiverTelemetryBackfillTask task;

  private boolean disableForTesting;

  @Before
  public void setup() {
    task = new PolicyWaiverTelemetryBackfillTask(policyWaiverTelemetryBackfillService, taskScheduler, tenantUtil);
    disableForTesting = task.disableForTesting;
    task.disableForTesting = false;
  }

  @After
  public void cleanup() {
    task.disableForTesting = disableForTesting;
  }

  @Test
  public void testSchedulePolicyWaiverTelemetryBackfillTask() {
    final var startTime = DateUtils.addMinutes(new Date(), (int) TASK_STARTUP_DELAY_MINUTES);
    for (boolean isSingleTenant : new boolean[]{true, false}) {
      lenient().when(tenantUtil.isSingleTenant()).thenReturn(isSingleTenant);
      lenient().when(policyWaiverTelemetryBackfillService.isTelemetryCollectionComplete()).thenReturn(false);

      task.register();

      ArgumentCaptor<Date> startTimeCaptor = ArgumentCaptor.forClass(Date.class);
      verify(taskScheduler, times(1)).schedulePeriodicTask(any(PolicyWaiverTelemetryBackfillTask.class),
          eq(Duration.ofDays(1)), startTimeCaptor.capture());

      assertThat(startTimeCaptor.getValue()).isAfterOrEqualTo(startTime);
      reset(taskScheduler);
    }
  }

  @Test
  public void testSchedulePolicyWaiverTelemetryBackfillTask_telemetryCollectionIsComplete() {
    when(tenantUtil.isSingleTenant()).thenReturn(true);
    when(policyWaiverTelemetryBackfillService.isTelemetryCollectionComplete()).thenReturn(true);

    task.register();

    verify(taskScheduler, never()).schedulePeriodicTask(any(PolicyWaiverTelemetryBackfillTask.class),
        eq(Duration.ofDays(1)), any());
  }

  @Test
  public void testSchedulePolicyWaiverTelemetryBackfillTask_telemetryCollectionIsComplete_multiTenant() {
    when(tenantUtil.isSingleTenant()).thenReturn(false);

    task.register();

    verify(taskScheduler).schedulePeriodicTask(any(PolicyWaiverTelemetryBackfillTask.class),
        eq(Duration.ofDays(1)), any());
  }

  @Test
  public void testExecute_AdminTask() throws Exception {
    when(tenantUtil.isSingleTenant()).thenReturn(true);
    when(policyWaiverTelemetryBackfillService.isTelemetryCollectionComplete()).thenReturn(false);

    task.execute(Map.of(), new PrintWriter(OutputStream.nullOutputStream()));

    verify(taskScheduler).scheduleOneTimeTask(any());
  }

  @Test
  public void testExecute_AdminTask_telemetryCollectionIsComplete() throws Exception {
    when(tenantUtil.isSingleTenant()).thenReturn(true);
    when(policyWaiverTelemetryBackfillService.isTelemetryCollectionComplete()).thenReturn(true);

    task.execute(Map.of(), new PrintWriter(OutputStream.nullOutputStream()));

    verify(taskScheduler, never()).scheduleOneTimeTask(any());
  }

  @Test
  public void testExecute_AdminTask_multiTenant() throws Exception {
    when(tenantUtil.isSingleTenant()).thenReturn(false);

    task.execute(Map.of(), new PrintWriter(OutputStream.nullOutputStream()));

    verify(taskScheduler).scheduleOneTimeTask(any());
  }

  @Test
  public void testExecute_QuartzJob() throws Exception {
    when(tenantUtil.isSingleTenant()).thenReturn(true);

    task.execute(jobExecutionContext);

    verify(policyWaiverTelemetryBackfillService).collectAndSendPolicyWaiverBackfillTelemetry();
  }

  @Test
  public void testExecuteForTenant() {
    task.executeForTenant(jobExecutionContext, Tenant.SINGLE_TENANT);

    verify(policyWaiverTelemetryBackfillService).collectAndSendPolicyWaiverBackfillTelemetry();
  }
}
