/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.api.GitApiClientUtils;
import com.sonatype.nexus.scm.api.PullRequestInfoProvider;
import com.sonatype.nexus.scm.api.model.CommitInformation;
import com.sonatype.nexus.scm.api.model.ProjectUrl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class PullRequestInfoClient
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestInfoClient.class);

  static final int COMMIT_HISTORY_FETCH_COUNT = 12;

  static final int APPLICATION_PULL_REQUEST_FETCH_COUNT = 10;

  private final GitClientFactory gitClientFactory;

  @Inject
  public PullRequestInfoClient(GitClientFactory gitClientFactory) {
    this.gitClientFactory = gitClientFactory;
  }

  public CommitInformation getCommitInfoFromScm(GitRepositoryInfo gitRepositoryInfo, String commitHash) {
    CommitInformation result;

    GitApiClientUtils gitApiClientUtils = new GitApiClientFactory().getGitApiClientUtils(gitRepositoryInfo.provider);
    ProjectUrl projectUri = gitApiClientUtils.createProjectUrl(gitRepositoryInfo.normalizedRepositoryUrl);

    try {
      PullRequestInfoProvider client = gitClientFactory.createPullRequestInfoClient(gitRepositoryInfo);
      result = client.getCommitInformationForCommit(
          projectUri.getNamespace(),
          projectUri.getProject(),
          commitHash,
          gitRepositoryInfo.baseBranch,
          COMMIT_HISTORY_FETCH_COUNT,
          APPLICATION_PULL_REQUEST_FETCH_COUNT
      );
      log.debug("obtained CommitInfo from SCM for commit '{}' with {} pull request(s) and {} base branch commit(s)",
          commitHash, result.getPullRequests().size(), result.getCommits().size());
    }
    catch (IOException e) {
      String message = String.format("Failed to obtain CommitInfo from SCM for project %s, commit %s - reason: %s",
          projectUri, commitHash, e.getMessage());
      throw new SourceControlException(message, e);
    }

    return result;
  }
}
