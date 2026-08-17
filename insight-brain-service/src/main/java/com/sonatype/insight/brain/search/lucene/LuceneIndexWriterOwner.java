/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
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
 * {@link #rebuildExclusive(WriterWork)} builds a green generation without wiping the serving (blue)
 * index; readers keep using the blue {@link LuceneSearcherManagerHolder} until a short cutover.
 * {@link #runWithWriter(WriterWork)} is skipped while a full rebuild is in progress so the outbox
 * can catch up after cutover. Cutover briefly holds the tenant {@link TenantIndex#lock}.
 * <p>
 * Blue/green requires renaming the serving directory while searchers may still hold its files open, which is
 * POSIX-only. Deployments on filesystems that refuse it — Windows — rebuild in place instead, so search is
 * unavailable for the duration of a rebuild there.
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

  /**
   * False on filesystems that refuse to rename a directory holding open files, where the cutover cannot work.
   */
  private final boolean blueGreenSupported;

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
    this(luceneComponents, shutdownHandler, meterRegistry, supportsRenameWithOpenReaders());
  }

  LuceneIndexWriterOwner(
      final LuceneComponents luceneComponents,
      final ShutdownHandler shutdownHandler,
      final MeterRegistry meterRegistry,
      final boolean blueGreenSupported)
  {
    this.luceneComponents = luceneComponents;
    this.testAnalyzer = null;
    this.meterRegistry = meterRegistry;
    this.tenantIndexes = new ConcurrentHashMap<>();
    this.testIndex = null;
    this.blueGreenSupported = blueGreenSupported;
    if (luceneComponents != null && !blueGreenSupported) {
      log.info("Filesystem does not support renaming a directory with open readers; Lucene full rebuilds will run "
          + "in place and search will be unavailable while one runs.");
    }
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
    this.blueGreenSupported = false;
    TenantIndex index = new TenantIndex();
    index.directory = directory;
    index.writer = openWriter(directory, analyzer, OpenMode.CREATE_OR_APPEND);
    this.testIndex = index;
  }

  /**
   * Cutover renames the serving directory while searchers may still hold its files open: {@link
   * LuceneSearcherManagerHolder#acquire()} runs outside the holder lock to avoid deadlocking with {@code pause()},
   * so pausing cannot drain a searcher acquired just before it. POSIX allows the rename regardless; Windows fails it
   * with {@code AccessDeniedException}.
   */
  private static boolean supportsRenameWithOpenReaders() {
    return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
  }

  /**
   * True when a full rebuild will build a separate green generation and cut over, rather than wiping in place.
   */
  private boolean usesBlueGreen() {
    return luceneComponents != null && blueGreenSupported;
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
      return usableHolder(index);
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

  /**
   * Reads the holder without {@link TenantIndex#lock} so reads don't serialize behind the writer's commit; empty when
   * the index is not yet initialized, unavailable, or paused.
   */
  public Optional<LuceneSearcherManagerHolder> getSearcherManagerHolderIfUsable() {
    return usableHolder(existingState());
  }

  private static Optional<LuceneSearcherManagerHolder> usableHolder(final TenantIndex index) {
    if (index == null || !index.available) {
      return Optional.empty();
    }
    LuceneSearcherManagerHolder holder = index.searcherManagerHolder;
    // Read the volatile once: a re-read could observe null after the usability check passes.
    if (holder == null || !holder.isUsable()) {
      return Optional.empty();
    }
    return Optional.of(holder);
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
    if (index.fullRebuildInProgress.get()) {
      log.debug("Skipping Lucene incremental update while a full rebuild is in progress");
      return;
    }
    index.lock.lock();
    try {
      if (index.fullRebuildInProgress.get()) {
        log.debug("Skipping Lucene incremental update while a full rebuild is in progress");
        return;
      }
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

  /**
   * True while a blue/green full rebuild is building green or cutting over. Incremental writers
   * should leave {@code search_index_change} rows for catch-up after cutover.
   */
  public boolean isFullRebuildInProgress() {
    TenantIndex index = existingState();
    return index != null && index.fullRebuildInProgress.get();
  }

  /**
   * Requests that an in-flight full rebuild abort before cutover. Blue keeps serving; green is
   * discarded. Cooperative — checked during populate, when the green build returns, and again
   * immediately before cutover.
   */
  public void requestCancelFullRebuild() {
    TenantIndex index = existingState();
    if (index != null) {
      index.cancelRequested.set(true);
    }
  }

  /**
   * Throws if {@link #requestCancelFullRebuild()} was called for the current tenant's rebuild.
   */
  public void throwIfFullRebuildCancelled() {
    TenantIndex index = existingState();
    if (index != null) {
      throwIfRebuildCancelled(index);
    }
  }

  /**
   * Builds a new (green) Lucene generation, then atomically cuts over the serving (blue) path.
   * Readers continue on blue until cutover. On failure or cancel, green is discarded and blue is
   * left serving.
   * <p>
   * Falls back to wipe-in-place {@link OpenMode#CREATE} on the serving directory when the filesystem cannot rename a
   * directory with open readers, and for unit-test owners backed by an in-memory directory.
   */
  public void rebuildExclusive(final WriterWork rebuild) {
    if (closed.get()) {
      throw new SearchIndexException("Lucene index writer is closed; cannot rebuild.",
          new IllegalStateException("Lucene index writer is closed."));
    }
    TenantIndex index = state();
    if (!index.fullRebuildInProgress.compareAndSet(false, true)) {
      throw new SearchIndexException("Lucene full rebuild is already in progress.",
          new IllegalStateException("Lucene full rebuild is already in progress."));
    }
    // The flag is deliberately not cleared here. A rebuild is scheduled as a task and starts some time after the
    // request that triggered it, so a cancel accepted in that gap arrives before this method runs; clearing on entry
    // would discard it and take the rebuild all the way to cutover. Callers only accept a cancel while a rebuild is
    // in progress or scheduled, and the finally below clears the flag once the rebuild it belongs to has ended.
    try {
      // A cancel that landed while this rebuild was still queued means the work is already unwanted. Checking before
      // any of it starts saves a full reindex, and on the in-place path it is what keeps the serving index from being
      // wiped for a rebuild nobody is waiting for.
      throwIfRebuildCancelled(index);
      if (usesBlueGreen()) {
        rebuildBlueGreen(index, rebuild);
      }
      else {
        rebuildInPlace(index, rebuild);
      }
    }
    finally {
      index.fullRebuildInProgress.set(false);
      index.cancelRequested.set(false);
    }
  }

  private void rebuildBlueGreen(final TenantIndex index, final WriterWork rebuild) {
    Path greenPath = null;
    Directory greenDirectory = null;
    IndexWriter greenWriter = null;
    Path retiredPath = null;
    boolean blueTornDown = false;
    boolean rotated = false;
    try {
      greenPath = luceneComponents.createBuildingGenerationDirectory();
      greenDirectory = luceneComponents.openSearchIndexAt(greenPath, false);
      greenWriter = openWriter(greenDirectory, OpenMode.CREATE);

      // Build green without holding the tenant lock so blue SearcherManager keeps serving.
      rebuild.run(greenWriter);
      throwIfRebuildCancelled(index);
      greenWriter.commit();
      // Each handle is dropped from its local before being closed, so a close that throws cannot leave the local set
      // and have the catch below close the same handle a second time.
      IndexWriter committedWriter = greenWriter;
      greenWriter = null;
      committedWriter.close();
      Directory builtDirectory = greenDirectory;
      greenDirectory = null;
      builtDirectory.close();

      index.lock.lock();
      try {
        // Must stay ahead of the teardown below. This is the last point a cancel costs nothing but the green tree;
        // once the SearcherManager is paused and blue is closed, aborting would leave search unavailable rather than
        // still serving the old generation, which is the guarantee cancel exists to provide.
        throwIfRebuildCancelled(index);
        quiesceBlue(index, "cutover");
        try {
          closeDirectory(index);
        }
        catch (IOException e) {
          log.warn("Unable to close Lucene blue directory before cutover", e);
          index.directory = null;
        }
        blueTornDown = true;

        retiredPath = luceneComponents.cutoverBuildingGeneration(greenPath);
        greenPath = null;
        rotated = true;

        index.directory = luceneComponents.openSearchIndex(false);
        index.writer = openWriter(index.directory, OpenMode.CREATE_OR_APPEND);
        index.available = true;
        if (index.searcherManagerHolder != null) {
          index.searcherManagerHolder.reopen(index.writer);
        }
      }
      finally {
        index.lock.unlock();
      }

      if (retiredPath != null) {
        try {
          luceneComponents.deleteIndexGeneration(retiredPath);
        }
        catch (IOException e) {
          log.warn("Unable to delete retired Lucene index generation at {}", retiredPath, e);
        }
      }
    }
    catch (Exception e) {
      if (!rotated) {
        closeQuietly(greenWriter, greenDirectory);
        if (greenPath != null) {
          try {
            luceneComponents.deleteIndexGeneration(greenPath);
          }
          catch (IOException deleteFailure) {
            log.warn("Unable to delete incomplete Lucene green generation at {}", greenPath, deleteFailure);
          }
        }
        if (blueTornDown) {
          // Cutover itself failed after blue's writer and directory were already closed. Without
          // this the tenant keeps available=true with a null writer, and reads silently degrade to
          // uncached DirectoryReader.open until some later write happens to reopen it.
          closeQuietly(index);
        }
      }
      else {
        // Cutover already replaced serving; reopen failed. Close any half-open serving handles
        // so we do not leak Directory/writer for the life of the process.
        closeQuietly(index);
      }
      throw new SearchIndexException("Error rebuilding Lucene search index", e);
    }
  }

  /**
   * Wipes the serving directory and rebuilds into it under the tenant lock, leaving search unavailable until the
   * rebuild finishes. Used where blue/green cannot be: filesystems that refuse to rename a directory with open
   * readers, and in-memory test owners. Cancel cannot preserve the serving index here, because the wipe has already
   * happened by the time the rebuild work runs.
   */
  private void rebuildInPlace(final TenantIndex index, final WriterWork rebuild) {
    index.lock.lock();
    try {
      try {
        quiesceBlue(index, "rebuild");
        if (index.directory == null) {
          throw new SearchIndexException("Lucene directory is not available for rebuild",
              new IllegalStateException("Lucene directory is null"));
        }
        index.writer = openWriter(index.directory, OpenMode.CREATE);
        index.available = true;
        rebuild.run(index.writer);
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

  /**
   * Brings the serving (blue) generation to rest: buffered work is committed, the SearcherManager stops handing out
   * searchers, and the writer is closed. Each step is best-effort, since none of them is a reason to abandon a rebuild
   * that is about to replace this generation.
   * <p>
   * The order is load-bearing: the SearcherManager is paused before the writer closes, so nothing opens a searcher
   * against a writer that is going away. Both rebuild paths quiesce identically, and only the blue/green path goes on
   * to close the directory as well — the in-place path rebuilds into the directory it already holds.
   *
   * @param phase what the caller is quiescing for, used in the log messages
   */
  private void quiesceBlue(final TenantIndex index, final String phase) {
    try {
      if (index.writer != null && index.writer.isOpen()) {
        index.writer.flush();
        index.writer.commit();
      }
    }
    catch (IOException | RuntimeException e) {
      log.warn("Unable to commit Lucene serving writer before {}", phase, e);
    }
    try {
      pauseSearcherManagerHolder(index);
    }
    catch (IOException e) {
      log.warn("Unable to pause Lucene SearcherManager before {}", phase, e);
    }
    try {
      closeWriter(index);
    }
    catch (IOException e) {
      log.warn("Unable to close Lucene serving writer before {}", phase, e);
      index.writer = null;
    }
  }

  private void throwIfRebuildCancelled(final TenantIndex index) {
    if (index.cancelRequested.get()) {
      throw new SearchIndexException("Lucene full rebuild was cancelled; serving index unchanged.",
          new IllegalStateException("Lucene full rebuild was cancelled."));
    }
  }

  private void closeQuietly(final IndexWriter writer, final Directory directory) {
    if (writer != null) {
      try {
        writer.close();
      }
      catch (IOException e) {
        log.warn("Unable to close Lucene green writer after failed rebuild", e);
      }
    }
    if (directory != null) {
      try {
        directory.close();
      }
      catch (IOException e) {
        log.warn("Unable to close Lucene green directory after failed rebuild", e);
      }
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

    final AtomicBoolean fullRebuildInProgress = new AtomicBoolean();

    final AtomicBoolean cancelRequested = new AtomicBoolean();

    Directory directory;

    IndexWriter writer;

    volatile LuceneSearcherManagerHolder searcherManagerHolder;

    volatile boolean available = true;
  }
}
