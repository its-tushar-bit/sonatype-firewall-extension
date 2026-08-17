/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.session;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.micrometer.core.instrument.MeterRegistry;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.brain.search.lucene.LowerCaseKeywordAnalyzer;
import com.sonatype.insight.brain.search.lucene.LuceneIndexWriterOwner;
import com.sonatype.insight.brain.search.lucene.LuceneSearcherManagerHolder;
import com.sonatype.insight.brain.search.lucene.SearcherManagerUnavailableException;
import com.sonatype.insight.brain.security.CurrentUser;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.ORGANIZATION_ID;
import static org.apache.lucene.document.Field.Store.YES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IndexReadSessionFactoryTest
{
  private UserPrincipal principal;

  private CurrentUser currentUser;

  private ReadableContextAuthzCache authzCache;

  @BeforeEach
  public void setUp() {
    principal = new UserPrincipal("u", "User", "default", Set.of());
    currentUser = mock(CurrentUser.class);
    authzCache = mock(ReadableContextAuthzCache.class);
    when(currentUser.getUserPrincipal()).thenReturn(principal);
    when(authzCache.compiledRbacFilter(principal)).thenReturn(new MatchAllDocsQuery());
  }

  @Test
  public void open_injectsFailClosedFilterForRestrictedUser() throws Exception {
    UserPrincipal restrictedPrincipal = new UserPrincipal("restricted", "Restricted User", "default", Set.of());
    when(currentUser.getUserPrincipal()).thenReturn(restrictedPrincipal);
    when(authzCache.compiledRbacFilter(restrictedPrincipal)).thenReturn(new MatchNoDocsQuery());

    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(document("visible-app", "visible-org"));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexReadSessionFactory factory = IndexReadSessionFactory.forTest(holder, currentUser, authzCache);

        try (IndexReadSession session = factory.open()) {
          assertThat(session.count(new MatchAllDocsQuery())).isZero();
        }
      }
    }
  }

  @Test
  public void open_doesNotBlockWhileWriteLockHeld() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        LuceneIndexWriterOwner owner = LuceneIndexWriterOwner.openForTest(directory, analyzer))
    {
      owner.runWithWriter(writer -> writer.addDocument(document("visible-app", "visible-org")));
      owner.setSearcherManagerHolder(new LuceneSearcherManagerHolder(owner.getWriter(), (MeterRegistry) null));

      IndexReadSessionFactory factory =
          IndexReadSessionFactory.forProduction(owner, currentUser, authzCache, null, null);

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

      AtomicReference<IndexReadSession> opened = new AtomicReference<>();
      CountDownLatch openCompleted = new CountDownLatch(1);
      Thread readerThread = new Thread(() -> {
        opened.set(factory.open());
        openCompleted.countDown();
      });
      readerThread.start();
      boolean completedWhileLockHeld = openCompleted.await(10, TimeUnit.SECONDS);

      releaseWriteLock.countDown();
      writerThread.join(30_000L);
      readerThread.join(30_000L);

      assertThat(completedWhileLockHeld)
          .as("Lucene IndexReadSession open must not block on the write lock during incremental indexing")
          .isTrue();
      try (IndexReadSession session = opened.get()) {
        assertThat(session).isNotNull();
        assertThat(session.count(new MatchAllDocsQuery())).isEqualTo(1L);
      }
    }
  }

  @Test
  public void open_fallsBackToBlockingHolderWhenLockFreeReadIsEmpty() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(document("visible-app", "visible-org"));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        LuceneIndexWriterOwner owner = mock(LuceneIndexWriterOwner.class);
        when(owner.getSearcherManagerHolderIfUsable()).thenReturn(Optional.empty());
        when(owner.getSearcherManagerHolder()).thenReturn(holder);

        IndexReadSessionFactory factory =
            IndexReadSessionFactory.forProduction(owner, currentUser, authzCache, null, null);

        try (IndexReadSession session = factory.open()) {
          assertThat(session.count(new MatchAllDocsQuery())).isEqualTo(1L);
        }
      }
    }
  }

  @Test
  public void open_retriesViaBlockingHolderWhenLockFreeHolderPausedBeforeAcquire() throws Exception {
    try (Directory pausedDirectory = new ByteBuffersDirectory();
        Directory usableDirectory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter pausedWriter = new IndexWriter(pausedDirectory, new IndexWriterConfig(analyzer));
        IndexWriter usableWriter = new IndexWriter(usableDirectory, new IndexWriterConfig(analyzer)))
    {
      usableWriter.addDocument(document("visible-app", "visible-org"));
      usableWriter.commit();

      LuceneSearcherManagerHolder pausedHolder = new LuceneSearcherManagerHolder(pausedWriter, (MeterRegistry) null);
      pausedHolder.pause();
      LuceneSearcherManagerHolder usableHolder = new LuceneSearcherManagerHolder(usableWriter, (MeterRegistry) null);

      try (pausedHolder; usableHolder) {
        LuceneIndexWriterOwner owner = mock(LuceneIndexWriterOwner.class);
        // Paused holder from the lock-free read stands in for the check-then-pause cutover race.
        when(owner.getSearcherManagerHolderIfUsable()).thenReturn(Optional.of(pausedHolder));
        when(owner.getSearcherManagerHolder()).thenReturn(usableHolder);

        IndexReadSessionFactory factory =
            IndexReadSessionFactory.forProduction(owner, currentUser, authzCache, null, null);

        try (IndexReadSession session = factory.open()) {
          assertThat(session.count(new MatchAllDocsQuery())).isEqualTo(1L);
        }
      }
    }
  }

  @Test
  public void open_surfacesExceptionWhenSuppliedHolderPausedWithNoOwnerToRetry() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer));
        LuceneSearcherManagerHolder pausedHolder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null))
    {
      pausedHolder.pause();
      IndexReadSessionFactory factory = IndexReadSessionFactory.forTest(pausedHolder, currentUser, authzCache);

      assertThatThrownBy(factory::open)
          .isInstanceOf(SearchIndexException.class)
          .hasCauseInstanceOf(SearcherManagerUnavailableException.class);
    }
  }

  @Test
  public void open_retriesAtMostOnceWhenBlockingHolderAlsoPaused() throws Exception {
    try (Directory firstDirectory = new ByteBuffersDirectory();
        Directory secondDirectory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter firstWriter = new IndexWriter(firstDirectory, new IndexWriterConfig(analyzer));
        IndexWriter secondWriter = new IndexWriter(secondDirectory, new IndexWriterConfig(analyzer));
        LuceneSearcherManagerHolder lockFreeHolder = new LuceneSearcherManagerHolder(firstWriter, (MeterRegistry) null);
        LuceneSearcherManagerHolder blockingHolder =
            new LuceneSearcherManagerHolder(secondWriter, (MeterRegistry) null))
    {
      lockFreeHolder.pause();
      blockingHolder.pause();

      LuceneIndexWriterOwner owner = mock(LuceneIndexWriterOwner.class);
      when(owner.getSearcherManagerHolderIfUsable()).thenReturn(Optional.of(lockFreeHolder));
      when(owner.getSearcherManagerHolder()).thenReturn(blockingHolder);

      IndexReadSessionFactory factory =
          IndexReadSessionFactory.forProduction(owner, currentUser, authzCache, null, null);

      assertThatThrownBy(factory::open).isInstanceOf(SearchIndexException.class);
      verify(owner, times(1)).getSearcherManagerHolder();
    }
  }

  private Document document(final String applicationId, final String organizationId) {
    Document document = new Document();
    document.add(new StringField(APPLICATION_ID.label, applicationId, YES));
    document.add(new StringField(ORGANIZATION_ID.label, organizationId, YES));
    return document;
  }
}
