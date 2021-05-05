/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.Duration;

import javax.inject.Inject;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class PullRequestDetailsUpdaterTest
    extends AbstractComponentTest
{
  @Inject
  private PullRequestDetailsUpdater pullRequestDetailsUpdater;

  @Inject
  private InsightConfig insightConfig;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    super.configure(binder);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(PullRequestDetailsUpdater.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testStart() throws Exception {
    pullRequestDetailsUpdater.start();

    verify(taskSchedulerMock).schedulePeriodicTask(PullRequestDetailsUpdater.class, PullRequestDetailsUpdater.TASK_NAME,
        Duration.ofSeconds(insightConfig.getPullRequestDetailsUpdateIntervalInSeconds()));
  }

  @Test
  public void testExecute() {
    PullRequestDetailsUpdater pullRequestDetailsUpdaterSpy = spy(pullRequestDetailsUpdater);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(pullRequestDetailsUpdaterSpy).updatePullRequestDetails();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      pullRequestDetailsUpdaterSpy.execute(mock(JobExecutionContext.class));
    }

    verify(pullRequestDetailsUpdaterSpy).updatePullRequestDetails();
  }
}
