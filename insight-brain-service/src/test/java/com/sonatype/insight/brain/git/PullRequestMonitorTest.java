/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.GitImplementation;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Category(SlowTest.class)
public class PullRequestMonitorTest
    extends AbstractComponentTest
{
  @Inject
  private SourceControlEventDAO sourceControlEventDAO;

  @Inject
  private PullRequestMonitor pullRequestMonitor;

  @Inject
  private SourceControlPullRequestDAO pullRequestDAO;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private GitApiFactory gitApiFactoryMock;

  @Mock
  private GitApi gitApiMock;

  @Mock
  private SourceControlEventPublisher sourceControlEventPublisherMock;

  @Mock
  private IqForScmLicenseChecker mockLicenseChecker;

  @Mock
  private SourceControlUtils mockSourceControlUtils;

  @Mock
  private ApiConfigFeaturesService mockApiConfigFeaturesService;

  @Inject
  private SourceControlConfigurationDAO sourceControlConfigurationDAO;

  @Inject
  private Configuration configuration;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Override
  public void configure(Binder binder) {
    lenient().when(taskSchedulerMock.isSchedulerInitialized()).thenReturn(true);
    lenient().when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(true);
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    binder.bind(GitApiFactory.class).toInstance(gitApiFactoryMock);
    binder.bind(SourceControlEventPublisher.class).toInstance(sourceControlEventPublisherMock);
    binder.bind(IqForScmLicenseChecker.class).toInstance(mockLicenseChecker);
    binder.bind(SourceControlUtils.class).toInstance(mockSourceControlUtils);
    binder.bind(ApiConfigFeaturesService.class).toInstance(mockApiConfigFeaturesService);
    binder.bind(ShutdownHandler.class).toInstance(mockShutdownHandler);
    super.configure(binder);
  }

  @Before
  public void before() {
    lenient().when(gitApiFactoryMock.createGitApi(any())).thenReturn(gitApiMock);
  }

  @Test
  public void testGetExecutorService() {
    ExecutorService executorService = pullRequestMonitor.getExecutorService();

    verify(mockShutdownHandler).add(executorService);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(PullRequestMonitor.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testExecute() {
    when(mockLicenseChecker.isIqForScmSupported()).thenReturn(true);

    PullRequestMonitor pullRequestMonitorSpy = spy(pullRequestMonitor);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(pullRequestMonitorSpy).updatePullRequestDetails();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      pullRequestMonitorSpy.execute(mock(JobExecutionContext.class));
    }

    verify(pullRequestMonitorSpy).updatePullRequestDetails();
  }

  @Test
  public void testExecute_Unlicensed() {
    when(mockLicenseChecker.isIqForScmSupported()).thenReturn(false);

    PullRequestMonitor pullRequestMonitorSpy = spy(pullRequestMonitor);
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      pullRequestMonitorSpy.execute(mock(JobExecutionContext.class));
    }

    verify(pullRequestMonitorSpy, never()).updatePullRequestDetails();
  }

  @Test
  public void testExecute_FeatureIsNotEnabled() {
    when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(false);

    PullRequestMonitor pullRequestMonitorSpy = spy(pullRequestMonitor);
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      pullRequestMonitorSpy.execute(mock(JobExecutionContext.class));
    }

    verify(pullRequestMonitorSpy, never()).updatePullRequestDetails();
  }

  @Test
  public void testUpdatePullRequestDetails_DeletesClosedPRs() throws Exception {
    createSourceControlForRootOrg();

    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo();
    gitRepositoryInfo.pullRequestCommentingEnabled = true;
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(any())).thenReturn(gitRepositoryInfo);

    // Given two pull requests
    Application app = tempEntity.newApplicationWithParent();
    String repositoryUrl = "http://example.com/testorg/testproject";
    tempEntity.newSourceControl(app.getId(), repositoryUrl);
    SourceControlPullRequest pullRequest1 = tempEntity.newSourceControlPullRequest(repositoryUrl, 1,
        "testHeadCommitHash1", "testBaseCommitHash1", "testBranchName1", "baseBranchName");
    SourceControlPullRequest pullRequest2 = tempEntity.newSourceControlPullRequest(repositoryUrl, 2,
        "testHeadCommitHash2", "testBaseCommitHash2", "testBranchName2", "baseBranchName");

    // Only for the first pull request the branch still exists
    when(gitApiMock.getHeadCommitsForAllBranches(repositoryUrl))
        .thenReturn(Collections.singletonMap("testBranchName1", "testHeadCommitHash1"));

    pullRequestMonitor.updatePullRequestDetails();

    // Then the pull request without a branch is deleted
    assertThat(pullRequestDAO.getById(pullRequest1.getId())).isNotNull();
    assertThat(pullRequestDAO.getById(pullRequest2.getId())).isNull();

    // And no source control event is sent
    verify(sourceControlEventPublisherMock, never()).publishEvent(any());
  }

  @Test
  public void testUpdatePullRequestDetails() throws Exception {
    createSourceControlForRootOrg();
    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo();
    gitRepositoryInfo.pullRequestCommentingEnabled = true;
    lenient().when(mockSourceControlUtils.getGitRepositoryInfoForApplication(any())).thenReturn(gitRepositoryInfo);
    Application app = tempEntity.newApplicationWithParent();
    String repositoryUrl = "http://example.com/testorg/testproject";
    tempEntity.newSourceControl(app.getId(), repositoryUrl);
    tempEntity.newSourceControlPullRequest(repositoryUrl, 1, "testHeadCommitHash1", "testBaseCommitHash1",
        "testBranchName1", "baseBranchName1", new Date(), new Date(), new Date(), null, PullRequestSource.AUTOMATIC);
    tempEntity.newSourceControlPullRequest(repositoryUrl, 2, "testHeadCommitHash2", "testBaseCommitHash2",
        "testBranchName2", "baseBranchName2", new Date(), new Date(), new Date(), null,
        PullRequestSource.AUTOMATIC_INNER_SOURCE);
    tempEntity.newSourceControlPullRequest(repositoryUrl, 3, "testHeadCommitHash3", "testBaseCommitHash3",
        "testBranchName3", "baseBranchName3", new Date(), new Date(), new Date(), null, PullRequestSource.MANUAL);
    tempEntity.newSourceControlPullRequest(repositoryUrl, 4, "testHeadCommitHash4", "testBaseCommitHash4",
        "testBranchName4", "baseBranchName4", new Date(), new Date(), new Date(), null,
        PullRequestSource.MANUAL_INNER_SOURCE);
    tempEntity.newSourceControlPullRequest(repositoryUrl, 5, "testHeadCommitHash5", "testBaseCommitHash5",
        "testBranchName5", "baseBranchName5", new Date(), new Date(), new Date(), null, null);
    tempEntity.newSourceControlPullRequest(repositoryUrl, 6, "testHeadCommitHash6", "testBaseCommitHash6",
        "testBranchName6", "baseBranchName6", new Date(), new Date(), new Date(), null, PullRequestSource.EXTERNAL);
    lenient().when(gitApiMock.getHeadCommitsForAllBranches(repositoryUrl))
        .thenReturn(Map.of(
            "testBranchName1", "testHeadCommitHashUpdated1",
            "testBranchName2", "testHeadCommitHashUpdated2",
            "testBranchName3", "testHeadCommitHashUpdated3",
            "testBranchName4", "testHeadCommitHashUpdated4",
            "testBranchName5", "testHeadCommitHashUpdated5",
            "testBranchName6", "testHeadCommitHashUpdated6"));

    pullRequestMonitor.updatePullRequestDetails();

    ArgumentCaptor<SourceControlEvent> captor = ArgumentCaptor.forClass(SourceControlEvent.class);
    verify(sourceControlEventPublisherMock, times(2)).publishEvent(captor.capture());
    List<SourceControlEvent> sourceControlEvents = captor.getAllValues();
    // We only expect PullRequestMonitor to consider external pull requests
    // which either have a source of null or explicitly set to external
    assertThat(sourceControlEvents).extracting(SourceControlEvent::getCommitHash)
        .containsExactlyInAnyOrder("testHeadCommitHashUpdated5", "testHeadCommitHashUpdated6");
  }

  @Test
  public void testUpdatePullRequestDetails_TwoPRsSameRepositoryUrl() throws Exception {
    createSourceControlForRootOrg();

    // Given two pull requests for the same repository
    Application app = tempEntity.newApplicationWithParent();
    String repositoryUrl = "http://example.com/testorg/testproject";
    tempEntity.newSourceControl(app.getId(), repositoryUrl);
    Date createTime = new Date(System.currentTimeMillis() - 2000);
    Date lastUpdateTime = new Date(System.currentTimeMillis() - 1000);
    SourceControlPullRequest pullRequest1 = tempEntity.newSourceControlPullRequest(repositoryUrl, 1,
        "testHeadCommitHash1", "testBaseCommitHash1", "testBranchName1", "baseBranchName",
        createTime, lastUpdateTime, lastUpdateTime);
    SourceControlPullRequest pullRequest2 = tempEntity.newSourceControlPullRequest(repositoryUrl, 2,
        "testHeadCommitHash2", "testBaseCommitHash2", "testBranchName2", "baseBranchName",
        createTime, lastUpdateTime, lastUpdateTime);

    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo();
    gitRepositoryInfo.pullRequestCommentingEnabled = true;
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(any())).thenReturn(gitRepositoryInfo);

    // First branch is updated
    Map<String, String> headCommitsByBranch = new HashMap<>();
    headCommitsByBranch.put("testBranchName1", "testHeadCommitHash1Updated");
    headCommitsByBranch.put("testBranchName2", "testHeadCommitHash2");
    when(gitApiMock.getHeadCommitsForAllBranches(repositoryUrl)).thenReturn(headCommitsByBranch);

    Date before = new Date();
    pullRequestMonitor.updatePullRequestDetails();
    Date after = new Date();

    // Then only the first pull request is updated
    pullRequest1 = pullRequestDAO.getById(pullRequest1.getId());
    assertPullRequest(pullRequest1, repositoryUrl, 1, "testHeadCommitHash1Updated", "testBaseCommitHash1",
        "testBranchName1", createTime);
    assertThat(pullRequest1.getLastCheckTime()).isBetween(before, after, true, true);
    assertThat(pullRequest1.getLastDetectedUpdateTime()).isBetween(before, after, true, true);
    pullRequest2 = pullRequestDAO.getById(pullRequest2.getId());
    assertPullRequest(pullRequest2, repositoryUrl, 2, "testHeadCommitHash2", "testBaseCommitHash2", "testBranchName2",
        createTime);
    assertThat(pullRequest2.getLastCheckTime()).isBetween(before, after, true, true);
    assertThat(pullRequest2.getLastDetectedUpdateTime()).isEqualTo(lastUpdateTime);

    verifySourceControlEventWasSent(app.getId(), pullRequest1);
  }

  @Test
  public void testUpdatePullRequestDetails_TwoApplicationsSameRepositoryUrl() throws Exception {
    createSourceControlForRootOrg();

    // Given two pull requests for the different repositories
    Application app1 = tempEntity.newApplicationWithParent();
    String repositoryUrl = "http://example.com/testorg/testproject";
    tempEntity.newSourceControl(app1.getId(), repositoryUrl);
    Application app2 = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(app2.getId(), repositoryUrl);

    Date createTime = new Date(System.currentTimeMillis() - 2000);
    Date lastUpdateTime = new Date(System.currentTimeMillis() - 1000);
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(repositoryUrl, 1,
        "testHeadCommitHash", "testBaseCommitHash1", "testBranchName", "baseBranchName",
        createTime, lastUpdateTime, lastUpdateTime);

    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo();
    gitRepositoryInfo.pullRequestCommentingEnabled = true;
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(any())).thenReturn(gitRepositoryInfo);

    // First branch is updated
    when(gitApiMock.getHeadCommitsForAllBranches(repositoryUrl))
        .thenReturn(Collections.singletonMap("testBranchName", "testHeadCommitHashUpdated"));
    Date before = new Date();
    pullRequestMonitor.updatePullRequestDetails();
    Date after = new Date();

    // Then only the first pull request is updated
    pullRequest = pullRequestDAO.getById(pullRequest.getId());
    assertPullRequest(pullRequest, repositoryUrl, 1, "testHeadCommitHashUpdated", "testBaseCommitHash1",
        "testBranchName", createTime);
    assertThat(pullRequest.getLastCheckTime()).isBetween(before, after, true, true);
    assertThat(pullRequest.getLastDetectedUpdateTime()).isBetween(before, after, true, true);

    // And events are sent for both apps
    ArgumentCaptor<SourceControlEvent> sourceControlEventArgumentCaptor =
        ArgumentCaptor.forClass(SourceControlEvent.class);
    verify(sourceControlEventPublisherMock, times(2)).publishEvent(sourceControlEventArgumentCaptor.capture());
    List<SourceControlEvent> sourceControlEvents = sourceControlEventArgumentCaptor.getAllValues();
    assertThat(sourceControlEvents).extracting(SourceControlEvent::getApplicationId)
        .containsExactlyInAnyOrder(app1.getId(), app2.getId());
    for (SourceControlEvent sourceControlEvent : sourceControlEvents) {
      assertThat(sourceControlEvent.getEventType()).isEqualTo(SourceControlEvent.UPDATED_PULL_REQUEST_EVENT);
      assertThat(sourceControlEvent.getBranchName()).isEqualTo(pullRequest.getBranchName());
      assertThat(sourceControlEvent.getCommitHash()).isEqualTo(pullRequest.getHeadCommitHash());
      assertThat(sourceControlEvent.getPullRequestNumber()).isEqualTo(pullRequest.getPullRequestId());
    }
  }

  @Test
  public void testUpdatePullRequestDetails_PrCommentingDisabledForApplication() throws Exception {
    createSourceControlForRootOrg();

    // Given a pull request for an app for which PR commenting is disabled
    Application app1 = tempEntity.newApplicationWithParent();
    String repositoryUrl = "http://example.com/testorg/testproject";
    tempEntity.newSourceControl(app1.getId(), repositoryUrl);

    Date createTime = new Date(System.currentTimeMillis() - 2000);
    Date lastUpdateTime = new Date(System.currentTimeMillis() - 1000);
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(repositoryUrl, 1,
        "testHeadCommitHash", "testBaseCommitHash1", "testBranchName", "baseBranchName",
        createTime, lastUpdateTime, lastUpdateTime);

    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo();
    gitRepositoryInfo.pullRequestCommentingEnabled = false;
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(any())).thenReturn(gitRepositoryInfo);

    // When update pull request details
    pullRequestMonitor.updatePullRequestDetails();

    // Then the PR is not updated
    pullRequest = pullRequestDAO.getById(pullRequest.getId());
    assertPullRequest(pullRequest, repositoryUrl, 1, "testHeadCommitHash", "testBaseCommitHash1",
        "testBranchName", createTime);
    assertThat(pullRequest.getLastCheckTime()).isEqualTo(lastUpdateTime);
    assertThat(pullRequest.getLastDetectedUpdateTime()).isEqualTo(lastUpdateTime);

    // No events are sent
    verify(sourceControlEventPublisherMock, never()).publishEvent(any());
    verifyNoInteractions(gitApiMock);
  }

  @Test
  public void testUpdatePullRequestDetails_UntrackedBranch() throws Exception {
    createSourceControlForRootOrg();

    // Given a pull request
    Application app = tempEntity.newApplicationWithParent();
    String repositoryUrl = "http://example.com/testorg/testproject";
    tempEntity.newSourceControl(app.getId(), repositoryUrl);
    Date createTime = new Date(System.currentTimeMillis() - 2000);
    Date lastUpdateTime = new Date(System.currentTimeMillis() - 1000);
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(repositoryUrl, 1,
        "testHeadCommitHash1", "testBaseCommitHash1", "testBranchName1", "baseBranchName",
        createTime, lastUpdateTime, lastUpdateTime);

    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo();
    gitRepositoryInfo.pullRequestCommentingEnabled = true;
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(any())).thenReturn(gitRepositoryInfo);

    // First branch is updated and one branch is not tracked (i.e. no corresponding PR)
    Map<String, String> headCommitsByBranch = new HashMap<>();
    headCommitsByBranch.put("testBranchName1", "testHeadCommitHash1Updated");
    headCommitsByBranch.put("testBranchName2", "testHeadCommitHash2");
    when(gitApiMock.getHeadCommitsForAllBranches(repositoryUrl)).thenReturn(headCommitsByBranch);

    Date before = new Date();
    pullRequestMonitor.updatePullRequestDetails();
    Date after = new Date();

    // Then only the first pull request is updated
    pullRequest = pullRequestDAO.getById(pullRequest.getId());
    assertPullRequest(pullRequest, repositoryUrl, 1, "testHeadCommitHash1Updated", "testBaseCommitHash1",
        "testBranchName1", createTime);
    assertThat(pullRequest.getLastCheckTime()).isBetween(before, after, true, true);
    assertThat(pullRequest.getLastDetectedUpdateTime()).isBetween(before, after, true, true);

    // And source control event is sent
    verifySourceControlEventWasSent(app.getId(), pullRequest);
  }

  @Test
  public void testUpdatePullRequestDetails_DoesNotSendEventIfEventIsAlreadyPending() throws Exception {
    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo();
    gitRepositoryInfo.pullRequestCommentingEnabled = true;
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(any())).thenReturn(gitRepositoryInfo);

    testUpdatePullRequestDetails_DoesNotSendEventIfEventExists(SourceControlEvent.EVENT_STATUS_NEW);
  }

  @Test
  public void testUpdatePullRequestDetails_DoesNotSendEventIfEventIsAlreadyInProgress() throws Exception {
    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo();
    gitRepositoryInfo.pullRequestCommentingEnabled = true;
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(any())).thenReturn(gitRepositoryInfo);

    testUpdatePullRequestDetails_DoesNotSendEventIfEventExists(SourceControlEvent.EVENT_STATUS_IN_PROGRESS);
  }

  @Test
  public void testSourceControlConfigurationChanged_NullConfiguration() {
    configuration.sourceControlConfigurationChanged();

    verify(taskSchedulerMock).schedulePeriodicTask(pullRequestMonitor,
        Duration.ofSeconds(new SourceControlConfiguration().getPullRequestMonitoringIntervalSeconds()));
  }

  @Test
  public void testSourceControlConfigurationChanged_UpdatedPullRequestMonitoringIntervalSeconds() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setPullRequestMonitoringIntervalSeconds(160);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);

    configuration.sourceControlConfigurationChanged();

    verify(taskSchedulerMock).schedulePeriodicTask(pullRequestMonitor,
        Duration.ofSeconds(sourceControlConfiguration.getPullRequestMonitoringIntervalSeconds()));
  }

  @Test
  public void testSourceControlConfigurationChanged_NoRelevantUpdate() {
    lenient().when(taskSchedulerMock.isTaskScheduled(any())).thenReturn(true);
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setGitImplementation(GitImplementation.JAVA);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);

    configuration.sourceControlConfigurationChanged();

    verify(taskSchedulerMock, never()).schedulePeriodicTask(any(), any());
  }

  @Test
  public void testUpdatePullRequestDetails_TwoApplicationsSameRepositoryUrlOneEnabledOneDisabled() throws Exception {
    createSourceControlForRootOrg();

    // Given two pull requests for the different repositories
    Application app1 = tempEntity.newApplicationWithParent();
    String repositoryUrl = "http://example.com/testorg/testproject";
    tempEntity.newSourceControl(app1.getId(), repositoryUrl);
    Application app2 = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(app2.getId(), repositoryUrl);

    Date createTime = new Date(System.currentTimeMillis() - 2000);
    Date lastUpdateTime = new Date(System.currentTimeMillis() - 1000);
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(repositoryUrl, 1,
        "testHeadCommitHash", "testBaseCommitHash1", "testBranchName", "baseBranchName",
        createTime, lastUpdateTime, lastUpdateTime);

    GitRepositoryInfo gitRepositoryInfo1 = new GitRepositoryInfo();
    gitRepositoryInfo1.pullRequestCommentingEnabled = true;

    GitRepositoryInfo gitRepositoryInfo2 = new GitRepositoryInfo();
    gitRepositoryInfo2.pullRequestCommentingEnabled = false;

    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(app1.getId())).thenReturn(gitRepositoryInfo1);
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(app2.getId())).thenReturn(gitRepositoryInfo2);

    when(gitApiMock.getHeadCommitsForAllBranches(repositoryUrl))
        .thenReturn(Collections.singletonMap("testBranchName", "testHeadCommitHashUpdated"));
    Date before = new Date();
    pullRequestMonitor.updatePullRequestDetails();
    Date after = new Date();

    pullRequest = pullRequestDAO.getById(pullRequest.getId());
    assertPullRequest(pullRequest, repositoryUrl, 1, "testHeadCommitHashUpdated", "testBaseCommitHash1",
        "testBranchName", createTime);
    assertThat(pullRequest.getLastCheckTime()).isBetween(before, after, true, true);
    assertThat(pullRequest.getLastDetectedUpdateTime()).isBetween(before, after, true, true);

    // And event sent for enabled app only
    ArgumentCaptor<SourceControlEvent> sourceControlEventArgumentCaptor =
        ArgumentCaptor.forClass(SourceControlEvent.class);
    verify(sourceControlEventPublisherMock, times(1)).publishEvent(sourceControlEventArgumentCaptor.capture());
    List<SourceControlEvent> sourceControlEvents = sourceControlEventArgumentCaptor.getAllValues();
    assertThat(sourceControlEvents).extracting(SourceControlEvent::getApplicationId)
        .containsExactlyInAnyOrder(app1.getId());
    for (SourceControlEvent sourceControlEvent : sourceControlEvents) {
      assertThat(sourceControlEvent.getEventType()).isEqualTo(SourceControlEvent.UPDATED_PULL_REQUEST_EVENT);
      assertThat(sourceControlEvent.getBranchName()).isEqualTo(pullRequest.getBranchName());
      assertThat(sourceControlEvent.getCommitHash()).isEqualTo(pullRequest.getHeadCommitHash());
      assertThat(sourceControlEvent.getPullRequestNumber()).isEqualTo(pullRequest.getPullRequestId());
    }
  }

  private void testUpdatePullRequestDetails_DoesNotSendEventIfEventExists(String eventStatus) throws Exception {
    createSourceControlForRootOrg();

    // Given a pull request
    Application app = tempEntity.newApplicationWithParent();
    String repositoryUrl = "http://example.com/testorg/testproject";
    tempEntity.newSourceControl(app.getId(), repositoryUrl);
    Date createTime = new Date(System.currentTimeMillis() - 2000);
    Date lastUpdateTime = new Date(System.currentTimeMillis() - 1000);
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(repositoryUrl, 1,
        "testHeadCommitHash", "testBaseCommitHash1", "testBranchName", "baseBranchName",
        createTime, lastUpdateTime, lastUpdateTime);
    // And an existing event
    SourceControlEvent existingEvent = new SourceControlEvent() //
        .setApplicationId(app.getId()) //
        .setEventType(SourceControlEvent.UPDATED_PULL_REQUEST_EVENT) //
        .setPullRequestNumber(pullRequest.getPullRequestId()) //
        .setEventStatus(eventStatus);
    sourceControlEventDAO.insert(existingEvent);

    // The branch is updated
    when(gitApiMock.getHeadCommitsForAllBranches(repositoryUrl))
        .thenReturn(Collections.singletonMap("testBranchName", "testHeadCommitHashUpdated"));

    pullRequestMonitor.updatePullRequestDetails();

    verify(sourceControlEventPublisherMock, never()).publishEvent(any());
  }

  private void assertPullRequest(
      SourceControlPullRequest pullRequest,
      String repositoryUrl,
      int pullRequestId,
      String headCommitHash,
      String baseCommitHash,
      String branchName,
      Date createTime)
  {
    assertThat(pullRequest.getRepositoryUrl()).isEqualTo(repositoryUrl);
    assertThat(pullRequest.getPullRequestId()).isEqualTo(pullRequestId);
    assertThat(pullRequest.getHeadCommitHash()).isEqualTo(headCommitHash);
    assertThat(pullRequest.getBaseCommitHash()).isEqualTo(baseCommitHash);
    assertThat(pullRequest.getBranchName()).isEqualTo(branchName);
    assertThat(pullRequest.getCreateTime()).isEqualTo(createTime);
  }

  private void createSourceControlForRootOrg() {
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
  }

  private void verifySourceControlEventWasSent(String appId, SourceControlPullRequest pullRequest) {
    ArgumentCaptor<SourceControlEvent> sourceControlEventArgumentCaptor =
        ArgumentCaptor.forClass(SourceControlEvent.class);
    verify(sourceControlEventPublisherMock).publishEvent(sourceControlEventArgumentCaptor.capture());
    SourceControlEvent sourceControlEvent = sourceControlEventArgumentCaptor.getValue();
    assertThat(sourceControlEvent.getEventType()).isEqualTo(SourceControlEvent.UPDATED_PULL_REQUEST_EVENT);
    assertThat(sourceControlEvent.getApplicationId()).isEqualTo(appId);
    assertThat(sourceControlEvent.getBranchName()).isEqualTo(pullRequest.getBranchName());
    assertThat(sourceControlEvent.getCommitHash()).isEqualTo(pullRequest.getHeadCommitHash());
    assertThat(sourceControlEvent.getPullRequestNumber()).isEqualTo(pullRequest.getPullRequestId());
  }
}
