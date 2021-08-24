/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.git;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Logic for if the automated pull request feature should run
 */
@Named
@Singleton
public class RemediationPullRequestFeatureCheck
{
  private static final Logger log = LoggerFactory.getLogger(RemediationPullRequestFeatureCheck.class);

  private final IqForScmLicenseChecker licenseChecker;

  private final PullRequestRepositoryValidator pullRequestRepositoryValidator;

  @Inject
  public RemediationPullRequestFeatureCheck(
      final IqForScmLicenseChecker licenseChecker,
      final PullRequestRepositoryValidator pullRequestRepositoryValidator)
  {
    this.licenseChecker = licenseChecker;
    this.pullRequestRepositoryValidator = pullRequestRepositoryValidator;
  }

  /**
   * determine if the pull request feature is supported for this application
   * and repository.
   *
   * @param app         Application
   * @param gitRepoInfo GitRepositoryRepo from configurations
   * @return true/false if supported
   */
  public boolean isPullRequestFeatureSupported(
      final Application app, final GitRepositoryInfo gitRepoInfo)
  {
    if (!licenseChecker.isPullRequestRemediationSupported()) {
      log.debug("Remediation pull request feature is not supported for this license");
      return false;
    }

    if (!isApplicationConfiguredForPR(gitRepoInfo)) {
      log.debug("Pull requests have not been configured for application '{}'", app.getId());
      return false;
    }

    if (!gitRepoInfo.provider.supportsPullRequests()) {
      log.debug("Source provider '{}' is not supported", gitRepoInfo.provider);
      return false;
    }
    
    if (!pullRequestRepositoryValidator.isRepoValidForPRs(gitRepoInfo)) {
      log.debug("Pull requests are not supported for application '{}' and repository '{}'",
          app.getId(), gitRepoInfo.repositoryUrl);
      return false;
    }

    return true;
  }

  private boolean isApplicationConfiguredForPR(final GitRepositoryInfo gitRepositoryInfo) {
    if (gitRepositoryInfo == null) {
      return false;
    }
    if (!isTrue(gitRepositoryInfo.remediationPullRequestsEnabled)) {
      log.debug("Pull Requests have been explicitly disabled");
      return false;
    }

    // check for missing fields
    List<String> missingFields = new ArrayList<>();
    if (gitRepositoryInfo.provider == null) {
      missingFields.add("Provider");
    }
    if (isBlank(gitRepositoryInfo.repositoryUrl)) {
      missingFields.add("Repository URL");
    }
    if (gitRepositoryInfo.provider != null && gitRepositoryInfo.provider.requiresUsername() &&
        isBlank(gitRepositoryInfo.username)) {
      missingFields.add("Username");
    }
    if (isBlank(gitRepositoryInfo.token)) {
      missingFields.add("Token");
    }
    if (!missingFields.isEmpty()) {
      log.debug("Application has not been fully configured for pull requests. Missing: [{}]",
          String.join(", ", missingFields));
      return false;
    }

    return true;
  }
}
