/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.PrintWriter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobExecutionContext;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

  @Inject
  private Configuration configuration;

  private HistoricalPolicyViolationTelemetryTask task;

  @Before
  public void setup() {
    task = new HistoricalPolicyViolationTelemetryTask(configuration, historicalPolicyViolationTelemetryService,
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
  public void testExecute() throws Exception {
    Map<String, List<String>> parameters = Map.of();
    PrintWriter printWriter = new PrintWriter(System.out);

    when(tenantUtil.isSingleTenant()).thenReturn(true);
    when(historicalPolicyViolationTelemetryService.isTelemetryCollectionComplete()).thenReturn(false);

    task.execute(parameters, printWriter);

    verify(taskScheduler).scheduleOneTimeTask(any());
  }

  @Test
  public void testExecute_telemetryCollectionIsComplete() throws Exception {
    Map<String, List<String>> parameters = Map.of();
    PrintWriter printWriter = new PrintWriter(System.out);

    when(tenantUtil.isSingleTenant()).thenReturn(true);
    when(historicalPolicyViolationTelemetryService.isTelemetryCollectionComplete()).thenReturn(true);

    task.execute(parameters, printWriter);

    verify(taskScheduler, never()).scheduleOneTimeTask(any());
  }

  @Test
  public void testExecute_telemetryCollectionIsComplete_multiTenant() throws Exception {
    Map<String, List<String>> parameters = Map.of();
    PrintWriter printWriter = new PrintWriter(System.out);

    when(tenantUtil.isSingleTenant()).thenReturn(false);

    task.execute(parameters, printWriter);

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
    // Clock frozen at Mon Jan 20 2025 01:13:35 Z
    final long nowMs = 1737335615000L;
    final long taskStartUpDelayMinutes = 15L;
    final Clock fixedClock = Clock.fixed(Instant.ofEpochMilli(nowMs), ZoneId.systemDefault());
    ZonedDateTime expectedStartTime = fixedClock.instant()
        .atZone(ZoneId.systemDefault())
        .plusMinutes(taskStartUpDelayMinutes);

    Date startTime = task.getStartTime(fixedClock.instant().atZone(ZoneId.systemDefault()).toLocalDateTime());

    assertThat(startTime).isEqualTo(Date.from(expectedStartTime.toInstant()));
  }

  @Test
  public void testGetStartTime_nowFixedBefore_HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR_3AM() {
    // Clock frozen at Mon Jan 20 2025 01:13:35 Z
    final long frozenNowMs = 1737335615000L;
    final ZoneId zone = ZoneId.systemDefault();
    final LocalDateTime now = Instant.ofEpochMilli(frozenNowMs).atZone(zone).toLocalDateTime();
    int startHour = 3;

    tempEntity.newSystemConfigurationProperty(HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR, String.valueOf(startHour));
    configuration.configurationChanged(Sets.newHashSet(HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR));

    Date startTime = task.getStartTime(now);

    assertThat(startTime).isEqualTo(Date.from(nextScheduledStartFromHour(now, startHour, zone).toInstant()));
  }

  @Test
  public void testGetStartTime_nowFixedAfter_HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR_3AM() {
    // Clock frozen at Mon Jan 20 2025 14:13:35 Z
    final long frozenNowMs = 1737382415000L;
    final ZoneId zone = ZoneId.systemDefault();
    final LocalDateTime now = Instant.ofEpochMilli(frozenNowMs).atZone(zone).toLocalDateTime();
    int startHour = 3;

    tempEntity.newSystemConfigurationProperty(HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR, String.valueOf(startHour));
    configuration.configurationChanged(Sets.newHashSet(HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR));

    Date startTime = task.getStartTime(now);

    assertThat(startTime).isEqualTo(Date.from(nextScheduledStartFromHour(now, startHour, zone).toInstant()));
  }

  /** Mirrors {@link HistoricalPolicyViolationTelemetryTask#getNextStartTimeFromHour}. */
  private static ZonedDateTime nextScheduledStartFromHour(
      final LocalDateTime now,
      final int startHour,
      final ZoneId zone)
  {
    ZonedDateTime zonedStartTime = now.withHour(startHour)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)
        .atZone(zone);
    if (!zonedStartTime.isAfter(now.atZone(zone))) {
      zonedStartTime = zonedStartTime.plusDays(1);
    }
    return zonedStartTime;
  }
}
