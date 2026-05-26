/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.organization;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DEVELOPMENT_DASHBOARD_METRIC_COLLECTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import jakarta.inject.Inject;
import java.time.LocalTime;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class ApplicationCountHistoryKeeperTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationCountHistoryKeeper applicationCountHistoryKeeper;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Captor
  private ArgumentCaptor<LocalTime> timeCaptor;

  @Test
  public void testApplicationCountHistoryKeeper__shouldRegisterCountHistoryTaskWhenFeatureEnabled() {
    applicationCountHistoryKeeper.register();

    verify(mockTaskScheduler).scheduleDailyTask(eq(applicationCountHistoryKeeper), timeCaptor.capture());
    assertThat(timeCaptor.getValue().getHour()).isEqualTo(1);
    assertThat(timeCaptor.getValue().getMinute()).isBetween(30, 59); // (inclusive, inclusive)
  }

  @Test
  public void testApplicationCountHistoryKeeper__shouldNotRegisterCountHistoryTaskWhenFeatureDisabled() {
    tempEntity.newSystemConfigurationProperty(DEVELOPMENT_DASHBOARD_METRIC_COLLECTION, "false");
    applicationCountHistoryKeeper.register();

    verify(mockTaskScheduler, never()).scheduleDailyTask(any(), any());
  }
}
