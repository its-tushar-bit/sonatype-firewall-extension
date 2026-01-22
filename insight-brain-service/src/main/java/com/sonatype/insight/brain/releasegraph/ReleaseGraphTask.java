/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.google.common.cache.LoadingCache;
import io.dropwizard.servlets.tasks.Task;

@Named
public class ReleaseGraphTask
    extends Task
{
  private final ReleaseGraphCacheProvider releaseGraphCacheProvider;

  @Inject
  public ReleaseGraphTask(ReleaseGraphCacheProvider releaseGraphCacheProvider) {
    super("clearReleaseGraphCache");
    this.releaseGraphCacheProvider = releaseGraphCacheProvider;
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) throws Exception {
    LoadingCache<ReleaseGraphKey, byte[]> cache = releaseGraphCacheProvider.get();
    output.write("Starting cache size: " + cache.size());
    cache.invalidateAll();
    output.write("\nFinal cache size: " + cache.size());
  }
}
