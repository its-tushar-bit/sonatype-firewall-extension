/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * Logic for if the automated pull request feature should run
 */
@Named
@Singleton
public class RemediationPullRequestFeatureCheck
    extends PullRequestFeatureCheck
{
  private final PullRequestRepositoryValidator pullRequestRepositoryValidator;

  @Inject
  public RemediationPullRequestFeatureCheck(
      final IqForScmLicenseChecker licenseChecker,
      final PullRequestRepositoryValidator pullRequestRepositoryValidator)
  {
    super(licenseChecker);
    this.pullRequestRepositoryValidator = pullRequestRepositoryValidator;
  }

  /**
   * Determine if the pull request feature is supported for this application and repository.
   *
   * @param app Application
   * @param gitRepoInfo GitRepositoryRepo from configurations
   * @return true/false if supported
   */
  public boolean isPullRequestFeatureSupported(
      final Application app,
      final GitRepositoryInfo gitRepoInfo,
      final boolean isInnerSourceComponent)
  {
    if (!isLicenseSupported()) {
      log.debug("Remediation pull request feature is not supported for this license");
      return false;
    }

    if (!isApplicationConfiguredForPR(gitRepoInfo, isInnerSourceComponent)) {
      log.debug("Pull requests have not been configured for application '{}'", app.getId());
      return false;
    }

    if (!isSourceProviderSupported(gitRepoInfo)) {
      log.debug("Source provider '{}' is not supported", gitRepoInfo.provider);
      return false;
    }

    if (!isRepositoryValidForPRs(gitRepoInfo)) {
      log.debug("Pull requests are not supported for application '{}' and repository '{}'",
          app.getId(), gitRepoInfo.normalizedRepositoryUrl);
      return false;
    }

    return true;
  }

  public boolean isApplicationConfiguredForPR(
      final GitRepositoryInfo gitRepositoryInfo,
      final boolean isInnerSourceComponent)
  {
    if (gitRepositoryInfo == null) {
      return false;
    }
    if (isInnerSourceComponent) {
      if (!isTrue(gitRepositoryInfo.innerSourceAutomatedUpdatesEnabled)) {
        log.debug("InnerSource Pull Requests have been explicitly disabled");
        return false;
      }
    }
    else {
      if (!isTrue(gitRepositoryInfo.remediationPullRequestsEnabled)) {
        log.debug("Pull Requests have been explicitly disabled");
        return false;
      }
    }

    return isSCMConfigured(gitRepositoryInfo);
  }

  public boolean isRepositoryValidForPRs(final GitRepositoryInfo gitRepoInfo) {
    return pullRequestRepositoryValidator.isRepoValidForPRs(gitRepoInfo);
  }
}
