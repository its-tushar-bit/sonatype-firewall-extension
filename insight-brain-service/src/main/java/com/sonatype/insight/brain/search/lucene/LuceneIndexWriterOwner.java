/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns the tenant-scoped Lucene writer and near-real-time {@link LuceneSearcherManagerHolder}.
 * <p>
 * On-prem SINGLE_TENANT keeps one entry. MTIQ opens a distinct writer/searcher per tenant because
 * {@link LuceneComponents#openSearchIndex(boolean)} resolves a tenant-specific directory via
 * {@link com.sonatype.insight.brain.service.InsightWork#getSearchIndexDir()}.
 * <p>
 * {@link #rebuildExclusive(Runnable)} and {@link #runWithWriter(WriterWork)} are mutually exclusive
 * per tenant: both hold the tenant's {@link TenantIndex#lock}. Read paths use
 * {@link #tryGetSearcherManagerHolder()} ({@link ReentrantLock#tryLock()}) so searches fall back to
 * {@link org.apache.lucene.index.DirectoryReader} instead of blocking while a rebuild holds the lock.
 */
public class LuceneIndexWriterOwner
    implements Closeable, TenantManaged
{
  private static final Logger log = LoggerFactory.getLogger(LuceneIndexWriterOwner.class);

  private final LuceneComponents luceneComponents;

  /**
   * Analyzer used when {@link #luceneComponents} is null (test owners). Production owners resolve
   * a fresh analyzer from {@link LuceneComponents} on each writer open.
   */
  private final Analyzer testAnalyzer;

  private final MeterRegistry meterRegistry;

  private final ConcurrentMap<Tenant, TenantIndex> tenantIndexes;

  /**
   * Fixed index state for {@link #openForTest(Directory, Analyzer)}; null in production.
   */
  private final TenantIndex testIndex;

  private final AtomicBoolean closed = new AtomicBoolean();

  public LuceneIndexWriterOwner(
      final LuceneComponents luceneComponents,
      final ShutdownHandler shutdownHandler)
  {
    this(luceneComponents, shutdownHandler, null);
  }

  public LuceneIndexWriterOwner(
      final LuceneComponents luceneComponents,
      final ShutdownHandler shutdownHandler,
      final MeterRegistry meterRegistry)
  {
    this.luceneComponents = luceneComponents;
    this.testAnalyzer = null;
    this.meterRegistry = meterRegistry;
    this.tenantIndexes = new ConcurrentHashMap<>();
    this.testIndex = null;
    shutdownHandler.add(() -> {
      try {
        close();
      }
      catch (IOException e) {
        log.error("Unable to close Lucene index writer cleanly during shutdown.", e);
      }
      return false;
    }, ShutdownPriority.DEFAULT);
  }

  public static LuceneIndexWriterOwner openForTest(
      final Directory directory,
      final Analyzer analyzer) throws IOException
  {
    return new LuceneIndexWriterOwner(directory, analyzer);
  }

  private LuceneIndexWriterOwner(
      final Directory directory,
      final Analyzer analyzer) throws IOException
  {
    this.luceneComponents = null;
    this.testAnalyzer = analyzer;
    this.meterRegistry = null;
    this.tenantIndexes = null;
    TenantIndex index = new TenantIndex();
    index.directory = directory;
    index.writer = openWriter(directory, analyzer, OpenMode.CREATE_OR_APPEND);
    this.testIndex = index;
  }

  public IndexWriter getWriter() {
    TenantIndex index = state();
    index.lock.lock();
    try {
      ensureAvailable(index);
      ensureWriterOpen(index);
      return index.writer;
    }
    finally {
      index.lock.unlock();
    }
  }

  public Optional<LuceneSearcherManagerHolder> tryGetSearcherManagerHolder() {
    TenantIndex index = existingState();
    if (index == null || !index.lock.tryLock()) {
      return Optional.empty();
    }
    try {
      if (!index.available || index.searcherManagerHolder == null || !index.searcherManagerHolder.isUsable()) {
        return Optional.empty();
      }
      return Optional.of(index.searcherManagerHolder);
    }
    finally {
      index.lock.unlock();
    }
  }

  public LuceneSearcherManagerHolder getSearcherManagerHolder() {
    TenantIndex index = state();
    index.lock.lock();
    try {
      ensureAvailable(index);
      if (index.searcherManagerHolder == null) {
        throw new SearchIndexException("Lucene SearcherManager holder is not available.",
            new IllegalStateException("Lucene SearcherManager holder is not available."));
      }
      return index.searcherManagerHolder;
    }
    finally {
      index.lock.unlock();
    }
  }

  public void setSearcherManagerHolder(final LuceneSearcherManagerHolder searcherManagerHolder) {
    TenantIndex index = state();
    index.lock.lock();
    try {
      if (index.searcherManagerHolder != null && index.searcherManagerHolder != searcherManagerHolder) {
        try {
          index.searcherManagerHolder.close();
        }
        catch (IOException e) {
          log.warn("Unable to close previous Lucene SearcherManager holder", e);
        }
      }
      index.searcherManagerHolder = searcherManagerHolder;
    }
    finally {
      index.lock.unlock();
    }
  }

  public void commitAndMaybeRefresh() throws IOException {
    TenantIndex index = state();
    index.lock.lock();
    try {
      ensureAvailable(index);
      ensureWriterOpen(index);
      try {
        index.writer.commit();
        if (index.searcherManagerHolder != null) {
          index.searcherManagerHolder.onCommitSignal();
        }
      }
      catch (IOException | RuntimeException e) {
        markUnavailable(index);
        throw e;
      }
    }
    finally {
      index.lock.unlock();
    }
  }

  /**
   * Runs {@code work} against the open writer under the tenant monitor, then commits and signals
   * the SearcherManager holder (same end state as {@link #commitAndMaybeRefresh()}).
   */
  public void runWithWriter(final WriterWork work) throws Exception {
    TenantIndex index = state();
    index.lock.lock();
    try {
      ensureAvailable(index);
      ensureWriterOpen(index);
      try {
        work.run(index.writer);
        index.writer.commit();
        if (index.searcherManagerHolder != null) {
          index.searcherManagerHolder.onCommitSignal();
        }
      }
      catch (Exception e) {
        markUnavailable(index);
        throw e;
      }
    }
    finally {
      index.lock.unlock();
    }
  }

  @FunctionalInterface
  public interface WriterWork
  {
    void run(IndexWriter writer) throws Exception;
  }

  public void rebuildExclusive(final Runnable rebuild) {
    if (closed.get()) {
      throw new SearchIndexException("Lucene index writer is closed; cannot rebuild.",
          new IllegalStateException("Lucene index writer is closed."));
    }
    TenantIndex index = state();
    index.lock.lock();
    try {
      try {
        // Best-effort quiesce. Integration tests (and some ops paths) may delete the on-disk
        // index directory out from under the live writer; flush/commit then fails with
        // NoSuchFileException on write.lock. Recover by recreating directory + writer.
        try {
          if (index.writer != null && index.writer.isOpen()) {
            index.writer.flush();
            index.writer.commit();
          }
        }
        catch (IOException | RuntimeException e) {
          log.warn("Unable to commit Lucene writer before rebuild; recreating index from scratch", e);
        }
        try {
          pauseSearcherManagerHolder(index);
        }
        catch (IOException e) {
          log.warn("Unable to pause Lucene SearcherManager before rebuild", e);
        }
        try {
          closeWriter(index);
        }
        catch (IOException e) {
          log.warn("Unable to close Lucene writer before rebuild", e);
          index.writer = null;
        }
        if (luceneComponents != null) {
          try {
            closeDirectory(index);
          }
          catch (IOException e) {
            log.warn("Unable to close Lucene directory before rebuild", e);
            index.directory = null;
          }
          index.directory = luceneComponents.openSearchIndex(false);
        }
        else if (index.directory == null) {
          throw new SearchIndexException("Lucene directory is not available for rebuild",
              new IllegalStateException("Lucene directory is null"));
        }
        index.writer = openWriter(index.directory, OpenMode.CREATE);
        index.available = true;
        rebuild.run();
        index.writer.commit();
        if (index.searcherManagerHolder != null) {
          index.searcherManagerHolder.reopen(index.writer);
        }
      }
      catch (Exception e) {
        markUnavailable(index);
        throw new SearchIndexException("Error rebuilding Lucene search index", e);
      }
    }
    finally {
      index.lock.unlock();
    }
  }

  @Override
  public void deregister() {
    if (tenantIndexes == null) {
      return;
    }
    TenantIndex removed = tenantIndexes.remove(TenantThreadLocal.getTenant());
    if (removed != null) {
      closeQuietly(removed);
    }
  }

  @Override
  public void close() throws IOException {
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    IOException failure = null;
    List<TenantIndex> indexes = new ArrayList<>();
    if (testIndex != null) {
      indexes.add(testIndex);
    }
    if (tenantIndexes != null) {
      indexes.addAll(tenantIndexes.values());
      tenantIndexes.clear();
    }
    for (TenantIndex index : indexes) {
      failure = closeTenantIndex(index, failure);
    }
    if (failure != null) {
      throw failure;
    }
  }

  TenantIndex currentIndexForTest() {
    return state();
  }

  private TenantIndex state() {
    if (testIndex != null) {
      return testIndex;
    }
    if (closed.get()) {
      throw new SearchIndexException("Lucene index writer is closed; cannot rebuild.",
          new IllegalStateException("Lucene index writer is closed."));
    }
    Tenant tenant = TenantThreadLocal.getTenant();
    return tenantIndexes.computeIfAbsent(tenant, ignored -> openTenantIndex());
  }

  private TenantIndex existingState() {
    if (testIndex != null) {
      return testIndex;
    }
    if (closed.get()) {
      return null;
    }
    return tenantIndexes.get(TenantThreadLocal.getTenant());
  }

  private TenantIndex openTenantIndex() {
    try {
      Directory directory = luceneComponents.openSearchIndex(false);
      IndexWriter writer = openWriter(directory, OpenMode.CREATE_OR_APPEND);
      LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, meterRegistry);
      TenantIndex index = new TenantIndex();
      index.directory = directory;
      index.writer = writer;
      index.searcherManagerHolder = holder;
      return index;
    }
    catch (IOException e) {
      throw new SearchIndexException("Unable to open Lucene index writer", e);
    }
  }

  private IndexWriter openWriter(final Directory directory, final OpenMode openMode) throws IOException {
    Analyzer analyzer = luceneComponents != null ? luceneComponents.newAnalyzerForSearch() : testAnalyzer;
    if (analyzer == null) {
      throw new IllegalStateException("Lucene analyzer is not available");
    }
    return openWriter(directory, analyzer, openMode);
  }

  private IndexWriter openWriter(
      final Directory directory,
      final Analyzer analyzer,
      final OpenMode openMode) throws IOException
  {
    try {
      return new IndexWriter(directory, new IndexWriterConfig(analyzer).setOpenMode(openMode));
    }
    catch (LockObtainFailedException e) {
      throw new LockObtainFailedException(
          "Unable to obtain Lucene write.lock for the search index. Another process may still own the index, or a " +
              "previous process may have exited uncleanly. Stop competing IQ Server processes and inspect the search " +
              "index directory before restarting.",
          e);
    }
  }

  private void ensureAvailable(final TenantIndex index) {
    if (closed.get() || !index.available) {
      throw new SearchIndexException("Lucene index writer is unavailable; indexing is halted until the writer reopens.",
          new IllegalStateException("Lucene index writer is unavailable."));
    }
  }

  private void ensureWriterOpen(final TenantIndex index) {
    if (index.writer == null || !index.writer.isOpen()) {
      markUnavailable(index);
      throw new SearchIndexException("Lucene index writer is unavailable; indexing is halted until process restart.",
          new IllegalStateException("Lucene index writer is closed."));
    }
  }

  private void markUnavailable(final TenantIndex index) {
    index.available = false;
  }

  private void closeWriter(final TenantIndex index) throws IOException {
    if (index.writer != null) {
      index.writer.close();
      index.writer = null;
    }
  }

  private void closeSearcherManagerHolder(final TenantIndex index) throws IOException {
    if (index.searcherManagerHolder != null) {
      index.searcherManagerHolder.close();
    }
  }

  private void pauseSearcherManagerHolder(final TenantIndex index) throws IOException {
    if (index.searcherManagerHolder != null) {
      index.searcherManagerHolder.pause();
    }
  }

  private void closeDirectory(final TenantIndex index) throws IOException {
    if (index.directory != null) {
      index.directory.close();
      index.directory = null;
    }
  }

  private void closeQuietly(final TenantIndex index) {
    try {
      IOException failure = closeTenantIndex(index, null);
      if (failure != null) {
        log.warn("Unable to close Lucene index cleanly for tenant deregister", failure);
      }
    }
    catch (RuntimeException e) {
      log.warn("Unable to close Lucene index cleanly for tenant deregister", e);
    }
  }

  private IOException closeTenantIndex(final TenantIndex index, final IOException previous) {
    IOException failure = previous;
    index.lock.lock();
    try {
      try {
        if (index.writer != null && index.writer.isOpen()) {
          index.writer.flush();
          index.writer.commit();
        }
      }
      catch (IOException e) {
        failure = firstOrSuppressed(failure, e);
      }
      failure = closeAndCapture(() -> closeSearcherManagerHolder(index), failure);
      failure = closeAndCapture(() -> closeWriter(index), failure);
      failure = closeAndCapture(() -> closeDirectory(index), failure);
      index.available = false;
    }
    finally {
      index.lock.unlock();
    }
    return failure;
  }

  private IOException closeAndCapture(final CloseOperation closeOperation, final IOException previous) {
    try {
      closeOperation.close();
      return previous;
    }
    catch (IOException e) {
      return firstOrSuppressed(previous, e);
    }
  }

  private static IOException firstOrSuppressed(final IOException previous, final IOException next) {
    if (previous != null) {
      previous.addSuppressed(next);
      return previous;
    }
    return next;
  }

  @FunctionalInterface
  private interface CloseOperation
  {
    void close() throws IOException;
  }

  static final class TenantIndex
  {
    final ReentrantLock lock = new ReentrantLock();

    Directory directory;

    IndexWriter writer;

    LuceneSearcherManagerHolder searcherManagerHolder;

    volatile boolean available = true;
  }
}
