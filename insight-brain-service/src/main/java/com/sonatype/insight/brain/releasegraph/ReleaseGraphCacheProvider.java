/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.InsightConfig;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;

@Named
@Singleton
public class ReleaseGraphCacheProvider
    implements Provider<LoadingCache<ReleaseGraphKey, byte[]>>
{
  private final InsightConfig config;

  private final Provider<ReleaseGraphCacheLoader> cacheLoaderProvider;

  @Inject
  public ReleaseGraphCacheProvider(InsightConfig config, Provider<ReleaseGraphCacheLoader> cacheLoaderProvider) {
    this.config = config;
    this.cacheLoaderProvider = cacheLoaderProvider;
  }

  @Override
  public LoadingCache<ReleaseGraphKey, byte[]> get() {
    return CacheBuilder.newBuilder().maximumSize(config.getReleaseGraphCacheSize()).build(cacheLoaderProvider.get());
  }
}
