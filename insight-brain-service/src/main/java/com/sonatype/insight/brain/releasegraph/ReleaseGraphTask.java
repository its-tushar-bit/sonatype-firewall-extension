/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.cache.LoadingCache;
import io.dropwizard.servlets.tasks.Task;

@Named
public class ReleaseGraphTask
    extends Task
{
  private LoadingCache<ReleaseGraphKey, byte[]> cache;

  @Inject
  public ReleaseGraphTask(LoadingCache<ReleaseGraphKey, byte[]> cache) {
    super("clearReleaseGraphCache");
    this.cache = cache;
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) throws Exception {
    output.write("Starting cache size: " + cache.size());
    cache.invalidateAll();
    output.write("\nFinal cache size: " + cache.size());
  }
}
