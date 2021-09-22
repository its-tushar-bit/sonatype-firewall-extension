/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.Date;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.nexus.git.utils.api.GitException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PullRequestBaseCommitPolicyEvaluationResolverTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private SourceControlScanService mockSourceControlScanService;

  public PullRequestBaseCommitPolicyEvaluationResolverTest() {
    super(PullRequestBaseCommitPolicyEvaluationResolver.class);
  }

  @Before
  @Override
  public void setup() {
    super.setup();
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testGetOrPerformBaseCommitPolicyEvaluation_noExistingPolicyEvaluations()
      throws GitException, IOException
  {
    // setup
    final String applicationId = "app1";
    final String commit = "commit-123";
    final String baseBranchName = "main";
    PullRequestBaseCommitPolicyEvaluationResolver policyEvaluationResolver =
        new TestableBaseCommitPolicyEvaluationBuilder()
            .build();

    // when
    policyEvaluationResolver.getOrPerformBaseCommitPolicyEvaluation(applicationId, baseBranchName, commit);

    // then: a sync scan is performed
    verify(mockSourceControlScanService, times(1)).doSynchronousSourceControlScan(
        eq(applicationId), any(), eq(baseBranchName), eq(commit));
  }

  @Test
  public void testGetOrPerformBaseCommitPolicyEvaluation_noPolicyEvaluation_externalEvaluationsExist()
      throws GitException, IOException
  {
    // setup
    final String applicationId = "app1";
    final String commit = "commit-123";
    final String baseBranchName = "main";
    PullRequestBaseCommitPolicyEvaluationResolver policyEvaluationResolver =
        new TestableBaseCommitPolicyEvaluationBuilder()
            .hasExternalPolicyEvaluations(true)
            .build();

    // when
    PolicyEvaluation baseCommitPolicyEvaluation =
        policyEvaluationResolver.getOrPerformBaseCommitPolicyEvaluation(applicationId, baseBranchName, commit);

    // then: no record returned and no scan is triggered
    assertThat(baseCommitPolicyEvaluation).isNull();

    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(
        eq(applicationId), any(), eq(baseBranchName), eq(commit));
  }

  @Test
  public void testGetOrPerformBaseCommitPolicyEvaluation_internalPolicyEvaluationExists()
      throws GitException, IOException
  {
    // setup
    final String applicationId = "app1";
    final String commit = "commit-123";
    final String baseBranchName = "main";
    PolicyEvaluation policyEvaluation =
        createPolicyEvaluation(Stage.ID_BUILD, ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST);

    PullRequestBaseCommitPolicyEvaluationResolver policyEvaluationResolver =
        new TestableBaseCommitPolicyEvaluationBuilder()
            .withLatestBaseCommitPolicyEvaluation(policyEvaluation)
            .withBuildStagePolicyEvaluation(policyEvaluation)
            .build();

    // when
    PolicyEvaluation baseCommitPolicyEvaluation =
        policyEvaluationResolver.getOrPerformBaseCommitPolicyEvaluation(applicationId, baseBranchName, commit);

    // then: the only policy evaluation available is used
    assertThat(baseCommitPolicyEvaluation).isEqualTo(policyEvaluation);

    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(eq(applicationId), any(), any());
  }

  @Test
  public void testGetOrPerformBaseCommitPolicyEvaluation_externalPolicyEvaluationExists()
      throws GitException, IOException
  {
    // setup
    final String applicationId = "app1";
    final String commit = "commit-123";
    final String baseBranchName = "main";
    PolicyEvaluation policyEvaluation = createPolicyEvaluation(Stage.ID_BUILD, ScanTriggerType.CONTINUOUS_INTEGRATION);

    PullRequestBaseCommitPolicyEvaluationResolver policyEvaluationResolver =
        new TestableBaseCommitPolicyEvaluationBuilder()
            .withLatestBaseCommitPolicyEvaluation(policyEvaluation)
            .withBuildStagePolicyEvaluation(policyEvaluation)
            .hasExternalPolicyEvaluations(true)
            .build();

    // when
    PolicyEvaluation baseCommitPolicyEvaluation =
        policyEvaluationResolver.getOrPerformBaseCommitPolicyEvaluation(applicationId, baseBranchName, commit);

    // then: the only policy evaluation available is used
    assertThat(baseCommitPolicyEvaluation).isEqualTo(policyEvaluation);

    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(eq(applicationId), any(), any());
  }

  @Test
  public void testGetOrPerformBaseCommitPolicyEvaluation_externalReleaseStagePolicyEvaluationExists()
      throws GitException, IOException
  {
    // setup
    final String applicationId = "app1";
    final String commit = "commit-123";
    final String baseBranchName = "main";
    PolicyEvaluation policyEvaluation =
        createPolicyEvaluation(Stage.ID_RELEASE, ScanTriggerType.CONTINUOUS_INTEGRATION);

    PullRequestBaseCommitPolicyEvaluationResolver policyEvaluationResolver =
        new TestableBaseCommitPolicyEvaluationBuilder()
            .withLatestBaseCommitPolicyEvaluation(policyEvaluation)
            .hasExternalPolicyEvaluations(true)
            .build();

    // when
    PolicyEvaluation baseCommitPolicyEvaluation =
        policyEvaluationResolver.getOrPerformBaseCommitPolicyEvaluation(applicationId, baseBranchName, commit);

    // then: the only policy evaluation available is used, even though we prefer BUILD and SOURCE stage evaluations
    assertThat(baseCommitPolicyEvaluation).isEqualTo(policyEvaluation);

    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(eq(applicationId), any(), any());
  }

  @Test
  public void testGetOrPerformBaseCommitPolicyEvaluation_buildAndSourceStagePolicyEvaluations()
      throws GitException, IOException
  {
    // setup
    final String applicationId = "app1";
    final String commit1 = "commit1";
    final String baseBranchName = "main";
    PolicyEvaluation sourcePolicyEvaluation =
        createPolicyEvaluation(Stage.ID_SOURCE, ScanTriggerType.CONTINUOUS_INTEGRATION);
    PolicyEvaluation buildPolicyEvaluation =
        createPolicyEvaluation(Stage.ID_BUILD, ScanTriggerType.CONTINUOUS_INTEGRATION);

    PullRequestBaseCommitPolicyEvaluationResolver policyEvaluationResolver =
        new TestableBaseCommitPolicyEvaluationBuilder()
            .withSourceStagePolicyEvaluation(sourcePolicyEvaluation)
            .withLatestBaseCommitPolicyEvaluation(sourcePolicyEvaluation)
            .withBuildStagePolicyEvaluation(buildPolicyEvaluation)
            .hasExternalPolicyEvaluations(true)
            .build();

    // when
    PolicyEvaluation baseCommitPolicyEvaluation =
        policyEvaluationResolver.getOrPerformBaseCommitPolicyEvaluation(applicationId, baseBranchName, commit1);

    // then: the most recent policy evaluation available (SOURCE stage) is not used
    // the build policy evaluation is preferred over the source one
    assertThat(baseCommitPolicyEvaluation).isEqualTo(buildPolicyEvaluation);

    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(eq(applicationId), any(), any(),
        any());
  }

  @Test
  public void testGetOrPerformBaseCommitPolicyEvaluation_buildAndSourceStagePolicyEvaluations_mixedTriggers()
      throws GitException, IOException
  {
    // setup
    final String applicationId = "app1";
    final String commit1 = "commit1";
    final String baseBranchName = "main";
    PolicyEvaluation sourcePolicyEvaluation =
        createPolicyEvaluation(Stage.ID_SOURCE, ScanTriggerType.CONTINUOUS_INTEGRATION);
    PolicyEvaluation buildPolicyEvaluation =
        createPolicyEvaluation(Stage.ID_BUILD, ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST);

    PullRequestBaseCommitPolicyEvaluationResolver policyEvaluationResolver =
        new TestableBaseCommitPolicyEvaluationBuilder()
            .withSourceStagePolicyEvaluation(sourcePolicyEvaluation)
            .withLatestBaseCommitPolicyEvaluation(sourcePolicyEvaluation)
            .withBuildStagePolicyEvaluation(buildPolicyEvaluation)
            .hasExternalPolicyEvaluations(true)
            .build();

    // when
    PolicyEvaluation baseCommitPolicyEvaluation =
        policyEvaluationResolver.getOrPerformBaseCommitPolicyEvaluation(applicationId, baseBranchName, commit1);

    // then: the most recent policy evaluation available (SOURCE stage; externally triggered) is used
    // the build policy evaluation is nor eligible because it's internally triggered
    assertThat(baseCommitPolicyEvaluation).isEqualTo(sourcePolicyEvaluation);

    verify(mockSourceControlScanService, never()).doSynchronousSourceControlScan(eq(applicationId), any(), any(),
        any());
  }

  private PolicyEvaluation createPolicyEvaluation(String stageTypeId, ScanTriggerType scanTriggerType) {
    PolicyEvaluation policyEvaluation =
        new PolicyEvaluation("app1", stageTypeId, "scan-id", "init", scanTriggerType);
    policyEvaluation.setCommitHash("commit-123");
    policyEvaluation.setTime(new Date());
    return policyEvaluation;
  }

  private class TestableBaseCommitPolicyEvaluationBuilder
  {
    @Mock
    private PolicyEvaluationDAO mockPolicyEvaluationDAO;

    private PolicyEvaluation latestBaseCommitPolicyEvaluation;

    private PolicyEvaluation buildStagePolicyEvaluation;

    private PolicyEvaluation sourceStagePolicyEvaluation;

    private PolicyEvaluation syncRunPolicyEvaluation;

    private boolean hasExternalPolicyEvaluations = false;

    TestableBaseCommitPolicyEvaluationBuilder() {
      MockitoAnnotations.openMocks(this);
    }

    TestableBaseCommitPolicyEvaluationBuilder withLatestBaseCommitPolicyEvaluation(PolicyEvaluation policyEvaluation) {
      latestBaseCommitPolicyEvaluation = policyEvaluation;
      return this;
    }

    TestableBaseCommitPolicyEvaluationBuilder withBuildStagePolicyEvaluation(PolicyEvaluation policyEvaluation) {
      buildStagePolicyEvaluation = policyEvaluation;
      return this;
    }

    TestableBaseCommitPolicyEvaluationBuilder withSourceStagePolicyEvaluation(PolicyEvaluation policyEvaluation) {
      sourceStagePolicyEvaluation = policyEvaluation;
      return this;
    }

    TestableBaseCommitPolicyEvaluationBuilder withSyncRunPolicyEvaluation(PolicyEvaluation policyEvaluation) {
      syncRunPolicyEvaluation = policyEvaluation;
      return this;
    }

    TestableBaseCommitPolicyEvaluationBuilder hasExternalPolicyEvaluations(boolean hasExternalPolicyEvaluations) {
      this.hasExternalPolicyEvaluations = hasExternalPolicyEvaluations;
      return this;
    }

    PullRequestBaseCommitPolicyEvaluationResolver build() throws GitException, IOException {
      doReturn(latestBaseCommitPolicyEvaluation).when(mockPolicyEvaluationDAO)
          .getLastByApplicationAndCommitHashAndTriggerType(any(), any(), anyBoolean());

      doReturn(buildStagePolicyEvaluation).when(mockPolicyEvaluationDAO)
          .getLastByApplicationIdCommitHashAndStageId(any(), any(), eq(Stage.ID_BUILD));
      doReturn(sourceStagePolicyEvaluation).when(mockPolicyEvaluationDAO)
          .getLastByApplicationIdCommitHashAndStageId(any(), any(), eq(Stage.ID_SOURCE));

      doReturn(hasExternalPolicyEvaluations).when(mockPolicyEvaluationDAO)
          .hasExternalPolicyEvaluations(any(), any());

      doReturn(sourceStagePolicyEvaluation).when(mockSourceControlScanService)
          .doSynchronousSourceControlScan(any(), any(), any());
      doReturn(syncRunPolicyEvaluation).when(mockSourceControlScanService)
          .doSynchronousSourceControlScan(any(), any(), any(), notNull());

      return new PullRequestBaseCommitPolicyEvaluationResolver(
          mockPolicyEvaluationDAO,
          mockSourceControlScanService);
    }
  }
}
