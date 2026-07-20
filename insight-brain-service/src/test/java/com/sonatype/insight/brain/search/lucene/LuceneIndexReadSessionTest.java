/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.junit.Test;

import com.sonatype.insight.brain.search.session.IndexPageRequest;
import com.sonatype.insight.brain.search.session.IndexPageResult;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;

import static org.apache.lucene.document.Field.Store.YES;
import static org.assertj.core.api.Assertions.assertThat;

public class LuceneIndexReadSessionTest
{
  @Test
  public void searchPage_collectsOneExtraHitForHasNextCursor() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(document("one"));
      writer.addDocument(document("two"));
      writer.addDocument(document("three"));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          IndexPageResult firstPage = session.searchPage(
              new IndexPageRequest(new MatchAllDocsQuery(), null, 2, List.of()));

          assertThat(firstPage.docs()).hasSize(2);
          assertThat(firstPage.hasNext()).isTrue();
          assertThat(firstPage.nextSearchAfter()).isNotEmpty();

          IndexPageResult secondPage = session.searchPage(
              new IndexPageRequest(new MatchAllDocsQuery(), null, 2, firstPage.nextSearchAfter()));

          assertThat(secondPage.docs()).hasSize(1);
          assertThat(secondPage.hasNext()).isFalse();
          assertThat(secondPage.nextSearchAfter()).isEmpty();
        }
      }
    }
  }

  @Test
  public void searchPage_sortedSearchAfterPreservesDocIdTieBreaker() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(document("one", "alpha"));
      writer.addDocument(document("two", "alpha"));
      writer.addDocument(document("three", "alpha"));
      writer.addDocument(document("four", "beta"));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          Sort sort = new Sort(new SortField("sortKey", SortField.Type.STRING));
          IndexPageResult firstPage = session.searchPage(
              new IndexPageRequest(new MatchAllDocsQuery(), sort, 2, List.of()));

          assertThat(values(firstPage.docs())).containsExactly("one", "two");
          assertThat(firstPage.hasNext()).isTrue();

          IndexPageResult secondPage = session.searchPage(
              new IndexPageRequest(new MatchAllDocsQuery(), sort, 2, firstPage.nextSearchAfter()));

          assertThat(values(secondPage.docs())).containsExactly("three", "four");
          assertThat(secondPage.hasNext()).isFalse();
          assertThat(secondPage.nextSearchAfter()).isEmpty();
        }
      }
    }
  }

  @Test
  public void termsAggregation_returnsTopBucketsByCountNotLexicographicOrder() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      // "zebra" is lexicographically last but most frequent; "apple" is first but least frequent.
      writer.addDocument(documentWithTerm("zebra"));
      writer.addDocument(documentWithTerm("zebra"));
      writer.addDocument(documentWithTerm("zebra"));
      writer.addDocument(documentWithTerm("mango"));
      writer.addDocument(documentWithTerm("mango"));
      writer.addDocument(documentWithTerm("apple"));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          List<IndexTermsBucket> buckets = session.termsAggregation(new MatchAllDocsQuery(), "term", 2);

          assertThat(buckets).extracting(IndexTermsBucket::key).containsExactly("zebra", "mango");
          assertThat(buckets).extracting(IndexTermsBucket::count).containsExactly(3L, 2L);
        }
      }
    }
  }

  private Document document(final String value) {
    Document document = new Document();
    document.add(new StringField("value", value, YES));
    return document;
  }

  private Document documentWithTerm(final String term) {
    Document document = new Document();
    document.add(new StringField("term", term, YES));
    return document;
  }

  private Document document(final String value, final String sortKey) {
    Document document = document(value);
    document.add(new SortedDocValuesField("sortKey", new BytesRef(sortKey)));
    return document;
  }

  private List<String> values(final List<Document> documents) {
    return documents.stream()
        .map(document -> document.get("value"))
        .toList();
  }
}
