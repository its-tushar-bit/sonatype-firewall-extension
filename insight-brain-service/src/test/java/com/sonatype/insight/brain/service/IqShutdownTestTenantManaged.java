/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.tenancy.TenantManaged;

import org.quartz.Scheduler;

/**
 * Test-only TenantManaged bean used by IqShutdownTest to verify that TenantManaged beans are deregistered
 * before the TaskScheduler is shutdown.
 */
@Named
@Singleton
public class IqShutdownTestTenantManaged
    implements TenantManaged
{
  @Inject
  private TaskScheduler taskScheduler;

  private Scheduler schedulerDuringDeregistration;

  @Override
  public void deregister() {
    schedulerDuringDeregistration = taskScheduler.getScheduler();
  }

  public Scheduler getSchedulerDuringDeregistration() {
    return schedulerDuringDeregistration;
  }
}
