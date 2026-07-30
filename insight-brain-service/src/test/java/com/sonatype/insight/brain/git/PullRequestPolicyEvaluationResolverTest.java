/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.git.utils.api.GitException;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.model.CommitInformation;
import com.sonatype.nexus.scm.api.model.PullRequest;
import com.sonatype.nexus.scm.github.dto.GithubPullRequest;
import com.sonatype.nexus.scm.gitlab.dto.GitlabMergeRequestResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
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

  @Mock
  private SourceControlScanService mockSourceControlScanService;

  @Mock
  private SourceControlUtils sourceControlUtils;

  private ApplicationDAO applicationDAO;

  private Application application;

  public PullRequestPolicyEvaluationResolverTest() {
    super(PullRequestPolicyEvaluationResolver.class);
  }

  @Before
  @Override
  public void setup() {
    super.setup();
    MockitoAnnotations.openMocks(this);
    applicationDAO = daoFactory.createApplicationDAO();
    application = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testResolveForPolicyEvaluation_missingFeatureBranchPolicyEvaluation() {
    // given: no available policy eval for the given feature branch policy eval ID
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .build();

    // when: resolve policy evaluations
    List<PullRequestPolicyEvaluationsDTO> policyEvaluationsDTOs = pullRequestPolicyEvaluationResolver
        .resolveForPolicyEvaluation(application.getId(), gitRepositoryInfo, "eval123", "commit123");

    // then: result is empty
    assertThat(policyEvaluationsDTOs).isEmpty();

    // and that: NO attempt was made to update default branch commit history
    verify(mockPullRequestInfoClient, never()).getCommitInfoFromScm(gitRepositoryInfo, "commit123");
    verify(mockGitCommitHistoryService, never()).updateCommitHistoryForCommits(any(), any());
  }

  @Test
  public void testResolveForPolicyEvaluation_missingDefaultBranchPolicyEvaluation() {
    // given: a feature branch policy eval but no default branch policy eval
    final String featureBranchPolicyEvaluationId = "feature-policy-1";
    final String featureCommit = "commit123";
    final String featureBranchName = "feature-branch";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .withFeatureBranchPolicyEvaluation(application.getId(), featureBranchPolicyEvaluationId, false)
        .withPullRequest(2, featureBranchName, featureCommit, true)
        .build();

    // when: resolve policy evaluations
    List<PullRequestPolicyEvaluationsDTO> policyEvaluationsDTOs = pullRequestPolicyEvaluationResolver
        .resolveForPolicyEvaluation(application.getId(), gitRepositoryInfo, featureBranchPolicyEvaluationId,
            "commit123");

    // then: result is empty
    assertThat(policyEvaluationsDTOs).isEmpty();

    // and that: attempt was made to update default branch commit history
    verify(mockPullRequestInfoClient, times(1)).getCommitInfoFromScm(gitRepositoryInfo, "commit123");
    verify(mockGitCommitHistoryService, times(1)).updateCommitHistoryForCommits(any(), any());
    assertThatLogMessagesEqual(
        debug("0 base branch commits to process for application '" + application.getId() + "'"),
        warn("no policy evaluation for base branch, skipping PR commenting for application '" + application.getId() +
            "' pull request '2'"));
  }

  @Test
  public void testResolveForPolicyEvaluation_noPRsForCommitHash() {
    // given: a feature branch policy eval but no PRs exist for the commit hash in the policy eval
    final String featureBranchPolicyEvaluationId = "feature-policy-1";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .withFeatureBranchPolicyEvaluation(application.getId(), featureBranchPolicyEvaluationId, false)
        .build();

    // when: resolve policy evaluations
    List<PullRequestPolicyEvaluationsDTO> policyEvaluationsDTOs = pullRequestPolicyEvaluationResolver
        .resolveForPolicyEvaluation(application.getId(), gitRepositoryInfo, featureBranchPolicyEvaluationId,
            "commit123");

    // then: result is empty
    assertThat(policyEvaluationsDTOs).isEmpty();

    // and that: attempt was made to update default branch commit history
    verify(mockPullRequestInfoClient, times(1)).getCommitInfoFromScm(gitRepositoryInfo, "commit123");
    verify(mockGitCommitHistoryService, times(1)).updateCommitHistoryForCommits(any(), any());
    assertThatLogMessagesEqual(
        debug("0 base branch commits to process for application '" + application.getId() + "'"));
  }

  @Test
  public void testResolveForPolicyEvaluation_haveNeededPolicyEvaluations() throws GitException, IOException {
    // given: default and feature branch policy evals
    final String defaultBranchPolicyEvaluationId = "default-policy-2";
    final String featureBranchPolicyEvaluationId = "feature-policy-2";
    final String featureCommit = "commit123";
    final String featureBranchName = "feature-branch";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .withDefaultBranchPolicyEvaluation(application.getId(), defaultBranchPolicyEvaluationId, false)
        .withFeatureBranchPolicyEvaluation(application.getId(), featureBranchPolicyEvaluationId, false)
        .withPullRequest(2, featureBranchName, featureCommit, true)
        .build();

    // when: resolve policy evaluations
    List<PullRequestPolicyEvaluationsDTO> policyEvaluationsDTOs = pullRequestPolicyEvaluationResolver
        .resolveForPolicyEvaluation(application.getId(), gitRepositoryInfo, featureBranchPolicyEvaluationId,
            featureCommit);

    // then: we have a PR we can comment on
    assertThat(policyEvaluationsDTOs.size()).isEqualTo(1);
    PullRequestPolicyEvaluationsDTO dto = policyEvaluationsDTOs.get(0);
    assertThat(dto.getApplicationId()).isEqualTo(application.getId());
    assertThat(dto.getFeatureBranchName()).isEqualTo(featureBranchName);
    assertThat(dto.getPullRequestHeadCommit()).isEqualTo(featureCommit);
    assertThat(dto.getTargetPolicyEvaluationId()).isEqualTo(defaultBranchPolicyEvaluationId);
    assertThat(dto.getFeatureBranchPolicyEvaluationId()).isEqualTo(featureBranchPolicyEvaluationId);
    assertThatLogMessagesEqual(
        debug("0 base branch commits to process for application '" + application.getId() + "'"));
  }

  @Test
  public void testResolveForPolicyEvaluation_haveNeededPolicyEvaluationsForCommit() throws GitException, IOException {
    // given: default and feature branch policy evals
    final String defaultBranchPolicyEvaluationId = "default-policy-5";
    final String policyEvaluationIdForCommit = "default-policy-6";
    final String featureBranchPolicyEvaluationId = "feature-policy-5";
    final String baseCommit = "baseCommit";
    final String featureCommit = "commit123";
    final String featureBranchName = "feature-branch";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .withDefaultBranchPolicyEvaluation(application.getId(), defaultBranchPolicyEvaluationId, false)
        .withBaseCommitPolicyEvaluation(application.getId(), baseCommit, policyEvaluationIdForCommit, false)
        .withFeatureBranchPolicyEvaluation(application.getId(), featureBranchPolicyEvaluationId, false)
        .withPullRequestWithBaseCommit(5, featureBranchName, baseCommit, featureCommit, true)
        .build();

    // when: resolve policy evaluations
    List<PullRequestPolicyEvaluationsDTO> policyEvaluationsDTOs = pullRequestPolicyEvaluationResolver
        .resolveForPolicyEvaluation(application.getId(), gitRepositoryInfo, featureBranchPolicyEvaluationId,
            featureCommit);

    // then: we have a PR we can comment on
    assertThat(policyEvaluationsDTOs.size()).isEqualTo(1);
    PullRequestPolicyEvaluationsDTO dto = policyEvaluationsDTOs.get(0);
    assertThat(dto.getApplicationId()).isEqualTo(application.getId());
    assertThat(dto.getFeatureBranchName()).isEqualTo(featureBranchName);
    assertThat(dto.getPullRequestHeadCommit()).isEqualTo(featureCommit);
    assertThat(dto.getTargetPolicyEvaluationId()).isEqualTo(policyEvaluationIdForCommit);
    assertThat(dto.getFeatureBranchPolicyEvaluationId()).isEqualTo(featureBranchPolicyEvaluationId);
    assertThatLogMessagesEqual(
        debug("0 base branch commits to process for application '" + application.getId() + "'"));
  }

  @Test
  public void testResolveForPullRequest_missingFeatureBranchPolicyEvaluation() throws GitException, IOException {
    // given: no available policy eval for the pull request head commit
    final GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();
    final int pullRequestNumber = 3;
    final String defaultBranchPolicyEvaluationId = "default-policy-1";

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .withDefaultBranchPolicyEvaluation(application.getId(), defaultBranchPolicyEvaluationId, true)
        .build();

    // when: resolve policy evaluations
    PullRequestPolicyEvaluationsDTO policyEvaluationsDTO = pullRequestPolicyEvaluationResolver
        .resolveForPullRequest(application.getId(), gitRepositoryInfo, pullRequestNumber,
            "eval123", "main", "commit123", null);

    // then: result is empty
    assertThat(policyEvaluationsDTO).isNull();
    assertThatLogMessagesEqual(
        debug("Cannot comment - missing feature branch policy evaluation for application " + application.getPublicId() +
            " repository https://gitlab.com/test/project1"));
  }

  @Test
  public void testResolveForPullRequest_missingDefaultBranchPolicyEvaluation() {
    // given: a feature branch policy eval but no default branch policy eval
    final String featureBranchPolicyEvaluationId = "feature-policy-1";
    final String commitHash = "commit123";
    final String featureBranchName = "feature-branch";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .withFeatureBranchPolicyEvaluationForCommit(application.getId(), featureBranchPolicyEvaluationId, commitHash,
            true)
        .build();

    // when: resolve policy evaluations
    PullRequestPolicyEvaluationsDTO policyEvaluationsDTO = pullRequestPolicyEvaluationResolver
        .resolveForPullRequest(application.getId(), gitRepositoryInfo, 2,
            featureBranchName, "main", commitHash, null);

    // then: result is empty
    assertThat(policyEvaluationsDTO).isNull();
    assertThatLogMessagesEqual(
        debug("Cannot comment - missing base branch policy evaluation for application " + application.getPublicId()
            + " repository https://gitlab.com/test/project1"));
  }

  @Test
  public void testResolveForPullRequest_haveExternalInternalMismatch() throws GitException, IOException {
    // given: an externally triggered default branch policy eval and an internally triggered feature branch policy eval
    final String defaultBranchPolicyEvaluationId = "default-policy-4";
    final String featureBranchPolicyEvaluationId = "feature-policy-4";
    final String featureCommit = "commit123";
    final String featureBranchName = "feature-branch";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        // with an externally triggered default branch policy eval
        .withDefaultBranchPolicyEvaluation(application.getId(), defaultBranchPolicyEvaluationId, false)
        // with an internally triggered feature branch policy eval
        .withFeatureBranchPolicyEvaluationForCommit(application.getId(), featureBranchPolicyEvaluationId, featureCommit,
            true)
        .withPullRequest(2, featureBranchName, featureCommit, true)
        .build();

    // when: resolve policy evaluations
    PullRequestPolicyEvaluationsDTO policyEvaluationsDTO = pullRequestPolicyEvaluationResolver
        .resolveForPullRequest(application.getId(), gitRepositoryInfo, 4,
            featureBranchName, "main", featureCommit, null);

    // then: result is empty
    assertThat(policyEvaluationsDTO).isNull();
    assertThatLogMessagesEqual(
        debug("Cannot comment - internal/external policy evaluation mismatch for application " +
            application.getPublicId() + " repository https://gitlab.com/test/project1"));
  }

  @Test
  public void testResolveForPullRequest_haveInternalExternalMismatch() throws GitException, IOException {
    // given: an internally triggered default branch policy eval and an externally triggered feature branch policy eval
    final String defaultBranchPolicyEvaluationId = "default-policy-4";
    final String featureBranchPolicyEvaluationId = "feature-policy-4";
    final String featureCommit = "commit123";
    final String featureBranchName = "feature-branch";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        // with an internally triggered default branch policy eval
        .withDefaultBranchPolicyEvaluation(application.getId(), defaultBranchPolicyEvaluationId, true)
        // with an externally triggered feature branch policy eval
        .withFeatureBranchPolicyEvaluationForCommit(application.getId(), featureBranchPolicyEvaluationId, featureCommit,
            false)
        .withPullRequest(2, featureBranchName, featureCommit, true)
        .build();

    // when: resolve policy evaluations
    PullRequestPolicyEvaluationsDTO policyEvaluationsDTO = pullRequestPolicyEvaluationResolver
        .resolveForPullRequest(application.getId(), gitRepositoryInfo, 4,
            featureBranchName, "main", featureCommit, null);

    // then: result is empty
    assertThat(policyEvaluationsDTO).isNull();
    assertThatLogMessagesEqual(
        debug("Cannot comment - internal/external policy evaluation mismatch for application " +
            application.getPublicId() + " repository https://gitlab.com/test/project1"));
  }

  @Test
  public void testResolveForPullRequest_haveNeededPolicyEvaluations_FeatureBranchOnDefaultBranch() throws Exception {
    // given: default and feature branch policy evals
    final String defaultBranchPolicyEvaluationId = "default-policy-3";
    final String featureBranchPolicyEvaluationId = "feature-policy-3";
    final String featureCommit = "commit123";
    final String featureBranchName = "feature-branch";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .withDefaultBranchPolicyEvaluation(application.getId(), defaultBranchPolicyEvaluationId, true)
        .withFeatureBranchPolicyEvaluationForCommit(application.getId(), featureBranchPolicyEvaluationId, featureCommit,
            true)
        .withPullRequest(2, featureBranchName, featureCommit, true)
        .build();

    // when: resolve policy evaluations
    PullRequestPolicyEvaluationsDTO policyEvaluationsDTO = pullRequestPolicyEvaluationResolver
        .resolveForPullRequest(application.getId(), gitRepositoryInfo, 4,
            featureBranchName, "main", featureCommit, null);

    // then: we have a PR we can comment on
    assertThat(policyEvaluationsDTO).isNotNull();
    assertThat(policyEvaluationsDTO.getApplicationId()).isEqualTo(application.getId());
    assertThat(policyEvaluationsDTO.getFeatureBranchName()).isEqualTo(featureBranchName);
    assertThat(policyEvaluationsDTO.getPullRequestHeadCommit()).isEqualTo(featureCommit);
    assertThat(policyEvaluationsDTO.getTargetPolicyEvaluationId()).isEqualTo(defaultBranchPolicyEvaluationId);
    assertThat(policyEvaluationsDTO.getFeatureBranchPolicyEvaluationId()).isEqualTo(featureBranchPolicyEvaluationId);
  }

  @Test
  public void testResolveForPullRequest_haveNeededPolicyEvaluations_FeatureBranchOnFeatureBranch() {
    // given: policy evals for two feature branches
    String baseFeatureBranchName = "baseFeatureBranchName";
    String baseFeatureCommit = "baseFeatureCommit";
    String baseFeatureBranchPolicyEvaluationId = "baseFeatureBranchPolicyEvaluationId";
    String childFeatureBranchName = "childFeatureBranchName";
    String childFeatureCommit = "childFeatureCommit";
    String childFeatureBranchPolicyEvaluationId = "childFeatureBranchPolicyEvaluationId";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .withFeatureBranchPolicyEvaluationForCommit(application.getId(), baseFeatureBranchPolicyEvaluationId,
            baseFeatureCommit, true)
        .withFeatureBranchPolicyEvaluationForCommit(application.getId(), childFeatureBranchPolicyEvaluationId,
            childFeatureCommit, true)
        .withPullRequest(2, childFeatureBranchName, childFeatureCommit, true)
        .build();

    // when: resolve policy evaluations
    PullRequestPolicyEvaluationsDTO policyEvaluationsDTO =
        pullRequestPolicyEvaluationResolver.resolveForPullRequest(application.getId(), gitRepositoryInfo, 2,
            childFeatureBranchName, baseFeatureBranchName, childFeatureCommit, baseFeatureCommit);

    // then: we have a PR we can comment on
    assertThat(policyEvaluationsDTO).isNotNull();
    assertThat(policyEvaluationsDTO.getApplicationId()).isEqualTo(application.getId());
    assertThat(policyEvaluationsDTO.getFeatureBranchName()).isEqualTo(childFeatureBranchName);
    assertThat(policyEvaluationsDTO.getPullRequestHeadCommit()).isEqualTo(childFeatureCommit);
    assertThat(policyEvaluationsDTO.getTargetPolicyEvaluationId()).isEqualTo(baseFeatureBranchPolicyEvaluationId);
    assertThat(policyEvaluationsDTO.getFeatureBranchPolicyEvaluationId())
        .isEqualTo(childFeatureBranchPolicyEvaluationId);
  }

  @Test
  public void testResolveForPullRequest_haveNeededPolicyEvaluationsForCommit() throws GitException, IOException {
    // given: default and feature branch policy evals
    final String defaultBranchPolicyEvaluationId = "default-policy-7";
    final String policyEvaluationIdForCommit = "default-policy-8";
    final String featureBranchPolicyEvaluationId = "feature-policy-7";
    final String baseCommit = "baseCommit";
    final String featureCommit = "commit123";
    final String featureBranchName = "feature-branch";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .withDefaultBranchPolicyEvaluation(application.getId(), defaultBranchPolicyEvaluationId, true)
        .withBaseCommitPolicyEvaluation(application.getId(), baseCommit, policyEvaluationIdForCommit, true)
        .withFeatureBranchPolicyEvaluationForCommit(application.getId(), featureBranchPolicyEvaluationId, featureCommit,
            true)
        .withPullRequest(2, featureBranchName, featureCommit, true)
        .build();

    // when: resolve policy evaluations
    PullRequestPolicyEvaluationsDTO policyEvaluationsDTO = pullRequestPolicyEvaluationResolver
        .resolveForPullRequest(application.getId(), gitRepositoryInfo, 4,
            featureBranchName, "main", featureCommit, baseCommit);

    // then: we have a PR we can comment on
    assertThat(policyEvaluationsDTO).isNotNull();
    assertThat(policyEvaluationsDTO.getApplicationId()).isEqualTo(application.getId());
    assertThat(policyEvaluationsDTO.getFeatureBranchName()).isEqualTo(featureBranchName);
    assertThat(policyEvaluationsDTO.getPullRequestHeadCommit()).isEqualTo(featureCommit);
    assertThat(policyEvaluationsDTO.getTargetPolicyEvaluationId()).isEqualTo(policyEvaluationIdForCommit);
    assertThat(policyEvaluationsDTO.getFeatureBranchPolicyEvaluationId()).isEqualTo(featureBranchPolicyEvaluationId);
  }

  @Test
  public void testResolveForPullRequest_deleteCheckoutDirectoryOnInvalidArgumentException() throws GitException, IOException {
    // given: default and feature branch policy evals
    final String message = "Error on SCM";
    final String baseCommit = "badBaseCommit";
    final String featureCommit = "commit123";
    final String featureBranchName = "feature-branch";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .withTargetCommitPolicyEvaluationResolverThrowing(new IllegalArgumentException(message))
        .build();

    // when: resolve policy evaluations
    assertThatExceptionOfType(SourceControlException.class).isThrownBy(() -> pullRequestPolicyEvaluationResolver
        .resolveForPullRequest(application.getId(), gitRepositoryInfo, 4,
            featureBranchName, "main", featureCommit, baseCommit))
        .withMessage(String.format(
            "Cannot comment - unable to resolve policy evaluations for application %s repository %s " +
                "pull request %s - reason: %s",
            application.getPublicId(), gitRepositoryInfo.getRepositoryUrl(), 4, message));

    // and: source control folder is deleted
    verify(sourceControlUtils, times(1)).deleteCheckoutDirectory(any(Application.class));
  }

  @Test
  public void testResolveForPullRequest_deleteCheckoutDirectoryOnGitException() throws GitException, IOException {
    // given: default and feature branch policy evals
    final String message = "Error on SCM";
    final String baseCommit = "badBaseCommit";
    final String featureCommit = "commit123";
    final String featureBranchName = "feature-branch";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .withTargetCommitPolicyEvaluationResolverThrowing(new GitException(message))
        .build();

    // when: resolve policy evaluations
    assertThatExceptionOfType(SourceControlException.class).isThrownBy(() -> pullRequestPolicyEvaluationResolver
        .resolveForPullRequest(application.getId(), gitRepositoryInfo, 4,
            featureBranchName, "main", featureCommit, baseCommit))
        .withMessage(String.format(
            "Cannot comment - unable to resolve policy evaluations for application %s repository %s " +
                "pull request %s - reason: %s",
            application.getPublicId(), gitRepositoryInfo.getRepositoryUrl(), 4, message));

    // and: source control folder is deleted
    verify(sourceControlUtils, times(1)).deleteCheckoutDirectory(any(Application.class));
  }

  @Test
  public void testResolveForPullRequest_doNotDeleteCheckoutDirectoryOnRuntimeException() throws GitException, IOException {
    // given: default and feature branch policy evals
    final String message = "Error on SCM";
    final String baseCommit = "badBaseCommit";
    final String featureCommit = "commit123";
    final String featureBranchName = "feature-branch";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();

    PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver = new TestablePolicyEvaluationResolver()
        .withTargetCommitPolicyEvaluationResolverThrowing(new RuntimeException(message))
        .build();

    // when: resolve policy evaluations
    assertThatExceptionOfType(SourceControlException.class).isThrownBy(() -> pullRequestPolicyEvaluationResolver
        .resolveForPullRequest(application.getId(), gitRepositoryInfo, 4,
            featureBranchName, "main", featureCommit, baseCommit))
        .withMessage(String.format(
            "Cannot comment - unable to resolve policy evaluations for application %s repository %s " +
                "pull request %s - reason: %s",
            application.getPublicId(), gitRepositoryInfo.getRepositoryUrl(), 4, message));

    // and: source control folder is NOT deleted
    verify(sourceControlUtils, never()).deleteCheckoutDirectory(any(Application.class));
  }

  private GitRepositoryInfo createDefaultGitRepositoryInfo() {
    return new GitRepositoryInfo("https://gitlab.com/test/project1", null, "user", "token",
        SourceControlProvider.GITLAB, "main", true, true, true, true, true, true, false, null);
  }

  private class TestablePolicyEvaluationResolver
  {
    @Mock
    private PolicyEvaluationDAO mockPolicyEvaluationDAO;

    @Mock
    private PullRequestDefaultBranchPolicyEvaluationResolver mockDefaultBranchPolicyEvaluationResolver;

    @Mock
    private PullRequestTargetCommitPolicyEvaluationResolver mockTargetCommitPolicyEvaluationResolver;

    @Mock
    private PullRequestEligibilityValidator mockPullRequestEligibilityValidator;

    private final CommitInformation commitInformation = new CommitInformation();

    TestablePolicyEvaluationResolver() {
      MockitoAnnotations.openMocks(this);
    }

    private TestablePolicyEvaluationResolver withDefaultBranchPolicyEvaluation(
        String applicationId,
        String policyEvaluationId,
        boolean internallyTriggered) throws GitException, IOException
    {
      return withBaseCommitPolicyEvaluation(applicationId, null, policyEvaluationId, internallyTriggered);
    }

    private TestablePolicyEvaluationResolver withBaseCommitPolicyEvaluation(
        String applicationId,
        String commitHash,
        String policyEvaluationId,
        boolean internallyTriggered) throws GitException, IOException
    {
      PolicyEvaluation policyEvaluation = new PolicyEvaluation();
      policyEvaluation.setOwnerId(applicationId);
      policyEvaluation.setId(policyEvaluationId);
      policyEvaluation.setCommitHash(commitHash);
      policyEvaluation.setScanTriggerType(
          internallyTriggered ? ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING : ScanTriggerType.CLI);
      doReturn(Optional.of(policyEvaluation)).when(mockGitCommitHistoryService)
          .getLatestPolicyEvaluationForApplicationBaseBranch(applicationId, !internallyTriggered);
      if (null == commitHash) {
        doReturn(policyEvaluation).when(mockDefaultBranchPolicyEvaluationResolver)
            .getOrPerformDefaultBranchPolicyEvaluation(eq(applicationId), any(), any());
      }
      else {
        doReturn(policyEvaluation).when(mockTargetCommitPolicyEvaluationResolver)
            .getOrPerformTargetCommitPolicyEvaluation(any(), any(), any(), any(), any());
      }
      return this;
    }

    private TestablePolicyEvaluationResolver withFeatureBranchPolicyEvaluation(
        String applicationId,
        String policyEvaluationId,
        boolean internallyTriggered)
    {
      PolicyEvaluation policyEvaluation = new PolicyEvaluation();
      policyEvaluation.setOwnerId(applicationId);
      policyEvaluation.setId(policyEvaluationId);
      policyEvaluation.setScanTriggerType(
          internallyTriggered ? ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST : ScanTriggerType.CLI);
      doReturn(policyEvaluation).when(mockPolicyEvaluationDAO).getById(policyEvaluationId);
      return this;
    }

    private TestablePolicyEvaluationResolver withFeatureBranchPolicyEvaluationForCommit(
        String applicationId,
        String policyEvaluationId,
        String commitHash,
        boolean internallyTriggered)
    {
      PolicyEvaluation policyEvaluation = new PolicyEvaluation();
      policyEvaluation.setOwnerId(applicationId);
      policyEvaluation.setId(policyEvaluationId);
      policyEvaluation.setCommitHash(commitHash);
      policyEvaluation.setScanTriggerType(
          internallyTriggered ? ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST : ScanTriggerType.CLI);
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

    private TestablePolicyEvaluationResolver withPullRequestWithBaseCommit(
        int pullRequestNumber,
        String branchName,
        String baseCommitHash,
        String headCommitHash,
        boolean isEligibleForCommenting)
    {
      PullRequest pullRequest = new GithubPullRequest();
      pullRequest.setNumber(pullRequestNumber);
      pullRequest.setHead(branchName);
      pullRequest.setBaseCommitHash(baseCommitHash);
      pullRequest.setHeadCommitHash(headCommitHash);

      doReturn(isEligibleForCommenting).when(mockPullRequestEligibilityValidator)
          .isPullRequestEligibleForCommenting(any(), any(), any(), any());

      commitInformation.addPullRequest(pullRequest);
      return this;
    }

    private TestablePolicyEvaluationResolver withTargetCommitPolicyEvaluationResolverThrowing(
        Exception exception) throws GitException, IOException
    {
      doThrow(exception).when(mockTargetCommitPolicyEvaluationResolver)
          .getOrPerformTargetCommitPolicyEvaluation(any(), any(), any(), any(), any());
      return this;
    }

    PullRequestPolicyEvaluationResolver build() {
      doReturn(commitInformation).when(mockPullRequestInfoClient).getCommitInfoFromScm(any(), any());

      return new PullRequestPolicyEvaluationResolver(
          mockGitCommitHistoryService,
          mockPolicyEvaluationDAO,
          mockDefaultBranchPolicyEvaluationResolver,
          mockTargetCommitPolicyEvaluationResolver,
          mockPullRequestEligibilityValidator,
          mockPullRequestInfoClient,
          mockSourceControlScanService,
          sourceControlUtils,
          applicationDAO);
    }
  }
}
