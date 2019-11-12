/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PolicyMonitorSchedulerTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyMonitorScheduler schedulerSpy;

  @Mock
  private ScheduledExecutorService executorMock;

  @Mock
  private ProductLicense productLicenseMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(ProductLicense.class).toInstance(productLicenseMock);
    super.configure(binder);
  }

  @Before
  public void init() {
    schedulerSpy = spy(schedulerSpy);
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
    verify(executorMock).scheduleAtFixedRate(any(Runnable.class), anyLong(), eq(TimeUnit.DAYS.toMillis(1)),
        eq(TimeUnit.MILLISECONDS));
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
    verify(executorMock).scheduleAtFixedRate(any(Runnable.class), anyLong(), eq(TimeUnit.DAYS.toMillis(1)),
        eq(TimeUnit.MILLISECONDS));
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
}
