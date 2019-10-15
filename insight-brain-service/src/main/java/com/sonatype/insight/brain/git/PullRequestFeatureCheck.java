/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.ImmutableList;
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

  private static final List<SourceControlProvider> SUPPORTED_PROVIDERS = ImmutableList.of(SourceControlProvider.GITHUB);

  private final ProductLicense productLicense;

  private final PullRequestUtils pullRequestUtils;

  @Inject
  public PullRequestFeatureCheck(
      final ProductLicense productLicense,
      final PullRequestUtils pullRequestUtils)
  {
    this.productLicense = productLicense;
    this.pullRequestUtils = pullRequestUtils;
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
      throws IOException
  {
    if (!isLicenseValid()) {
      log.debug("Pull request feature is not supported for this license");
      return false;
    }

    if (!isApplicationConfiguredForPR(gitRepoInfo)) {
      log.debug("Pull requests have not been configured for application '{}'", app.getId());
      return false;
    }

    if (!isProviderSupported(gitRepoInfo)) {
      log.debug("Source provider '{}' is not supported", gitRepoInfo.provider);
    }

    if (!pullRequestUtils.isPullRequestAllowed(gitRepoInfo)) {
      log.debug("Pull requests are not supported for application '{}' and repository '{}'",
          app.getId(), gitRepoInfo.repositoryUrl);
      return false;
    }

    return true;
  }

  private boolean isLicenseValid() {
    return productLicense.hasFeature(LicensedFeature.AUTOMATION);
  }

  private boolean isProviderSupported(final GitRepositoryInfo gitRepoInfo) {
    return gitRepoInfo != null && SUPPORTED_PROVIDERS.contains(gitRepoInfo.provider);
  }

  private boolean isApplicationConfiguredForPR(final GitRepositoryInfo gitRepositoryInfo) {
    // Check that we have all the necessary fields
    return gitRepositoryInfo != null && gitRepositoryInfo.isDataComplete();
  }
}
