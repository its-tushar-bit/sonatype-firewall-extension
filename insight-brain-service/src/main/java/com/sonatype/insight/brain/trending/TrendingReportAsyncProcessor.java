/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.trending;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.model.trending.TrendingReport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.7
 */
@Named
public class TrendingReportAsyncProcessor
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final TrendingReportCache cache;

  private final TrendingReportProcessor processor;

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
          cache.writeCache(report);
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
  public TrendingReportAsyncProcessor(TrendingReportCache cache, TrendingReportProcessor processor) {
    this.cache = cache;
    this.processor = processor;
  }

  public void calculate() {
    worker.schedule();
  }

}
