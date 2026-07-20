/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.session;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.lucene.LowerCaseKeywordAnalyzer;
import com.sonatype.insight.brain.search.lucene.LuceneComponents;
import com.sonatype.insight.brain.search.lucene.LuceneIndexReadSession;
import com.sonatype.insight.brain.search.lucene.LuceneSearcherManagerHolder;
import com.sonatype.insight.brain.search.opensearch.OpenSearchIndexReadSession;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.pit.CreatePitRequest;
import org.opensearch.client.opensearch.core.pit.CreatePitResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.HitsMetadata;
import org.opensearch.client.opensearch.core.search.TotalHits;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;

import static org.apache.lucene.document.Field.Store.YES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class IndexReadSessionContractTest
{
  private final String mode;

  private final SessionFactory sessionFactory;

  public IndexReadSessionContractTest(final String mode, final SessionFactory sessionFactory) {
    this.mode = mode;
    this.sessionFactory = sessionFactory;
  }

  @Parameterized.Parameters(name = "{0}")
  public static Object[][] modes() {
    return new Object[][]{
      {"lucene", (SessionFactory) IndexReadSessionContractTest::luceneSession},
      {"opensearch", (SessionFactory) IndexReadSessionContractTest::openSearchSession},
      {"hybrid", (SessionFactory) () -> new HybridIndexReadSession(luceneSession())}
    };
  }

  @Test
  public void searchPage_collectsOneExtraHitAndResumesWithBackendBoundSearchAfter() throws Exception {
    try (IndexReadSession session = sessionFactory.open()) {
      IndexPageResult firstPage = session.searchPage(new IndexPageRequest(new MatchAllDocsQuery(), null, 2, List.of()));

      assertThat(values(firstPage.docs()))
          .as(mode)
          .containsExactly("one", "two");
      assertThat(firstPage.hasNext()).as(mode).isTrue();
      assertThat(firstPage.nextSearchAfter()).as(mode).isNotEmpty();

      IndexPageResult secondPage = session.searchPage(
          new IndexPageRequest(new MatchAllDocsQuery(), null, 2, firstPage.nextSearchAfter()));

      assertThat(values(secondPage.docs()))
          .as(mode)
          .containsExactly("three");
      assertThat(secondPage.hasNext()).as(mode).isFalse();
      assertThat(secondPage.nextSearchAfter()).as(mode).isEmpty();
    }
  }

  @Test
  public void count_returnsMatchingDocumentCount() throws Exception {
    try (IndexReadSession session = sessionFactory.open()) {
      assertThat(session.count(new MatchAllDocsQuery()))
          .as(mode)
          .isEqualTo(3L);
    }
  }

  @Test
  public void termsAggregation_returnsNonEmptyBuckets() throws Exception {
    try (IndexReadSession session = sessionFactory.open()) {
      assertThat(session.termsAggregation(new MatchAllDocsQuery(), "category", 10))
          .as(mode)
          .containsExactly(
              new IndexTermsBucket("alpha", 2L),
              new IndexTermsBucket("beta", 1L));
    }
  }

  @Test
  public void searchPage_rejectsCursorFromDifferentBackend() throws Exception {
    try (IndexReadSession session = sessionFactory.open()) {
      String otherBackend = session.backendId().equals("lucene") ? "opensearch" : "lucene";

      assertThatThrownBy(() -> session.searchPage(new IndexPageRequest(
          new MatchAllDocsQuery(),
          null,
          10,
          IndexSessionCursors.encode(otherBackend, List.of("cursor")))))
              .as(mode)
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining(otherBackend)
              .hasMessageContaining(session.backendId());
    }
  }

  private static IndexReadSession luceneSession() throws Exception {
    Directory directory = new ByteBuffersDirectory();
    Analyzer analyzer = new LowerCaseKeywordAnalyzer();
    IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer));
    writer.addDocument(document("one", "alpha"));
    writer.addDocument(document("two", "alpha"));
    writer.addDocument(document("three", "beta"));
    writer.commit();
    LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null);
    IndexSearcher searcher = holder.acquire();
    return new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)
    {
      @Override
      public void close() {
        super.close();
        try {
          holder.close();
          writer.close();
          analyzer.close();
          directory.close();
        }
        catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  private static IndexReadSession openSearchSession() throws Exception {
    OpenSearchClient client = mock(OpenSearchClient.class);
    CreatePitResponse pit = mock(CreatePitResponse.class);
    when(pit.pitId()).thenReturn("pit-1");
    when(client.createPit(any(CreatePitRequest.class))).thenReturn(pit);
    when(client.search(any(SearchRequest.class), eq(Map.class))).thenAnswer(invocation -> {
      SearchRequest request = invocation.getArgument(0);
      if (request.aggregations() != null && !request.aggregations().isEmpty()) {
        return searchResponse(List.of(), 3L, Map.of("terms", termsAggregate()));
      }
      if (Integer.valueOf(0).equals(request.size())) {
        return searchResponse(List.of(), 3L, Map.of());
      }
      if (request.searchAfter() != null && !request.searchAfter().isEmpty()) {
        return searchResponse(List.of(hit("three", "beta", "3")), 3L, Map.of());
      }
      return searchResponse(List.of(
          hit("one", "alpha", "1"),
          hit("two", "alpha", "2"),
          hit("three", "beta", "3")), 3L, Map.of());
    });
    return new OpenSearchIndexReadSession(
        client,
        "test-index",
        new ConversionHelper(new LuceneComponents(mock(InsightWork.class))),
        new MatchAllDocsQuery(),
        Instant.EPOCH,
        "15m");
  }

  private static Document document(final String value, final String category) {
    Document document = new Document();
    document.add(new StringField("value", value, YES));
    document.add(new StringField("category", category, YES));
    return document;
  }

  private static List<String> values(final List<Document> documents) {
    return documents.stream()
        .map(document -> document.get("value"))
        .toList();
  }

  @SuppressWarnings("unchecked")
  private static Hit<Map> hit(final String value, final String category, final String sortValue) {
    Hit<Map> hit = mock(Hit.class);
    when(hit.source()).thenReturn(Map.of("value", value, "category", category));
    when(hit.sort()).thenReturn(List.of(sortValue));
    return hit;
  }

  @SuppressWarnings("unchecked")
  private static SearchResponse<Map> searchResponse(
      final List<Hit<Map>> hits,
      final long total,
      final Map<String, Aggregate> aggregations)
  {
    SearchResponse<Map> response = mock(SearchResponse.class);
    HitsMetadata<Map> hitsMetadata = mock(HitsMetadata.class);
    when(response.hits()).thenReturn(hitsMetadata);
    when(response.aggregations()).thenReturn(aggregations);
    when(hitsMetadata.total()).thenReturn(TotalHits.of(t -> t.value(total).relation(TotalHitsRelation.Eq)));
    when(hitsMetadata.hits()).thenReturn(hits);
    return response;
  }

  private static Aggregate termsAggregate() {
    return Aggregate.of(a -> a.sterms(s -> s
        .sumOtherDocCount(0L)
        .buckets(b -> b.array(List.of(
            bucket("alpha", 2L),
            bucket("beta", 1L))))));
  }

  private static StringTermsBucket bucket(final String key, final long docCount) {
    return StringTermsBucket.of(b -> b.key(key).docCount(docCount));
  }

  private interface SessionFactory
  {
    IndexReadSession open() throws Exception;
  }
}
