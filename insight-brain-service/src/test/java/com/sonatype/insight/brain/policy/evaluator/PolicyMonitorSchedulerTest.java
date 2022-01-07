/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.time.LocalTime;

import javax.inject.Inject;

import com.sonatype.insight.brain.policy.PolicyMonitoringTask;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class PolicyMonitorSchedulerTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyMonitorScheduler policyMonitorScheduler;

  @Inject
  private InsightConfig insightConfig;

  @Mock
  private ProductLicense productLicenseMock;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(ProductLicense.class).toInstance(productLicenseMock);
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);

    super.configure(binder);
  }

  private void enableLicenseForApplications(boolean enable) {
    lenient().when(productLicenseMock.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(enable);
  }

  private void enableLicenseForFirewall(boolean enable) {
    lenient().when(productLicenseMock.hasFeature(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE)).thenReturn(enable);
    lenient().when(productLicenseMock.hasFeature(LicensedFeature.RELEASE_INTEGRITY)).thenReturn(enable);
  }

  @Test
  public void testStartServer_PolicyMonitoringUnlicensed() {
    enableLicenseForApplications(false);
    enableLicenseForFirewall(false);

    policyMonitorScheduler.start();

    verifyNoInteractions(taskSchedulerMock);
  }

  @Test
  public void testStartServer_PolicyMonitoringLicensedForApplications() {
    enableLicenseForApplications(true);
    enableLicenseForFirewall(false);

    testStartServer_PolicyMonitoringLicensed();
  }

  @Test
  public void testStartServer_PolicyMonitoringLicensedForFirewall() {
    enableLicenseForApplications(false);
    enableLicenseForFirewall(true);

    testStartServer_PolicyMonitoringLicensed();
  }

  private void testStartServer_PolicyMonitoringLicensed() {
    policyMonitorScheduler.start();

    ArgumentCaptor<LocalTime> startTimeCaptor = ArgumentCaptor.forClass(LocalTime.class);
    verify(taskSchedulerMock).scheduleDailyTask(eq(PolicyMonitoringTask.class), eq(PolicyMonitoringTask.NAME),
        startTimeCaptor.capture());
    assertThat(startTimeCaptor.getValue()).isBetween(LocalTime.of(insightConfig.getPolicyMonitoringHour(), 0),
        LocalTime.of(insightConfig.getPolicyMonitoringHour(), 15));
  }

  @Test
  public void testProductLicenseChanged_MonitoringWasAddedForApplications() {
    enableLicenseForApplications(true);

    testProductLicenseChanged_MonitoringWasAdded();
  }

  @Test
  public void testProductLicenseChanged_MonitoringWasAddedForFirewall() {
    enableLicenseForFirewall(true);

    testProductLicenseChanged_MonitoringWasAdded();
  }

  private void testProductLicenseChanged_MonitoringWasAdded() {
    policyMonitorScheduler.productLicenseChanged();

    ArgumentCaptor<LocalTime> startTimeCaptor = ArgumentCaptor.forClass(LocalTime.class);
    verify(taskSchedulerMock).scheduleDailyTask(eq(PolicyMonitoringTask.class), eq(PolicyMonitoringTask.NAME),
        startTimeCaptor.capture());
    assertThat(startTimeCaptor.getValue()).isBetween(LocalTime.of(insightConfig.getPolicyMonitoringHour(), 0),
        LocalTime.of(insightConfig.getPolicyMonitoringHour(), 15));
  }

  @Test
  public void testProductLicenseChanged_MonitoringWasRemoved() {
    policyMonitorScheduler.productLicenseChanged();

    verify(taskSchedulerMock).unscheduleTask(PolicyMonitoringTask.NAME);
  }
}
