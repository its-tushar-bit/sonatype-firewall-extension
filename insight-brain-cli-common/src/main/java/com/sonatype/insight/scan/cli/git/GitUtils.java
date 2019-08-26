/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli.git;

import java.util.Optional;

import com.sonatype.nexus.git.utils.Environment.GitLabCI;
import com.sonatype.nexus.git.utils.commit.CommitHashFinderBuilder;
import com.sonatype.nexus.git.utils.repository.RepositoryUrlFinderBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitUtils
{
  private static final Logger log = LoggerFactory.getLogger(GitUtils.class);

  private GitUtils() {}

  public static Optional<String> tryGetCommitHash(String fallBackValue) {
    try {
      return new CommitHashFinderBuilder()
          .withEnvironmentVariableDefault()
          .withEnvironmentVariableNamed(GitLabCI.COMMIT_HASH_ENV_VARIABLE)
          .withGitRepo()
          .withFallBack(fallBackValue)
          .build()
          .tryGetCommitHash();
    }
    catch (Exception e) {
      log.error("Failed to get the commit hash due to: ", e);
    }
    return Optional.empty();
  }

  public static Optional<String> tryGetRepositoryUrl(String fallBackValue) {
    try {
      return new RepositoryUrlFinderBuilder()
          .withEnvironmentVariableDefault()
          .withEnvironmentVariableNamed(GitLabCI.REPOSITORY_URL_ENV_VARIABLE)
          .withGitRepo()
          .withFallBack(fallBackValue)
          .build()
          .tryGetRepositoryUrl();
    }
    catch (Exception e) {
      log.error("Failed to get the repository URL due to: ", e);
    }
    return Optional.empty();
  }
}
