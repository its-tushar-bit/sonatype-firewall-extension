/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.firewall.metrics;

import java.time.LocalTime;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DeleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJobTest
    extends AbstractComponentTest
{
  @Inject
  private DeleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob
      deleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Mock
  private FirewallMetricsDAO mockFirewallMetricsDAO;

  @Captor
  private ArgumentCaptor<LocalTime> timeCaptor;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    binder.bind(FirewallMetricsDAO.class).toInstance(mockFirewallMetricsDAO);
    super.configure(binder);
  }

  @Test
  public void testStart() {
    deleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob.register();

    verify(mockTaskScheduler).scheduleDailyTask(
        eq(deleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob), timeCaptor.capture());
    assertThat(timeCaptor.getValue().getHour()).isEqualTo(1);
    assertThat(timeCaptor.getValue().getMinute()).isBetween(30, 59); // (inclusive, inclusive)
  }

  @Test
  public void testExecute() {
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(mockFirewallMetricsDAO).deleteRecordsOlderThanOneYear(
        FirewallMetricsName.SAFE_VERSIONS_SELECTED_AUTOMATICALLY);

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      deleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob.execute(mock(JobExecutionContext.class));
    }

    verify(mockFirewallMetricsDAO).deleteRecordsOlderThanOneYear(
        FirewallMetricsName.SAFE_VERSIONS_SELECTED_AUTOMATICALLY);
  }
}
