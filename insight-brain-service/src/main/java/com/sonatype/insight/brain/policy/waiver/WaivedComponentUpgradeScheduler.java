/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.waiver;

import java.time.LocalTime;
import java.util.Objects;
import java.util.Random;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
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
public class WaivedComponentUpgradeScheduler
    implements TenantManaged, WaivedComponentUpgradeListener
{
  private static final Logger log = LoggerFactory.getLogger(WaivedComponentUpgradeScheduler.class);

  private final Configuration configuration;

  private final TaskScheduler taskScheduler;

  private final OrganizationDAO organizationDAO;

  private final WaivedComponentUpgradeTask waivedComponentUpgradeTask;

  // Variable used during tests. Expected to be public
  public boolean disableForTesting;

  // Random to distribute load for HDS. Does not need to be a secure random
  private final Random random = new Random();

  @Inject
  public WaivedComponentUpgradeScheduler(
      Configuration configuration,
      TaskScheduler taskScheduler,
      OrganizationDAO organizationDAO,
      WaivedComponentUpgradeTask waivedComponentUpgradeTask)
  {
    this.configuration = configuration;
    this.taskScheduler = taskScheduler;
    this.organizationDAO = organizationDAO;
    this.waivedComponentUpgradeTask = waivedComponentUpgradeTask;
  }

  public void scheduleWaivedComponentUpgradeInspection() {
    if (taskCanBeScheduled()) {
      // randomize start time to minimize potential concurrent load on queries to HDS made by the process
      final int randomizedStartMinuteAfterConfiguredHour = random.nextInt(180 /* up to 3 hours */);
      LocalTime startTime = LocalTime.of(configuration.getWaivedComponentUpgradeInspectionHour(), 0)
          .plusMinutes(randomizedStartMinuteAfterConfiguredHour);
      taskScheduler.scheduleDailyTask(waivedComponentUpgradeTask, startTime);
      log.info("Next waived component upgrade inspection execution scheduled for {}",
          taskScheduler.getNextExecutionTime(waivedComponentUpgradeTask));
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
    if (!disableForTesting && taskScheduler.isTaskScheduled(waivedComponentUpgradeTask)) {
      taskScheduler.unscheduleTask(waivedComponentUpgradeTask);
    }
  }

  private boolean taskCanBeScheduled() {
    return !disableForTesting && configuration.getWaivedComponentUpgradeInspectionHour() != null && isStageConfigured();
  }

  private boolean isStageConfigured() {
    Organization rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    return rootOrganization != null && rootOrganization.getWaivedComponentUpgradeStageTypeId() != null;
  }

  @Override
  public void waivedComponentUpgradeNotificationStageUpdated(final String newStage) {
    if (Objects.isNull(newStage)) {
      log.info("Stopping waived component upgrade scheduler as the stage is now set to null.");
      deregister();
      return;
    }

    log.info("Restarting or rescheduling waived component upgrade scheduler for stage {}.", newStage);
    scheduleWaivedComponentUpgradeInspection();
  }
}
