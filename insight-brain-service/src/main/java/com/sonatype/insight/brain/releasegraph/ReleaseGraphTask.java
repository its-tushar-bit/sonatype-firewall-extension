/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import com.google.common.cache.LoadingCache;
import com.sonatype.insight.brain.service.AdminTask;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class ReleaseGraphTask
    extends AdminTask
{
  public static final String PATH = "clearReleaseGraphCache";

  private final ReleaseGraphCacheProvider releaseGraphCacheProvider;

  @Inject
  public ReleaseGraphTask(ReleaseGraphCacheProvider releaseGraphCacheProvider) {
    super(PATH);
    this.releaseGraphCacheProvider = releaseGraphCacheProvider;
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) throws Exception {
    clearCache();
  }

  public void clearCache() {
    LoadingCache<ReleaseGraphKey, byte[]> cache = releaseGraphCacheProvider.get();
    cache.invalidateAll();
  }
}
