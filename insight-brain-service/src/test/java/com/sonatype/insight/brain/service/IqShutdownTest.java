/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

import com.google.inject.Inject;
import org.junit.After;
import org.junit.Test;
import org.quartz.Scheduler;

import static org.assertj.core.api.Assertions.assertThat;

public class IqShutdownTest
    extends AbstractBrainServiceIntegrationTest
{
  /**
   * Tests against https://issues.sonatype.org/browse/CLM-24625. This bug was caused by the TaskScheduler being shutdown
   * before the TenantManaged beans are deregistered (most of which are quartz jobs). This means that any beans that
   * perform tidy-up of quartz jobs during de-registration would fail causing the server to not shutdown gracefully and
   * for old quartz configuration to be left behind.
   * This is checking that DefaultTenantManagedInitializer#stop is called before TaskScheduler#stop
   */
  @Test
  public void taskSchedulerShouldBeShutdownAfterTenantManagedBeansAreDeregistered() throws Exception {
    TaskScheduler taskScheduler = testCLMServer.getCLMServer().getInstance(TaskScheduler.class);
    taskScheduler.disableForTesting = false;
    taskScheduler.start();

    assertThat(taskScheduler.getScheduler()).isNotNull();

    TempTenantManaged tenantManaged = getCLMServer().getInstance(TempTenantManaged.class);

    getCLMServer().stop();

    assertThat(tenantManaged.schedulerDuringDeregistration).isNotNull();
  }

  @After
  public void restartClmServer() throws Exception {
    if (!getCLMServer().isRunning()) {
      getCLMServer().start();
    }
  }

  @Named
  @Singleton
  public static class TempTenantManaged
      implements TenantManaged
  {
    @Inject
    private TaskScheduler taskScheduler;

    private Scheduler schedulerDuringDeregistration;

    @Override
    public void deregister() {
      schedulerDuringDeregistration = taskScheduler.getScheduler();
    }
  }
}
