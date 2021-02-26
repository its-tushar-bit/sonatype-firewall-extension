/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.List;
import java.util.Optional;

import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.model.CommitInformation;
import com.sonatype.nexus.scm.api.model.PullRequest;
import com.sonatype.nexus.scm.gitlab.dto.GitlabMergeRequestResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PullRequestPolicyEvaluationResolverTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private GitCommitHistoryService mockGitCommitHistoryService;

  @Mock
  private PullRequestInfoClient mockPullRequestInfoClient;

  public PullRequestPolicyEvaluationResolverTest() {
    super(PullRequestPolicyEvaluationResolver.class);
  }

  @Before
  @Override
  public void setup() {
    super.setup();
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testResolveForPolicyEvaluation_missingFeatureBranchPolicyEvaluation() {
    // given: no available policy eval for the given feature branch policy eval ID
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .build();

    // when: resolve policy evaluations
    List<PullRequestPolicyEvaluationsDTO> policyEvaluationsDTOs = pullRequestPolicyEvaluationResolver
        .resolveForPolicyEvaluation("appId", gitRepositoryInfo, "eval123", "commit123");

    // then: result is empty
    assertThat(policyEvaluationsDTOs).isEmpty();

    // and that: NO attempt was made to update default branch commit history
    verify(mockPullRequestInfoClient, never()).getCommitInfoFromScm(gitRepositoryInfo, "commit123");
    verify(mockGitCommitHistoryService, never()).updateCommitHistoryForCommits(any(), any());
  }

  @Test
  public void testResolveForPolicyEvaluation_missingDefaultBranchPolicyEvaluation() {
    // given: a feature branch policy eval but no default branch policy eval
    final String applicationId = "app1";
    final String featureBranchPolicyEvaluationId = "feature-policy-1";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .withFeatureBranchPolicyEvaluation(applicationId, featureBranchPolicyEvaluationId)
        .build();

    // when: resolve policy evaluations
    List<PullRequestPolicyEvaluationsDTO> policyEvaluationsDTOs = pullRequestPolicyEvaluationResolver
        .resolveForPolicyEvaluation(applicationId, gitRepositoryInfo, featureBranchPolicyEvaluationId, "commit123");

    // then: result is empty
    assertThat(policyEvaluationsDTOs).isEmpty();

    // and that: attempt was made to update default branch commit history
    verify(mockPullRequestInfoClient, times(1)).getCommitInfoFromScm(gitRepositoryInfo, "commit123");
    verify(mockGitCommitHistoryService, times(1)).updateCommitHistoryForCommits(any(), any());
    assertThatLogMessagesEqual(
        debug("0 base branch commits to process for application 'app1'")
    );
  }

  @Test
  public void testResolveForPolicyEvaluation_haveNeededPolicyEvaluations() {
    // given: a feature branch policy eval but no default branch policy eval
    final String applicationId = "app2";
    final String defaultBranchPolicyEvaluationId = "default-policy-2";
    final String featureBranchPolicyEvaluationId = "feature-policy-2";
    final String featureCommit = "commit123";
    final String featureBranchName = "feature-branch";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .withDefaultBranchPolicyEvaluation(applicationId, defaultBranchPolicyEvaluationId)
        .withFeatureBranchPolicyEvaluation(applicationId, featureBranchPolicyEvaluationId)
        .withPullRequest(2, featureBranchName, featureCommit, true)
        .build();

    // when: resolve policy evaluations
    List<PullRequestPolicyEvaluationsDTO> policyEvaluationsDTOs = pullRequestPolicyEvaluationResolver
        .resolveForPolicyEvaluation(applicationId, gitRepositoryInfo, featureBranchPolicyEvaluationId, featureCommit);

    // then: we have a PR we can comment on
    assertThat(policyEvaluationsDTOs.size()).isEqualTo(1);
    PullRequestPolicyEvaluationsDTO dto = policyEvaluationsDTOs.get(0);
    assertThat(dto.getApplicationId()).isEqualTo(applicationId);
    assertThat(dto.getFeatureBranchName()).isEqualTo(featureBranchName);
    assertThat(dto.getPullRequestHeadCommit()).isEqualTo(featureCommit);
    assertThat(dto.getDefaultBranchPolicyEvaluationId()).isEqualTo(defaultBranchPolicyEvaluationId);
    assertThat(dto.getFeatureBranchPolicyEvaluationId()).isEqualTo(featureBranchPolicyEvaluationId);
    assertThatLogMessagesEqual(
        debug("0 base branch commits to process for application 'app2'")
    );
  }

  @Test
  public void testResolveForPullRequest_missingFeatureBranchPolicyEvaluation() {
    // given: no available policy eval for the pull request head commit
    final GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();
    final int pullRequestNumber = 3;

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .build();

    // when: resolve policy evaluations
    PullRequestPolicyEvaluationsDTO policyEvaluationsDTO = pullRequestPolicyEvaluationResolver
        .resolveForPullRequest("appId", gitRepositoryInfo, pullRequestNumber, "eval123", "commit123");

    // then: result is empty
    assertThat(policyEvaluationsDTO).isNull();

    // and that: NO attempt was made to update default branch commit history
    verify(mockPullRequestInfoClient, never()).getCommitInfoFromScm(gitRepositoryInfo, "commit123");
    verify(mockGitCommitHistoryService, never()).updateCommitHistoryForCommits(any(), any());
  }

  @Test
  public void testResolveForPullRequest_missingDefaultBranchPolicyEvaluation() {
    // given: a feature branch policy eval but no default branch policy eval
    final String applicationId = "app1";
    final String featureBranchPolicyEvaluationId = "feature-policy-1";
    final String commitHash = "commit123";
    final String featureBranchName = "feature-branch";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .withFeatureBranchPolicyEvaluationForCommit(applicationId, featureBranchPolicyEvaluationId, commitHash)
        .build();

    // when: resolve policy evaluations
    PullRequestPolicyEvaluationsDTO policyEvaluationsDTO = pullRequestPolicyEvaluationResolver
        .resolveForPullRequest(applicationId, gitRepositoryInfo, 2, featureBranchName, commitHash);

    // then: result is empty
    assertThat(policyEvaluationsDTO).isNull();

    // and that: attempt was made to update default branch commit history
    verify(mockPullRequestInfoClient, times(1)).getCommitInfoFromScm(gitRepositoryInfo, commitHash);
    verify(mockGitCommitHistoryService, times(1)).updateCommitHistoryForCommits(any(), any());
    assertThatLogMessagesEqual(
        debug("0 base branch commits to process for application 'app1'"),
        warn("no policy evaluation for base branch, skipping PR commenting for application 'app1' pull request '2'")
    );
  }

  @Test
  public void testResolveForPullRequest_haveNeededPolicyEvaluations() {
    // given: a feature branch policy eval but no default branch policy eval
    final String applicationId = "app2";
    final String defaultBranchPolicyEvaluationId = "default-policy-3";
    final String featureBranchPolicyEvaluationId = "feature-policy-3";
    final String featureCommit = "commit123";
    final String featureBranchName = "feature-branch";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .withDefaultBranchPolicyEvaluation(applicationId, defaultBranchPolicyEvaluationId)
        .withFeatureBranchPolicyEvaluationForCommit(applicationId, featureBranchPolicyEvaluationId, featureCommit)
        .withPullRequest(2, featureBranchName, featureCommit, true)
        .build();

    // when: resolve policy evaluations
    PullRequestPolicyEvaluationsDTO policyEvaluationsDTO = pullRequestPolicyEvaluationResolver
        .resolveForPullRequest(applicationId, gitRepositoryInfo, 4, featureBranchName, featureCommit);

    // then: we have a PR we can comment on
    assertThat(policyEvaluationsDTO).isNotNull();
    assertThat(policyEvaluationsDTO.getApplicationId()).isEqualTo(applicationId);
    assertThat(policyEvaluationsDTO.getFeatureBranchName()).isEqualTo(featureBranchName);
    assertThat(policyEvaluationsDTO.getPullRequestHeadCommit()).isEqualTo(featureCommit);
    assertThat(policyEvaluationsDTO.getDefaultBranchPolicyEvaluationId()).isEqualTo(defaultBranchPolicyEvaluationId);
    assertThat(policyEvaluationsDTO.getFeatureBranchPolicyEvaluationId()).isEqualTo(featureBranchPolicyEvaluationId);
  }

  private GitRepositoryInfo createDefaultGitRepositoryInfo() {
    return new GitRepositoryInfo("https://gitlab.com/test/project1", "user", "token", SourceControlProvider.GITLAB,
        "master", true, true);
  }

  private class TestablePolicyEvaluationResolver
  {
    @Mock
    private PolicyEvaluationDAO mockPolicyEvaluationDAO;

    @Mock
    private PullRequestEligibilityValidator mockPullRequestEligibilityValidator;

    private CommitInformation commitInformation = new CommitInformation();

    TestablePolicyEvaluationResolver() {
      MockitoAnnotations.openMocks(this);
    }

    private TestablePolicyEvaluationResolver withDefaultBranchPolicyEvaluation(
        String applicationId,
        String policyEvaluationId)
    {
      PolicyEvaluation policyEvaluation = new PolicyEvaluation();
      policyEvaluation.setApplicationId(applicationId);
      policyEvaluation.setId(policyEvaluationId);
      doReturn(Optional.of(policyEvaluation)).when(mockGitCommitHistoryService)
          .getLatestPolicyEvaluationForApplicationBaseBranch(applicationId);
      return this;
    }

    private TestablePolicyEvaluationResolver withFeatureBranchPolicyEvaluation(
        String applicationId,
        String policyEvaluationId)
    {
      PolicyEvaluation policyEvaluation = new PolicyEvaluation();
      policyEvaluation.setApplicationId(applicationId);
      policyEvaluation.setId(policyEvaluationId);
      doReturn(policyEvaluation).when(mockPolicyEvaluationDAO).getById(policyEvaluationId);

      return this;
    }

    private TestablePolicyEvaluationResolver withFeatureBranchPolicyEvaluationForCommit(
        String applicationId,
        String policyEvaluationId,
        String commitHash)
    {
      PolicyEvaluation policyEvaluation = new PolicyEvaluation();
      policyEvaluation.setApplicationId(applicationId);
      policyEvaluation.setId(policyEvaluationId);
      policyEvaluation.setCommitHash(commitHash);

      doReturn(policyEvaluation).when(mockPolicyEvaluationDAO)
          .getLastByApplicationAndCommitHash(applicationId, commitHash);

      return this;
    }

    private TestablePolicyEvaluationResolver withPullRequest(
        int pullRequestNumber,
        String branchName,
        String commitHash,
        boolean isEligibleForCommenting)
    {
      PullRequest pullRequest = new GitlabMergeRequestResponse();
      pullRequest.setNumber(pullRequestNumber);
      pullRequest.setHead(branchName);
      pullRequest.setHeadCommitHash(commitHash);

      doReturn(isEligibleForCommenting).when(mockPullRequestEligibilityValidator)
          .isPullRequestEligibleForCommenting(any(), any(), any(), any());

      commitInformation.addPullRequest(pullRequest);
      return this;
    }

    PullRequestPolicyEvaluationResolver build() {
      doReturn(commitInformation).when(mockPullRequestInfoClient).getCommitInfoFromScm(any(), any());

      return new PullRequestPolicyEvaluationResolver(
          mockGitCommitHistoryService,
          mockPolicyEvaluationDAO,
          mockPullRequestEligibilityValidator,
          mockPullRequestInfoClient);
    }
  }
}
