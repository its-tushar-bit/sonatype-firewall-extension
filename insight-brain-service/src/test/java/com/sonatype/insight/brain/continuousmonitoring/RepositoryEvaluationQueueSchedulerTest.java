/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Set;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.Configuration;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RepositoryEvaluationQueueScheduler} (CLM-40039 §6.1, AT-011):
 * feature-flag gating, periodic 24h cadence at {@code policyMonitoringHour + 10m ± jitter},
 * config-change start/stop transitions.
 */
@RunWith(MockitoJUnitRunner.class)
public class RepositoryEvaluationQueueSchedulerTest
{
  private static final Duration EXPECTED_INTERVAL = Duration.ofHours(24);

  private static final int POLICY_MONITORING_HOUR = 2;

  private static final int JITTER_MINUTES = 5;

  @Mock
  private Configuration configuration;

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private RepositoryEvaluationQueueProducerJob producer;

  private RepositoryEvaluationQueueScheduler underTest;

  @BeforeClass
  public static void installFeatureFlagShim() {
    HostedRepositoryEvaluationFeatureFlagTestRule.install();
  }

  @AfterClass
  public static void uninstallFeatureFlagShim() {
    HostedRepositoryEvaluationFeatureFlagTestRule.uninstall();
  }

  @Before
  public void setup() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
    when(configuration.getPolicyMonitoringHour()).thenReturn(POLICY_MONITORING_HOUR);
    when(configuration.getContinuousMonitoringJitterMinutes()).thenReturn(JITTER_MINUTES);
    underTest = new RepositoryEvaluationQueueScheduler(configuration, taskScheduler, producer);
  }

  @After
  public void tearDown() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
  }

  @Test
  public void testRegister_noOpsWhenFeatureDisabled() {
    underTest.register();

    verifyNoInteractions(taskScheduler);
  }

  @Test
  public void testRegister_schedulesPeriodicWithinJitterWindowWhenEnabled() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);

    underTest.register();

    ArgumentCaptor<Date> startTime = ArgumentCaptor.forClass(Date.class);
    verify(taskScheduler).schedulePeriodicTask(eq(producer), eq(EXPECTED_INTERVAL), startTime.capture());

    assertWithinJitterWindowOfDailyAnchor(startTime.getValue().toInstant(), POLICY_MONITORING_HOUR, JITTER_MINUTES);
  }

  @Test
  public void testRegister_handlesNullPolicyMonitoringHourAsZero() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    when(configuration.getPolicyMonitoringHour()).thenReturn(null);

    underTest.register();

    ArgumentCaptor<Date> startTime = ArgumentCaptor.forClass(Date.class);
    verify(taskScheduler).schedulePeriodicTask(eq(producer), eq(EXPECTED_INTERVAL), startTime.capture());

    assertWithinJitterWindowOfDailyAnchor(startTime.getValue().toInstant(), 0, JITTER_MINUTES);
  }

  @Test
  public void testRegister_handlesNullJitterAsFiveMinutes() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    when(configuration.getContinuousMonitoringJitterMinutes()).thenReturn(null);

    underTest.register();

    ArgumentCaptor<Date> startTime = ArgumentCaptor.forClass(Date.class);
    verify(taskScheduler).schedulePeriodicTask(eq(producer), eq(EXPECTED_INTERVAL), startTime.capture());

    assertWithinJitterWindowOfDailyAnchor(startTime.getValue().toInstant(), POLICY_MONITORING_HOUR, 5);
  }

  /**
   * Asserts that {@code captured} falls within {@code ±jitterMinutes} of either today's or tomorrow's
   * anchor (computed from {@code anchorHour:10}). Uses {@link Instant} arithmetic throughout so the
   * assertion is calendar-safe — does not break when the anchor hour and jitter combine to roll
   * past midnight, where a {@link LocalTime}-based {@code isBetween} would silently fail because
   * {@code LocalTime} ordering does not wrap (see CLAUDE.md §6 / PR #15402 calendar-flaky bug class).
   */
  private static void assertWithinJitterWindowOfDailyAnchor(
      final Instant captured,
      final int anchorHour,
      final int jitterMinutes)
  {
    Instant todayAnchor = LocalDateTime
        .of(LocalDate.now(), LocalTime.of(anchorHour, 10))
        .atZone(ZoneId.systemDefault())
        .toInstant();
    Instant tomorrowAnchor = todayAnchor.plus(Duration.ofDays(1));
    Duration jitter = Duration.ofMinutes(jitterMinutes);
    boolean inToday = !captured.isBefore(todayAnchor.minus(jitter))
        && !captured.isAfter(todayAnchor.plus(jitter));
    boolean inTomorrow = !captured.isBefore(tomorrowAnchor.minus(jitter))
        && !captured.isAfter(tomorrowAnchor.plus(jitter));
    assertThat(inToday || inTomorrow)
        .as("captured %s should fall within ±%d min of today's or tomorrow's anchor at %02d:10",
            captured, jitterMinutes, anchorHour)
        .isTrue();
  }

  @Test
  public void testRegister_withZeroJitterPinsToAnchorMinute() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    when(configuration.getContinuousMonitoringJitterMinutes()).thenReturn(0);

    underTest.register();

    ArgumentCaptor<Date> startTime = ArgumentCaptor.forClass(Date.class);
    verify(taskScheduler).schedulePeriodicTask(eq(producer), eq(EXPECTED_INTERVAL), startTime.capture());

    LocalTime captured = startTime.getValue().toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
    assertThat(captured).isEqualTo(LocalTime.of(POLICY_MONITORING_HOUR, 10));
  }

  @Test
  public void testRegister_startTimeIsTodayOrTomorrowDependingOnAnchor() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);

    underTest.register();

    ArgumentCaptor<Date> startTime = ArgumentCaptor.forClass(Date.class);
    verify(taskScheduler).schedulePeriodicTask(eq(producer), eq(EXPECTED_INTERVAL), startTime.capture());

    Instant capturedInstant = startTime.getValue().toInstant();
    Instant todayAnchor = LocalDateTime
        .of(LocalDate.now(), LocalTime.of(POLICY_MONITORING_HOUR, 10))
        .atZone(ZoneId.systemDefault())
        .toInstant();
    Instant tomorrowAnchor = todayAnchor.plus(Duration.ofDays(1));
    Duration jitter = Duration.ofMinutes(JITTER_MINUTES);
    boolean inToday = !capturedInstant.isBefore(todayAnchor.minus(jitter))
        && !capturedInstant.isAfter(todayAnchor.plus(jitter));
    boolean inTomorrow = !capturedInstant.isBefore(tomorrowAnchor.minus(jitter))
        && !capturedInstant.isAfter(tomorrowAnchor.plus(jitter));
    assertThat(inToday || inTomorrow).isTrue();
  }

  @Test
  public void testRegister_skipsSchedulingWhenDisabledForTesting() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    underTest.disableForTesting = true;

    underTest.register();

    verifyNoInteractions(taskScheduler);
  }

  @Test
  public void testDeregister_unschedulesProducer() {
    underTest.deregister();

    verify(taskScheduler).unscheduleTask(producer);
  }

  @Test
  public void testReschedule_noOpsWhenFeatureDisabled() {
    underTest.reschedule();

    verifyNoInteractions(taskScheduler);
  }

  @Test
  public void testReschedule_startsSchedulingWhenFeatureEnabled() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);

    underTest.reschedule();

    verify(taskScheduler).schedulePeriodicTask(eq(producer), eq(EXPECTED_INTERVAL), any(Date.class));
  }

  @Test
  public void testConfigurationChanged_ignoresUnrelatedProperties() {
    underTest.configurationChanged(Set.of(SystemConfigurationProperty.HDS_URL));

    verifyNoInteractions(taskScheduler);
  }

  @Test
  public void testConfigurationChanged_startsSchedulingWhenFeatureBecomesEnabled() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);

    underTest.configurationChanged(Set.of(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION));

    verify(taskScheduler).schedulePeriodicTask(eq(producer), eq(EXPECTED_INTERVAL), any(Date.class));
  }

  @Test
  public void testConfigurationChanged_stopsSchedulingWhenFeatureBecomesDisabled() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
    when(taskScheduler.unscheduleTask(producer)).thenReturn(true);

    underTest.configurationChanged(Set.of(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION));

    verify(taskScheduler).unscheduleTask(producer);
    verify(taskScheduler, never()).schedulePeriodicTask(any(), any(Duration.class), any(Date.class));
  }

  @Test
  public void testConfigurationChanged_reschedulesWhenJitterChangesWhileEnabled() {
    // Operator tunes the jitter property while the feature is enabled — we must re-anchor the
    // scheduled start time so the new jitter takes effect without a server restart.
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);

    underTest.configurationChanged(Set.of(SystemConfigurationProperty.CONTINUOUS_MONITORING_JITTER_MINUTES));

    verify(taskScheduler).schedulePeriodicTask(eq(producer), eq(EXPECTED_INTERVAL), any(Date.class));
  }

  @Test
  public void testConfigurationChanged_jitterChangeWhileDisabledIsNoOp() {
    // Operator tunes the jitter property while the feature is disabled — nothing should be
    // scheduled or unscheduled. A diagnostic log fires (not asserted here) so the operator who
    // tuned jitter sees that the new value is queued for next enable.
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);

    underTest.configurationChanged(Set.of(SystemConfigurationProperty.CONTINUOUS_MONITORING_JITTER_MINUTES));

    verifyNoInteractions(taskScheduler);
  }

  @Test
  public void testStartScheduling_firstUnschedulesPriorRunBeforeRescheduling() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);

    underTest.register();
    underTest.reschedule();

    verify(taskScheduler, times(2)).unscheduleTask(producer);
    verify(taskScheduler, times(2))
        .schedulePeriodicTask(eq(producer), eq(EXPECTED_INTERVAL), any(Date.class));
  }
}
