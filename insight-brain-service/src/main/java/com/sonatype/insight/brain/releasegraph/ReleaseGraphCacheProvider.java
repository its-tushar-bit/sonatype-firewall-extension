/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.Configuration;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;

@Named
@Singleton
public class ReleaseGraphCacheProvider
    implements Provider<LoadingCache<ReleaseGraphKey, byte[]>>
{
  private final Configuration configuration;

  private final Provider<ReleaseGraphCacheLoader> cacheLoaderProvider;

  private final AtomicReference<LoadingCache<ReleaseGraphKey, byte[]>> cache = new AtomicReference<>();

  @Inject
  public ReleaseGraphCacheProvider(Configuration configuration, Provider<ReleaseGraphCacheLoader> cacheLoaderProvider) {
    this.configuration = configuration;
    this.cacheLoaderProvider = cacheLoaderProvider;
    initializeCache();
  }

  public void initializeCache() {
    cache.set(CacheBuilder.newBuilder()
        .maximumSize(configuration.getReleaseGraphCacheSize())
        .build(cacheLoaderProvider.get()));
  }

  @Override
  public LoadingCache<ReleaseGraphKey, byte[]> get() {
    return cache.get();
  }
}
