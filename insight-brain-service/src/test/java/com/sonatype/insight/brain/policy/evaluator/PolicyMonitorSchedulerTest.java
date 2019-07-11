/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.sonatype.insight.brain.features.LicensedFeature;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PolicyMonitorSchedulerTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyMonitorScheduler scheduler;

  @Mock
  private ScheduledExecutorService executor;

  @Mock
  private ProductLicense productLicense;

  @Mock
  private CLMLicenseManager licenseManager;

  @Override
  public void configure(Binder binder) {
    binder.bind(ProductLicense.class).toInstance(productLicense);
    binder.bind(CLMLicenseManager.class).toInstance(licenseManager);
    super.configure(binder);
  }

  @Before
  public void init() {
    scheduler = spy(scheduler);
    lenient().doReturn(executor).when(scheduler).newExecutor();
  }

  @Test
  public void testLicenseListenerAddition() {
    verify(licenseManager).addListener(isA(PolicyMonitorScheduler.class));
  }

  @Test
  public void testStartServer_PolicyMonitoringUnlicensed() {
    when(productLicense.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(false);
    scheduler.start();
    verifyZeroInteractions(executor);
  }

  @Test
  public void testStartServer_PolicyMonitoringLicensed() {
    when(productLicense.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(true);
    scheduler.start();
    verify(executor).scheduleAtFixedRate(any(Runnable.class), anyLong(), eq(TimeUnit.DAYS.toMillis(1)),
        eq(TimeUnit.MILLISECONDS));
  }

  @Test
  public void testStopServer() {
    when(productLicense.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(true);
    scheduler.start();
    scheduler.stop();
    verify(executor).shutdown();
  }

  @Test
  public void testLicenseChanged_MonitoringWasAdded() {
    when(productLicense.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(false);
    scheduler.start();
    when(productLicense.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(true);
    scheduler.licenseChanged();
    verify(executor).scheduleAtFixedRate(any(Runnable.class), anyLong(), eq(TimeUnit.DAYS.toMillis(1)),
        eq(TimeUnit.MILLISECONDS));
  }

  @Test
  public void testLicenseChanged_MonitoringWasRemoved() {
    when(productLicense.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(true);
    scheduler.start();
    when(productLicense.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(false);
    scheduler.licenseChanged();
    verify(executor).shutdown();
  }

  @Test
  public void testLicenseChanged_MonitoringStillAvailable() {
    when(productLicense.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(true);
    scheduler.start();
    reset(executor);
    scheduler.licenseChanged();
    verifyZeroInteractions(executor);
  }

  @Test
  public void testLicenseChanged_MonitoringStillUnavailable() {
    when(productLicense.hasFeature(LicensedFeature.POLICY_MONITORING)).thenReturn(false);
    scheduler.start();
    reset(executor);
    scheduler.licenseChanged();
    verifyZeroInteractions(executor);
  }
}
