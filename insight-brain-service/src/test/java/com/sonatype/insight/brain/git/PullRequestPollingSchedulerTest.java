/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.service.ScmNodeProcessor;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.awaitility.Awaitility.await;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PullRequestPollingSchedulerTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private PullRequestPollingService pullRequestPollingService;

  @Mock
  private IqForScmLicenseChecker licenseChecker;

  @Mock
  private ApiConfigFeaturesService mockApiConfigFeaturesService;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Mock
  private ScmNodeProcessor scmNodeProcessor;

  public PullRequestPollingSchedulerTest() {
    super(PullRequestPollingScheduler.class);
  }

  @AfterEach
  public void shutdownAllExecutors() throws InterruptedException {
    ArgumentCaptor<ExecutorService> captor = ArgumentCaptor.forClass(ExecutorService.class);
    verify(mockShutdownHandler, atLeast(0)).add(captor.capture());
    List<ExecutorService> executors = captor.getAllValues();
    for (ExecutorService executor : executors) {
      executor.shutdownNow();
    }
    for (ExecutorService executor : executors) {
      executor.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  @Test
  public void testNewExecutor() {
    PullRequestPollingScheduler pullRequestPollingScheduler = new PullRequestPollingScheduler(pullRequestPollingService,
        licenseChecker, mockApiConfigFeaturesService, 2, 1, mockShutdownHandler, scmNodeProcessor);

    ScheduledExecutorService scheduledExecutorService = pullRequestPollingScheduler.newExecutor();

    verify(mockShutdownHandler).add(scheduledExecutorService);
  }

  @Test
  public void testPullRequestPollingScheduler_startAndStop() throws Exception {
    // given: scheduler instance with valid product license and scm feature is enabled
    final int delaySeconds = 5;
    final int intervalSeconds = 1;
    PullRequestPollingScheduler scheduler = new PullRequestPollingScheduler(pullRequestPollingService, licenseChecker,
        mockApiConfigFeaturesService, delaySeconds, intervalSeconds, mockShutdownHandler, scmNodeProcessor);
    when(licenseChecker.isPullRequestCommentingSupported()).thenReturn(true);
    when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(true);
    when(scmNodeProcessor.shouldRun()).thenReturn(true);

    // when: start scheduler and wait (well within the initial delay; 3.5s margin)
    scheduler.register();
    Thread.sleep(1500);

    // then: no invocations of polling yet (delay is 5s, only 1.5s elapsed)
    verify(pullRequestPollingService, never()).fetchAndSendPullRequestsForCommenting();
    assertThatLogMessagesEqual(
        info("Scheduled discovery of SCM pull requests every 1 second(s) starting in 5 second(s)"));

    // when: wait for 3 polling cycles (using Awaitility to avoid timing flakes)
    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(pullRequestPollingService, atLeast(3)).fetchAndSendPullRequestsForCommenting());

    // then: polling service invoked at least 3 times and log messages contain expected subsequence
    assertThatLogMessagesContainSubsequence(
        info("Scheduled discovery of SCM pull requests every 1 second(s) starting in 5 second(s)"),
        debug("Commencing pull request polling cycle"),
        debug("Pull request polling cycle complete"),
        debug("Commencing pull request polling cycle"),
        debug("Pull request polling cycle complete"),
        debug("Commencing pull request polling cycle"),
        debug("Pull request polling cycle complete"));

    // when: stop scheduler
    scheduler.deregister();
    clearInvocations(pullRequestPollingService);
    Thread.sleep(2000);

    // then: scheduler stopped and no new invocations of the polling service after deregister
    verify(pullRequestPollingService, never()).fetchAndSendPullRequestsForCommenting();
    assertThatLogMessagesContainSubsequence(
        info("Scheduled discovery of SCM pull requests every 1 second(s) starting in 5 second(s)"),
        debug("Commencing pull request polling cycle"),
        debug("Pull request polling cycle complete"),
        info("Stopped SCM pull request discovery"));
  }

  @Test
  public void testPullRequestPollingScheduler_exceptionInPolling() throws Exception {
    // given: scheduler instance with polling service that throws runtime exceptions
    final int delaySeconds = 1;
    final int intervalSeconds = 1;
    PullRequestPollingScheduler scheduler = new PullRequestPollingScheduler(pullRequestPollingService, licenseChecker,
        mockApiConfigFeaturesService, delaySeconds, intervalSeconds, mockShutdownHandler, scmNodeProcessor);
    doThrow(new RuntimeException("some runtime exception")).when(pullRequestPollingService)
        .fetchAndSendPullRequestsForCommenting();
    when(licenseChecker.isPullRequestCommentingSupported()).thenReturn(true);
    when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(true);
    when(scmNodeProcessor.shouldRun()).thenReturn(true);

    // when: start scheduler, wait for first invocation
    scheduler.register();
    await().atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(pullRequestPollingService, atLeast(1)).fetchAndSendPullRequestsForCommenting());

    // then: exception was thrown, caught and handled
    assertThatLogMessagesContainSubsequence(
        info("Scheduled discovery of SCM pull requests every 1 second(s) starting in 1 second(s)"),
        debug("Commencing pull request polling cycle"),
        error("Failed to run pull request discovery cycle: some runtime exception"));

    // when: throw runtime exception again and wait for second invocation
    doThrow(new RuntimeException("some runtime exception")).when(pullRequestPollingService)
        .fetchAndSendPullRequestsForCommenting();
    await().atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(pullRequestPollingService, atLeast(2)).fetchAndSendPullRequestsForCommenting());

    // then: polling still occurring and runtime exception was handled again
    assertThatLogMessagesContainSubsequence(
        info("Scheduled discovery of SCM pull requests every 1 second(s) starting in 1 second(s)"),
        debug("Commencing pull request polling cycle"),
        error("Failed to run pull request discovery cycle: some runtime exception"),
        debug("Commencing pull request polling cycle"),
        error("Failed to run pull request discovery cycle: some runtime exception"));

    // when: stop throwing and wait for next successful invocation
    // lenient: on slow CI agents, the successful-cycle log assertion (await below) may be satisfied
    // by an in-flight cycle before this fresh stub is invoked, causing a Mockito strict-stubbing failure.
    lenient().doNothing().when(pullRequestPollingService).fetchAndSendPullRequestsForCommenting();
    await().atMost(5, TimeUnit.SECONDS)
        .until(() -> logMessagesContainTuple(debug("Pull request polling cycle complete")));

    // then: polling still occurring and successful cycle completed
    assertThatLogMessagesContainSubsequence(
        debug("Commencing pull request polling cycle"),
        error("Failed to run pull request discovery cycle: some runtime exception"),
        debug("Commencing pull request polling cycle"),
        debug("Pull request polling cycle complete"));

    // cleanup: stop the scheduler so as not to interfere with other tests
    scheduler.deregister();
  }

  @Test
  public void testPullRequestPollingScheduler_unlicensed() throws Exception {
    // given: valid scheduler instance but missing license feature
    PullRequestPollingScheduler scheduler = new PullRequestPollingScheduler(pullRequestPollingService, licenseChecker,
        mockApiConfigFeaturesService, 2, 1, mockShutdownHandler, scmNodeProcessor);
    when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(true);
    when(scmNodeProcessor.shouldRun()).thenReturn(true);

    // when: start scheduler and wait (less than full initial delay)
    scheduler.register();

    // then: PR polling scheduler is started, but it does nothing
    assertThatLogMessagesEqual(
        info("Scheduled discovery of SCM pull requests every 1 second(s) starting in 2 second(s)"));

    Thread.sleep(2500);

    verify(pullRequestPollingService, never()).fetchAndSendPullRequestsForCommenting();

    scheduler.deregister();
  }

  @Test
  public void testPullRequestPollingScheduler_saasLifecycleScmFeatureDisabled() throws Exception {
    // given: valid scheduler instance but missing scm feature flag
    lenient().when(licenseChecker.isPullRequestCommentingSupported()).thenReturn(true);
    when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(false);
    when(scmNodeProcessor.shouldRun()).thenReturn(true);

    PullRequestPollingScheduler scheduler = new PullRequestPollingScheduler(pullRequestPollingService, licenseChecker,
        mockApiConfigFeaturesService, 2, 1, mockShutdownHandler, scmNodeProcessor);

    // when: start scheduler and wait (less than full initial delay)
    scheduler.register();

    // then: PR polling scheduler is started, but it does nothing
    assertThatLogMessagesEqual(
        info("Scheduled discovery of SCM pull requests every 1 second(s) starting in 2 second(s)"));

    Thread.sleep(2500);

    verify(pullRequestPollingService, never()).fetchAndSendPullRequestsForCommenting();

    scheduler.deregister();
  }

  @Test
  public void testPullRequestPollingScheduler_saasLifecycleNonBatchModeConfig() throws Exception {
    // given: valid scheduler instance but missing scm feature flag
    lenient().when(licenseChecker.isPullRequestCommentingSupported()).thenReturn(true);
    when(scmNodeProcessor.shouldRun()).thenReturn(false);

    PullRequestPollingScheduler scheduler = new PullRequestPollingScheduler(pullRequestPollingService, licenseChecker,
        mockApiConfigFeaturesService, 2, 1, mockShutdownHandler, scmNodeProcessor);

    // when: start scheduler and wait (less than full initial delay)
    scheduler.register();

    Thread.sleep(2500);

    verify(pullRequestPollingService, never()).fetchAndSendPullRequestsForCommenting();

    scheduler.deregister();
  }

  @Test
  public void testPullRequestPollingScheduler_saasLifecycleBatchModeConfig() throws Exception {
    // given: valid scheduler instance but missing scm feature flag
    lenient().when(licenseChecker.isPullRequestCommentingSupported()).thenReturn(true);
    when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(true);
    when(scmNodeProcessor.shouldRun()).thenReturn(false);
    when(scmNodeProcessor.shouldRun()).thenReturn(true);

    PullRequestPollingScheduler scheduler = new PullRequestPollingScheduler(pullRequestPollingService, licenseChecker,
        mockApiConfigFeaturesService, 2, 1, mockShutdownHandler, scmNodeProcessor);

    // when: start scheduler and wait (less than full initial delay)
    scheduler.register();

    // then: PR polling scheduler is started, but it does nothing
    assertThatLogMessagesEqual(
        info("Scheduled discovery of SCM pull requests every 1 second(s) starting in 2 second(s)"));

    Thread.sleep(2500);

    verify(pullRequestPollingService, atLeastOnce()).fetchAndSendPullRequestsForCommenting();

    scheduler.deregister();
  }
}
