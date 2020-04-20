/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.iq.location.discovery.PositionDiscoveryExecutor;
import com.sonatype.nexus.iq.location.dto.DiffPosition;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;
import com.sonatype.nexus.iq.location.dto.PositionDiscoveryResult;
import com.sonatype.nexus.iq.location.dto.RankedSourceLocation;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.CommentResponse;
import com.sonatype.nexus.scm.github.dto.GithubCommentResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PullRequestLineCommentingServiceTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private GitClientFactory mockGitClientFactory;

  @Mock
  private GitApiClient mockGitApiClient;

  @Mock
  private GitRepositoryInfo gitRepositoryInfo;

  private final int pullRequestId = 1;

  private final String branch = "branch";

  private final String commitHash = "a1b2c3d4e";

  private final String applicationId = "appOne";

  private final String sourcePolicyEvaluationId = "pe1";

  private final String basePolicyEvaluationId = "pe2";

  private final String markupContent = "markup content";

  private final int scmId = 11;

  private final ComponentIdentifier identifier1 =
      ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0");

  private final ComponentIdentifier identifier2 =
      ComponentIdentifier.createMavenCoordinates("group2", "artifact2", "2.0");

  public PullRequestLineCommentingServiceTest() {
    super(PullRequestLineCommentingService.class);
  }

  @Before
  @Override
  public void setup() {
    MockitoAnnotations.initMocks(this);
    super.setup();
  }

  @Test
  public void testCreatePullRequestLineComments_oneComment() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder().build();

    // when: try to create line comments
    List<PullRequestLineCommentDTO> lineComments = service.createPullRequestLineComments(getViolationList(1),
        gitRepositoryInfo, pullRequestId, branch, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId);

    // then: one comment should be created
    verify(mockGitClientFactory, atLeastOnce()).createApiClient(any());
    assertThat(lineComments).isNotEmpty();
    assertThat(lineComments.size()).isEqualTo(1);
    assertThat(lineComments.get(0).getScmId()).isEqualTo(scmId);
    assertThat(lineComments.get(0).getMarkup()).isEqualTo(markupContent);
  }

  @Test
  public void testCreatePullRequestLineComments_twoComments() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder()
        .withTwoComponentsFoundInCode()
        .withTwoComponentsFoundInPrDiff()
        .build();

    // when: try to create line comments
    List<PullRequestLineCommentDTO> lineComments = service.createPullRequestLineComments(getViolationList(2),
        gitRepositoryInfo, pullRequestId, branch, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId);

    // then: two comments should be created
    verify(mockGitClientFactory, atLeastOnce()).createApiClient(any());
    assertThat(lineComments).isNotEmpty();
    assertThat(lineComments.size()).isEqualTo(2);
  }

  @Test
  public void testCreatePullRequestLineComments_twoCommentsOnlyOneFoundInCode() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder().build();

    // when: try to create line comments
    List<PullRequestLineCommentDTO> lineComments = service.createPullRequestLineComments(getViolationList(2),
        gitRepositoryInfo, pullRequestId, branch, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId);

    // then: one comment should be created
    verify(mockGitClientFactory, atLeastOnce()).createApiClient(any());
    assertThat(lineComments).isNotEmpty();
    assertThat(lineComments.size()).isEqualTo(1);
  }

  @Test
  public void testCreatePullRequestLineComments_twoCommentsOnlyOneFoundInDiff() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder()
        .withTwoComponentsFoundInCode()
        .build();

    // when: try to create line comments
    List<PullRequestLineCommentDTO> lineComments = service.createPullRequestLineComments(getViolationList(2),
        gitRepositoryInfo, pullRequestId, branch, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId);

    // then: one comment should be created
    verify(mockGitClientFactory, atLeastOnce()).createApiClient(any());
    assertThat(lineComments).isNotEmpty();
    assertThat(lineComments.size()).isEqualTo(1);
  }

  @Test
  public void testCreatePullRequestLineComments_noLocationFound() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder()
        .withNoSourceLocationsAvailable()
        .build();

    // when: try to create line comments
    List<PullRequestLineCommentDTO> lineComments = service.createPullRequestLineComments(getViolationList(1),
        gitRepositoryInfo, pullRequestId, branch, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId);

    // then: no comment should be created
    verify(mockGitClientFactory, never()).createApiClient(any());
    assertThat(lineComments).isEmpty();
  }

  @Test
  public void testCreatePullRequestLineComments_noPositionFound() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder()
        .withNoPositionsAvailable()
        .build();

    // when: try to create line comments
    List<PullRequestLineCommentDTO> lineComments = service.createPullRequestLineComments(getViolationList(1),
        gitRepositoryInfo, pullRequestId, branch, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId);

    // then: no comment should be created
    assertThat(lineComments).isEmpty();
  }

  @Test
  public void testCreatePullRequestLineComments_emptyViolationList() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder().build();

    // when: try to create line comments
    List<PullRequestLineCommentDTO> lineComments = service.createPullRequestLineComments(null, gitRepositoryInfo,
        pullRequestId, branch, commitHash, applicationId,sourcePolicyEvaluationId, basePolicyEvaluationId);

    // then: one comment should be created
    verify(mockGitClientFactory, never()).createApiClient(any());
    assertThat(lineComments).isEmpty();
  }

  @Test
  public void testCreatePullRequestLineComments_featureFlagOff() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder()
        .withFeatureFlagDisabled()
        .build();

    // when: try to create line comments
    List<PullRequestLineCommentDTO> lineComments = service.createPullRequestLineComments(
        null, null, 1, null, null, null, null, null);

    // then: no comment should be created
    verify(mockGitClientFactory, never()).createApiClient(any());
    assertThat(lineComments).isEmpty();
  }

  private List<PolicyViolation> getViolationList(int itemCount) {
    List<PolicyViolation> violations = new LinkedList<>();
    PolicyViolation violation = new PolicyViolation();
    violation.setComponentIdentifier(identifier1);
    violation.setHash("hash");
    violations.add(violation);
    if (itemCount == 2) {
      violation.setComponentIdentifier(identifier2);
      violation.setHash("hash2");
      violations.add(violation);
    }
    return violations;
  }

  private class TestablePullRequestLineCommentingServiceBuilder
  {
    @Mock
    private SourceControlPullRequestCommentDAO mockPullRequestCommentDAO;

    @Mock
    private PullRequestFeedbackMarkupService mockPullRequestFeedbackMarkupService;

    @Mock
    private ApiComponentRemediationService mockApiComponentRemediationService;

    @Mock
    private SourceControlTaskRunner mockSourceControlTaskRunner;

    @Mock
    private PositionDiscoveryExecutor mockPositionDiscoveryExecutor;

    private boolean featureFlagEnabled = true;

    private boolean sourceLocationsAvailable = true;

    private boolean positionsAvailable = true;

    private int componentsFoundInCode = 1;

    private int componentsFoundInPrDiff = 1;

    PullRequestLineCommentingService build() throws Exception {
      MockitoAnnotations.initMocks(this);

      if (featureFlagEnabled) {
        when(mockGitClientFactory.createApiClient(gitRepositoryInfo)).thenReturn(mockGitApiClient);

        if (sourceLocationsAvailable) {
          LocationDiscoveryResult discoveryResult = new LocationDiscoveryResult();
          List<RankedSourceLocation> list = new LinkedList<>();
          list.add(new RankedSourceLocation("path", 1, 1));
          discoveryResult.getLocationMap().put(identifier1, list);
          if (componentsFoundInCode == 2) {
            list = new LinkedList<>();
            list.add(new RankedSourceLocation("path", 2, 2));
            discoveryResult.getLocationMap().put(identifier2, list);
          }
          when(mockSourceControlTaskRunner
              .doPullRequestLocationDiscovery(anyList(), any(), anyString(), anyString()))
              .thenReturn(discoveryResult);
        }

        if (positionsAvailable) {
          PositionDiscoveryResult positionDiscoveryResult = new PositionDiscoveryResult();
          List<DiffPosition> list = new LinkedList<>();
          list.add(new DiffPosition("path", 1, 1, 1));
          positionDiscoveryResult.addDiffPositionsForComponent(identifier1, list);
          if (componentsFoundInPrDiff == 2) {
            list = new LinkedList<>();
            list.add(new DiffPosition("path", 2, 2, 2));
            positionDiscoveryResult.addDiffPositionsForComponent(identifier2, list);
          }
          when(mockPositionDiscoveryExecutor.execute(anyMap(), anyInt(), any())).thenReturn(positionDiscoveryResult);
        }

        ApiComponentRemediationDTO remediationDTO = new ApiComponentRemediationDTO();
        when(mockApiComponentRemediationService
            .getSuggestedRemediationForComponentNoAuth(any(), any(), any(), any(), any(), any()))
            .thenReturn(remediationDTO);

        Optional<String> markup = Optional.of(markupContent);
        when(mockPullRequestFeedbackMarkupService.createLineMarkup(anyList(), any(), any()))
            .thenReturn(markup);

        CommentResponse response = new GithubCommentResponse();
        response.setId(scmId);
        when(mockGitApiClient.createPullRequestLineComment(anyInt(), anyString(), anyString(), anyString(), anyInt()))
            .thenReturn(response);
      }

      return new PullRequestLineCommentingService(
          mockGitClientFactory,
          mockPullRequestCommentDAO,
          mockPullRequestFeedbackMarkupService,
          mockApiComponentRemediationService,
          mockSourceControlTaskRunner,
          mockPositionDiscoveryExecutor,
          getInsightConfig(featureFlagEnabled)
      );
    }

    TestablePullRequestLineCommentingServiceBuilder withFeatureFlagDisabled() {
      this.featureFlagEnabled = false;
      return this;
    }

    TestablePullRequestLineCommentingServiceBuilder withNoSourceLocationsAvailable() {
      this.sourceLocationsAvailable = false;
      return this;
    }

    TestablePullRequestLineCommentingServiceBuilder withNoPositionsAvailable() {
      this.positionsAvailable = false;
      return this;
    }

    TestablePullRequestLineCommentingServiceBuilder withTwoComponentsFoundInCode() {
      this.componentsFoundInCode = 2;
      return this;
    }

    TestablePullRequestLineCommentingServiceBuilder withTwoComponentsFoundInPrDiff() {
      this.componentsFoundInPrDiff = 2;
      return this;
    }

    private InsightConfig getInsightConfig(boolean enableFeatureFlag) {
      InsightConfig config = new InsightConfig();
      Map<String, Boolean> expFeatures = new HashMap<>();
      expFeatures.put(PullRequestLineCommentingService.LINE_COMMENT_FEATURE, enableFeatureFlag);
      config.setExperimentalFeatures(expFeatures);
      return config;
    }
  }
}
