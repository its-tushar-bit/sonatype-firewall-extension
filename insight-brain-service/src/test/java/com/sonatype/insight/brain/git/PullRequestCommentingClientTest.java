/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.Optional;

import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
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
    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo("repoUrl", "username", "token", SourceControlProvider.GITLAB, "baseBranch", true, true);
    int pullRequestNumber = 1;
    String commentText = "Comment text";
    PullRequestCommentTelemetry prCommentTelemetry = new PullRequestCommentTelemetry("app1", 1);
    DefaultCommentResponse gitApiResponse = new DefaultCommentResponse();
    gitApiResponse.setId(20);
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
    assertThat(commentResponse.getId()).isEqualTo(gitApiResponse.getId());
    assertThat(commentResponse.getVersion()).isEqualTo(gitApiResponse.getVersion());
  }

  @Test
  public void createOrUpdateCommentInGitSCM_updateCommentWithoutVersion() throws IOException {
    // given: PR related data with comment created without versionç
    String applicationId = "app1";
    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo("repoUrl", "username", "token", SourceControlProvider.GITLAB, "baseBranch", true, true);
    int pullRequestNumber = 1;
    String commentText = "Comment text";
    PullRequestCommentTelemetry prCommentTelemetry = new PullRequestCommentTelemetry("app1", 1);
    Integer commentVersion = null;
    SourceControlPullRequestComment prComment =
        createSourceControlPullRequestComment(applicationId, pullRequestNumber, 1, commentVersion);
    DefaultCommentResponse gitApiResponse = new DefaultCommentResponse();
    gitApiResponse.setId(20);
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
    assertThat(commentResponse.getId()).isEqualTo(gitApiResponse.getId());
    assertThat(commentResponse.getVersion()).isEqualTo(gitApiResponse.getVersion());
  }

  @Test
  public void createOrUpdateCommentInGitSCM_updateCommentWithVersion() throws IOException {
    // given: PR related data with comment created with version
    String applicationId = "app1";
    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo("repoUrl", "username", "token", SourceControlProvider.GITLAB, "baseBranch", true, true);
    int pullRequestNumber = 1;
    String commentText = "Comment text";
    PullRequestCommentTelemetry prCommentTelemetry = new PullRequestCommentTelemetry("app1", 1);
    Integer commentVersion = 10;
    SourceControlPullRequestComment prComment =
        createSourceControlPullRequestComment(applicationId, pullRequestNumber, 20, commentVersion);
    DefaultCommentResponse gitApiResponse = new DefaultCommentResponse();
    gitApiResponse.setId(20);
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
    assertThat(commentResponse.getId()).isEqualTo(gitApiResponse.getId());
    assertThat(commentResponse.getVersion()).isEqualTo(gitApiResponse.getVersion());
  }

  @Test
  public void createOrUpdateCommentInGitSCM_updateCommentGeneratesHTTPNotFoundException() throws IOException {
    // given: PR related data with comment created with version
    String applicationId = "app1";
    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo("repoUrl", "username", "token", SourceControlProvider.GITLAB, "baseBranch", true, true);
    int pullRequestNumber = 1;
    String commentText = "Comment text";
    PullRequestCommentTelemetry prCommentTelemetry = new PullRequestCommentTelemetry("app1", 1);
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
    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo("repoUrl", "username", "token", SourceControlProvider.GITLAB, "baseBranch", true, true);
    int pullRequestNumber = 1;
    String commentText = "Comment text";
    PullRequestCommentTelemetry prCommentTelemetry = new PullRequestCommentTelemetry("app1", 1);
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
}
