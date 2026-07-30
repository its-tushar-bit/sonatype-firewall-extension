/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.git.utils.api.GitException;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.model.Commit;
import com.sonatype.nexus.scm.api.model.CommitInformation;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PullRequestDefaultBranchPolicyEvaluationResolverTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private SourceControlScanService mockSourceControlScanService;

  @Mock
  private PullRequestInfoClient mockPullRequestInfoClient;

  public PullRequestDefaultBranchPolicyEvaluationResolverTest() {
    super(PullRequestDefaultBranchPolicyEvaluationResolver.class);
  }

  @Before
  @Override
  public void setup() {
    super.setup();
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testGetOrPerformDefaultBranchPolicyEvaluation_noExistingPolicyEvaluation() throws GitException, IOException {
    // setup
    final String applicationId = "app1";
    final String commit = "commit-123";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();
    PullRequestDefaultBranchPolicyEvaluationResolver policyEvaluationResolver =
        new TestableDefaultBranchPolicyEvaluationBuilder()
            .build();

    // when
    policyEvaluationResolver.getOrPerformDefaultBranchPolicyEvaluation(applicationId, gitRepositoryInfo, commit);

    // then
    verify(mockPullRequestInfoClient, times(1)).getCommitInfoFromScm(any(), any());
    verify(mockSourceControlScanService, times(1)).doSynchronousSourceControlScan(eq(applicationId), any(), any());
  }

  @Test
  public void testGetOrPerformDefaultBranchPolicyEvaluation_noExistingPolicyEvaluation_externalEvaluationsExist() throws GitException, IOException {
    // setup
    final String applicationId = "app1";
    final String commit = "commit-123";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();
    PullRequestDefaultBranchPolicyEvaluationResolver policyEvaluationResolver =
        new TestableDefaultBranchPolicyEvaluationBuilder()
            .hasExternalPolicyEvaluations(true)
            .build();

    // when
    PolicyEvaluation defaultBranchPolicyEvaluation =
        policyEvaluationResolver.getOrPerformDefaultBranchPolicyEvaluation(applicationId, gitRepositoryInfo, commit);

    // then: no record returned and no scan is triggered
    assertThat(defaultBranchPolicyEvaluation).isNull();

    verify(mockPullRequestInfoClient, times(1)).getCommitInfoFromScm(any(), any());
    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(eq(applicationId), any(), any());
  }

  @Test
  public void testGetOrPerformDefaultBranchPolicyEvaluation_externalPolicyEvaluation() throws GitException, IOException {
    // setup
    final String applicationId = "app1";
    final String commit = "commit-123";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();
    PolicyEvaluation policyEvaluation = createPolicyEvaluation(Stage.ID_BUILD, ScanTriggerType.CONTINUOUS_INTEGRATION);

    PullRequestDefaultBranchPolicyEvaluationResolver policyEvaluationResolver =
        new TestableDefaultBranchPolicyEvaluationBuilder()
            .withDefaultBranchCommitHistoryPolicyEvaluation(policyEvaluation)
            .withBuildStagePolicyEvaluation(policyEvaluation)
            .hasExternalPolicyEvaluations(true)
            .build();

    // when
    PolicyEvaluation defaultBranchPolicyEvaluation =
        policyEvaluationResolver.getOrPerformDefaultBranchPolicyEvaluation(applicationId, gitRepositoryInfo, commit);

    // then: the only policy evaluation available is used
    assertThat(defaultBranchPolicyEvaluation).isEqualTo(policyEvaluation);

    verify(mockPullRequestInfoClient, never()).getCommitInfoFromScm(any(), any());
    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(eq(applicationId), any(), any());
  }

  @Test
  public void testGetOrPerformDefaultBranchPolicyEvaluation_buildAndReleaseStagePolicyEvaluations() throws GitException, IOException {
    // setup
    final String applicationId = "app1";
    final String commit = "commit-123";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();
    PolicyEvaluation releasePolicyEvaluation = createPolicyEvaluation(Stage.ID_RELEASE, ScanTriggerType.CLI);
    PolicyEvaluation buildPolicyEvaluation = createPolicyEvaluation(Stage.ID_BUILD, ScanTriggerType.CLI);

    PullRequestDefaultBranchPolicyEvaluationResolver policyEvaluationResolver =
        new TestableDefaultBranchPolicyEvaluationBuilder()
            .withDefaultBranchCommitHistoryPolicyEvaluation(releasePolicyEvaluation)
            .withReleaseStagePolicyEvaluation(releasePolicyEvaluation)
            .withBuildStagePolicyEvaluation(buildPolicyEvaluation)
            .hasExternalPolicyEvaluations(true)
            .build();

    // when
    PolicyEvaluation defaultBranchPolicyEvaluation =
        policyEvaluationResolver.getOrPerformDefaultBranchPolicyEvaluation(applicationId, gitRepositoryInfo, commit);

    // then: the build policy evaluation is preferred over the release one
    assertThat(defaultBranchPolicyEvaluation).isEqualTo(buildPolicyEvaluation);

    verify(mockPullRequestInfoClient, never()).getCommitInfoFromScm(any(), any());
    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(eq(applicationId), any(), any());
  }

  @Test
  public void testGetOrPerformDefaultBranchPolicyEvaluation_buildAndSourceStagePolicyEvaluations() throws GitException, IOException {
    // setup
    final String applicationId = "app1";
    final String commit = "commit-123";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();
    PolicyEvaluation sourcePolicyEvaluation =
        createPolicyEvaluation(Stage.ID_SOURCE, ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST);
    PolicyEvaluation buildPolicyEvaluation = createPolicyEvaluation(Stage.ID_BUILD, ScanTriggerType.CLI);

    PullRequestDefaultBranchPolicyEvaluationResolver policyEvaluationResolver =
        new TestableDefaultBranchPolicyEvaluationBuilder()
            .withDefaultBranchCommitHistoryPolicyEvaluation(buildPolicyEvaluation)
            .withSourceStagePolicyEvaluation(sourcePolicyEvaluation)
            .withBuildStagePolicyEvaluation(buildPolicyEvaluation)
            .hasExternalPolicyEvaluations(true)
            .build();

    // when
    PolicyEvaluation defaultBranchPolicyEvaluation =
        policyEvaluationResolver.getOrPerformDefaultBranchPolicyEvaluation(applicationId, gitRepositoryInfo, commit);

    // then: the build policy evaluation is preferred over the source one
    assertThat(defaultBranchPolicyEvaluation).isEqualTo(buildPolicyEvaluation);

    verify(mockPullRequestInfoClient, never()).getCommitInfoFromScm(any(), any());
    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(eq(applicationId), any(), any());
  }

  @Test
  public void testGetOrPerformDefaultBranchPolicyEvaluation_sourceAndReleaseStagePolicyEvaluations() throws GitException, IOException {
    // setup
    final String applicationId = "app1";
    final String commit = "commit-123";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();
    PolicyEvaluation releasePolicyEvaluation = createPolicyEvaluation(Stage.ID_RELEASE, ScanTriggerType.CLI);
    PolicyEvaluation sourcePolicyEvaluation =
        createPolicyEvaluation(Stage.ID_SOURCE, ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST);

    PullRequestDefaultBranchPolicyEvaluationResolver policyEvaluationResolver =
        new TestableDefaultBranchPolicyEvaluationBuilder()
            .withDefaultBranchCommitHistoryPolicyEvaluation(releasePolicyEvaluation)
            .withReleaseStagePolicyEvaluation(releasePolicyEvaluation)
            .withSourceStagePolicyEvaluation(sourcePolicyEvaluation)
            .hasExternalPolicyEvaluations(true)
            .build();

    // when
    PolicyEvaluation defaultBranchPolicyEvaluation =
        policyEvaluationResolver.getOrPerformDefaultBranchPolicyEvaluation(applicationId, gitRepositoryInfo, commit);

    // then: the release policy evaluation is preferred over the release one
    assertThat(defaultBranchPolicyEvaluation).isEqualTo(releasePolicyEvaluation);

    verify(mockPullRequestInfoClient, never()).getCommitInfoFromScm(any(), any());
    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(eq(applicationId), any(), any());
  }

  @Test
  public void testGetOrPerformDefaultBranchPolicyEvaluation_internalPolicyEvaluation() throws GitException, IOException {
    // setup
    final String applicationId = "app1";
    final String commit = "commit-123";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();
    PolicyEvaluation sourcePolicyEvaluation =
        createPolicyEvaluation(Stage.ID_SOURCE, ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST);

    PullRequestDefaultBranchPolicyEvaluationResolver policyEvaluationResolver =
        new TestableDefaultBranchPolicyEvaluationBuilder()
            .withDefaultBranchCommitHistoryPolicyEvaluation(sourcePolicyEvaluation)
            .withSourceStagePolicyEvaluation(sourcePolicyEvaluation)
            .build();

    // when
    PolicyEvaluation defaultBranchPolicyEvaluation =
        policyEvaluationResolver.getOrPerformDefaultBranchPolicyEvaluation(applicationId, gitRepositoryInfo, commit);

    // then: the source policy evaluation is used
    assertThat(defaultBranchPolicyEvaluation).isEqualTo(sourcePolicyEvaluation);

    verify(mockPullRequestInfoClient, never()).getCommitInfoFromScm(any(), any());
    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(eq(applicationId), any(), any());
  }

  @Test
  public void testGetOrPerformDefaultBranchPolicyEvaluation_staleInternalPolicyEvaluation() throws GitException, IOException {
    // setup
    final String applicationId = "app1";
    final String commit = "commit-123";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();
    PolicyEvaluation policyEvaluation =
        createPolicyEvaluation(Stage.ID_SOURCE, ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST);

    PullRequestDefaultBranchPolicyEvaluationResolver policyEvaluationResolver =
        new TestableDefaultBranchPolicyEvaluationBuilder()
            .withDefaultBranchCommitHistoryPolicyEvaluation(policyEvaluation)
            .withSourceStagePolicyEvaluation(policyEvaluation)
            .withHeadCommit("commit-456")
            .build();

    // when
    policyEvaluationResolver.getOrPerformDefaultBranchPolicyEvaluation(applicationId, gitRepositoryInfo, commit);

    // then
    verify(mockPullRequestInfoClient, never()).getCommitInfoFromScm(any(), any());
    verify(mockSourceControlScanService, times(1)).doSynchronousSourceControlScan(eq(applicationId), any(), any());
  }

  @Test
  public void testGetOrPerformDefaultBranchPolicyEvaluation_staleInternalPolicyEvaluation_externalEvaluationsExist() throws GitException, IOException {
    // setup
    final String applicationId = "app1";
    final String commit = "commit-123";
    GitRepositoryInfo gitRepositoryInfo = createDefaultGitRepositoryInfo();
    PolicyEvaluation policyEvaluation =
        createPolicyEvaluation(Stage.ID_SOURCE, ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST);

    PullRequestDefaultBranchPolicyEvaluationResolver policyEvaluationResolver =
        new TestableDefaultBranchPolicyEvaluationBuilder()
            .withDefaultBranchCommitHistoryPolicyEvaluation(policyEvaluation)
            .withSourceStagePolicyEvaluation(policyEvaluation)
            .withHeadCommit("commit-456")
            .hasExternalPolicyEvaluations(true)
            .build();

    // when
    policyEvaluationResolver.getOrPerformDefaultBranchPolicyEvaluation(applicationId, gitRepositoryInfo, commit);

    // then
    verify(mockPullRequestInfoClient, never()).getCommitInfoFromScm(any(), any());
    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(eq(applicationId), any(), any());
  }

  private GitRepositoryInfo createDefaultGitRepositoryInfo() {
    return new GitRepositoryInfo("https://gitlab.com/test/project1", null, "user", "token",
        SourceControlProvider.GITLAB, "master", true, true, true, true, true, true, false, null);
  }

  private PolicyEvaluation createPolicyEvaluation(String stageTypeId, ScanTriggerType scanTriggerType, Date time) {
    PolicyEvaluation policyEvaluation =
        new PolicyEvaluation("app1", stageTypeId, "scan-id", "init", scanTriggerType);
    policyEvaluation.setCommitHash("commit-123");
    policyEvaluation.setTime(time);
    return policyEvaluation;
  }

  private PolicyEvaluation createPolicyEvaluation(String stageTypeId, ScanTriggerType scanTriggerType) {
    return createPolicyEvaluation(stageTypeId, scanTriggerType, new Date());
  }

  private class TestableDefaultBranchPolicyEvaluationBuilder
  {
    @Mock
    private GitCommitHistoryService mockGitCommitHistoryService;

    @Mock
    private PolicyEvaluationDAO mockPolicyEvaluationDAO;

    private PolicyEvaluation defaultBranchCommitHistoryPolicyEvaluation;

    private PolicyEvaluation buildStagePolicyEvaluation;

    private PolicyEvaluation sourceStagePolicyEvaluation;

    private PolicyEvaluation releaseStagePolicyEvaluation;

    private boolean hasExternalPolicyEvaluations = false;

    private String headCommit = "commit-123";

    TestableDefaultBranchPolicyEvaluationBuilder() {
      MockitoAnnotations.openMocks(this);
    }

    TestableDefaultBranchPolicyEvaluationBuilder withDefaultBranchCommitHistoryPolicyEvaluation(
        PolicyEvaluation policyEvaluation)
    {
      defaultBranchCommitHistoryPolicyEvaluation = policyEvaluation;
      return this;
    }

    TestableDefaultBranchPolicyEvaluationBuilder withBuildStagePolicyEvaluation(PolicyEvaluation policyEvaluation) {
      buildStagePolicyEvaluation = policyEvaluation;
      return this;
    }

    TestableDefaultBranchPolicyEvaluationBuilder withSourceStagePolicyEvaluation(PolicyEvaluation policyEvaluation) {
      sourceStagePolicyEvaluation = policyEvaluation;
      return this;
    }

    TestableDefaultBranchPolicyEvaluationBuilder withReleaseStagePolicyEvaluation(PolicyEvaluation policyEvaluation) {
      releaseStagePolicyEvaluation = policyEvaluation;
      return this;
    }

    TestableDefaultBranchPolicyEvaluationBuilder withHeadCommit(String commit) {
      headCommit = commit;
      return this;
    }

    TestableDefaultBranchPolicyEvaluationBuilder hasExternalPolicyEvaluations(boolean hasExternalPolicyEvaluations) {
      this.hasExternalPolicyEvaluations = hasExternalPolicyEvaluations;
      return this;
    }

    PullRequestDefaultBranchPolicyEvaluationResolver build() throws GitException, IOException {
      final CommitInformation commitInformation = new CommitInformation();
      commitInformation.addCommit(new Commit("commit-456", new Date()));
      doReturn(commitInformation).when(mockPullRequestInfoClient).getCommitInfoFromScm(any(), any());

      doReturn(headCommit).when(mockGitCommitHistoryService).getLatestCommitForApplication(any());

      doReturn(Optional.ofNullable(defaultBranchCommitHistoryPolicyEvaluation)).when(mockGitCommitHistoryService)
          .getLatestPolicyEvaluationForApplicationBaseBranch(any(), anyBoolean());

      doReturn(buildStagePolicyEvaluation).when(mockPolicyEvaluationDAO)
          .getLastByOwnerIdCommitHashAndStageId(any(), any(), eq(Stage.ID_BUILD));
      doReturn(sourceStagePolicyEvaluation).when(mockPolicyEvaluationDAO)
          .getLastByOwnerIdCommitHashAndStageId(any(), any(), eq(Stage.ID_SOURCE));
      doReturn(releaseStagePolicyEvaluation).when(mockPolicyEvaluationDAO)
          .getLastByOwnerIdCommitHashAndStageId(any(), any(), eq(Stage.ID_RELEASE));
      doReturn(hasExternalPolicyEvaluations).when(mockPolicyEvaluationDAO)
          .hasExternalPolicyEvaluations(any(), any());

      doReturn(sourceStagePolicyEvaluation).when(mockSourceControlScanService)
          .doSynchronousSourceControlScan(any(), any(), any());

      return new PullRequestDefaultBranchPolicyEvaluationResolver(
          mockGitCommitHistoryService,
          mockPolicyEvaluationDAO,
          mockPullRequestInfoClient,
          mockSourceControlScanService);
    }
  }
}
