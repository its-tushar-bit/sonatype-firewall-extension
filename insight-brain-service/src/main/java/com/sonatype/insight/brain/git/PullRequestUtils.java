/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.git;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.scm.api.GitApiClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Util methods that are highly specific to the Automated Pull Request feature set
 */
@Named
@Singleton
class PullRequestUtils
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestUtils.class);

  static final String GITHUB_COM = "https://github.com";

  private final GitClientFactory gitClientFactory;

  @Inject
  PullRequestUtils(final GitClientFactory gitClientFactory) {
    this.gitClientFactory = gitClientFactory;
  }

  /**
   * Check if a pull request is allowed for this repository.
   *
   */
  boolean isPullRequestAllowed(final GitRepositoryInfo gitRepositoryInfo) throws IOException {
    if (!gitRepositoryInfo.enablePullRequests) {
      log.debug("Pull requests have not been enabled for repository URL '{}'", gitRepositoryInfo.repositoryUrl);
      return false;
    }

    return isEffectivelyPrivate(gitRepositoryInfo);
  }

  /**
   * As per legal's requirements, we will only create pull requests for Git repositories which are only
   * accessible to registered/licensed users.
   * This may be if the service is on the internet and the repository is private/restricted,
   * or if the repository is internal-only.
   */
  private boolean isEffectivelyPrivate(GitRepositoryInfo gitRepositoryInfo) throws IOException {
    switch (gitRepositoryInfo.provider) {
      case GITHUB:
        // GitHub Enterprise's license prohibits making it accessible to the Internet, so on-prem
        // means it is only accessible to licensed, internal users
        return isRepoOnPremises(gitRepositoryInfo) || isPrivateRepository(gitRepositoryInfo);
      default:
        throw new UnsupportedOperationException(
            String.format("'%s' not supported yet", gitRepositoryInfo.provider.name()));
    }
  }

  private boolean isRepoOnPremises(final GitRepositoryInfo gitRepositoryInfo) {
    switch (gitRepositoryInfo.provider) {
      case GITHUB:
        return !gitRepositoryInfo.repositoryUrl.startsWith(GITHUB_COM);
      default:
        throw new UnsupportedOperationException(
            String.format("'%s' not supported yet", gitRepositoryInfo.provider.name()));
    }
  }

  private boolean isPrivateRepository(final GitRepositoryInfo gitRepositoryInfo) throws IOException {
    GitApiClient client = gitClientFactory.createApiClient(gitRepositoryInfo);
    return client.isRepositoryPrivate();
  }
}
