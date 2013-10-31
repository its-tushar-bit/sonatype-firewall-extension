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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.trending.TrendingReport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trending report generation and caching.
 * 
 * @since 1.7
 */
@Named
@Path(TrendingReportService.SERVICE_PATH)
public class TrendingReportService
{
  public static final String SERVICE_PATH = "rest/trending";

  public static final long CACHE_MAX_AGE_MS = 86400L * 1; // one day

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();

  private final TrendingReportProcessor processor;

  private final InsightWork insightWork;

  private class WorkerThread
      extends Thread
  {
    private final Object processorLock = new Object()
    {
    };

    public WorkerThread() {
      super("Trending report worker thread");
      setDaemon(true);
      start();
    }

    @Override
    public void run() {
      while (true) {
        synchronized (processorLock) {
          try {
            processorLock.wait();
          }
          catch (InterruptedException e) {
            break;
          }
        }

        try {
          TrendingReport report = processor.calculate();
          writeCache(report);
        }
        catch (IOException e) {
          log.error("Could not generate trending report", e);
        }
      }
      log.info(getName() + " terminated");
    }

    public void schedule() {
      synchronized (processorLock) {
        processorLock.notify();
      }
    }
  }

  private final WorkerThread worker = new WorkerThread();

  @Inject
  public TrendingReportService(TrendingReportProcessor processor, InsightWork insightWork) {
    this.processor = processor;
    this.insightWork = insightWork;
  }

  /**
   * Returns trending report data. Returns cached version, if available. Trending report data is automatically
   * regenerated if cached copy is older than {@link #CACHE_MAX_AGE_MS} milliseconds. If cached trending report data is
   * not available, initiates trending report data calculation in a background thread.
   * 
   * @param force is set to {@code true}, expire cached and generate new trending report data.
   * @return returns trending report data. returns {@code null} if trending report data has not been calculated yet.
   * @since 1.7
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public TrendingReport get(@QueryParam("force") boolean force) throws IOException {
    TrendingReport cached = !force ? readCached() : null;

    if (cached != null && (System.currentTimeMillis() - cached.getMeta().getGeneratedOn()) < CACHE_MAX_AGE_MS) {
      return cached;
    }

    worker.schedule();

    return cached;
  }

  private TrendingReport readCached() throws IOException {
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

  public void writeCache(TrendingReport report) throws IOException {
    cacheLock.writeLock().lock();
    try {
      JsonUtils.write(getCacheFile(), report);
    }
    finally {
      cacheLock.writeLock().unlock();
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
