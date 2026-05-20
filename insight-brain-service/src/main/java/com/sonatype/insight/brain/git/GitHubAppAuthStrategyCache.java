/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.security.PrivateKey;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
   * Get or create a GitHubAppAuthStrategy for the given ownerId.
   *
   * @param ownerId the GitHub App owner ID
   * @return cached or newly created GitHubAppAuthStrategy
   */
  public GitHubAppAuthStrategy getOrCreate(final String ownerId) {
    return getOrCreateCached(ownerId).strategy;
  }

  /**
   * Package-private: returns the holder so siblings can read {@code sourceGithubAppId}/{@code sourceInstallationId}
   * for audit/trace purposes without re-querying the DAO.
   */
  CachedAuthStrategy getOrCreateCached(final String ownerId) {
    try {
      return getCache().get(ownerId, () -> createAuthStrategy(ownerId));
    }
    catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      throw new RuntimeException("Failed to create GitHubAppAuthStrategy for ownerId: " + ownerId, cause);
    }
  }

  /**
   * Invalidate the cached strategy for the given ownerId.
   *
   * @param ownerId the GitHub App owner ID
   */
  public void invalidate(final String ownerId) {
    getCache().invalidate(ownerId);
    log.debug("Invalidated cached GitHubAppAuthStrategy for ownerId: {}", ownerId);
  }

  /**
   * Invalidate every cache entry whose strategy was sourced from the given GitHub App. This covers entries cached under
   * a child owner that resolved its auth via inheritance from a parent's app, which the per-owner {@link #invalidate}
   * call cannot reach. Best-effort: callers should not rely on a return value, and exceptions are swallowed.
   *
   * @param githubAppId the id of the GitHub App that was deleted, deactivated, or replaced; null is a no-op
   */
  public void invalidateByGitHubAppId(final Integer githubAppId) {
    if (githubAppId == null) {
      return;
    }
    Cache<String, CachedAuthStrategy> cache = getCache();
    List<String> toInvalidate = cache.asMap()
        .entrySet()
        .stream()
        .filter(e -> Objects.equals(e.getValue().sourceGithubAppId, githubAppId))
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
    cache.invalidateAll(toInvalidate);
    log.debug("Invalidated {} cached GitHubAppAuthStrategy entries derived from githubAppId: {}",
        toInvalidate.size(), githubAppId);
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

  private CachedAuthStrategy createAuthStrategy(final String ownerId) {
    log.debug("Creating new GitHubAppAuthStrategy for ownerId: {}", ownerId);

    GitHubApp githubApp = githubAppDAO.getByOwnerIdNotNull(ownerId);

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
   * Internal cache value pairing the strategy with the source GitHub App identifiers, so that
   * {@link #invalidateByGitHubAppId} can target by-app eviction across owner-hierarchy inheritance.
   */
  static final class CachedAuthStrategy
  {
    final GitHubAppAuthStrategy strategy;

    final Integer sourceGithubAppId;

    final Long sourceInstallationId;

    CachedAuthStrategy(
        final GitHubAppAuthStrategy strategy,
        final Integer sourceGithubAppId,
        final Long sourceInstallationId)
    {
      this.strategy = strategy;
      this.sourceGithubAppId = sourceGithubAppId;
      this.sourceInstallationId = sourceInstallationId;
    }
  }
}
