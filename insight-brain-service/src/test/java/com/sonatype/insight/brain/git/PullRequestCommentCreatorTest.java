/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.development.prioritization.DevelopmentPrioritiesUtilsService;
import com.sonatype.insight.brain.metrics.ScmOperationMetrics;
import com.sonatype.insight.brain.git.dto.PullRequestLineCommentCreationResult;
import com.sonatype.insight.brain.scm.event.PullRequestCommentingLogger;
import com.sonatype.insight.brain.scm.event.SourceControlEventLoggerFactory;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetryDataObfuscator;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.model.CommentResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Category(SlowTest.class)
public class PullRequestCommentCreatorTest
    extends VerifiableLoggingTestBase
{
  public PullRequestCommentCreatorTest() {
    super(PullRequestCommentCreator.class);
  }

  @Mock
  private PullRequestCommentingClient mockCommentingClient;

  @Mock
  private PullRequestCommentingMetricsService mockCommentingMetricsService;

  private Configuration configurationMock = mock(Configuration.class);

  private TelemetryUtils telemetryUtils = new TelemetryUtils(new TelemetryDataObfuscator(configurationMock));

  @Before
  @Override
  public void setup() {
    MockitoAnnotations.openMocks(this);
    super.setup();
  }

  @Test
  public void testDoCreateOrUpdateComments_noMarkup() throws IOException {
    // given: valid test case but no markup generated
    final String featureBranchHeadCommit = "feature-1-commit-1";
    TestCase testCase = new TestCase()
        .forApplication("app1")
        .withPullRequest(1, featureBranchHeadCommit)
        .withDefaultBranchPolicyEvaluation("default-eval-1", "default-commit-1")
        .withFeatureBranchPolicyEvaluation("feature-eval-1", featureBranchHeadCommit)
        .withContentHash("contentHash");

    PullRequestCommentCreator pullRequestCommentCreator = new TestablePullRequestCommentCreatorBuilder()
        .withoutMarkup()
        .build();

    // when: try to create a comment
    pullRequestCommentCreator
        .createPullRequestComment(testCase.pullRequestPolicyEvaluationsDTO, testCase.policyViolationDiff,
            testCase.remediationVersionMap, testCase.contentHash);

    // then: no comment created and no telemetry generated
    assertThatLogMessagesEqual(
        info("generated feedback markup was empty for application 'app1' pull request '1'")
    );
    verify(mockCommentingClient, never()).createOrUpdateCommentInGitSCM(any(), any(), anyInt(), any(), any(), any());
    verify(mockCommentingMetricsService, never()).sendTelemetry(any());

    // and when: try to update a comment
    testCase.withExistingPullRequestComment().withPullRequest(2, featureBranchHeadCommit);
    pullRequestCommentCreator
        .updatePullRequestComment(testCase.pullRequestPolicyEvaluationsDTO, testCase.existingPullRequestComment,
            testCase.policyViolationDiff, testCase.remediationVersionMap, testCase.contentHash);

    // then: no comment updated and no telemetry generated
    assertThatLogMessagesEqual(
        info("generated feedback markup was empty for application 'app1' pull request '1'"),
        info("generated feedback markup was empty for application 'app1' pull request '2'")
    );
    verify(mockCommentingClient, never()).createOrUpdateCommentInGitSCM(any(), any(), anyInt(), any(), any(), any());
    verify(mockCommentingMetricsService, never()).sendTelemetry(any());
  }

  @Test
  public void testCreatePullRequestComment_withMarkup() throws IOException {
    // given: valid test case but no markup generated
    final String featureBranchHeadCommit = "feature-1-commit-1";
    TestCase testCase = new TestCase()
        .forApplication("app1")
        .withPullRequest(1, featureBranchHeadCommit)
        .withDefaultBranchPolicyEvaluation("default-eval-1", "default-commit-1")
        .withFeatureBranchPolicyEvaluation("feature-eval-1", featureBranchHeadCommit)
        .withContentHash("contentHash");

    PullRequestPostCommentAction mockPostCommentAction = mock(PullRequestPostCommentAction.class);

    PullRequestCommentCreator pullRequestCommentCreator = new TestablePullRequestCommentCreatorBuilder()
        .withLineComments(5)
        .withMarkup("simulated-markup")
        .withPostCommentAction(mockPostCommentAction)
        .build();

    testCase.policyViolationDiff.addAppeared(new PolicyViolation());

    // when: try to create a comment
    pullRequestCommentCreator
        .createPullRequestComment(testCase.pullRequestPolicyEvaluationsDTO, testCase.policyViolationDiff,
            testCase.remediationVersionMap, testCase.contentHash);

    // then: comment created, metrics generated, and downstream processes invoked
    verify(mockCommentingClient, times(1)).createOrUpdateCommentInGitSCM(any(), any(), anyInt(), any(), any(), any());
    ArgumentCaptor<PullRequestCommentTelemetry> telemetryCaptor =
        ArgumentCaptor.forClass(PullRequestCommentTelemetry.class);
    verify(mockCommentingMetricsService).sendTelemetry(telemetryCaptor.capture());
    assertThat(telemetryCaptor.getValue().lineCommentCount).isEqualTo(5);
    verify(mockPostCommentAction, times(1)).invokeAction(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testCreatePullRequestComment_withMarkup_NoPolicyDiff_Bitbucket() throws IOException {
    when(configurationMock.getAdvanceReportingInsightsEnabled()).thenReturn(true);

    final String featureBranchHeadCommit = "feature-1-commit-1";
    TestCase testCase = new TestCase()
        .forApplication("app1")
        .withPullRequest(1, featureBranchHeadCommit)
        .withDefaultBranchPolicyEvaluation("default-eval-1", "default-commit-1")
        .withFeatureBranchPolicyEvaluation("feature-eval-1", featureBranchHeadCommit)
        .withContentHash("contentHash");

    PullRequestPostCommentAction mockPostCommentAction = mock(PullRequestPostCommentAction.class);

    PullRequestCommentCreator pullRequestCommentCreator = new TestablePullRequestCommentCreatorBuilder()
        .withLineComments(5)
        .withMarkup("simulated-markup")
        .withPostCommentAction(mockPostCommentAction)
        .build();

    GitRepositoryInfo repositoryInfo = new GitRepositoryInfo();
    repositoryInfo.provider = SourceControlProvider.BITBUCKET;

    testCase.pullRequestPolicyEvaluationsDTO.setGitRepositoryInfo(repositoryInfo);

    pullRequestCommentCreator
        .createPullRequestComment(testCase.pullRequestPolicyEvaluationsDTO, testCase.policyViolationDiff,
            testCase.remediationVersionMap, testCase.contentHash);

    // For bitbucket, we do not want the PR commenting, but we want the mockPostCommentAction which posts code insights
    verify(mockCommentingClient, never()).createOrUpdateCommentInGitSCM(any(), any(), anyInt(), any(), any(), any());
    ArgumentCaptor<PullRequestCommentTelemetry> telemetryCaptor =
        ArgumentCaptor.forClass(PullRequestCommentTelemetry.class);
    verify(mockCommentingMetricsService).sendTelemetry(telemetryCaptor.capture());
    assertThat(telemetryCaptor.getValue().lineCommentCount).isEqualTo(5);
    verify(mockPostCommentAction, times(1)).invokeAction(any(), any(), any(), any(), any(), any(), any(), any());
    assertThat(telemetryCaptor.getValue().realApplicationId).isEqualTo("app1");
  }

  @Test
  public void testCreatePullRequestComment_withMarkup_NoPolicyDiff_Bitbucket_noAdvancedReporting() throws IOException {
    final String featureBranchHeadCommit = "feature-1-commit-1";
    TestCase testCase = new TestCase()
        .forApplication("app1")
        .withPullRequest(1, featureBranchHeadCommit)
        .withDefaultBranchPolicyEvaluation("default-eval-1", "default-commit-1")
        .withFeatureBranchPolicyEvaluation("feature-eval-1", featureBranchHeadCommit)
        .withContentHash("contentHash");

    PullRequestPostCommentAction mockPostCommentAction = mock(PullRequestPostCommentAction.class);

    PullRequestCommentCreator pullRequestCommentCreator = new TestablePullRequestCommentCreatorBuilder()
        .withLineComments(5)
        .withMarkup("simulated-markup")
        .withPostCommentAction(mockPostCommentAction)
        .build();

    GitRepositoryInfo repositoryInfo = new GitRepositoryInfo();
    repositoryInfo.provider = SourceControlProvider.BITBUCKET;

    testCase.pullRequestPolicyEvaluationsDTO.setGitRepositoryInfo(repositoryInfo);

    pullRequestCommentCreator
        .createPullRequestComment(testCase.pullRequestPolicyEvaluationsDTO, testCase.policyViolationDiff,
            testCase.remediationVersionMap, testCase.contentHash);

    // For bitbucket, we do not want the PR commenting, but we want the mockPostCommentAction which posts code insights
    verify(mockCommentingClient, never()).createOrUpdateCommentInGitSCM(any(), any(), anyInt(), any(), any(), any());
    ArgumentCaptor<PullRequestCommentTelemetry> telemetryCaptor =
        ArgumentCaptor.forClass(PullRequestCommentTelemetry.class);
    verify(mockCommentingMetricsService).sendTelemetry(telemetryCaptor.capture());
    assertThat(telemetryCaptor.getValue().lineCommentCount).isEqualTo(5);
    verify(mockPostCommentAction, times(1)).invokeAction(any(), any(), any(), any(), any(), any(), any(), any());
    assertThat(telemetryCaptor.getValue().realApplicationId).isEqualTo(telemetryUtils.obfuscate("app1"));
  }

  @Test
  public void testCreatePullRequestComment_withMarkup_NoPolicyDiff_Github() throws IOException {
    final String featureBranchHeadCommit = "feature-1-commit-1";
    TestCase testCase = new TestCase()
        .forApplication("app1")
        .withPullRequest(1, featureBranchHeadCommit)
        .withDefaultBranchPolicyEvaluation("default-eval-1", "default-commit-1")
        .withFeatureBranchPolicyEvaluation("feature-eval-1", featureBranchHeadCommit)
        .withContentHash("contentHash");

    PullRequestPostCommentAction mockPostCommentAction = mock(PullRequestPostCommentAction.class);

    PullRequestCommentCreator pullRequestCommentCreator = new TestablePullRequestCommentCreatorBuilder()
        .withLineComments(5)
        .withMarkup("simulated-markup")
        .withPostCommentAction(mockPostCommentAction)
        .build();

    GitRepositoryInfo repositoryInfo = new GitRepositoryInfo();
    repositoryInfo.provider = SourceControlProvider.GITHUB;

    testCase.pullRequestPolicyEvaluationsDTO.setGitRepositoryInfo(repositoryInfo);

    pullRequestCommentCreator
        .createPullRequestComment(testCase.pullRequestPolicyEvaluationsDTO, testCase.policyViolationDiff,
            testCase.remediationVersionMap, testCase.contentHash);

    // For any SCM provider except bitbucket, we do not want to invoke PR commenting or post actions if
    // diff is empty
    verify(mockCommentingClient, never()).createOrUpdateCommentInGitSCM(any(), any(), anyInt(), any(), any(), any());
    verify(mockCommentingMetricsService, never()).sendTelemetry(any());
    verify(mockPostCommentAction, never()).invokeAction(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testUpdatePullRequestComment_withMarkup() throws IOException {
    // given: valid test case but no markup generated
    final String featureBranchHeadCommit = "feature-1-commit-1";
    TestCase testCase = new TestCase()
        .forApplication("app1")
        .withPullRequest(1, featureBranchHeadCommit)
        .withDefaultBranchPolicyEvaluation("default-eval-1", "default-commit-1")
        .withFeatureBranchPolicyEvaluation("feature-eval-1", featureBranchHeadCommit)
        .withContentHash("contentHash")
        .withExistingPullRequestComment();

    PullRequestPostCommentAction mockPostCommentAction = mock(PullRequestPostCommentAction.class);

    PullRequestCommentCreator pullRequestCommentCreator = new TestablePullRequestCommentCreatorBuilder()
        .withLineComments(3)
        .withMarkup("simulated-markup")
        .withPostCommentAction(mockPostCommentAction)
        .build();

    // when: try to create a comment
    pullRequestCommentCreator
        .updatePullRequestComment(testCase.pullRequestPolicyEvaluationsDTO, testCase.existingPullRequestComment,
            testCase.policyViolationDiff, testCase.remediationVersionMap, testCase.contentHash);

    // then: comment created, metrics generated, and downstream processes invoked
    verify(mockCommentingClient, times(1)).createOrUpdateCommentInGitSCM(any(), any(), anyInt(), any(), any(), any());
    ArgumentCaptor<PullRequestCommentTelemetry> telemetryCaptor =
        ArgumentCaptor.forClass(PullRequestCommentTelemetry.class);
    verify(mockCommentingMetricsService).sendTelemetry(telemetryCaptor.capture());
    assertThat(telemetryCaptor.getValue().lineCommentCount).isEqualTo(3);
    verify(mockPostCommentAction, times(1)).invokeAction(any(), any(), any(), any(), any(), any(), any(), any());
  }

  private static class TestCase
  {
    private final PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO =
        new PullRequestPolicyEvaluationsDTO();

    private final PolicyViolationDiff<PolicyViolation> policyViolationDiff = new PolicyViolationDiff<>();

    private final Map<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap = new HashMap<>();

    private String contentHash;

    private SourceControlPullRequestComment existingPullRequestComment;

    TestCase() {
      pullRequestPolicyEvaluationsDTO.setGitRepositoryInfo(
          new GitRepositoryInfo("http://gitlab.com/test/app1", null, "user", "token", SourceControlProvider.GITLAB,
              "master", true, true,true, true, true, true, false, null));
    }

    TestCase forApplication(String applicationId) {
      pullRequestPolicyEvaluationsDTO.setApplicationId(applicationId);
      return this;
    }

    TestCase withContentHash(String contentHash) {
      this.contentHash = contentHash;
      return this;
    }

    TestCase withExistingPullRequestComment() {
      existingPullRequestComment = new SourceControlPullRequestComment();
      return this;
    }

    TestCase withPullRequest(int pullRequestNumber, String headCommit) {
      pullRequestPolicyEvaluationsDTO
          .setPullRequestNumber(pullRequestNumber)
          .setPullRequestHeadCommit(headCommit);
      return this;
    }

    TestCase withDefaultBranchPolicyEvaluation(String id, String commitHash) {
      PolicyEvaluation policyEvaluation = new PolicyEvaluation();
      policyEvaluation.setId(id);
      policyEvaluation.setCommitHash(commitHash);
      pullRequestPolicyEvaluationsDTO.setTargetPolicyEvaluation(policyEvaluation);
      return this;
    }

    TestCase withFeatureBranchPolicyEvaluation(String id, String commitHash) {
      PolicyEvaluation policyEvaluation = new PolicyEvaluation();
      policyEvaluation.setId(id);
      policyEvaluation.setCommitHash(commitHash);
      pullRequestPolicyEvaluationsDTO.setFeatureBranchPolicyEvaluation(policyEvaluation);
      return this;
    }
  }

  private class TestablePullRequestCommentCreatorBuilder
  {
    @Mock
    private GitClientFactory mockGitClientFactory;

    @Mock
    private SourceControlPullRequestCommentDAO mockPullRequestCommentDAO;

    @Mock
    private PullRequestFeedbackMarkupService mockFeedbackMarkupService;

    @Mock
    private PullRequestLineCommentingService mockLineCommentingService;

    @Mock
    private PullRequestLocationDiscoveryService mockLocationDiscoveryService;

    @Mock
    private DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService;

    @Mock
    private PullRequestCommentingEligibilityValidator mockPullRequestCommentingEligibilityValidator;

    @Mock
    private SourceControlComponentLoader mockComponentLoader;

    @Mock
    private ProductLicense mockProductLicense;

    @Mock
    private SourceControlEventLoggerFactory mockScmEventLoggerFactory;

    @Mock
    private ApplicationDAO mockApplicationDAO;

    @Mock
    private OrganizationDAO mockOrganizationDAO;

    @Mock
    private ScmOperationMetrics mockScmOperationMetrics;

    private final LocationDiscoveryResult locationDiscoveryResult = new LocationDiscoveryResult();

    private final Set<PullRequestPostCommentAction> postCommentActionList = new HashSet<>();

    private Optional<String> markup = Optional.of("default-markup");

    PullRequestCommentCreator build() throws IOException {
      MockitoAnnotations.openMocks(this);

      doReturn(locationDiscoveryResult).when(mockLocationDiscoveryService)
          .doLocationDiscovery(anyList(), any(), anyString(), anyString());

      doReturn(markup).when(mockFeedbackMarkupService)
          .createMarkup(any(), any(), any(), any(), anyInt(), any(), any(), any(), any(), anyBoolean(), any());

      doReturn(result).when(mockLineCommentingService)
          .createPullRequestLineComments(any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), any());

      doReturn(Optional.of(mock(CommentResponse.class))).when(mockCommentingClient)
          .createOrUpdateCommentInGitSCM(any(), any(), anyInt(), any(), any(), any());

      doReturn(mock(PullRequestCommentingLogger.class)).when(mockScmEventLoggerFactory)
          .newLogger(any(), any(), any(), any());

      doReturn(null).when(mockApplicationDAO).getById(anyString());
      doReturn(null).when(mockOrganizationDAO).getById(anyString());

      return new PullRequestCommentCreator(
          mockGitClientFactory,
          mockPullRequestCommentDAO,
          mockFeedbackMarkupService,
          mockCommentingClient,
          mockCommentingMetricsService,
          mockLineCommentingService,
          postCommentActionList,
          mockLocationDiscoveryService,
          developmentPrioritiesUtilsService,
          mockPullRequestCommentingEligibilityValidator,
          mockComponentLoader,
          mockProductLicense,
          telemetryUtils,
          mockScmEventLoggerFactory,
          mockApplicationDAO,
          mockOrganizationDAO,
          mockScmOperationMetrics
      );
    }

    TestablePullRequestCommentCreatorBuilder withoutMarkup() {
      markup = Optional.empty();
      return this;
    }

    TestablePullRequestCommentCreatorBuilder withMarkup(String markup) {
      this.markup = Optional.of(markup);
      return this;
    }

    private final PullRequestLineCommentCreationResult result = new PullRequestLineCommentCreationResult();

    TestablePullRequestCommentCreatorBuilder withLineComments(int lineCommentCount) {
      List<PullRequestLineCommentDTO> lineComments = result.getPullRequestLineCommentDtoList();
      for (int i = 0; i < lineCommentCount; i++) {
        lineComments.add(mock(PullRequestLineCommentDTO.class));
      }
      return this;
    }

    TestablePullRequestCommentCreatorBuilder withPostCommentAction(PullRequestPostCommentAction postCommentAction) {
      this.postCommentActionList.add(postCommentAction);
      return this;
    }
  }
}
