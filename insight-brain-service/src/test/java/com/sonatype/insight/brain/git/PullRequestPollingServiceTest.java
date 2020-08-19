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
import java.util.Optional;

import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventService;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.PullRequestInfoProvider;
import com.sonatype.nexus.scm.api.model.ProjectUri;
import com.sonatype.nexus.scm.api.model.PullRequest;
import com.sonatype.nexus.scm.common.SimpleProjectUri;
import com.sonatype.nexus.scm.github.dto.GithubPullRequest;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PullRequestPollingServiceTest
    extends VerifiableLoggingTestBase
{
  @Mock
  SourceControlEventService mockSourceControlEventService;

  @Mock
  private PullRequestInfoProvider mockClient;

  public PullRequestPollingServiceTest() {
    super(PullRequestPollingService.class);
  }

  @Before
  @Override
  public void setup() {
    MockitoAnnotations.initMocks(this);
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
    verify(mockSourceControlEventService, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual();
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_noSourcePolicyEvals() throws IOException {
    // given: missing source policy eval
    Date pullRequestCreateDate = new Date(System.currentTimeMillis() - 1000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("app1", "org/repo", SourceControlProvider.GITHUB)
        .withPullRequest(10, pullRequestCreateDate, "feature-branch")
        .build();

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: event emitted
    verify(mockSourceControlEventService, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 1 pull request(s) for org 'org' and repo 'none specified' since " + pullRequestCreateDate),
        debug("Policy evaluation not yet available for 'org/repo' pull request '10'"),
        debug("Pull request polling time updated for 'org/repo'")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_pullRequestIsForBaseBranch() throws IOException {
    // given: necessary ingredients to emit a discovered pull request event
    Date pullRequestCreateDate = new Date(System.currentTimeMillis() - 1000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("app1", "org/repo", SourceControlProvider.GITHUB)
        .withPullRequest(10, pullRequestCreateDate, "master")
        .withSourcePolicyEvaluation("app1", "spe1")
        .build();

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: event emitted
    verify(mockSourceControlEventService, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 1 pull request(s) for org 'org' and repo 'none specified' since " + pullRequestCreateDate),
        debug("application 'app1' pull request '10' is for the base branch, skipping commenting for this PR"),
        debug("Pull request polling time updated for 'org/repo'")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_shouldPostEvent() throws IOException {
    // given: necessary ingredients to emit a discovered pull request event
    Date pullRequestCreateDate = new Date(System.currentTimeMillis() - 1000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("app1", "org/repo", SourceControlProvider.GITHUB)
        .withPullRequest(10, pullRequestCreateDate, "feature-branch")
        .withSourcePolicyEvaluation("app1", "spe1")
        .build();

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: event emitted
    verify(mockSourceControlEventService, times(1)).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 1 pull request(s) for org 'org' and repo 'none specified' since " + pullRequestCreateDate),
        info("Sent pull request discovered event for application 'app1' with PR# '10' and policy evaluation 'spe1'"),
        debug("Pull request polling time updated for 'org/repo'")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_shouldPostEventNotPrivateButInternal() throws IOException {
    // given: necessary ingredients to emit a discovered pull request event
    Date pullRequestCreateDate = new Date(System.currentTimeMillis() - 1000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("app1", "org/repo", SourceControlProvider.GITHUB)
        .withPullRequest(10, pullRequestCreateDate, "feature-branch")
        .withSourcePolicyEvaluation("app1", "spe1")
        .withGitRepositoryPrivate(false)
        .withGitRepositoryInternal(true)
        .build();

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: event emitted
    verify(mockSourceControlEventService, times(1)).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 1 pull request(s) for org 'org' and repo 'none specified' since " + pullRequestCreateDate),
        info("Sent pull request discovered event for application 'app1' with PR# '10' and policy evaluation 'spe1'"),
        debug("Pull request polling time updated for 'org/repo'")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_featureFlagOff() throws Exception {
    // given:
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("app1", "org/repo", SourceControlProvider.GITLAB)
        .withExperimentalFeatureFlag(false)
        .build();

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: no events emitted
    verify(mockSourceControlEventService, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("GITLAB is not currently supported for pull request commenting on repository https://domain.com/org/repo")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_bitbucketCloudNotSupported() throws IOException {
    // given:
    String repositoryUrl = "https://bitbucket.org/org/repo";
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository(repositoryUrl, "app1", "org/repo", SourceControlProvider.BITBUCKET)
        .build();

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: no events emitted
    verify(mockSourceControlEventService, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("BITBUCKET is not currently supported for pull request commenting on repository " + repositoryUrl)
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_RepositoryNotPrivate() throws IOException {
    // given: necessary ingredients to emit a discovered pull request event with a non-private repo
    Date pullRequestCreateDate = new Date(System.currentTimeMillis() - 1000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("app1", "org/repo", SourceControlProvider.GITHUB)
        .withPullRequest(10, pullRequestCreateDate, "feature-branch")
        .withSourcePolicyEvaluation("app1", "spe1")
        .withGitRepositoryPrivate(false)
        .build();

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: no events emitted
    verify(mockSourceControlEventService, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 1 pull request(s) for org 'org' and repo 'none specified' since " + pullRequestCreateDate),
        debug("Repository is not valid for pull requests, check that it is private: https://domain.com/org/repo"),
        debug("Pull request polling time updated for 'org/repo'")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_errorFetchingPullRequests() throws IOException {
    // given:
    Date pullRequestCreateDate = new Date(System.currentTimeMillis() - 1000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("app1", "org/repo", SourceControlProvider.GITHUB)
        .withId("sourceControl1")
        .withPullRequest(10, pullRequestCreateDate, "feature-branch")
        .withSourcePolicyEvaluation("app1", "spe1")
        .build();
    doThrow(new IOException("scm error"))
        .when(mockClient)
        .getPullRequestsSince(any(String.class), any(OffsetDateTime.class),
            eq(PullRequestPollingService.PULL_REQUESTS_PER_MONITOR_CYCLE));

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: no events emitted
    verify(mockSourceControlEventService, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        warn(
            "Could not fetch pull requests for org 'org' and repo 'none specified'; will retry in 5 minutes.  Please " +
                "check that the configured project url https://domain.com/org/repo is correct, that it is for " +
                "'github' and that the API token is valid")
    );
  }

  private class TestablePullRequestPollingServiceBuilder
  {
    @Mock
    private SourceControlDAO mockSourceControlDAO;

    @Mock
    private PolicyEvaluationDAO mockPolicyEvaluationDAO;

    @Mock
    private GitCommitHistoryService mockGitCommitHistoryService;

    @Mock
    private SourceControlUtils mockSourceControlUtils;

    @Mock
    private GitClientFactory mockGitClientFactory;

    @Mock
    private GitApiClient mockGitApiClient;

    @Mock
    private PullRequestRepositoryValidator mockPullRequestRepositoryValidator;

    private SourceControl sourceControl;

    private GitRepositoryInfo gitRepositoryInfo;

    private PolicyEvaluation sourcePolicyEvaluation;

    private PolicyEvaluation targetPolicyEvaluation;

    private List<PullRequest> pullRequests = new ArrayList<>();

    private String orgAndRepoName;

    private boolean isGitRepositoryPrivate = true;

    private boolean isGitRepositoryInternal = false;

    private boolean mrCommenting = true;

    private Class<? extends Exception> thrownException;

    PullRequestPollingService build() throws IOException {
      MockitoAnnotations.initMocks(this);

      doReturn(mockGitApiClient).when(mockGitClientFactory).createApiClient(gitRepositoryInfo);
      doReturn(mockClient).when(mockGitClientFactory).createPullRequestInfoClient(gitRepositoryInfo);

      doReturn(sourceControl, (SourceControl) null).when(mockSourceControlDAO).getNextRepositoryToPoll();
      if (null != sourceControl) {
        doReturn(sourceControl).when(mockSourceControlDAO).getById(sourceControl.getId());
      }
      doReturn(buildSourceControlList()).when(mockSourceControlDAO).getByRepositoryOwnerAndName(any());
      doReturn(gitRepositoryInfo).when(mockSourceControlUtils).getGitRepositoryInfoForApplication(any());

      doReturn(false).when(mockSourceControlUtils).isScmEnabled((GitRepositoryInfo) null);
      doReturn(true).when(mockSourceControlUtils).isScmEnabled(gitRepositoryInfo);

      doReturn(buildSourcePolicyEvaluationList()).when(mockPolicyEvaluationDAO)
          .getLastByCommitHashPerApplication(any());
      doReturn(Optional.ofNullable(targetPolicyEvaluation)).when(mockGitCommitHistoryService)
          .getLatestPolicyEvaluationForApplicationBaseBranch(any());

      if (null != gitRepositoryInfo) {
        ProjectUri projectUri = new SimpleProjectUri(gitRepositoryInfo.repositoryUrl);
        doReturn(projectUri).when(mockGitApiClient).getProjectUri();
      }

      doReturn(pullRequests).when(mockClient)
          .getPullRequestsSince(any(), any(OffsetDateTime.class), anyInt());

      if (thrownException != null) {
        doThrow(UnsupportedOperationException.class).when(mockPullRequestRepositoryValidator)
            .isInternalRepository(eq(gitRepositoryInfo));
      }
      else {
        doReturn(isGitRepositoryInternal).when(mockPullRequestRepositoryValidator)
            .isInternalRepository(eq(gitRepositoryInfo));
      }
      pullRequests.forEach(pullRequest -> pullRequest.setRepositoryPrivate(isGitRepositoryPrivate));

      return new PullRequestPollingService(mockSourceControlDAO, mockSourceControlEventService, mockPolicyEvaluationDAO,
          mockGitCommitHistoryService, mockSourceControlUtils, mockGitClientFactory,
          mockPullRequestRepositoryValidator, getInsightConfig());
    }

    private List<SourceControl> buildSourceControlList() {
      return null != sourceControl ? ImmutableList.of(sourceControl) : new ArrayList<>();
    }

    private List<PolicyEvaluation> buildSourcePolicyEvaluationList() {
      return null != sourcePolicyEvaluation ? ImmutableList.of(sourcePolicyEvaluation) : new ArrayList<>();
    }

    TestablePullRequestPollingServiceBuilder forRepository(
        String applicationId,
        String orgAndRepoName,
        SourceControlProvider provider)
    {
      return forRepository("https://domain.com/" + orgAndRepoName, applicationId, orgAndRepoName, provider);
    }

    TestablePullRequestPollingServiceBuilder forRepository(
        String url,
        String applicationId,
        String orgAndRepoName,
        SourceControlProvider provider)
    {
      String username = provider.requiresUsername() ? "username" : null;
      sourceControl = new SourceControl(applicationId, url, username, "token", provider, true, true, "master");
      sourceControl.setPullRequestPollTime(new Date());
      gitRepositoryInfo = new GitRepositoryInfo(url, username, "token", provider, "master", true, true);
      this.orgAndRepoName = orgAndRepoName;
      return this;
    }

    TestablePullRequestPollingServiceBuilder withId(String id) {
      sourceControl.setId(id);
      return this;
    }

    TestablePullRequestPollingServiceBuilder withSourcePolicyEvaluation(
        String applicationId,
        String policyEvaluationId)
    {
      sourcePolicyEvaluation = new PolicyEvaluation();
      sourcePolicyEvaluation.setApplicationId(applicationId);
      sourcePolicyEvaluation.setId(policyEvaluationId);
      return this;
    }

    TestablePullRequestPollingServiceBuilder withPullRequest(int id, Date created, String headBranch) {
      PullRequest pullRequest = new GithubPullRequest();
      pullRequest.setNumber(id);
      pullRequest.setCreated(created);
      pullRequest.setHead(headBranch);
      pullRequest.setRepository(orgAndRepoName);
      pullRequest.setRepositoryPrivate(isGitRepositoryPrivate);
      pullRequests.add(pullRequest);
      return this;
    }

    TestablePullRequestPollingServiceBuilder withGitRepositoryPrivate(boolean isGitRepositoryPrivate) {
      this.isGitRepositoryPrivate = isGitRepositoryPrivate;
      return this;
    }

    TestablePullRequestPollingServiceBuilder withGitRepositoryInternal(boolean isGitRepositoryInternal) {
      this.isGitRepositoryInternal = isGitRepositoryInternal;
      return this;
    }

    TestablePullRequestPollingServiceBuilder withExperimentalFeatureFlag(boolean mrCommenting) {
      this.mrCommenting = mrCommenting;
      return this;
    }

    private InsightConfig getInsightConfig() {
      InsightConfig insightConfig = new InsightConfig();
      Map<String, Boolean> experimentalFeatures = new HashMap<>();
      experimentalFeatures.put("mrCommenting", mrCommenting);
      insightConfig.setExperimentalFeatures(experimentalFeatures);
      return insightConfig;
    }
  }
}
