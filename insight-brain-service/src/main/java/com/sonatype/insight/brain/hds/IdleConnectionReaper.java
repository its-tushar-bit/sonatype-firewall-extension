/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.http.conn.HttpClientConnectionManager;

/**
 * Background thread that evicts HTTP connections that have not been used for some time from their pool as recommended
 * by <a href="https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html">Connection eviction
 * policy</a>.
 * 
 * @since 1.18
 */
@Named
@Singleton
public class IdleConnectionReaper
{
  private final Collection<SoftReference<HttpClientConnectionManager>> connectionManagerRefs = new CopyOnWriteArraySet<>();

  public IdleConnectionReaper() {
    new Worker(this).start();
  }

  private void closeIdleConnections() {
    for (SoftReference<HttpClientConnectionManager> connectionManagerRef : connectionManagerRefs) {
      HttpClientConnectionManager connectionManager = connectionManagerRef.get();
      if (connectionManager != null) {
        connectionManager.closeIdleConnections(30, TimeUnit.SECONDS);
      }
      else {
        connectionManagerRefs.remove(connectionManagerRef);
      }
    }
  }

  public void register(HttpClientConnectionManager connectionManager) {
    connectionManagerRefs.add(new SoftReference<>(connectionManager));
  }

  private static class Worker
      extends Thread
  {
    private final SoftReference<IdleConnectionReaper> reaperRef;

    public Worker(IdleConnectionReaper reaper) {
      super(IdleConnectionReaper.class.getSimpleName());
      setDaemon(true);
      setPriority(MIN_PRIORITY);
      this.reaperRef = new SoftReference<>(reaper);
    }

    @Override
    public void run() {
      while (true) {
        try {
          Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        }
        catch (InterruptedException e) {
          // ignored
        }
        IdleConnectionReaper reaper = reaperRef.get();
        if (reaper == null) {
          return;
        }
        reaper.closeIdleConnections();
      }
    }
  }
}
