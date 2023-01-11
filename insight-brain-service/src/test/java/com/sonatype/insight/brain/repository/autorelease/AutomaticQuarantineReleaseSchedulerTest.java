/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.autorelease;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;

import javax.inject.Inject;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class AutomaticQuarantineReleaseSchedulerTest
    extends AbstractComponentTest
{
  @Inject
  private AutomaticQuarantineReleaseScheduler automaticQuarantineReleaseScheduler;

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

  private void enableLicenseForFirewall(boolean enable) {
    lenient().when(productLicenseMock.hasFeature(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE)).thenReturn(enable);
    lenient().when(productLicenseMock.hasFeature(LicensedFeature.RELEASE_INTEGRITY)).thenReturn(enable);
  }

  @Test
  public void testStartServer_Unlicensed() {
    enableLicenseForFirewall(false);
    automaticQuarantineReleaseScheduler.start();
    verifyNoInteractions(taskSchedulerMock);
  }

  @Test
  public void testStartServer_Licensed() {
    enableLicenseForFirewall(true);
    String beforeSchedulingTime =
        LocalDateTime.now().plusMinutes(configuration.getAutomaticQuarantineReleaseTimeIntervalInMinutes()).toString();
    automaticQuarantineReleaseScheduler.start();

    ArgumentCaptor<Date> startTimeCaptor = ArgumentCaptor.forClass(Date.class);
    ArgumentCaptor<Duration> timeIntervalCaptor = ArgumentCaptor.forClass(Duration.class);
    verify(taskSchedulerMock).schedulePeriodicTask(eq(AutomaticQuarantineReleaseTask.class),
        eq(AutomaticQuarantineReleaseTask.NAME),
        timeIntervalCaptor.capture(), startTimeCaptor.capture());
    assertThat(startTimeCaptor.getValue()).isBetween(beforeSchedulingTime,
        LocalDateTime.now().plusMinutes(configuration.getAutomaticQuarantineReleaseTimeIntervalInMinutes()).toString());
    assertThat(timeIntervalCaptor.getValue()).isEqualTo(
        Duration.ofMinutes(configuration.getAutomaticQuarantineReleaseTimeIntervalInMinutes()));
  }

  @Test
  public void testProductLicenseChanged_AutomaticQuarantineReleaseWasAdded() {
    enableLicenseForFirewall(true);
    String beforeSchedulingTime =
        LocalDateTime.now().plusMinutes(configuration.getAutomaticQuarantineReleaseTimeIntervalInMinutes()).toString();
    automaticQuarantineReleaseScheduler.productLicenseChanged();

    ArgumentCaptor<Date> startTimeCaptor = ArgumentCaptor.forClass(Date.class);
    ArgumentCaptor<Duration> timeIntervalCaptor = ArgumentCaptor.forClass(Duration.class);
    verify(taskSchedulerMock).schedulePeriodicTask(eq(AutomaticQuarantineReleaseTask.class),
        eq(AutomaticQuarantineReleaseTask.NAME),
        timeIntervalCaptor.capture(), startTimeCaptor.capture());
    assertThat(startTimeCaptor.getValue()).isBetween(beforeSchedulingTime,
        LocalDateTime.now().plusMinutes(configuration.getAutomaticQuarantineReleaseTimeIntervalInMinutes()).toString());
    assertThat(timeIntervalCaptor.getValue()).isEqualTo(
        Duration.ofMinutes(configuration.getAutomaticQuarantineReleaseTimeIntervalInMinutes()));
  }

  @Test
  public void testProductLicenseChanged_AutomaticQuarantineReleaseWasRemoved() {
    automaticQuarantineReleaseScheduler.productLicenseChanged();
    verify(taskSchedulerMock).unscheduleTask(AutomaticQuarantineReleaseTask.NAME);
  }
}
