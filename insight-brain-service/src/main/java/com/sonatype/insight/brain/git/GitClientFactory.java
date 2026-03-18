/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.nexus.scm.github.auth.GitHubAppAuthStrategy;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControl.AuthenticationType;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class GitClientFactory
{
  private final InsightProxy insightProxy;

  private final GitHubAppAuthStrategyCache authStrategyCache;

  private final GitApiClientFactory gitApiClientFactory = new GitApiClientFactory();

  private final PasswordHandler passwordHandler;

  private static final Logger log = LoggerFactory.getLogger(GitClientFactory.class);

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
  public GitClientFactory(
      final InsightProxy insightProxy,
      final GitHubAppAuthStrategyCache authStrategyCache,
      final PasswordHandler passwordHandler)
  {
    this.insightProxy = insightProxy;
    this.authStrategyCache = authStrategyCache;
    this.passwordHandler = passwordHandler;
  }

  public GitApiClient createApiClient(GitRepositoryInfo gitRepositoryInfo) {
    Configuration configuration = gitApiClientFactory.createConfiguration();
    String apiUrl = getApiUrl(gitRepositoryInfo, configuration);
    insightProxy.contextualize(configuration, apiUrl);

    if (gitRepositoryInfo.authenticationType == AuthenticationType.GITHUB_APP) {
      GitHubAppAuthStrategy authStrategy = getGitHubAppAuthStrategy(gitRepositoryInfo);
      return gitApiClientFactory.getGitHubApiClient(
          configuration,
          gitRepositoryInfo.normalizedRepositoryUrl,
          authStrategy);
    }

    return gitApiClientFactory.getGitApiClient(gitRepositoryInfo.provider, configuration,
        gitRepositoryInfo.normalizedRepositoryUrl, gitRepositoryInfo.username, gitRepositoryInfo.token);
  }

  public PullRequestInfoProvider createPullRequestInfoClient(GitRepositoryInfo gitRepositoryInfo) {
    Configuration configuration = gitApiClientFactory.createConfiguration();
    String graphqlApiUrl = getPullRequestInfoClientUrl(gitRepositoryInfo, configuration);
    insightProxy.contextualize(configuration, graphqlApiUrl);

    if (gitRepositoryInfo.authenticationType == AuthenticationType.GITHUB_APP) {
      GitHubAppAuthStrategy authStrategy = getGitHubAppAuthStrategy(gitRepositoryInfo);
      return gitApiClientFactory.getGitHubPullRequestInfoClient(
          configuration,
          authStrategy);
    }

    return gitApiClientFactory.getPullRequestInfoClient(gitRepositoryInfo.provider, configuration,
        gitRepositoryInfo.username, gitRepositoryInfo.token, gitRepositoryInfo.normalizedRepositoryUrl);
  }

  public ContributorInfoProvider createContributorInfoProvider(GitRepositoryInfo gitRepositoryInfo) {
    Configuration configuration = gitApiClientFactory.createConfiguration();
    String graphqlApiUrl = getContributorInfoProviderUrl(gitRepositoryInfo, configuration);
    insightProxy.contextualize(configuration, graphqlApiUrl);

    if (gitRepositoryInfo.authenticationType == AuthenticationType.GITHUB_APP) {
      GitHubAppAuthStrategy authStrategy = getGitHubAppAuthStrategy(gitRepositoryInfo);
      return gitApiClientFactory.getGitHubContributorInfoClient(
          configuration,
          authStrategy);
    }

    return gitApiClientFactory.getContributorInfoClient(
        gitRepositoryInfo.provider, configuration, gitRepositoryInfo.token);
  }

  public GeneralSCMApiClient createGeneralApiClient(
      final SourceControlProvider sourceControlProvider,
      final String hostUrl,
      final String username,
      final String token)
  {
    Configuration configuration = createConfigurationWithBaseUrl(sourceControlProvider, hostUrl, token);
    return gitApiClientFactory.getGeneralSCMApiClient(sourceControlProvider, configuration, username, token);
  }

  public GitApiClientUtils getClientUtils(final SourceControlProvider provider, Configuration configuration) {
    return gitApiClientFactory.getGitApiClientUtils(provider, configuration);
  }

  private Configuration createConfigurationWithBaseUrl(
      final SourceControlProvider provider,
      final String hostUrl,
      final String token)
  {
    Configuration configuration = gitApiClientFactory.createConfiguration();
    insightProxy.contextualize(configuration);
    String baseApiUrl = getClientUtils(provider, configuration).getBaseApiUrl(hostUrl, token);
    configuration.setServerUrl(baseApiUrl);
    return configuration;
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

  private GitHubAppAuthStrategy getGitHubAppAuthStrategy(final GitRepositoryInfo gitRepositoryInfo) {
    if (gitRepositoryInfo.authOwnerId == null) {
      throw new IllegalStateException(
          "GitHub App authentication is configured but no owner ID found for authentication lookup. "
              + "Repository: " + gitRepositoryInfo.normalizedRepositoryUrl
              + ". Please ensure a GitHub App is registered at the application or parent organization level.");
    }

    return authStrategyCache.getOrCreate(gitRepositoryInfo.authOwnerId);
  }

  public GeneralSCMApiClient createGeneralApiClient(
      final SourceControlProvider provider,
      final String hostUrl,
      final GitHubApp gitHubApp) throws IOException
  {
    if (provider != SourceControlProvider.GITHUB) {
      throw new IllegalArgumentException("GitHub App authentication only supports GitHub provider");
    }

    if (gitHubApp.getInstallationId() == null) {
      throw new IllegalArgumentException("GitHub App installation ID is required");
    }

    Configuration configuration = createConfigurationWithBaseUrl(provider, hostUrl, null);

    log.debug("Creating GitHub API client with GitHub App installationId: {}", gitHubApp.getInstallationId());

    GitHubAppAuthStrategy authStrategy = authStrategyCache.getOrCreate(gitHubApp.getOwnerId());

    return gitApiClientFactory.getGitHubGeneralSCMApiClient(configuration, authStrategy);
  }
}
