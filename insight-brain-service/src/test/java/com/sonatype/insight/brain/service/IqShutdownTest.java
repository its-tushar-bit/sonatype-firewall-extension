/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class IqShutdownTest
    extends AbstractBrainServiceIntegrationTest
{
  @Override
  protected List<Module> getBrainModules() {
    List<Module> modules = new ArrayList<>(super.getBrainModules());

    // Add a module that registers our test TenantManaged bean using Multibinder
    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        // Explicit binding required for IqShutdownTestTenantManaged
        bind(IqShutdownTestTenantManaged.class);

        // Use Multibinder to add our test TenantManaged bean to the set
        Multibinder<TenantManaged> tenantManagedBinder = Multibinder.newSetBinder(binder(), TenantManaged.class);
        tenantManagedBinder.addBinding().to(IqShutdownTestTenantManaged.class);
      }
    });

    return modules;
  }

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

    IqShutdownTestTenantManaged tenantManaged = getCLMServer().getInstance(IqShutdownTestTenantManaged.class);

    getCLMServer().stop();

    assertThat(tenantManaged.getSchedulerDuringDeregistration()).isNotNull();
  }

  @After
  public void restartClmServer() throws Exception {
    if (!getCLMServer().isRunning()) {
      getCLMServer().start();
    }
  }
}
