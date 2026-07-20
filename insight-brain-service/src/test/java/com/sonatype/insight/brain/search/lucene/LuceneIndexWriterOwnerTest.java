/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.FilterDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LuceneIndexWriterOwnerTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @After
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

      LuceneIndexWriterOwner owner = new LuceneIndexWriterOwner(luceneComponents, mock(ShutdownHandler.class));
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
  public void rebuildExclusive_recoversWhenIndexDirectoryWasDeletedUnderWriter() throws Exception {
    Path indexPath = temporaryFolder.newFolder("deleted-under-writer").toPath();
    AtomicInteger openCount = new AtomicInteger();
    LuceneComponents luceneComponents = mock(LuceneComponents.class);
    when(luceneComponents.newAnalyzerForSearch()).thenReturn(new LowerCaseKeywordAnalyzer());
    when(luceneComponents.openSearchIndex(false)).thenAnswer(invocation -> {
      openCount.incrementAndGet();
      Files.createDirectories(indexPath);
      return FSDirectory.open(indexPath);
    });

    LuceneIndexWriterOwner owner = new LuceneIndexWriterOwner(luceneComponents, mock(ShutdownHandler.class));
    try {
      owner.setSearcherManagerHolder(mock(LuceneSearcherManagerHolder.class));

      // Simulate ApplicationsListTestSupport.runWithoutSearchIndex / work-dir wipe while writer is live.
      try (var paths = Files.walk(indexPath)) {
        paths.sorted(Comparator.reverseOrder()).forEach(path -> {
          try {
            Files.deleteIfExists(path);
          }
          catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
      }

      owner.rebuildExclusive(() -> {
        try {
          Document document = new Document();
          document.add(new StringField("id", "recovered", Store.YES));
          owner.getWriter().addDocument(document);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

      assertThat(owner.getWriter().isOpen()).isTrue();
      assertThat(openCount.get()).isGreaterThanOrEqualTo(2);
    }
    finally {
      owner.close();
    }
  }

  @Test
  public void rebuildExclusive_flushesCommitsPausesManagerThenClosesWriterBeforeReplace() throws Exception {
    List<String> order = new ArrayList<>();
    Directory rebuildDirectory = new ByteBuffersDirectory();
    LuceneComponents luceneComponents = mock(LuceneComponents.class);
    when(luceneComponents.newAnalyzerForSearch()).thenReturn(new LowerCaseKeywordAnalyzer());
    when(luceneComponents.openSearchIndex(false)).thenReturn(new ByteBuffersDirectory(), rebuildDirectory);

    LuceneIndexWriterOwner owner = new LuceneIndexWriterOwner(luceneComponents, mock(ShutdownHandler.class));
    try {
      IndexWriter oldWriter = mock(IndexWriter.class);
      when(oldWriter.isOpen()).thenReturn(true);
      doAnswer(invocation -> {
        order.add("flush");
        return null;
      }).when(oldWriter).flush();
      doAnswer(invocation -> {
        order.add("commit");
        return null;
      }).when(oldWriter).commit();
      doAnswer(invocation -> {
        order.add("writer");
        return null;
      }).when(oldWriter).close();

      LuceneSearcherManagerHolder searcherManagerHolder = mock(LuceneSearcherManagerHolder.class);
      doAnswer(invocation -> {
        order.add("manager");
        return null;
      }).when(searcherManagerHolder).pause();
      doAnswer(invocation -> {
        order.add("reopen");
        return null;
      }).when(searcherManagerHolder).reopen(org.mockito.ArgumentMatchers.any(IndexWriter.class));

      // Close the eagerly opened real writer/directory, then inject mocks for order verification.
      owner.getWriter().close();
      LuceneIndexWriterOwner.TenantIndex index = owner.currentIndexForTest();
      setField(index, "writer", oldWriter);
      setField(index, "directory", new ByteBuffersDirectory());
      owner.setSearcherManagerHolder(searcherManagerHolder);

      owner.rebuildExclusive(() -> order.add("rebuild"));

      assertThat(order).containsExactly("flush", "commit", "manager", "writer", "rebuild", "reopen");
    }
    finally {
      owner.close();
      rebuildDirectory.close();
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
      Thread rebuildThread = new Thread(() -> owner.rebuildExclusive(() -> {
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

    LuceneIndexWriterOwner owner = new LuceneIndexWriterOwner(luceneComponents, mock(ShutdownHandler.class));
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
