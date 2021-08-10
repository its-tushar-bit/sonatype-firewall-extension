/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.PullRequestInfoProvider;
import com.sonatype.nexus.scm.api.model.ProjectUri;
import com.sonatype.nexus.scm.api.model.PullRequest;
import com.sonatype.nexus.scm.common.SimpleProjectUri;
import com.sonatype.nexus.scm.github.dto.GithubPullRequest;
import com.sonatype.nexus.scm.gitlab.dto.GitlabMergeRequestResponse;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PullRequestPollingServiceTest
    extends VerifiableLoggingTestBase
{
  @Mock
  SourceControlEventPublisher sourceControlEventPublisher;

  @Mock
  private SourceControlPullRequestDAO mockSourceControlPullRequestDAO;

  private Map<String, PullRequestInfoProvider> mockClientMap = new HashMap<>();

  public PullRequestPollingServiceTest() {
    super(PullRequestPollingService.class);
  }

  @Before
  @Override
  public void setup() {
    MockitoAnnotations.openMocks(this);
    super.setup();
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_noRepositoriesToPoll() throws IOException {
    // given:
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .build();

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: no events emitted
    verify(sourceControlEventPublisher, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual();
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_pullRequestIsForBaseBranch() throws IOException {
    // given: necessary ingredients to emit a discovered pull request event
    final Date pullRequestCreateDate = new Date();
    final Date pullRequestPollingTime = new Date(System.currentTimeMillis() - 1000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("org5/repo5", SourceControlProvider.GITHUB)
        .withApplication("appBaseBranch", "develop")
        .withPollingTime(pullRequestPollingTime)
        .withPullRequest(10, pullRequestCreateDate, "develop", "main-branch", "feature-commit-xyz-1")
        .build();

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: event emitted
    verify(sourceControlEventPublisher, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 1 pull request(s) for org 'org5' since " + pullRequestPollingTime),
        debug("Repository 'https://domain.com/org5/repo5' pull request '10' is for application 'appBaseBranch' base " +
            "branch, skipping commenting"),
        debug("Pull request polling time updated for 'https://domain.com/org5/repo5'")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_shouldPostEvent() throws IOException {
    // given: necessary ingredients to emit a discovered pull request event
    final Date pullRequestCreateDate = new Date();
    final Date pullRequestPollingTime = new Date(System.currentTimeMillis() - 3000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("org7/repo7", SourceControlProvider.GITHUB)
        .withApplication("appPost", "main-branch")
        .withPollingTime(pullRequestPollingTime)
        .withPullRequest(10, pullRequestCreateDate, "feature-branch", "main-branch", "feature-commit-xyz-1")
        .build();

    // when: fetch and send
    Date before = new Date();
    pollingService.fetchAndSendPullRequestsForCommenting();
    Date after = new Date();

    // then: pull request is persisted and event emitted
    ArgumentCaptor<SourceControlPullRequest> sourceControlPullRequestArgumentCaptor =
        ArgumentCaptor.forClass(SourceControlPullRequest.class);
    verify(mockSourceControlPullRequestDAO, times(1)).insert(sourceControlPullRequestArgumentCaptor.capture());
    SourceControlPullRequest sourceControlPullRequest = sourceControlPullRequestArgumentCaptor.getValue();
    assertSourceControlPullRequest(sourceControlPullRequest, "https://domain.com/org7/repo7", 10,
        "feature-commit-xyz-1", "feature-branch", pullRequestCreateDate, before, after);

    verify(sourceControlEventPublisher, times(1)).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 1 pull request(s) for org 'org7' since " + pullRequestPollingTime),
        info("Sent pull request discovered event for application 'appPost' with PR# '10' and commit " +
            "'feature-commit-xyz-1'"),
        debug("Pull request polling time updated for 'https://domain.com/org7/repo7'")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_unlicensed() throws IOException {
    // given: necessary ingredients to emit a discovered pull request event,
    // but the license does not support PR commenting
    final Date pullRequestCreateDate = new Date();
    final Date pullRequestPollingTime = new Date(System.currentTimeMillis() - 3000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("org7/repo7", SourceControlProvider.GITHUB)
        .withApplication("appPost", "main-branch")
        .withPollingTime(pullRequestPollingTime)
        .withPullRequest(10, pullRequestCreateDate, "feature-branch", "main-branch", "feature-commit-xyz-1")
        .unlicensed()
        .build();

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: no events are created
    verify(sourceControlEventPublisher, never()).publishEvent(any(SourceControlEvent.class));
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_MultipleAppsSameRepositoryUrl() throws IOException {
    // given:
    final Date pullRequestCreateDate = new Date();
    final Date pullRequestPollingTime = new Date(System.currentTimeMillis() - 3000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("testorg/testrepo", SourceControlProvider.GITHUB).withApplication("app1", "main-branch")
        .withPollingTime(pullRequestPollingTime)
        .withPullRequest(10, pullRequestCreateDate, "feature-branch", "main-branch", "feature-commit-xyz-1")
        .forRepository("testorg/testrepo", SourceControlProvider.GITHUB).withApplication("app2", "main-branch")
        .withPollingTime(pullRequestPollingTime).build();

    // when: fetch and send
    Date before = new Date();
    pollingService.fetchAndSendPullRequestsForCommenting();
    Date after = new Date();

    // then: one pull request is persisted and two events are emitted
    ArgumentCaptor<SourceControlPullRequest> sourceControlPullRequestArgumentCaptor =
        ArgumentCaptor.forClass(SourceControlPullRequest.class);
    verify(mockSourceControlPullRequestDAO, times(1)).insert(sourceControlPullRequestArgumentCaptor.capture());
    SourceControlPullRequest sourceControlPullRequest = sourceControlPullRequestArgumentCaptor.getValue();
    assertSourceControlPullRequest(sourceControlPullRequest, "https://domain.com/testorg/testrepo", 10,
        "feature-commit-xyz-1", "feature-branch", pullRequestCreateDate, before, after);

    verify(sourceControlEventPublisher, times(2)).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(debug("Fetched 1 pull request(s) for org 'testorg' since " + pullRequestPollingTime),
        info("Sent pull request discovered event for application 'app1' with PR# '10' and commit "
            + "'feature-commit-xyz-1'"),
        debug("Pull request polling time updated for 'https://domain.com/testorg/testrepo'"),
        info("Sent pull request discovered event for application 'app2' with PR# '10' and commit "
            + "'feature-commit-xyz-1'"),
        debug("Pull request polling time updated for 'https://domain.com/testorg/testrepo'"));
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_noPolicyEvalAndDoesNotTargetBaseBranch() throws IOException {
    // given: PR without associated policy eval and PR does not target a base branch for any application
    final Date pullRequestCreateDate = new Date();
    final Date pullRequestPollingTime = new Date(System.currentTimeMillis() - 5000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("org9/repo9", SourceControlProvider.GITHUB)
        .withApplication("appNoTarget", "main-branch")
        .withPollingTime(pullRequestPollingTime)
        .withPullRequest(10, pullRequestCreateDate, "feature-branch", "not-main-branch", "feature-commit-xyz-1")
        .build();

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: event emitted
    verify(sourceControlEventPublisher, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 1 pull request(s) for org 'org9' since " + pullRequestPollingTime),
        debug("Repository 'https://domain.com/org9/repo9' pull request '10' for application 'appNoTarget' neither " +
            "targets the default branch nor has a policy evaluation associated with the head commit, skipping " +
            "commenting"),
        debug("Pull request polling time updated for 'https://domain.com/org9/repo9'")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_withPolicyEvalAndDoesNotTargetBaseBranch() throws IOException {
    // given: PR without associated policy eval and PR does not target a base branch for any application
    final Date pullRequestCreateDate = new Date();
    final Date pullRequestPollingTime = new Date(System.currentTimeMillis() - 1000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("org3/repo3", SourceControlProvider.GITHUB)
        .withApplication("appWithPolicy", "main-branch")
        .withPollingTime(pullRequestPollingTime)
        .withPullRequest(10, pullRequestCreateDate, "feature-branch", "not-main-branch", "feature-commit-xyz-1")
        .withPolicyEvaluation()
        .build();

    // when: fetch and send
    Date before = new Date();
    pollingService.fetchAndSendPullRequestsForCommenting();
    Date after = new Date();

    // then: pull request is persisted and event emitted
    ArgumentCaptor<SourceControlPullRequest> sourceControlPullRequestArgumentCaptor =
        ArgumentCaptor.forClass(SourceControlPullRequest.class);
    verify(mockSourceControlPullRequestDAO, times(1)).insert(sourceControlPullRequestArgumentCaptor.capture());
    SourceControlPullRequest sourceControlPullRequest = sourceControlPullRequestArgumentCaptor.getValue();
    assertSourceControlPullRequest(sourceControlPullRequest, "https://domain.com/org3/repo3", 10,
        "feature-commit-xyz-1", "feature-branch", pullRequestCreateDate, before, after);

    verify(sourceControlEventPublisher, times(1)).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 1 pull request(s) for org 'org3' since " + pullRequestPollingTime),
        info("Sent pull request discovered event for application 'appWithPolicy' with PR# '10' and commit " +
            "'feature-commit-xyz-1'"),
        debug("Pull request polling time updated for 'https://domain.com/org3/repo3'")
    );
  }

  private void assertSourceControlPullRequest(
      SourceControlPullRequest actual,
      String repositoryUrl,
      int pullRequestId,
      String headCommitHash,
      String branchName,
      Date createTime,
      Date before,
      Date after)
  {
    assertThat(actual.getRepositoryUrl()).isEqualTo(repositoryUrl);
    assertThat(actual.getPullRequestId()).isEqualTo(pullRequestId);
    assertThat(actual.getHeadCommitHash()).isEqualTo(headCommitHash);
    assertThat(actual.getBranchName()).isEqualTo(branchName);
    assertThat(actual.getCreateTime()).isEqualTo(createTime);
    assertThat(actual.getLastCheckTime()).isBetween(before, after, true, true);
    assertThat(actual.getLastDetectedUpdateTime()).isBetween(before, after, true, true);
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_shouldPostEventNotPrivateButInternal() throws IOException {
    // given: necessary ingredients to emit a discovered pull request event
    final Date pullRequestCreateDate = new Date();
    final Date pullRequestPollingTime = new Date(System.currentTimeMillis() - 1000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("orgInt/repoInt", SourceControlProvider.GITHUB)
        .withApplication("appInternal", "main-branch")
        .withPollingTime(pullRequestPollingTime)
        .withPullRequest(10, pullRequestCreateDate, "feature-branch", "main-branch", "feature-commit-xyz-1")
        .withGitRepositoryPrivate(false)
        .withGitRepositoryInternal(true)
        .build();

    // when: fetch and send
    Date before = new Date();
    pollingService.fetchAndSendPullRequestsForCommenting();
    Date after = new Date();

    // then: pull request is persisted and event emitted
    ArgumentCaptor<SourceControlPullRequest> sourceControlPullRequestArgumentCaptor =
        ArgumentCaptor.forClass(SourceControlPullRequest.class);
    verify(mockSourceControlPullRequestDAO, times(1)).insert(sourceControlPullRequestArgumentCaptor.capture());
    SourceControlPullRequest sourceControlPullRequest = sourceControlPullRequestArgumentCaptor.getValue();
    assertSourceControlPullRequest(sourceControlPullRequest, "https://domain.com/orgint/repoint", 10,
        "feature-commit-xyz-1", "feature-branch", pullRequestCreateDate, before, after);

    verify(sourceControlEventPublisher, times(1)).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 1 pull request(s) for org 'orgInt' since " + pullRequestPollingTime),
        info("Sent pull request discovered event for application 'appInternal' with PR# '10' and commit " +
            "'feature-commit-xyz-1'"),
        debug("Pull request polling time updated for 'https://domain.com/orgInt/repoInt'")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_bitbucketCloudNotSupported() throws IOException {
    // given:
    String repositoryUrl = "https://bitbucket.org/orgBbcns/repoBbcns";
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository(repositoryUrl, "orgBbcns/repoBbcns", SourceControlProvider.BITBUCKET)
        .withApplication("appBitbucket", "main-branch")
        .build();

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: no events emitted
    verify(sourceControlEventPublisher, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("BITBUCKET is not currently supported for pull request commenting on repository " + repositoryUrl)
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_RepositoryNotPrivate() throws IOException {
    // given: necessary ingredients to emit a discovered pull request event with a non-private repo
    final Date pullRequestCreateDate = new Date();
    final Date pullRequestPollingTime = new Date(System.currentTimeMillis() - 1000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("orgNp/repoNp", SourceControlProvider.GITHUB)
        .withApplication("appNotPrivate", "main-branch")
        .withPollingTime(pullRequestPollingTime)
        .withPullRequest(10, pullRequestCreateDate, "feature-branch", "main-branch", "feature-commit-xyz-1")
        .withGitRepositoryPrivate(false)
        .build();

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: no events emitted
    verify(sourceControlEventPublisher, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 1 pull request(s) for org 'orgNp' since " + pullRequestPollingTime),
        debug("Repository is not valid for pull requests, check that it is private: https://domain.com/orgNp/repoNp"),
        debug("Pull request polling time updated for 'https://domain.com/orgNp/repoNp'")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_errorFetchingPullRequests_GitHub() throws IOException {
    // given:
    Date pullRequestCreateDate = new Date(System.currentTimeMillis() - 1000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("orgErr/repoErr", SourceControlProvider.GITHUB)
        .withApplication("appGithub", "main-branch")
        .withPollingTime(new Date())
        .withId("sourceControl1")
        .withPullRequest(10, pullRequestCreateDate, "feature-branch", "main-branch", "feature-commit-xyz-1")
        .build();
    doThrow(new IOException("scm error"))
        .when(mockClientMap.get("orgErr/repoErr"))
        .getPullRequestsSince(any(String.class), any(OffsetDateTime.class),
            eq(PullRequestPollingService.PULL_REQUESTS_PER_MONITOR_CYCLE));

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: no events emitted
    verify(sourceControlEventPublisher, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        warn(
            "Could not fetch pull requests for org 'orgErr'; will retry in 5 minutes.  Please " +
                "check that the configured project url https://domain.com/orgErr/repoErr is correct, that it is for " +
                "'github' and that the API token is valid")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_errorFetchingPullRequests_GitLab() throws IOException {
    // given:
    Date pullRequestCreateDate = new Date(System.currentTimeMillis() - 1000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("orgErr/repoErr", SourceControlProvider.GITLAB)
        .withApplication("appGitLab", "main-branch")
        .withPollingTime(new Date())
        .withId("sourceControl1")
        .withPullRequest(10, pullRequestCreateDate, "feature-branch", "main-branch", "feature-commit-xyz-1")
        .build();
    doThrow(new IOException("scm error"))
        .when(mockClientMap.get("orgErr/repoErr"))
        .getPullRequestsSince(any(String.class), any(OffsetDateTime.class),
            eq(PullRequestPollingService.PULL_REQUESTS_PER_MONITOR_CYCLE));

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: no events emitted
    verify(sourceControlEventPublisher, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        warn(
            "Could not fetch pull requests for org 'orgErr' repo 'repoErr'; will retry in 5 minutes.  Please " +
                "check that the configured project url https://domain.com/orgErr/repoErr is correct, that it is for " +
                "'gitlab' and that the API token is valid")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_multipleReposGitLab() throws Exception {
    // given: more than one repo with PRs to poll
    final Date repo1pullRequestCreateDate = new Date(System.currentTimeMillis() - 5000);
    final Date repo2pullRequestCreateDate = new Date(System.currentTimeMillis() - 3000);
    final Date repo1pullRequestPollingTime = new Date(System.currentTimeMillis() - 10000);
    final Date repo2pullRequestPollingTime = new Date(System.currentTimeMillis() - 8000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()

        .forRepository("org/multi-1", SourceControlProvider.GITLAB)
        .withApplication("gitlab1", "main-branch")
        .withPollingTime(repo1pullRequestPollingTime)
        .withPullRequest(10, repo1pullRequestCreateDate, "feature-branch", "main-branch", "feature-commit-xyz-1")

        .forRepository("org/multi-2", SourceControlProvider.GITLAB)
        .withApplication("gitlab2", "main-branch")
        .withPollingTime(repo2pullRequestPollingTime)
        .withPullRequest(20, repo2pullRequestCreateDate, "R2-feature-branch", "main-branch", "feature-commit-abc-2")

        .build();

    // when:
    Date before = new Date();
    pollingService.fetchAndSendPullRequestsForCommenting();
    Date after = new Date();

    // then: pull requests are persisted and events emitted
    ArgumentCaptor<SourceControlPullRequest> sourceControlPullRequestArgumentCaptor =
        ArgumentCaptor.forClass(SourceControlPullRequest.class);
    verify(mockSourceControlPullRequestDAO, times(2)).insert(sourceControlPullRequestArgumentCaptor.capture());
    List<SourceControlPullRequest> sourceControlPullRequests = sourceControlPullRequestArgumentCaptor.getAllValues();
    assertSourceControlPullRequest(sourceControlPullRequests.get(0), "https://domain.com/org/multi-1", 10,
        "feature-commit-xyz-1", "feature-branch", repo1pullRequestCreateDate, before, after);
    assertSourceControlPullRequest(sourceControlPullRequests.get(1), "https://domain.com/org/multi-2", 20,
        "feature-commit-abc-2", "R2-feature-branch", repo2pullRequestCreateDate, before, after);

    verify(sourceControlEventPublisher, times(2)).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 1 pull request(s) for org 'org' repo 'multi-1' since " + repo1pullRequestPollingTime),
        debug("Fetched 1 pull request(s) for org 'org' repo 'multi-2' since " + repo2pullRequestPollingTime),
        info(
            "Sent pull request discovered event for application 'gitlab1' with PR# '10' and commit " +
                "'feature-commit-xyz-1'"),
        debug("Pull request polling time updated for 'https://domain.com/org/multi-1'"),
        info(
            "Sent pull request discovered event for application 'gitlab2' with PR# '20' and commit " +
                "'feature-commit-abc-2'"),
        debug("Pull request polling time updated for 'https://domain.com/org/multi-2'")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_isRemediationPullRequest() throws IOException {
    // given: polling service setup to fetch a remediation PR
    final Date pullRequestCreateDate = new Date();
    final Date pullRequestPollingTime = new Date(System.currentTimeMillis() - 5000);
    final String appId = "app123456";
    String branchPrefix = new RemediationBranchNamePrefixGenerator().generatePrefixForApplication(appId);

    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("githubOrg/remediation", SourceControlProvider.GITHUB)
        .withApplication(appId, "main-branch")
        .withPollingTime(pullRequestPollingTime)
        .withPullRequest(10, pullRequestCreateDate, branchPrefix + "/com.sonatype/iq-server/1.108", "main-branch",
            "feature-commit-xyz-1")
        .build();

    // when: fetch and send PRs
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: no events sent and log message explains why
    verify(sourceControlEventPublisher, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesContain(
        debug("Pull request 10 for branch app123/com.sonatype/iq-server/1.108 is determined to be an IQ Server " +
            "generated remediation PR.  We will not comment on it."),
        debug("Pull request polling time updated for 'https://domain.com/githubOrg/remediation'")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_multipleReposGitHub() throws Exception {
    // given: more than one repo with PRs to poll
    final Date repo1pullRequestCreateDate = new Date(System.currentTimeMillis() - 5000);
    final Date repo2pullRequestCreateDate = new Date(System.currentTimeMillis() - 3000);
    final Date repo1pullRequestPollingTime = new Date(System.currentTimeMillis() - 10000);
    final Date repo2pullRequestPollingTime = new Date(System.currentTimeMillis() - 8000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()

        .forRepository("githubOrg/multi-1", SourceControlProvider.GITHUB)
        .withApplication("github1", "main-branch")
        .withPollingTime(repo1pullRequestPollingTime)
        .withPullRequest(10, repo1pullRequestCreateDate, "feature-branch", "main-branch", "feature-commit-xyz-1")

        .forRepository("githubOrg/multi-2", SourceControlProvider.GITHUB)
        .withApplication("github2", "main-branch")
        .withPollingTime(repo2pullRequestPollingTime)
        .withPullRequest(20, repo2pullRequestCreateDate, "R2-feature-branch", "main-branch", "feature-commit-abc-2")

        .build();

    // when:
    Date before = new Date();
    pollingService.fetchAndSendPullRequestsForCommenting();
    Date after = new Date();

    // then: pull requests are persisted and events emitted
    ArgumentCaptor<SourceControlPullRequest> sourceControlPullRequestArgumentCaptor =
        ArgumentCaptor.forClass(SourceControlPullRequest.class);
    verify(mockSourceControlPullRequestDAO, times(2)).insert(sourceControlPullRequestArgumentCaptor.capture());
    List<SourceControlPullRequest> sourceControlPullRequests = sourceControlPullRequestArgumentCaptor.getAllValues();
    assertSourceControlPullRequest(sourceControlPullRequests.get(0), "https://domain.com/githuborg/multi-1", 10,
        "feature-commit-xyz-1", "feature-branch", repo1pullRequestCreateDate, before, after);
    assertSourceControlPullRequest(sourceControlPullRequests.get(1), "https://domain.com/githuborg/multi-2", 20,
        "feature-commit-abc-2", "R2-feature-branch", repo2pullRequestCreateDate, before, after);

    verify(sourceControlEventPublisher, times(2)).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 2 pull request(s) for org 'githubOrg' since " + repo1pullRequestPollingTime),
        info(
            "Sent pull request discovered event for application 'github1' with PR# '10' and commit " +
                "'feature-commit-xyz-1'"),
        debug("Pull request polling time updated for 'https://domain.com/githubOrg/multi-1'"),
        info(
            "Sent pull request discovered event for application 'github2' with PR# '20' and commit " +
                "'feature-commit-abc-2'"),
        debug("Pull request polling time updated for 'https://domain.com/githubOrg/multi-2'")
    );
  }

  private class TestablePullRequestPollingServiceBuilder
  {
    private List<MockRepo> mockRepoList = new ArrayList<>();

    private Map<String, List<Application>> repoApplications = new HashMap<>();

    private MockRepo currentMockRepo;

    @Mock
    private ApplicationDAO mockApplicationDAO;

    @Mock
    private PolicyEvaluationDAO mockPolicyEvaluationDAO;

    @Mock
    private SourceControlDAO mockSourceControlDAO;

    @Mock
    private SourceControlInstanceManager mockSourceControlInstanceManager;

    @Mock
    private SourceControlUtils mockSourceControlUtils;

    @Mock
    private GitClientFactory mockGitClientFactory;

    @Mock
    private PullRequestRepositoryValidator mockPullRequestRepositoryValidator;

    @Mock
    private IqForScmLicenseChecker mockLicenseChecker;

    private Class<? extends Exception> thrownException;

    private boolean pullRequestCommentingSupported = true;

    PullRequestPollingService build() throws IOException {
      MockitoAnnotations.openMocks(this);

      List<PullRequest> allPullRequests = new ArrayList<>();

      List<SourceControl> sourceControlList = new ArrayList<>();
      for (MockRepo mockRepo : mockRepoList) {
        doReturn(mockRepo.mockGitApiClient).when(mockGitClientFactory).createApiClient(mockRepo.gitRepositoryInfo);
        doReturn(mockClientMap.get(mockRepo.orgAndRepoName)).when(mockGitClientFactory)
            .createPullRequestInfoClient(mockRepo.gitRepositoryInfo);

        if (null != mockRepo.sourceControl) {
          sourceControlList.add(mockRepo.sourceControl);
          doReturn(mockRepo.sourceControl).when(mockSourceControlDAO).getById(mockRepo.sourceControl.getId());
        }
        doReturn(buildSourceControlList(mockRepo)).when(mockSourceControlDAO)
            .getByRepositoryOwnerAndName(eq(mockRepo.orgAndRepoName));
        doReturn(mockRepo.gitRepositoryInfo).when(mockSourceControlUtils)
            .getGitRepositoryInfoForApplication(eq(mockRepo.applicationId));

        String repositoryURL = mockRepo.gitRepositoryInfo.getRepositoryUrl();
        doReturn(repoApplications.get(repositoryURL)).when(mockApplicationDAO)
            .getByRepositoryUrl(repositoryURL);

        doReturn(false).when(mockSourceControlUtils).isScmEnabled((GitRepositoryInfo) null);
        doReturn(true).when(mockSourceControlUtils).isScmEnabled(mockRepo.gitRepositoryInfo);
        doReturn(mockRepo.gitRepositoryInfo.repositoryUrl.contains("bitbucket.org"))
            .when(mockSourceControlUtils).isBitbucketCloud(mockRepo.gitRepositoryInfo);

        if (null != mockRepo.gitRepositoryInfo) {
          ProjectUri projectUri = new SimpleProjectUri(mockRepo.gitRepositoryInfo.repositoryUrl);
          doReturn(projectUri).when(mockRepo.mockGitApiClient).getProjectUri();
        }

        if (null != mockRepo.gitRepositoryInfo &&
            mockRepo.gitRepositoryInfo.provider.supportsOrganizationWidePullRequestQueries()) {
          allPullRequests.addAll(mockRepo.pullRequests);
          doReturn(allPullRequests).when(mockClientMap.get(mockRepo.orgAndRepoName))
              .getPullRequestsSince(any(), any(OffsetDateTime.class), anyInt());
        }
        else {
          doReturn(mockRepo.pullRequests).when(mockClientMap.get(mockRepo.orgAndRepoName))
              .getPullRequestsSince(any(), any(OffsetDateTime.class), anyInt());
        }

        if (thrownException != null) {
          doThrow(UnsupportedOperationException.class).when(mockPullRequestRepositoryValidator)
              .isInternalRepository(eq(mockRepo.gitRepositoryInfo));
        }
        else {
          doReturn(mockRepo.isGitRepositoryInternal).when(mockPullRequestRepositoryValidator)
              .isInternalRepository(eq(mockRepo.gitRepositoryInfo));
        }
        mockRepo.pullRequests.forEach(pullRequest -> pullRequest.setRepositoryPrivate(mockRepo.isGitRepositoryPrivate));

        doReturn(true).when(mockSourceControlInstanceManager).canPoll();

        doReturn(pullRequestCommentingSupported).when(mockLicenseChecker).isPullRequestCommentingSupported();

        if (mockRepo.hasPolicyEvaluation) {
          doReturn(new PolicyEvaluation()).when(mockPolicyEvaluationDAO)
              .getLastByApplicationAndCommitHash(any(), any());
        }
      }

      // setup next repo to poll sequence
      sourceControlList.add(null);
      if (sourceControlList.size() > 1) {
        SourceControl[] sourceControlArray =
            sourceControlList.subList(1, sourceControlList.size() - 1).toArray(new SourceControl[0]);
        doReturn(sourceControlList.get(0), (Object[]) sourceControlArray)
            .when(mockSourceControlDAO).getNextRepositoryToPoll();
      }
      else {
        doReturn(sourceControlList.get(0)).when(mockSourceControlDAO).getNextRepositoryToPoll();
      }

      return new PullRequestPollingService(mockApplicationDAO, mockPolicyEvaluationDAO, mockSourceControlDAO,
          mockSourceControlPullRequestDAO, sourceControlEventPublisher, mockSourceControlUtils, mockGitClientFactory,
          mockPullRequestRepositoryValidator, mockSourceControlInstanceManager, mockLicenseChecker);
    }

    private List<SourceControl> buildSourceControlList(MockRepo mockRepo) {
      return null != mockRepo.sourceControl ? ImmutableList.of(mockRepo.sourceControl) : new ArrayList<>();
    }

    TestablePullRequestPollingServiceBuilder forRepository(
        String orgAndRepoName,
        SourceControlProvider provider)
    {
      return forRepository("https://domain.com/" + orgAndRepoName, orgAndRepoName, provider);
    }

    TestablePullRequestPollingServiceBuilder forRepository(
        String url,
        String orgAndRepoName,
        SourceControlProvider provider)
    {
      currentMockRepo = new MockRepo(orgAndRepoName);
      currentMockRepo.repositoryUrl = url;
      currentMockRepo.sourceControlProvider = provider;
      mockRepoList.add(currentMockRepo);
      mockClientMap.put(orgAndRepoName, mock(PullRequestInfoProvider.class));

      return this;
    }

    TestablePullRequestPollingServiceBuilder withApplication(String applicationId, String defaultBranch) {
      currentMockRepo.applicationId = applicationId;
      List<Application> repoApps =
          repoApplications.computeIfAbsent(currentMockRepo.repositoryUrl, appList -> new ArrayList<>());
      Application application = new Application(applicationId, applicationId, null);
      application.setId(applicationId);
      repoApps.add(application);

      String username = currentMockRepo.sourceControlProvider.requiresUsername() ? "username" : null;
      currentMockRepo.sourceControl =
          new SourceControl(applicationId, currentMockRepo.repositoryUrl, username, "token",
              currentMockRepo.sourceControlProvider, true, true, defaultBranch, false, false, null);
      currentMockRepo.sourceControl.setId(UUID.randomUUID().toString());
      currentMockRepo.gitRepositoryInfo = new GitRepositoryInfo(currentMockRepo.repositoryUrl, username, "token",
          currentMockRepo.sourceControlProvider, defaultBranch, true, true);
      return this;
    }

    TestablePullRequestPollingServiceBuilder withPollingTime(Date date) {
      currentMockRepo.sourceControl.setPullRequestPollTime(date);
      return this;
    }

    TestablePullRequestPollingServiceBuilder withPolicyEvaluation() {
      currentMockRepo.hasPolicyEvaluation = true;
      return this;
    }

    TestablePullRequestPollingServiceBuilder withId(String id) {
      currentMockRepo.sourceControl.setId(id);
      return this;
    }

    TestablePullRequestPollingServiceBuilder withPullRequest(
        int id,
        Date created,
        String headBranch,
        String baseBranch,
        String headCommit)
    {
      PullRequest pullRequest;
      switch (currentMockRepo.gitRepositoryInfo.provider) {
        case GITLAB:
          pullRequest = new GitlabMergeRequestResponse();
          break;
        default:
          pullRequest = new GithubPullRequest();
          break;
      }
      pullRequest.setNumber(id);
      pullRequest.setCreated(created);
      pullRequest.setHead(headBranch);
      pullRequest.setBase(baseBranch);
      pullRequest.setRepository(currentMockRepo.repositoryUrl);
      pullRequest.setUrl(currentMockRepo.repositoryUrl);
      pullRequest.setRepositoryPrivate(currentMockRepo.isGitRepositoryPrivate);
      pullRequest.setHeadCommitHash(headCommit);
      currentMockRepo.pullRequests.add(pullRequest);
      return this;
    }

    TestablePullRequestPollingServiceBuilder withGitRepositoryPrivate(boolean isGitRepositoryPrivate) {
      currentMockRepo.isGitRepositoryPrivate = isGitRepositoryPrivate;
      return this;
    }

    TestablePullRequestPollingServiceBuilder withGitRepositoryInternal(boolean isGitRepositoryInternal) {
      currentMockRepo.isGitRepositoryInternal = isGitRepositoryInternal;
      return this;
    }

    public TestablePullRequestPollingServiceBuilder unlicensed() {
      pullRequestCommentingSupported = false;
      return this;
    }
  }

  private class MockRepo
  {
    private GitApiClient mockGitApiClient = mock(GitApiClient.class);

    final String orgAndRepoName;

    String applicationId;

    String repositoryUrl;

    boolean isGitRepositoryInternal = false;

    boolean isGitRepositoryPrivate = true;

    SourceControlProvider sourceControlProvider;

    SourceControl sourceControl;

    GitRepositoryInfo gitRepositoryInfo;

    List<PullRequest> pullRequests = new ArrayList<>();

    boolean hasPolicyEvaluation;

    MockRepo(String orgAndRepoName) {
      this.orgAndRepoName = orgAndRepoName;
    }
  }
}
