/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
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

public class PullRequestTargetCommitPolicyEvaluationResolverTest
    extends VerifiableLoggingTestBase
{
  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Mock
  private SourceControlScanService mockSourceControlScanService;

  private File sourceControlDir;

  private Application application;

  private GitRepositoryInfo gitRepositoryInfo;

  public PullRequestTargetCommitPolicyEvaluationResolverTest() {
    super(PullRequestTargetCommitPolicyEvaluationResolver.class);
  }

  @Before
  @Override
  public void setup() {
    super.setup();
    MockitoAnnotations.openMocks(this);

    try {
      sourceControlDir = tmpDir.newFolder();
    }
    catch (final IOException ioEx) {
      throw new RuntimeException("failed creating temp source control dir", ioEx);
    }
    application = new Application();
    application.setId("app1");
    gitRepositoryInfo = new GitRepositoryInfo();
  }

  @Test
  public void testGetOrPerformTargetCommitPolicyEvaluation_noExistingPolicyEvaluations() throws GitException, IOException {
    // setup
    final String headBranchName = "feature-1";
    final String baseBranchName = "main";
    final String baseCommit = "base-commit";
    final String commonCommit = "common-commit";

    PullRequestTargetCommitPolicyEvaluationResolver policyEvaluationResolver =
        new TestableTargetCommitPolicyEvaluationBuilder()
            .withCommonAncestor(commonCommit)
            .build();

    // when
    policyEvaluationResolver.getOrPerformTargetCommitPolicyEvaluation(
        application, gitRepositoryInfo, baseBranchName, baseCommit, headBranchName);

    // then: a sync scan is performed
    verify(mockSourceControlScanService, times(1)).doSynchronousSourceControlScan(
        eq(application.getId()), any(), eq(baseBranchName), eq(commonCommit));
  }

  @Test
  public void testGetOrPerformTargetCommitPolicyEvaluation_noPolicyEvaluation_externalEvaluationsExist() throws GitException, IOException {
    // setup
    final String headBranchName = "feature-1";
    final String baseBranchName = "main";
    final String baseCommit = "base-commit";
    final String commonCommit = "common-commit";

    PullRequestTargetCommitPolicyEvaluationResolver policyEvaluationResolver =
        new TestableTargetCommitPolicyEvaluationBuilder()
            .hasExternalPolicyEvaluations(true)
            .withCommonAncestor(commonCommit)
            .build();

    // when
    PolicyEvaluation targetCommitPolicyEvaluation =
        policyEvaluationResolver.getOrPerformTargetCommitPolicyEvaluation(
            application, gitRepositoryInfo, baseBranchName, baseCommit, headBranchName);

    // then: no record returned and no scan is triggered
    assertThat(targetCommitPolicyEvaluation).isNull();

    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(
        eq(application.getId()), any(), eq(baseBranchName), any());
  }

  @Test
  public void testGetOrPerformTargetCommitPolicyEvaluation_internalPolicyEvaluationExists() throws GitException, IOException {
    // setup
    final String headBranchName = "feature-1";
    final String baseBranchName = "main";
    final String baseCommit = "base-commit";
    final String commonCommit = "common-commit";

    PolicyEvaluation policyEvaluation =
        createPolicyEvaluation(Stage.ID_BUILD, ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST);

    PullRequestTargetCommitPolicyEvaluationResolver policyEvaluationResolver =
        new TestableTargetCommitPolicyEvaluationBuilder()
            .withLatestCommonAncestorPolicyEvaluation(policyEvaluation)
            .withBuildStagePolicyEvaluation(policyEvaluation)
            .withCommonAncestor(commonCommit)
            .build();

    // when
    PolicyEvaluation targetCommitPolicyEvaluation =
        policyEvaluationResolver.getOrPerformTargetCommitPolicyEvaluation(
            application, gitRepositoryInfo, baseBranchName, baseCommit, headBranchName);

    // then: the only policy evaluation available is used
    assertThat(targetCommitPolicyEvaluation).isEqualTo(policyEvaluation);

    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(eq(application.getId()), any(), any());
  }

  @Test
  public void testGetOrPerformTargetCommitPolicyEvaluation_externalPolicyEvaluationExists() throws GitException, IOException {
    // setup
    final String headBranchName = "feature-1";
    final String baseBranchName = "main";
    final String baseCommit = "base-commit";
    final String commonCommit = "common-commit";

    PolicyEvaluation policyEvaluation = createPolicyEvaluation(Stage.ID_BUILD, ScanTriggerType.CONTINUOUS_INTEGRATION);

    PullRequestTargetCommitPolicyEvaluationResolver policyEvaluationResolver =
        new TestableTargetCommitPolicyEvaluationBuilder()
            .withLatestCommonAncestorPolicyEvaluation(policyEvaluation)
            .withBuildStagePolicyEvaluation(policyEvaluation)
            .withCommonAncestor(commonCommit)
            .hasExternalPolicyEvaluations(true)
            .build();

    // when
    PolicyEvaluation targetCommitPolicyEvaluation =
        policyEvaluationResolver.getOrPerformTargetCommitPolicyEvaluation(
            application, gitRepositoryInfo, baseBranchName, baseCommit, headBranchName);

    // then: the only policy evaluation available is used
    assertThat(targetCommitPolicyEvaluation).isEqualTo(policyEvaluation);

    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(eq(application.getId()), any(), any());
  }

  @Test
  public void testGetOrPerformTargetCommitPolicyEvaluation_externalPolicyEvaluationExists_noCommonAncestor() throws GitException, IOException {
    // setup
    final String headBranchName = "feature-1";
    final String baseBranchName = "main";
    final String baseCommit = "base-commit";

    PolicyEvaluation policyEvaluation = createPolicyEvaluation(Stage.ID_BUILD, ScanTriggerType.CONTINUOUS_INTEGRATION);

    PullRequestTargetCommitPolicyEvaluationResolver policyEvaluationResolver =
        new TestableTargetCommitPolicyEvaluationBuilder()
            .withLatestBaseCommitPolicyEvaluation(policyEvaluation)
            .withBuildStagePolicyEvaluation(policyEvaluation)
            .withBaseCommit(baseCommit)
            .hasExternalPolicyEvaluations(true)
            .build();

    // when
    PolicyEvaluation targetCommitPolicyEvaluation =
        policyEvaluationResolver.getOrPerformTargetCommitPolicyEvaluation(
            application, gitRepositoryInfo, baseBranchName, baseCommit, headBranchName);

    // then: the only policy evaluation available is used
    assertThat(targetCommitPolicyEvaluation).isEqualTo(policyEvaluation);

    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(eq(application.getId()), any(), any());
  }

  @Test
  public void testGetOrPerformTargetCommitPolicyEvaluation_externalReleaseStagePolicyEvaluationExists() throws GitException, IOException {
    // setup
    final String headBranchName = "feature-1";
    final String baseBranchName = "main";
    final String baseCommit = "base-commit";
    final String commonCommit = "common-commit";

    PolicyEvaluation policyEvaluation =
        createPolicyEvaluation(Stage.ID_RELEASE, ScanTriggerType.CONTINUOUS_INTEGRATION);

    PullRequestTargetCommitPolicyEvaluationResolver policyEvaluationResolver =
        new TestableTargetCommitPolicyEvaluationBuilder()
            .withLatestCommonAncestorPolicyEvaluation(policyEvaluation)
            .hasExternalPolicyEvaluations(true)
            .withCommonAncestor(commonCommit)
            .build();

    // when
    PolicyEvaluation targetCommitPolicyEvaluation =
        policyEvaluationResolver.getOrPerformTargetCommitPolicyEvaluation(
            application, gitRepositoryInfo, baseBranchName, baseCommit, headBranchName);

    // then: the only policy evaluation available is used, even though we prefer BUILD and SOURCE stage evaluations
    assertThat(targetCommitPolicyEvaluation).isEqualTo(policyEvaluation);

    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(eq(application.getId()), any(), any());
  }

  @Test
  public void testGetOrPerformTargetCommitPolicyEvaluation_buildAndSourceStagePolicyEvaluations() throws GitException, IOException {
    // setup
    final String headBranchName = "feature-1";
    final String baseBranchName = "main";
    final String baseCommit = "base-commit";
    final String commonCommit = "common-commit";

    PolicyEvaluation sourcePolicyEvaluation =
        createPolicyEvaluation(Stage.ID_SOURCE, ScanTriggerType.CONTINUOUS_INTEGRATION);
    PolicyEvaluation buildPolicyEvaluation =
        createPolicyEvaluation(Stage.ID_BUILD, ScanTriggerType.CONTINUOUS_INTEGRATION);

    PullRequestTargetCommitPolicyEvaluationResolver policyEvaluationResolver =
        new TestableTargetCommitPolicyEvaluationBuilder()
            .withSourceStagePolicyEvaluation(sourcePolicyEvaluation)
            .withLatestCommonAncestorPolicyEvaluation(sourcePolicyEvaluation)
            .withBuildStagePolicyEvaluation(buildPolicyEvaluation)
            .hasExternalPolicyEvaluations(true)
            .withCommonAncestor(commonCommit)
            .build();

    // when
    PolicyEvaluation targetCommitPolicyEvaluation =
        policyEvaluationResolver.getOrPerformTargetCommitPolicyEvaluation(
            application, gitRepositoryInfo, baseBranchName, baseCommit, headBranchName);

    // then: the most recent policy evaluation available (SOURCE stage) is not used
    // the build policy evaluation is preferred over the source one
    assertThat(targetCommitPolicyEvaluation).isEqualTo(buildPolicyEvaluation);

    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(eq(application.getId()), any(), any(),
        any());
  }

  @Test
  public void testGetOrPerformTargetCommitPolicyEvaluation_featureBranchOnDefaultBranch_noExistingPolicyEvaluations() //
      throws Exception //
  {
    // setup
    String defaultBranchName = "branch-default";
    String featureBranchName = "branch-feature";
    String featureBranchCommit = "commit-feature";
    String commonCommit = "common-commit";

    gitRepositoryInfo.baseBranch = defaultBranchName;

    PolicyEvaluation defaultBranchPolicyEvaluation =
        createPolicyEvaluation(Stage.ID_SOURCE, ScanTriggerType.CONTINUOUS_INTEGRATION);

    PullRequestTargetCommitPolicyEvaluationResolver policyEvaluationResolver =
        new TestableTargetCommitPolicyEvaluationBuilder().hasExternalPolicyEvaluations(false)
            .withCommonAncestor(commonCommit)
            .build();
    doReturn(defaultBranchPolicyEvaluation).when(mockSourceControlScanService)
        .doSynchronousSourceControlScan(eq(application.getId()), any(), eq(defaultBranchName), eq(commonCommit));

    // when
    PolicyEvaluation targetCommitPolicyEvaluation = policyEvaluationResolver.getOrPerformTargetCommitPolicyEvaluation(
        application, gitRepositoryInfo, defaultBranchName, featureBranchCommit, featureBranchName);
    // Sanity check
    assertThat(targetCommitPolicyEvaluation).isEqualTo(defaultBranchPolicyEvaluation);
    assertThat(targetCommitPolicyEvaluation.getStageTypeId()).isEqualTo(Stage.ID_SOURCE);

    // then: a policy evaluation at develop stage is performed for defaultBranchName/commonCommit
    ArgumentCaptor<Stage> stageArgumentCaptor = ArgumentCaptor.forClass(Stage.class);
    verify(mockSourceControlScanService, times(1)).doSynchronousSourceControlScan(eq(application.getId()),
        stageArgumentCaptor.capture(), eq(defaultBranchName), eq(commonCommit));
    assertThat(stageArgumentCaptor.getValue().getStageTypeId()).isEqualTo(Stage.ID_SOURCE);
  }

  @Test
  public void testGetOrPerformTargetCommitPolicyEvaluation_featureBranchOnFeatureBranch_noExistingPolicyEvaluations() //
      throws Exception //
  {
    // setup
    String baseBranchName = "branch-base";
    String childBranchName = "branch-child";
    String childBranchCommit = "commit-child";
    String commonCommit = "common-commit";

    PolicyEvaluation baseBranchPolicyEvaluation =
        createPolicyEvaluation(Stage.ID_DEVELOP, ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST);

    PullRequestTargetCommitPolicyEvaluationResolver policyEvaluationResolver =
        new TestableTargetCommitPolicyEvaluationBuilder()
            .hasExternalPolicyEvaluations(false)
            .withCommonAncestor(commonCommit)
            .build();
    doReturn(baseBranchPolicyEvaluation).when(mockSourceControlScanService)
        .doSynchronousSourceControlScan(eq(application.getId()), any(), eq(baseBranchName), eq(commonCommit));

    // when
    PolicyEvaluation targetCommitPolicyEvaluation = policyEvaluationResolver.getOrPerformTargetCommitPolicyEvaluation(
        application, gitRepositoryInfo, baseBranchName, childBranchCommit, childBranchName);
    // Sanity check
    assertThat(targetCommitPolicyEvaluation).isEqualTo(baseBranchPolicyEvaluation);
    assertThat(targetCommitPolicyEvaluation.getStageTypeId()).isEqualTo(Stage.ID_DEVELOP);

    // then: a policy evaluation at develop stage is performed for baseBranchName/commonCommit
    ArgumentCaptor<Stage> stageArgumentCaptor = ArgumentCaptor.forClass(Stage.class);
    verify(mockSourceControlScanService, times(1)).doSynchronousSourceControlScan(eq(application.getId()),
        stageArgumentCaptor.capture(), eq(baseBranchName), eq(commonCommit));
    assertThat(stageArgumentCaptor.getValue().getStageTypeId()).isEqualTo(Stage.ID_DEVELOP);
  }

  @Test
  public void testGetOrPerformTargetCommitPolicyEvaluation_buildAndSourceStagePolicyEvaluations_mixedTriggers() throws GitException, IOException {
    // setup
    final String headBranchName = "feature-1";
    final String baseBranchName = "main";
    final String baseCommit = "base-commit";
    final String commonCommit = "common-commit";

    PolicyEvaluation sourcePolicyEvaluation =
        createPolicyEvaluation(Stage.ID_SOURCE, ScanTriggerType.CONTINUOUS_INTEGRATION);
    PolicyEvaluation buildPolicyEvaluation =
        createPolicyEvaluation(Stage.ID_BUILD, ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST);

    PullRequestTargetCommitPolicyEvaluationResolver policyEvaluationResolver =
        new TestableTargetCommitPolicyEvaluationBuilder()
            .withSourceStagePolicyEvaluation(sourcePolicyEvaluation)
            .withLatestCommonAncestorPolicyEvaluation(sourcePolicyEvaluation)
            .withBuildStagePolicyEvaluation(buildPolicyEvaluation)
            .hasExternalPolicyEvaluations(true)
            .withCommonAncestor(commonCommit)
            .build();

    // when
    PolicyEvaluation targetCommitPolicyEvaluation =
        policyEvaluationResolver.getOrPerformTargetCommitPolicyEvaluation(
            application, gitRepositoryInfo, baseBranchName, baseCommit, headBranchName);

    // then: the most recent policy evaluation available (SOURCE stage; externally triggered) is used
    // the build policy evaluation is nor eligible because it's internally triggered
    assertThat(targetCommitPolicyEvaluation).isEqualTo(sourcePolicyEvaluation);

    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(eq(application.getId()), any(), any(),
        any());
  }

  private PolicyEvaluation createPolicyEvaluation(String stageTypeId, ScanTriggerType scanTriggerType) {
    PolicyEvaluation policyEvaluation =
        new PolicyEvaluation("app1", stageTypeId, "scan-id", "init", scanTriggerType);
    policyEvaluation.setCommitHash("commit-123");
    policyEvaluation.setTime(new Date());
    return policyEvaluation;
  }

  private class TestableTargetCommitPolicyEvaluationBuilder
  {
    @Mock
    private GitApiFactory gitApiFactory;

    @Mock
    private GitApi gitApi;

    @Mock
    private SourceControlUtils mockSourceControlUtils;

    @Mock
    private PolicyEvaluationDAO mockPolicyEvaluationDAO;

    private PolicyEvaluation latestBaseCommitPolicyEvaluation;

    private PolicyEvaluation latestCommonAncestorPolicyEvaluation;

    private PolicyEvaluation buildStagePolicyEvaluation;

    private PolicyEvaluation sourceStagePolicyEvaluation;

    private boolean hasExternalPolicyEvaluations = false;

    private String commonAncestorCommit = null;

    private String baseCommit = null;

    TestableTargetCommitPolicyEvaluationBuilder() {
      MockitoAnnotations.openMocks(this);
    }

    TestableTargetCommitPolicyEvaluationBuilder withLatestBaseCommitPolicyEvaluation(
        PolicyEvaluation policyEvaluation)
    {
      latestBaseCommitPolicyEvaluation = policyEvaluation;
      return this;
    }

    TestableTargetCommitPolicyEvaluationBuilder withLatestCommonAncestorPolicyEvaluation(
        PolicyEvaluation policyEvaluation)
    {
      latestCommonAncestorPolicyEvaluation = policyEvaluation;
      return this;
    }

    TestableTargetCommitPolicyEvaluationBuilder withBuildStagePolicyEvaluation(PolicyEvaluation policyEvaluation) {
      buildStagePolicyEvaluation = policyEvaluation;
      return this;
    }

    TestableTargetCommitPolicyEvaluationBuilder withSourceStagePolicyEvaluation(PolicyEvaluation policyEvaluation) {
      sourceStagePolicyEvaluation = policyEvaluation;
      return this;
    }

    TestableTargetCommitPolicyEvaluationBuilder hasExternalPolicyEvaluations(boolean hasExternalPolicyEvaluations) {
      this.hasExternalPolicyEvaluations = hasExternalPolicyEvaluations;
      return this;
    }

    TestableTargetCommitPolicyEvaluationBuilder withCommonAncestor(String commit) {
      this.commonAncestorCommit = commit;
      return this;
    }

    TestableTargetCommitPolicyEvaluationBuilder withBaseCommit(String commit) {
      this.baseCommit = commit;
      return this;
    }

    PullRequestTargetCommitPolicyEvaluationResolver build() throws GitException, IOException {
      doReturn(gitApi).when(gitApiFactory).createGitApi(any());
      doReturn(commonAncestorCommit).when(gitApi).getCommonAncestorCommit(any(), any(), any());

      doReturn(sourceControlDir).when(mockSourceControlUtils).getCheckoutDirectory(any(Application.class));

      if (baseCommit != null) {
        doReturn(latestBaseCommitPolicyEvaluation).when(mockPolicyEvaluationDAO)
            .getLastByApplicationAndCommitHashAndTriggerType(any(), eq(baseCommit), anyBoolean());
      }
      if (commonAncestorCommit != null) {
        doReturn(latestCommonAncestorPolicyEvaluation).when(mockPolicyEvaluationDAO)
            .getLastByApplicationAndCommitHashAndTriggerType(any(), eq(commonAncestorCommit), anyBoolean());
      }

      doReturn(buildStagePolicyEvaluation).when(mockPolicyEvaluationDAO)
          .getLastByOwnerIdCommitHashAndStageId(any(), any(), eq(Stage.ID_BUILD));
      doReturn(sourceStagePolicyEvaluation).when(mockPolicyEvaluationDAO)
          .getLastByOwnerIdCommitHashAndStageId(any(), any(), eq(Stage.ID_SOURCE));

      doReturn(hasExternalPolicyEvaluations).when(mockPolicyEvaluationDAO)
          .hasExternalPolicyEvaluations(any(), any());

      doReturn(sourceStagePolicyEvaluation).when(mockSourceControlScanService)
          .doSynchronousSourceControlScan(any(), any(), any());

      return new PullRequestTargetCommitPolicyEvaluationResolver(
          gitApiFactory,
          mockSourceControlUtils,
          mockPolicyEvaluationDAO,
          mockSourceControlScanService);
    }
  }
}
