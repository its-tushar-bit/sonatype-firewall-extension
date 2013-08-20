/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.cache.LoadingCache;
import com.yammer.metrics.core.HealthCheck;

@Named
public class ReleaseGraphHealthCheck
    extends HealthCheck
{
  private LoadingCache<ReleaseGraphKey, byte[]> cache;

  @Inject
  public ReleaseGraphHealthCheck(LoadingCache<ReleaseGraphKey, byte[]> cache) {
    super("Release Graph");
    this.cache = cache;
  }

  @Override
  protected Result check() throws Exception {
    return Result.healthy("Cache Size - " + cache.size());
  }
}
