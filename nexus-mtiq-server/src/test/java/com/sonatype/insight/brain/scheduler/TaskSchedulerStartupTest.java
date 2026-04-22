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

import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.spi.JobFactory;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class TaskSchedulerStartupTest
{
  @Test
  public void testStartupOrder() throws Exception {
    // Note EVERY single MTIQ startup asserts that the tenants are registered BEFORE the task scheduler can start.
    // See TenantManager#tenantsPreRegistered and MultiTenantTaskScheduler#assertTenantsArePreRegistered
    // This test manually runs one before the other to verify

    ShutdownHandler shutdownHandler = Mockito.mock(ShutdownHandler.class);
    // Simulate System.exit() behavior - it never returns, so we throw an exception to stop execution
    doThrow(new RuntimeException("Simulated System.exit(11)")).when(shutdownHandler).exit(11);

    MultiTenantTaskScheduler multiTenantTaskScheduler =
        new MultiTenantTaskScheduler(Mockito.mock(MultiTenantQuartzJobStoreTX.class),
            Mockito.mock(MultiTenantBatchModeJobStoreTX.class), Mockito.mock(JobFactory.class), "schedulerName",
            Mockito.mock(QuartzTriggerListener.class), Mockito.mock(QuartzConcurrencyListener.class),
            () -> Mockito.mock(TenantContextJobListener.class),
            Mockito.mock(SystemConfigurationPropertyDAO.class), () -> Mockito.mock(TenantManager.class),
            Mockito.mock(TenantUtil.class), shutdownHandler,
            Mockito.mock(QuartzJobSchedulingService.class));

    assertThatThrownBy(() -> multiTenantTaskScheduler.start())
        .hasMessage("Simulated System.exit(11)");

    verify(shutdownHandler).exit(11);
  }
}
