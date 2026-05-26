/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.scheduler.MultiTenantTaskScheduler;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.quartz.Scheduler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@Category(SlowTest.class)
public class MtiqShutdownTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  /**
   * Test configuration that registers TempTenantManaged as a TenantManaged bean.
   */
  @TestConfiguration
  static class MtiqShutdownTestConfig
  {
    @Bean
    TempTenantManaged tempTenantManaged() {
      return new TempTenantManaged();
    }
  }

  /**
   * Tests against https://issues.sonatype.org/browse/CLM-24625. This bug was caused by the TaskScheduler being shutdown
   * before the TenantManaged beans are deregistered (most of which are quartz jobs). This means that any beans that
   * perform tidy-up of quartz jobs during de-registration would fail causing the server to not shutdown gracefully and
   * for old quartz configuration to be left behind.
   * This is checking that MultiTenantManagedInitializer#stop is called before MultiTenantTaskScheduler#stop
   */
  @Test
  public void taskSchedulerShouldBeShutdownAfterTenantManagedBeansAreDeregistered() throws Exception {
    MultiTenantTaskScheduler taskScheduler = testCLMServer.getCLMServer().getInstance(MultiTenantTaskScheduler.class);
    taskScheduler.disableForTesting = false;
    taskScheduler.start();

    assertThat(taskScheduler.getScheduler()).isNotNull();

    TempTenantManaged tenantManaged = getCLMServer()
        .getApplicationContext()
        .getBean("tempTenantManaged", TempTenantManaged.class);

    getCLMServer().getInstance(TenantManagedInitializer.class).stop();

    assertThat(tenantManaged.schedulerDuringDeregistration).isNotNull();

    stopClmServer();
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
