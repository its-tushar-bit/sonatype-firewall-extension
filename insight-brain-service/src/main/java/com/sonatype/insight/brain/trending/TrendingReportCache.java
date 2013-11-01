/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.trending;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.trending.TrendingReport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

/**
 * @since 1.7 
 */
@Named
@Singleton
public class TrendingReportCache
{
  private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();

  private final InsightWork insightWork;

  @Inject
  public TrendingReportCache(InsightWork insightWork) {
    this.insightWork = insightWork;
  }

  public void writeCache(TrendingReport report) throws IOException {
    cacheLock.writeLock().lock();
    try {
      JsonUtils.write(getCacheFile(), report);
    }
    finally {
      cacheLock.writeLock().unlock();
    }
  }

  public TrendingReport readCached() throws IOException {
    cacheLock.readLock().lock();
    try {
      File cacheFile = getCacheFile();
      if (cacheFile.canRead()) {
        return JsonUtils.read(cacheFile, TrendingReport.class);
      }
      return null;
    }
    finally {
      cacheLock.readLock().unlock();
    }
  }

  /**
   * Public to facilitate testing
   * 
   * @since 1.7
   */
  public File getCacheFile() {
    return new File(insightWork.getReportDir(), "trending-report.json");
  }

  /**
   * For testing purposes
   * 
   * @since 1.7
   */
  public void purgeCache() {
    cacheLock.writeLock().lock();
    try {
      getCacheFile().delete();
    }
    finally {
      cacheLock.writeLock().unlock();
    }
  }

}
