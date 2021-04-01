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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

  @Test
  public void testStartServer_PolicyMonitoringUnlicensed() {
    when(productLicenseMock.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(false);

    policyMonitorScheduler.start();

    verifyNoInteractions(taskSchedulerMock);
  }

  @Test
  public void testStartServer_PolicyMonitoringLicensed() {
    when(productLicenseMock.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(true);

    policyMonitorScheduler.start();

    ArgumentCaptor<LocalTime> startTimeCaptor = ArgumentCaptor.forClass(LocalTime.class);
    verify(taskSchedulerMock).scheduleDailyTask(eq(PolicyMonitoringTask.class), eq(PolicyMonitoringTask.NAME),
        startTimeCaptor.capture());
    assertThat(startTimeCaptor.getValue()).isBetween(LocalTime.of(insightConfig.getPolicyMonitoringHour(), 0),
        LocalTime.of(insightConfig.getPolicyMonitoringHour(), 15));
  }

  @Test
  public void testProductLicenseChanged_MonitoringWasAdded() {
    when(productLicenseMock.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(true);

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
