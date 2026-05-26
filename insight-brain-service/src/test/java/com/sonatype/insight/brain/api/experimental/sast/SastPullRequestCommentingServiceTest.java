/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.api.experimental.sast;

import static com.sonatype.insight.brain.telemetry.SastPullRequestCommentTelemetry.SAST_PULL_REQUEST_COMMENT_TELEMETRY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.sast.SastPullRequestCommentDAO;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.git.PullRequestInfoClient;
import com.sonatype.insight.brain.git.ScmRepoVisibilityService;
import com.sonatype.insight.brain.git.SourceControlException;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sast.SastFinding;
import com.sonatype.insight.brain.model.sast.SastPullRequestComment;
import com.sonatype.insight.brain.model.sast.SastScan;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.SastPullRequestCommentTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.util.HashUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.CommentResponse;
import com.sonatype.nexus.scm.api.model.CommitInformation;
import com.sonatype.nexus.scm.api.model.DefaultCommentResponse;
import com.sonatype.nexus.scm.api.model.ProjectUrl;
import com.sonatype.nexus.scm.api.model.PullRequest;
import com.sonatype.nexus.scm.api.model.PullRequestImpl;
import com.sonatype.nexus.scm.api.model.PullRequestState;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

@Category(SlowTest.class)
public class SastPullRequestCommentingServiceTest
    extends AbstractComponentTest
{
  private static final String COMMIT_HASH = "test-commit-hash";

  private static final long COMMENT_ID = 123;

  @Inject
  private SastPullRequestCommentingService pullRequestCommentingService;

  @Inject
  private SastPullRequestCommentDAO pullRequestCommentDAO;

  @Mock
  private FeaturesService featuresService;

  @Mock
  private PullRequestInfoClient pullRequestInfoClient;

  @Mock
  private ScmRepoVisibilityService mockScmRepoVisibilityService;

  @Mock
  private GitClientFactory gitClientFactory;

  @Mock
  private ProjectUrl projectUrl;

  @Mock
  private GitApiClient gitApiClient;

  @Mock
  private TelemetrySender telemetrySender;

  private Application application;

  @Before
  public void before() {
    final Organization org = tempEntity.newOrganization();
    application = tempEntity.newApplication(org.getId());
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_CannotComment_WhenDeveloperFeatureNotInLicense() throws Exception {
    // Given: Developer is not included in the license
    setUpGitApiClient();
    setUpSourceControl(true);
    doReturn(Collections.singleton(LicensedFeature.DASHBOARD))
        .when(featuresService)
        .getFeatures();

    // Given: an open pull request
    final CommitInformation commitInfo = new CommitInformation();
    addPullRequest(PullRequestState.OPEN, commitInfo);
    addPullRequest(PullRequestState.CLOSED, commitInfo);
    doReturn(commitInfo)
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: at least 1 SAST finding
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    addSastFinding(sastScan, 1);

    // When: PR commenting is attempted
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: no PR comment is created
    final SastPullRequestComment pullRequestComment = pullRequestCommentDAO.getBySastScanId(sastScan.getId());
    assertThat(pullRequestComment).isNull();
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_CanComment_WhenPRsExist_AndOpenPRExists() throws Exception {
    // Given: pull request commenting is enabled, the Developer license feature is included, a base URL is configured,
    // and the repo is private or internal
    setUpGitApiClient();
    setUpSourceControl(true);
    includeDeveloperLicenseFeature();
    setRepoPrivate();
    setBaseUrl("http://localhost:1234");
    setCommentResponseForCreateComment();

    // Given: an open pull request
    final CommitInformation commitInfo = new CommitInformation();
    final PullRequest openPullRequest = addPullRequest(PullRequestState.OPEN, commitInfo);
    addPullRequest(PullRequestState.CLOSED, commitInfo);
    doReturn(commitInfo)
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: at least 1 SAST finding
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    addSastFinding(sastScan, 1);

    // When: PR commenting is attempted
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: a PR comment is created
    final SastPullRequestComment createdPullRequestComment = pullRequestCommentDAO.getBySastScanId(sastScan.getId());
    assertThat(createdPullRequestComment.getPullRequestUrl())
        .isEqualTo(openPullRequest.getUrl());
    assertThat(createdPullRequestComment.getPullRequestCommentVersion())
        .isZero();
    assertThat(createdPullRequestComment.getCreatedAt())
        .isNotNull();
    assertThat(createdPullRequestComment.getPullRequestCommentId())
        .isEqualTo(String.valueOf(COMMENT_ID));
    assertThat(createdPullRequestComment.getCommitHash())
        .isEqualTo(COMMIT_HASH);
    assertThat(createdPullRequestComment.getSastScanId())
        .isEqualTo(sastScan.getId());
    assertThat(createdPullRequestComment.getContentHash())
        .isNotNull();
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_CannotComment_WhenPRsExist_AndNoOpenPRExists() throws Exception {
    // Given: pull request commenting is enabled, and the Developer license feature is included
    setUpGitApiClient();
    setUpSourceControl(true);
    includeDeveloperLicenseFeature();

    // Given: no open PRs
    final CommitInformation commitInfo = new CommitInformation();
    addPullRequest(PullRequestState.CLOSED, commitInfo);
    addPullRequest(PullRequestState.CLOSED, commitInfo);
    doReturn(commitInfo)
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: at least 1 SAST finding
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    addSastFinding(sastScan, 1);

    // When: PR commenting is attempted
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: no PR comment is created
    final SastPullRequestComment pullRequestComment = pullRequestCommentDAO.getBySastScanId(sastScan.getId());
    assertThat(pullRequestComment).isNull();
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_CannotComment_WhenNoPRsExist() throws Exception {
    // Given: pull request commenting is enabled, and the Developer license feature is included
    setUpGitApiClient();
    setUpSourceControl(true);
    includeDeveloperLicenseFeature();

    // Given: no existing PRs
    doReturn(new CommitInformation())
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: at least 1 SAST finding
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    addSastFinding(sastScan, 1);

    // When: PR commenting is attempted
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: no PR comment is created
    final SastPullRequestComment pullRequestComment = pullRequestCommentDAO.getBySastScanId(sastScan.getId());
    assertThat(pullRequestComment).isNull();
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_CannotComment_WhenNoSastFindingsExist() throws Exception {
    // Given: pull request commenting is enabled, and the Developer license feature is included
    setUpGitApiClient();
    setUpSourceControl(true);
    includeDeveloperLicenseFeature();

    // Given: an open pull request
    final CommitInformation commitInfo = new CommitInformation();
    addPullRequest(PullRequestState.OPEN, commitInfo);
    addPullRequest(PullRequestState.CLOSED, commitInfo);
    doReturn(commitInfo)
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: 0 SAST findings
    final SastScan sastScan = tempEntity.newSastScan(application.getId());

    // When: PR commenting is attempted
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: no PR comment is created
    final SastPullRequestComment pullRequestComment = pullRequestCommentDAO.getBySastScanId(sastScan.getId());
    assertThat(pullRequestComment).isNull();
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_CannotComment_WhenPrCommentingIsDisabled() throws Exception {
    // Given: pull request commenting is NOT enabled, and the Developer license feature is included
    setUpGitApiClient();
    setUpSourceControl(false);
    includeDeveloperLicenseFeature();

    // Given: an open pull request
    final CommitInformation commitInfo = new CommitInformation();
    addPullRequest(PullRequestState.OPEN, commitInfo);
    addPullRequest(PullRequestState.CLOSED, commitInfo);
    doReturn(commitInfo)
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: at least 1 SAST finding
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    addSastFinding(sastScan, 1);

    // When: PR commenting is attempted
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: no PR comment is created
    final SastPullRequestComment pullRequestComment = pullRequestCommentDAO.getBySastScanId(sastScan.getId());
    assertThat(pullRequestComment).isNull();
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_CannotComment_WhenRepoIsNotPrivateOrInternal() throws Exception {
    // Given: pull request commenting is enabled, the Developer license feature is enabled, and the repo is NOT
    // private or internal
    setUpGitApiClient();
    setUpSourceControl(true);
    includeDeveloperLicenseFeature();

    // Given: an open pull request
    final CommitInformation commitInfo = new CommitInformation();
    addPullRequest(PullRequestState.OPEN, commitInfo);
    addPullRequest(PullRequestState.CLOSED, commitInfo);
    doReturn(commitInfo)
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: at least 1 SAST finding
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    addSastFinding(sastScan, 1);

    // When: PR commenting is attempted
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: no PR comment is created
    final SastPullRequestComment pullRequestComment = pullRequestCommentDAO.getBySastScanId(sastScan.getId());
    assertThat(pullRequestComment).isNull();
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_CannotComment_WhenNoBaseUrlIsConfigured() throws Exception {
    // Given: pull request commenting is enabled, the Developer license feature is included, the repo is private or
    // internal, and NO base URL is configured
    setUpGitApiClient();
    setUpSourceControl(true);
    includeDeveloperLicenseFeature();
    setRepoPrivate();
    setBaseUrl(null);

    // Given: an open pull request
    final CommitInformation commitInfo = new CommitInformation();
    addPullRequest(PullRequestState.OPEN, commitInfo);
    addPullRequest(PullRequestState.CLOSED, commitInfo);
    doReturn(commitInfo)
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: at least 1 SAST finding
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    addSastFinding(sastScan, 1);

    // When: PR commenting is attempted
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: no PR comment is created
    final SastPullRequestComment pullRequestComment = pullRequestCommentDAO.getBySastScanId(sastScan.getId());
    assertThat(pullRequestComment).isNull();
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_CannotComment_WhenFailingToFetchCommitInfo() throws Exception {
    // Given: pull request commenting is enabled
    setUpSourceControl(true);

    // Given: missing commit info
    doThrow(SourceControlException.class)
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: at least 1 SAST finding
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    addSastFinding(sastScan, 1);

    // When: PR commenting is attempted
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: no PR comment is created
    final SastPullRequestComment pullRequestComment = pullRequestCommentDAO.getBySastScanId(sastScan.getId());
    assertThat(pullRequestComment).isNull();
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_CreateComment_WhenNoPriorCommentForPrExists() throws Exception {
    // Given: pull request commenting is enabled, the Developer license feature is enabled, the repo is private or
    // internal, and a base URL is configured
    setUpGitApiClient();
    setUpSourceControl(true);
    includeDeveloperLicenseFeature();
    setRepoPrivate();
    setBaseUrl("http://localhost:1234");
    setCommentResponseForCreateComment();

    // Given: an open pull request
    final CommitInformation commitInfo = new CommitInformation();
    final PullRequest openPullRequest = addPullRequest(PullRequestState.OPEN, commitInfo);
    addPullRequest(PullRequestState.CLOSED, commitInfo);
    doReturn(commitInfo)
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: at least 1 SAST finding
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    addSastFinding(sastScan, 1);

    // Given: no preexisting comment
    assertThat(pullRequestCommentDAO.getAll())
        .isEmpty();

    // When: PR commenting is attempted
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: a new comment is created
    final SastPullRequestComment createdPullRequestComment = pullRequestCommentDAO.getBySastScanId(sastScan.getId());
    assertThat(createdPullRequestComment.getPullRequestUrl())
        .isEqualTo(openPullRequest.getUrl());
    assertThat(createdPullRequestComment.getPullRequestCommentVersion())
        .isZero();
    assertThat(createdPullRequestComment.getCreatedAt())
        .isNotNull();
    assertThat(createdPullRequestComment.getPullRequestCommentId())
        .isEqualTo(String.valueOf(COMMENT_ID));
    assertThat(createdPullRequestComment.getCommitHash())
        .isEqualTo(COMMIT_HASH);
    assertThat(createdPullRequestComment.getSastScanId())
        .isEqualTo(sastScan.getId());
    assertThat(createdPullRequestComment.getContentHash())
        .isNotNull();
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_UpdateComment_WhenPriorCommentForPrExists() throws Exception {
    // Given: pull request commenting is enabled, the Developer license feature is enabled, the repo is private or
    // internal, and a base URL is configured
    setUpGitApiClient();
    setUpSourceControl(true);
    includeDeveloperLicenseFeature();
    setRepoPrivate();
    setBaseUrl("http://localhost:1234");
    setCommentResponseForUpdateComment();

    // Given: an open pull request
    final CommitInformation commitInfo = new CommitInformation();
    final PullRequest openPullRequest = addPullRequest(PullRequestState.OPEN, commitInfo);
    addPullRequest(PullRequestState.CLOSED, commitInfo);
    doReturn(commitInfo)
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: at least 1 SAST finding
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    addSastFinding(sastScan, 1);

    // Given: a preexisting comment on the open PR
    final SastPullRequestComment preexistingPrComment = new SastPullRequestComment(sastScan.getId(),
        openPullRequest.getUrl(), COMMIT_HASH, "content-hash", String.valueOf(COMMENT_ID), 0);
    pullRequestCommentDAO.insert(preexistingPrComment);
    assertThat(pullRequestCommentDAO.getBySastScanId(sastScan.getId()))
        .isNotNull();

    // When: PR commenting is attempted
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: the preexisting PR comment is updated with the appropriate changes
    final SastPullRequestComment firstUpdatePullRequestComment =
        pullRequestCommentDAO.getBySastScanId(sastScan.getId());
    assertThat(firstUpdatePullRequestComment.getPullRequestUrl())
        .isEqualTo(preexistingPrComment.getPullRequestUrl());
    assertThat(firstUpdatePullRequestComment.getPullRequestCommentVersion())
        .isEqualTo(1);
    assertThat(firstUpdatePullRequestComment.getCreatedAt())
        .isEqualTo(preexistingPrComment.getCreatedAt());
    assertThat(firstUpdatePullRequestComment.getPullRequestCommentId())
        .isEqualTo(preexistingPrComment.getPullRequestCommentId());
    assertThat(firstUpdatePullRequestComment.getCommitHash())
        .isEqualTo(preexistingPrComment.getCommitHash());
    assertThat(firstUpdatePullRequestComment.getSastScanId())
        .isEqualTo(preexistingPrComment.getSastScanId());
    assertThat(firstUpdatePullRequestComment.getContentHash())
        .isNotNull();
    assertThat(firstUpdatePullRequestComment.getLastUpdatedAt())
        .isAfter(preexistingPrComment.getLastUpdatedAt());

    // And given: a new SAST finding and commit hash
    addSastFinding(sastScan, 3);
    final String differentCommitHash = "different-commit-hash";

    // When: PR commenting is attempted
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, differentCommitHash);

    // Then: the preexisting (already updated once) PR comment is updated with the appropriate changes
    final SastPullRequestComment secondUpdatePullRequestComment =
        pullRequestCommentDAO.getBySastScanId(sastScan.getId());
    assertThat(secondUpdatePullRequestComment.getPullRequestCommentVersion())
        .isEqualTo(2);
    assertThat(secondUpdatePullRequestComment.getContentHash())
        .isNotEqualTo(firstUpdatePullRequestComment.getContentHash());
    assertThat(secondUpdatePullRequestComment.getCommitHash())
        .isEqualTo(differentCommitHash);
    assertThat(secondUpdatePullRequestComment.getLastUpdatedAt())
        .isAfter(firstUpdatePullRequestComment.getLastUpdatedAt());
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_CommentTextReflectsWhenNoCriticalSastFindingsExist() throws Exception {
    // Given: pull request commenting is enabled, the Developer license feature is enabled, the repo is private or
    // internal, and a base URL is configured
    setUpGitApiClient();
    setUpSourceControl(true);
    includeDeveloperLicenseFeature();
    setRepoPrivate();
    setBaseUrl("http://localhost:1234");
    setCommentResponseForCreateComment();

    // Given: an open pull request
    final CommitInformation commitInfo = new CommitInformation();
    addPullRequest(PullRequestState.OPEN, commitInfo);
    addPullRequest(PullRequestState.CLOSED, commitInfo);
    doReturn(commitInfo)
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: 3 SAST findings, no critical severity
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    addSastFinding(sastScan, 1);
    addSastFinding(sastScan, 2);
    addSastFinding(sastScan, 3);

    // When: PR commenting is attempted
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: a new comment is created with text showing the total number of findings
    final String expectedContentPattern = "Sonatype found \\*\\*3 total\\*\\* issues active in this branch\\. " +
        "\\[Click here]\\(.+\\) to view the full SAST report\\.";
    final ArgumentCaptor<String> contentArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(gitApiClient).createPullRequestComment(anyInt(), contentArgumentCaptor.capture());
    final String actualContent = contentArgumentCaptor.getValue();
    assertThat(actualContent)
        .matches(expectedContentPattern);
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_CommentTextReflectsWhenCriticalSastFindingsExist() throws Exception {
    // Given: pull request commenting is enabled, the Developer license feature is enabled, the repo is private or
    // internal, and a base URL is configured
    setUpGitApiClient();
    setUpSourceControl(true);
    includeDeveloperLicenseFeature();
    setRepoPrivate();
    setBaseUrl("http://localhost:1234");
    setCommentResponseForCreateComment();

    // Given: an open pull request
    final CommitInformation commitInfo = new CommitInformation();
    addPullRequest(PullRequestState.OPEN, commitInfo);
    addPullRequest(PullRequestState.CLOSED, commitInfo);
    doReturn(commitInfo)
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: 4 SAST findings, 1 with critical severity
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    addSastFinding(sastScan, 1);
    addSastFinding(sastScan, 2);
    addSastFinding(sastScan, 3);
    addSastFinding(sastScan, 4);

    // When: PR commenting is attempted
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: a new comment is created with text showing the number of critical findings + total number of findings
    final String expectedContentPattern = "Sonatype found \\*\\*1 critical\\*\\* issue out of \\*\\*4 total\\*\\* " +
        "issues active in this branch\\. \\[Click here]\\(.+\\) to view the full SAST report\\.";
    final ArgumentCaptor<String> contentArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(gitApiClient).createPullRequestComment(anyInt(), contentArgumentCaptor.capture());
    final String actualContent = contentArgumentCaptor.getValue();
    assertThat(actualContent)
        .matches(expectedContentPattern);
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_CommentTextReflectsWhenNoSastFindingsExist_WhenUpdatingComment() throws Exception {
    // Given: pull request commenting is enabled, the Developer license feature is enabled, the repo is private or
    // internal, and a base URL is configured
    setUpGitApiClient();
    setUpSourceControl(true);
    includeDeveloperLicenseFeature();
    setRepoPrivate();
    setBaseUrl("http://localhost:1234");
    setCommentResponseForUpdateComment();

    // Given: an open pull request
    final CommitInformation commitInfo = new CommitInformation();
    final PullRequest openPullRequest = addPullRequest(PullRequestState.OPEN, commitInfo);
    addPullRequest(PullRequestState.CLOSED, commitInfo);
    doReturn(commitInfo)
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: no SAST findings
    final SastScan sastScan = tempEntity.newSastScan(application.getId());

    // Given: a preexisting comment on the open PR
    final SastPullRequestComment preexistingPrComment = new SastPullRequestComment(sastScan.getId(),
        openPullRequest.getUrl(), COMMIT_HASH, "content-hash", String.valueOf(COMMENT_ID), 0);
    pullRequestCommentDAO.insert(preexistingPrComment);
    assertThat(pullRequestCommentDAO.getBySastScanId(sastScan.getId()))
        .isNotNull();

    // When: PR commenting is attempted
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: a new comment is created with text showing that there were no findings
    final String expectedContentPattern = "Sonatype found \\*\\*0 issues\\*\\* active in this branch\\. " +
        "\\[Click here]\\(.+\\) to view the full SAST report\\.";
    final ArgumentCaptor<String> contentArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(gitApiClient).updatePullRequestComment(anyLong(), anyInt(), anyInt(), contentArgumentCaptor.capture());
    final String actualContent = contentArgumentCaptor.getValue();
    assertThat(actualContent)
        .matches(expectedContentPattern);
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_DoNotStoreCommentRecord_WhenCreateCommentFails() throws Exception {
    // Given: pull request commenting is enabled, the Developer license feature is enabled, the repo is private or
    // internal, and a base URL is configured
    setUpGitApiClient();
    setUpSourceControl(true);
    includeDeveloperLicenseFeature();
    setRepoPrivate();
    setBaseUrl("http://localhost:1234");

    // Given: an open pull request
    final CommitInformation commitInfo = new CommitInformation();
    addPullRequest(PullRequestState.OPEN, commitInfo);
    addPullRequest(PullRequestState.CLOSED, commitInfo);
    doReturn(commitInfo)
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: at least SAST finding
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    addSastFinding(sastScan, 1);

    // When: PR commenting is attempted and creating a new comment fails
    doThrow(IOException.class)
        .when(gitApiClient)
        .createPullRequestComment(anyInt(), anyString());
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: no comment record is stored in the database
    assertThat(pullRequestCommentDAO.getBySastScanId(sastScan.getId()))
        .isNull();
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_DoNotUpdateCommentRecord_WhenUpdateCommentFails() throws Exception {
    // Given: pull request commenting is enabled, the Developer license feature is enabled, the repo is private or
    // internal, and a base URL is configured
    setUpGitApiClient();
    setUpSourceControl(true);
    includeDeveloperLicenseFeature();
    setRepoPrivate();
    setBaseUrl("http://localhost:1234");

    // Given: an open pull request
    final CommitInformation commitInfo = new CommitInformation();
    final PullRequest openPullRequest = addPullRequest(PullRequestState.OPEN, commitInfo);
    addPullRequest(PullRequestState.CLOSED, commitInfo);
    doReturn(commitInfo)
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: at least 1 SAST finding
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    addSastFinding(sastScan, 1);

    // Given: a preexisting comment
    final int commentId = 123;
    final SastPullRequestComment preexistingPrComment = new SastPullRequestComment(sastScan.getId(),
        openPullRequest.getUrl(), COMMIT_HASH, "content-hash", String.valueOf(commentId), 1);
    pullRequestCommentDAO.insert(preexistingPrComment);
    assertThat(pullRequestCommentDAO.getBySastScanId(sastScan.getId()))
        .isNotNull();

    // When: PR commenting is attempted and updating the preexisting comment fails
    doThrow(IOException.class)
        .when(gitApiClient)
        .updatePullRequestComment(anyLong(), anyInt(), anyInt(), anyString());
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: the preexisting comment record is not updated
    final SastPullRequestComment prCommentAfterFailedUpdate = pullRequestCommentDAO.getBySastScanId(sastScan.getId());
    assertThat(prCommentAfterFailedUpdate)
        .usingRecursiveComparison()
        .isEqualTo(preexistingPrComment);
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_SendTelemetryAfterCommentCreated() throws Exception {
    // Given: pull request commenting is enabled, the Developer license feature is enabled, the repo is private or
    // internal, and a base URL is configured
    setUpGitApiClient();
    setUpSourceControl(true);
    includeDeveloperLicenseFeature();
    setRepoPrivate();
    setBaseUrl("http://localhost:1234");
    setCommentResponseForCreateComment();

    // Given: an open pull request
    final CommitInformation commitInfo = new CommitInformation();
    final PullRequest openPullRequest = addPullRequest(PullRequestState.OPEN, commitInfo);
    addPullRequest(PullRequestState.CLOSED, commitInfo);
    doReturn(commitInfo)
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: at least 1 SAST finding
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    addSastFinding(sastScan, 1);

    // When: PR commenting is attempted
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: a new comment is created
    final SastPullRequestComment createdPullRequestComment = pullRequestCommentDAO.getBySastScanId(sastScan.getId());
    assertThat(createdPullRequestComment)
        .isNotNull();

    // Then: telemetry data is sent
    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    final TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    assertThat(telemetryData.getPurpose())
        .isEqualTo(TelemetryPurpose.SAST_SOURCE_CONTROL_PULL_REQUEST_COMMENT);

    final SastPullRequestCommentTelemetry telemetry =
        (SastPullRequestCommentTelemetry) telemetryData.getAttributes().get(SAST_PULL_REQUEST_COMMENT_TELEMETRY);
    assertThat(telemetry.prNumber)
        .isEqualTo(openPullRequest.getNumber());
    assertThat(telemetry.commentId)
        .isEqualTo(Integer.parseInt(createdPullRequestComment.getPullRequestCommentId()));
    assertThat(telemetry.action)
        .isEqualTo(SastPullRequestCommentTelemetry.ACTION_CREATED);
    assertThat(telemetry.applicationId)
        .isEqualTo(HashUtils.hash(sastScan.getApplicationId(), HashUtils.SHA1));
    assertThat(telemetry.provider)
        .isEqualTo(SourceControlProvider.GITHUB.name());
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_SendTelemetryAfterCommentUpdated() throws Exception {
    // Given: pull request commenting is enabled, the Developer license feature is enabled, the repo is private or
    // internal, and a base URL is configured
    setUpGitApiClient();
    setUpSourceControl(true);
    includeDeveloperLicenseFeature();
    setRepoPrivate();
    setBaseUrl("http://localhost:1234");
    setCommentResponseForUpdateComment();

    // Given: an open pull request
    final CommitInformation commitInfo = new CommitInformation();
    final PullRequest openPullRequest = addPullRequest(PullRequestState.OPEN, commitInfo);
    addPullRequest(PullRequestState.CLOSED, commitInfo);
    doReturn(commitInfo)
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: at least 1 SAST finding
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    addSastFinding(sastScan, 1);

    // Given: a preexisting comment on the open PR
    final SastPullRequestComment preexistingPrComment = new SastPullRequestComment(sastScan.getId(),
        openPullRequest.getUrl(), COMMIT_HASH, "content-hash", String.valueOf(COMMENT_ID), 0);
    pullRequestCommentDAO.insert(preexistingPrComment);
    assertThat(pullRequestCommentDAO.getBySastScanId(sastScan.getId()))
        .isNotNull();

    // When: PR commenting is attempted
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: the preexisting comment is updated
    final SastPullRequestComment updatedPullRequestComment = pullRequestCommentDAO.getBySastScanId(sastScan.getId());
    assertThat(updatedPullRequestComment)
        .isNotNull();
    assertThat(updatedPullRequestComment.getPullRequestCommentVersion())
        .isEqualTo(1);

    // Then: telemetry data is sent
    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    final TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    assertThat(telemetryData.getPurpose())
        .isEqualTo(TelemetryPurpose.SAST_SOURCE_CONTROL_PULL_REQUEST_COMMENT);

    final SastPullRequestCommentTelemetry telemetry =
        (SastPullRequestCommentTelemetry) telemetryData.getAttributes().get(SAST_PULL_REQUEST_COMMENT_TELEMETRY);
    assertThat(telemetry.prNumber)
        .isEqualTo(openPullRequest.getNumber());
    assertThat(telemetry.commentId)
        .isEqualTo(Integer.parseInt(updatedPullRequestComment.getPullRequestCommentId()));
    assertThat(telemetry.action)
        .isEqualTo(SastPullRequestCommentTelemetry.ACTION_UPDATED);
    assertThat(telemetry.applicationId)
        .isEqualTo(HashUtils.hash(sastScan.getApplicationId(), HashUtils.SHA1));
    assertThat(telemetry.provider)
        .isEqualTo(SourceControlProvider.GITHUB.name());
  }

  @Test
  public void testCreateOrUpdateSastPullRequestComment_DoNotSendTelemetryIfCommentNotCreatedOrUpdated() throws Exception {
    // Given: pull request commenting is enabled, the Developer license feature is enabled, the repo is private or
    // internal, and a base URL is configured
    setUpGitApiClient();
    setUpSourceControl(true);
    includeDeveloperLicenseFeature();
    setRepoPrivate();
    setBaseUrl("http://localhost:1234");
    doThrow(IOException.class)
        .when(gitApiClient)
        .createPullRequestComment(anyInt(), anyString());

    // Given: an open pull request
    final CommitInformation commitInfo = new CommitInformation();
    final PullRequest openPullRequest = addPullRequest(PullRequestState.OPEN, commitInfo);
    addPullRequest(PullRequestState.CLOSED, commitInfo);
    doReturn(commitInfo)
        .when(pullRequestInfoClient)
        .getCommitInfoFromScm(any(), anyString());

    // Given: at least 1 SAST finding
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    addSastFinding(sastScan, 1);

    // When: PR commenting is attempted
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: telemetry data is not sent
    verify(telemetrySender, never()).send(any(TelemetryData.class));

    // And given: a preexisting comment on the open PR
    final SastPullRequestComment preexistingPrComment = new SastPullRequestComment(sastScan.getId(),
        openPullRequest.getUrl(), COMMIT_HASH, "content-hash", String.valueOf(COMMENT_ID), 0);
    pullRequestCommentDAO.insert(preexistingPrComment);
    assertThat(pullRequestCommentDAO.getBySastScanId(sastScan.getId()))
        .isNotNull();

    // When: PR commenting is attempted
    doThrow(IOException.class)
        .when(gitApiClient)
        .updatePullRequestComment(anyLong(), anyInt(), anyInt(), anyString());
    pullRequestCommentingService.createOrUpdateSastPullRequestComment(sastScan, COMMIT_HASH);

    // Then: telemetry data is not sent
    verify(telemetrySender, never()).send(any(TelemetryData.class));
  }

  private void setUpGitApiClient() {
    doReturn(gitApiClient)
        .when(gitClientFactory)
        .createApiClient(any());
  }

  private void setUpSourceControl(final boolean prCommentingEnabled) throws PlexusCipherException {
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    final SourceControl sourceControl = new SourceControl();
    sourceControl.setPullRequestCommentingEnabled(prCommentingEnabled);
    sourceControl.setOwnerId(application.getId());
    sourceControl.setRepositoryUrl("https://github.com/org/proj");
    sourceControl.setToken(new DefaultPlexusCipher().encrypt("root-token", "CMMDwoV"));
    tempEntity.newSourceControl(sourceControl);
  }

  private void setRepoPrivate() {
    doReturn(true).when(mockScmRepoVisibilityService).isRepositoryValidForPullRequestFeatures(any());
  }

  private void setCommentResponseForCreateComment() throws IOException {
    final CommentResponse commentResponse = new DefaultCommentResponse();
    commentResponse.setId(COMMENT_ID);
    doReturn(commentResponse)
        .when(gitApiClient)
        .createPullRequestComment(anyInt(), anyString());
  }

  private void setCommentResponseForUpdateComment() throws IOException {
    final CommentResponse commentResponse = new DefaultCommentResponse();
    commentResponse.setId(COMMENT_ID);
    doReturn(commentResponse)
        .when(gitApiClient)
        .updatePullRequestComment(anyLong(), anyInt(), anyInt(), anyString());
  }

  private void includeDeveloperLicenseFeature() {
    doReturn(Collections.singleton(LicensedFeature.DEVELOPER_DASHBOARD))
        .when(featuresService)
        .getFeatures();
  }

  private PullRequest addPullRequest(final PullRequestState pullRequestState, final CommitInformation commitInfo) {
    final PullRequest pullRequest = new PullRequestImpl();
    pullRequest.setState(pullRequestState);
    pullRequest.setUrl(pullRequestState.name());
    commitInfo.addPullRequest(pullRequest);
    return pullRequest;
  }

  private void addSastFinding(final SastScan sastScan, final int severityId) {
    final SastFinding sastFinding = new SastFinding();
    sastFinding.setSastScanId(sastScan.getId());
    sastFinding.setCwe("cwe");
    sastFinding.setConfidence(0);
    sastFinding.setSeverityId(severityId);
    sastFinding.setDescription("someDescription");
    sastFinding.setCoordinate("someCoordinate");
    sastFinding.setLineNumber(null);
    sastFinding.setRuleName("someRuleName");

    tempEntity.newSastFinding(sastFinding);
  }
}
