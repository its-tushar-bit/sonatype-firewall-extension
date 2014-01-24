/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.model.trending.TrendingReport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.7
 */
@Named
@Singleton
public class TrendingReportCache
{
  private static final Logger log = LoggerFactory.getLogger(TrendingReportCache.class);

  private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();

  private final InsightWork insightWork;

  private final FileCleaner fileCleaner;

  @Inject
  public TrendingReportCache(InsightWork insightWork, FileCleaner fileCleaner) {
    this.insightWork = insightWork;
    this.fileCleaner = fileCleaner;
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
    File file = getCacheFile();
    try {
      fileCleaner.delete(file);
    }
    catch (FileDeletionException e) {
      log.error("Could not delete incomplete report: {}", file, e);
    }
    finally {
      cacheLock.writeLock().unlock();
    }
  }

}
