/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.iq.location.discovery.PositionDiscoveryExecutor;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;
import com.sonatype.nexus.iq.location.dto.PositionDiscoveryResult;
import com.sonatype.nexus.iq.location.dto.RankedSourceLocation;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.DiffPosition;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.CommentResponse;
import com.sonatype.nexus.scm.api.model.DefaultCommentResponse;

import org.apache.http.client.HttpResponseException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

  @Mock
  private SourceControlPullRequestCommentDAO mockPullRequestCommentDAO;

  private final Map<ComponentIdentifier, String> remediationVersionMap = new HashMap<>();

  private final int pullRequestId = 1;

  private final String branch = "branch";

  private final String commitHash = "a1b2c3d4e";

  private final String applicationId = "appOne";

  private final String sourcePolicyEvaluationId = "pe1";

  private final String basePolicyEvaluationId = "pe2";

  private final String markupContent = "markup content";

  private final int scmId = 11;

  private LocationDiscoveryResult locationDiscoveryResult;

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
    MockitoAnnotations.openMocks(this);
    super.setup();
    when(gitRepositoryInfo.getProvider()).thenReturn(SourceControlProvider.GITHUB);
    locationDiscoveryResult = new LocationDiscoveryResult();
    List<RankedSourceLocation> list = new LinkedList<>();
    list.add(new RankedSourceLocation("path", 1, 1));
    locationDiscoveryResult.getLocationMap().put(identifier1, list);
  }

  @Test
  public void testCreatePullRequestLineComments_oneComment() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder()
        .withCommentVersion(85)
        .build();

    // when: try to create line comments
    List<PullRequestLineCommentDTO> lineComments = service.createPullRequestLineComments(getViolationList(1),
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult);

    // then: one comment should be created
    verify(mockGitClientFactory, atLeastOnce()).createApiClient(any());
    assertThat(lineComments).isNotEmpty();
    assertThat(lineComments.size()).isEqualTo(1);
    assertThat(lineComments.get(0).getScmId()).isEqualTo(scmId);
    assertThat(lineComments.get(0).getScmVersion()).isEqualTo(85);
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
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult);

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
        gitRepositoryInfo, remediationVersionMap, pullRequestId, branch, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult);

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
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult);

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
    locationDiscoveryResult = new LocationDiscoveryResult();

    // when: try to create line comments
    List<PullRequestLineCommentDTO> lineComments = service.createPullRequestLineComments(getViolationList(1),
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult);

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
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult);

    // then: no comment should be created
    assertThat(lineComments).isEmpty();
  }

  @Test
  public void testCreatePullRequestLineComments_emptyViolationList() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder().build();

    // when: try to create line comments
    List<PullRequestLineCommentDTO> lineComments = service.createPullRequestLineComments(null, 
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult);

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
        null, null, null, 1, null, null, null, null, null);

    // then: no comment should be created
    verify(mockGitClientFactory, never()).createApiClient(any());
    assertThat(lineComments).isEmpty();
  }

  @Test
  public void testCreatePullRequestLineComments_deleteNoExistingViolations() throws Exception {
    // given:
    PullRequestLineCommentingService service =
        new TestablePullRequestLineCommentingServiceBuilder().withExistingLineComments(0).build();

    // when: try to create line comments
    service.createPullRequestLineComments(null, gitRepositoryInfo, remediationVersionMap, pullRequestId, 
        commitHash, applicationId, sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult);

    // then: gitApiClient client should not be created, and delete should never be called on DAO
    verify(mockGitClientFactory, never()).createApiClient(any());
    verify(mockPullRequestCommentDAO, never()).delete(any());
  }

  @Test
  public void testCreatePullRequestLineComments_deleteSomeExistingViolations() throws Exception {
    // given:
    PullRequestLineCommentingService service =
        new TestablePullRequestLineCommentingServiceBuilder().withExistingLineComments(5).build();

    // when: try to create line comments
    service.createPullRequestLineComments(null, gitRepositoryInfo, remediationVersionMap, pullRequestId, 
        commitHash, applicationId, sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult);

    // then: gitApiClient client should be created, and delete should be called on client and DAO for each
    verify(mockGitClientFactory).createApiClient(any());
    verify(mockGitApiClient, times(5)).deletePullRequestLineComment(anyInt(), anyInt(), anyInt());
    verify(mockPullRequestCommentDAO, times(5)).delete(any());
  }

  @Test
  public void testCreatePullRequestLineComments_deleteSomeExistingViolationsWith404() throws Exception {
    // given:
    PullRequestLineCommentingService service =
        new TestablePullRequestLineCommentingServiceBuilder().withExistingLineComments(5).build();
    doThrow(new HttpResponseException(404, "Not Found")).when(mockGitApiClient).deletePullRequestLineComment(3, 1, 1);

    // when: try to create line comments
    service.createPullRequestLineComments(null, gitRepositoryInfo, remediationVersionMap, pullRequestId, 
        commitHash, applicationId, sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult);

    // then: delete should be called on API client for each, dao for all that were deleted on api
    verify(mockGitClientFactory).createApiClient(any());
    verify(mockGitApiClient, times(5)).deletePullRequestLineComment(anyInt(), anyInt(), anyInt());
    verify(mockPullRequestCommentDAO, times(4)).delete(any());
  }

  @Test
  public void testCreatePullRequestLineComments_deleteSomeExistingViolationsWithApiError() throws Exception {
    // given:
    PullRequestLineCommentingService service =
        new TestablePullRequestLineCommentingServiceBuilder().withExistingLineComments(5).build();
    doThrow(new HttpResponseException(400, "Bad Request")).when(mockGitApiClient)
        .deletePullRequestLineComment(anyInt(), anyInt(), anyInt());

    // when: try to create line comments
    service.createPullRequestLineComments(null, gitRepositoryInfo, remediationVersionMap, pullRequestId,
        commitHash, applicationId, sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult);

    // then: processing of deletes should stop after exception
    verify(mockGitClientFactory).createApiClient(any());
    verify(mockGitApiClient, times(1)).deletePullRequestLineComment(anyInt(), anyInt(), anyInt());
    verify(mockPullRequestCommentDAO, never()).delete(any());
  }

  @Test
  public void testCreatePullRequestLineComments_gitlab() throws Exception {
    // given:
    when(gitRepositoryInfo.getProvider()).thenReturn(SourceControlProvider.GITLAB);
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder()
        .withTwoComponentsFoundInCode()
        .withTwoComponentsFoundInPrDiff()
        .build();

    // when: try to create line comments
    List<PullRequestLineCommentDTO> lineComments = service.createPullRequestLineComments(getViolationList(2),
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult);

    // then: two comments should be created
    verify(mockGitClientFactory, atLeastOnce()).createApiClient(any());
    assertThat(lineComments).isNotEmpty();
    assertThat(lineComments.size()).isEqualTo(2);
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
    private PullRequestFeedbackMarkupService mockPullRequestFeedbackMarkupService;

    @Mock
    private PullRequestLocationDiscoveryService mockLocationDiscoveryService;

    @Mock
    private PositionDiscoveryExecutor mockPositionDiscoveryExecutor;

    private boolean featureFlagEnabled = true;

    private boolean sourceLocationsAvailable = true;

    private boolean positionsAvailable = true;

    private int componentsFoundInCode = 1;

    private int componentsFoundInPrDiff = 1;

    private int existingLineCommentsCount = 0;

    private Integer commentVersion = null;

    PullRequestLineCommentingService build() throws Exception {
      MockitoAnnotations.openMocks(this);

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
          when(mockLocationDiscoveryService.doLocationDiscovery(any(), any(), anyString(), anyString()))
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
        
        Optional<String> markup = Optional.of(markupContent);
        when(mockPullRequestFeedbackMarkupService.createLineMarkup(anyList(), any(), any(), any()))
            .thenReturn(markup);

        CommentResponse response = new DefaultCommentResponse();
        response.setId(scmId);
        response.setVersion(commentVersion);
        when(mockGitApiClient.createPullRequestLineComment(anyInt(), anyString(), anyString(), any(DiffPosition.class)))
            .thenReturn(response);

        if (existingLineCommentsCount > 0) {
          List<SourceControlPullRequestComment> existingLineComments = new ArrayList<>(existingLineCommentsCount);
          for (int i = 0; i < existingLineCommentsCount; i++) {
            SourceControlPullRequestComment sourceControlPullRequestComment =
                new SourceControlPullRequestComment(applicationId, "componentHash" + i, pullRequestId, i, 1, "", "");
            existingLineComments.add(sourceControlPullRequestComment);
          }
          when(mockPullRequestCommentDAO.getByApplicationIdAndPullRequestIdWithComponents(applicationId, pullRequestId))
              .thenReturn(existingLineComments);
        }
      }

      return new PullRequestLineCommentingService(
          mockGitClientFactory,
          mockPullRequestCommentDAO,
          mockPullRequestFeedbackMarkupService,
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

    TestablePullRequestLineCommentingServiceBuilder withExistingLineComments(int existingLineCommentsCount) {
      this.existingLineCommentsCount = existingLineCommentsCount;
      return this;
    }

    TestablePullRequestLineCommentingServiceBuilder withCommentVersion(Integer commentVersion) {
      this.commentVersion = commentVersion;
      return this;
    }

    private InsightConfig getInsightConfig(boolean enableFeatureFlag) {
      InsightConfig config = new InsightConfig();
      Map<String, Boolean> features = new HashMap<>();
      features.put(Feature.PR_LINE_COMMENTING.getFlag(), enableFeatureFlag);
      config.setFeatures(features);
      return config;
    }
  }
}
