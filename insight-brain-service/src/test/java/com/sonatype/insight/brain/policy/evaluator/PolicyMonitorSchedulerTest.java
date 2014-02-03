/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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

  private ScheduledExecutorService executor;

  private CLMLicenseManager licenseManager;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    licenseManager = mock(CLMLicenseManager.class);
    binder.bind(CLMLicenseManager.class).toInstance(licenseManager);
  }

  @Before
  public void init() {
    scheduler = spy(scheduler);
    executor = mock(ScheduledExecutorService.class);
    doReturn(executor).when(scheduler).newExecutor();
  }

  @Test
  public void testLicenseListenerAddition() {
    verify(licenseManager).addListener(isA(PolicyMonitorScheduler.class));
  }

  @Test
  public void testStartServer_PolicyMonitoringUnlicensed() {
    when(licenseManager.hasPolicyMonitoring()).thenReturn(false);
    scheduler.start();
    verifyZeroInteractions(executor);
  }

  @Test
  public void testStartServer_PolicyMonitoringLicensed() {
    when(licenseManager.hasPolicyMonitoring()).thenReturn(true);
    scheduler.start();
    verify(executor).scheduleAtFixedRate(any(Runnable.class), anyLong(), eq(TimeUnit.DAYS.toMillis(1)),
        eq(TimeUnit.MILLISECONDS));
  }

  @Test
  public void testStopServer() {
    when(licenseManager.hasPolicyMonitoring()).thenReturn(true);
    scheduler.start();
    scheduler.stop();
    verify(executor).shutdown();
  }

  @Test
  public void testLicenseChanged_MonitoringWasAdded() {
    when(licenseManager.hasPolicyMonitoring()).thenReturn(false);
    scheduler.start();
    when(licenseManager.hasPolicyMonitoring()).thenReturn(true);
    scheduler.licenseChanged();
    verify(executor).scheduleAtFixedRate(any(Runnable.class), anyLong(), eq(TimeUnit.DAYS.toMillis(1)),
        eq(TimeUnit.MILLISECONDS));
  }

  @Test
  public void testLicenseChanged_MonitoringWasRemoved() {
    when(licenseManager.hasPolicyMonitoring()).thenReturn(true);
    scheduler.start();
    when(licenseManager.hasPolicyMonitoring()).thenReturn(false);
    scheduler.licenseChanged();
    verify(executor).shutdown();
  }

  @Test
  public void testLicenseChanged_MonitoringStillAvailable() {
    when(licenseManager.hasPolicyMonitoring()).thenReturn(true);
    scheduler.start();
    reset(executor);
    scheduler.licenseChanged();
    verifyZeroInteractions(executor);
  }

  @Test
  public void testLicenseChanged_MonitoringStillUnavailable() {
    when(licenseManager.hasPolicyMonitoring()).thenReturn(false);
    scheduler.start();
    reset(executor);
    scheduler.licenseChanged();
    verifyZeroInteractions(executor);
  }
}
