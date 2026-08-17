/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.Test;

import static org.apache.lucene.document.Field.Store.YES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LuceneSearcherManagerHolderTest
{
  @Test
  public void releaseAfterReopen_usesOriginatingSearcherManager() throws Exception {
    try (Directory firstDirectory = new ByteBuffersDirectory();
        Directory secondDirectory = new ByteBuffersDirectory();
        Analyzer firstAnalyzer = new LowerCaseKeywordAnalyzer();
        Analyzer secondAnalyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter firstWriter = new IndexWriter(firstDirectory, new IndexWriterConfig(firstAnalyzer));
        IndexWriter secondWriter = new IndexWriter(secondDirectory, new IndexWriterConfig(secondAnalyzer));
        LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(firstWriter, Clock.systemUTC(), false))
    {
      firstWriter.addDocument(document("first"));
      firstWriter.commit();
      IndexSearcher firstSearcher = holder.acquire();

      secondWriter.addDocument(document("second"));
      secondWriter.commit();
      holder.reopen(secondWriter);

      holder.release(firstSearcher);
    }
  }

  @Test
  public void release_handlesDuplicateAcquiresOfSameSearcher() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer));
        LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, Clock.systemUTC(), false))
    {
      writer.addDocument(document("value"));
      writer.commit();

      IndexSearcher firstSearcher = holder.acquire();
      IndexSearcher secondSearcher = holder.acquire();
      try {
        assertThat(secondSearcher).isSameAs(firstSearcher);
      }
      finally {
        holder.release(firstSearcher);
        holder.release(secondSearcher);
      }
    }
  }

  @Test
  public void constructor_registersAcquireWaitAndRefreshLagMetrics() throws Exception {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    Clock clock = Clock.fixed(Instant.ofEpochMilli(1_000L), ZoneOffset.UTC);

    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer));
        LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, clock, false, meterRegistry))
    {
      IndexSearcher searcher = holder.acquire();
      holder.release(searcher);

      assertThat(meterRegistry.find("search.lucene.searcher.acquire.wait").timer()).isNotNull();
      assertThat(meterRegistry.find("search.lucene.searcher.refresh.lag").gauge()).isNotNull();
      assertThat(meterRegistry.get("search.lucene.searcher.acquire.wait").timer().count()).isEqualTo(1L);
      assertThat(meterRegistry.get("search.lucene.searcher.acquire.wait").timer().totalTime(TimeUnit.NANOSECONDS))
          .isGreaterThanOrEqualTo(0.0);
      assertThat(meterRegistry.get("search.lucene.searcher.refresh.lag").gauge().value()).isEqualTo(0.0);
    }
  }

  @Test
  public void acquire_managerClosedDuringAcquire_throwsSearcherManagerUnavailable() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer));
        LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, Clock.systemUTC(), false))
    {
      writer.addDocument(document("value"));
      writer.commit();

      closeManagerBehindHoldersBack(holder);

      assertThatThrownBy(holder::acquire)
          .isInstanceOf(SearcherManagerUnavailableException.class)
          .hasCauseInstanceOf(AlreadyClosedException.class);
    }
  }

  private static void closeManagerBehindHoldersBack(final LuceneSearcherManagerHolder holder) throws Exception {
    Field field = LuceneSearcherManagerHolder.class.getDeclaredField("searcherManager");
    field.setAccessible(true);
    ((SearcherManager) field.get(holder)).close();
  }

  private Document document(final String value) {
    Document document = new Document();
    document.add(new StringField("value", value, YES));
    return document;
  }
}
