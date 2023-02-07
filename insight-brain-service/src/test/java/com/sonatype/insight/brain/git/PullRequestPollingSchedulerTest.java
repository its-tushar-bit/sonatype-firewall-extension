/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestPollingSchedulerTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private PullRequestPollingService pullRequestPollingService;

  @Mock
  private DefaultIqForScmLicenseChecker licenseChecker;

  public PullRequestPollingSchedulerTest() {
    super(PullRequestPollingScheduler.class);
  }

  @Test
  public void testPullRequestPollingScheduler_startAndStop() throws Exception {
    // given: scheduler instance with valid product license
    final int delaySeconds = 2;
    final int intervalSeconds = 1;
    PullRequestPollingScheduler scheduler =
        new PullRequestPollingScheduler(pullRequestPollingService, licenseChecker, delaySeconds, intervalSeconds);
    when(licenseChecker.isPullRequestCommentingSupported()).thenReturn(true);

    // when: start scheduler and wait (less than full initial delay)
    scheduler.start();
    Thread.sleep(1500);

    // then: no invocations of polling yet
    verify(pullRequestPollingService, never()).fetchAndSendPullRequestsForCommenting();
    assertThatLogMessagesEqual(
        info("Scheduled monitoring of SCM pull requests every 1 second(s) starting in 2 second(s)")
    );

    // when: wait 3 polling cycles
    Thread.sleep(3000);

    // then: polling service invoked 3 times
    verify(pullRequestPollingService, times(3)).fetchAndSendPullRequestsForCommenting();
    assertThatLogMessagesEqual(
        info("Scheduled monitoring of SCM pull requests every 1 second(s) starting in 2 second(s)"),
        debug("Commencing pull request polling cycle"),
        debug("Pull request polling cycle complete"),
        debug("Commencing pull request polling cycle"),
        debug("Pull request polling cycle complete"),
        debug("Commencing pull request polling cycle"),
        debug("Pull request polling cycle complete")
    );

    // when: stop scheduler and wait (2 intervals)
    scheduler.stop();
    Thread.sleep(2000);

    // then: scheduler stopped and no new invocations of the polling service
    verify(pullRequestPollingService, times(3)).fetchAndSendPullRequestsForCommenting();
    assertThatLogMessagesEqual(
        info("Scheduled monitoring of SCM pull requests every 1 second(s) starting in 2 second(s)"),
        debug("Commencing pull request polling cycle"),
        debug("Pull request polling cycle complete"),
        debug("Commencing pull request polling cycle"),
        debug("Pull request polling cycle complete"),
        debug("Commencing pull request polling cycle"),
        debug("Pull request polling cycle complete"),
        info("Stopped SCM pull request monitoring")
    );
  }

  @Test
  public void testPullRequestPollingScheduler_exceptionInPolling() throws Exception {
    // given: scheduler instance with polling service that throws IO exceptions
    final int delaySeconds = 1;
    final int intervalSeconds = 1;
    PullRequestPollingScheduler scheduler =
        new PullRequestPollingScheduler(pullRequestPollingService, licenseChecker, delaySeconds, intervalSeconds);
    doThrow(new RuntimeException("some runtime exception")).when(pullRequestPollingService)
        .fetchAndSendPullRequestsForCommenting();
    when(licenseChecker.isPullRequestCommentingSupported()).thenReturn(true);

    // when: start scheduler, wait (delay + 1 interval)
    scheduler.start();
    Thread.sleep(1500);

    // then : expecting exception was thrown, caught and handled
    verify(pullRequestPollingService, times(1)).fetchAndSendPullRequestsForCommenting();
    assertThatLogMessagesEqual(
        info("Scheduled monitoring of SCM pull requests every 1 second(s) starting in 1 second(s)"),
        debug("Commencing pull request polling cycle"),
        error("some runtime exception"),
        debug("Pull request polling cycle complete")
    );

    // when: throw runtime exception instead and wait another interval
    doThrow(new RuntimeException("some runtime exception")).when(pullRequestPollingService)
        .fetchAndSendPullRequestsForCommenting();
    Thread.sleep(1000);

    // then: polling still occurring and runtime exception was handled
    verify(pullRequestPollingService, times(2)).fetchAndSendPullRequestsForCommenting();
    assertThatLogMessagesEqual(
        info("Scheduled monitoring of SCM pull requests every 1 second(s) starting in 1 second(s)"),
        debug("Commencing pull request polling cycle"),
        error("some runtime exception"),
        debug("Pull request polling cycle complete"),
        debug("Commencing pull request polling cycle"),
        error("some runtime exception"),
        debug("Pull request polling cycle complete")
    );

    // when: stop throwing and wait
    doNothing().when(pullRequestPollingService).fetchAndSendPullRequestsForCommenting();
    Thread.sleep(1000);

    // then: polling still occurring
    verify(pullRequestPollingService, times(3)).fetchAndSendPullRequestsForCommenting();
    assertThatLogMessagesEqual(
        info("Scheduled monitoring of SCM pull requests every 1 second(s) starting in 1 second(s)"),
        debug("Commencing pull request polling cycle"),
        error("some runtime exception"),
        debug("Pull request polling cycle complete"),
        debug("Commencing pull request polling cycle"),
        error("some runtime exception"),
        debug("Pull request polling cycle complete"),
        debug("Commencing pull request polling cycle"),
        debug("Pull request polling cycle complete")
    );

    // cleanup: stop the scheduler so as not to interfere with other tests
    scheduler.stop();
  }

  @Test
  public void testPullRequestPollingScheduler_unlicensed() throws Exception {
    // given: valid scheduler instance but missing license feature
    PullRequestPollingScheduler scheduler =
        new PullRequestPollingScheduler(pullRequestPollingService, licenseChecker, 2, 1);

    // when: start scheduler and wait (less than full initial delay)
    scheduler.start();

    // then: PR polling scheduler is started, but it does nothing
    assertThatLogMessagesEqual(
        info("Scheduled monitoring of SCM pull requests every 1 second(s) starting in 2 second(s)")
    );
    verify(pullRequestPollingService, never()).fetchAndSendPullRequestsForCommenting();

    scheduler.stop();
  }
}
