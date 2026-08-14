/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantContextJobListener;
import com.sonatype.insight.brain.tenancy.TenantManager;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.quartz.spi.JobFactory;

public class TaskSchedulerStartupTest
{
  @Test
  public void afterPropertiesSetShouldBeNoOpUntilAfterSingletonsInstantiated() {
    TenantManager tenantManager = Mockito.mock(TenantManager.class);
    ShutdownHandler shutdownHandler = Mockito.mock(ShutdownHandler.class);

    MultiTenantTaskScheduler multiTenantTaskScheduler =
        new MultiTenantTaskScheduler(Mockito.mock(MultiTenantQuartzJobStoreTX.class),
            Mockito.mock(MultiTenantBatchModeJobStoreTX.class), Mockito.mock(JobFactory.class), "schedulerName",
            Mockito.mock(QuartzTriggerListener.class), Mockito.mock(QuartzConcurrencyListener.class),
            () -> Mockito.mock(TenantContextJobListener.class),
            Mockito.mock(SystemConfigurationPropertyDAO.class), () -> tenantManager,
            Mockito.mock(TenantUtil.class), shutdownHandler,
            Mockito.mock(QuartzJobSchedulingService.class));

    assertThatNoException().isThrownBy(multiTenantTaskScheduler::afterPropertiesSet);

    verifyNoInteractions(tenantManager, shutdownHandler);
  }

  @Test
  public void afterSingletonsInstantiatedShouldPreRegisterTenantsBeforeStartingSchedulers() throws Exception {
    MultiTenantQuartzJobStoreTX quartzJobStoreTX = Mockito.mock(MultiTenantQuartzJobStoreTX.class);
    TenantManager tenantManager = Mockito.mock(TenantManager.class);
    MultiTenantTaskScheduler multiTenantTaskScheduler =
        spy(new MultiTenantTaskScheduler(quartzJobStoreTX,
            Mockito.mock(MultiTenantBatchModeJobStoreTX.class), Mockito.mock(JobFactory.class), "schedulerName",
            Mockito.mock(QuartzTriggerListener.class), Mockito.mock(QuartzConcurrencyListener.class),
            () -> Mockito.mock(TenantContextJobListener.class),
            Mockito.mock(SystemConfigurationPropertyDAO.class), () -> tenantManager,
            Mockito.mock(TenantUtil.class), Mockito.mock(ShutdownHandler.class),
            Mockito.mock(QuartzJobSchedulingService.class)));

    doNothing().when(multiTenantTaskScheduler).startScheduler(anyString(), any(QuartzJobStoreTX.class));

    multiTenantTaskScheduler.afterSingletonsInstantiated();

    InOrder inOrder = Mockito.inOrder(tenantManager, multiTenantTaskScheduler);
    inOrder.verify(tenantManager).ensureTenantsPreRegistered();
    inOrder.verify(multiTenantTaskScheduler).startScheduler("schedulerName", quartzJobStoreTX);
  }
}
