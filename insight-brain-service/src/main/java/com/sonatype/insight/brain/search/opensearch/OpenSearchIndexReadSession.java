/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.brain.search.session.IndexPageRequest;
import com.sonatype.insight.brain.search.session.IndexPageResult;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexSessionCursors;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.pit.CreatePitRequest;
import org.opensearch.client.opensearch.core.pit.DeletePitRequest;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.HitsMetadata;
import org.opensearch.client.opensearch.core.search.Pit;
import org.opensearch.client.opensearch.core.search.SourceConfig;
import org.opensearch.client.opensearch.core.search.TotalHits;
import org.opensearch.client.opensearch.core.search.TrackHits;

import static org.apache.lucene.search.BooleanClause.Occur.FILTER;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;

public class OpenSearchIndexReadSession
    implements IndexReadSession
{
  private static final Logger log = LoggerFactory.getLogger(OpenSearchIndexReadSession.class);

  public static final String BACKEND_ID = OpenSearchSearchIndexClient.BACKEND_ID;

  private final OpenSearchClient client;

  private final ConversionHelper conversionHelper;

  private final Query rbacFilter;

  private final Instant lastUpdatedAt;

  private final String pitKeepAlive;

  private final String pitId;

  private volatile boolean closed;

  public OpenSearchIndexReadSession(
      final OpenSearchClient client,
      final String indexName,
      final ConversionHelper conversionHelper,
      final Query rbacFilter,
      final Instant lastUpdatedAt,
      final String pitKeepAlive)
  {
    this.client = client;
    this.conversionHelper = conversionHelper;
    this.rbacFilter = rbacFilter;
    this.lastUpdatedAt = lastUpdatedAt;
    this.pitKeepAlive = pitKeepAlive;
    try {
      CreatePitRequest request = new CreatePitRequest.Builder()
          .targetIndexes(indexName)
          .keepAlive(t -> t.time(pitKeepAlive))
          .build();
      this.pitId = client.createPit(request)
          .pitId();
    }
    catch (IOException e) {
      throw new SearchIndexException(e);
    }
  }

  @Override
  public String backendId() {
    return BACKEND_ID;
  }

  @Override
  public Instant lastUpdatedAt() {
    return lastUpdatedAt;
  }

  @Override
  public String snapshotHandle() {
    return pitId;
  }

  @Override
  public IndexPageResult searchPage(final IndexPageRequest request) {
    ensureOpen();
    if (request.pageSize() <= 0) {
      throw new IllegalArgumentException("pageSize must be > 0");
    }

    List<SortOptions> sortOptions = buildSortOptions(request.sort());
    SearchRequest.Builder builder = new SearchRequest.Builder()
        .pit(new Pit.Builder().id(pitId).keepAlive(pitKeepAlive).build())
        .query(LuceneToOpenSearchQueryAdapter.toOpenSearch(withRbac(request.query())))
        .size(request.pageSize() + 1)
        .trackTotalHits(new TrackHits.Builder().enabled(true).build())
        .source(SourceConfig.of(s -> s.fetch(true)))
        .sort(sortOptions);

    List<Object> searchAfter = IndexSessionCursors.decode(BACKEND_ID, request.searchAfter());
    if (!searchAfter.isEmpty()) {
      // Pagination is restricted to keyword/string sort fields (see buildSortOptions); cursors store strings.
      builder.searchAfter(searchAfter.stream().map(String::valueOf).toList());
    }

    try {
      SearchResponse<Map> response = client.search(builder.build(), Map.class);
      List<Hit<Map>> hits = response.hits().hits();
      AbstractSearchIndexClient.HasMoreResult<Hit<Map>> page =
          AbstractSearchIndexClient.detectHasMore(hits, request.pageSize());
      List<Document> docs = new ArrayList<>(page.rows().size());
      for (Hit<Map> hit : page.rows()) {
        Map<String, Object> source = hit.source();
        if (source == null) {
          throw new SearchIndexException("OpenSearch search hit " + hit.id() + " carried a null _source", null);
        }
        docs.add(conversionHelper.mapToDocument(source));
      }

      List<Object> nextSearchAfter = List.of();
      if (page.hasMore() && !page.rows().isEmpty()) {
        List<String> sortValues = page.rows().get(page.rows().size() - 1).sort();
        if (sortValues == null || sortValues.isEmpty()) {
          throw new SearchIndexException("overfetch indicated a next page but the last hit carried no sort tuple",
              null);
        }
        nextSearchAfter = IndexSessionCursors.encode(BACKEND_ID, sortValues);
      }
      return new IndexPageResult(docs, nextSearchAfter, page.hasMore());
    }
    catch (IOException e) {
      throw new SearchIndexException(e);
    }
  }

  @Override
  public long count(final Query query) {
    ensureOpen();
    try {
      SearchResponse<Map> response = client.search(new SearchRequest.Builder()
          .pit(new Pit.Builder().id(pitId).keepAlive(pitKeepAlive).build())
          .query(LuceneToOpenSearchQueryAdapter.toOpenSearch(withRbac(query)))
          .size(0)
          .trackTotalHits(new TrackHits.Builder().enabled(true).build())
          .build(), Map.class);
      return Optional.ofNullable(response.hits()).map(HitsMetadata::total).map(TotalHits::value).orElse(0L);
    }
    catch (IOException e) {
      throw new SearchIndexException(e);
    }
  }

  @Override
  public List<IndexTermsBucket> termsAggregation(final Query query, final String field, final int maxBuckets) {
    ensureOpen();
    if (maxBuckets <= 0) {
      return List.of();
    }
    try {
      SearchResponse<Map> response = client.search(new SearchRequest.Builder()
          .pit(new Pit.Builder().id(pitId).keepAlive(pitKeepAlive).build())
          .query(LuceneToOpenSearchQueryAdapter.toOpenSearch(withRbac(query)))
          .size(0)
          .aggregations("terms", a -> a.terms(t -> t.field(field).size(maxBuckets)))
          .build(), Map.class);

      Aggregate aggregate = response.aggregations() == null ? null : response.aggregations().get("terms");
      if (aggregate == null || !aggregate.isSterms()) {
        return List.of();
      }
      return aggregate.sterms()
          .buckets()
          .array()
          .stream()
          .map(this::toBucket)
          .toList();
    }
    catch (IOException e) {
      throw new SearchIndexException(e);
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    try {
      client.deletePit(new DeletePitRequest.Builder().pitId(List.of(pitId)).build());
    }
    catch (IOException e) {
      // PIT expires via keepAlive; do not suppress an exception from the try-with-resources body.
      log.warn("Failed to delete OpenSearch PIT {}; it will expire after keepAlive", pitId, e);
    }
  }

  private IndexTermsBucket toBucket(final StringTermsBucket bucket) {
    return new IndexTermsBucket(bucket.key(), bucket.docCount());
  }

  private Query withRbac(final Query query) {
    Query baseQuery = query == null ? new MatchAllDocsQuery() : query;
    return new BooleanQuery.Builder()
        .add(baseQuery, MUST)
        .add(rbacFilter, FILTER)
        .build();
  }

  private List<SortOptions> buildSortOptions(final Sort sort) {
    List<SortOptions> options = new ArrayList<>();
    if (sort != null) {
      for (SortField sf : sort.getSort()) {
        if (sf.getField() == null) {
          if (sf.getType() != SortField.Type.SCORE) {
            throw new IllegalArgumentException(
                "OpenSearch IndexReadSession only supports Lucene special sort FIELD_SCORE; got: " + sf);
          }
          options.add(SortOptions.of(o -> o.score(s -> s
              .order(sf.getReverse() ? SortOrder.Asc : SortOrder.Desc))));
        }
        else if (sf.getType() != SortField.Type.STRING) {
          // OpenSearch search_after requires stable string sort keys; numeric sorts stringify incorrectly.
          throw new IllegalArgumentException(
              "OpenSearch IndexReadSession only supports keyword/string sort fields for search_after pagination; got: "
                  + sf);
        }
        else {
          options.add(SortOptions.of(o -> o
              .field(f -> f
                  .field(sf.getField())
                  .order(sf.getReverse() ? SortOrder.Desc : SortOrder.Asc))));
        }
      }
    }
    else {
      options.add(SortOptions.of(o -> o.score(s -> s.order(SortOrder.Desc))));
    }
    options.add(SortOptions.of(o -> o
        .field(f -> f.field(FieldIdentifier.DOCUMENT_KEY.label).order(SortOrder.Asc))));
    return options;
  }

  private void ensureOpen() {
    if (closed) {
      throw new IllegalStateException("OpenSearch IndexReadSession is closed.");
    }
  }
}
