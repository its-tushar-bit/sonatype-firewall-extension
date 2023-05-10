/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.Duration;

import com.sonatype.insight.brain.scheduler.TaskScheduler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestPollingTaskTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private PullRequestPollingService pullRequestPollingService;

  @Mock
  private IqForScmLicenseChecker licenseChecker;

  public PullRequestPollingTaskTest() {
    super(PullRequestPollingTask.class);
  }

  @Test
  public void testPullRequestPollingTask_licensed() {
    PullRequestPollingTask task =
        new PullRequestPollingTask(taskSchedulerMock, pullRequestPollingService, licenseChecker, 2, 1);
    when(licenseChecker.isPullRequestCommentingSupported()).thenReturn(true);

    task.productLicenseChanged();

    assertThatLogMessagesEqual(
        info("Pull Request Monitoring is licensed"),
        info("Scheduled monitoring of SCM pull requests every 1 second(s) starting in 2 second(s)")
    );

    task.deregister();
  }

  @Test
  public void testPullRequestPollingTask_unlicensed() {
    PullRequestPollingTask task =
        new PullRequestPollingTask(taskSchedulerMock, pullRequestPollingService, licenseChecker, 2, 1);

    task.productLicenseChanged();

    assertThatLogMessagesEqual(
        info("Pull Request Monitoring is not licensed"),
        info("Stopped SCM pull request monitoring")
    );

    task.deregister();
  }

  @Test
  public void testPullRequestPollingTask_register_deregister() {
    PullRequestPollingTask task =
        new PullRequestPollingTask(taskSchedulerMock, pullRequestPollingService, licenseChecker, 2, 1);
    when(licenseChecker.isPullRequestCommentingSupported()).thenReturn(true);

    task.register();

    verify(taskSchedulerMock).schedulePeriodicTask(eq(task), eq(Duration.ofSeconds(1)), any());

    task.deregister();

    verify(taskSchedulerMock).unscheduleTask(eq(task));
  }
}
