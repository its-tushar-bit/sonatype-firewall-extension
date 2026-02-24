/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.Optional;

import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.CommentResponse;
import com.sonatype.nexus.scm.api.model.DefaultCommentResponse;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry.ACTION_CREATED;
import static com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry.ACTION_UPDATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PullRequestCommentingClientTest
    extends VerifiableLoggingTestBase
{
  private static final String TEST_REPO_URL = "https://github.com/org/repo";

  private static final String TEST_SSH_URL = "git@github.com:org/repo.git";

  @Mock
  private GitClientFactory mockGitClientFactory;

  @Mock
  private GitApiClient mockGitClientApi;

  @InjectMocks
  private PullRequestCommentingClient pullRequestCommentingClient;

  public PullRequestCommentingClientTest() {
    super(PullRequestCommentingClient.class);
  }

  @Before
  @Override
  public void setup() {
    MockitoAnnotations.openMocks(this);
    setupGitClientFactory();
    super.setup();
  }

  @Test
  public void createOrUpdateCommentInGitSCM_prCommentCreated() throws IOException {
    // given: PR related data without comment (Default test data)
    String applicationId = "app1";
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo();
    int pullRequestNumber = 1;
    String commentText = "Comment text";
    PullRequestCommentTelemetry prCommentTelemetry = new PullRequestCommentTelemetry("app1", 1, "app1");
    DefaultCommentResponse gitApiResponse = new DefaultCommentResponse();
    gitApiResponse.setId(20L);
    gitApiResponse.setVersion(1);
    when(mockGitClientApi.createPullRequestComment(any(), any()))
        .thenReturn(gitApiResponse);

    // when: creating PR comment in git SCM
    CommentResponse commentResponse = pullRequestCommentingClient
        .createOrUpdateCommentInGitSCM(
            applicationId,
            gitRepositoryInfo,
            pullRequestNumber,
            commentText,
            null,
            prCommentTelemetry).get();

    // then: expecting PR comment created
    verify(mockGitClientFactory, times(1)).createApiClient(gitRepositoryInfo);
    verify(mockGitClientApi, times(1))
        .createPullRequestComment(pullRequestNumber, commentText);
    assertThatLogMessagesEqual(
        info("pull request comment '20' created for application 'app1' pull request '1'")
    );
    assertThat(prCommentTelemetry.action).isEqualTo(ACTION_CREATED);
    assertThat(prCommentTelemetry.commentId).isEqualTo(20);
    assertThat(prCommentTelemetry.provider).isEqualTo("gitlab");
    assertThat(commentResponse.getVersion()).isEqualTo(gitApiResponse.getVersion());
  }

  @Test
  public void createOrUpdateCommentInGitSCM_updateCommentWithoutVersion() throws IOException {
    // given: PR related data with comment created without version
    String applicationId = "app1";
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo();
    int pullRequestNumber = 1;
    String commentText = "Comment text";
    PullRequestCommentTelemetry prCommentTelemetry = new PullRequestCommentTelemetry("app1", 1, "app1");
    Integer commentVersion = null;
    SourceControlPullRequestComment prComment =
        createSourceControlPullRequestComment(applicationId, pullRequestNumber, 1, commentVersion);
    DefaultCommentResponse gitApiResponse = new DefaultCommentResponse();
    gitApiResponse.setId(20L);
    gitApiResponse.setVersion(null);
    when(mockGitClientApi.updatePullRequestComment(any(), any(), any(), any()))
        .thenReturn(gitApiResponse);

    // when: updating PR comment in git SCM
    CommentResponse commentResponse = pullRequestCommentingClient
        .createOrUpdateCommentInGitSCM(
            applicationId,
            gitRepositoryInfo,
            pullRequestNumber,
            commentText,
            prComment,
            prCommentTelemetry).get();

    // then: PR comment without version updated
    verify(mockGitClientFactory, times(1)).createApiClient(gitRepositoryInfo);
    verify(mockGitClientApi, times(1))
        .updatePullRequestComment(
            prComment.getPullRequestCommentId(),
            pullRequestNumber,
            prComment.getPullRequestCommentVersion(),
            commentText);
    assertThatLogMessagesEqual(
        info("pull request comment '20' updated for application 'app1' pull request '1'")
    );
    assertThat(prCommentTelemetry.action).isEqualTo(ACTION_UPDATED);
    assertThat(prCommentTelemetry.commentId).isEqualTo(20);
    assertThat(prCommentTelemetry.provider).isEqualTo("gitlab");
    assertThat(commentResponse.getId()).isEqualTo(gitApiResponse.getId());
    assertThat(commentResponse.getVersion()).isEqualTo(gitApiResponse.getVersion());
  }

  @Test
  public void createOrUpdateCommentInGitSCM_updateCommentWithVersion() throws IOException {
    // given: PR related data with comment created with version
    String applicationId = "app1";
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo();
    int pullRequestNumber = 1;
    String commentText = "Comment text";
    PullRequestCommentTelemetry prCommentTelemetry = new PullRequestCommentTelemetry("app1", 1, "app1");
    Integer commentVersion = 10;
    SourceControlPullRequestComment prComment =
        createSourceControlPullRequestComment(applicationId, pullRequestNumber, 20, commentVersion);
    DefaultCommentResponse gitApiResponse = new DefaultCommentResponse();
    gitApiResponse.setId(20L);
    gitApiResponse.setVersion(11);
    when(mockGitClientApi.updatePullRequestComment(any(), any(), any(), any()))
        .thenReturn(gitApiResponse);

    // when: updating PR comment in git SCM
    CommentResponse commentResponse = pullRequestCommentingClient
        .createOrUpdateCommentInGitSCM(
            applicationId,
            gitRepositoryInfo,
            pullRequestNumber,
            commentText,
            prComment,
            prCommentTelemetry).get();

    // then: expecting no attempt to create a comment
    verify(mockGitClientFactory, times(1)).createApiClient(gitRepositoryInfo);
    verify(mockGitClientApi, times(1))
        .updatePullRequestComment(
            prComment.getPullRequestCommentId(),
            pullRequestNumber,
            prComment.getPullRequestCommentVersion(),
            commentText);
    assertThatLogMessagesEqual(
        info("pull request comment '20' with version '11' updated for application 'app1' pull request '1'")
    );
    assertThat(prCommentTelemetry.action).isEqualTo(ACTION_UPDATED);
    assertThat(prCommentTelemetry.commentId).isEqualTo(20);
    assertThat(prCommentTelemetry.provider).isEqualTo("gitlab");
    assertThat(commentResponse.getId()).isEqualTo(gitApiResponse.getId());
    assertThat(commentResponse.getVersion()).isEqualTo(gitApiResponse.getVersion());
  }

  @Test
  public void createOrUpdateCommentInGitSCM_updateCommentGeneratesHTTPNotFoundException() throws IOException {
    // given: PR related data with comment created with version
    String applicationId = "app1";
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo();
    int pullRequestNumber = 1;
    String commentText = "Comment text";
    PullRequestCommentTelemetry prCommentTelemetry = new PullRequestCommentTelemetry("app1", 1, "app1");
    Integer commentVersion = 10;
    SourceControlPullRequestComment prComment =
        createSourceControlPullRequestComment(applicationId, pullRequestNumber, 20, commentVersion);
    HttpResponseException e = new HttpResponseException(HttpStatus.SC_NOT_FOUND, "");
    when(mockGitClientApi.updatePullRequestComment(any(), any(), any(), any()))
        .thenThrow(e);

    // when: git SCM api HTTP 404 code response
    Optional<CommentResponse> commentResponse = pullRequestCommentingClient
        .createOrUpdateCommentInGitSCM(
            applicationId,
            gitRepositoryInfo,
            pullRequestNumber,
            commentText,
            prComment,
            prCommentTelemetry);

    // then: expecting no attempt to create a comment
    verify(mockGitClientFactory, times(1)).createApiClient(gitRepositoryInfo);
    verify(mockGitClientApi, times(1))
        .updatePullRequestComment(
            prComment.getPullRequestCommentId(),
            pullRequestNumber,
            prComment.getPullRequestCommentVersion(),
            commentText);
    assertThatLogMessagesEqual(
        warn("Updating pull request comment '20' for application 'app1' pull request '1' returned 404 NOT FOUND")
    );
    assertThat(prCommentTelemetry.action).isEqualTo(ACTION_UPDATED);
    assertThat(prCommentTelemetry.commentId).isZero();
    assertThat(commentResponse).isNotPresent();
  }

  @Test
  public void createOrUpdateCommentInGitSCM_updateCommentGeneratesHTTPException() throws IOException {
    // given: PR related data with comment created with version
    String applicationId = "app1";
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo();
    int pullRequestNumber = 1;
    String commentText = "Comment text";
    PullRequestCommentTelemetry prCommentTelemetry = new PullRequestCommentTelemetry("app1", 1, "app1");
    Integer commentVersion = 10;
    SourceControlPullRequestComment prComment =
        createSourceControlPullRequestComment(applicationId, pullRequestNumber, 20, commentVersion);
    HttpResponseException e = new HttpResponseException(HttpStatus.SC_MULTI_STATUS, "");
    when(mockGitClientApi.updatePullRequestComment(any(), any(), any(), any()))
        .thenThrow(e);

    // when: HTTP exception when updating (Different than HTTP 404)
    // then: Exception thrown from the updating method
    assertThatExceptionOfType(HttpResponseException.class).isThrownBy(() -> pullRequestCommentingClient
        .createOrUpdateCommentInGitSCM(
            applicationId,
            gitRepositoryInfo,
            pullRequestNumber,
            commentText,
            prComment,
            prCommentTelemetry)).withMessageContaining("");
    verify(mockGitClientFactory, times(1)).createApiClient(gitRepositoryInfo);
    verify(mockGitClientApi, times(1))
        .updatePullRequestComment(
            prComment.getPullRequestCommentId(),
            pullRequestNumber,
            prComment.getPullRequestCommentVersion(),
            commentText);
  }

  private SourceControlPullRequestComment createSourceControlPullRequestComment(
      String applicationId,
      int pullRequestNumber,
      Integer prCommentId,
      Integer prCommentVersion)
  {
    PolicyEvaluation sourcePolicyEvaluation = new PolicyEvaluation();
    sourcePolicyEvaluation.setId("sourcePE");
    PolicyEvaluation targetPolicyEvaluation = new PolicyEvaluation();
    sourcePolicyEvaluation.setId("targetPE");

    return new SourceControlPullRequestComment(
        applicationId,
        pullRequestNumber,
        prCommentId,
        prCommentVersion,
        "contentHash",
        sourcePolicyEvaluation.getId(),
        targetPolicyEvaluation.getId()
    );
  }

  private void setupGitClientFactory() {
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mockGitClientApi);
  }

  private GitRepositoryInfo getGitRepositoryInfo() {
    return new GitRepositoryInfo("repoUrl", "sshRepoUrl", "username", "token", SourceControlProvider.GITLAB,
        "baseBranch", true, true, true,true, true, true, false, null);
  }

  @Test
  public void testCreateOrUpdateComment_WithPATAuthentication() throws IOException {
    // given: PR related data with PAT (Personal Access Token) authentication
    String applicationId = "app-pat";
    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo(
        TEST_REPO_URL,
        TEST_REPO_URL,  // normalizedRepositoryUrl
        TEST_SSH_URL,
        "username",
        "token123",  // PAT token
        SourceControlProvider.GITHUB,
        "main",
        true, true, true, true, true, true, false,
        null,  // sourceControlScanTarget
        null,  // PAT auth uses null authenticationType
        null); // PAT auth uses null ownerId
    int pullRequestNumber = 1;
    String commentText = "Comment text with PAT";
    PullRequestCommentTelemetry prCommentTelemetry = new PullRequestCommentTelemetry("app1", 1, "app1");
    DefaultCommentResponse gitApiResponse = new DefaultCommentResponse();
    gitApiResponse.setId(29L);
    gitApiResponse.setVersion(1);
    when(mockGitClientApi.createPullRequestComment(any(), any()))
        .thenReturn(gitApiResponse);

    // Setup ArgumentCaptor to capture GitRepositoryInfo
    ArgumentCaptor<GitRepositoryInfo> repoInfoCaptor = ArgumentCaptor.forClass(GitRepositoryInfo.class);

    // when: creating PR comment in git SCM with PAT auth
    CommentResponse commentResponse = pullRequestCommentingClient
        .createOrUpdateCommentInGitSCM(
            applicationId,
            gitRepositoryInfo,
            pullRequestNumber,
            commentText,
            null,
            prCommentTelemetry).get();

    // then: verify gitClientFactory was called with PAT authentication (null authenticationType)
    verify(mockGitClientFactory, times(1)).createApiClient(repoInfoCaptor.capture());

    GitRepositoryInfo captured = repoInfoCaptor.getValue();
    assertThat(captured.getAuthenticationType()).isNull();  // PAT auth uses null
    assertThat(captured.getOwnerId()).isNull();  // PAT auth uses null
    assertThat(captured.token).isEqualTo("token123");  // PAT auth uses token field

    // and: PR comment was created
    verify(mockGitClientApi, times(1)).createPullRequestComment(pullRequestNumber, commentText);
    assertThat(commentResponse.getId()).isEqualTo(29L);
  }

  @Test
  public void testCreateOrUpdateComment_WithGitHubAppAuthentication() throws IOException {
    // given: PR related data with GitHub App authentication
    String applicationId = "app-githubapp";
    String ownerId = "app-123";
    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo(
        TEST_REPO_URL,
        TEST_REPO_URL,  // normalizedRepositoryUrl
        TEST_SSH_URL,
        null,  // no username for GitHub App
        null,  // no token for GitHub App
        SourceControlProvider.GITHUB,
        "main",
        true, true, true, true, true, true, false,
        null,  // sourceControlScanTarget
        SourceControl.AuthenticationType.GITHUB_APP,
        ownerId);
    int pullRequestNumber = 1;
    String commentText = "Comment text with GitHub App";
    PullRequestCommentTelemetry prCommentTelemetry = new PullRequestCommentTelemetry("app1", 1, "app1");
    DefaultCommentResponse gitApiResponse = new DefaultCommentResponse();
    gitApiResponse.setId(30L);
    gitApiResponse.setVersion(1);
    when(mockGitClientApi.createPullRequestComment(any(), any()))
        .thenReturn(gitApiResponse);

    // Setup ArgumentCaptor to capture GitRepositoryInfo
    ArgumentCaptor<GitRepositoryInfo> repoInfoCaptor = ArgumentCaptor.forClass(GitRepositoryInfo.class);

    // when: creating PR comment in git SCM with GitHub App auth
    CommentResponse commentResponse = pullRequestCommentingClient
        .createOrUpdateCommentInGitSCM(
            applicationId,
            gitRepositoryInfo,
            pullRequestNumber,
            commentText,
            null,
            prCommentTelemetry).get();

    // then: verify gitClientFactory was called with correct GitHub App authentication
    verify(mockGitClientFactory, times(1)).createApiClient(repoInfoCaptor.capture());

    GitRepositoryInfo captured = repoInfoCaptor.getValue();
    assertThat(captured.getAuthenticationType()).isEqualTo(SourceControl.AuthenticationType.GITHUB_APP);
    assertThat(captured.getOwnerId()).isEqualTo(ownerId);
    assertThat(captured.token).isNull();

    // and: PR comment was created
    verify(mockGitClientApi, times(1)).createPullRequestComment(pullRequestNumber, commentText);
    assertThat(commentResponse.getId()).isEqualTo(30L);
  }
}
