/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.scm.api.GitApiClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logic for the 'visibility' settings of a SCM repository. Mainly if 'public' or 'private', but some SCMs such as
 * GitHub also have the concept of an 'internal' repository which is a repo marked as 'private' but not available on the
 * public Internet.
 */
@Named
@Singleton
public class ScmRepoVisibilityService
{
  private static final Logger log = LoggerFactory.getLogger(ScmRepoVisibilityService.class);

  private final FeaturesService featuresService;

  private final GitClientFactory gitClientFactory;

  @Inject
  public ScmRepoVisibilityService(final FeaturesService featuresService, final GitClientFactory gitClientFactory) {
    this.featuresService = featuresService;
    this.gitClientFactory = gitClientFactory;
  }

  /**
   * Pull requests are only allowed on repositories that are either internal or private. Public repositories are only
   * allowed based on the licensed feature flag {@link LicensedFeature#ALLOW_SCM_ON_PUBLIC_REPOS}.
   *
   * @return true if the repository is valid for pull requests
   */
  public boolean isRepositoryValidForPullRequestFeatures(final GitRepositoryInfo gitRepositoryInfo) {
    return isInternalRepository(gitRepositoryInfo) || isPrivateRepository(gitRepositoryInfo) ||
        isScmAllowedOnPublicRepositories();
  }

  /**
   * Returns {@code true} if a repository is internal only (e.g. GitHub Enterprise)
   */
  public boolean isInternalRepository(final GitRepositoryInfo gitRepositoryInfo) {
    return gitRepositoryInfo.provider.isScmSecured(gitRepositoryInfo.normalizedRepositoryUrl);
  }

  public boolean isPrivateRepository(final GitRepositoryInfo gitRepositoryInfo) {
    GitApiClient client = gitClientFactory.createApiClient(gitRepositoryInfo);
    try {
      return client.isRepositoryPrivate();
    }
    catch (IOException e) {
      log.error("Error when checking if repository is private", e);
      throw new UncheckedIOException("Unable to connect to the repository " + gitRepositoryInfo.normalizedRepositoryUrl,
          e);
    }
  }

  /**
   * Some features that are visible on the customer side in the SCM repo (e.g. pull requests) are normally not allowed
   * on publicly visible repositories. This is due to legal concerns of leaking our proprietary data to the public.
   * However, we have a license based feature flag that allows disabling this restriction.
   */
  public boolean isScmAllowedOnPublicRepositories() {
    Set<Feature> features = featuresService.getFeatures();
    return features.contains(LicensedFeature.ALLOW_SCM_ON_PUBLIC_REPOS);
  }
}
