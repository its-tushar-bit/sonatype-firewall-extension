/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.FilterDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import com.sonatype.insight.brain.testsupport.TempFolder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class LuceneIndexWriterOwnerTest
{
  @RegisterExtension
  public TempFolder temporaryFolder = new TempFolder();

  @AfterEach
  public void resetTenant() {
    TenantTestHelper.resetAfterTest();
  }

  @Test
  public void updateTwice_reusesSameWriterInstance() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        LuceneIndexWriterOwner owner = LuceneIndexWriterOwner.openForTest(directory, analyzer))
    {
      IndexWriter first = owner.getWriter();
      owner.commitAndMaybeRefresh();

      IndexWriter second = owner.getWriter();

      assertThat(second).isSameAs(first);
    }
  }

  @Test
  public void constructor_opensWriterAndFailsFastWhenWriteLockIsHeld() throws Exception {
    Path indexPath = temporaryFolder.newFolder("index").toPath();
    LuceneComponents luceneComponents = mock(LuceneComponents.class);
    when(luceneComponents.newAnalyzerForSearch()).thenReturn(new LowerCaseKeywordAnalyzer());

    try (Directory lockDirectory = FSDirectory.open(indexPath);
        Directory ownerDirectory = FSDirectory.open(indexPath);
        Analyzer lockAnalyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter ignored = new IndexWriter(lockDirectory, new IndexWriterConfig(lockAnalyzer)))
    {
      when(luceneComponents.openSearchIndex(false)).thenReturn(ownerDirectory);

      LuceneIndexWriterOwner owner = newBlueGreenOwner(luceneComponents);
      try {
        assertThatThrownBy(owner::getWriter)
            .isInstanceOf(SearchIndexException.class)
            .hasRootCauseInstanceOf(LockObtainFailedException.class)
            .hasMessageContaining("Unable to open Lucene index writer");
      }
      finally {
        owner.close();
      }
    }
  }

  @Test
  public void close_flushesCommitsClosesManagerWriterThenDirectory() throws Exception {
    List<String> closeOrder = new ArrayList<>();
    Directory directory = new RecordingDirectory(closeOrder);
    try (Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        LuceneIndexWriterOwner owner = LuceneIndexWriterOwner.openForTest(directory, analyzer))
    {
      owner.getWriter().close();
      IndexWriter writer = mock(IndexWriter.class);
      when(writer.isOpen()).thenReturn(true);
      doAnswer(invocation -> {
        closeOrder.add("flush");
        return null;
      }).when(writer).flush();
      doAnswer(invocation -> {
        closeOrder.add("commit");
        return null;
      }).when(writer).commit();
      doAnswer(invocation -> {
        closeOrder.add("writer");
        return null;
      }).when(writer).close();

      LuceneSearcherManagerHolder searcherManagerHolder = mock(LuceneSearcherManagerHolder.class);
      doAnswer(invocation -> {
        closeOrder.add("manager");
        return null;
      }).when(searcherManagerHolder).close();

      LuceneIndexWriterOwner.TenantIndex index = owner.currentIndexForTest();
      setField(index, "writer", writer);
      owner.setSearcherManagerHolder(searcherManagerHolder);

      owner.close();

      assertThat(closeOrder).containsExactly("flush", "commit", "manager", "writer", "directory");
    }
  }

  @Test
  public void rebuildExclusive_keepsBlueSearcherAvailableDuringGreenBuild() throws Exception {
    LuceneComponents luceneComponents = newFsLuceneComponents("blue-green-serve");
    LuceneIndexWriterOwner owner = newBlueGreenOwner(luceneComponents);
    try {
      owner.runWithWriter(writer -> {
        Document document = new Document();
        document.add(new StringField("id", "blue-doc", Store.YES));
        writer.addDocument(document);
      });

      CountDownLatch greenStarted = new CountDownLatch(1);
      CountDownLatch releaseGreen = new CountDownLatch(1);
      AtomicBoolean sawBlueDuringRebuild = new AtomicBoolean();

      Thread rebuildThread = new Thread(() -> owner.rebuildExclusive(greenWriter -> {
        greenStarted.countDown();
        try {
          assertThat(releaseGreen.await(30, TimeUnit.SECONDS)).isTrue();
          Document document = new Document();
          document.add(new StringField("id", "green-doc", Store.YES));
          greenWriter.addDocument(document);
        }
        catch (Exception e) {
          throw new RuntimeException(e);
        }
      }));
      rebuildThread.start();
      assertThat(greenStarted.await(5, TimeUnit.SECONDS)).isTrue();

      Optional<LuceneSearcherManagerHolder> holder = owner.tryGetSearcherManagerHolder();
      assertThat(holder).isPresent();
      IndexSearcher searcher = holder.get().acquire();
      try {
        TopDocs hits = searcher.search(new TermQuery(new Term("id", "blue-doc")), 1);
        sawBlueDuringRebuild.set(hits.totalHits.value > 0);
      }
      finally {
        holder.get().release(searcher);
      }
      assertThat(sawBlueDuringRebuild).isTrue();
      assertThat(owner.isFullRebuildInProgress()).isTrue();

      releaseGreen.countDown();
      rebuildThread.join(30_000L);

      assertThat(countDocs(luceneComponents, "blue-doc")).isZero();
      assertThat(countDocs(luceneComponents, "green-doc")).isEqualTo(1);
      Path generations = searchDir("blue-green-serve").toPath().resolve("generations");
      if (Files.exists(generations)) {
        try (var paths = Files.list(generations)) {
          assertThat(paths.findAny()).isEmpty();
        }
      }
    }
    finally {
      owner.close();
    }
  }

  /**
   * The other failure tests all throw while green is still building, before blue is touched. This one fails the cutover
   * itself, after blue's writer and directory have already been closed, so the tenant must end up explicitly
   * unavailable rather than reporting available with a null writer.
   */
  @Test
  public void rebuildExclusive_cutoverFailureAfterBlueTeardownMarksIndexUnavailable() throws Exception {
    LuceneComponents luceneComponents = spy(newFsLuceneComponents("blue-green-cutover-fail"));
    LuceneIndexWriterOwner owner = newBlueGreenOwner(luceneComponents);
    try {
      owner.runWithWriter(writer -> {
        Document document = new Document();
        document.add(new StringField("id", "keep-me", Store.YES));
        writer.addDocument(document);
      });
      doThrow(new IOException("simulated cutover failure")).when(luceneComponents)
          .cutoverBuildingGeneration(any(Path.class));

      assertThatThrownBy(() -> owner.rebuildExclusive(greenWriter -> {
        Document document = new Document();
        document.add(new StringField("id", "green-doc", Store.YES));
        greenWriter.addDocument(document);
      })).isInstanceOf(SearchIndexException.class);

      assertThat(owner.tryGetSearcherManagerHolder()).isEmpty();
      assertThatThrownBy(owner::getWriter).isInstanceOf(SearchIndexException.class)
          .hasMessageContaining("unavailable");
      // Cutover never moved anything, so the previous index is still on disk and green is cleaned up.
      assertThat(countDocs(luceneComponents, "keep-me")).isEqualTo(1);
      assertThat(countDocs(luceneComponents, "green-doc")).isZero();
      Path generations = searchDir("blue-green-cutover-fail").toPath().resolve("generations");
      if (Files.exists(generations)) {
        try (var paths = Files.list(generations)) {
          assertThat(paths.findAny()).isEmpty();
        }
      }
    }
    finally {
      owner.close();
    }
  }

  /**
   * A process killed mid-rebuild leaves its green tree behind, and nothing else deletes it. Without the sweep those
   * trees accumulate across restarts until the search volume fills.
   */
  @Test
  public void rebuildExclusive_deletesGenerationsOrphanedByAnEarlierProcess() throws Exception {
    LuceneComponents luceneComponents = newFsLuceneComponents("blue-green-orphans");
    LuceneIndexWriterOwner owner = newBlueGreenOwner(luceneComponents);
    try {
      Path generations = searchDir("blue-green-orphans").toPath().resolve("generations");
      Path orphanedGreen = generations.resolve("11111111-1111-1111-1111-111111111111");
      Path orphanedRetired = generations.resolve("retired-22222222-2222-2222-2222-222222222222");
      Files.createDirectories(orphanedGreen);
      Files.createDirectories(orphanedRetired);
      Files.writeString(orphanedGreen.resolve("segments_1"), "stale");
      backdate(orphanedGreen);
      backdate(orphanedRetired);

      owner.rebuildExclusive(greenWriter -> {
        Document document = new Document();
        document.add(new StringField("id", "fresh-doc", Store.YES));
        greenWriter.addDocument(document);
      });

      assertThat(countDocs(luceneComponents, "fresh-doc")).isEqualTo(1);
      try (var paths = Files.list(generations)) {
        assertThat(paths.findAny()).isEmpty();
      }
    }
    finally {
      owner.close();
    }
  }

  /**
   * The rebuild flag that would prove a generation is dead is per-process, so where several nodes share the search
   * volume a peer's in-flight green tree is indistinguishable from an orphan. Recent modification is the only
   * available evidence that something is still writing, and deleting on that evidence would pull the index out from
   * under a peer mid-rebuild.
   */
  @Test
  public void rebuildExclusive_leavesRecentlyModifiedGenerationsAlone() throws Exception {
    LuceneComponents luceneComponents = newFsLuceneComponents("blue-green-peer");
    LuceneIndexWriterOwner owner = newBlueGreenOwner(luceneComponents);
    try {
      Path generations = searchDir("blue-green-peer").toPath().resolve("generations");
      Path peerGeneration = generations.resolve("33333333-3333-3333-3333-333333333333");
      Files.createDirectories(peerGeneration);
      Files.writeString(peerGeneration.resolve("segments_1"), "still being written by another node");

      owner.rebuildExclusive(greenWriter -> {
        Document document = new Document();
        document.add(new StringField("id", "fresh-doc", Store.YES));
        greenWriter.addDocument(document);
      });

      assertThat(countDocs(luceneComponents, "fresh-doc")).isEqualTo(1);
      assertThat(Files.readString(peerGeneration.resolve("segments_1")))
          .isEqualTo("still being written by another node");
    }
    finally {
      owner.close();
    }
  }

  /**
   * A cutover that fails on its final rename deliberately leaves the staged copy in place, because at that moment it
   * holds the only copy of the rebuilt index. The tree lands beside the serving index rather than under
   * {@code generations/}, so the generation sweep cannot see it and it would otherwise outlive every later rebuild.
   * The same age guard applies: a peer mid-cutover is staging into the same directory.
   */
  @Test
  public void rebuildExclusive_deletesAStagedCopyStrandedByAFailedCutover() throws Exception {
    LuceneComponents luceneComponents = newFsLuceneComponents("blue-green-staging");
    LuceneIndexWriterOwner owner = newBlueGreenOwner(luceneComponents);
    try {
      Path search = searchDir("blue-green-staging").toPath();
      Path stranded = search.resolve("index.incoming-44444444-4444-4444-4444-444444444444");
      Path peerStaging = search.resolve("index.incoming-55555555-5555-5555-5555-555555555555");
      Files.createDirectories(stranded);
      Files.createDirectories(peerStaging);
      Files.writeString(stranded.resolve("segments_1"), "stranded by a failed cutover");
      Files.writeString(peerStaging.resolve("segments_1"), "still being staged by another node");
      backdate(stranded);

      owner.rebuildExclusive(greenWriter -> {
        Document document = new Document();
        document.add(new StringField("id", "fresh-doc", Store.YES));
        greenWriter.addDocument(document);
      });

      assertThat(countDocs(luceneComponents, "fresh-doc")).isEqualTo(1);
      assertThat(stranded).doesNotExist();
      assertThat(Files.readString(peerStaging.resolve("segments_1")))
          .isEqualTo("still being staged by another node");
    }
    finally {
      owner.close();
    }
  }

  /**
   * A rebuild runs as a scheduled task, so a cancel routinely arrives before the task starts. The request has to
   * survive that gap, and the rebuild it belongs to must then abort without spending the reindex or touching what is
   * serving.
   */
  @Test
  public void rebuildExclusive_honoursACancelRequestedBeforeTheRebuildStarts() throws Exception {
    LuceneComponents luceneComponents = newFsLuceneComponents("blue-green-cancel-early");
    LuceneIndexWriterOwner owner = newBlueGreenOwner(luceneComponents);
    try {
      owner.runWithWriter(writer -> {
        Document document = new Document();
        document.add(new StringField("id", "blue-doc", Store.YES));
        writer.addDocument(document);
      });
      AtomicBoolean rebuildRan = new AtomicBoolean();

      owner.requestCancelFullRebuild();

      assertThatThrownBy(() -> owner.rebuildExclusive(greenWriter -> {
        rebuildRan.set(true);
        Document document = new Document();
        document.add(new StringField("id", "green-doc", Store.YES));
        greenWriter.addDocument(document);
      })).isInstanceOf(SearchIndexException.class).hasMessageContaining("cancelled");

      assertThat(rebuildRan).isFalse();
      assertThat(countDocs(luceneComponents, "blue-doc")).isEqualTo(1);
      assertThat(countDocs(luceneComponents, "green-doc")).isZero();
    }
    finally {
      owner.close();
    }
  }

  /**
   * The cancel request belongs to one rebuild. Once that rebuild has ended the request must be spent, or the next
   * rebuild aborts for a cancel nobody asked for.
   */
  @Test
  public void rebuildExclusive_doesNotCarryACancelIntoTheFollowingRebuild() throws Exception {
    LuceneComponents luceneComponents = newFsLuceneComponents("blue-green-cancel-once");
    LuceneIndexWriterOwner owner = newBlueGreenOwner(luceneComponents);
    try {
      owner.runWithWriter(writer -> writer.addDocument(new Document()));

      owner.requestCancelFullRebuild();
      assertThatThrownBy(() -> owner.rebuildExclusive(greenWriter -> {
      })).isInstanceOf(SearchIndexException.class);

      owner.rebuildExclusive(greenWriter -> {
        Document document = new Document();
        document.add(new StringField("id", "second-rebuild-doc", Store.YES));
        greenWriter.addDocument(document);
      });

      assertThat(countDocs(luceneComponents, "second-rebuild-doc")).isEqualTo(1);
    }
    finally {
      owner.close();
    }
  }

  /**
   * Windows refuses to rename a directory a live searcher still holds open, so the cutover cannot work there. Those
   * deployments keep the in-place rebuild they had before blue/green rather than failing the rebuild outright.
   */
  @Test
  public void rebuildExclusive_rebuildsInPlaceWhenTheFilesystemCannotRenameOpenDirectories() throws Exception {
    LuceneComponents luceneComponents = newFsLuceneComponents("no-rename-fallback");
    LuceneIndexWriterOwner owner =
        new LuceneIndexWriterOwner(luceneComponents, mock(ShutdownHandler.class), null, false);
    try {
      owner.runWithWriter(writer -> {
        Document document = new Document();
        document.add(new StringField("id", "old-doc", Store.YES));
        writer.addDocument(document);
      });

      owner.rebuildExclusive(greenWriter -> {
        Document document = new Document();
        document.add(new StringField("id", "new-doc", Store.YES));
        greenWriter.addDocument(document);
      });

      assertThat(countDocs(luceneComponents, "new-doc")).isEqualTo(1);
      assertThat(countDocs(luceneComponents, "old-doc")).isZero();
      assertThat(owner.getWriter().isOpen()).isTrue();
      assertThat(owner.tryGetSearcherManagerHolder()).isPresent();
      // The rebuild wiped and refilled the serving directory, so no generation was ever created.
      assertThat(Files.exists(searchDir("no-rename-fallback").toPath().resolve("generations"))).isFalse();
    }
    finally {
      owner.close();
    }
  }

  @Test
  public void rebuildExclusive_failureDeletesGreenAndKeepsBlueDocuments() throws Exception {
    LuceneComponents luceneComponents = newFsLuceneComponents("blue-green-fail");
    LuceneIndexWriterOwner owner = newBlueGreenOwner(luceneComponents);
    try {
      owner.runWithWriter(writer -> {
        Document document = new Document();
        document.add(new StringField("id", "keep-me", Store.YES));
        writer.addDocument(document);
      });

      assertThatThrownBy(() -> owner.rebuildExclusive(greenWriter -> {
        Document document = new Document();
        document.add(new StringField("id", "doomed", Store.YES));
        greenWriter.addDocument(document);
        throw new IOException("simulated rebuild failure");
      })).isInstanceOf(SearchIndexException.class);

      assertThat(countDocs(luceneComponents, "keep-me")).isEqualTo(1);
      assertThat(countDocs(luceneComponents, "doomed")).isZero();
      assertThat(owner.getWriter().isOpen()).isTrue();
      Path generations = searchDir("blue-green-fail").toPath().resolve("generations");
      if (Files.exists(generations)) {
        try (var paths = Files.list(generations)) {
          assertThat(paths.findAny()).isEmpty();
        }
      }
    }
    finally {
      owner.close();
    }
  }

  @Test
  public void rebuildExclusive_cancelBeforeCutoverKeepsBlueDocuments() throws Exception {
    LuceneComponents luceneComponents = newFsLuceneComponents("blue-green-cancel");
    LuceneIndexWriterOwner owner = newBlueGreenOwner(luceneComponents);
    try {
      owner.runWithWriter(writer -> {
        Document document = new Document();
        document.add(new StringField("id", "stay-blue", Store.YES));
        writer.addDocument(document);
      });

      assertThatThrownBy(() -> owner.rebuildExclusive(greenWriter -> {
        Document document = new Document();
        document.add(new StringField("id", "cancelled-green", Store.YES));
        greenWriter.addDocument(document);
        owner.requestCancelFullRebuild();
      })).isInstanceOf(SearchIndexException.class)
          .hasRootCauseMessage("Lucene full rebuild was cancelled.");

      assertThat(countDocs(luceneComponents, "stay-blue")).isEqualTo(1);
      assertThat(countDocs(luceneComponents, "cancelled-green")).isZero();
    }
    finally {
      owner.close();
    }
  }

  @Test
  public void runWithWriter_skipsWhileFullRebuildInProgress() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        LuceneIndexWriterOwner owner = LuceneIndexWriterOwner.openForTest(directory, analyzer))
    {
      CountDownLatch rebuildStarted = new CountDownLatch(1);
      CountDownLatch releaseRebuild = new CountDownLatch(1);
      AtomicInteger incrementalWrites = new AtomicInteger();

      Thread rebuildThread = new Thread(() -> owner.rebuildExclusive(writer -> {
        rebuildStarted.countDown();
        try {
          assertThat(releaseRebuild.await(30, TimeUnit.SECONDS)).isTrue();
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }));
      rebuildThread.start();
      assertThat(rebuildStarted.await(5, TimeUnit.SECONDS)).isTrue();

      owner.runWithWriter(writer -> incrementalWrites.incrementAndGet());
      assertThat(incrementalWrites.get()).isZero();

      releaseRebuild.countDown();
      rebuildThread.join(30_000L);
    }
  }

  @Test
  public void rebuildExclusive_recoversWhenIndexDirectoryWasDeletedUnderWriter() throws Exception {
    LuceneComponents luceneComponents = newFsLuceneComponents("deleted-under-writer");
    LuceneIndexWriterOwner owner = newBlueGreenOwner(luceneComponents);
    try {
      owner.runWithWriter(writer -> {
        Document document = new Document();
        document.add(new StringField("id", "before-wipe", Store.YES));
        writer.addDocument(document);
      });

      Path indexPath = searchDir("deleted-under-writer").toPath().resolve("index");
      try (var paths = Files.walk(indexPath)) {
        paths.sorted(Comparator.reverseOrder()).forEach(path -> {
          try {
            if (!path.equals(indexPath)) {
              Files.deleteIfExists(path);
            }
          }
          catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
      }

      owner.rebuildExclusive(writer -> {
        Document document = new Document();
        document.add(new StringField("id", "recovered", Store.YES));
        writer.addDocument(document);
      });

      assertThat(owner.getWriter().isOpen()).isTrue();
      assertThat(countDocs(luceneComponents, "recovered")).isEqualTo(1);
    }
    finally {
      owner.close();
    }
  }

  @Test
  public void tryGetSearcherManagerHolder_returnsEmptyQuicklyWhileRebuildHoldsLock() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        LuceneIndexWriterOwner owner = LuceneIndexWriterOwner.openForTest(directory, analyzer))
    {
      LuceneSearcherManagerHolder searcherManagerHolder = mock(LuceneSearcherManagerHolder.class);
      when(searcherManagerHolder.isUsable()).thenReturn(true);
      owner.setSearcherManagerHolder(searcherManagerHolder);

      CountDownLatch rebuildStarted = new CountDownLatch(1);
      CountDownLatch releaseRebuild = new CountDownLatch(1);
      Thread rebuildThread = new Thread(() -> owner.rebuildExclusive(greenWriter -> {
        rebuildStarted.countDown();
        try {
          assertThat(releaseRebuild.await(30, TimeUnit.SECONDS)).isTrue();
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }));
      rebuildThread.start();

      assertThat(rebuildStarted.await(5, TimeUnit.SECONDS)).isTrue();

      long startNanos = System.nanoTime();
      Optional<LuceneSearcherManagerHolder> result = owner.tryGetSearcherManagerHolder();
      long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000L;

      assertThat(result).isEmpty();
      assertThat(elapsedMillis).isLessThan(500L);

      releaseRebuild.countDown();
      rebuildThread.join(30_000L);
    }
  }

  @Test
  public void getSearcherManagerHolderIfUsable_returnsHolderWithoutBlockingWhileWriteLockHeld() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        LuceneIndexWriterOwner owner = LuceneIndexWriterOwner.openForTest(directory, analyzer))
    {
      LuceneSearcherManagerHolder searcherManagerHolder = mock(LuceneSearcherManagerHolder.class);
      when(searcherManagerHolder.isUsable()).thenReturn(true);
      owner.setSearcherManagerHolder(searcherManagerHolder);

      CountDownLatch writeLockHeld = new CountDownLatch(1);
      CountDownLatch releaseWriteLock = new CountDownLatch(1);
      Thread writerThread = new Thread(() -> {
        try {
          owner.runWithWriter(writer -> {
            writeLockHeld.countDown();
            assertThat(releaseWriteLock.await(30, TimeUnit.SECONDS)).isTrue();
          });
        }
        catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
      writerThread.start();
      assertThat(writeLockHeld.await(5, TimeUnit.SECONDS)).isTrue();

      AtomicReference<Optional<LuceneSearcherManagerHolder>> result = new AtomicReference<>(Optional.empty());
      CountDownLatch readCompleted = new CountDownLatch(1);
      Thread readerThread = new Thread(() -> {
        result.set(owner.getSearcherManagerHolderIfUsable());
        readCompleted.countDown();
      });
      readerThread.start();
      boolean completedWhileLockHeld = readCompleted.await(10, TimeUnit.SECONDS);

      releaseWriteLock.countDown();
      writerThread.join(30_000L);
      readerThread.join(30_000L);

      assertThat(completedWhileLockHeld)
          .as("session read must not block on the write lock while incremental indexing holds it")
          .isTrue();
      assertThat(result.get()).contains(searcherManagerHolder);
    }
  }

  @Test
  public void getSearcherManagerHolderIfUsable_returnsEmptyWhenHolderNotUsable() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        LuceneIndexWriterOwner owner = LuceneIndexWriterOwner.openForTest(directory, analyzer))
    {
      LuceneSearcherManagerHolder searcherManagerHolder = mock(LuceneSearcherManagerHolder.class);
      when(searcherManagerHolder.isUsable()).thenReturn(false);
      owner.setSearcherManagerHolder(searcherManagerHolder);

      assertThat(owner.getSearcherManagerHolderIfUsable()).isEmpty();
    }
  }

  @Test
  public void getSearcherManagerHolderIfUsable_returnsEmptyWhenIndexUnavailable() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        LuceneIndexWriterOwner owner = LuceneIndexWriterOwner.openForTest(directory, analyzer))
    {
      LuceneSearcherManagerHolder searcherManagerHolder = mock(LuceneSearcherManagerHolder.class);
      when(searcherManagerHolder.isUsable()).thenReturn(true);
      owner.setSearcherManagerHolder(searcherManagerHolder);
      setField(owner.currentIndexForTest(), "available", false);

      assertThat(owner.getSearcherManagerHolderIfUsable()).isEmpty();
    }
  }

  @Test
  public void getSearcherManagerHolder_isIsolatedPerTenant() throws Exception {
    TenantTestHelper.initMultiTenantMode();
    Path tenant1Path = temporaryFolder.newFolder("tenant1-index").toPath();
    Path tenant2Path = temporaryFolder.newFolder("tenant2-index").toPath();
    LuceneComponents luceneComponents = mock(LuceneComponents.class);
    when(luceneComponents.newAnalyzerForSearch()).thenReturn(new LowerCaseKeywordAnalyzer());
    when(luceneComponents.openSearchIndex(false)).thenAnswer(invocation -> {
      String slug = TenantThreadLocal.getTenant().tenantSlug;
      Path path = slug.contains("tenant1") ? tenant1Path : tenant2Path;
      Files.createDirectories(path);
      return FSDirectory.open(path);
    });

    LuceneIndexWriterOwner owner = newBlueGreenOwner(luceneComponents);
    AtomicReference<LuceneSearcherManagerHolder> tenant1Holder = new AtomicReference<>();
    AtomicReference<LuceneSearcherManagerHolder> tenant2Holder = new AtomicReference<>();
    try {
      Tenant tenant1 = TenantTestHelper.testAsNewTenant("lucene-owner-tenant1", tenant -> {
        tenant1Holder.set(owner.getSearcherManagerHolder());
        assertThat(owner.getWriter().isOpen()).isTrue();
      });
      Tenant tenant2 = TenantTestHelper.testAsNewTenant("lucene-owner-tenant2", tenant -> {
        tenant2Holder.set(owner.getSearcherManagerHolder());
        assertThat(owner.getWriter().isOpen()).isTrue();
      });

      assertThat(tenant1Holder.get()).isNotNull();
      assertThat(tenant2Holder.get()).isNotNull();
      assertThat(tenant1Holder.get()).isNotSameAs(tenant2Holder.get());

      TenantTestHelper.testAsTenant(tenant1, tenant -> assertThat(owner.getSearcherManagerHolder())
          .isSameAs(tenant1Holder.get()));
      TenantTestHelper.testAsTenant(tenant2, tenant -> assertThat(owner.getSearcherManagerHolder())
          .isSameAs(tenant2Holder.get()));
    }
    finally {
      owner.close();
    }
  }

  private void setField(final Object target, final String fieldName, final Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private File searchDir(final String name) throws IOException {
    File dir = new File(temporaryFolder.getRoot(), name);
    Files.createDirectories(dir.toPath());
    return dir;
  }

  /**
   * Pins blue/green on rather than letting it be decided by the host filesystem, so these tests exercise the cutover
   * path on any platform.
   */
  private static LuceneIndexWriterOwner newBlueGreenOwner(final LuceneComponents luceneComponents) {
    return new LuceneIndexWriterOwner(luceneComponents, mock(ShutdownHandler.class), null, true);
  }

  /**
   * Ages a tree past the sweep's minimum, which the sweep measures from the newest entry anywhere beneath the
   * generation.
   */
  private static void backdate(final Path root) throws IOException {
    FileTime aged = FileTime.from(Instant.now().minus(Duration.ofDays(3)));
    try (var paths = Files.walk(root)) {
      for (Path path : paths.toList()) {
        Files.setLastModifiedTime(path, aged);
      }
    }
  }

  private LuceneComponents newFsLuceneComponents(final String searchDirName) throws IOException {
    File search = searchDir(searchDirName);
    InsightWork insightWork = mock(InsightWork.class);
    when(insightWork.getSearchDir()).thenReturn(search);
    when(insightWork.getSearchIndexDir()).thenReturn(new File(search, "index"));
    when(insightWork.getSearchIndexGenerationsDir()).thenReturn(new File(search, "generations"));
    return new LuceneComponents(insightWork);
  }

  private static long countDocs(final LuceneComponents luceneComponents, final String id) throws IOException {
    try (Directory directory = luceneComponents.openSearchIndex(true)) {
      if (directory == null) {
        return 0L;
      }
      try (DirectoryReader reader = DirectoryReader.open(directory)) {
        return new IndexSearcher(reader).search(new TermQuery(new Term("id", id)), 1).totalHits.value;
      }
      catch (org.apache.lucene.index.IndexNotFoundException e) {
        return 0L;
      }
    }
  }

  @Test
  public void runWithWriter_commitsAndSignalsSearcherManager() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        LuceneIndexWriterOwner owner = LuceneIndexWriterOwner.openForTest(directory, analyzer))
    {
      LuceneSearcherManagerHolder searcherManagerHolder = mock(LuceneSearcherManagerHolder.class);
      owner.setSearcherManagerHolder(searcherManagerHolder);

      AtomicInteger writes = new AtomicInteger();
      owner.runWithWriter(writer -> {
        writer.addDocument(new Document());
        writes.incrementAndGet();
      });

      assertThat(writes.get()).isEqualTo(1);
      org.mockito.Mockito.verify(searcherManagerHolder).onCommitSignal();
    }
  }

  private static class RecordingDirectory
      extends FilterDirectory
  {
    private final List<String> closeOrder;

    RecordingDirectory(final List<String> closeOrder) {
      super(new ByteBuffersDirectory());
      this.closeOrder = closeOrder;
    }

    @Override
    public void close() throws IOException {
      closeOrder.add("directory");
      super.close();
    }
  }
}
