/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.Status;
import com.sonatype.nexus.scm.api.model.StatusRequest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class PullRequestStatusServiceTest
    extends VerifiableLoggingTestBase
{
  public static final String DEFAULT_REPO_URL = "https://localhost:9999/sonatype/test-repo";

  public static final String SUCCEED_STATUS = "succeed";

  public static final String STATUS_RESPONSE = "status created";

  @Mock
  private GitClientFactory mockGitClientFactory;

  @Mock
  private ScanPolicyEvaluator mockScanPolicyEvaluator;

  @Mock
  private ScmStatusHelper mockScmStatusHelper;

  @Mock
  private GitApiClient mockGitApiClient;

  @Mock
  private StatusRequest mockStatusRequest;

  @Mock
  private Status mockStatus;

  @Mock
  private PolicyEvaluationResult mockPolicyEvaluationResult;

  @Mock
  private PolicyEvaluation mockPolicyEvaluation;

  @InjectMocks
  private PullRequestStatusService pullRequestStatusService;

  private static final Integer PR_ID = 9;

  public PullRequestStatusServiceTest() {
    super(PullRequestStatusService.class);
  }

  @Before
  @Override
  public void setup() {
    MockitoAnnotations.openMocks(this);
    super.setup();
  }

  @Test
  public void testDoCreatePullRequestStatus_notSupportedForGitHub() throws Exception {
    // given: a pull request policy evaluation
    SourceControlProvider provider = SourceControlProvider.GITHUB;
    PullRequestPolicyEvaluationsDTO dto = buildPullRequestPolicyEvaluationsDTO(provider);

    // when: creating a pull request status
    pullRequestStatusService.doCreatePullRequestStatus(dto);

    // then: no pull request status is created
    verifyPullRequestStatusNotCreated();
  }

  @Test
  public void testDoCreatePullRequestStatus_notSupportedForGitLab() throws Exception {
    // given: a pull request policy evaluation
    SourceControlProvider provider = SourceControlProvider.GITLAB;
    PullRequestPolicyEvaluationsDTO dto = buildPullRequestPolicyEvaluationsDTO(provider);

    // when: creating a pull request status
    pullRequestStatusService.doCreatePullRequestStatus(dto);

    // then: no pull request status is created
    verifyPullRequestStatusNotCreated();
  }

  @Test
  public void testDoCreatePullRequestStatus_notSupportedForBitbucket() throws Exception {
    // given: a pull request policy evaluation
    SourceControlProvider provider = SourceControlProvider.BITBUCKET;
    PullRequestPolicyEvaluationsDTO dto = buildPullRequestPolicyEvaluationsDTO(provider);

    // when: creating a pull request status
    pullRequestStatusService.doCreatePullRequestStatus(dto);

    // then: no pull request status is created
    verifyPullRequestStatusNotCreated();
  }

  @Test
  public void testDoCreatePullRequestStatus_success() throws Exception {
    // given: a valid system configuration
    createMocksForPullRequestStatusRequestCreation();
    mockCreatePullRequestStatusCreation(null);

    // and: a new pull request policy evaluation
    SourceControlProvider provider = SourceControlProvider.AZURE;
    PullRequestPolicyEvaluationsDTO dto = buildPullRequestPolicyEvaluationsDTO(provider);

    // when: creating a pull request status
    pullRequestStatusService.doCreatePullRequestStatus(dto);

    // then : pull request status creation was triggered
    verifyPullRequestStatusCreationTriggered(dto.getGitRepositoryInfo());

    // and: proper log is created
    assertThatLogMessagesEqual(
        info(String.format("Pull request status sent for repository: %s," +
            " pull request: %s state: %s, response: %s", DEFAULT_REPO_URL,
            PR_ID, SUCCEED_STATUS, STATUS_RESPONSE)));
  }

  @Test
  public void testDoCreatePullRequestStatus_notCreatedIfEvaluationDoesNotExists() throws Exception {
    // given: a valid system configuration
    createMocksForPullRequestStatusRequestCreation();
    IOException exception = new IOException("SCM error");
    mockCreatePullRequestStatusCreation(exception);

    // and: a new pull request policy evaluation
    SourceControlProvider provider = SourceControlProvider.AZURE;
    PullRequestPolicyEvaluationsDTO dto = buildPullRequestPolicyEvaluationsDTO(provider);

    // expect:
    assertThatExceptionOfType(SourceControlException.class)
        .isThrownBy(() -> pullRequestStatusService.doCreatePullRequestStatus(dto))
        .withMessage(String.format("Failed to update pull request status for repository: %s, " +
            "pull request Id: %s reason: %s", DEFAULT_REPO_URL,
            PR_ID, exception.getMessage()));

    // then : pull request status creation was triggered
    verifyPullRequestStatusCreationTriggered(dto.getGitRepositoryInfo());
  }

  private void verifyPullRequestStatusNotCreated() throws Exception {
    // verify policy evaluation result is NOT created
    verify(mockScanPolicyEvaluator, times(0)).createPolicyEvaluationResult(any(), anyBoolean());

    // verify git api client is NOT created
    verify(mockGitClientFactory, times(0)).createApiClient(any());

    // verify a status is NOT created
    verify(mockScmStatusHelper, times(0)).createStatusRequestFromPolicyEvaluation(any(),
        any(), any(), any());

    // verify pull request status is NOT created
    verify(mockGitApiClient, times(0)).createPullRequestStatus(any(), any());
  }

  private void verifyPullRequestStatusCreationTriggered(GitRepositoryInfo repositoryInfo) throws Exception {

    // verify policy evaluation result is created
    verify(mockScanPolicyEvaluator, times(1)).createPolicyEvaluationResult(
        mockPolicyEvaluation, true);

    // verify git api client is created
    verify(mockGitClientFactory, times(1)).createApiClient(repositoryInfo);

    // verify status is created
    verify(mockScmStatusHelper, times(1))
        .createStatusRequestFromPolicyEvaluation(mockPolicyEvaluation, mockPolicyEvaluationResult,
            mockGitApiClient, SourceControlProvider.AZURE);

    // verify pull request status is created
    verify(mockGitApiClient, times(1)).createPullRequestStatus(PR_ID, mockStatusRequest);
  }

  private void createMocksForPullRequestStatusRequestCreation() {
    // mock policy evaluation result creation
    doReturn(mockPolicyEvaluationResult).when(mockScanPolicyEvaluator)
        .createPolicyEvaluationResult(any(PolicyEvaluation.class), anyBoolean());

    // mock git api client factory
    doReturn(mockGitApiClient).when(mockGitClientFactory)
        .createApiClient(any(GitRepositoryInfo.class));

    // mock status request creation
    doReturn(SUCCEED_STATUS).when(mockStatusRequest).getState();
    doReturn(mockStatusRequest).when(mockScmStatusHelper)
        .createStatusRequestFromPolicyEvaluation(mockPolicyEvaluation, mockPolicyEvaluationResult,
            mockGitApiClient, SourceControlProvider.AZURE);
  }

  private void mockCreatePullRequestStatusCreation(Exception exceptionToThrow) throws Exception {
    if (exceptionToThrow != null) {
      // throw exception
      doThrow(exceptionToThrow).when(mockGitApiClient)
          .createPullRequestStatus(PR_ID, mockStatusRequest);
    }
    else {
      // mock pull request status creation
      doReturn(STATUS_RESPONSE).when(mockStatus).toString();
      doReturn(mockStatus).when(mockGitApiClient)
          .createPullRequestStatus(PR_ID, mockStatusRequest);
    }
  }

  private PullRequestPolicyEvaluationsDTO buildPullRequestPolicyEvaluationsDTO(SourceControlProvider provider) {
    GitRepositoryInfo gitRepositoryInfo = buildRepositoryInfo(provider);
    return new PullRequestPolicyEvaluationsDTO()
        .setGitRepositoryInfo(gitRepositoryInfo)
        .setPullRequestNumber(PR_ID)
        .setFeatureBranchPolicyEvaluation(mockPolicyEvaluation);
  }

  private GitRepositoryInfo buildRepositoryInfo(final SourceControlProvider provider) {
    GitRepositoryInfo repositoryInfo = new GitRepositoryInfo();
    repositoryInfo.provider = provider;
    repositoryInfo.repositoryUrl = DEFAULT_REPO_URL;
    repositoryInfo.normalizedRepositoryUrl = DEFAULT_REPO_URL;
    return repositoryInfo;
  }
}
