/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PolicyMonitorSchedulerTest
    extends AbstractComponentTest
{
  private static final long ONE_SECOND_IN_MS = 1000L;

  private static final long ONE_HOUR_IN_MS = 60L * 60 * ONE_SECOND_IN_MS;

  private static final long ONE_DAY_IN_MS = 24L * ONE_HOUR_IN_MS;

  @Inject
  private PolicyMonitorScheduler scheduler;

  private PolicyMonitorScheduler schedulerSpy;

  @Inject
  private InsightConfig insightConfig;

  @Mock
  private ScheduledExecutorService executorMock;

  @Mock
  private ProductLicense productLicenseMock;

  @Mock
  private PolicyMonitor policyMonitorMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(ProductLicense.class).toInstance(productLicenseMock);
    binder.bind(PolicyMonitor.class).toInstance(policyMonitorMock);

    super.configure(binder);
  }

  @Before
  public void init() {
    schedulerSpy = spy(scheduler);
    lenient().doReturn(executorMock).when(schedulerSpy).newExecutor();
  }

  @Test
  public void testStartServer_PolicyMonitoringUnlicensed() {
    when(productLicenseMock.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(false);
    schedulerSpy.start();
    verifyNoInteractions(executorMock);
  }

  @Test
  public void testStartServer_PolicyMonitoringLicensed() {
    when(productLicenseMock.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(true);
    schedulerSpy.start();
    verify(executorMock).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS));
  }

  @Test
  public void testStopServer() {
    when(productLicenseMock.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(true);
    schedulerSpy.start();
    schedulerSpy.stop();
    verify(executorMock).shutdown();
  }

  @Test
  public void testProductLicenseChanged_MonitoringWasAdded() {
    when(productLicenseMock.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(false);
    schedulerSpy.start();
    when(productLicenseMock.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(true);
    schedulerSpy.productLicenseChanged();
    verify(executorMock).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS));
  }

  @Test
  public void testProductLicenseChanged_MonitoringWasRemoved() {
    when(productLicenseMock.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(true);
    schedulerSpy.start();
    when(productLicenseMock.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(false);
    schedulerSpy.productLicenseChanged();
    verify(executorMock).shutdown();
  }

  @Test
  public void testProductLicenseChanged_MonitoringStillAvailable() {
    when(productLicenseMock.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(true);
    schedulerSpy.start();
    reset(executorMock);
    schedulerSpy.productLicenseChanged();
    verifyNoInteractions(executorMock);
  }

  @Test
  public void testProductLicenseChanged_MonitoringStillUnavailable() {
    when(productLicenseMock.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(false);
    schedulerSpy.start();
    reset(executorMock);
    schedulerSpy.productLicenseChanged();
    verifyNoInteractions(executorMock);
  }

  @Test
  public void testDetermineNextExecutionTime_NowBeforeConfiguredPolicyMonitoringHour() {
    int policyMonitoringHour = 10;
    ZonedDateTime currentDateTime =
        ZonedDateTime.of(2019 /* year */, 6 /* month */, 25 /* dayOfMonth */, policyMonitoringHour - 1 /* hour */,
            59 /* minute */, 59 /* second */, 0 /* nanoOfSecond */, ZoneId.of("America/New_York"));
    long expectedTimeDifferenceInMillisecs = ONE_SECOND_IN_MS;
    testDetermineNextExecutionTime(policyMonitoringHour, currentDateTime, expectedTimeDifferenceInMillisecs);
  }

  @Test
  public void testDetermineNextExecutionTime_NowEqualsConfiguredPolicyMonitoringHour() {
    int policyMonitoringHour = 10;
    ZonedDateTime currentDateTime =
        ZonedDateTime.of(2019 /* year */, 6 /* month */, 25 /* dayOfMonth */, policyMonitoringHour /* hour */,
            0 /* minute */, 0 /* second */, 0 /* nanoOfSecond */, ZoneId.of("America/New_York"));
    long expectedTimeDifferenceInMillisecs = ONE_DAY_IN_MS;
    testDetermineNextExecutionTime(policyMonitoringHour, currentDateTime, expectedTimeDifferenceInMillisecs);
  }

  @Test
  public void testDetermineNextExecutionTime_NowAfterConfiguredPolicyMonitoringHour() {
    int policyMonitoringHour = 10;
    ZonedDateTime currentDateTime =
        ZonedDateTime.of(2019 /* year */, 6 /* month */, 25 /* dayOfMonth */, policyMonitoringHour /* hour */,
            0 /* minute */, 1 /* second */, 0 /* nanoOfSecond */, ZoneId.of("America/New_York"));
    long expectedTimeDifferenceInMillisecs = ONE_DAY_IN_MS - ONE_SECOND_IN_MS;
    testDetermineNextExecutionTime(policyMonitoringHour, currentDateTime, expectedTimeDifferenceInMillisecs);
  }

  @Test
  public void testDetermineNextExecutionTime_DSTSpring_NowAfterDSTChange_PolicyMonitoringHourAtDSTTimeChange() {
    int policyMonitoringHour = 2;
    // In this timezone, the clocks are turned forward on Mar 10, 2019, 2:00am becomes 3:00am.
    ZonedDateTime currentDateTime = ZonedDateTime.of(2019 /* year */, 3 /* month */, 10 /* dayOfMonth */, 4 /* hour */,
        0 /* minute */, 0 /* second */, 0 /* nanoOfSecond */, ZoneId.of("America/New_York"));
    long expectedTimeDifferenceInMillisecs = 22 * ONE_HOUR_IN_MS;
    testDetermineNextExecutionTime(policyMonitoringHour, currentDateTime, expectedTimeDifferenceInMillisecs);
  }

  @Test
  public void testDetermineNextExecutionTime_DSTSpring_NowBeforeDSTChange() {
    int policyMonitoringHour = 10;
    // In this timezone, the clocks are turned forward on Mar 10, 2019, 2:00am becomes 3:00am.
    ZonedDateTime currentDateTime = ZonedDateTime.of(2019 /* year */, 3 /* month */, 10 /* dayOfMonth */, 1 /* hour */,
        59 /* minute */, 59 /* second */, 0 /* nanoOfSecond */, ZoneId.of("America/New_York"));
    long expectedTimeDifferenceInMillisecs = 7 * ONE_HOUR_IN_MS + ONE_SECOND_IN_MS;
    testDetermineNextExecutionTime(policyMonitoringHour, currentDateTime, expectedTimeDifferenceInMillisecs);
  }

  @Test
  public void testDetermineNextExecutionTime_DSTSpring_NowEqualsDSTChange() {
    int policyMonitoringHour = 10;
    // In this timezone, the clocks are turned forward on Mar 10, 2019, 2:00am becomes 3:00am.
    ZonedDateTime currentDateTime = ZonedDateTime.of(2019 /* year */, 3 /* month */, 10 /* dayOfMonth */, 1 /* hour */,
        59 /* minute */, 59 /* second */, 0 /* nanoOfSecond */, ZoneId.of("America/New_York"));
    currentDateTime = currentDateTime.plusSeconds(1);
    assertThat(currentDateTime.getHour()).isEqualTo(3);
    assertThat(currentDateTime.getMinute()).isEqualTo(0);
    assertThat(currentDateTime.getSecond()).isEqualTo(0);
    long expectedTimeDifferenceInMillisecs = 7 * ONE_HOUR_IN_MS;
    testDetermineNextExecutionTime(policyMonitoringHour, currentDateTime, expectedTimeDifferenceInMillisecs);
  }

  @Test
  public void testDetermineNextExecutionTime_DSTSpring_ImpossibleTime() {
    int policyMonitoringHour = 10;
    // In this timezone, the clocks are turned forward on Mar 10, 2019, 2:00am becomes 3:00am.
    // There is no 2:00am on Mar 10, 2019, but lets be sure it is handled correctly anyway.
    ZonedDateTime currentDateTime = ZonedDateTime.of(2019 /* year */, 3 /* month */, 10 /* dayOfMonth */, 2 /* hour */,
        0 /* minute */, 0 /* second */, 0 /* nanoOfSecond */, ZoneId.of("America/New_York"));
    assertThat(currentDateTime.getHour()).isEqualTo(3);
    assertThat(currentDateTime.getMinute()).isEqualTo(0);
    assertThat(currentDateTime.getSecond()).isEqualTo(0);
    long expectedTimeDifferenceInMillisecs = 7 * ONE_HOUR_IN_MS;
    testDetermineNextExecutionTime(policyMonitoringHour, currentDateTime, expectedTimeDifferenceInMillisecs);
  }

  @Test
  public void testDetermineNextExecutionTime_DSTSpring_NowAfterDSTChange() {
    int policyMonitoringHour = 10;
    // In this timezone, the clocks are turned forward on Mar 10, 2019, 2:00am becomes 3:00am.
    ZonedDateTime currentDateTime = ZonedDateTime.of(2019 /* year */, 3 /* month */, 10 /* dayOfMonth */, 3 /* hour */,
        0 /* minute */, 1 /* second */, 0 /* nanoOfSecond */, ZoneId.of("America/New_York"));
    long expectedTimeDifferenceInMillisecs = 7 * ONE_HOUR_IN_MS - ONE_SECOND_IN_MS;
    testDetermineNextExecutionTime(policyMonitoringHour, currentDateTime, expectedTimeDifferenceInMillisecs);
  }

  @Test
  public void testDetermineNextExecutionTime_DSTFall_NowBeforeDSTChange() {
    int policyMonitoringHour = 10;
    // In this timezone, the clocks are turned back on Nov 3, 2019, 2:00am becomes 1:00am.
    ZonedDateTime currentDateTime = ZonedDateTime.of(2019 /* year */, 11 /* month */, 3 /* dayOfMonth */, 1 /* hour */,
        59 /* minute */, 59 /* second */, 0 /* nanoOfSecond */, ZoneId.of("America/New_York"));
    long expectedTimeDifferenceInMillisecs = 9 * ONE_HOUR_IN_MS + ONE_SECOND_IN_MS;
    testDetermineNextExecutionTime(policyMonitoringHour, currentDateTime, expectedTimeDifferenceInMillisecs);
  }

  @Test
  public void testDetermineNextExecutionTime_DSTFall_NowEqualsDSTChange() {
    int policyMonitoringHour = 10;
    // In this timezone, the clocks are turned back on Nov 3, 2019, 2:00am becomes 1:00am.
    // This means there is a time overlap between 1:00am and 2:00am.
    // The clocks go from 1:00am to 2:00am first and when they reach 2:00am,
    // they are turned back to 1:00am and they go again from 1:00am to 2:00am.

    // Test when the time advances to 2:00am.
    ZonedDateTime currentDateTime = ZonedDateTime.of(2019 /* year */, 11 /* month */, 3 /* dayOfMonth */, 1 /* hour */,
        59 /* minute */, 59 /* second */, 0 /* nanoOfSecond */, ZoneId.of("America/New_York"));
    currentDateTime = currentDateTime.plusSeconds(1);
    assertThat(currentDateTime.getHour()).isEqualTo(1);
    assertThat(currentDateTime.getMinute()).isEqualTo(0);
    assertThat(currentDateTime.getSecond()).isEqualTo(0);
    long expectedTimeDifferenceInMillisecs = 9 * ONE_HOUR_IN_MS;
    testDetermineNextExecutionTime(policyMonitoringHour, currentDateTime, expectedTimeDifferenceInMillisecs);

    // Test when the time is at 2:00am the second time.
    currentDateTime = ZonedDateTime.of(2019 /* year */, 11 /* month */, 3 /* dayOfMonth */, 2 /* hour */,
        0 /* minute */, 0 /* second */, 0 /* nanoOfSecond */, ZoneId.of("America/New_York"));
    expectedTimeDifferenceInMillisecs = 8 * ONE_HOUR_IN_MS;
    testDetermineNextExecutionTime(policyMonitoringHour, currentDateTime, expectedTimeDifferenceInMillisecs);
  }

  @Test
  public void testDetermineNextExecutionTime_DSTFall_NowAfterDSTChange() {
    int policyMonitoringHour = 10;
    // In this timezone, the clocks are turned back on Nov 3, 2019, 2:00am becomes 1am.
    ZonedDateTime currentDateTime = ZonedDateTime.of(2019 /* year */, 11 /* month */, 3 /* dayOfMonth */, 2 /* hour */,
        0 /* minute */, 1 /* second */, 0 /* nanoOfSecond */, ZoneId.of("America/New_York"));
    long expectedTimeDifferenceInMillisecs = 8 * ONE_HOUR_IN_MS - ONE_SECOND_IN_MS;
    testDetermineNextExecutionTime(policyMonitoringHour, currentDateTime, expectedTimeDifferenceInMillisecs);
  }

  private void testDetermineNextExecutionTime(
      int policyMonitoringHour,
      ZonedDateTime currentDateTime,
      long expectedTimeDifferenceInMillisecs)
  {
    insightConfig.setPolicyMonitoringHour(policyMonitoringHour);
    ZonedDateTime nextExecutionDateTime = scheduler.determineNextExecutionTime(currentDateTime);

    assertThat(nextExecutionDateTime.getHour()).isEqualTo(policyMonitoringHour);
    assertThat(nextExecutionDateTime.toInstant().toEpochMilli() - currentDateTime.toInstant().toEpochMilli())
        .isEqualTo(expectedTimeDifferenceInMillisecs);
  }

  @Test
  public void testPolicyMonitoringReschedulesItself() {
    when(productLicenseMock.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(true);
    schedulerSpy.start();
    clearInvocations(executorMock);

    schedulerSpy.policyMonitoringRunnable.run();
    verify(executorMock).schedule(eq(schedulerSpy.policyMonitoringRunnable), anyLong(), eq(TimeUnit.MILLISECONDS));
  }

  @Test
  public void testPolicyMonitoringReschedulesItselfWhenPolicyMonitorThrowsException() {
    when(productLicenseMock.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(true);
    schedulerSpy.start();
    clearInvocations(executorMock);
    doThrow(new RuntimeException("test")).when(policyMonitorMock).run();

    schedulerSpy.policyMonitoringRunnable.run();
    verify(executorMock).schedule(eq(schedulerSpy.policyMonitoringRunnable), anyLong(), eq(TimeUnit.MILLISECONDS));
  }
}
