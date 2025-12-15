/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantContextJobListener;
import com.sonatype.insight.brain.tenancy.TenantManager;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.mockito.Mockito;
import org.quartz.spi.JobFactory;

public class TaskSchedulerStartupTest
{
  @Rule
  public final ExpectedSystemExit exit = ExpectedSystemExit.none();

  @Test
  public void testStartupOrder() throws Exception {
    // Note EVERY single MTIQ startup asserts that the tenants are registered BEFORE the task scheduler can start.
    // See TenantManager#tenantsPreRegistered and MultiTenantTaskScheduler#assertTenantsArePreRegistered
    // This test manually runs one before the other to verify

    MultiTenantTaskScheduler multiTenantTaskScheduler =
        new MultiTenantTaskScheduler(Mockito.mock(MultiTenantQuartzJobStoreTX.class),
            Mockito.mock(MultiTenantBatchModeJobStoreTX.class), Mockito.mock(JobFactory.class), "schedulerName",
            Mockito.mock(QuartzTriggerListener.class), () -> Mockito.mock(TenantContextJobListener.class),
            Mockito.mock(SystemConfigurationPropertyDAO.class), () -> Mockito.mock(TenantManager.class),
            Mockito.mock(TenantUtil.class), Mockito.mock(ShutdownHandler.class),
            Mockito.mock(QuartzJobSchedulingService.class));

    exit.expectSystemExitWithStatus(11);
    multiTenantTaskScheduler.start();
  }
}
