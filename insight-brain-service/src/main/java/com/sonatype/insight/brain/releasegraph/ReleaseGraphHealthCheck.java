/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.service.AbstractOperationalCheck;

import com.google.common.cache.LoadingCache;

@Named
public class ReleaseGraphHealthCheck
    extends AbstractOperationalCheck
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
