/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.quartz.Scheduler;
import org.quartz.spi.JobFactory;

@Named
@Singleton
public class TestTaskScheduler
    extends TaskScheduler
{
  @Inject
  public TestTaskScheduler(
      QuartzJobStoreTX quartzJobStoreTX,
      JobFactory jobFactory,
      @Named("${scheduler.name:-" + DEFAULT_SCHEDULER_NAME + "}") String schedulerName,
      QuartzTriggerListener quartzTriggerListener)
  {
    super(quartzJobStoreTX, jobFactory, schedulerName, quartzTriggerListener);
  }

  @Override
  public void stop() {
    Scheduler scheduler = getScheduler();
    if (scheduler != null) {
      try {
        scheduler.clear();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      try {
        scheduler.shutdown(false);
        waitForShutdown();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void waitForShutdown() {
    long start = System.currentTimeMillis();
    while (getScheduler() != null && (System.currentTimeMillis() - start) < 10000) {
      Thread.yield();
    }
  }
}
