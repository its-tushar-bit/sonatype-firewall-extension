/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.scm.api.model.PullRequest;
import com.sonatype.nexus.scm.api.model.PullRequestState;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

public class PullRequestEligibilityValidatorTest
    extends VerifiableLoggingTestBase
{
  private TestScenario testScenario;

  public PullRequestEligibilityValidatorTest() {
    super(PullRequestEligibilityValidator.class);
  }

  @Override
  @Before
  public void setup() {
    super.setup();
    // test scenario initialized with default values that result in an eligible pull request
    testScenario = new TestScenario()
        .forApplication("app1")
        .withDefaultBranch("trunk")
        .withHeadCommit("abc123")
        .withInternalRepository(true)
        .withOpenPullRequest(true)
        .withPolicyEvalCommit("abc123")
        .withPrivateRepository(true)
        .withPullRequestHead("feature-branch-1")
        .withRepositoryUrl("http://gitlab.com/projects/app1");
  }

  @Test
  public void testIsPullRequestEligibleForCommenting_repoNotPrivateNorInternal() {
    // given: a non-private/internal repository and all other conditions marked as eligible
    testScenario
        .withInternalRepository(false)
        .withPrivateRepository(false);

    // then
    assertThat(testScenario.isPullRequestEligibleForCommenting()).isFalse();
    assertThatLogMessagesEqual(
        debug("Repository is not valid for pull requests, ensure that it is private or internal: "
            + "http://gitlab.com/projects/app1"));
  }

  @Test
  public void testIsPullRequestEligibleForCommenting_repoIsInternal() {
    // given: an internal repository and all other conditions marked as eligible
    testScenario
        .withInternalRepository(true)
        .withPrivateRepository(false);

    // then
    assertThat(testScenario.isPullRequestEligibleForCommenting()).isTrue();
  }

  @Test
  public void testIsPullRequestEligibleForCommenting_repoIsPrivate() {
    // given: a private repository and all other conditions marked as eligible
    testScenario
        .withInternalRepository(false)
        .withPrivateRepository(true);

    // then
    assertThat(testScenario.isPullRequestEligibleForCommenting()).isTrue();
  }

  @Test
  public void testIsPullRequestEligibleForCommenting_repoIsBothPrivateAndInternal() {
    // given: a private repository and all other conditions marked as eligible
    testScenario
        .withInternalRepository(true)
        .withPrivateRepository(true);

    // then
    assertThat(testScenario.isPullRequestEligibleForCommenting()).isTrue();
  }

  @Test
  public void testIsPullRequestEligibleForCommenting_pullRequestNotOpen() {
    // given: a closed pull request
    testScenario.withOpenPullRequest(false);

    // then
    assertThat(testScenario.isPullRequestEligibleForCommenting()).isFalse();
    assertThatLogMessagesEqual(
        debug("application 'app1' pull request '0' state 'CLOSED' is not open"));
  }

  @Test
  public void testIsPullRequestEligibleForCommenting_pullRequestForDefaultBranch() {
    // given: pull request is for the default branch (i.e. trying to merge default branch to some other branch)
    testScenario.withPullRequestHead("trunk");

    // then
    assertThat(testScenario.isPullRequestEligibleForCommenting()).isFalse();
    assertThatLogMessagesEqual(
        debug("application 'app1' pull request '0' is for the default branch"));
  }

  @Test
  public void testIsPullRequestEligibleForCommenting_policyEvalNotForHeadCommit() {
    // given: policy eval is not for the head commit
    testScenario.withPolicyEvalCommit("tail-123");

    // then
    assertThat(testScenario.isPullRequestEligibleForCommenting()).isFalse();
    assertThatLogMessagesEqual(
        debug("The head commit hash 'abc123', for application 'app1', PR '0' does not match the commit on the policy" +
            " evaluation 'tail-123'"));
  }

  @Test
  public void testIsPullRequestEligibleForCommenting_pullRequestIsEligible() {
    // the default test scenario should represent an eligible pull request
    assertThat(testScenario.isPullRequestEligibleForCommenting()).isTrue();
  }

  private static class TestScenario
  {
    @Mock
    private PullRequest mockPullRequest;

    private final PolicyEvaluation policyEvaluation = new PolicyEvaluation();

    @Mock
    private GitRepositoryInfo mockGitRepositoryInfo;

    @Mock
    private ScmRepoVisibilityService mockScmRepoVisibilityService;

    private boolean isRepoPrivate;

    private boolean isRepoInternal;

    private String applicationId;

    TestScenario() {
      MockitoAnnotations.openMocks(this);
    }

    TestScenario forApplication(String applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    TestScenario withInternalRepository(boolean isInternal) {
      isRepoInternal = isInternal;
      return this;
    }

    TestScenario withPrivateRepository(boolean isPrivate) {
      isRepoPrivate = isPrivate;
      doReturn(isPrivate).when(mockPullRequest).isRepositoryPrivate();
      return this;
    }

    TestScenario withHeadCommit(String headCommit) {
      doReturn(headCommit).when(mockPullRequest).getHeadCommitHash();
      return this;
    }

    TestScenario withPolicyEvalCommit(String policyEvalCommit) {
      policyEvaluation.setCommitHash(policyEvalCommit);
      return this;
    }

    TestScenario withOpenPullRequest(boolean isOpen) {
      doReturn(isOpen ? PullRequestState.OPEN : PullRequestState.CLOSED).when(mockPullRequest).getState();
      return this;
    }

    TestScenario withPullRequestHead(String pullRequestHead) {
      doReturn(pullRequestHead).when(mockPullRequest).getHead();
      return this;
    }

    TestScenario withDefaultBranch(String defaultBranch) {
      doReturn(defaultBranch).when(mockGitRepositoryInfo).getBaseBranch();
      return this;
    }

    TestScenario withRepositoryUrl(String repositoryUrl) {
      doReturn(repositoryUrl).when(mockGitRepositoryInfo).getRepositoryUrl();
      return this;
    }

    boolean isPullRequestEligibleForCommenting() {
      doReturn(isRepoPrivate || isRepoInternal).when(mockScmRepoVisibilityService)
          .isRepositoryValidForPullRequestFeatures(eq(mockGitRepositoryInfo));
      return new PullRequestEligibilityValidator(mockScmRepoVisibilityService)
          .isPullRequestEligibleForCommenting(applicationId, mockPullRequest, mockGitRepositoryInfo, policyEvaluation);
    }
  }
}
