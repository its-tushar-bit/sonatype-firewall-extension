/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.Closeable;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Holds the near-real-time searcher manager for the process-lifetime Lucene writer.
 * <p>
 * Refresh lag is the age, in milliseconds, of the oldest committed writer change that has not yet
 * been observed by a successful {@link SearcherManager#maybeRefresh()} or {@link #reopen(IndexWriter)}.
 * Scheduled no-op/failed refresh attempts do not reset that clock, so the gauge reports searcher
 * staleness rather than refresh task activity.
 */
public class LuceneSearcherManagerHolder
    implements Closeable
{
  static final long REFRESH_CADENCE_MILLIS = 1_000L;

  private static final long NO_PENDING_REFRESH = -1L;

  private static final Logger log = LoggerFactory.getLogger(LuceneSearcherManagerHolder.class);

  private final Clock clock;

  private final ScheduledExecutorService refreshExecutor;

  private final Map<IndexSearcher, SearcherAcquisition> acquiredSearchers = new IdentityHashMap<>();

  private final Timer acquireWaitTimer;

  private volatile SearcherManager searcherManager;

  private final AtomicLong pendingRefreshSinceMillis = new AtomicLong(NO_PENDING_REFRESH);

  private volatile long lastAcquireWaitNanos;

  private volatile long lastSuccessfulRefreshMillis;

  private volatile boolean closed;

  LuceneSearcherManagerHolder(final IndexWriter writer) throws IOException {
    this(writer, Clock.systemUTC(), true, null);
  }

  LuceneSearcherManagerHolder(
      final IndexWriter writer,
      final Clock clock,
      final boolean scheduleRefresh) throws IOException
  {
    this(writer, clock, scheduleRefresh, null);
  }

  LuceneSearcherManagerHolder(
      final IndexWriter writer,
      final Clock clock,
      final boolean scheduleRefresh,
      final MeterRegistry meterRegistry) throws IOException
  {
    this.clock = clock;
    this.searcherManager = new SearcherManager(writer, null);
    this.lastSuccessfulRefreshMillis = clock.millis();
    this.acquireWaitTimer = registerMetrics(meterRegistry);
    if (scheduleRefresh) {
      this.refreshExecutor = Executors.newSingleThreadScheduledExecutor(
          new ThreadFactoryBuilder().setNameFormat("lucene-searcher-refresh-%d").setDaemon(true).build());
      this.refreshExecutor.scheduleAtFixedRate(this::refreshSafely, REFRESH_CADENCE_MILLIS, REFRESH_CADENCE_MILLIS,
          TimeUnit.MILLISECONDS);
    }
    else {
      this.refreshExecutor = null;
    }
  }

  public LuceneSearcherManagerHolder(final IndexWriter writer, final MeterRegistry meterRegistry) throws IOException {
    this(writer, Clock.systemUTC(), true, meterRegistry);
  }

  /**
   * Acquires a searcher from the current manager, then registers it under {@code acquiredSearchers}.
   * The lock intentionally does not wrap {@link SearcherManager#acquire()} (avoids deadlock with
   * {@link #pause()}/{@link #reopen()}). Lucene's release path remains safe if the manager is closed
   * in the gap between acquire and registration.
   */
  public IndexSearcher acquire() throws IOException {
    long start = System.nanoTime();
    try {
      SearcherManager manager = currentManager();
      IndexSearcher searcher = manager.acquire();
      synchronized (acquiredSearchers) {
        SearcherAcquisition acquisition = acquiredSearchers.get(searcher);
        if (acquisition == null) {
          acquiredSearchers.put(searcher, new SearcherAcquisition(manager));
        }
        else {
          acquisition.increment();
        }
      }
      return searcher;
    }
    finally {
      recordAcquireWait(System.nanoTime() - start);
    }
  }

  public void release(final IndexSearcher indexSearcher) throws IOException {
    SearcherManager manager;
    synchronized (acquiredSearchers) {
      SearcherAcquisition acquisition = acquiredSearchers.get(indexSearcher);
      if (acquisition == null) {
        throw new IOException("Lucene IndexSearcher was not acquired from this holder.");
      }
      manager = acquisition.manager();
      if (acquisition.decrement() == 0) {
        acquiredSearchers.remove(indexSearcher);
      }
    }
    manager.release(indexSearcher);
  }

  public void onCommitSignal() throws IOException {
    pendingRefreshSinceMillis.compareAndSet(NO_PENDING_REFRESH, clock.millis());
    maybeRefresh();
  }

  public synchronized void reopen(final IndexWriter writer) throws IOException {
    SearcherManager previous = searcherManager;
    searcherManager = new SearcherManager(writer, null);
    markRefreshSuccessful(clock.millis());
    if (previous != null) {
      previous.close();
    }
  }

  public synchronized void pause() throws IOException {
    SearcherManager previous = searcherManager;
    searcherManager = null;
    if (previous != null) {
      previous.close();
    }
  }

  /**
   * False while paused/closed so callers can fall back to opening a DirectoryReader instead of
   * failing {@link #acquire()} during rebuild/shutdown.
   */
  public synchronized boolean isUsable() {
    return !closed && searcherManager != null;
  }

  public long getLastAcquireWaitNanos() {
    return lastAcquireWaitNanos;
  }

  public long getRefreshLagMillis() {
    long pendingSince = pendingRefreshSinceMillis.get();
    if (pendingSince == NO_PENDING_REFRESH) {
      return 0L;
    }
    return Math.max(0L, clock.millis() - pendingSince);
  }

  public Instant getLastSuccessfulRefreshInstant() {
    return Instant.ofEpochMilli(lastSuccessfulRefreshMillis);
  }

  @Override
  public synchronized void close() throws IOException {
    if (closed) {
      return;
    }
    closed = true;
    if (refreshExecutor != null) {
      refreshExecutor.shutdownNow();
    }
    synchronized (acquiredSearchers) {
      if (!acquiredSearchers.isEmpty()) {
        log.warn("Closing Lucene SearcherManager with {} acquired searchers still registered",
            acquiredSearchers.size());
        acquiredSearchers.clear();
      }
    }
    if (searcherManager != null) {
      searcherManager.close();
    }
  }

  private synchronized void maybeRefresh() throws IOException {
    if (closed || searcherManager == null) {
      return;
    }
    long now = clock.millis();
    if (currentManager().maybeRefresh()) {
      markRefreshSuccessful(now);
    }
  }

  private void refreshSafely() {
    try {
      maybeRefresh();
    }
    catch (Exception e) {
      log.warn("Unable to refresh Lucene searcher manager; search results may be stale until the next refresh.", e);
    }
  }

  private SearcherManager currentManager() throws IOException {
    SearcherManager manager = searcherManager;
    if (closed || manager == null) {
      throw new SearcherManagerUnavailableException("Lucene SearcherManager is closed.");
    }
    return manager;
  }

  private Timer registerMetrics(final MeterRegistry meterRegistry) {
    if (meterRegistry == null) {
      return null;
    }
    Timer timer = Timer.builder("search.lucene.searcher.acquire.wait")
        .description("Time spent acquiring Lucene NRT searchers")
        .register(meterRegistry);
    Gauge.builder("search.lucene.searcher.refresh.lag", this, LuceneSearcherManagerHolder::getRefreshLagMillis)
        .description("Milliseconds since committed Lucene writer changes became pending for SearcherManager refresh")
        .baseUnit("milliseconds")
        .register(meterRegistry);
    return timer;
  }

  private void recordAcquireWait(final long waitNanos) {
    lastAcquireWaitNanos = waitNanos;
    if (acquireWaitTimer != null) {
      acquireWaitTimer.record(waitNanos, TimeUnit.NANOSECONDS);
    }
  }

  private void markRefreshSuccessful(final long refreshMillis) {
    pendingRefreshSinceMillis.set(NO_PENDING_REFRESH);
    lastSuccessfulRefreshMillis = refreshMillis;
  }

  private static class SearcherAcquisition
  {
    private final SearcherManager manager;

    private int count = 1;

    SearcherAcquisition(final SearcherManager manager) {
      this.manager = manager;
    }

    SearcherManager manager() {
      return manager;
    }

    void increment() {
      count++;
    }

    int decrement() {
      return --count;
    }
  }
}
