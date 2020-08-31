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
import java.util.UUID;

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
import com.sonatype.nexus.scm.gitlab.dto.GitlabMergeRequestResponse;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PullRequestPollingServiceTest
    extends VerifiableLoggingTestBase
{
  @Mock
  SourceControlEventService mockSourceControlEventService;

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
    verify(mockSourceControlEventService, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual();
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_noSourcePolicyEvals() throws IOException {
    // given: missing source policy eval
    final Date pullRequestCreateDate = new Date();
    final Date pullRequestPollingTime = new Date(System.currentTimeMillis() - 1000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("app1", "orgNspe/repoNspe", SourceControlProvider.GITHUB)
        .withPollingTime(pullRequestPollingTime)
        .withPullRequest(10, pullRequestCreateDate, "feature-branch")
        .build();

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: event emitted
    verify(mockSourceControlEventService, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 1 pull request(s) for org 'orgNspe' since " + pullRequestPollingTime),
        debug("Policy evaluation not yet available for 'orgNspe/repoNspe' pull request '10'"),
        debug("Pull request polling time updated for 'orgNspe/repoNspe'")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_pullRequestIsForBaseBranch() throws IOException {
    // given: necessary ingredients to emit a discovered pull request event
    final Date pullRequestCreateDate = new Date();
    final Date pullRequestPollingTime = new Date(System.currentTimeMillis() - 1000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("app1", "orgBb/repoBb", SourceControlProvider.GITHUB)
        .withPollingTime(pullRequestPollingTime)
        .withPullRequest(10, pullRequestCreateDate, "master")
        .withSourcePolicyEvaluation("app1", "spe1")
        .build();

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: event emitted
    verify(mockSourceControlEventService, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 1 pull request(s) for org 'orgBb' since " + pullRequestPollingTime),
        debug("application 'app1' pull request '10' is for the base branch, skipping commenting for this PR"),
        debug("Pull request polling time updated for 'orgBb/repoBb'")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_shouldPostEvent() throws IOException {
    // given: necessary ingredients to emit a discovered pull request event
    final Date pullRequestCreateDate = new Date();
    final Date pullRequestPollingTime = new Date(System.currentTimeMillis() - 1000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("app1", "orgOk/repoOk", SourceControlProvider.GITHUB)
        .withPollingTime(pullRequestPollingTime)
        .withPullRequest(10, pullRequestCreateDate, "feature-branch")
        .withSourcePolicyEvaluation("app1", "spe1")
        .build();

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: event emitted
    verify(mockSourceControlEventService, times(1)).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 1 pull request(s) for org 'orgOk' since " + pullRequestPollingTime),
        info("Sent pull request discovered event for application 'app1' with PR# '10' and policy evaluation 'spe1'"),
        debug("Pull request polling time updated for 'orgOk/repoOk'")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_shouldPostEventNotPrivateButInternal() throws IOException {
    // given: necessary ingredients to emit a discovered pull request event
    final Date pullRequestCreateDate = new Date();
    final Date pullRequestPollingTime = new Date(System.currentTimeMillis() - 1000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("app1", "orgInt/repoInt", SourceControlProvider.GITHUB)
        .withPollingTime(pullRequestPollingTime)
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
        debug("Fetched 1 pull request(s) for org 'orgInt' since " + pullRequestPollingTime),
        info("Sent pull request discovered event for application 'app1' with PR# '10' and policy evaluation 'spe1'"),
        debug("Pull request polling time updated for 'orgInt/repoInt'")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_featureFlagOff() throws Exception {
    // given:
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("app1", "orgFfo/repoFfo", SourceControlProvider.GITLAB)
        .withExperimentalFeatureFlag(false)
        .build();

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: no events emitted
    verify(mockSourceControlEventService, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("GITLAB is not currently supported for pull request commenting on repository"
            + " https://domain.com/orgFfo/repoFfo")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_bitbucketCloudNotSupported() throws IOException {
    // given:
    String repositoryUrl = "https://bitbucket.org/orgBbcns/repoBbcns";
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository(repositoryUrl, "app1", "orgBbcns/repoBbcns", SourceControlProvider.BITBUCKET)
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
    final Date pullRequestCreateDate = new Date();
    final Date pullRequestPollingTime = new Date(System.currentTimeMillis() - 1000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("app1", "orgNp/repoNp", SourceControlProvider.GITHUB)
        .withPollingTime(pullRequestPollingTime)
        .withPullRequest(10, pullRequestCreateDate, "feature-branch")
        .withSourcePolicyEvaluation("app1", "spe1")
        .withGitRepositoryPrivate(false)
        .build();

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: no events emitted
    verify(mockSourceControlEventService, never()).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 1 pull request(s) for org 'orgNp' since " + pullRequestPollingTime),
        debug("Repository is not valid for pull requests, check that it is private: https://domain.com/orgNp/repoNp"),
        debug("Pull request polling time updated for 'orgNp/repoNp'")
    );
  }

  @Test
  public void testFetchAndSendPullRequestsForCommenting_errorFetchingPullRequests_GitHub() throws IOException {
    // given:
    Date pullRequestCreateDate = new Date(System.currentTimeMillis() - 1000);
    PullRequestPollingService pollingService = new TestablePullRequestPollingServiceBuilder()
        .forRepository("app1", "orgErr/repoErr", SourceControlProvider.GITHUB)
        .withPollingTime(new Date())
        .withId("sourceControl1")
        .withPullRequest(10, pullRequestCreateDate, "feature-branch")
        .withSourcePolicyEvaluation("app1", "spe1")
        .build();
    doThrow(new IOException("scm error"))
        .when(mockClientMap.get("orgErr/repoErr"))
        .getPullRequestsSince(any(String.class), any(OffsetDateTime.class),
            eq(PullRequestPollingService.PULL_REQUESTS_PER_MONITOR_CYCLE));

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: no events emitted
    verify(mockSourceControlEventService, never()).publishEvent(any(SourceControlEvent.class));
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
        .forRepository("app1", "orgErr/repoErr", SourceControlProvider.GITLAB)
        .withPollingTime(new Date())
        .withId("sourceControl1")
        .withPullRequest(10, pullRequestCreateDate, "feature-branch")
        .withSourcePolicyEvaluation("app1", "spe1")
        .build();
    doThrow(new IOException("scm error"))
        .when(mockClientMap.get("orgErr/repoErr"))
        .getPullRequestsSince(any(String.class), any(OffsetDateTime.class),
            eq(PullRequestPollingService.PULL_REQUESTS_PER_MONITOR_CYCLE));

    // when: fetch and send
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then: no events emitted
    verify(mockSourceControlEventService, never()).publishEvent(any(SourceControlEvent.class));
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

        .forRepository("app1", "org/multi-1", SourceControlProvider.GITLAB)
        .withPollingTime(repo1pullRequestPollingTime)
        .withPullRequest(10, repo1pullRequestCreateDate, "feature-branch")
        .withSourcePolicyEvaluation("app1", "spe1")

        .forRepository("app2", "org/multi-2", SourceControlProvider.GITLAB)
        .withPollingTime(repo2pullRequestPollingTime)
        .withPullRequest(20, repo2pullRequestCreateDate, "R2-feature-branch")
        .withSourcePolicyEvaluation("app2", "spe2")

        .build();

    // when:
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then:
    verify(mockSourceControlEventService, times(2)).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 1 pull request(s) for org 'org' repo 'multi-1' since " + repo1pullRequestPollingTime),
        debug("Fetched 1 pull request(s) for org 'org' repo 'multi-2' since " + repo2pullRequestPollingTime),
        info("Sent pull request discovered event for application 'app1' with PR# '10' and policy evaluation 'spe1'"),
        debug("Pull request polling time updated for 'org/multi-1'"),
        info("Sent pull request discovered event for application 'app2' with PR# '20' and policy evaluation 'spe2'"),
        debug("Pull request polling time updated for 'org/multi-2'")
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

        .forRepository("app1", "githubOrg/multi-1", SourceControlProvider.GITHUB)
        .withPollingTime(repo1pullRequestPollingTime)
        .withPullRequest(10, repo1pullRequestCreateDate, "feature-branch")
        .withSourcePolicyEvaluation("app1", "spe1")

        .forRepository("app2", "githubOrg/multi-2", SourceControlProvider.GITHUB)
        .withPollingTime(repo2pullRequestPollingTime)
        .withPullRequest(20, repo2pullRequestCreateDate, "R2-feature-branch")
        .withSourcePolicyEvaluation("app2", "spe2")

        .build();

    // when:
    pollingService.fetchAndSendPullRequestsForCommenting();

    // then:
    verify(mockSourceControlEventService, times(2)).publishEvent(any(SourceControlEvent.class));
    assertThatLogMessagesEqual(
        debug("Fetched 2 pull request(s) for org 'githubOrg' since " + repo1pullRequestPollingTime),
        info("Sent pull request discovered event for application 'app1' with PR# '10' and policy evaluation 'spe1'"),
        debug("Pull request polling time updated for 'githubOrg/multi-1'"),
        info("Sent pull request discovered event for application 'app2' with PR# '20' and policy evaluation 'spe2'"),
        debug("Pull request polling time updated for 'githubOrg/multi-2'")
    );
  }

  private class TestablePullRequestPollingServiceBuilder
  {
    private List<MockRepo> mockRepoList = new ArrayList<>();

    private MockRepo currentMockRepo;

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
    private PullRequestRepositoryValidator mockPullRequestRepositoryValidator;

    boolean mrCommenting = true;

    private Class<? extends Exception> thrownException;

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

        doReturn(false).when(mockSourceControlUtils).isScmEnabled((GitRepositoryInfo) null);
        doReturn(true).when(mockSourceControlUtils).isScmEnabled(mockRepo.gitRepositoryInfo);

        doReturn(buildSourcePolicyEvaluationList(mockRepo)).when(mockPolicyEvaluationDAO)
            .getLastByCommitHashPerApplication(mockRepo.commitHash);
        doReturn(Optional.ofNullable(mockRepo.targetPolicyEvaluation)).when(mockGitCommitHistoryService)
            .getLatestPolicyEvaluationForApplicationBaseBranch(mockRepo.applicationId);

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

      return new PullRequestPollingService(mockSourceControlDAO, mockSourceControlEventService, mockPolicyEvaluationDAO,
          mockGitCommitHistoryService, mockSourceControlUtils, mockGitClientFactory,
          mockPullRequestRepositoryValidator, getInsightConfig());
    }

    private List<SourceControl> buildSourceControlList(MockRepo mockRepo) {
      return null != mockRepo.sourceControl ? ImmutableList.of(mockRepo.sourceControl) : new ArrayList<>();
    }

    private List<PolicyEvaluation> buildSourcePolicyEvaluationList(MockRepo mockRepo) {
      return null != mockRepo.sourcePolicyEvaluation
          ? ImmutableList.of(mockRepo.sourcePolicyEvaluation) : new ArrayList<>();
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
      currentMockRepo = new MockRepo(orgAndRepoName);
      mockRepoList.add(currentMockRepo);
      mockClientMap.put(orgAndRepoName, mock(PullRequestInfoProvider.class));

      currentMockRepo.applicationId = applicationId;

      String username = provider.requiresUsername() ? "username" : null;
      currentMockRepo.sourceControl =
          new SourceControl(applicationId, url, username, "token", provider, true, true, "master");
      currentMockRepo.sourceControl.setId(UUID.randomUUID().toString());
      currentMockRepo.gitRepositoryInfo = new GitRepositoryInfo(url, username, "token", provider, "master", true, true);
      return this;
    }

    TestablePullRequestPollingServiceBuilder withPollingTime(Date date) {
      currentMockRepo.sourceControl.setPullRequestPollTime(date);
      return this;
    }

    TestablePullRequestPollingServiceBuilder withId(String id) {
      currentMockRepo.sourceControl.setId(id);
      return this;
    }

    TestablePullRequestPollingServiceBuilder withSourcePolicyEvaluation(
        String applicationId,
        String policyEvaluationId)
    {
      PolicyEvaluation policyEvaluation = new PolicyEvaluation();
      policyEvaluation.setApplicationId(applicationId);
      policyEvaluation.setId(policyEvaluationId);
      currentMockRepo.sourcePolicyEvaluation = policyEvaluation;
      return this;
    }

    TestablePullRequestPollingServiceBuilder withPullRequest(int id, Date created, String headBranch) {
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
      pullRequest.setRepository(currentMockRepo.orgAndRepoName);
      pullRequest.setRepositoryPrivate(currentMockRepo.isGitRepositoryPrivate);
      pullRequest.setHeadCommitHash(currentMockRepo.commitHash);
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

  private class MockRepo
  {
    private GitApiClient mockGitApiClient = mock(GitApiClient.class);

    final String orgAndRepoName;

    String applicationId;

    String commitHash = UUID.randomUUID().toString();

    boolean isGitRepositoryInternal = false;

    boolean isGitRepositoryPrivate = true;

    SourceControl sourceControl;

    GitRepositoryInfo gitRepositoryInfo;

    PolicyEvaluation sourcePolicyEvaluation;

    PolicyEvaluation targetPolicyEvaluation;

    List<PullRequest> pullRequests = new ArrayList<>();

    MockRepo(String orgAndRepoName) {
      this.orgAndRepoName = orgAndRepoName;
    }
  }
}
