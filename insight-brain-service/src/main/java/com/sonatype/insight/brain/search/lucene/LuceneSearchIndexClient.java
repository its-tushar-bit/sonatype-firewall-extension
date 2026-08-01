/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicLong;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.search.global.GlobalSearchRequest;
import com.sonatype.insight.brain.search.global.GlobalSearchResult;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.session.ReadableContextAuthzCache;
import com.sonatype.insight.brain.security.AuthorizationChecker;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;

import jakarta.inject.Inject;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.CheckIndex.CheckIndexException;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexFormatTooNewException;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MergePolicy.MergeException;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.TwoPhaseCommitTool.CommitFailException;
import org.apache.lucene.index.TwoPhaseCommitTool.PrepareCommitFailException;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.IndexSearcher.TooManyClauses;
import static org.apache.lucene.search.BooleanClause.Occur.FILTER;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollectorManager;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.search.TotalHits.Relation;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.LockReleaseFailedException;
import org.apache.lucene.util.ThreadInterruptedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lucene support for {@link SearchIndexClient}
 */
public class LuceneSearchIndexClient
    extends AbstractSearchIndexClient
{
  public static final String BACKEND_ID = "lucene";

  private static final Logger log = LoggerFactory.getLogger(LuceneSearchIndexClient.class);

  private static final Set<Class<?>> SYSTEMIC_LUCENE_EXCEPTIONS = Set.of(
      AlreadyClosedException.class,
      CheckIndexException.class,
      CommitFailException.class,
      CorruptIndexException.class,
      FileNotFoundException.class,
      FileSystemException.class,
      IndexFormatTooNewException.class,
      IndexFormatTooOldException.class,
      LockObtainFailedException.class,
      LockReleaseFailedException.class,
      MergeException.class,
      PrepareCommitFailException.class,
      ThreadInterruptedException.class);

  private static final Set<String> SYSTEMIC_LUCENE_LOWERCASE_EXCEPTION_MESSAGES = Set.of(
      "access denied",
      "access is denied",
      "no space left",
      "not enough space",
      "permission denied",
      "too many open files");

  private final InsightWork insightWork;

  private final LuceneIndexWriterOwner indexWriterOwner;

  private final AtomicLong lastSearcherManagerUnavailableWarnMillis = new AtomicLong();

  private static final long SEARCHER_MANAGER_UNAVAILABLE_WARN_INTERVAL_MILLIS = 60_000L;

  @Inject
  public LuceneSearchIndexClient(
      final ApplicationDAO applicationDAO,
      final LabelDAO labelDAO,
      final OrganizationDAO organizationDAO,
      final OwnerDAO ownerDAO,
      final PolicyDAO policyDAO,
      final PolicyWaiverDAO policyWaiverDAO,
      final AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      final TagDAO tagDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final DocumentBuilderHelper documentBuilderHelper,
      final ProductLicense productLicense,
      final TelemetrySender telemetrySender,
      final SearchIndexChangeDAO searchIndexChangeDAO,
      final LuceneComponents luceneComponents,
      final LuceneIndexWriterOwner indexWriterOwner,
      final InsightWork insightWork,
      final AdvancedSearchTelemetryMetrics advancedSearchTelemetryMetrics,
      final Configuration configuration,
      final PermissionService permissionService,
      final AuthorizationChecker authorizationChecker,
      final CurrentUser currentUser,
      final ConversionHelper conversionHelper,
      final ShutdownHandler shutdownHandler,
      final ReadableContextAuthzCache readableContextAuthzCache)
  {
    super(applicationDAO, labelDAO, organizationDAO, ownerDAO, policyDAO, policyWaiverDAO, autoPolicyWaiverDAO,
        searchIndexChangeDAO,
        tagDAO, thirdPartySbomMetadataDAO, documentBuilderHelper, productLicense, telemetrySender, luceneComponents,
        advancedSearchTelemetryMetrics, configuration, permissionService, authorizationChecker, currentUser,
        conversionHelper, shutdownHandler, readableContextAuthzCache);
    this.insightWork = insightWork;
    this.indexWriterOwner = indexWriterOwner;
  }

  @Override
  public void populateIndex() {
    log.info("creating search index...");
    long start = System.currentTimeMillis();
    try {
      indexWriterOwner.rebuildExclusive(
          () -> doPopulateIndex(new LuceneIndexingContext(ownerDAO, indexWriterOwner.getWriter(), conversionHelper)));
      log.info("all indexing complete");
    }
    catch (Exception e) {
      throw new SearchIndexException("Error creating search index", e);
    }
    sendAdvancedSearchIndexingTelemetry(System.currentTimeMillis() - start);
    log.info("index creation exit");
  }

  @Override
  public long getIndexSize() {
    try (Directory indexDir = luceneComponents.openSearchIndex(true)) {
      long bytes = 0;
      if (indexDir != null) {
        for (String filename : indexDir.listAll()) {
          bytes += indexDir.fileLength(filename);
        }
      }
      return bytes;
    }
    catch (Exception e) {
      throw new SearchIndexException("Error getting search index size", e);
    }
  }

  @Override
  public void updateIndex(
      final List<SearchIndexChange> searchIndexChanges,
      final Consumer<SearchIndexChange> deletionCallback)
  {
    if (searchIndexChanges.isEmpty()) {
      return;
    }
    try {
      indexWriterOwner.runWithWriter(writer -> {
        processSearchIndexChanges(searchIndexChanges,
            new LuceneIndexingContext(ownerDAO, writer, conversionHelper),
            deletionCallback);
      });
    }
    catch (Exception e) {
      if (shouldThrow(e)) {
        throw new SearchIndexException("Error updating the search index", e);
      }
      log.debug("Unable to update the search index");
    }
  }

  @Override
  public Long getLastIndexTime() {
    try (Directory directory = luceneComponents.openSearchIndex(true)) {
      if (directory == null) {
        return null;
      }
      String lastCommitSegmentsFileName = SegmentInfos.getLastCommitSegmentsFileName(directory);
      if (lastCommitSegmentsFileName == null) {
        return null;
      }
      return new File(insightWork.getSearchIndexDir(), lastCommitSegmentsFileName).lastModified();
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }

  @Override
  public SearchResultDTO searchIndex(
      final String searchQuery,
      final int pageSize,
      final int page,
      final boolean allComponents,
      final boolean isSbomManagerMode,
      final List<String> searchAfter) throws SearchIndexException
  {
    checkMode(isSbomManagerMode);

    boolean initialSearch = false;
    int finalPage = page;
    if (page == 0) {
      // when actually paging through the results, a positive page index is used
      // 0 denotes first page of new search
      finalPage = 1;
      initialSearch = true;
    }

    updateMaxQueryClauseCount();

    try {
      Optional<LuceneSearcherManagerHolder> searcherManagerHolder = getAvailableSearcherManagerHolder();
      if (searcherManagerHolder.isPresent()) {
        LuceneReaderTiming.startAcquisition();
        try {
          IndexSearcher indexSearcher = searcherManagerHolder.get().acquire();
          try {
            LuceneReaderTiming.endAcquisition();
            return searchIndexWithSearcher(searchQuery, pageSize, allComponents, isSbomManagerMode,
                initialSearch, finalPage, indexSearcher);
          }
          finally {
            searcherManagerHolder.get().release(indexSearcher);
          }
        }
        catch (IOException e) {
          LuceneReaderTiming.abort();
          if (!isSearcherManagerUnavailable(e)) {
            throw e;
          }
          log.debug("SearcherManager unavailable during rebuild/pause; falling back to DirectoryReader", e);
        }
      }

      LuceneReaderTiming.startAcquisition();
      try (Directory directory = openSearchIndex();
          IndexReader indexReader = DirectoryReader.open(directory))
      {
        LuceneReaderTiming.endAcquisition();
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        return searchIndexWithSearcher(searchQuery, pageSize, allComponents, isSbomManagerMode,
            initialSearch, finalPage, indexSearcher);
      }
    }
    catch (Exception e) {
      LuceneReaderTiming.abort();
      if (e instanceof TooManyClauses) {
        throw TOO_MANY_CLAUSES_EXCEPTION;
      }
      if (e instanceof BadRequestException badRequestException) {
        throw badRequestException;
      }
      if (e instanceof ConflictException conflictException) {
        throw conflictException;
      }
      throw new SearchIndexException(e);
    }
  }

  @Override
  public String backendId() {
    return BACKEND_ID;
  }

  /** {@code request.baseQuery()} MUST already be permission-wrapped; this runs it verbatim. */
  @Override
  public GlobalSearchResult searchGlobal(final GlobalSearchRequest request) {
    if (!isGlobalSearchEnabled()) {
      throw new ConflictException("Global Search is disabled");
    }
    updateMaxQueryClauseCount();
    try (Directory directory = openSearchIndex();
        IndexReader indexReader = DirectoryReader.open(directory))
    {
      IndexSearcher indexSearcher = new IndexSearcher(indexReader);
      int pageSize = request.pageSize();
      AuditData.get()
          .setData("searchPageSize", pageSize)
          .setData("searchSort", describeSort(request.sort()))
          .setData("searchAfterPresent", !request.searchAfter().isEmpty())
          .setData("searchBackend", BACKEND_ID);
      int collectCap = Math.max(1, pageSize + 1);
      int totalHitsThreshold = AbstractSearchIndexClient.GLOBAL_SEARCH_TRACK_TOTAL_HITS_CAP;
      Sort sort = effectiveSort(request.sort());
      List<String> searchAfter = request.searchAfter();

      FieldDoc after = searchAfter.isEmpty() ? null : decodeFieldDocAfter(searchAfter, sort);
      TopDocs topDocs = indexSearcher.search(request.baseQuery(),
          new TopFieldCollectorManager(sort, collectCap, after, totalHitsThreshold, true));

      ScoreDoc[] hits = topDocs.scoreDocs;
      int returnCount = Math.min(hits.length, pageSize);
      List<SearchResultItemDTO> rows = new ArrayList<>(returnCount);
      for (int i = 0; i < returnCount; i++) {
        Document doc = indexSearcher.storedFields().document(hits[i].doc);
        rows.add(new SearchResultItemDTO(doc));
      }

      List<String> nextSearchAfter = List.of();
      if (hits.length > pageSize && returnCount > 0) {
        ScoreDoc last = hits[returnCount - 1];
        nextSearchAfter = encodeNextSearchAfter(last, sort);
      }

      long capped = AbstractSearchIndexClient.capTotalHitsForGlobalSearch(topDocs.totalHits.value);
      boolean exactTotal = topDocs.totalHits.relation == Relation.EQUAL_TO;
      AuditData.get().setData("resultRecordCount", rows.size());
      return new GlobalSearchResult(rows, capped, nextSearchAfter, exactTotal);
    }
    catch (SearchIndexException searchIndexException) {
      throw searchIndexException;
    }
    catch (Exception e) {
      throw mapSearchException(e);
    }
  }

  /** Appends the per-doc-unique {@link FieldIdentifier#DOCUMENT_KEY} tie-breaker for a stable searchAfter cursor. */
  private static Sort effectiveSort(final Sort requestSort) {
    SortField docKey = new SortField(FieldIdentifier.DOCUMENT_KEY.label, SortField.Type.STRING);
    if (requestSort == null) {
      return new Sort(SortField.FIELD_SCORE, docKey);
    }
    SortField[] base = requestSort.getSort();
    SortField[] withKey = Arrays.copyOf(base, base.length + 1);
    withKey[base.length] = docKey;
    return new Sort(withKey);
  }

  private static FieldDoc decodeFieldDocAfter(final List<String> searchAfter, final Sort sort) {
    SortField[] sortFields = sort.getSort();
    if (searchAfter.size() != sortFields.length) {
      throw new BadRequestException("Invalid searchAfter tuple for Global Search sort.");
    }
    Object[] fieldValues = new Object[sortFields.length];
    for (int i = 0; i < sortFields.length; i++) {
      fieldValues[i] = decodeSortValue(sortFields[i], searchAfter.get(i));
    }
    // Sentinel docId: the DOCUMENT_KEY tuple slot is the tie-breaker, so Lucene never consults it.
    return new FieldDoc(Integer.MAX_VALUE, Float.NaN, fieldValues);
  }

  /**
   * {@link SortedNumericSortField#getType()} is always {@link SortField.Type#CUSTOM}; cursor
   * encode/decode must use {@link SortedNumericSortField#getNumericType()} (LONG/FLOAT/…) so
   * Ana/index-query sorts (created-at, threat level, latest evaluation) can mint {@code nextSearchAfter}.
   */
  static SortField.Type sortValueType(final SortField sortField) {
    if (sortField instanceof SortedNumericSortField sortedNumeric) {
      return sortedNumeric.getNumericType();
    }
    return sortField.getType();
  }

  private static Object decodeSortValue(final SortField sortField, final String raw) {
    SortField.Type type = sortValueType(sortField);
    try {
      return switch (type) {
        case STRING, STRING_VAL -> new BytesRef(raw);
        case LONG -> "MIN".equals(raw) ? Long.MIN_VALUE : Long.parseLong(raw);
        case INT -> "MIN".equals(raw) ? Integer.MIN_VALUE : Integer.parseInt(raw);
        case FLOAT, SCORE -> "NaN".equals(raw) ? Float.NaN : Float.parseFloat(raw);
        case DOUBLE -> "NaN".equals(raw) ? Double.NaN : Double.parseDouble(raw);
        default -> throw new BadRequestException("Unsupported SortField.Type in searchAfter: " + type);
      };
    }
    catch (NumberFormatException nfe) {
      throw new BadRequestException("Invalid searchAfter value for SortField.Type " + type + ": " + raw);
    }
  }

  private static List<String> encodeNextSearchAfter(final ScoreDoc last, final Sort sort) {
    List<String> out = new ArrayList<>();
    SortField[] sortFields = sort.getSort();
    if (last instanceof FieldDoc fieldDoc && fieldDoc.fields != null) {
      for (int i = 0; i < fieldDoc.fields.length; i++) {
        out.add(encodeSortValue(sortFields[i], fieldDoc.fields[i]));
      }
    }
    return out;
  }

  /** Encodes a sort-tuple slot; null encodes to a re-decodable sentinel so a null boundary round-trips. */
  static String encodeSortValue(final SortField sortField, final Object v) {
    SortField.Type type = sortValueType(sortField);
    if (v == null) {
      return switch (type) {
        case LONG, INT -> "MIN";
        case FLOAT, DOUBLE, SCORE -> "NaN";
        case STRING, STRING_VAL -> "";
        default -> throw new BadRequestException("Unsupported SortField.Type in searchAfter: " + type);
      };
    }
    return switch (type) {
      case STRING, STRING_VAL -> v instanceof BytesRef br ? br.utf8ToString() : v.toString();
      case LONG, INT, FLOAT, DOUBLE, SCORE -> v.toString();
      default -> throw new BadRequestException("Unsupported SortField.Type in searchAfter: " + type);
    };
  }

  // Update the static setting within lucene for the max query clause count, based on the current value in the
  // configuration
  @Override
  protected void updateMaxQueryClauseCount() {
    IndexSearcher.setMaxClauseCount(configuration.getMaxAdvancedSearchClauseCount());
  }

  private void groupDocuments(
      final IndexSearcher indexSearcher,
      final ScoreDoc[] scoreDocs,
      final int page,
      final int pageSize,
      final SearchResultDTO searchResultDTO,
      final Map<String, String> groupFieldNamesByItemType)
  {
    int startIndex = (page - 1) * pageSize;
    int endIndex = page * pageSize;
    Supplier<Document> documentSupplier = new Supplier<>()
    {
      private int currentIndex = startIndex;

      @Override
      public Document get() {
        if (currentIndex < endIndex && currentIndex < scoreDocs.length) {
          try {
            LuceneReaderTiming.recordStoredFieldDocumentLoad();
            return indexSearcher.storedFields().document(scoreDocs[currentIndex++].doc);
          }
          catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        }
        return null;
      }
    };
    groupDocuments(page, pageSize, documentSupplier, searchResultDTO, groupFieldNamesByItemType);
  }

  private SearchResultDTO searchIndexWithSearcher(
      final String searchQuery,
      final int pageSize,
      final boolean allComponents,
      final boolean isSbomManagerMode,
      final boolean initialSearch,
      final int finalPage,
      final IndexSearcher indexSearcher) throws Exception
  {
    LuceneReaderTiming.startExecution();
    LuceneReaderTiming.startQueryBuild();
    AuditData.get()
        .setData("searchQuery", searchQuery)
        .setData("searchPageSize", pageSize)
        .setData("searchPageIndex", finalPage - 1);

    String initialQuery = createInitialQuery(searchQuery, allComponents);
    Set<String> fieldNames = getFieldNames(initialQuery);
    populateTelemetry(initialSearch, fieldNames);
    checkFieldNames(fieldNames);
    FinalQueryWithRbacMeta finalQueryMeta = createFinalQueryWithRbacMeta(initialQuery, isSbomManagerMode);
    String finalQuery = finalQueryMeta.query();
    LuceneReaderTiming.endQueryBuild(finalQuery, finalQueryMeta.rbacContextCount());

    // Passing 0 to IndexSearcher#search throws IllegalArgumentException with 'numHits must be > 0'
    LuceneReaderTiming.startQueryParse();
    Query query = conversionHelper.stringToQuery(finalQuery);
    LuceneReaderTiming.endQueryParse();
    int collectN = Math.max(1, indexSearcher.getIndexReader().maxDoc());
    LuceneReaderTiming.startSearch();
    TopDocs topDocs = indexSearcher.search(query, collectN);
    LuceneReaderTiming.endSearch(collectN, topDocs.scoreDocs.length, topDocs.totalHits.value);

    SearchResultDTO searchResultDTO = new SearchResultDTO();
    searchResultDTO.searchQuery = searchQuery;
    searchResultDTO.page = finalPage;
    searchResultDTO.pageSize = pageSize;
    LuceneReaderTiming.startGroupDocuments();
    groupDocuments(indexSearcher, topDocs.scoreDocs, finalPage, pageSize, searchResultDTO,
        getGroupFieldNamesByItemType(fieldNames));
    LuceneReaderTiming.endGroupDocuments();
    searchResultDTO.totalNumberOfHits = (int) topDocs.totalHits.value;
    searchResultDTO.isExactTotalNumberOfHits = topDocs.totalHits.relation == Relation.EQUAL_TO;

    AuditData.get().setData("resultRecordCount", searchResultDTO.countSearchResults());
    LuceneReaderTiming.endExecution();
    return searchResultDTO;
  }

  private Directory openSearchIndex() {
    try {
      Directory directory = luceneComponents.openSearchIndex(true);
      if (directory == null || !DirectoryReader.indexExists(directory)) {
        if (directory != null) {
          directory.close();
        }
        throw new ConflictException(NO_INDEX_ERROR_MESSAGE);
      }
      return directory;
    }
    catch (IOException e) {
      throw new ConflictException(NO_INDEX_ERROR_MESSAGE, e);
    }
  }

  @Override
  protected boolean isChangeSpecificError(final Exception e) {
    return isCommonChangeSpecificError(e);
  }

  @Override
  protected boolean isSystemicError(final Exception e) {
    return isCommonSystemicError(e) || hasCauseOrMessage(e, cause -> {
      if (SYSTEMIC_LUCENE_EXCEPTIONS.contains(cause.getClass())) {
        return true;
      }
      String msg = cause.getMessage();
      if (msg != null) {
        String lowerMsg = msg.toLowerCase();
        return SYSTEMIC_LUCENE_LOWERCASE_EXCEPTION_MESSAGES.stream().anyMatch(lowerMsg::contains);
      }
      return false;
    });
  }

  @Override
  public long count(final String metricQuery) {
    updateMaxQueryClauseCount();

    Optional<LuceneSearcherManagerHolder> searcherManagerHolder = getAvailableSearcherManagerHolder();
    if (searcherManagerHolder.isPresent()) {
      LuceneReaderTiming.startAcquisition();
      try {
        IndexSearcher indexSearcher = searcherManagerHolder.get().acquire();
        try {
          LuceneReaderTiming.endAcquisition();
          LuceneReaderTiming.startExecution();
          long result = countWithSearcher(indexSearcher, metricQuery, null, buildRbacFilterQuery());
          LuceneReaderTiming.endExecution();
          return result;
        }
        finally {
          searcherManagerHolder.get().release(indexSearcher);
        }
      }
      catch (Exception e) {
        LuceneReaderTiming.abort();
        if (!(e instanceof IOException ioException) || !isSearcherManagerUnavailable(ioException)) {
          throw mapSearchException(e);
        }
        log.debug("SearcherManager unavailable during rebuild/pause; falling back to DirectoryReader", e);
      }
    }

    LuceneReaderTiming.startAcquisition();
    try (Directory directory = openSearchIndex();
        IndexReader indexReader = DirectoryReader.open(directory))
    {
      LuceneReaderTiming.endAcquisition();
      LuceneReaderTiming.startExecution();
      IndexSearcher indexSearcher = new IndexSearcher(indexReader);
      long result = countWithSearcher(indexSearcher, metricQuery, null, buildRbacFilterQuery());
      LuceneReaderTiming.endExecution();
      return result;
    }
    catch (Exception e) {
      LuceneReaderTiming.abort();
      throw mapSearchException(e);
    }
  }

  private RuntimeException mapSearchException(final Exception e) {
    if (e instanceof TooManyClauses) {
      return TOO_MANY_CLAUSES_EXCEPTION;
    }
    if (e instanceof BadRequestException badRequestException) {
      return badRequestException;
    }
    if (e instanceof ConflictException conflictException) {
      return conflictException;
    }
    return new SearchIndexException(e);
  }

  @Override
  public MetricAggregationResult aggregateCountByField(
      final String metricQuery,
      final String bucketField,
      final Map<String, int[]> ranges)
  {
    validateRangeBounds(ranges);
    updateMaxQueryClauseCount();

    try (Directory directory = openSearchIndex();
        IndexReader indexReader = DirectoryReader.open(directory))
    {
      IndexSearcher indexSearcher = new IndexSearcher(indexReader);
      // RBAC is identical across total + bucket counts — resolve once per request.
      Query rbac = buildRbacFilterQuery();
      long total = countWithSearcher(indexSearcher, metricQuery, null, rbac);
      Map<String, Long> buckets = new LinkedHashMap<>();
      for (Map.Entry<String, int[]> entry : ranges.entrySet()) {
        int[] bounds = entry.getValue();
        // Build the numeric range programmatically (matching the OpenSearch sibling's
        // AggregationRange) rather than concatenating bounds + bucketField into a
        // re-parsed query string — the same string-interpolation footgun this client
        // deliberately avoids for the RBAC filter (programmatic TermInSetQuery).
        Query bandFilter = IntPoint.newRangeQuery(bucketField, bounds[0], bounds[1]);
        buckets.put(entry.getKey(), countWithSearcher(indexSearcher, metricQuery, bandFilter, rbac));
      }
      return new MetricAggregationResult(total, buckets);
    }
    catch (Exception e) {
      throw mapSearchException(e);
    }
  }

  @Override
  public MetricAggregationResult aggregateCountByFloatField(
      final String metricQuery,
      final String bucketField,
      final Map<String, float[]> ranges,
      final String distinctField)
  {
    validateFloatRangeBounds(ranges);
    if (distinctField != null) {
      checkFieldNames(new HashSet<>(List.of(bucketField, distinctField)));
    }
    updateMaxQueryClauseCount();

    try (Directory directory = openSearchIndex();
        IndexReader indexReader = DirectoryReader.open(directory))
    {
      IndexSearcher indexSearcher = new IndexSearcher(indexReader);
      // RBAC is identical across total + bucket counts — resolve once per request.
      Query rbac = buildRbacFilterQuery();
      long total = countWithSearcher(indexSearcher, metricQuery, null, rbac);
      Map<String, Long> buckets = new LinkedHashMap<>();
      for (Map.Entry<String, float[]> entry : ranges.entrySet()) {
        float[] bounds = entry.getValue();
        // Half-open [minInclusive, maxExclusive): step the upper bound down to the previous
        // representable float so FloatPoint's inclusive newRangeQuery excludes it (the same
        // nextDown trick QueryCompiler uses for exclusive float range bounds). This keeps a CVSS
        // boundary value (e.g. 7.0) in exactly one band across both backends. Build the range
        // programmatically (not by concatenating bounds into a re-parsed query string) — the same
        // string-interpolation footgun this client deliberately avoids for the RBAC filter.
        Query bandFilter = FloatPoint.newRangeQuery(bucketField, bounds[0], FloatPoint.nextDown(bounds[1]));
        long count = distinctField == null
            ? countWithSearcher(indexSearcher, metricQuery, bandFilter, rbac)
            : countDistinctWithSearcher(indexSearcher, metricQuery, List.of(distinctField), bandFilter, rbac);
        buckets.put(entry.getKey(), count);
      }
      return new MetricAggregationResult(total, buckets);
    }
    catch (Exception e) {
      throw mapSearchException(e);
    }
  }

  @Override
  public long countDistinct(final String metricQuery, final List<String> compositeKeyFields) {
    validateCompositeKeyFields(compositeKeyFields);
    checkFieldNames(new HashSet<>(compositeKeyFields));
    updateMaxQueryClauseCount();

    try (Directory directory = openSearchIndex();
        IndexReader indexReader = DirectoryReader.open(directory))
    {
      IndexSearcher indexSearcher = new IndexSearcher(indexReader);
      Query rbac = buildRbacFilterQuery();
      return countDistinctWithSearcher(indexSearcher, metricQuery, compositeKeyFields, null, rbac);
    }
    catch (Exception e) {
      throw mapSearchException(e);
    }
  }

  @Override
  public Map<String, Long> countDistinctGroupedBy(
      final String metricQuery,
      final String groupField,
      final String distinctField,
      final Collection<String> groupValues)
  {
    checkFieldNames(new HashSet<>(List.of(groupField, distinctField)));
    if (groupValues == null || groupValues.isEmpty()) {
      return Map.of();
    }
    updateMaxQueryClauseCount();
    try (Directory directory = openSearchIndex();
        IndexReader indexReader = DirectoryReader.open(directory))
    {
      IndexSearcher indexSearcher = new IndexSearcher(indexReader);
      Query query = buildRbacFilteredMetricQuery(metricQuery, null, buildRbacFilterQuery());
      DistinctGroupedStoredFieldCollector collector = new DistinctGroupedStoredFieldCollector(
          indexSearcher.storedFields(), groupField, distinctField, groupValues);
      indexSearcher.search(query, collector);
      return collector.groupCounts();
    }
    catch (Exception e) {
      throw mapSearchException(e);
    }
  }

  @Override
  public Map<String, Map<String, Long>> countDistinctGroupedByBands(
      final String metricQuery,
      final String groupField,
      final String distinctField,
      final Collection<String> groupValues,
      final String bandField,
      final Map<String, int[]> bands)
  {
    checkFieldNames(new HashSet<>(List.of(groupField, distinctField, bandField)));
    validateRangeBounds(bands);
    if (groupValues == null || groupValues.isEmpty() || bands == null || bands.isEmpty()) {
      return Map.of();
    }
    updateMaxQueryClauseCount();
    try (Directory directory = openSearchIndex();
        IndexReader indexReader = DirectoryReader.open(directory))
    {
      IndexSearcher indexSearcher = new IndexSearcher(indexReader);
      Query rbac = buildRbacFilterQuery();
      Map<String, Map<String, Long>> byGroup = new LinkedHashMap<>();
      for (Map.Entry<String, int[]> band : bands.entrySet()) {
        int[] bounds = band.getValue();
        // Programmatic int range (matching aggregateCountByField / the OpenSearch sibling), never a
        // string-interpolated band clause fed back through the query parser.
        Query bandFilter = IntPoint.newRangeQuery(bandField, bounds[0], bounds[1]);
        Query query = buildRbacFilteredMetricQuery(metricQuery, bandFilter, rbac);
        DistinctGroupedStoredFieldCollector collector = new DistinctGroupedStoredFieldCollector(
            indexSearcher.storedFields(), groupField, distinctField, groupValues);
        indexSearcher.search(query, collector);
        collector.groupCounts()
            .forEach(
                (group, count) -> byGroup.computeIfAbsent(group, g -> new LinkedHashMap<>()).put(band.getKey(), count));
      }
      return byGroup;
    }
    catch (Exception e) {
      throw mapSearchException(e);
    }
  }

  private long countWithSearcher(
      final IndexSearcher indexSearcher,
      final String metricQuery,
      final Query extraFilter,
      final Query rbac) throws Exception
  {
    return indexSearcher.count(buildRbacFilteredMetricQuery(metricQuery, extraFilter, rbac));
  }

  private Optional<LuceneSearcherManagerHolder> getAvailableSearcherManagerHolder() {
    try {
      // NRT SearcherManager can keep serving after the on-disk index directory is deleted
      // (open file handles / in-memory readers). Fall through to openSearchIndex() so callers
      // still get ConflictException/409 when the index is gone.
      if (!Files.exists(insightWork.getSearchIndexDir().toPath())) {
        return Optional.empty();
      }
      LuceneSearcherManagerHolder holder = indexWriterOwner.tryGetSearcherManagerHolder().orElse(null);
      if (holder != null) {
        return Optional.of(holder);
      }
      log.debug("Lucene SearcherManager holder is unavailable; falling back to DirectoryReader");
      return Optional.empty();
    }
    catch (SearchIndexException e) {
      long now = System.currentTimeMillis();
      long lastWarn = lastSearcherManagerUnavailableWarnMillis.get();
      if (now - lastWarn >= SEARCHER_MANAGER_UNAVAILABLE_WARN_INTERVAL_MILLIS &&
          lastSearcherManagerUnavailableWarnMillis.compareAndSet(lastWarn, now))
      {
        log.warn("Lucene SearcherManager holder is unavailable; falling back to DirectoryReader until it reopens", e);
      }
      else {
        log.debug("Lucene SearcherManager holder is unavailable; falling back to DirectoryReader", e);
      }
      return Optional.empty();
    }
  }

  private static boolean isSearcherManagerUnavailable(final IOException e) {
    return e instanceof SearcherManagerUnavailableException;
  }

  /**
   * Counts distinct composite keys among the RBAC-filtered documents matching {@code metricQuery} (further
   * narrowed by {@code extraFilter}, e.g. a per-band float-range clause, when non-null). The matching
   * documents are visited via a {@link SimpleCollector}; for each, the stored values of {@code compositeKeyFields}
   * are joined into a single key accumulated in a {@link HashSet}, and the set size is returned. Reuses the same
   * programmatic RBAC FILTER (and {@link MatchNoDocsQuery} fail-closed behavior) as {@link #countWithSearcher}.
   * <p>
   * Note (scale): this materializes one entry per distinct key in memory. At ~1M SECURITY_VULNERABILITY docs this
   * is bounded by the number of distinct (applicationId, componentHash) pairs and is acceptable for current scale;
   * F28 scale certification (CLM-40928) will validate this and switch to a streaming/docvalues approach if needed.
   */
  private long countDistinctWithSearcher(
      final IndexSearcher indexSearcher,
      final String metricQuery,
      final List<String> compositeKeyFields,
      final Query extraFilter,
      final Query rbac) throws Exception
  {
    Query query = buildRbacFilteredMetricQuery(metricQuery, extraFilter, rbac);
    Set<String> fieldsToLoad = new HashSet<>(compositeKeyFields);
    StoredFields storedFields = indexSearcher.storedFields();
    Set<String> distinctKeys = new HashSet<>();
    indexSearcher.search(query, new SimpleCollector()
    {
      private int docBase;

      @Override
      protected void doSetNextReader(final LeafReaderContext context) {
        this.docBase = context.docBase;
      }

      @Override
      public void collect(final int doc) throws IOException {
        Document document = storedFields.document(docBase + doc, fieldsToLoad);
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < compositeKeyFields.size(); i++) {
          if (i > 0) {
            key.append('\u0000');
          }
          String value = document.get(compositeKeyFields.get(i));
          key.append(value == null ? "" : value);
        }
        distinctKeys.add(key.toString());
      }

      @Override
      public ScoreMode scoreMode() {
        return ScoreMode.COMPLETE_NO_SCORES;
      }
    });
    return distinctKeys.size();
  }

  private Query buildRbacFilteredMetricQuery(
      final String metricQuery,
      final Query extraFilter,
      final Query rbac) throws Exception
  {
    String initialQuery = createInitialQuery(metricQuery, true);
    Set<String> fieldNames = getFieldNames(initialQuery);
    checkFieldNames(fieldNames);
    Query metric = conversionHelper.stringToQuery(initialQuery);
    BooleanQuery.Builder combined = new BooleanQuery.Builder();
    combined.add(metric, MUST);
    combined.add(rbac, FILTER);
    if (extraFilter != null) {
      combined.add(extraFilter, FILTER);
    }
    return combined.build();
  }

  private Query buildRbacFilterQuery() {
    return resolveReadableContextRbacFilterForCurrentUser();
  }
}
