/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.organization;

import java.time.LocalTime;
import javax.inject.Inject;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class ApplicationCountHistoryKeeperTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationCountHistoryKeeper applicationCountHistoryKeeper;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Captor
  private ArgumentCaptor<LocalTime> timeCaptor;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    super.configure(binder);
  }

  @Test
  public void testStart() {
    applicationCountHistoryKeeper.register();

    verify(mockTaskScheduler).scheduleDailyTask(eq(applicationCountHistoryKeeper), timeCaptor.capture());
    assertThat(timeCaptor.getValue().getHour()).isEqualTo(1);
    assertThat(timeCaptor.getValue().getMinute()).isBetween(30, 59); // (inclusive, inclusive)
  }
}
