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
import com.sonatype.insight.brain.git.dto.PullRequestLineCommentCreationResult;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.product.license.ProductLicense;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

  private PullRequestCommentingEligibilityValidator pullRequestCommentingEligibilityValidator;

  private final Map<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap = new HashMap<>();

  private final int pullRequestId = 1;

  private final String branch = "branch";

  private final String commitHash = "a1b2c3d4e";

  private final String applicationId = "appOne";

  private final String sourcePolicyEvaluationId = "pe1";

  private final String basePolicyEvaluationId = "pe2";

  private final String markupContent = "markup content";

  private final String featureBranchScanId = "myScanId";

  private final long scmId = 11;

  private LocationDiscoveryResult locationDiscoveryResult;

  private final ComponentIdentifier identifier1 =
      ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0");

  private final ComponentIdentifier identifier2 =
      ComponentIdentifier.createMavenCoordinates("group2", "artifact2", "2.0");

  private final DiffPosition diffPosition1 = new DiffPosition("path1", 1, 0, 1, "456", 1);

  private final DiffPosition diffPosition2 = new DiffPosition("path2", 2, 1, 2, "456", 2);

  public PullRequestLineCommentingServiceTest() {
    super(PullRequestLineCommentingService.class);
  }

  @Before
  @Override
  public void setup() {
    MockitoAnnotations.openMocks(this);
    super.setup();
    when(gitRepositoryInfo.getProvider()).thenReturn(SourceControlProvider.GITHUB);
    when(gitRepositoryInfo.getAuthenticationType()).thenReturn(null);  // Default to PAT
    when(gitRepositoryInfo.getOwnerId()).thenReturn(null);
    locationDiscoveryResult = new LocationDiscoveryResult();
    List<RankedSourceLocation> list = new LinkedList<>();
    list.add(new RankedSourceLocation("path", 1, "content", 1));
    locationDiscoveryResult.getLocationMap().put(identifier1, list);
  }

  @Test
  public void testCreatePullRequestLineComments_oneComment() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder()
        .withCommentVersion(85)
        .build();

    // when: try to create line comments
    PullRequestLineCommentCreationResult result = service.createPullRequestLineComments(getViolationList(1),
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult, featureBranchScanId);

    // then: one comment should be created
    List<PullRequestLineCommentDTO> lineComments = result.getPullRequestLineCommentDtoList();
    verify(mockGitClientFactory, atLeastOnce()).createApiClient(any());
    assertThat(lineComments).isNotEmpty();
    assertThat(lineComments.size()).isEqualTo(1);
    assertThat(lineComments.get(0).getScmId()).isEqualTo(scmId);
    assertThat(lineComments.get(0).getScmVersion()).isEqualTo(85);
    assertThat(lineComments.get(0).getMarkup()).isEqualTo(markupContent);

    // and: there were no exceptions recorded
    assertThat(result.hasExceptions()).isFalse();

    // and: the SourceControlPullRequestComment was saved to the db
    ArgumentCaptor<SourceControlPullRequestComment> sourceControlPullRequestCommentCaptor =
        ArgumentCaptor.forClass(SourceControlPullRequestComment.class);
    verify(mockPullRequestCommentDAO).insert(sourceControlPullRequestCommentCaptor.capture());
    SourceControlPullRequestComment sourceControlPullRequestComment = sourceControlPullRequestCommentCaptor.getValue();
    assertThat(sourceControlPullRequestComment.getApplicationId()).isEqualTo(applicationId);
    assertThat(sourceControlPullRequestComment.getComponentHash()).isEqualTo("hash1");
    assertThat(sourceControlPullRequestComment.getPathname()).isEqualTo("path1");
    assertThat(sourceControlPullRequestComment.getPullRequestId()).isEqualTo(pullRequestId);
    assertThat(sourceControlPullRequestComment.getPullRequestCommentId()).isEqualTo(scmId);
    assertThat(sourceControlPullRequestComment.getPullRequestCommentVersion()).isEqualTo(85);
    assertThat(sourceControlPullRequestComment.getSourcePolicyEvaluationId()).isEqualTo(sourcePolicyEvaluationId);
    assertThat(sourceControlPullRequestComment.getTargetPolicyEvaluationId()).isEqualTo(basePolicyEvaluationId);
  }

  @Test
  public void testCreatePullRequestLineComments_twoComments() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder()
        .withTwoComponentsFoundInCode()
        .withTwoComponentsFoundInPrDiff()
        .withCommentVersion(85)
        .build();

    // when: try to create line comments
    PullRequestLineCommentCreationResult result = service.createPullRequestLineComments(getViolationList(2),
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult, featureBranchScanId);

    // then: two comments should be created
    List<PullRequestLineCommentDTO> lineComments = result.getPullRequestLineCommentDtoList();
    verify(mockGitClientFactory, atLeastOnce()).createApiClient(any());
    assertThat(lineComments).isNotEmpty();
    assertThat(lineComments.size()).isEqualTo(2);

    // and: there were no exceptions recorded
    assertThat(result.hasExceptions()).isFalse();

    // and: the SourceControlPullRequestComments ware saved to the db
    ArgumentCaptor<SourceControlPullRequestComment> sourceControlPullRequestCommentCaptor =
        ArgumentCaptor.forClass(SourceControlPullRequestComment.class);
    verify(mockPullRequestCommentDAO, times(2)).insert(sourceControlPullRequestCommentCaptor.capture());
    SourceControlPullRequestComment sourceControlPullRequestComment1 =
        sourceControlPullRequestCommentCaptor.getAllValues().get(0);
    assertThat(sourceControlPullRequestComment1.getApplicationId()).isEqualTo(applicationId);
    assertThat(sourceControlPullRequestComment1.getComponentHash()).isEqualTo("hash1");
    assertThat(sourceControlPullRequestComment1.getPathname()).isEqualTo("path1");
    assertThat(sourceControlPullRequestComment1.getPullRequestId()).isEqualTo(pullRequestId);
    assertThat(sourceControlPullRequestComment1.getPullRequestCommentId()).isEqualTo(scmId);
    assertThat(sourceControlPullRequestComment1.getPullRequestCommentVersion()).isEqualTo(85);
    assertThat(sourceControlPullRequestComment1.getSourcePolicyEvaluationId()).isEqualTo(sourcePolicyEvaluationId);
    assertThat(sourceControlPullRequestComment1.getTargetPolicyEvaluationId()).isEqualTo(basePolicyEvaluationId);

    SourceControlPullRequestComment sourceControlPullRequestComment2 =
        sourceControlPullRequestCommentCaptor.getAllValues().get(1);
    assertThat(sourceControlPullRequestComment2.getApplicationId()).isEqualTo(applicationId);
    assertThat(sourceControlPullRequestComment2.getComponentHash()).isEqualTo("hash2");
    assertThat(sourceControlPullRequestComment2.getPathname()).isEqualTo("path2");
    assertThat(sourceControlPullRequestComment2.getPullRequestId()).isEqualTo(pullRequestId);
    assertThat(sourceControlPullRequestComment2.getPullRequestCommentId()).isEqualTo(scmId);
    assertThat(sourceControlPullRequestComment2.getPullRequestCommentVersion()).isEqualTo(85);
    assertThat(sourceControlPullRequestComment2.getSourcePolicyEvaluationId()).isEqualTo(sourcePolicyEvaluationId);
    assertThat(sourceControlPullRequestComment2.getTargetPolicyEvaluationId()).isEqualTo(basePolicyEvaluationId);
  }

  @Test
  public void testCreatePullRequestLineComments_twoCommentsOnlyOneFoundInCode() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder().build();

    // when: try to create line comments
    PullRequestLineCommentCreationResult result = service.createPullRequestLineComments(getViolationList(2),
        gitRepositoryInfo, remediationVersionMap, pullRequestId, branch, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult, featureBranchScanId);

    // then: one comment should be created
    List<PullRequestLineCommentDTO> lineComments = result.getPullRequestLineCommentDtoList();
    verify(mockGitClientFactory, atLeastOnce()).createApiClient(any());
    assertThat(lineComments).isNotEmpty();
    assertThat(lineComments.size()).isEqualTo(1);

    // and: there were no exceptions recorded
    assertThat(result.hasExceptions()).isFalse();
  }

  @Test
  public void testCreatePullRequestLineComments_twoCommentsOnlyOneFoundInDiff() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder()
        .withTwoComponentsFoundInCode()
        .build();

    // when: try to create line comments
    PullRequestLineCommentCreationResult result = service.createPullRequestLineComments(getViolationList(2),
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult, featureBranchScanId);

    // then: one comment should be created
    List<PullRequestLineCommentDTO> lineComments = result.getPullRequestLineCommentDtoList();
    verify(mockGitClientFactory, atLeastOnce()).createApiClient(any());
    assertThat(lineComments).isNotEmpty();
    assertThat(lineComments.size()).isEqualTo(1);

    // and: there were no exceptions recorded
    assertThat(result.hasExceptions()).isFalse();
  }

  @Test
  public void testCreatePullRequestLineComments_noLocationFound() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder()
        .withNoSourceLocationsAvailable()
        .build();
    locationDiscoveryResult = new LocationDiscoveryResult();

    // when: try to create line comments
    PullRequestLineCommentCreationResult result = service.createPullRequestLineComments(getViolationList(1),
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult, featureBranchScanId);

    // then: no comment should be created
    List<PullRequestLineCommentDTO> lineComments = result.getPullRequestLineCommentDtoList();
    verify(mockGitClientFactory, never()).createApiClient(any());
    assertThat(lineComments).isEmpty();

    // and: there were no exceptions recorded
    assertThat(result.hasExceptions()).isFalse();
  }

  @Test
  public void testCreatePullRequestLineComments_noPositionFound() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder()
        .withNoPositionsAvailable()
        .build();

    // when: try to create line comments
    PullRequestLineCommentCreationResult result = service.createPullRequestLineComments(getViolationList(1),
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult, featureBranchScanId);

    // then: no comment should be created
    List<PullRequestLineCommentDTO> lineComments = result.getPullRequestLineCommentDtoList();
    assertThat(lineComments).isEmpty();

    // and: there were no exceptions recorded
    assertThat(result.hasExceptions()).isFalse();
  }

  @Test
  public void testCreatePullRequestLineComments_emptyViolationList() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder().build();

    // when: try to create line comments
    PullRequestLineCommentCreationResult result = service.createPullRequestLineComments(null,
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult, featureBranchScanId);

    // then: one comment should be created
    verify(mockGitClientFactory, never()).createApiClient(any());
    List<PullRequestLineCommentDTO> lineComments = result.getPullRequestLineCommentDtoList();
    assertThat(lineComments).isEmpty();

    // and: there were no exceptions recorded
    assertThat(result.hasExceptions()).isFalse();
  }

  @Test
  public void testCreatePullRequestLineComments_featureFlagOff() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder()
        .withFeatureFlagDisabled()
        .build();

    // when: try to create line comments
    PullRequestLineCommentCreationResult result = service.createPullRequestLineComments(
        null, null, null, 1, null, null, null, null, null, null);

    // then: no comment should be created
    verify(mockGitClientFactory, never()).createApiClient(any());
    List<PullRequestLineCommentDTO> lineComments = result.getPullRequestLineCommentDtoList();
    assertThat(lineComments).isEmpty();

    // and: there were no exceptions recorded
    assertThat(result.hasExceptions()).isFalse();
  }

  @Test
  public void testCreatePullRequestLineComments_deleteNoExistingViolations() throws Exception {
    // given:
    PullRequestLineCommentingService service =
        new TestablePullRequestLineCommentingServiceBuilder().withExistingLineComments(0).build();

    // when: try to create line comments
    PullRequestLineCommentCreationResult result =
        service.createPullRequestLineComments(
            null,
            gitRepositoryInfo,
            remediationVersionMap,
            pullRequestId,
            commitHash,
            applicationId,
            sourcePolicyEvaluationId,
            basePolicyEvaluationId,
            locationDiscoveryResult,
            featureBranchScanId);

    // then: gitApiClient client should not be created, and delete should never be called on DAO
    verify(mockGitClientFactory, never()).createApiClient(any());
    verify(mockPullRequestCommentDAO, never()).delete(any());

    // and: there were no exceptions recorded
    assertThat(result.hasExceptions()).isFalse();
  }

  @Test
  public void testCreatePullRequestLineComments_deleteSomeExistingViolations() throws Exception {
    // given:
    PullRequestLineCommentingService service =
        new TestablePullRequestLineCommentingServiceBuilder().withExistingLineComments(5).build();

    // when: try to create line comments
    PullRequestLineCommentCreationResult result =
        service.createPullRequestLineComments(null,
            gitRepositoryInfo,
            remediationVersionMap,
            pullRequestId,
            commitHash,
            applicationId,
            sourcePolicyEvaluationId,
            basePolicyEvaluationId,
            locationDiscoveryResult,
            featureBranchScanId);

    // then: gitApiClient client should be created, and delete should be called on client and DAO for each
    verify(mockGitClientFactory).createApiClient(any());
    verify(mockGitApiClient, times(5)).deletePullRequestLineComment(anyLong(), anyInt(), anyInt());
    verify(mockPullRequestCommentDAO, times(5)).delete(any());

    // and: there were no exceptions recorded
    assertThat(result.hasExceptions()).isFalse();
  }

  @Test
  public void testCreatePullRequestLineComments_deleteSomeExistingViolationsWith404() throws Exception {
    // given:
    PullRequestLineCommentingService service =
        new TestablePullRequestLineCommentingServiceBuilder().withExistingLineComments(5).build();
    doThrow(new HttpResponseException(404, "Not Found")).when(mockGitApiClient).deletePullRequestLineComment(3L, 1, 1);

    // when: try to create line comments
    PullRequestLineCommentCreationResult result =
        service.createPullRequestLineComments(null,
            gitRepositoryInfo,
            remediationVersionMap,
            pullRequestId,
            commitHash,
            applicationId,
            sourcePolicyEvaluationId,
            basePolicyEvaluationId,
            locationDiscoveryResult,
            featureBranchScanId);

    // then: delete should be called on API client for each, dao for all that were deleted on api
    verify(mockGitClientFactory).createApiClient(any());
    verify(mockGitApiClient, times(5)).deletePullRequestLineComment(anyLong(), anyInt(), anyInt());
    verify(mockPullRequestCommentDAO, times(5)).delete(any());

    // and: there were no exceptions recorded
    assertThat(result.hasExceptions()).isFalse();
  }

  @Test
  public void testCreatePullRequestLineComments_deleteSomeExistingCommentsWithApiError() throws Exception {
    // given:
    PullRequestLineCommentingService service =
        new TestablePullRequestLineCommentingServiceBuilder().withExistingLineComments(2).build();
    doThrow(new HttpResponseException(400, "Bad Request")).when(mockGitApiClient)
        .deletePullRequestLineComment(anyLong(), anyInt(), anyInt());

    // when: try to create line comments
    PullRequestLineCommentCreationResult result =
        service.createPullRequestLineComments(null,
            gitRepositoryInfo,
            remediationVersionMap,
            pullRequestId,
            commitHash,
            applicationId,
            sourcePolicyEvaluationId,
            basePolicyEvaluationId,
            locationDiscoveryResult,
            featureBranchScanId);

    // then: processing of deletes should stop after exception
    verify(mockGitClientFactory).createApiClient(any());
    verify(mockGitApiClient, times(2)).deletePullRequestLineComment(anyLong(), anyInt(), anyInt());
    verify(mockPullRequestCommentDAO, times(2)).delete(any());

    // and: there were 2 exceptions recorded
    assertThat(result.hasExceptions()).isTrue();
    assertThat(result.getExceptionList().size()).isEqualTo(2);
  }

  @Test
  public void testCreatePullRequestLineComments_createCommentWithApiError() throws Exception {
    // given:
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder()
        .withCommentVersion(85)
        .build();
    doThrow(new HttpResponseException(400, "Bad Request")).when(mockGitApiClient)
        .createPullRequestLineComment(anyInt(), any(), any(), any());

    // when: try to create line comments
    PullRequestLineCommentCreationResult result = service.createPullRequestLineComments(getViolationList(1),
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult, featureBranchScanId);

    // then: one comment should be created
    verify(mockGitClientFactory, atLeastOnce()).createApiClient(any());
    assertThat(result.getPullRequestLineCommentDtoList()).isEmpty();

    // and: there was 1 exception recorded
    assertThat(result.hasExceptions()).isTrue();
    assertThat(result.getExceptionList().size()).isEqualTo(1);
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
    PullRequestLineCommentCreationResult result = service.createPullRequestLineComments(getViolationList(2),
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult, featureBranchScanId);

    // then: two comments should be created
    verify(mockGitClientFactory, atLeastOnce()).createApiClient(any());
    List<PullRequestLineCommentDTO> lineComments = result.getPullRequestLineCommentDtoList();
    assertThat(lineComments).isNotEmpty();
    assertThat(lineComments.size()).isEqualTo(2);

    // and: there were no exceptions recorded
    assertThat(result.hasExceptions()).isFalse();
  }

  @Test
  public void testCreatePullRequestLineComments_bitbucket() throws Exception {
    // given:
    when(gitRepositoryInfo.getProvider()).thenReturn(SourceControlProvider.BITBUCKET);
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder()
        .withTwoComponentsFoundInCode()
        .withTwoComponentsFoundInPrDiff()
        .build();
    CommentResponse response = new DefaultCommentResponse();
    response.setId(scmId);
    when(mockGitApiClient.createPullRequestLineComment(anyInt(), anyString(), anyString(), eq(diffPosition1)))
        .thenReturn(response);
    when(mockGitApiClient.createPullRequestLineComment(anyInt(), anyString(), anyString(), eq(diffPosition2)))
        .thenReturn(new DefaultCommentResponse());

    // when: try to create line comments
    PullRequestLineCommentCreationResult result = service.createPullRequestLineComments(getViolationList(2),
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult, featureBranchScanId);

    // then: only one comment should be created
    verify(mockGitClientFactory, atLeastOnce()).createApiClient(any());
    List<PullRequestLineCommentDTO> lineComments = result.getPullRequestLineCommentDtoList();
    assertThat(lineComments).isNotEmpty();
    assertThat(lineComments.size()).isEqualTo(1);

    // and: there were no exceptions recorded
    assertThat(result.hasExceptions()).isFalse();
  }

  @Test
  public void testCreatePullRequestLineComments_sameCommentTwice() throws Exception {
    // given:
    PositionDiscoveryResult positionDiscoveryResult = new PositionDiscoveryResult();
    List<DiffPosition> diffPositionlist = new ArrayList<>();
    diffPositionlist.add(new DiffPosition("path1", 1, 1, 1, "456", 1));
    diffPositionlist.add(new DiffPosition("path2", 2, 2, 2, "456", 2));
    positionDiscoveryResult.addDiffPositionsForComponent(identifier1, diffPositionlist);
    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder() //
        .withCommentVersion(85) //
        .withPositionDiscoveryResult(positionDiscoveryResult) //
        .build();
    locationDiscoveryResult = new LocationDiscoveryResult();
    List<RankedSourceLocation> rankedSourceLocationlist = new ArrayList<>();
    rankedSourceLocationlist.add(new RankedSourceLocation("path1", 1, "content", 1));
    rankedSourceLocationlist.add(new RankedSourceLocation("path2", 2, "content", 2));
    locationDiscoveryResult.getLocationMap().put(identifier1, rankedSourceLocationlist);

    // when: try to create line comments
    PullRequestLineCommentCreationResult result = service.createPullRequestLineComments(getViolationList(1),
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId, sourcePolicyEvaluationId,
        basePolicyEvaluationId, locationDiscoveryResult, featureBranchScanId);

    // then: two comments should be created
    List<PullRequestLineCommentDTO> lineComments = result.getPullRequestLineCommentDtoList();
    verify(mockGitClientFactory, atLeastOnce()).createApiClient(any());
    assertThat(lineComments).hasSize(2);
    assertThat(lineComments.get(0).getScmId()).isEqualTo(scmId);
    assertThat(lineComments.get(0).getScmVersion()).isEqualTo(85);
    assertThat(lineComments.get(0).getMarkup()).isEqualTo(markupContent);

    // and: there were no exceptions recorded
    assertThat(result.hasExceptions()).isFalse();

    // and: the SourceControlPullRequestComments ware saved to the db
    ArgumentCaptor<SourceControlPullRequestComment> sourceControlPullRequestCommentCaptor =
        ArgumentCaptor.forClass(SourceControlPullRequestComment.class);
    verify(mockPullRequestCommentDAO, times(2)).insert(sourceControlPullRequestCommentCaptor.capture());
    SourceControlPullRequestComment sourceControlPullRequestComment1 =
        sourceControlPullRequestCommentCaptor.getAllValues().get(0);
    assertThat(sourceControlPullRequestComment1.getApplicationId()).isEqualTo(applicationId);
    assertThat(sourceControlPullRequestComment1.getComponentHash()).isEqualTo("hash1");
    assertThat(sourceControlPullRequestComment1.getPathname()).isEqualTo("path1");
    assertThat(sourceControlPullRequestComment1.getPullRequestId()).isEqualTo(pullRequestId);
    assertThat(sourceControlPullRequestComment1.getPullRequestCommentId()).isEqualTo(scmId);
    assertThat(sourceControlPullRequestComment1.getPullRequestCommentVersion()).isEqualTo(85);
    assertThat(sourceControlPullRequestComment1.getSourcePolicyEvaluationId()).isEqualTo(sourcePolicyEvaluationId);
    assertThat(sourceControlPullRequestComment1.getTargetPolicyEvaluationId()).isEqualTo(basePolicyEvaluationId);

    SourceControlPullRequestComment sourceControlPullRequestComment2 =
        sourceControlPullRequestCommentCaptor.getAllValues().get(1);
    assertThat(sourceControlPullRequestComment2.getApplicationId()).isEqualTo(applicationId);
    assertThat(sourceControlPullRequestComment2.getComponentHash()).isEqualTo("hash1");
    assertThat(sourceControlPullRequestComment2.getPathname()).isEqualTo("path2");
    assertThat(sourceControlPullRequestComment2.getPullRequestId()).isEqualTo(pullRequestId);
    assertThat(sourceControlPullRequestComment2.getPullRequestCommentId()).isEqualTo(scmId);
    assertThat(sourceControlPullRequestComment2.getPullRequestCommentVersion()).isEqualTo(85);
    assertThat(sourceControlPullRequestComment2.getSourcePolicyEvaluationId()).isEqualTo(sourcePolicyEvaluationId);
    assertThat(sourceControlPullRequestComment2.getTargetPolicyEvaluationId()).isEqualTo(basePolicyEvaluationId);
  }

  @Test
  public void testCreatePullRequestLineComments_WithPATAuthentication() throws Exception {
    // given: PAT (Personal Access Token) authentication configured
    when(gitRepositoryInfo.getProvider()).thenReturn(SourceControlProvider.GITHUB);
    when(gitRepositoryInfo.getAuthenticationType()).thenReturn(null);  // PAT auth uses null
    when(gitRepositoryInfo.getOwnerId()).thenReturn(null);  // PAT auth uses null
    when(gitRepositoryInfo.getToken()).thenReturn("ghp_test_token_123");  // PAT uses token

    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder()
        .withCommentVersion(94)
        .build();

    // Setup ArgumentCaptor to capture GitRepositoryInfo
    ArgumentCaptor<GitRepositoryInfo> repoInfoCaptor = ArgumentCaptor.forClass(GitRepositoryInfo.class);

    // when: try to create line comments with PAT auth
    PullRequestLineCommentCreationResult result = service.createPullRequestLineComments(getViolationList(1),
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult, featureBranchScanId);

    // then: verify gitClientFactory was called with PAT authentication (null authenticationType)
    verify(mockGitClientFactory, atLeastOnce()).createApiClient(repoInfoCaptor.capture());

    GitRepositoryInfo captured = repoInfoCaptor.getValue();
    assertThat(captured.getAuthenticationType()).isNull();  // PAT auth uses null
    assertThat(captured.getOwnerId()).isNull();  // PAT auth uses null
    assertThat(captured.getToken()).isEqualTo("ghp_test_token_123");  // PAT auth uses token

    // and: one comment should be created
    List<PullRequestLineCommentDTO> lineComments = result.getPullRequestLineCommentDtoList();
    assertThat(lineComments).isNotEmpty();
    assertThat(lineComments.size()).isEqualTo(1);
    assertThat(lineComments.get(0).getScmId()).isEqualTo(scmId);
    assertThat(lineComments.get(0).getScmVersion()).isEqualTo(94);

    // and: there were no exceptions recorded
    assertThat(result.hasExceptions()).isFalse();
  }

  @Test
  public void testCreatePullRequestLineComments_WithGitHubAppAuthentication() throws Exception {
    // given: GitHub App authentication configured
    String ownerId = "app-789";
    when(gitRepositoryInfo.getProvider()).thenReturn(SourceControlProvider.GITHUB);
    when(gitRepositoryInfo.getAuthenticationType()).thenReturn(SourceControl.AuthenticationType.GITHUB_APP);
    when(gitRepositoryInfo.getOwnerId()).thenReturn(ownerId);

    PullRequestLineCommentingService service = new TestablePullRequestLineCommentingServiceBuilder()
        .withCommentVersion(95)
        .build();

    // Setup ArgumentCaptor to capture GitRepositoryInfo
    ArgumentCaptor<GitRepositoryInfo> repoInfoCaptor = ArgumentCaptor.forClass(GitRepositoryInfo.class);

    // when: try to create line comments with GitHub App auth
    PullRequestLineCommentCreationResult result = service.createPullRequestLineComments(getViolationList(1),
        gitRepositoryInfo, remediationVersionMap, pullRequestId, commitHash, applicationId,
        sourcePolicyEvaluationId, basePolicyEvaluationId, locationDiscoveryResult, featureBranchScanId);

    // then: verify gitClientFactory was called with correct GitHub App authentication
    verify(mockGitClientFactory, atLeastOnce()).createApiClient(repoInfoCaptor.capture());

    GitRepositoryInfo captured = repoInfoCaptor.getValue();
    assertThat(captured.getAuthenticationType()).isEqualTo(SourceControl.AuthenticationType.GITHUB_APP);
    assertThat(captured.getOwnerId()).isEqualTo(ownerId);

    // and: one comment should be created
    List<PullRequestLineCommentDTO> lineComments = result.getPullRequestLineCommentDtoList();
    assertThat(lineComments).isNotEmpty();
    assertThat(lineComments.size()).isEqualTo(1);
    assertThat(lineComments.get(0).getScmId()).isEqualTo(scmId);
    assertThat(lineComments.get(0).getScmVersion()).isEqualTo(95);

    // and: there were no exceptions recorded
    assertThat(result.hasExceptions()).isFalse();
  }

  private List<PolicyViolation> getViolationList(int itemCount) {
    List<PolicyViolation> violations = new LinkedList<>();
    PolicyViolation violation1 = new PolicyViolation();
    violation1.setComponentIdentifier(identifier1);
    violation1.setHash("hash1");
    violations.add(violation1);
    if (itemCount == 2) {
      PolicyViolation violation2 = new PolicyViolation();
      violation2.setComponentIdentifier(identifier2);
      violation2.setHash("hash2");
      violations.add(violation2);
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

    @Mock
    private ProductLicense mockProductLicense;

    private boolean featureFlagEnabled = true;

    private boolean sourceLocationsAvailable = true;

    private boolean positionsAvailable = true;

    private PositionDiscoveryResult positionDiscoveryResult;

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
          list.add(new RankedSourceLocation("path", 1, "content", 1));
          discoveryResult.getLocationMap().put(identifier1, list);
          if (componentsFoundInCode == 2) {
            list = new LinkedList<>();
            list.add(new RankedSourceLocation("path", 2, "content", 2));
            discoveryResult.getLocationMap().put(identifier2, list);
          }
          when(mockLocationDiscoveryService.doLocationDiscovery(any(), any(), anyString(), anyString()))
              .thenReturn(discoveryResult);
        }

        if (positionsAvailable) {
          if (positionDiscoveryResult == null) {
            positionDiscoveryResult = new PositionDiscoveryResult();
            List<DiffPosition> list = new LinkedList<>();
            list.add(diffPosition1);
            positionDiscoveryResult.addDiffPositionsForComponent(identifier1, list);
            if (componentsFoundInPrDiff == 2) {
              list = new LinkedList<>();
              // this diff position is for an unchanged line in the PR diff
              list.add(diffPosition2);
              positionDiscoveryResult.addDiffPositionsForComponent(identifier2, list);
            }
          }
          when(mockPositionDiscoveryExecutor.execute(anyMap(), anyInt(), any())).thenReturn(positionDiscoveryResult);
        }

        Optional<String> markup = Optional.of(markupContent);
        when(mockPullRequestFeedbackMarkupService.createLineMarkup(
                anyList(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                anyBoolean()
        ))
            .thenReturn(markup);

        CommentResponse response = new DefaultCommentResponse();
        response.setId(scmId);
        response.setVersion(commentVersion);
        when(mockGitApiClient.createPullRequestLineComment(anyInt(), anyString(), anyString(), any(DiffPosition.class)))
            .thenReturn(response);

        if (existingLineCommentsCount > 0) {
          List<SourceControlPullRequestComment> existingLineComments = new ArrayList<>(existingLineCommentsCount);
          for (int i = 0; i < existingLineCommentsCount; i++) {
            SourceControlPullRequestComment sourceControlPullRequestComment = new SourceControlPullRequestComment(
                applicationId, "componentHash" + i, "path" + i, pullRequestId, i, 1, "", "");
            existingLineComments.add(sourceControlPullRequestComment);
          }
          when(mockPullRequestCommentDAO.getByApplicationIdAndPullRequestIdWithComponents(applicationId, pullRequestId))
              .thenReturn(existingLineComments);
        }
      }

      SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.setEnabled(featureFlagEnabled);
      pullRequestCommentingEligibilityValidator = new PullRequestCommentingEligibilityValidator();

      return new PullRequestLineCommentingService(
          mockGitClientFactory,
          mockPullRequestCommentDAO,
          mockPullRequestFeedbackMarkupService,
          mockPositionDiscoveryExecutor,
          pullRequestCommentingEligibilityValidator,
          mockProductLicense
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

    TestablePullRequestLineCommentingServiceBuilder withPositionDiscoveryResult(
        PositionDiscoveryResult positionDiscoveryResult)
    {
      positionsAvailable = true;
      this.positionDiscoveryResult = positionDiscoveryResult;
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
  }
}
