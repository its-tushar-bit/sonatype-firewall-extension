/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.io.PrintWriter;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMultimap;
import com.yammer.dropwizard.tasks.Task;

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
  public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
    output.write("Starting cache size: " + cache.size());
    cache.invalidateAll();
    output.write("\nFinal cache size: " + cache.size());
  }
}
