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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.RankedGroupsResult;
import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.brain.search.session.IndexPageRequest;
import com.sonatype.insight.brain.search.session.IndexPageResult;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexSessionCursors;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.index.MultiDocValues.MultiSortedDocValues;
import org.apache.lucene.index.OrdinalMap;
import org.apache.lucene.index.SortedDocValues;
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
import org.apache.lucene.util.FixedBitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.lucene.search.BooleanClause.Occur.FILTER;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;

public class LuceneIndexReadSession
    implements IndexReadSession
{
  private static final Logger log = LoggerFactory.getLogger(LuceneIndexReadSession.class);

  public static final String BACKEND_ID = "lucene";

  /**
   * Soft warn threshold for interim stored-field distinct-grouped scans (Track B replaces this).
   * Compared against {@link DistinctGroupedStoredFieldCollector#matchedDocuments()}, which counts
   * only docs with non-blank fields whose group value is in the allowed set — not every Lucene hit.
   */
  static final long COUNT_DISTINCT_GROUPED_WARN_MATCHED_DOCUMENTS = 50_000L;

  /**
   * Distinct-value-count threshold for the per-band distinct scan warning. When the cardinality
   * collected by {@link #countDistinctWithSearcher} exceeds this, a single warn is emitted per
   * aggregateCountByFloatField request (not per band). Unit: number of distinct field values.
   */
  static final long COUNT_DISTINCT_VALUES_WARN_THRESHOLD = 50_000L;

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
  public Map<String, Long> countDistinctGroupedBy(
      final Query query,
      final String groupField,
      final String distinctField,
      final Collection<String> groupValues)
  {
    ensureOpen();
    if (groupValues == null || groupValues.isEmpty()) {
      return Map.of();
    }
    long startNanos = System.nanoTime();
    DistinctGroupedStoredFieldCollector collector;
    try {
      collector = new DistinctGroupedStoredFieldCollector(
          searcher.storedFields(), groupField, distinctField, groupValues);
      searcher.search(withRbac(query), collector);
    }
    catch (IOException e) {
      throw new SearchIndexException(e);
    }
    long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
    long matchedDocuments = collector.matchedDocuments();
    if (matchedDocuments > COUNT_DISTINCT_GROUPED_WARN_MATCHED_DOCUMENTS) {
      log.warn(
          "countDistinctGroupedBy matched {} documents (threshold {}); consider Track B docValues cardinality",
          matchedDocuments,
          COUNT_DISTINCT_GROUPED_WARN_MATCHED_DOCUMENTS);
    }
    log.debug(
        "DASHBOARD_BENCHMARK metric=count_distinct_grouped matchedDocuments={} durationMs={}",
        matchedDocuments,
        durationMs);
    return collector.groupCounts();
  }

  @Override
  public RankedGroupsResult rankGroupsByMaxMetric(
      final Query query,
      final String groupField,
      final String metricField,
      final int limit,
      final boolean ascending,
      final Map<String, float[]> metricBands)
  {
    ensureOpen();
    AbstractSearchIndexClient.validateFloatRangeBounds(metricBands);
    if (limit <= 0) {
      return RankedGroupsResult.empty(metricBands);
    }
    try {
      IndexReader indexReader = searcher.getIndexReader();
      SortedDocValues globalGroups = MultiDocValues.getSortedValues(indexReader, groupField);
      if (globalGroups == null) {
        return RankedGroupsResult.empty(metricBands);
      }
      OrdinalMap ordinalMap = globalGroups instanceof MultiSortedDocValues multi ? multi.mapping : null;

      int ordCount = globalGroups.getValueCount();
      float[] maxByOrd = new float[ordCount];
      Arrays.fill(maxByOrd, Float.NaN);
      FixedBitSet seen = new FixedBitSet(Math.max(ordCount, 1));

      searcher.search(withRbac(query),
          new MaxMetricByGroupCollector(groupField, metricField, ordinalMap, maxByOrd, seen));

      return RankedGroupsReduction.reduceRankedGroups(
          globalGroups, maxByOrd, seen, ordCount, limit, ascending, metricBands);
    }
    catch (IOException e) {
      throw new SearchIndexException(e);
    }
  }

  /**
   * Lucene implements each int band as a separate filtered count (one scan per band). OpenSearch uses
   * a single range aggregation. Track B docValues faceting replaces this fan-out.
   */
  @Override
  public MetricAggregationResult aggregateCountByField(
      final Query query,
      final String bucketField,
      final Map<String, int[]> ranges)
  {
    ensureOpen();
    AbstractSearchIndexClient.validateRangeBounds(ranges);
    try {
      long total = searcher.count(withRbac(query));
      Map<String, Long> buckets = new LinkedHashMap<>();
      for (Map.Entry<String, int[]> entry : ranges.entrySet()) {
        int[] bounds = entry.getValue();
        Query bandFilter = IntPoint.newRangeQuery(bucketField, bounds[0], bounds[1]);
        buckets.put(entry.getKey(), (long) searcher.count(withRbacAndFilter(query, bandFilter)));
      }
      return new MetricAggregationResult(total, buckets);
    }
    catch (IOException e) {
      throw new SearchIndexException(e);
    }
  }

  /**
   * Lucene implements each float band as a separate filtered scan (and, when {@code distinctField} is
   * set, an interim stored-field distinct collect). OpenSearch uses a single range aggregation.
   * Track B docValues faceting replaces this fan-out. Distinct-cardinality warn fires at most once
   * per call.
   */
  @Override
  public MetricAggregationResult aggregateCountByFloatField(
      final Query query,
      final String bucketField,
      final Map<String, float[]> ranges,
      final String distinctField)
  {
    ensureOpen();
    AbstractSearchIndexClient.validateFloatRangeBounds(ranges);
    try {
      long total = searcher.count(withRbac(query));
      Map<String, Long> buckets = new LinkedHashMap<>();
      boolean[] warnedDistinct = {false};
      for (Map.Entry<String, float[]> entry : ranges.entrySet()) {
        float[] bounds = entry.getValue();
        long count = 0;
        if (bounds[0] < bounds[1]) {
          Query bandFilter = FloatPoint.newRangeQuery(bucketField, bounds[0], FloatPoint.nextDown(bounds[1]));
          Query bandQuery = withRbacAndFilter(query, bandFilter);
          count = distinctField == null
              ? searcher.count(bandQuery)
              : countDistinctWithSearcher(bandQuery, distinctField, warnedDistinct);
        }
        buckets.put(entry.getKey(), count);
      }
      return new MetricAggregationResult(total, buckets);
    }
    catch (IOException e) {
      throw new SearchIndexException(e);
    }
  }

  /**
   * Lucene runs one filtered distinct-grouped collect per band (interim stored-field path). OpenSearch
   * uses a single nested range→terms→cardinality aggregation. Track B replaces this fan-out.
   */
  @Override
  public Map<String, Map<String, Long>> countDistinctGroupedByBands(
      final Query query,
      final String groupField,
      final String distinctField,
      final Collection<String> groupValues,
      final String bandField,
      final Map<String, int[]> bands)
  {
    ensureOpen();
    AbstractSearchIndexClient.validateRangeBounds(bands);
    if (groupValues == null || groupValues.isEmpty() || bands == null || bands.isEmpty()) {
      return Map.of();
    }
    Map<String, Map<String, Long>> byGroup = new LinkedHashMap<>();
    try {
      for (Map.Entry<String, int[]> band : bands.entrySet()) {
        int[] bounds = band.getValue();
        Query bandFilter = IntPoint.newRangeQuery(bandField, bounds[0], bounds[1]);
        DistinctGroupedStoredFieldCollector collector = new DistinctGroupedStoredFieldCollector(
            searcher.storedFields(), groupField, distinctField, groupValues);
        searcher.search(withRbacAndFilter(query, bandFilter), collector);
        collector.groupCounts()
            .forEach(
                (group, count) -> byGroup.computeIfAbsent(group, g -> new LinkedHashMap<>()).put(band.getKey(), count));
      }
      return byGroup;
    }
    catch (IOException e) {
      throw new SearchIndexException(e);
    }
  }

  @Override
  public Map<String, Long> sumGroupedBy(
      final Query query,
      final String groupField,
      final String sumField,
      final Collection<String> groupValues)
  {
    ensureOpen();
    AbstractSearchIndexClient.requireIntegralSumField(sumField);
    if (groupValues == null || groupValues.isEmpty()) {
      return Map.of();
    }
    try {
      SumGroupedDocValuesCollector collector = new SumGroupedDocValuesCollector(groupField, sumField, groupValues);
      searcher.search(withRbac(query), collector);
      return collector.groupSums();
    }
    catch (IOException e) {
      throw new SearchIndexException(e);
    }
  }

  /**
   * Lucene runs one filtered sum collect per band. OpenSearch uses a single nested range→terms→sum
   * aggregation. Track B may collapse this fan-out when sum callers land.
   */
  @Override
  public Map<String, Map<String, Long>> sumGroupedByBands(
      final Query query,
      final String groupField,
      final String sumField,
      final Collection<String> groupValues,
      final String bandField,
      final Map<String, int[]> bands)
  {
    ensureOpen();
    AbstractSearchIndexClient.requireIntegralSumField(sumField);
    AbstractSearchIndexClient.validateRangeBounds(bands);
    if (groupValues == null || groupValues.isEmpty() || bands == null || bands.isEmpty()) {
      return Map.of();
    }
    Map<String, Map<String, Long>> byGroup = new LinkedHashMap<>();
    try {
      for (Map.Entry<String, int[]> band : bands.entrySet()) {
        int[] bounds = band.getValue();
        Query bandFilter = IntPoint.newRangeQuery(bandField, bounds[0], bounds[1]);
        SumGroupedDocValuesCollector collector = new SumGroupedDocValuesCollector(groupField, sumField, groupValues);
        searcher.search(withRbacAndFilter(query, bandFilter), collector);
        collector.groupSums()
            .forEach(
                (group, sum) -> byGroup.computeIfAbsent(group, g -> new LinkedHashMap<>()).put(band.getKey(), sum));
      }
      return byGroup;
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

  private Query withRbacAndFilter(final Query query, final Query extraFilter) {
    return new BooleanQuery.Builder()
        .add(withRbac(query), MUST)
        .add(extraFilter, FILTER)
        .build();
  }

  private long countDistinctWithSearcher(
      final Query query,
      final String distinctField,
      final boolean[] warned) throws IOException
  {
    StoredFields storedFields = searcher.storedFields();
    Set<String> distinctValues = new HashSet<>();
    searcher.search(query, new SimpleCollector()
    {
      private int docBase;

      @Override
      protected void doSetNextReader(final LeafReaderContext context) {
        docBase = context.docBase;
      }

      @Override
      public void collect(final int doc) throws IOException {
        Document document = storedFields.document(docBase + doc, Set.of(distinctField));
        String value = document.get(distinctField);
        if (StringUtils.isBlank(value)) {
          return;
        }
        distinctValues.add(value.toLowerCase(Locale.ROOT));
      }

      @Override
      public ScoreMode scoreMode() {
        return ScoreMode.COMPLETE_NO_SCORES;
      }
    });
    long distinctCount = distinctValues.size();
    if (distinctCount > COUNT_DISTINCT_VALUES_WARN_THRESHOLD && !warned[0]) {
      warned[0] = true;
      log.warn(
          "countDistinctWithSearcher collected {} distinct values for field {} (threshold {}); "
              + "consider Track B docValues cardinality",
          distinctCount,
          distinctField,
          COUNT_DISTINCT_VALUES_WARN_THRESHOLD);
    }
    return distinctCount;
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
