/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.scheduler.MultiTenantTaskScheduler;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.tenancy.TenantManaged;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import org.junit.Test;
import org.quartz.Scheduler;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class MtiqShutdownTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Override
  protected List<Module> getBrainModules() {
    List<Module> modules = new ArrayList<>(super.getBrainModules());

    // Add a module that registers our test TenantManaged bean using Multibinder
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        // Explicit binding required for TempTenantManaged
        bind(TempTenantManaged.class);

        // Use Multibinder to add our test TenantManaged bean to the set
        Multibinder<TenantManaged> tenantManagedBinder = Multibinder.newSetBinder(binder(), TenantManaged.class);
        tenantManagedBinder.addBinding().to(TempTenantManaged.class);
      }
    });

    return modules;
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

    TempTenantManaged tenantManaged = getCLMServer().getInstance(TempTenantManaged.class);

    stopClmServer();

    assertThat(tenantManaged.schedulerDuringDeregistration).isNotNull();
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
