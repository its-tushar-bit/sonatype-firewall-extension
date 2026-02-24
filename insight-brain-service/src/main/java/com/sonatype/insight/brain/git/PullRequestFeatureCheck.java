/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.git;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.sourcecontrol.AuthenticationValidator;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isBlank;

public abstract class PullRequestFeatureCheck
{
  protected static final Logger log = LoggerFactory.getLogger(PullRequestFeatureCheck.class);

  protected final IqForScmLicenseChecker licenseChecker;

  protected PullRequestFeatureCheck(final IqForScmLicenseChecker licenseChecker) {
    this.licenseChecker = licenseChecker;
  }

  protected boolean isLicenseSupported() {
    return licenseChecker.isPullRequestRemediationSupported();
  }

  protected boolean isSourceProviderSupported(final GitRepositoryInfo gitRepoInfo) {
    return gitRepoInfo.provider.supportsPullRequests();
  }

  protected boolean isSCMConfigured(final GitRepositoryInfo gitRepositoryInfo) {
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
    // Check authentication based on type - for backward compatibility, null/unknown types check token
    if (!AuthenticationValidator.hasValidCredentials(gitRepositoryInfo)) {
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
