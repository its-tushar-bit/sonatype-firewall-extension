/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.firewall.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import jakarta.inject.Inject;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

public class FirewallMetricsComponentWaivedConsolidatorCronJobTest
    extends AbstractComponentTest
{
  @Inject
  private FirewallMetricsComponentWaivedConsolidatorCronJob firewallMetricsComponentWaivedConsolidatorCronJob;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private WaivedComponentMetricsConsolidator waivedComponentMetricsConsolidatorMock;

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(FirewallMetricsComponentWaivedConsolidatorCronJob.class)
        .build()
        .isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testStart() {
    firewallMetricsComponentWaivedConsolidatorCronJob.register();

    verify(taskSchedulerMock).schedulePeriodicTask(firewallMetricsComponentWaivedConsolidatorCronJob,
        FirewallMetricsComponentWaivedConsolidatorCronJob.PERIOD);
  }

  @Test
  public void testExecute() {
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(waivedComponentMetricsConsolidatorMock).consolidate();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      firewallMetricsComponentWaivedConsolidatorCronJob.execute(mock(JobExecutionContext.class));
    }

    verify(waivedComponentMetricsConsolidatorMock).consolidate();
  }
}
