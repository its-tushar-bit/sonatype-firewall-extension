/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sonatype.insight.brain.policy.PolicyMonitoringTask;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.license.model.LicensedFeature;
import jakarta.inject.Inject;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@ComponentH2Test
public class PolicyMonitorSchedulerTest
    extends AbstractComponentH2Test
{
  @Inject
  private PolicyMonitorScheduler policyMonitorScheduler;

  @Inject
  private Configuration configuration;

  @Mock
  private ProductLicense productLicenseMock;

  @Mock
  private TaskScheduler taskSchedulerMock;

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
  public void testExecute_AdminTask() throws Exception {
    policyMonitorScheduler.execute(null, new PrintWriter(OutputStream.nullOutputStream()));

    verify(taskSchedulerMock).triggerTaskNow(any(PolicyMonitoringTask.class), eq(null));
  }
}
