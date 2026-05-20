/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.security.PrivateKey;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.github.auth.GitHubAppAuthStrategy;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches GitHubAppAuthStrategy instances to avoid excessive token generation.
 * Cache is tenant-aware and expires entries after 30 minutes of inactivity.
 *
 * @since 1.201
 */
@Named
@Singleton
public class GitHubAppAuthStrategyCache
{
  private static final Logger log = LoggerFactory.getLogger(GitHubAppAuthStrategyCache.class);

  static final String DEFAULT_GITHUB_API_BASE_URL = "https://api.github.com";

  // 30-minute TTL balances performance (reuse) and memory (cleanup for inactive apps)
  static final Duration EXPIRATION_AFTER_ACCESS = Duration.ofMinutes(30);

  static final long MAXIMUM_SIZE = 1000L;

  private final GitHubAppDAO githubAppDAO;

  private final InsightProxy insightProxy;

  private final GitApiClientFactory gitApiClientFactory;

  private final PasswordHandler passwordHandler;

  private final String githubApiBaseUrl;

  private final TenantReference<Cache<String, CachedAuthStrategy>> caches;

  @Inject
  public GitHubAppAuthStrategyCache(
      final GitHubAppDAO githubAppDAO,
      final InsightProxy insightProxy,
      final GitApiClientFactory gitApiClientFactory,
      final PasswordHandler passwordHandler)
  {
    this(githubAppDAO, insightProxy, gitApiClientFactory, passwordHandler, DEFAULT_GITHUB_API_BASE_URL);
  }

  public GitHubAppAuthStrategyCache(
      final GitHubAppDAO githubAppDAO,
      final InsightProxy insightProxy,
      final GitApiClientFactory gitApiClientFactory,
      final PasswordHandler passwordHandler,
      final String githubApiBaseUrl)
  {
    this.githubAppDAO = githubAppDAO;
    this.insightProxy = insightProxy;
    this.gitApiClientFactory = gitApiClientFactory;
    this.passwordHandler = passwordHandler;
    this.githubApiBaseUrl = githubApiBaseUrl;
    this.caches = new TenantReference<>(this::createCache);
  }

  /**
   * Get or create a GitHubAppAuthStrategy for the given githubAppId.
   *
   * @param githubAppId the GitHub App internal UUID
   * @return cached or newly created GitHubAppAuthStrategy
   */
  public GitHubAppAuthStrategy getOrCreate(final String githubAppId) {
    return getOrCreateCached(githubAppId).strategy;
  }

  /**
   * Package-private: returns the holder so siblings can read {@code sourceGithubAppId}/{@code sourceInstallationId}
   * for audit/trace purposes without re-querying the DAO.
   */
  CachedAuthStrategy getOrCreateCached(final String githubAppId) {
    try {
      return getCache().get(githubAppId, () -> createAuthStrategy(githubAppId));
    }
    catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      throw new RuntimeException("Failed to create GitHubAppAuthStrategy for githubAppId: " + githubAppId, cause);
    }
  }

  /**
   * Invalidate the cached strategy for the given githubAppId.
   *
   * @param githubAppId the GitHub App internal UUID
   */
  public void invalidate(final String githubAppId) {
    getCache().invalidate(githubAppId);
    log.debug("Invalidated cached GitHubAppAuthStrategy for githubAppId: {}", githubAppId);
  }

  private Cache<String, CachedAuthStrategy> createCache() {
    return newCacheBuilder()
        .expireAfterAccess(EXPIRATION_AFTER_ACCESS.toMillis(), TimeUnit.MILLISECONDS)
        .maximumSize(MAXIMUM_SIZE)
        .build();
  }

  private CacheBuilder<Object, Object> newCacheBuilder() {
    return CacheBuilder.newBuilder();
  }

  private Cache<String, CachedAuthStrategy> getCache() {
    return caches.get();
  }

  private CachedAuthStrategy createAuthStrategy(final String githubAppId) {
    log.debug("Creating new GitHubAppAuthStrategy for githubAppId: {}", githubAppId);

    GitHubApp githubApp = githubAppDAO.getByGithubAppId(githubAppId);
    if (githubApp == null) {
      throw new com.sonatype.insight.error.exception.NotFoundException(
          "GitHub App not found: " + githubAppId);
    }

    String decryptedBase64Key = passwordHandler.decryptPassword(githubApp.getPrivateKey());
    PrivateKey privateKey = GitHubAppKeyUtils.parsePrivateKeyFromBase64Pkcs8(decryptedBase64Key);

    Configuration restConfiguration = gitApiClientFactory.createConfiguration();
    insightProxy.contextualize(restConfiguration, githubApiBaseUrl);

    GitHubAppAuthStrategy strategy = new GitHubAppAuthStrategy(
        restConfiguration,
        privateKey,
        githubApp.getAppId(),
        githubApp.getInstallationId());

    return new CachedAuthStrategy(strategy, githubApp.getAppId(), githubApp.getInstallationId());
  }

  /**
   * Internal cache value pairing the strategy with the source GitHub App identifiers
   */
  record CachedAuthStrategy(GitHubAppAuthStrategy strategy, Integer sourceGithubAppId, Long sourceInstallationId)
  {
  }
}
