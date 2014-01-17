/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.model.GAVPopularity;
import com.sonatype.insight.brain.model.ReportPopularity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReleaseGraphCacheLoader
    extends CacheLoader<ReleaseGraphKey, byte[]>
{
  private static final Logger log = LoggerFactory.getLogger(ReleaseGraphCacheLoader.class);

  private LoadingCache<ReportItemKey, ReportPopularity> cache = CacheBuilder.newBuilder()
      .expireAfterAccess(5, TimeUnit.MINUTES).build(new ReportItemCacheLoader());

  @Override
  public byte[] load(ReleaseGraphKey key) throws Exception {
    ReportPopularity reportPopularity = cache.get(key.getReportItemKey());
    for (GAVPopularity pop : reportPopularity.getPopularity()) {
      if (key.isMatch(pop)) {
        ReleaseGraph graph = new ReleaseGraph(ReleaseGraphModel.build(pop, reportPopularity.getFirstCatalog(),
            reportPopularity.getLastCatalog(), ReleaseGraphModel.SLOTS), ReleaseGraphModel.SLOTS);
        return graph.getBytes();
      }
    }
    log.debug("ReleaseGraphCacheLoader: No match for GAV: {}", key.getGAV());
    return new byte[0];
  }
}
