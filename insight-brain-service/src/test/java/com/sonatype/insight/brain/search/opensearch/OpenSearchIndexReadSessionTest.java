/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.lucene.LuceneComponents;
import com.sonatype.insight.brain.search.session.IndexPageRequest;
import com.sonatype.insight.brain.search.session.IndexPageResult;
import com.sonatype.insight.brain.search.session.IndexSessionCursors;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.pit.CreatePitResponse;
import org.opensearch.client.opensearch.core.pit.CreatePitRequest;
import org.opensearch.client.opensearch.core.pit.DeletePitRequest;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.HitsMetadata;
import org.opensearch.client.opensearch.core.search.TotalHits;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpenSearchIndexReadSessionTest
{
  private OpenSearchClient client;

  private OpenSearchIndexReadSession session;

  @Before
  public void setUp() throws Exception {
    client = mock(OpenSearchClient.class);
    CreatePitResponse pit = mock(CreatePitResponse.class);
    when(pit.pitId()).thenReturn("pit-1");
    when(client.createPit(any(CreatePitRequest.class))).thenReturn(pit);
    session = new OpenSearchIndexReadSession(
        client,
        "test-index",
        new ConversionHelper(new LuceneComponents(mock(InsightWork.class))),
        new MatchAllDocsQuery(),
        Instant.EPOCH,
        "15m");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchPage_usesPitAndEmitsBackendBoundCursor() throws Exception {
    Hit<Map> first = mock(Hit.class);
    when(first.source()).thenReturn(Map.of("value", "one"));
    when(first.sort()).thenReturn(List.of("one"));
    Hit<Map> extra = mock(Hit.class);
    stubSearchResponse(List.of(first, extra), 2L);

    IndexPageResult result = session.searchPage(new IndexPageRequest(new MatchAllDocsQuery(), null, 1, List.of()));

    assertThat(result.docs()).hasSize(1);
    assertThat(result.hasNext()).isTrue();
    assertThat(IndexSessionCursors.decode(OpenSearchIndexReadSession.BACKEND_ID, result.nextSearchAfter()))
        .containsExactly("one");

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    verify(client).search(captor.capture(), eq(Map.class));
    assertThat(captor.getValue().pit().id()).isEqualTo("pit-1");
    assertThat(captor.getValue().index()).isEmpty();
  }

  @Test
  public void searchPage_reusesBackendBoundCursorAsSearchAfter() throws Exception {
    stubSearchResponse(List.of(), 0L);

    session.searchPage(new IndexPageRequest(
        new MatchAllDocsQuery(),
        null,
        1,
        IndexSessionCursors.encode(OpenSearchIndexReadSession.BACKEND_ID, List.of("one"))));

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    verify(client).search(captor.capture(), eq(Map.class));
    assertThat(captor.getValue().searchAfter()).containsExactly("one");
  }

  @Test
  public void termsAggregation_usesPitAndMapsStringTermsBuckets() throws Exception {
    stubAggregationResponse(Map.of("terms", termsAggregate()));

    assertThat(session.termsAggregation(new MatchAllDocsQuery(), "category", 10))
        .containsExactly(
            new IndexTermsBucket("alpha", 2L),
            new IndexTermsBucket("beta", 1L));

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    verify(client).search(captor.capture(), eq(Map.class));
    assertThat(captor.getValue().pit().id()).isEqualTo("pit-1");
    assertThat(captor.getValue().index()).isEmpty();
    assertThat(captor.getValue().aggregations()).containsKey("terms");
  }

  @Test
  public void createPit_usesConfiguredKeepAlive() throws Exception {
    ArgumentCaptor<CreatePitRequest> captor = ArgumentCaptor.forClass(CreatePitRequest.class);
    verify(client).createPit(captor.capture());
    assertThat(captor.getValue().keepAlive().time()).isEqualTo("15m");
  }

  @Test
  public void searchPage_rejectsNumericSortFieldsForPagination() {
    Sort numericSort = new Sort(new SortField("priority", SortField.Type.LONG, false));

    assertThatThrownBy(
        () -> session.searchPage(new IndexPageRequest(new MatchAllDocsQuery(), numericSort, 1, List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("keyword/string sort fields");
  }

  @Test
  public void close_deletesPit() throws Exception {
    session.close();

    verify(client).deletePit(any(DeletePitRequest.class));
  }

  @SuppressWarnings("unchecked")
  private void stubSearchResponse(final List<Hit<Map>> hits, final long total) throws Exception {
    SearchResponse<Map> response = mock(SearchResponse.class);
    HitsMetadata<Map> hitsMetadata = mock(HitsMetadata.class);
    when(response.hits()).thenReturn(hitsMetadata);
    when(hitsMetadata.total()).thenReturn(TotalHits.of(t -> t.value(total).relation(TotalHitsRelation.Eq)));
    when(hitsMetadata.hits()).thenReturn(hits);
    when(client.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);
  }

  @SuppressWarnings("unchecked")
  private void stubAggregationResponse(final Map<String, Aggregate> aggregations) throws Exception {
    SearchResponse<Map> response = mock(SearchResponse.class);
    HitsMetadata<Map> hitsMetadata = mock(HitsMetadata.class);
    when(response.hits()).thenReturn(hitsMetadata);
    when(hitsMetadata.total()).thenReturn(TotalHits.of(t -> t.value(0L).relation(TotalHitsRelation.Eq)));
    when(hitsMetadata.hits()).thenReturn(List.of());
    when(response.aggregations()).thenReturn(aggregations);
    when(client.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);
  }

  private Aggregate termsAggregate() {
    return Aggregate.of(a -> a.sterms(s -> s
        .sumOtherDocCount(0L)
        .buckets(b -> b.array(List.of(
            bucket("alpha", 2L),
            bucket("beta", 1L))))));
  }

  private StringTermsBucket bucket(final String key, final long docCount) {
    return StringTermsBucket.of(b -> b.key(key).docCount(docCount));
  }
}
