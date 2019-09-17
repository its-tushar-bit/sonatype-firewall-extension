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

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.license.model.LicensedFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logic for if the automated pull request feature should run
 */
@Named
@Singleton
public class PullRequestFeatureCheck
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestFeatureCheck.class);

  private final ProductLicense productLicense;

  private final GitApiService gitApiService;

  private final PullRequestUtils pullRequestUtils;

  @Inject
  public PullRequestFeatureCheck(
      final ProductLicense productLicense,
      final GitApiService gitApiService,
      final PullRequestUtils pullRequestUtils)
  {
    this.productLicense = productLicense;
    this.gitApiService = gitApiService;
    this.pullRequestUtils = pullRequestUtils;
  }

  public boolean isPullRequestFeatureSupported(final Application app) throws IOException {
    if (!isLicenseValid()) {
      log.debug("Pull request feature is not supported for this license");
      return false;
    }

    String applicationId = app.getPublicId();
    GitRepositoryInfo gitRepoInfo = gitApiService.getGitRepositoryInfoForApplication(applicationId);

    if (!isApplicationConfiguredForPR(gitRepoInfo)) {
      log.debug("Pull requests have not been configured for application '{}'", applicationId);
      return false;
    }

    if (!pullRequestUtils.isPullRequestAllowed(gitRepoInfo)) {
      log.debug("Pull requests are not supported for application '{}' and repository '{}'",
          applicationId, gitRepoInfo.repositoryUrl);
      return false;
    }

    return true;
  }

  private boolean isLicenseValid() {
    // TODO(INT-2096): the exact feature required for Git Notifications are still TBD
    return productLicense.hasFeature(LicensedFeature.NOTIFICATIONS);
  }

  private boolean isApplicationConfiguredForPR(final GitRepositoryInfo gitRepositoryInfo) {
    // Check that we have all the necessary fields
    return gitRepositoryInfo.isDataComplete();
  }
}
