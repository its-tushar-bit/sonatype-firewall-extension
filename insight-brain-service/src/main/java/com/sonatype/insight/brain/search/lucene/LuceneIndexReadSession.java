/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.brain.search.session.IndexPageRequest;
import com.sonatype.insight.brain.search.session.IndexPageResult;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexSessionCursors;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.lucene.search.BooleanClause.Occur.FILTER;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;

public class LuceneIndexReadSession
    implements IndexReadSession
{
  private static final Logger log = LoggerFactory.getLogger(LuceneIndexReadSession.class);

  public static final String BACKEND_ID = "lucene";

  private final IndexSearcher searcher;

  private final Query rbacFilter;

  private final LuceneSearcherManagerHolder searcherManagerHolder;

  private final Instant lastUpdatedAt;

  private final String snapshotHandle;

  private volatile boolean closed;

  public LuceneIndexReadSession(
      final IndexSearcher searcher,
      final Query rbacFilter,
      final LuceneSearcherManagerHolder searcherManagerHolder)
  {
    this.searcher = searcher;
    this.rbacFilter = rbacFilter;
    this.searcherManagerHolder = searcherManagerHolder;
    this.lastUpdatedAt = searcherManagerHolder.getLastSuccessfulRefreshInstant();
    this.snapshotHandle = snapshotHandle(searcher);
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
    return snapshotHandle;
  }

  @Override
  public IndexPageResult searchPage(final IndexPageRequest request) {
    ensureOpen();
    if (request.pageSize() <= 0) {
      throw new IllegalArgumentException("pageSize must be > 0");
    }

    try {
      Query query = withRbac(request.query());
      int collectN = request.pageSize() + 1;
      ScoreDoc after = decodeSearchAfter(request.searchAfter(), request.sort());
      TopDocs topDocs = request.sort() == null
          ? searcher.searchAfter(after, query, collectN)
          : searcher.searchAfter(after, query, collectN, request.sort());

      ScoreDoc[] hits = topDocs.scoreDocs;
      int returnCount = Math.min(hits.length, request.pageSize());
      List<Document> docs = new ArrayList<>(returnCount);
      StoredFields storedFields = searcher.storedFields();
      for (int i = 0; i < returnCount; i++) {
        docs.add(storedFields.document(hits[i].doc));
      }

      boolean hasNext = hits.length > request.pageSize();
      List<Object> nextSearchAfter = hasNext && returnCount > 0
          ? encodeSearchAfter(hits[returnCount - 1], request.sort())
          : List.of();
      return new IndexPageResult(docs, nextSearchAfter, hasNext);
    }
    catch (IOException e) {
      throw new SearchIndexException(e);
    }
  }

  @Override
  public long count(final Query query) {
    ensureOpen();
    try {
      TotalHitCountCollector collector = new TotalHitCountCollector();
      searcher.search(withRbac(query), collector);
      return collector.getTotalHits();
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
      Map<String, Long> counts = new HashMap<>();
      StoredFields storedFields = searcher.storedFields();
      searcher.search(withRbac(query), new SimpleCollector()
      {
        private int docBase;

        @Override
        protected void doSetNextReader(final LeafReaderContext context) {
          docBase = context.docBase;
        }

        @Override
        public void collect(final int doc) throws IOException {
          Document document = storedFields.document(docBase + doc, Set.of(field));
          for (String value : document.getValues(field)) {
            counts.merge(value, 1L, Long::sum);
          }
        }

        @Override
        public ScoreMode scoreMode() {
          return ScoreMode.COMPLETE_NO_SCORES;
        }
      });
      // Match OpenSearch: top-N by doc count (desc), then term for stability — not lexicographic TreeMap order.
      return counts.entrySet()
          .stream()
          .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue)
              .reversed()
              .thenComparing(Map.Entry::getKey))
          .limit(maxBuckets)
          .map(entry -> new IndexTermsBucket(entry.getKey(), entry.getValue()))
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
      searcherManagerHolder.release(searcher);
    }
    catch (IOException e) {
      log.warn("Failed to release Lucene IndexSearcher on session close", e);
    }
  }

  private Query withRbac(final Query query) {
    Query baseQuery = query == null ? new MatchAllDocsQuery() : query;
    return new BooleanQuery.Builder()
        .add(baseQuery, MUST)
        .add(rbacFilter, FILTER)
        .build();
  }

  private ScoreDoc decodeSearchAfter(final List<Object> searchAfter, final Sort sort) {
    List<Object> luceneSearchAfter = IndexSessionCursors.decode(BACKEND_ID, searchAfter);
    if (luceneSearchAfter.isEmpty()) {
      return null;
    }
    if (sort != null) {
      if (luceneSearchAfter.size() != 3) {
        throw new IllegalArgumentException("Sorted searchAfter must contain doc id, score, and sort fields");
      }
      Object fields = luceneSearchAfter.get(2);
      if (fields instanceof List<?> fieldList) {
        return new FieldDoc(
            ((Number) luceneSearchAfter.get(0)).intValue(),
            ((Number) luceneSearchAfter.get(1)).floatValue(),
            fieldList.toArray());
      }
      if (fields instanceof Object[] fieldArray) {
        return new FieldDoc(
            ((Number) luceneSearchAfter.get(0)).intValue(),
            ((Number) luceneSearchAfter.get(1)).floatValue(),
            fieldArray);
      }
      throw new IllegalArgumentException("Sorted searchAfter sort fields must be a list or array");
    }
    if (luceneSearchAfter.size() != 2) {
      throw new IllegalArgumentException("Unsorted searchAfter must contain doc id and score");
    }
    return new ScoreDoc(
        ((Number) luceneSearchAfter.get(0)).intValue(),
        ((Number) luceneSearchAfter.get(1)).floatValue());
  }

  private List<Object> encodeSearchAfter(final ScoreDoc scoreDoc, final Sort sort) {
    if (sort != null) {
      if (scoreDoc instanceof FieldDoc fieldDoc && fieldDoc.fields != null) {
        return IndexSessionCursors.encode(BACKEND_ID,
            List.of(fieldDoc.doc, fieldDoc.score, Arrays.asList(fieldDoc.fields)));
      }
      return List.of();
    }
    return IndexSessionCursors.encode(BACKEND_ID, List.of(scoreDoc.doc, scoreDoc.score));
  }

  private void ensureOpen() {
    if (closed) {
      throw new IllegalStateException("Lucene IndexReadSession is closed.");
    }
  }

  private String snapshotHandle(final IndexSearcher searcher) {
    if (searcher.getIndexReader() instanceof DirectoryReader directoryReader) {
      return String.valueOf(directoryReader.getVersion());
    }
    return String.valueOf(System.identityHashCode(searcher.getIndexReader()));
  }
}
