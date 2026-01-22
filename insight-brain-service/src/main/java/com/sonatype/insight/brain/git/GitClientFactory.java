/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.ContributorInfoProvider;
import com.sonatype.nexus.scm.api.GeneralSCMApiClient;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.GitApiClientUtils;
import com.sonatype.nexus.scm.api.PullRequestInfoProvider;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class GitClientFactory
{
  private final InsightProxy insightProxy;

  private final GitApiClientFactory gitApiClientFactory = new GitApiClientFactory();

  /**
   * Caches API URLs for git API clients
   */
  private final TenantReference<Cache<String, String>> apiClientUrlCache =
      new TenantReference<>(() -> CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build());

  /**
   * Caches API URLs for PullRequest info clients
   */
  private final TenantReference<Cache<String, String>> prInfoClientUrlCache =
      new TenantReference<>(() -> CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build());

  @Inject
  public GitClientFactory(final InsightProxy insightProxy) {
    this.insightProxy = insightProxy;
  }

  public GitApiClient createApiClient(GitRepositoryInfo gitRepositoryInfo) {
    Configuration configuration = gitApiClientFactory.createConfiguration();
    String apiUrl = getApiUrl(gitRepositoryInfo, configuration);
    insightProxy.contextualize(configuration, apiUrl);

    return gitApiClientFactory.getGitApiClient(gitRepositoryInfo.provider, configuration,
        gitRepositoryInfo.normalizedRepositoryUrl, gitRepositoryInfo.username, gitRepositoryInfo.token);
  }

  public PullRequestInfoProvider createPullRequestInfoClient(GitRepositoryInfo gitRepositoryInfo) {
    Configuration configuration = gitApiClientFactory.createConfiguration();

    String graphqlApiUrl = getPullRequestInfoClientUrl(gitRepositoryInfo, configuration);
    insightProxy.contextualize(configuration, graphqlApiUrl);

    return gitApiClientFactory.getPullRequestInfoClient(gitRepositoryInfo.provider, configuration,
        gitRepositoryInfo.username, gitRepositoryInfo.token, gitRepositoryInfo.normalizedRepositoryUrl);
  }

  public ContributorInfoProvider createContributorInfoProvider(GitRepositoryInfo gitRepositoryInfo) {
    Configuration configuration = gitApiClientFactory.createConfiguration();

    final String graphqlApiUrl = getContributorInfoProviderUrl(gitRepositoryInfo, configuration);
    insightProxy.contextualize(configuration, graphqlApiUrl);

    return gitApiClientFactory.getContributorInfoClient(
        gitRepositoryInfo.provider, configuration, gitRepositoryInfo.token);
  }

  public GeneralSCMApiClient createGeneralApiClient(
      final SourceControlProvider sourceControlProvider,
      final String hostUrl,
      final String username,
      final String token)
  {
    Configuration configuration = gitApiClientFactory.createConfiguration();
    insightProxy.contextualize(configuration);
    String baseApiUrl = getClientUtils(sourceControlProvider, configuration).getBaseApiUrl(hostUrl, token);
    configuration.setServerUrl(baseApiUrl);
    return gitApiClientFactory.getGeneralSCMApiClient(sourceControlProvider, configuration, username, token);
  }

  public GitApiClientUtils getClientUtils(final SourceControlProvider provider, Configuration configuration) {
    return gitApiClientFactory.getGitApiClientUtils(provider, configuration);
  }

  @VisibleForTesting
  void addApiUrlMapping(String repositoryUrl, String apiUrl) {
    String cacheKey = computeKey(repositoryUrl);
    apiClientUrlCache.get().put(cacheKey, apiUrl);
  }

  @VisibleForTesting
  void addPullRequestInfoClientUrlMapping(String repositoryUrl, String apiUrl) {
    String cacheKey = computeKey(repositoryUrl);
    prInfoClientUrlCache.get().put(cacheKey, apiUrl);
  }

  @VisibleForTesting
  void clearUrlCaches() {
    apiClientUrlCache.get().invalidateAll();
    prInfoClientUrlCache.get().invalidateAll();
  }

  @VisibleForTesting
  String getApiUrl(final GitRepositoryInfo gitRepositoryInfo, Configuration configuration) {
    return getUrl(gitRepositoryInfo, apiClientUrlCache.get(),
        gri -> getClientUtils(gri.provider, configuration).getApiUrl(gri.normalizedRepositoryUrl, gri.token));
  }

  @VisibleForTesting
  String getPullRequestInfoClientUrl(final GitRepositoryInfo gitRepositoryInfo, Configuration configuration) {
    return getUrl(gitRepositoryInfo, prInfoClientUrlCache.get(),
        gri -> getClientUtils(gri.provider, configuration)
            .getPullRequestInfoProviderUrl(gri.normalizedRepositoryUrl, gri.token));
  }

  /**
   * Cache key is a shorter version of the repository URL containing only the scheme, host, (maybe port) and first path
   * segment (possible a URL context)
   */
  private String computeKey(final String repositoryUrl) {
    int index = StringUtils.ordinalIndexOf(repositoryUrl, "/", 4);
    return index == -1 ? repositoryUrl : repositoryUrl.substring(0, index);
  }

  private String getUrl(
      final GitRepositoryInfo gitRepositoryInfo,
      final Cache<String, String> cache,
      final Function<GitRepositoryInfo, String> urlGetter)
  {
    String cacheKey = computeKey(gitRepositoryInfo.normalizedRepositoryUrl);
    String apiUrl = cache.getIfPresent(cacheKey);
    if (apiUrl == null) { // not in cache
      apiUrl = urlGetter.apply(gitRepositoryInfo);
      cache.put(cacheKey, apiUrl);
    }
    return apiUrl;
  }

  private String getContributorInfoProviderUrl(final GitRepositoryInfo gitRepositoryInfo, Configuration configuration) {
    return getUrl(gitRepositoryInfo, prInfoClientUrlCache.get(),
        gri -> getClientUtils(gri.provider, configuration)
            .getContributorInfoProviderUrl(gri.normalizedRepositoryUrl));
  }
}
