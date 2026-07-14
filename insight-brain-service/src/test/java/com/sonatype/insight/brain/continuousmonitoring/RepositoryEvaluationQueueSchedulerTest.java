/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.time.LocalTime;
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

@RunWith(MockitoJUnitRunner.class)
public class RepositoryEvaluationQueueSchedulerTest
{
  private static final int POLICY_MONITORING_HOUR = 2;

  private static final int JITTER_MINUTES = 5;

  private static final int PRODUCER_OFFSET_MINUTES = 10;

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
  public void testRegister_schedulesDailyWithinJitterWindowWhenEnabled() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);

    underTest.register();

    ArgumentCaptor<LocalTime> startTime = ArgumentCaptor.forClass(LocalTime.class);
    verify(taskScheduler).scheduleDailyTask(eq(producer), startTime.capture());

    assertWithinJitterWindow(startTime.getValue(), POLICY_MONITORING_HOUR, JITTER_MINUTES);
  }

  @Test
  public void testRegister_handlesNullPolicyMonitoringHourAsZero() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    when(configuration.getPolicyMonitoringHour()).thenReturn(null);

    underTest.register();

    ArgumentCaptor<LocalTime> startTime = ArgumentCaptor.forClass(LocalTime.class);
    verify(taskScheduler).scheduleDailyTask(eq(producer), startTime.capture());

    assertWithinJitterWindow(startTime.getValue(), 0, JITTER_MINUTES);
  }

  @Test
  public void testRegister_handlesNullJitterAsFiveMinutes() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    when(configuration.getContinuousMonitoringJitterMinutes()).thenReturn(null);

    underTest.register();

    ArgumentCaptor<LocalTime> startTime = ArgumentCaptor.forClass(LocalTime.class);
    verify(taskScheduler).scheduleDailyTask(eq(producer), startTime.capture());

    assertWithinJitterWindow(startTime.getValue(), POLICY_MONITORING_HOUR, 5);
  }

  @Test
  public void testRegister_withZeroJitterPinsToAnchorMinute() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    when(configuration.getContinuousMonitoringJitterMinutes()).thenReturn(0);

    underTest.register();

    verify(taskScheduler).scheduleDailyTask(producer, LocalTime.of(POLICY_MONITORING_HOUR, PRODUCER_OFFSET_MINUTES));
  }

  @Test
  public void testRegister_withNegativeJitterConfigPinsToAnchorMinute() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    when(configuration.getContinuousMonitoringJitterMinutes()).thenReturn(-10);

    underTest.register();

    verify(taskScheduler).scheduleDailyTask(producer, LocalTime.of(POLICY_MONITORING_HOUR, PRODUCER_OFFSET_MINUTES));
  }

  @Test
  public void testRegister_lateHourWithLargeJitterWrapsAcrossMidnight() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    when(configuration.getPolicyMonitoringHour()).thenReturn(23);
    when(configuration.getContinuousMonitoringJitterMinutes()).thenReturn(240);

    underTest.register();

    ArgumentCaptor<LocalTime> startTime = ArgumentCaptor.forClass(LocalTime.class);
    verify(taskScheduler).scheduleDailyTask(eq(producer), startTime.capture());

    assertWithinJitterWindow(startTime.getValue(), 23, 240);
  }

  @Test
  public void testRegister_skipsSchedulingWhenDisabledForTesting() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    underTest.disableForTesting = true;

    underTest.register();

    verifyNoInteractions(taskScheduler);
  }

  @Test
  public void testRegister_repeatedCallsStayWithinJitterWindow() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);

    underTest.register();
    underTest.register();
    underTest.register();

    ArgumentCaptor<LocalTime> startTime = ArgumentCaptor.forClass(LocalTime.class);
    verify(taskScheduler, times(3)).scheduleDailyTask(eq(producer), startTime.capture());
    for (LocalTime captured : startTime.getAllValues()) {
      assertWithinJitterWindow(captured, POLICY_MONITORING_HOUR, JITTER_MINUTES);
    }
  }

  @Test
  public void testStartScheduling_doesNotUnscheduleBeforeRescheduling() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);

    underTest.register();
    underTest.reschedule();

    verify(taskScheduler, never()).unscheduleTask(producer);
    verify(taskScheduler, times(2)).scheduleDailyTask(eq(producer), any(LocalTime.class));
  }

  /**
   * SDEV-1312: {@code deregister()} must be a no-op — on MTIQ graceful shutdown the tenant
   * context leaks to global and {@code MultiTenantTaskScheduler.unscheduleTask} would delete
   * the producer trigger cluster-wide.
   */
  @Test
  public void testDeregister_isNoOpToPreserveTriggerAcrossGracefulShutdown() {
    underTest.deregister();

    verifyNoInteractions(taskScheduler);
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

    verify(taskScheduler).scheduleDailyTask(eq(producer), any(LocalTime.class));
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

    verify(taskScheduler).scheduleDailyTask(eq(producer), any(LocalTime.class));
  }

  @Test
  public void testConfigurationChanged_stopsSchedulingWhenFeatureBecomesDisabled() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
    when(taskScheduler.unscheduleTask(producer)).thenReturn(true);

    underTest.configurationChanged(Set.of(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION));

    verify(taskScheduler).unscheduleTask(producer);
    verify(taskScheduler, never()).scheduleDailyTask(any(), any(LocalTime.class));
  }

  @Test
  public void testConfigurationChanged_reschedulesWhenJitterChangesWhileEnabled() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);

    underTest.configurationChanged(Set.of(SystemConfigurationProperty.CONTINUOUS_MONITORING_JITTER_MINUTES));

    verify(taskScheduler).scheduleDailyTask(eq(producer), any(LocalTime.class));
  }

  @Test
  public void testConfigurationChanged_jitterChangeWhileDisabledIsNoOp() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);

    underTest.configurationChanged(Set.of(SystemConfigurationProperty.CONTINUOUS_MONITORING_JITTER_MINUTES));

    verifyNoInteractions(taskScheduler);
  }

  private static void assertWithinJitterWindow(
      final LocalTime captured,
      final int anchorHour,
      final int jitterMinutes)
  {
    int anchorMinuteOfDay = (anchorHour * 60 + PRODUCER_OFFSET_MINUTES) % (24 * 60);
    int capturedMinuteOfDay = captured.getHour() * 60 + captured.getMinute();
    int forward = Math.floorMod(capturedMinuteOfDay - anchorMinuteOfDay, 24 * 60);
    int backward = Math.floorMod(anchorMinuteOfDay - capturedMinuteOfDay, 24 * 60);
    int distance = Math.min(forward, backward);
    assertThat(distance)
        .as("captured %s should be within ±%d min of anchor %02d:%02d",
            captured, jitterMinutes, anchorMinuteOfDay / 60, anchorMinuteOfDay % 60)
        .isLessThanOrEqualTo(jitterMinutes);
  }
}
