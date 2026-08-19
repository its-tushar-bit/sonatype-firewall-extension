/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Util methods that are highly specific to the Automated Pull Request feature set
 */
@Named
@Singleton
public class PullRequestRepositoryValidator
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestRepositoryValidator.class);

  static final String GITHUB_COM = "https://github.com";

  private final ScmRepoVisibilityService scmRepoVisibilityService;

  @Inject
  PullRequestRepositoryValidator(final ScmRepoVisibilityService scmRepoVisibilityService) {
    this.scmRepoVisibilityService = scmRepoVisibilityService;
  }

  /**
   * Check if a pull request is allowed for this repository.
   *
   * As per legal's requirements, we will only create pull requests for Git repositories which are only accessible
   * to registered/licensed users. This may be if the service is on the internet and the repository is
   * private/restricted, or if the repository is internal-only
   */
  public boolean isRepoValidForPRs(final GitRepositoryInfo gitRepositoryInfo) {
    if (!gitRepositoryInfo.remediationPullRequestsEnabled && !gitRepositoryInfo.innerSourceAutomatedUpdatesEnabled) {
      log.debug("Pull requests have not been enabled for repository URL '{}'",
          gitRepositoryInfo.normalizedRepositoryUrl);
      return false;
    }
    if (!gitRepositoryInfo.provider.supportsPullRequests()) {
      throw new UnsupportedOperationException(
          String.format("'%s' not supported yet", gitRepositoryInfo.provider.name()));
    }

    return scmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(gitRepositoryInfo);
  }
}
