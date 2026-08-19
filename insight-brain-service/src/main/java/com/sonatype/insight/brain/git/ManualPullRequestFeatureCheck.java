/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.git;

import java.io.UncheckedIOException;
import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;

/**
 * Checks if manual pull requests are supported for a given application and repository.
 */
@Named
@Singleton
public class ManualPullRequestFeatureCheck
    extends PullRequestFeatureCheck
{
  private final ScmRepoVisibilityService scmRepoVisibilityService;

  @Inject
  public ManualPullRequestFeatureCheck(
      final IqForScmLicenseChecker licenseChecker,
      final ScmRepoVisibilityService scmRepoVisibilityService)
  {
    super(licenseChecker);
    this.scmRepoVisibilityService = scmRepoVisibilityService;
  }

  /**
   * Determine if manual pull requests are supported for a given application and repository.
   *
   * @param gitRepoInfo the git repository information
   * @return the cause of the manual pull request feature not being supported, if any
   */
  public Optional<ManualPullRequestImpossibilityReason> isManualPullRequestFeatureSupported(
      final GitRepositoryInfo gitRepoInfo)
  {
    if (!isLicenseSupported()) {
      return Optional.of(ManualPullRequestImpossibilityReason.NOT_SUPPORTED_FOR_LICENSE);
    }

    if (gitRepoInfo == null || !isSCMConfigured(gitRepoInfo)) {
      return Optional.of(ManualPullRequestImpossibilityReason.SCM_NOT_CONFIGURED);
    }

    if (Boolean.FALSE.equals(gitRepoInfo.getManualPullRequestsEnabled())) {
      return Optional.of(ManualPullRequestImpossibilityReason.CONFIGURATION_DISABLED);
    }

    if (!isSourceProviderSupported(gitRepoInfo)) {
      return Optional.of(ManualPullRequestImpossibilityReason.NOT_SUPPORTED_FOR_PROVIDER);
    }

    if (!isRepositoryValidForPullRequestFeatures(gitRepoInfo)) {
      return Optional.of(ManualPullRequestImpossibilityReason.NOT_SUPPORTED_FOR_REPOSITORY);
    }

    return Optional.empty();
  }

  private boolean isRepositoryValidForPullRequestFeatures(GitRepositoryInfo gitRepoInfo) {
    try {
      return scmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(gitRepoInfo);
    }
    catch (UncheckedIOException e) {
      log.error("Unable to determine if pull request are possible for repo {}:", gitRepoInfo.repositoryUrl, e);
      return false;
    }
  }
}
