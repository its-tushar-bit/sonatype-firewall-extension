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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
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
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.CheckIndex.CheckIndexException;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexFormatTooNewException;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.MergePolicy.MergeException;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.index.TwoPhaseCommitTool.CommitFailException;
import org.apache.lucene.index.TwoPhaseCommitTool.PrepareCommitFailException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.IndexSearcher.TooManyClauses;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits.Relation;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.LockReleaseFailedException;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.ThreadInterruptedException;

import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.ORGANIZATION_ID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lucene support for {@link SearchIndexClient}
 */
public class LuceneSearchIndexClient
    extends AbstractSearchIndexClient
{
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

  @Inject
  public LuceneSearchIndexClient(
      final ApplicationDAO applicationDAO,
      final LabelDAO labelDAO,
      final OrganizationDAO organizationDAO,
      final OwnerDAO ownerDAO,
      final PolicyDAO policyDAO,
      final TagDAO tagDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final DocumentBuilderHelper documentBuilderHelper,
      final ProductLicense productLicense,
      final TelemetrySender telemetrySender,
      final SearchIndexChangeDAO searchIndexChangeDAO,
      final LuceneComponents luceneComponents,
      final InsightWork insightWork,
      final AdvancedSearchTelemetryMetrics advancedSearchTelemetryMetrics,
      final Configuration configuration,
      final PermissionService permissionService,
      final CurrentUser currentUser,
      final ConversionHelper conversionHelper,
      final ShutdownHandler shutdownHandler)
  {
    super(applicationDAO, labelDAO, organizationDAO, ownerDAO, policyDAO, searchIndexChangeDAO,
        tagDAO, thirdPartySbomMetadataDAO, documentBuilderHelper, productLicense, telemetrySender, luceneComponents,
        advancedSearchTelemetryMetrics, configuration, permissionService, currentUser, conversionHelper,
        shutdownHandler);
    this.insightWork = insightWork;
  }

  @Override
  public void populateIndex() {
    log.info("creating search index...");
    long start = System.currentTimeMillis();
    try (Directory directory = luceneComponents.openSearchIndex(false);
        IndexWriter indexWriter = newIndexWriter(directory, OpenMode.CREATE))
    {
      doPopulateIndex(new LuceneIndexingContext(ownerDAO, indexWriter, conversionHelper));
      indexWriter.commit();
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
    try (Directory directory = luceneComponents.openSearchIndex(false);
        IndexWriter indexWriter = newIndexWriter(directory, OpenMode.CREATE_OR_APPEND))
    {
      processSearchIndexChanges(searchIndexChanges, new LuceneIndexingContext(ownerDAO, indexWriter, conversionHelper),
          deletionCallback);
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

  private IndexWriter newIndexWriter(final Directory directory, final OpenMode openMode) throws IOException {
    return new IndexWriter(directory,
        new IndexWriterConfig(luceneComponents.newAnalyzerForSearch()).setOpenMode(openMode));
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

    try (Directory directory = openSearchIndex();
        IndexReader indexReader = DirectoryReader.open(directory))
    {
      AuditData.get()
          .setData("searchQuery", searchQuery)
          .setData("searchPageSize", pageSize)
          .setData("searchPageIndex", finalPage - 1);

      String initialQuery = createInitialQuery(searchQuery, allComponents);
      Set<String> fieldNames = getFieldNames(initialQuery);
      populateTelemetry(initialSearch, fieldNames);
      checkFieldNames(fieldNames);
      String finalQuery = createFinalQuery(initialQuery, isSbomManagerMode);

      // Passing 0 to IndexSearcher#search throws IllegalArgumentException with 'numHits must be > 0'
      IndexSearcher indexSearcher = new IndexSearcher(indexReader);
      Query query = conversionHelper.stringToQuery(finalQuery);
      TopDocs topDocs = indexSearcher.search(query, Math.max(1, indexReader.maxDoc()));

      SearchResultDTO searchResultDTO = new SearchResultDTO();
      searchResultDTO.searchQuery = searchQuery;
      searchResultDTO.page = finalPage;
      searchResultDTO.pageSize = pageSize;
      groupDocuments(indexSearcher, topDocs.scoreDocs, finalPage, pageSize, searchResultDTO,
          getGroupFieldNamesByItemType(fieldNames));
      searchResultDTO.totalNumberOfHits = (int) topDocs.totalHits.value;
      searchResultDTO.isExactTotalNumberOfHits = topDocs.totalHits.relation == Relation.EQUAL_TO;

      AuditData.get().setData("resultRecordCount", searchResultDTO.countSearchResults());
      return searchResultDTO;
    }
    catch (Exception e) {
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
    // Opens its own Directory/IndexReader per call. Sharing a reader with
    // aggregateCountByField is unnecessary — aggregateCountByField already reuses one reader
    // for the total and all bucket counts in a single request.
    updateMaxQueryClauseCount();

    try (Directory directory = openSearchIndex();
        IndexReader indexReader = DirectoryReader.open(directory))
    {
      IndexSearcher indexSearcher = new IndexSearcher(indexReader);
      return countWithSearcher(indexSearcher, metricQuery, null, buildRbacFilterQuery());
    }
    catch (Exception e) {
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

  private long countWithSearcher(
      final IndexSearcher indexSearcher,
      final String metricQuery,
      final Query extraFilter,
      final Query rbac) throws Exception
  {
    String initialQuery = createInitialQuery(metricQuery, true);
    Set<String> fieldNames = getFieldNames(initialQuery);
    checkFieldNames(fieldNames);
    Query metric = conversionHelper.stringToQuery(initialQuery);
    BooleanQuery.Builder combined = new BooleanQuery.Builder();
    combined.add(metric, BooleanClause.Occur.MUST);
    combined.add(rbac, BooleanClause.Occur.FILTER);
    if (extraFilter != null) {
      combined.add(extraFilter, BooleanClause.Occur.FILTER);
    }
    return indexSearcher.count(combined.build());
  }

  private Query buildRbacFilterQuery() {
    Optional<Map<String, OwnerType>> readableContexts = resolveReadableContextIdsForCurrentUser();
    if (readableContexts.isEmpty()) {
      return new MatchAllDocsQuery();
    }

    Map<String, OwnerType> contextIdsWithReadPermissionMap = readableContexts.get();
    if (contextIdsWithReadPermissionMap.isEmpty()) {
      return new MatchNoDocsQuery();
    }

    List<BytesRef> applicationTerms = new ArrayList<>();
    List<BytesRef> organizationTerms = new ArrayList<>();
    contextIdsWithReadPermissionMap.forEach((contextId, type) -> {
      BytesRef term = new BytesRef(contextId.toLowerCase(Locale.ROOT));
      if (OwnerType.APPLICATION.equals(type)) {
        applicationTerms.add(term);
      }
      else if (OwnerType.ORGANIZATION.equals(type)) {
        organizationTerms.add(term);
      }
    });

    // Explicit fail-closed: if the user's readable contexts are all non-APPLICATION/
    // non-ORGANIZATION types (e.g. a Firewall-only user with only REPOSITORY*), both
    // term lists are empty. Return MatchNoDocs explicitly rather than relying on a
    // zero-should BooleanQuery + minimumShouldMatch=1 reading as match-none (kept in
    // lockstep with the OpenSearch sibling).
    if (applicationTerms.isEmpty() && organizationTerms.isEmpty()) {
      return new MatchNoDocsQuery();
    }

    BooleanQuery.Builder rbac = new BooleanQuery.Builder();
    if (!applicationTerms.isEmpty()) {
      rbac.add(new TermInSetQuery(APPLICATION_ID.label, applicationTerms), BooleanClause.Occur.SHOULD);
    }
    if (!organizationTerms.isEmpty()) {
      rbac.add(new TermInSetQuery(ORGANIZATION_ID.label, organizationTerms), BooleanClause.Occur.SHOULD);
    }
    rbac.setMinimumNumberShouldMatch(1);
    return rbac.build();
  }
}
