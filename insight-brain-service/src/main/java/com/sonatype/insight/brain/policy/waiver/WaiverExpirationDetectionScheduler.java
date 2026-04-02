/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.waiver;

import java.time.LocalTime;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.tenancy.TenantManaged;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduler for waiver expiration detection task.
 * Schedules the task to run daily at 2:00 AM to detect expired waivers and trigger webhook events.
 *
 * @since 1.178.0
 */
@Named
@Singleton
public class WaiverExpirationDetectionScheduler
    implements TenantManaged
{
  private static final Logger log = LoggerFactory.getLogger(WaiverExpirationDetectionScheduler.class);

  private static final int DETECTION_HOUR = 2; // 2 AM

  private static final int DETECTION_MINUTE = 0;

  private final TaskScheduler taskScheduler;

  private final WaiverExpirationDetectionTask waiverExpirationDetectionTask;

  // Package-private field for test suppression. Mutable singleton fields can cause issues in MTIQ.
  boolean disableForTesting;

  @Inject
  public WaiverExpirationDetectionScheduler(
      TaskScheduler taskScheduler,
      WaiverExpirationDetectionTask waiverExpirationDetectionTask)
  {
    this.taskScheduler = taskScheduler;
    this.waiverExpirationDetectionTask = waiverExpirationDetectionTask;
  }

  public void scheduleWaiverExpirationDetection() {
    if (!disableForTesting) {
      log.info("Scheduling waiver expiration detection to run daily at {}:{}",
          DETECTION_HOUR, String.format("%02d", DETECTION_MINUTE));
      LocalTime startTime = LocalTime.of(DETECTION_HOUR, DETECTION_MINUTE);
      taskScheduler.scheduleDailyTask(waiverExpirationDetectionTask, startTime);
    }
    else {
      log.info("Waiver expiration detection task disabled for testing");
    }
  }

  @Override
  public void register() {
    scheduleWaiverExpirationDetection();
  }

  @Override
  public void deregister() {
    // Do not unschedule task otherwise it will break MTIQ - SDEV-1312
  }
}
