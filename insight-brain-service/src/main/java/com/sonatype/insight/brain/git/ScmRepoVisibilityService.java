/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.scm.api.GitApiClient;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Logic for the 'visibility' of an SCM repository. This is primarily if the repo is 'public' or 'private', and then we
 * restrict usage of some SCM features for public repositories. Note that some SCMs such as GitLab also have the concept
 * of an 'internal' repository which is a repo marked as 'public' but still requiring authentication and therefore not
 * available on the public Internet (so private as far as our logic is concerned).
 */
@Named
@Singleton
public class ScmRepoVisibilityService
{
  private final FeaturesService featuresService;

  private final TenantReference<LoadingCache<GitRepositoryInfo, Boolean>> privateRepoCache;

  @Inject
  public ScmRepoVisibilityService(final FeaturesService featuresService, final GitClientFactory gitClientFactory) {
    this.featuresService = featuresService;

    privateRepoCache = new TenantReference<>(() -> CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES)
        .build(new GitPrivateRepoCacheLoader(gitClientFactory)));
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
    try {
      return privateRepoCache.get().get(gitRepositoryInfo);
    }
    catch (Exception e) {
      throw new RuntimeException(
          "Error when checking if repository is private " + gitRepositoryInfo.normalizedRepositoryUrl, e);
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

  /**
   * Local {@link CacheLoader} for {@link GitRepositoryInfo} objects. This is used to be the check to see if a
   * repository is private. We cannot permanently store this value as someone could then circumvent restrictions by
   * making a repo private, enabling the repo, and then making it public again. So we must check each time, but this is
   * also a performance hit. So we cache the value for 5 minutes.
   */
  private static class GitPrivateRepoCacheLoader
      extends CacheLoader<GitRepositoryInfo, Boolean>
  {
    private final GitClientFactory gitClientFactory;

    @Inject
    public GitPrivateRepoCacheLoader(final GitClientFactory gitClientFactory) {
      this.gitClientFactory = gitClientFactory;
    }

    @Override
    public Boolean load(final GitRepositoryInfo gitRepositoryInfo) throws IOException {
      GitApiClient client = gitClientFactory.createApiClient(gitRepositoryInfo);
      return client.isRepositoryPrivate();
    }
  }
}
