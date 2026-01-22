/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.firewall.metrics;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FirewallMetricsComponentsAutoReleasedConsolidatorCronJobTest
    extends AbstractComponentTest
{
  @Inject
  private FirewallMetricsComponentsAutoReleasedConsolidatorCronJob
      firewallMetricsComponentsAutoReleasedConsolidatorCronJob;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private ComponentsAutoReleasedMetricsConsolidator componentsAutoReleasedMetricsConsolidatorMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    binder.bind(ComponentsAutoReleasedMetricsConsolidator.class).toInstance(
        componentsAutoReleasedMetricsConsolidatorMock);
    super.configure(binder);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(FirewallMetricsComponentsAutoReleasedConsolidatorCronJob.class).build()
      .isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testStart() {
    firewallMetricsComponentsAutoReleasedConsolidatorCronJob.register();

    verify(taskSchedulerMock).schedulePeriodicTask(firewallMetricsComponentsAutoReleasedConsolidatorCronJob,
        FirewallMetricsComponentsAutoReleasedConsolidatorCronJob.PERIOD);
  }

  @Test
  public void testExecute() {
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(componentsAutoReleasedMetricsConsolidatorMock).consolidate();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      firewallMetricsComponentsAutoReleasedConsolidatorCronJob.execute(mock(JobExecutionContext.class));
    }

    verify(componentsAutoReleasedMetricsConsolidatorMock).consolidate();
  }
}
