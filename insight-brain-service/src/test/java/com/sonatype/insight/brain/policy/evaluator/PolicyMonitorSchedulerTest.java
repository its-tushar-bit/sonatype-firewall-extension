/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.time.LocalTime;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.policy.PolicyMonitoringTask;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
  private Configuration configuration;

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

  @Test
  public void testStartServer_PolicyMonitoringUnlicensed() {
    enableLicenseForApplications(false);

    policyMonitorScheduler.register();

    verifyNoInteractions(taskSchedulerMock);
  }

  @Test
  public void testStartServer_PolicyMonitoringLicensedForApplications() {
    enableLicenseForApplications(true);

    testStartServer_PolicyMonitoringLicensed();
  }

  private void testStartServer_PolicyMonitoringLicensed() {
    policyMonitorScheduler.register();

    ArgumentCaptor<LocalTime> startTimeCaptor = ArgumentCaptor.forClass(LocalTime.class);
    verify(taskSchedulerMock).scheduleDailyTask(any(PolicyMonitoringTask.class),
        startTimeCaptor.capture());

    LocalTime policyMonitoringHour = LocalTime.of(configuration.getPolicyMonitoringHour(), 0);
    assertThat(startTimeCaptor.getValue()).isBetween(LocalTime.of(configuration.getPolicyMonitoringHour(), 0),
        policyMonitoringHour.plusMinutes(120));
  }

  @Test
  public void testProductLicenseChanged_MonitoringWasAddedForApplications() {
    enableLicenseForApplications(true);

    testProductLicenseChanged_MonitoringWasAdded();
  }

  private void testProductLicenseChanged_MonitoringWasAdded() {
    policyMonitorScheduler.productLicenseChanged();

    ArgumentCaptor<LocalTime> startTimeCaptor = ArgumentCaptor.forClass(LocalTime.class);
    verify(taskSchedulerMock).scheduleDailyTask(any(PolicyMonitoringTask.class),
        startTimeCaptor.capture());

    LocalTime policyMonitoringHour = LocalTime.of(configuration.getPolicyMonitoringHour(), 0);
    assertThat(startTimeCaptor.getValue()).isBetween(LocalTime.of(configuration.getPolicyMonitoringHour(), 0),
        policyMonitoringHour.plusMinutes(120));
  }

  @Test
  public void testProductLicenseChanged_MonitoringWasRemoved() {
    policyMonitorScheduler.productLicenseChanged();

    verify(taskSchedulerMock).unscheduleTask(any(PolicyMonitoringTask.class));
  }

  @Test
  public void testExecute_DropwizardTask() {
    policyMonitorScheduler.execute(null, null);

    verify(taskSchedulerMock).triggerTaskNow(any(PolicyMonitoringTask.class), eq(null));
  }
}
