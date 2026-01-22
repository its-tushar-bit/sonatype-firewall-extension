/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.time.Duration;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;
import com.sonatype.insight.brain.model.component.RepositoryIdentifiedComponent;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;

@Named
@Singleton
public class RepositoryIdentifiedComponentCache
{
  // Visible for testing
  static final Duration MAX_AGE = Duration.ofDays(1);

  static final long MAXIMUM_SIZE = 100_000L;

  private final RepositoryIdentifiedComponentCacheLoader repositoryIdentifiedComponentCacheLoader;

  private final RepositoryIdentifiedComponentDAO repositoryIdentifiedComponentDAO;

  private final TenantReference<LoadingCache<String, ComponentIdentifier>> loadingCaches;

  @Inject
  public RepositoryIdentifiedComponentCache(
      RepositoryIdentifiedComponentCacheLoader repositoryIdentifiedComponentCacheLoader,
      RepositoryIdentifiedComponentDAO repositoryIdentifiedComponentDAO)
  {
    this.repositoryIdentifiedComponentCacheLoader = repositoryIdentifiedComponentCacheLoader;
    this.repositoryIdentifiedComponentDAO = repositoryIdentifiedComponentDAO;
    loadingCaches = new TenantReference<>(this::createLoadingCache);
  }

  // Visible for testing
  LoadingCache<String, ComponentIdentifier> createLoadingCache() {
    return newCacheBuilder()
        .expireAfterWrite(MAX_AGE.toMillis(), TimeUnit.MILLISECONDS)
        .maximumSize(MAXIMUM_SIZE)
        .build(repositoryIdentifiedComponentCacheLoader);
  }

  // Visible for testing
  CacheBuilder<Object, Object> newCacheBuilder() {
    return CacheBuilder.newBuilder();
  }

  public ComponentIdentifier get(String hash) {
    try {
      return getLoadingCache().getUnchecked(hash);
    }
    catch (Exception e) {
      if (e.getCause() instanceof NotFoundException) {
        return null;
      }
      throw e;
    }
  }

  public void put(String hash, ComponentIdentifier componentIdentifier) {
    Date date = new Date();
    RepositoryIdentifiedComponent repositoryIdentifiedComponent =
        new RepositoryIdentifiedComponent(hash, componentIdentifier, date, date);
    repositoryIdentifiedComponentDAO.update(repositoryIdentifiedComponent);
    getLoadingCache().put(hash, componentIdentifier);
  }

  public ComponentIdentifier removeByHash(String hash) {
    return getLoadingCache().asMap().remove(hash);
  }

  public int removeByComponentIdentifier(ComponentIdentifier componentIdentifier) {
    LoadingCache<String, ComponentIdentifier> loadingCache = getLoadingCache();
    Set<String> toRemove = loadingCache.asMap().entrySet().stream()
        .filter(e -> e.getValue().equals(componentIdentifier))
        .map(Entry::getKey)
        .collect(Collectors.toSet());
    toRemove.forEach(loadingCache::invalidate);
    return toRemove.size();
  }

  // Visible for testing
  public LoadingCache<String, ComponentIdentifier> getLoadingCache() {
    return loadingCaches.get();
  }

  public long removeAll() {
    LoadingCache<String, ComponentIdentifier> loadingCache = getLoadingCache();
    long size = loadingCache.size();
    loadingCache.invalidateAll();
    return size;
  }
}
