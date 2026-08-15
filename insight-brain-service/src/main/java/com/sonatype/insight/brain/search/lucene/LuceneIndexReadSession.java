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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.Map;

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
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.StringDocValuesReaderState;
import org.apache.lucene.facet.StringValueFacetCounts;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.FieldInfo;
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
   * Soft warn threshold for a distinct-grouped scan. Compared against
   * {@link DistinctGroupedDocValuesCollector#matchedDocuments()}, which counts only docs with non-blank
   * fields whose group value is in the allowed set — not every Lucene hit.
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

  /**
   * Per-field cache for StringDocValuesReaderState. The session pins one IndexReader for its lifetime,
   * so this map is safe to populate lazily.
   */
  private final Map<String, StringDocValuesReaderState> facetStateCache = new HashMap<>();

  /**
   * Fields already reported as un-faceted in this session. A pre-reindex index would otherwise log the
   * same warning for every field on every faceted request, so each field is warned once per session and
   * logged at debug thereafter.
   */
  private final Set<String> missingDocValuesWarnedFields = new HashSet<>();

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

    Facetability facetability = facetability(field);
    if (facetability != Facetability.FACETABLE) {
      reportUnfacetableOnce(field, facetability);
      return List.of();
    }

    try {
      FacetsCollector fc = new FacetsCollector();
      searcher.search(withRbac(query), fc);

      StringValueFacetCounts counts = new StringValueFacetCounts(facetState(field), fc);
      FacetResult top = counts.getTopChildren(maxBuckets, field);

      if (top == null || top.labelValues == null || top.labelValues.length == 0) {
        return List.of();
      }

      // Map to IndexTermsBucket, preserving the counter's ordering (top-N by count desc)
      List<IndexTermsBucket> buckets = new ArrayList<>(top.labelValues.length);
      for (LabelAndValue lv : top.labelValues) {
        buckets.add(new IndexTermsBucket(lv.label, lv.value.longValue()));
      }

      return buckets;
    }
    catch (IOException e) {
      throw new SearchIndexException(e);
    }
    catch (IllegalArgumentException | IllegalStateException e) {
      // fieldHasFacetDocValues above screens the known cases, but StringDocValuesReaderState and
      // StringValueFacetCounts also reject doc-values shapes it cannot see (for example a field whose
      // type differs across segments). A faceted request degrades to an empty facet rather than a 500.
      log.warn(
          "termsAggregation failed for field '{}': the index cannot serve it as a facet ({}). "
              + "A full index rebuild is required before this field can be faceted.",
          field,
          e.toString());
      return List.of();
    }
  }

  /**
   * Reports a field that cannot be aggregated, once per field per session and at debug afterwards.
   * <p>
   * A field absent from every segment is only logged at debug: an estate with no application categories
   * at all legitimately has no {@code applicationCategoryId} values, and an empty facet is the right
   * answer, so warning would imply a rebuild that would change nothing. Only a field written without its
   * doc-values column warrants a warning, because there a rebuild does fix it.
   */
  private void reportUnfacetableOnce(final String field, final Facetability facetability) {
    boolean first = missingDocValuesWarnedFields.add(field);
    if (facetability == Facetability.ABSENT) {
      log.debug("termsAggregation returned no buckets for field '{}': no document carries a value for it.",
          field);
      return;
    }
    String message = "termsAggregation skipped for field '{}': documents carry it without facet doc values. "
        + "A full index rebuild is required before this field can be faceted.";
    if (first) {
      log.warn(message, field);
    }
    else {
      log.debug(message, field);
    }
  }

  /**
   * Returns true only when {@code field} is safe to feed to {@link StringValueFacetCounts}: every
   * segment that carries the field must expose SORTED / SORTED_SET doc values, and at least one
   * segment must have it. A field present without doc values (e.g. an index written before the
   * facet doc-values were added) would make {@link StringDocValuesReaderState} throw; treat it as
   * un-faceted instead.
   */
  private boolean fieldHasFacetDocValues(final String field) {
    return facetability(field) == Facetability.FACETABLE;
  }

  /**
   * Whether {@code field} can be aggregated, distinguishing a field no document has written yet from one
   * written without its doc-values column. Both aggregate to nothing, but only the second means the index
   * needs rebuilding.
   */
  private Facetability facetability(final String field) {
    boolean anyUsable = false;
    for (LeafReaderContext leaf : searcher.getIndexReader().leaves()) {
      FieldInfo info = leaf.reader().getFieldInfos().fieldInfo(field);
      if (info == null) {
        continue;
      }
      DocValuesType type = info.getDocValuesType();
      if (type == DocValuesType.SORTED || type == DocValuesType.SORTED_SET) {
        anyUsable = true;
      }
      else {
        return Facetability.NOT_FACETABLE;
      }
    }
    return anyUsable ? Facetability.FACETABLE : Facetability.ABSENT;
  }

  private enum Facetability
  {
    /** Every segment carrying the field exposes facet doc values. */
    FACETABLE,

    /** No segment carries the field, so no document has a value for it yet. */
    ABSENT,

    /** A segment carries the field without facet doc values, so the index predates the column. */
    NOT_FACETABLE
  }

  /**
   * The facet-counting state for {@code field}, built once per session. Building it walks the field's
   * doc-values ordinals across every segment, so two callers must not both pay for it: the state is
   * computed atomically rather than with a get-then-put, which would let concurrent callers each build one
   * and discard all but the last.
   */
  private StringDocValuesReaderState facetState(final String field) {
    return facetStateCache.computeIfAbsent(field, f -> {
      try {
        return new StringDocValuesReaderState(searcher.getIndexReader(), f);
      }
      catch (IOException e) {
        // Unwrapped by termsAggregation's caller, which degrades the facet rather than failing the page.
        throw new SearchIndexException(e);
      }
    });
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
    boolean columnar = groupedDistinctIsColumnar(groupField, distinctField);
    GroupedDistinctPass pass;
    try {
      pass = runGroupedDistinctPass(withRbac(query), groupField, distinctField, groupValues, columnar);
    }
    catch (IOException e) {
      throw new SearchIndexException(e);
    }
    long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
    long matchedDocuments = pass.matchedDocuments();
    if (matchedDocuments > COUNT_DISTINCT_GROUPED_WARN_MATCHED_DOCUMENTS) {
      log.warn(
          "countDistinctGroupedBy matched {} documents (threshold {})",
          matchedDocuments,
          COUNT_DISTINCT_GROUPED_WARN_MATCHED_DOCUMENTS);
    }
    log.debug(
        "DASHBOARD_BENCHMARK metric=count_distinct_grouped source={} matchedDocuments={} durationMs={}",
        columnar ? "docValues" : "storedFields",
        matchedDocuments,
        durationMs);
    return pass.groupCounts();
  }

  /**
   * True when a grouped-distinct pass over these two fields can read doc-values columns. Both the group
   * and the distinct field must carry facet doc values; otherwise the pass reads stored fields.
   */
  private boolean groupedDistinctIsColumnar(final String groupField, final String distinctField) {
    return fieldHasFacetDocValues(groupField) && fieldHasFacetDocValues(distinctField);
  }

  /**
   * True when every segment can serve a banded pass from doc values, so all bands are counted in one
   * collect instead of one filtered search per band.
   */
  private boolean bandsAreColumnar(final String metricField, final String distinctField) throws IOException {
    for (LeafReaderContext leaf : searcher.getIndexReader().leaves()) {
      if (!BandedDistinctDocValuesCollector.canCollect(leaf.reader(), metricField, distinctField)) {
        return false;
      }
    }
    return true;
  }

  /**
   * One grouped-distinct pass over {@code scopedQuery} (RBAC and any band filter already applied).
   * {@code columnar} selects the doc-values collector; when false the pass reads stored fields, which is
   * what an index built before the facet doc-values columns existed requires to return correct counts.
   */
  private GroupedDistinctPass runGroupedDistinctPass(
      final Query scopedQuery,
      final String groupField,
      final String distinctField,
      final Collection<String> groupValues,
      final boolean columnar) throws IOException
  {
    if (columnar) {
      DistinctGroupedDocValuesCollector collector =
          new DistinctGroupedDocValuesCollector(groupField, distinctField, groupValues);
      searcher.search(scopedQuery, collector);
      return new GroupedDistinctPass(collector.groupCounts(), collector.matchedDocuments());
    }
    DistinctGroupedStoredFieldCollector collector = new DistinctGroupedStoredFieldCollector(
        searcher.storedFields(), groupField, distinctField, groupValues);
    searcher.search(scopedQuery, collector);
    return new GroupedDistinctPass(collector.groupCounts(), collector.matchedDocuments());
  }

  /** Result of one grouped-distinct pass: per-group distinct counts plus the matched-document total. */
  private record GroupedDistinctPass(Map<String, Long> groupCounts, long matchedDocuments)
  {
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
   * Lucene implements each int band as a separate filtered count (one scan per band); the counts are
   * cheap term-index counts with no stored-field or doc-values reads. OpenSearch uses a single range
   * aggregation.
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
   * Lucene resolves every float band in ONE pass from the metric's doc-values column (see
   * {@link BandedDistinctDocValuesCollector}), including the per-band distinct count when
   * {@code distinctField} is set. An index written before those columns existed falls back to one
   * filtered scan per band, where the distinct collect reads stored fields and the
   * distinct-cardinality warn fires at most once per call. OpenSearch uses a single range aggregation.
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
      // Single columnar pass over all bands when both columns carry doc values; the per-band filtered
      // searches below remain for an index written before those columns existed.
      if (bandsAreColumnar(bucketField, distinctField)) {
        BandedDistinctDocValuesCollector collector =
            new BandedDistinctDocValuesCollector(bucketField, distinctField, ranges);
        searcher.search(withRbac(query), collector);
        return new MetricAggregationResult(total, collector.bandCounts());
      }
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
   * Lucene runs one filtered grouped-distinct collect per band, each reading doc-values columns when the
   * group and distinct fields carry them (stored fields otherwise). OpenSearch uses a single nested
   * range→terms→cardinality aggregation.
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
      boolean columnar = groupedDistinctIsColumnar(groupField, distinctField);
      for (Map.Entry<String, int[]> band : bands.entrySet()) {
        int[] bounds = band.getValue();
        Query bandFilter = IntPoint.newRangeQuery(bandField, bounds[0], bounds[1]);
        GroupedDistinctPass pass = runGroupedDistinctPass(
            withRbacAndFilter(query, bandFilter), groupField, distinctField, groupValues, columnar);
        pass.groupCounts()
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
