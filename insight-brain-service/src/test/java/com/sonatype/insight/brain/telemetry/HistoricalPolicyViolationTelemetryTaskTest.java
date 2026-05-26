/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobExecutionContext;

@RunWith(MockitoJUnitRunner.class)
public class HistoricalPolicyViolationTelemetryTaskTest
    extends AbstractComponentTest
{
  @Mock
  private HistoricalPolicyViolationTelemetryService historicalPolicyViolationTelemetryService;

  @Mock
  private JobExecutionContext jobExecutionContext;

  @Mock
  private MigrationTrackerDAO migrationTrackerDAO;

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private TenantUtil tenantUtil;

  @Mock
  private Configuration configuration;

  private HistoricalPolicyViolationTelemetryTask task;

  @Before
  public void setup() {
    task = newTask(configuration);
  }

  private HistoricalPolicyViolationTelemetryTask newTask(final Configuration taskConfiguration) {
    return new HistoricalPolicyViolationTelemetryTask(taskConfiguration, historicalPolicyViolationTelemetryService,
        taskScheduler, tenantUtil, migrationTrackerDAO);
  }

  @Test
  public void testScheduleHistoricalPolicyViolationTelemetryTask() {
    when(tenantUtil.isSingleTenant()).thenReturn(true);
    when(historicalPolicyViolationTelemetryService.isTelemetryCollectionComplete()).thenReturn(false);

    task.scheduleHistoricalPolicyViolationTelemetryTask();

    verify(taskScheduler).schedulePeriodicTask(any(HistoricalPolicyViolationTelemetryTask.class),
        eq(Duration.ofDays(1)), any());
  }

  @Test
  public void testScheduleHistoricalPolicyViolationTelemetryTask_telemetryCollectionIsComplete() {
    when(tenantUtil.isSingleTenant()).thenReturn(true);
    when(historicalPolicyViolationTelemetryService.isTelemetryCollectionComplete()).thenReturn(true);

    task.scheduleHistoricalPolicyViolationTelemetryTask();

    verify(taskScheduler, never()).schedulePeriodicTask(any(HistoricalPolicyViolationTelemetryTask.class),
        eq(Duration.ofDays(1)), any());
  }

  @Test
  public void testScheduleHistoricalPolicyViolationTelemetryTask_telemetryCollectionIsComplete_multiTenant() {
    when(tenantUtil.isSingleTenant()).thenReturn(false);

    task.scheduleHistoricalPolicyViolationTelemetryTask();

    verify(taskScheduler).schedulePeriodicTask(any(HistoricalPolicyViolationTelemetryTask.class),
        eq(Duration.ofDays(1)), any());
  }

  @Test
  public void testExecute_AdminTask() throws Exception {
    when(tenantUtil.isSingleTenant()).thenReturn(true);
    when(historicalPolicyViolationTelemetryService.isTelemetryCollectionComplete()).thenReturn(false);

    task.execute(Map.of(), new PrintWriter(OutputStream.nullOutputStream()));

    verify(taskScheduler).scheduleOneTimeTask(any());
  }

  @Test
  public void testExecute_AdminTask_telemetryCollectionIsComplete() throws Exception {
    when(tenantUtil.isSingleTenant()).thenReturn(true);
    when(historicalPolicyViolationTelemetryService.isTelemetryCollectionComplete()).thenReturn(true);

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
  public void testExecuteForTenant() {
    when(migrationTrackerDAO.isTrackerPresent(anyString())).thenReturn(true);

    task.executeForTenant(jobExecutionContext, Tenant.SINGLE_TENANT);

    verify(historicalPolicyViolationTelemetryService).collectAndSendPolicyViolationTelemetry();
  }

  @Test
  public void testExecuteForTenant_policyViolationConstraintFactsJsonIsNotMigrationComplete() {
    when(migrationTrackerDAO.isTrackerPresent(anyString())).thenReturn(false);

    task.executeForTenant(jobExecutionContext, Tenant.SINGLE_TENANT);

    verify(historicalPolicyViolationTelemetryService, never()).collectAndSendPolicyViolationTelemetry();
  }

  @Test
  public void testGetStartTime_nowFixed_plus15() {
    LocalDateTime now = LocalDateTime.of(2025, 1, 20, 1, 13, 35);
    LocalDateTime expectedStartTime = now.plusMinutes(15);
    Configuration taskConfiguration = mock(Configuration.class);
    when(taskConfiguration.getHistoricalPolicyViolationTelemetryHour()).thenReturn(null);
    HistoricalPolicyViolationTelemetryTask taskUnderTest = newTask(taskConfiguration);

    Date startTime = taskUnderTest.getStartTime(now);

    assertThat(startTime).isEqualTo(Date.from(expectedStartTime.atZone(ZoneId.systemDefault()).toInstant()));
  }

  @Test
  public void testGetStartTime_nowFixedBefore_HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR_3am() {
    int startHour = 3;
    LocalDateTime now = LocalDateTime.of(2025, 1, 20, 1, 13, 35);
    LocalDateTime expectedStartTime = now.withHour(startHour).withMinute(0).withSecond(0).withNano(0);
    Configuration taskConfiguration = mock(Configuration.class);
    when(taskConfiguration.getHistoricalPolicyViolationTelemetryHour()).thenReturn(startHour);
    HistoricalPolicyViolationTelemetryTask taskUnderTest = newTask(taskConfiguration);

    Date startTime = taskUnderTest.getStartTime(now);

    assertThat(startTime).isEqualTo(Date.from(expectedStartTime.atZone(ZoneId.systemDefault()).toInstant()));
  }

  @Test
  public void testGetStartTime_nowFixedAfter_HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR_3am() {
    int startHour = 3;
    LocalDateTime now = LocalDateTime.of(2025, 1, 20, 14, 13, 35);
    LocalDateTime expectedStartTime = now.plusDays(1)
        .withHour(startHour)
        .withMinute(0)
        .withSecond(0)
        .withNano(0);
    Configuration taskConfiguration = mock(Configuration.class);
    when(taskConfiguration.getHistoricalPolicyViolationTelemetryHour()).thenReturn(startHour);
    HistoricalPolicyViolationTelemetryTask taskUnderTest = newTask(taskConfiguration);

    Date startTime = taskUnderTest.getStartTime(now);

    assertThat(startTime).isEqualTo(Date.from(expectedStartTime.atZone(ZoneId.systemDefault()).toInstant()));
  }
}
