/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.waiver;

import java.time.LocalTime;

import com.sonatype.insight.brain.git.VerifiableLoggingTestBase;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.Configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WaivedComponentUpgradeSchedulerTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private Configuration configuration;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private WaivedComponentUpgradeTask waivedComponentUpgradeTask;

  private WaivedComponentUpgradeScheduler scheduler;

  public WaivedComponentUpgradeSchedulerTest() {
    super(WaivedComponentUpgradeScheduler.class);
  }

  @BeforeEach
  public void before() {
    scheduler = new WaivedComponentUpgradeScheduler(configuration, taskSchedulerMock, waivedComponentUpgradeTask);
  }

  @Test
  public void testSchedulerCanScheduleTask() {
    scheduler.disableForTesting = false;
    when(configuration.getWaivedComponentUpgradeMonitoringEnabled()).thenReturn(true);
    when(configuration.getWaivedComponentUpgradeInspectionHour()).thenReturn(1);

    scheduler.scheduleWaivedComponentUpgradeInspection();

    verify(taskSchedulerMock).scheduleDailyTask(eq(waivedComponentUpgradeTask), any(LocalTime.class));
  }

  @Test
  public void testSchedulerCannotScheduleTask_MonitoringIsDisabled() {
    scheduler.disableForTesting = false;
    when(configuration.getWaivedComponentUpgradeMonitoringEnabled()).thenReturn(false);

    scheduler.scheduleWaivedComponentUpgradeInspection();

    assertThatLogMessagesContain(info("Waived component upgrade task not configured"));
  }

  @Test
  public void testSchedulerCannotScheduleTask_missingHourConfiguration() {
    scheduler.disableForTesting = false;
    when(configuration.getWaivedComponentUpgradeMonitoringEnabled()).thenReturn(true);
    when(configuration.getWaivedComponentUpgradeInspectionHour()).thenReturn(null);

    scheduler.scheduleWaivedComponentUpgradeInspection();

    assertThatLogMessagesContain(info("Waived component upgrade task not configured"));
  }

  @Test
  public void testSchedulerCannotScheduleTask_disabledForTesting() {
    scheduler.disableForTesting = true;

    scheduler.scheduleWaivedComponentUpgradeInspection();

    assertThatLogMessagesContain(info("Waived component upgrade task not configured"));
  }
}
