/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.githubapp;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.tenancy.TenantReference;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * The Optional<GitHubApp> is intentional here. It's used as a negative cache — the distinction between three states:
 * 1. null return from cache.getIfPresent() — key not in cache, need to query the DB
 * 2. Optional.empty() stored in cache — we already queried and know there's no GitHub App for this owner
 * 3. Optional.of(app) stored in cache — we have a cached result
 */
@Named
@Singleton
public class GitHubAppSelectionCache
{
  private final TenantReference<Cache<String, Optional<GitHubApp>>> caches;

  public GitHubAppSelectionCache() {
    this.caches = new TenantReference<>(this::createCache);
  }

  public Optional<GitHubApp> get(String requestingOwnerId) {
    return caches.get().getIfPresent(requestingOwnerId);
  }

  public void put(String requestingOwnerId, Optional<GitHubApp> app) {
    caches.get().put(requestingOwnerId, app);
  }

  public void invalidateAll() {
    caches.get().invalidateAll();
  }

  private Cache<String, Optional<GitHubApp>> createCache() {
    return CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build();
  }
}
