/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.githubapp;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.sonatype.insight.brain.scheduler.TaskScheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubAppCleanupSchedulerTest
{
  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private GitHubAppCleanupTask gitHubAppCleanupTask;

  private GitHubAppCleanupScheduler scheduler;

  @Before
  public void setUp() {
    scheduler = new GitHubAppCleanupScheduler(taskSchedulerMock, gitHubAppCleanupTask);
  }

  @Test
  public void register_SchedulesWeeklyTask() {
    scheduler.disableForTesting = false;

    scheduler.register();

    verify(taskSchedulerMock).scheduleWeeklyTask(eq(gitHubAppCleanupTask), eq(DayOfWeek.SUNDAY),
        eq(LocalTime.of(3, 0)));
  }

  @Test
  public void register_DisabledForTesting_DoesNotSchedule() {
    scheduler.disableForTesting = true;

    scheduler.register();

    verifyNoInteractions(taskSchedulerMock);
  }

  @Test
  public void deregister_DoesNotUnscheduleTask() {
    scheduler.deregister();

    verifyNoInteractions(taskSchedulerMock);
  }
}
