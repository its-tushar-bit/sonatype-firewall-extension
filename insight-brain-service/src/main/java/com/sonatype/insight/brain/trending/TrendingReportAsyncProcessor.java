/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.trending;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.model.trending.TrendingReport;
import com.sonatype.insight.brain.trending.TrendingReportProcessor.ProgressMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.7
 */
@Named
public class TrendingReportAsyncProcessor
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  private volatile Long startTime;

  private final AtomicInteger total = new AtomicInteger();

  private final AtomicInteger current = new AtomicInteger();

  private final TrendingReportCache cache;

  private final TrendingReportProcessor processor;

  private static final AtomicInteger id = new AtomicInteger();

  private class WorkerThread
      extends Thread
  {
    private final Object processorLock = new Object()
    {
    };

    public WorkerThread() {
      super("Trending report worker thread " + id.incrementAndGet());
      setDaemon(true);

      log.debug("Created TrendingReportAsyncProcessor worker thread '{}'", getName());
    }

    @Override
    public void run() {
      log.debug("Starting TrendingReportAsyncProcessor worker thread '{}'", getName());

      while (true) {
        try {
          beforeStart();
          TrendingReport report = processor.calculate(newProgressMonitor());
          cache.writeCache(report);
        }
        catch (IOException e) {
          log.error("Could not generate trending report", e);
        }
        finally {
          afterFinish();
        }

        synchronized (processorLock) {
          try {
            processorLock.wait();
          }
          catch (InterruptedException e) {
            break;
          }
        }
      }
      log.info(getName() + " terminated");
    }

    public void schedule() {
      log.debug("Scheduling TrendingReportAsyncProcessor worker execution '{}'", getName());

      synchronized (processorLock) {
        processorLock.notify();
      }
    }
  }

  private volatile WorkerThread worker;

  @Inject
  public TrendingReportAsyncProcessor(TrendingReportCache cache, TrendingReportProcessor processor) {
    this.cache = cache;
    this.processor = processor;
  }

  public void calculate() {
    if (worker == null) {
      synchronized (this) {
        if (worker == null) {
          worker = new WorkerThread();
          worker.start();
        }        
      }
    }
    worker.schedule();
  }

  protected void beforeStart() {
    startTime = new Long(System.currentTimeMillis());
    total.set(0);
    current.set(0);
  }

  protected void afterFinish() {
    startTime = null;
    total.set(0);
    current.set(0);
  }

  public long getStartTime() {
    return startTime != null ? startTime : -1;
  }

  public int getTotal() {
    return total.get();
  }

  public int getCurrent() {
    return current.get();
  }

  protected ProgressMonitor newProgressMonitor() {
    return new ProgressMonitor()
    {
      @Override
      public void tick(int total, int current) {
        TrendingReportAsyncProcessor.this.total.set(total);
        TrendingReportAsyncProcessor.this.current.set(current);
      }
    };
  }
}
