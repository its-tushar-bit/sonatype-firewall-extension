/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.waiver;

import java.time.LocalTime;
import java.util.Random;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.TenantManaged;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.159
 */
@Named
@Singleton
public class WaivedComponentUpgradeScheduler implements TenantManaged
{
  private static final Logger log = LoggerFactory.getLogger(WaivedComponentUpgradeScheduler.class);

  private final Configuration configuration;

  private final TaskScheduler taskScheduler;

  private final WaivedComponentUpgradeTask waivedComponentUpgradeTask;

  // Variable used during tests. Expected to be public
  public boolean disableForTesting;

  // Random to distribute load for HDS. Does not need to be a secure random
  private final Random random = new Random();

  @Inject
  public WaivedComponentUpgradeScheduler(
      Configuration configuration,
      TaskScheduler taskScheduler,
      WaivedComponentUpgradeTask waivedComponentUpgradeTask)
  {
    this.configuration = configuration;
    this.taskScheduler = taskScheduler;
    this.waivedComponentUpgradeTask = waivedComponentUpgradeTask;
  }

  public void scheduleWaivedComponentUpgradeInspection() {
    if (taskCanBeScheduled()) {
      log.info("Restarting or rescheduling waived component upgrade scheduler.");
      // randomize start time to minimize potential concurrent load on queries to HDS made by the process
      final int randomizedStartMinuteAfterConfiguredHour = random.nextInt(180 /* up to 3 hours */);
      LocalTime startTime = LocalTime.of(configuration.getWaivedComponentUpgradeInspectionHour(), 0)
          .plusMinutes(randomizedStartMinuteAfterConfiguredHour);
      taskScheduler.scheduleDailyTask(waivedComponentUpgradeTask, startTime);
    }
    else {
      log.info("Waived component upgrade task not configured");
    }
  }

  @Override
  public void register() {
    scheduleWaivedComponentUpgradeInspection();
  }

  @Override
  public void deregister() {
    // Do not unschedule task otherwise it will break MTIQ - SDEV-1312
  }

  private boolean taskCanBeScheduled() {
    return !disableForTesting && configuration.getWaivedComponentUpgradeMonitoringEnabled() &&
        configuration.getWaivedComponentUpgradeInspectionHour() != null;
  }
}
