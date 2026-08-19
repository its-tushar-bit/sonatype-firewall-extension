/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.CommentResponse;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry.ACTION_CREATED;
import static com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry.ACTION_UPDATED;

@Named
@Singleton
public class PullRequestCommentingClient
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestCommentingClient.class);

  private final GitClientFactory gitClientFactory;

  @Inject
  public PullRequestCommentingClient(GitClientFactory gitClientFactory) {
    this.gitClientFactory = gitClientFactory;
  }

  /**
   * creates or updates the pull request comment in GitHub for the given repo and pull request
   */
  public Optional<CommentResponse> createOrUpdateCommentInGitSCM(
      String applicationId,
      GitRepositoryInfo gitRepositoryInfo,
      int pullRequestNumber,
      String commentText,
      SourceControlPullRequestComment existingPullRequestComment,
      PullRequestCommentTelemetry telemetry) throws IOException
  {
    Optional<CommentResponse> response;

    GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepositoryInfo);
    telemetry.provider = getProvider(gitRepositoryInfo);
    if (existingPullRequestComment == null) {
      CommentResponse commentResponse = gitApiClient.createPullRequestComment(pullRequestNumber, commentText);
      log.info("pull request comment '{}' created for application '{}' pull request '{}'",
          commentResponse.getId(), applicationId, pullRequestNumber);
      telemetry.action = ACTION_CREATED;
      response = Optional.of(commentResponse);
    }
    else {
      response = updateCommentInGitSCM(applicationId, pullRequestNumber, commentText, existingPullRequestComment,
          gitApiClient);
      telemetry.action = ACTION_UPDATED;
    }
    response.ifPresent(commentResponse -> telemetry.commentId = commentResponse.getId());
    return response;
  }

  private String getProvider(GitRepositoryInfo gitRepositoryInfo) {
    return null != gitRepositoryInfo.getProvider() ? gitRepositoryInfo.getProvider().toString() : "not specified";
  }

  private Optional<CommentResponse> updateCommentInGitSCM(
      final String applicationId,
      final int pullRequestNumber,
      final String commentText,
      final SourceControlPullRequestComment existingPullRequestComment,
      final GitApiClient gitApiClient) throws IOException
  {
    CommentResponse commentResponse = null;
    try {
      commentResponse = gitApiClient
          .updatePullRequestComment(existingPullRequestComment.getPullRequestCommentId(), pullRequestNumber,
              existingPullRequestComment.getPullRequestCommentVersion(), commentText);
      if (commentResponse.getVersion() == null) {
        log.info("pull request comment '{}' updated for application '{}' pull request '{}'",
            commentResponse.getId(), applicationId, pullRequestNumber);
      }
      else {
        log.info("pull request comment '{}' with version '{}' updated for application '{}' pull request '{}'",
            commentResponse.getId(), commentResponse.getVersion(), applicationId, pullRequestNumber);
      }
    }
    catch (HttpResponseException e) {
      if (HttpStatus.SC_NOT_FOUND == e.getStatusCode()) {
        log.warn("Updating pull request comment '{}' for application '{}' pull request '{}' returned 404 NOT FOUND",
            existingPullRequestComment.getPullRequestCommentId(), applicationId, pullRequestNumber);
      }
      else {
        throw e;
      }
    }
    return Optional.ofNullable(commentResponse);
  }
}
