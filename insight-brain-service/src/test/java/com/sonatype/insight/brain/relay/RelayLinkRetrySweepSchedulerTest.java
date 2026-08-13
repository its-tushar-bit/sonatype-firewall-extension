/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.time.Duration;

import com.sonatype.insight.brain.scheduler.TaskScheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class RelayLinkRetrySweepSchedulerTest
{
  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private RelayLinkRetrySweepTask sweepTask;

  private RelayLinkRetrySweepScheduler scheduler;

  @BeforeEach
  public void before() {
    scheduler = new RelayLinkRetrySweepScheduler(taskScheduler, sweepTask);
  }

  @Test
  public void register_schedulesPeriodicTaskHourly() {
    scheduler.disableForTesting = false;

    scheduler.register();

    verify(taskScheduler).schedulePeriodicTask(sweepTask, Duration.ofHours(1));
  }

  @Test
  public void register_disabledForTesting_doesNotSchedule() {
    scheduler.disableForTesting = true;

    scheduler.register();

    verifyNoInteractions(taskScheduler);
  }

  @Test
  public void deregister_doesNotUnschedule() {
    scheduler.deregister();

    verifyNoInteractions(taskScheduler);
  }
}
