/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.global.GlobalSearchRequest;
import com.sonatype.insight.brain.search.global.GlobalSearchResult;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.search.SearchConfig.AwsHttpOpenSearchConfig;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.RankedGroupsResult;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.lucene.LuceneComponents;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.ReadableContextAuthzCache;
import com.sonatype.insight.brain.security.AuthorizationChecker;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;

import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.Script;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.AggregationRange;
import org.opensearch.client.opensearch._types.aggregations.RangeBucket;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.ScoreSort;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.StoreStats;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.pit.CreatePitRequest;
import org.opensearch.client.opensearch.core.pit.CreatePitResponse;
import org.opensearch.client.opensearch.core.pit.DeletePitRequest;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.HitsMetadata;
import org.opensearch.client.opensearch.core.search.Pit;
import org.opensearch.client.opensearch.core.search.SourceConfig;
import org.opensearch.client.opensearch.core.search.TotalHits;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;
import org.opensearch.client.opensearch.core.search.TrackHits;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexResponse;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.GetAliasRequest;
import org.opensearch.client.opensearch.indices.GetAliasResponse;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsRequest;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsResponse;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.IndexState;
import org.opensearch.client.opensearch.indices.IndicesStatsRequest;
import org.opensearch.client.opensearch.indices.IndicesStatsResponse;
import org.opensearch.client.opensearch.indices.UpdateAliasesRequest;
import org.opensearch.client.opensearch.indices.UpdateAliasesResponse;
import org.opensearch.client.opensearch.indices.get_alias.IndexAliases;
import org.opensearch.client.opensearch.indices.stats.IndicesStats;
import org.opensearch.client.opensearch.indices.update_aliases.Action;
import org.opensearch.client.opensearch.indices.update_aliases.AddAction;
import org.opensearch.client.opensearch.indices.update_aliases.RemoveAction;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.ORGANIZATION_ID;

/**
 * OpenSearch support for {@link SearchIndexClient}
 */
public class OpenSearchSearchIndexClient
    extends AbstractSearchIndexClient
{
  public static final String BACKEND_ID = "opensearch";

  private static final Logger log = LoggerFactory.getLogger(OpenSearchSearchIndexClient.class);

  private static final int DEFAULT_MAX_RESULT_WINDOW = 10000;

  private static final Set<Class<?>> SYSTEMIC_NETWORK_EXCEPTIONS = Set.of(
      SocketException.class,
      SocketTimeoutException.class,
      UnknownHostException.class);

  private static final Set<String> SYSTEMIC_OPENSEARCH_LOWERCASE_EXCEPTION_TYPES = Set.of(
      "circuit_breaking_exception",
      "cluster_block_exception",
      "master_not_discovered_exception",
      "no_shard_available_action_exception",
      "unavailable_shards_exception");

  private static final Set<String> SYSTEMIC_OPENSEARCH_LOWERCASE_REASON_FRAGMENTS = Set.of(
      "internal failure",
      "throttling",
      "too many requests");

  private static final Set<String> SYSTEMIC_OPENSEARCH_LOWERCASE_ERROR_MESSAGES = Set.of(
      "connection refused",
      "internal failure",
      "service unavailable",
      "throttling",
      "timeout",
      "unreachable");

  private static final Set<String> CHANGE_SPECIFIC_OPENSEARCH_LOWERCASE_ERROR_MESSAGES = Set.of(
      "document_parsing_exception",
      "illegal_argument_exception",
      "mapper_parsing_exception");

  private final OpenSearchTransport openSearchTransport;

  private final IndexConfigProvider indexConfigProvider;

  private final ClusterLockManager clusterLockManager;

  private volatile OpenSearchClient openSearchClient;

  private final SearchConfig searchConfig;

  @Inject
  public OpenSearchSearchIndexClient(
      final ApplicationDAO applicationDAO,
      final LabelDAO labelDAO,
      final OrganizationDAO organizationDAO,
      final OwnerDAO ownerDAO,
      final PolicyDAO policyDAO,
      final PolicyWaiverDAO policyWaiverDAO,
      final AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      final SearchIndexChangeDAO searchIndexChangeDAO,
      final TagDAO tagDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final DocumentBuilderHelper documentBuilderHelper,
      final ProductLicense productLicense,
      final TelemetrySender telemetrySender,
      final LuceneComponents luceneComponents,
      final AdvancedSearchTelemetryMetrics advancedSearchTelemetryMetrics,
      final Configuration configuration,
      final PermissionService permissionService,
      final AuthorizationChecker authorizationChecker,
      final CurrentUser currentUser,
      final ConversionHelper conversionHelper,
      final OpenSearchTransport openSearchTransport,
      final IndexConfigProvider indexConfigProvider,
      final ClusterLockManager clusterLockManager,
      final SearchConfig searchConfig,
      final ShutdownHandler shutdownHandler,
      final ReadableContextAuthzCache readableContextAuthzCache)
  {
    super(applicationDAO, labelDAO, organizationDAO, ownerDAO, policyDAO, policyWaiverDAO, autoPolicyWaiverDAO,
        searchIndexChangeDAO,
        tagDAO, thirdPartySbomMetadataDAO, documentBuilderHelper, productLicense, telemetrySender, luceneComponents,
        advancedSearchTelemetryMetrics, configuration, permissionService, authorizationChecker, currentUser,
        conversionHelper, shutdownHandler, readableContextAuthzCache);
    this.openSearchTransport = openSearchTransport;
    this.indexConfigProvider = indexConfigProvider;
    this.clusterLockManager = clusterLockManager;
    this.searchConfig = searchConfig;
  }

  @VisibleForTesting
  public OpenSearchClient getClient() {
    if (openSearchClient == null) {
      synchronized (this) {
        if (openSearchClient == null) {
          openSearchClient = new OpenSearchClient(openSearchTransport);
          createIndexIfNotExists();
        }
      }
    }
    return openSearchClient;
  }

  @Override
  public void populateIndex() {
    log.info("creating search index...");
    long start = System.currentTimeMillis();
    String oldIndexName = getRealIndexName();
    String newIndexName = generateIndexName();
    boolean newIndexCreated = false;
    boolean indexRotated = false;
    try (ClusterLock clusterLock = clusterLockManager.createForSearchIndexUpdate()) {
      createIndex(newIndexName);
      newIndexCreated = true;
      IndexConfig indexConfig = indexConfigProvider.getIndexConfig();

      clusterLock.lock();
      doPopulateIndex(createIndexingContext(() -> {
        IndexConfig newIndexConfig = new IndexConfig();
        newIndexConfig.setIndexMapping(indexConfig.getIndexMapping());
        newIndexConfig.setIndexName(newIndexName);
        return newIndexConfig;
      }));
      updateIndexAlias(newIndexName);
      indexRotated = true;

      log.info("all indexing complete");
    }
    catch (Exception e) {
      // Check if this is a rate limit error (may be wrapped in RuntimeException)
      if (isRateLimitError(e)) {
        log.error("Rate limit error during index population after retries. " +
            "Consider increasing bulkBatchDelayMs, bulkRetryBackoffSeconds, or " +
            "decreasing bulkBatchSize in AWS OpenSearch config. " +
            "New index '{}' will be deleted.", newIndexName);
      }
      throw new SearchIndexException("Error creating search index", e);
    }
    finally {
      if (indexRotated) {
        if (oldIndexName != null) {
          deleteIndex(oldIndexName);
        }
      }
      else {
        if (newIndexCreated) {
          log.info("Index rotation did not complete. Cleaning up new index '{}'", newIndexName);
          deleteIndex(newIndexName);
        }
      }
    }
    sendAdvancedSearchIndexingTelemetry(System.currentTimeMillis() - start);
    log.info("index creation exit");
  }

  @Override
  public void updateIndex(
      final List<SearchIndexChange> searchIndexChanges,
      final Consumer<SearchIndexChange> deletionCallback)
  {
    if (searchIndexChanges.isEmpty()) {
      return;
    }
    try (ClusterLock clusterLock = clusterLockManager.createForSearchIndexUpdate()) {
      if (clusterLock.tryLock()) {
        processSearchIndexChanges(searchIndexChanges, createIndexingContext(indexConfigProvider), deletionCallback);
      }
    }
    catch (Exception e) {
      if (shouldThrow(e)) {
        throw new SearchIndexException("Error updating the search index", e);
      }
      log.debug("Unable to connect to OpenSearch to update the search index.");
    }
  }

  @Override
  public Long getLastIndexTime() {
    try {
      SearchRequest searchRequest = new SearchRequest.Builder()
          .index(indexConfigProvider.getIndexConfig().getIndexName())
          .size(1)
          .sort(sort -> sort
              .field(f -> f
                  .field(IndexMapping.CREATED_AT_EPOCH_MS)
                  .order(SortOrder.Desc)))
          .build();
      SearchResponse<Map> searchResponse = getClient().search(searchRequest, Map.class);
      if (searchResponse.hits().hits().isEmpty()) {
        return null;
      }
      Map source = searchResponse.hits().hits().get(0).source();
      Object lastUpdate = source.get(IndexMapping.CREATED_AT_EPOCH_MS);
      if (lastUpdate instanceof Number lastUpdateNumber) {
        return lastUpdateNumber.longValue();
      }
      else if (lastUpdate instanceof String lastUpdateString) {
        return Long.parseLong(lastUpdateString);
      }
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }

  @Override
  public long getIndexSize() {
    try {
      IndicesStatsRequest indicesStatsRequest = new IndicesStatsRequest.Builder()
          .index(indexConfigProvider.getIndexConfig().getIndexName())
          .build();
      IndicesStatsResponse indicesStatsResponse = getClient().indices().stats(indicesStatsRequest);
      IndicesStats indicesStats =
          indicesStatsResponse.indices().get(getRealIndexName());
      return Optional.ofNullable(indicesStats.total().store()).map(StoreStats::sizeInBytes).orElse(0L);
    }
    catch (Exception e) {
      throw new SearchIndexException(e);
    }
  }

  @Override
  public SearchResultDTO searchIndex(
      final String searchQuery,
      final int pageSize,
      final int page,
      final boolean allComponents,
      final boolean isSbomManagerMode,
      final List<String> searchAfter)
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

    try {
      AuditData.get()
          .setData("searchQuery", searchQuery)
          .setData("searchPageSize", pageSize)
          .setData("searchPageIndex", finalPage - 1);

      String initialQuery = createInitialQuery(searchQuery, allComponents);
      Set<String> fieldNames = getFieldNames(initialQuery);
      populateTelemetry(initialSearch, fieldNames);
      checkFieldNames(fieldNames);
      String finalQuery = createFinalQuery(initialQuery, isSbomManagerMode);

      List<Hit<Map>> results = new ArrayList<>();
      long totalNumberOfHits;
      boolean isExactTotalNumberOfHits;

      List<String> currentSearchAfter = (searchAfter == null || searchAfter.isEmpty()) ? null : searchAfter;
      int desiredStartIndex = currentSearchAfter == null ? (finalPage - 1) * pageSize : 0;

      int maxResultWindow = getMaxResultWindow();
      int newPageSize = Math.min(maxResultWindow, desiredStartIndex + pageSize);
      int currentPageStartIndex = 0;
      while (true) {
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder()
            .index(indexConfigProvider.getIndexConfig().getIndexName())
            .query(q -> q
                .queryString(qs -> qs
                    .query(finalQuery)
                    .defaultField(FieldIdentifier.VULNERABILITY_ID.label)))
            .size(newPageSize)
            .trackTotalHits(new TrackHits.Builder().enabled(true).build())
            .sort(List.of(
                new SortOptions.Builder()
                    .score(new ScoreSort.Builder().order(SortOrder.Desc).build())
                    .build(),
                new SortOptions.Builder()
                    .field(new FieldSort.Builder().field("_id").order(SortOrder.Asc).build())
                    .build()));
        if (currentSearchAfter != null) {
          searchRequestBuilder.searchAfter(currentSearchAfter);
        }
        SearchRequest searchRequest = searchRequestBuilder.build();

        SearchResponse<Map> searchResponse = getClient().search(searchRequest, Map.class);
        HitsMetadata<Map> hitsMetadata = searchResponse.hits();
        totalNumberOfHits = Optional.ofNullable(hitsMetadata.total()).map(TotalHits::value).orElse(0L);
        isExactTotalNumberOfHits =
            Optional.ofNullable(hitsMetadata.total()).map(t -> t.relation() == TotalHitsRelation.Eq).orElse(true);
        List<Hit<Map>> hits = hitsMetadata.hits();

        // Prepare to request the next page
        if (!hits.isEmpty()) {
          Hit<Map> lastHit = hits.get(hits.size() - 1);
          List<String> sortValues = lastHit.sort();
          if (sortValues != null) {
            currentSearchAfter = sortValues;
          }
        }

        int nextPageStartIndex = currentPageStartIndex + hits.size();
        // Are there any results on this page we want?
        if (desiredStartIndex < nextPageStartIndex) {
          // If we started gathering on a previous page, then this could be negative
          int startIndex = desiredStartIndex - currentPageStartIndex;
          startIndex = Math.max(0, startIndex);

          int desiredNumberOfResults = pageSize - results.size();
          // If the page size is not large enough, then this could be beyond the page size
          int endIndex = startIndex + desiredNumberOfResults;
          endIndex = Math.min(hits.size(), endIndex);
          results.addAll(new ArrayList<>(hits.subList(startIndex, endIndex)));
        }

        // We've got all the results we want
        if (results.size() == pageSize) {
          break;
        }

        // We can't get any more results
        if (hits.size() < newPageSize) {
          break;
        }

        currentPageStartIndex += hits.size();
        newPageSize = Math.min(maxResultWindow, (desiredStartIndex - currentPageStartIndex) + pageSize);
      }

      Iterator<Document> documents = results.stream()
          .map(Hit::source)
          .map(conversionHelper::mapToDocument)
          .iterator();
      Supplier<Document> documentSupplier = () -> documents.hasNext() ? documents.next() : null;

      SearchResultDTO searchResultDTO = new SearchResultDTO();
      searchResultDTO.searchQuery = searchQuery;
      searchResultDTO.page = finalPage;
      searchResultDTO.pageSize = pageSize;
      groupDocuments(finalPage, pageSize, documentSupplier, searchResultDTO, getGroupFieldNamesByItemType(fieldNames));
      searchResultDTO.totalNumberOfHits = totalNumberOfHits;
      searchResultDTO.isExactTotalNumberOfHits = isExactTotalNumberOfHits;
      searchResultDTO.searchAfter = currentSearchAfter;

      AuditData.get().setData("resultRecordCount", searchResultDTO.countSearchResults());
      return searchResultDTO;
    }
    catch (Exception e) {
      throwIfIndexNotFound(e);
      if (e instanceof OpenSearchException openSearchException) {
        String responseJson = openSearchException.response().toJsonString();
        if (responseJson.contains("too_many_clauses")) {
          throw TOO_MANY_CLAUSES_EXCEPTION;
        }
      }
      if (e instanceof BadRequestException badRequestException) {
        throw badRequestException;
      }
      throw new SearchIndexException(e);
    }
  }

  @Override
  public String backendId() {
    return BACKEND_ID;
  }

  public IndexReadSession openReadSession(final org.apache.lucene.search.Query rbacFilter) {
    Long lastIndexTime = getLastIndexTime();
    Instant lastUpdatedAt = lastIndexTime == null ? Instant.EPOCH : Instant.ofEpochMilli(lastIndexTime);
    return new OpenSearchIndexReadSession(
        getClient(),
        indexConfigProvider.getIndexConfig().getIndexName(),
        conversionHelper,
        rbacFilter,
        lastUpdatedAt,
        searchConfig.getPitKeepAlive());
  }

  /** {@code request.baseQuery()} MUST already be permission-wrapped; this runs it verbatim. */
  @Override
  public GlobalSearchResult searchGlobal(final GlobalSearchRequest request) {
    if (!isSearchPreviewEnabled()) {
      throw new ConflictException("Global Search is disabled");
    }
    try {
      int pageSize = request.pageSize();
      AuditData.get()
          .setData("searchPageSize", pageSize)
          .setData("searchSort", describeSort(request.sort()))
          .setData("searchAfterPresent", !request.searchAfter().isEmpty())
          .setData("searchBackend", BACKEND_ID);
      org.opensearch.client.opensearch._types.query_dsl.Query openSearchQuery =
          LuceneToOpenSearchQueryAdapter.toOpenSearch(request.baseQuery());
      List<SortOptions> sortOptions = buildSortOptionsForGlobalSearch(request);
      SearchRequest.Builder builder = new SearchRequest.Builder()
          .index(indexConfigProvider.getIndexConfig().getIndexName())
          .query(openSearchQuery)
          .size(pageSize + 1)
          .trackTotalHits(t -> t.count(AbstractSearchIndexClient.GLOBAL_SEARCH_TRACK_TOTAL_HITS_CAP))
          .source(SourceConfig.of(s -> s.fetch(true)))
          .sort(sortOptions);
      if (!request.searchAfter().isEmpty()) {
        if (request.searchAfter().size() != sortOptions.size()) {
          throw new BadRequestException("Invalid searchAfter tuple for Global Search sort.");
        }
        builder.searchAfter(request.searchAfter());
      }
      SearchRequest searchRequest = builder.build();

      SearchResponse<Map> response = getClient().search(searchRequest, Map.class);
      HitsMetadata<Map> hitsMetadata = response.hits();
      TotalHits totalHits = hitsMetadata.total();
      long total = totalHits == null ? 0L : totalHits.value();
      boolean exactTotal = totalHits == null || totalHits.relation() == TotalHitsRelation.Eq;
      List<Hit<Map>> fetchedHits = hitsMetadata.hits();
      AbstractSearchIndexClient.HasMoreResult<Hit<Map>> paged =
          AbstractSearchIndexClient.detectHasMore(fetchedHits, pageSize);
      List<Hit<Map>> pageHits = paged.rows();
      boolean hasMore = paged.hasMore();

      List<SearchResultItemDTO> rows = new ArrayList<>(pageHits.size());
      for (Hit<Map> hit : pageHits) {
        Map<String, Object> source = hit.source();
        if (source == null) {
          throw new SearchIndexException("OpenSearch search hit " + hit.id() + " carried a null _source", null);
        }
        Document doc = conversionHelper.mapToDocument(source);
        rows.add(new SearchResultItemDTO(doc));
      }

      List<String> nextSearchAfter = List.of();
      if (hasMore && !pageHits.isEmpty()) {
        Hit<Map> last = pageHits.get(pageHits.size() - 1);
        List<String> sortValues = last.sort();
        if (sortValues == null || sortValues.isEmpty()) {
          throw new SearchIndexException(
              "overfetch indicated a next page but the last hit carried no sort tuple", null);
        }
        nextSearchAfter = List.copyOf(sortValues);
      }

      long capped = AbstractSearchIndexClient.capTotalHitsForGlobalSearch(total);
      AuditData.get().setData("resultRecordCount", rows.size());
      return new GlobalSearchResult(rows, capped, nextSearchAfter, exactTotal);
    }
    catch (SearchIndexException searchIndexException) {
      throw searchIndexException;
    }
    catch (Exception e) {
      throwIfIndexNotFound(e);
      if (e instanceof OpenSearchException openSearchException
          && openSearchException.response().toJsonString().contains("too_many_clauses"))
      {
        throw TOO_MANY_CLAUSES_EXCEPTION;
      }
      if (e instanceof BadRequestException badRequestException) {
        throw badRequestException;
      }
      throw new SearchIndexException(e);
    }
  }

  private static List<SortOptions> buildSortOptionsForGlobalSearch(final GlobalSearchRequest request) {
    List<SortOptions> options = new ArrayList<>();
    Sort sort = request.sort();
    if (sort != null) {
      for (SortField sf : sort.getSort()) {
        String field = sf.getField();
        SortOrder order = sf.getReverse() ? SortOrder.Desc : SortOrder.Asc;
        options.add(new SortOptions.Builder()
            .field(new FieldSort.Builder().field(field).order(order).build())
            .build());
      }
    }
    else {
      options.add(new SortOptions.Builder()
          .score(new ScoreSort.Builder().order(SortOrder.Desc).build())
          .build());
    }
    // Deterministic tie-breaker; sortable without fielddata, unlike _id.
    options.add(new SortOptions.Builder()
        .field(new FieldSort.Builder().field(FieldIdentifier.DOCUMENT_KEY.label).order(SortOrder.Asc).build())
        .build());
    return options;
  }

  private int getMaxResultWindow() throws IOException {
    GetIndicesSettingsRequest getIndicesSettingsRequest = new GetIndicesSettingsRequest.Builder()
        .index(indexConfigProvider.getIndexConfig().getIndexName())
        .build();
    GetIndicesSettingsResponse getIndicesSettingsResponse =
        getClient().indices().getSettings(getIndicesSettingsRequest);
    return Optional.ofNullable(
        getIndicesSettingsResponse.result().get(getRealIndexName()))
        .map(
            IndexState::settings)
        .map(IndexSettings::maxResultWindow)
        .orElse(DEFAULT_MAX_RESULT_WINDOW);
  }

  @Override
  protected void updateMaxQueryClauseCount() throws IOException {
    // No-op: AWS managed OpenSearch does not allow updating the max_clause_count cluster setting.
    // The default value is typically sufficient, and attempting to update it would cause failures
    // in AWS managed environments. See CLM-38052.
  }

  private void updateIndexAlias(final String newIndex) {
    try {
      String aliasName = indexConfigProvider.getIndexConfig().getIndexName();
      String oldIndex = getCurrentIndexNameForAlias(aliasName);

      List<Action> actions = new ArrayList<>();
      if (oldIndex != null) {
        actions.add(
            new Action.Builder().remove(new RemoveAction.Builder().index(oldIndex).alias(aliasName).build()).build());
      }
      actions.add(new Action.Builder().add(new AddAction.Builder().index(newIndex).alias(aliasName).build()).build());

      UpdateAliasesRequest updateAliasesRequest = new UpdateAliasesRequest.Builder()
          .actions(actions)
          .build();
      UpdateAliasesResponse updateAliasesResponse = getClient().indices().updateAliases(updateAliasesRequest);

      if (!updateAliasesResponse.acknowledged()) {
        throw new RuntimeException("Alias update not acknowledged.");
      }

      log.info("Alias '{}' now points to '{}'", aliasName, newIndex);
    }
    catch (Exception e) {
      throw new RuntimeException(
          "Failed to rotate index for alias '%s'".formatted(indexConfigProvider.getIndexConfig().getIndexName()), e);
    }
  }

  // Visible for testing
  public void deleteIndex() {
    deleteIndex(getRealIndexName());
  }

  private void deleteIndex(final String name) {
    try {
      DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest.Builder()
          .index(name)
          .build();
      DeleteIndexResponse deleteIndexResponse = getClient().indices().delete(deleteIndexRequest);
      if (!deleteIndexResponse.acknowledged()) {
        throw new RuntimeException("Delete not acknowledged.");
      }
      log.info("Deleted index '{}'", name);
    }
    catch (Exception e) {
      log.error("Failed to delete the index {}.", name, e);
    }
  }

  // Visible for testing
  String getRealIndexName() {
    return getCurrentIndexNameForAlias(indexConfigProvider.getIndexConfig().getIndexName());
  }

  private String getCurrentIndexNameForAlias(final String aliasName) {
    try {
      GetAliasRequest getAliasRequest = new GetAliasRequest.Builder()
          .build();
      GetAliasResponse getAliasResponse = getClient().indices().getAlias(getAliasRequest);
      Map<String, IndexAliases> result = getAliasResponse.result();
      for (Entry<String, IndexAliases> entry : result.entrySet()) {
        if (entry.getValue().aliases().containsKey(aliasName)) {
          return entry.getKey();
        }
      }
      return null;
    }
    catch (Exception e) {
      // If the error is because the alias doesn't exist (404), return null (first-time creation)
      if (e instanceof OpenSearchException ose && ose.status() == 404) {
        log.debug("Index not found for alias '{}' - this is expected for first-time index creation", aliasName);
        return null;
      }
      throw new RuntimeException("Failed to get current index name for alias '%s'".formatted(
          indexConfigProvider.getIndexConfig().getIndexName()), e);
    }
  }

  private String generateIndexName() {
    return indexConfigProvider.getIndexConfig().getIndexName() + "-" + UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * Checks if the given exception is an OpenSearchException with an "index_not_found_exception" error.
   * If so, throws a ConflictException with a user-friendly error message.
   *
   * @param e the exception to check
   * @throws ConflictException if the exception indicates the index was not found
   */
  private void throwIfIndexNotFound(final Exception e) {
    if (e instanceof OpenSearchException openSearchException) {
      if (openSearchException.status() == 404 ||
          openSearchException.response().toJsonString().contains("index_not_found_exception"))
      {
        throw new ConflictException(NO_INDEX_ERROR_MESSAGE);
      }
    }
  }

  private boolean indexExists(final String name) {
    try {
      ExistsRequest existsRequest = new ExistsRequest.Builder()
          .index(name)
          .build();
      BooleanResponse booleanResponse = getClient().indices().exists(existsRequest);
      if (booleanResponse.value()) {
        log.debug("OpenSearch index '{}' already exists", name);
        return true;
      }
      return false;
    }
    catch (Exception e) {
      throw new RuntimeException(String.format("Error checking existence for OpenSearch index: '%s'", name), e);
    }
  }

  // Visible for testing
  public void createIndexIfNotExists() {
    if (!indexExists(indexConfigProvider.getIndexConfig().getIndexName())) {
      String newIndexName = generateIndexName();
      createIndex(newIndexName);
      updateIndexAlias(newIndexName);
    }
  }

  // Visible for testing
  public void createIndex(final String name) {
    try {
      log.info("Creating OpenSearch index: {}", name);
      TypeMapping typeMapping = new TypeMapping.Builder()
          .properties(indexConfigProvider.getIndexConfig().getIndexMapping().getMappings())
          // date_detection off so any date-ish keyword field left to dynamic mapping (e.g. the
          // ISO-8601 policyWaiver*At fields) is never auto-typed as `date`. Applies to freshly
          // created indices only; explicit properties above already pin the known fields.
          .dateDetection(false)
          .build();
      CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder()
          .index(name)
          .mappings(typeMapping)
          .build();
      CreateIndexResponse createIndexResponse = getClient().indices().create(createIndexRequest);

      if (Boolean.TRUE.equals(createIndexResponse.acknowledged())) {
        log.info("OpenSearch index '{}' created successfully", createIndexResponse.index());
      }
    }
    catch (Exception e) {
      throw new RuntimeException(String.format("Error creating OpenSearch index: '%s'", name), e);
    }
  }

  private OpenSearchIndexingContext createIndexingContext(IndexConfigProvider configProvider) {
    // Get bulk configuration from AbstractSearchConfig
    if (!(searchConfig instanceof SearchConfig.AbstractSearchConfig abstractConfig)) {
      throw new IllegalStateException("Unknown search config type: " + searchConfig.getClass());
    }

    int batchSize = abstractConfig.getBulkBatchSize();
    int batchDelayMs = abstractConfig.getBulkBatchDelayMs();
    int maxRetries = abstractConfig.getBulkMaxRetries();
    int retryBackoffMs = abstractConfig.getBulkRetryBackoffSeconds() * 1000;
    int maxRetryBackoffMs = abstractConfig.getMaxBulkRetryBackoffSeconds() * 1000;

    String configType = searchConfig instanceof AwsHttpOpenSearchConfig ? "AWS" : "HTTP";
    log.debug(
        "Using {} OpenSearch bulk settings: batchSize={}, batchDelayMs={}, " +
            "maxRetries={}, retryBackoffMs={}, maxRetryBackoffMs={}",
        configType, batchSize, batchDelayMs, maxRetries, retryBackoffMs, maxRetryBackoffMs);

    return new OpenSearchIndexingContext(ownerDAO, conversionHelper, configProvider, getClient(),
        batchSize, batchDelayMs, maxRetries, retryBackoffMs, maxRetryBackoffMs);
  }

  @Override
  protected boolean isChangeSpecificError(final Exception e) {
    return isCommonChangeSpecificError(e) || hasCauseOrMessage(e, cause -> {
      String msg = cause.getMessage();
      return msg != null && CHANGE_SPECIFIC_OPENSEARCH_LOWERCASE_ERROR_MESSAGES.stream()
          .anyMatch(m -> msg.toLowerCase().contains(m));
    });
  }

  @Override
  protected boolean isSystemicError(final Exception e) {
    if (isCommonSystemicError(e)) {
      return true;
    }
    if (isRateLimitError(e)) {
      return true;
    }
    return hasCauseOrMessage(e, cause -> {
      if (SYSTEMIC_NETWORK_EXCEPTIONS.contains(cause.getClass())) {
        return true;
      }
      if (cause instanceof OpenSearchException ose) {
        int status = ose.status();
        if (status >= 500 && status < 600) {
          return true;
        }
        ErrorCause error = ose.error();
        if (error != null) {
          String lowerType = error.type().toLowerCase();
          if (SYSTEMIC_OPENSEARCH_LOWERCASE_EXCEPTION_TYPES.contains(lowerType)) {
            return true;
          }
          String reason = error.reason();
          if (reason != null) {
            String lowerReason = reason.toLowerCase();
            if (SYSTEMIC_OPENSEARCH_LOWERCASE_REASON_FRAGMENTS.stream().anyMatch(lowerReason::contains)) {
              return true;
            }
          }
        }
      }
      String msg = cause.getMessage();
      if (msg != null) {
        String lowerMessage = msg.toLowerCase();
        return SYSTEMIC_OPENSEARCH_LOWERCASE_ERROR_MESSAGES.stream().anyMatch(lowerMessage::contains);
      }
      return false;
    });
  }

  @Override
  public long count(final String metricQuery) {
    try {
      String initialQuery = createInitialQuery(metricQuery, true);
      Set<String> fieldNames = getFieldNames(initialQuery);
      checkFieldNames(fieldNames);

      SearchRequest searchRequest = new SearchRequest.Builder()
          .index(indexConfigProvider.getIndexConfig().getIndexName())
          .size(0)
          .trackTotalHits(new TrackHits.Builder().enabled(true).build())
          .query(buildMetricQuery(initialQuery))
          .build();

      SearchResponse<Map> searchResponse = getClient().search(searchRequest, Map.class);
      return Optional.ofNullable(searchResponse.hits().total()).map(TotalHits::value).orElse(0L);
    }
    catch (Exception e) {
      throwMetricSearchException(e);
      // throwMetricSearchException always throws; this keeps the no-throw path
      // structurally impossible so a future change can't make count() silently
      // return an unscoped 0 (which would read as success and fail RBAC open).
      throw new IllegalStateException("unreachable: throwMetricSearchException always throws", e);
    }
  }

  @Override
  public MetricAggregationResult aggregateCountByField(
      final String metricQuery,
      final String bucketField,
      final Map<String, int[]> ranges)
  {
    validateRangeBounds(ranges);
    try {
      String initialQuery = createInitialQuery(metricQuery, true);
      Set<String> fieldNames = getFieldNames(initialQuery);
      checkFieldNames(fieldNames);

      List<AggregationRange> aggregationRanges = new ArrayList<>();
      // Upper bound is exclusive in OpenSearch range aggs but inclusive in our contract, so add 1.
      // Use long arithmetic so an upper bound of Integer.MAX_VALUE doesn't overflow to a negative
      // value (which would silently produce a zero-count bucket).
      ranges.forEach((label, bounds) -> aggregationRanges.add(AggregationRange.of(r -> r
          .key(label)
          .from(String.valueOf(bounds[0]))
          .to(String.valueOf((long) bounds[1] + 1)))));

      SearchRequest searchRequest = new SearchRequest.Builder()
          .index(indexConfigProvider.getIndexConfig().getIndexName())
          .size(0)
          .trackTotalHits(new TrackHits.Builder().enabled(true).build())
          .query(buildMetricQuery(initialQuery))
          .aggregations("metricBuckets", a -> a
              .range(r -> r
                  .field(bucketField)
                  .ranges(aggregationRanges)))
          .build();

      SearchResponse<Map> searchResponse = getClient().search(searchRequest, Map.class);
      long total = Optional.ofNullable(searchResponse.hits().total()).map(TotalHits::value).orElse(0L);

      Map<String, Long> buckets = new LinkedHashMap<>();
      ranges.keySet().forEach(label -> buckets.put(label, 0L));
      Map<String, Aggregate> aggs = searchResponse.aggregations();
      Aggregate aggregate = aggs != null ? aggs.get("metricBuckets") : null;
      if (aggregate != null && aggregate.isRange()) {
        for (RangeBucket bucket : aggregate.range().buckets().array()) {
          String key = bucket.key();
          if (key != null) {
            buckets.put(key, bucket.docCount());
          }
        }
      }

      return new MetricAggregationResult(total, buckets);
    }
    catch (Exception e) {
      throwMetricSearchException(e);
      // See count(): never return null here — a null would NPE the caller on
      // result.total/result.buckets. Fail closed by throwing instead.
      throw new IllegalStateException("unreachable: throwMetricSearchException always throws", e);
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
    try {
      String initialQuery = createInitialQuery(metricQuery, true);
      Set<String> fieldNames = getFieldNames(initialQuery);
      checkFieldNames(fieldNames);

      List<AggregationRange> aggregationRanges = new ArrayList<>();
      // Our contract is half-open [minInclusive, maxExclusive), which is exactly OpenSearch range-agg
      // semantics: `from` is inclusive and `to` is exclusive. So — unlike the int overload, which adds
      // 1 to convert an inclusive upper bound to OpenSearch's exclusive `to` — the float bounds pass
      // through verbatim. This keeps a CVSS boundary value (e.g. 7.0) in exactly one band, identical to
      // the Lucene sibling's FloatPoint.nextDown(upper) treatment.
      ranges.forEach((label, bounds) -> aggregationRanges.add(AggregationRange.of(r -> r
          .key(label)
          .from(String.valueOf(bounds[0]))
          .to(String.valueOf(bounds[1])))));

      // distinctField != null: hang a cardinality sub-agg (HyperLogLog++) off each range bucket so a
      // band's count is distinct distinctField values, not raw docs — the range-agg analogue of the
      // terms+cardinality pattern in countDistinctGroupedBy. distinctField == null: plain per-band
      // docCount (raw). The distinct field is a keyword field, so use its grouped/keyword label.
      final String distinctLabel = distinctField == null ? null : resolveCompositeKeyFieldLabel(distinctField);
      SearchRequest searchRequest = new SearchRequest.Builder()
          .index(indexConfigProvider.getIndexConfig().getIndexName())
          .size(0)
          .trackTotalHits(new TrackHits.Builder().enabled(true).build())
          .query(buildMetricQuery(initialQuery))
          .aggregations("metricBuckets", a -> distinctLabel == null
              ? a.range(r -> r.field(bucketField).ranges(aggregationRanges))
              : a.range(r -> r.field(bucketField).ranges(aggregationRanges))
                  .aggregations("distinct", sub -> sub
                      .cardinality(c -> c.field(distinctLabel).precisionThreshold(40_000))))
          .build();

      SearchResponse<Map> searchResponse = getClient().search(searchRequest, Map.class);
      long total = Optional.ofNullable(searchResponse.hits().total()).map(TotalHits::value).orElse(0L);

      Map<String, Long> buckets = new LinkedHashMap<>();
      ranges.keySet().forEach(label -> buckets.put(label, 0L));
      Map<String, Aggregate> aggs = searchResponse.aggregations();
      Aggregate aggregate = aggs != null ? aggs.get("metricBuckets") : null;
      if (aggregate != null && aggregate.isRange()) {
        for (RangeBucket bucket : aggregate.range().buckets().array()) {
          String key = bucket.key();
          if (key != null) {
            long value = bucket.docCount();
            if (distinctLabel != null) {
              Aggregate distinct = bucket.aggregations().get("distinct");
              value = distinct != null && distinct.isCardinality() ? distinct.cardinality().value() : 0L;
            }
            buckets.put(key, value);
          }
        }
      }

      return new MetricAggregationResult(total, buckets);
    }
    catch (Exception e) {
      throwMetricSearchException(e);
      // See count(): never return null here — a null would NPE the caller on
      // result.total/result.buckets. Fail closed by throwing instead.
      throw new IllegalStateException("unreachable: throwMetricSearchException always throws", e);
    }
  }

  @Override
  public long countDistinct(final String metricQuery, final List<String> compositeKeyFields) {
    validateCompositeKeyFields(compositeKeyFields);
    checkFieldNames(new HashSet<>(compositeKeyFields));
    try {
      String initialQuery = createInitialQuery(metricQuery, true);
      Set<String> fieldNames = getFieldNames(initialQuery);
      checkFieldNames(fieldNames);

      Script compositeKeyScript = buildCompositeKeyScript(compositeKeyFields);

      SearchRequest searchRequest = new SearchRequest.Builder()
          .index(indexConfigProvider.getIndexConfig().getIndexName())
          .size(0)
          .query(buildMetricQuery(initialQuery))
          .aggregations("distinctCompositeKeys", a -> a
              .cardinality(c -> c
                  .script(compositeKeyScript)
                  // Max precision_threshold shrinks HLL++ error at ~320 KB/shard; still approximate above this.
                  .precisionThreshold(40_000)))
          .build();

      SearchResponse<Map> searchResponse = getClient().search(searchRequest, Map.class);
      Aggregate aggregate = searchResponse.aggregations().get("distinctCompositeKeys");
      if (aggregate != null && aggregate.isCardinality()) {
        return aggregate.cardinality().value();
      }
      return 0L;
    }
    catch (Exception e) {
      throwMetricSearchException(e);
      // throwMetricSearchException always throws; this keeps the no-throw path structurally impossible so a
      // future change can't make countDistinct() silently return an unscoped 0 (fail-open RBAC footgun).
      throw new IllegalStateException("unreachable: throwMetricSearchException always throws", e);
    }
  }

  @Override
  public RankedGroupsResult rankGroupsByMaxMetric(
      final String metricQuery,
      final String groupField,
      final String metricField,
      final int limit,
      final boolean ascending,
      final Map<String, float[]> metricBands)
  {
    checkFieldNames(new HashSet<>(List.of(groupField, metricField)));
    validateFloatRangeBounds(metricBands);
    if (limit <= 0) {
      return RankedGroupsResult.empty(metricBands);
    }
    String pitId = null;
    String pitKeepAlive = searchConfig.getPitKeepAlive();
    try {
      String initialQuery = createInitialQuery(metricQuery, true);
      checkFieldNames(getFieldNames(initialQuery));

      String groupLabel = resolveCompositeKeyFieldLabel(groupField);
      Query osQuery = buildMetricQuery(initialQuery);
      String indexName = indexConfigProvider.getIndexConfig().getIndexName();

      // Pin a PIT for the multi-page composite scan so refreshes cannot change the group set mid-walk.
      CreatePitResponse pitResponse = getClient().createPit(new CreatePitRequest.Builder()
          .targetIndexes(indexName)
          .keepAlive(t -> t.time(pitKeepAlive))
          .build());
      pitId = pitResponse.pitId();
      final String pinnedPitId = pitId;

      return OpenSearchRankedGroupsAggregation.execute(
          request -> getClient().search(request, Map.class),
          () -> new SearchRequest.Builder()
              .pit(new Pit.Builder().id(pinnedPitId).keepAlive(pitKeepAlive).build())
              .query(osQuery),
          groupLabel,
          metricField,
          limit,
          ascending,
          metricBands);
    }
    catch (Exception e) {
      throwMetricSearchException(e);
      throw new IllegalStateException("unreachable: throwMetricSearchException always throws", e);
    }
    finally {
      if (pitId != null) {
        try {
          getClient().deletePit(new DeletePitRequest.Builder().pitId(List.of(pitId)).build());
        }
        catch (Exception e) {
          // OpenSearchException (e.g. 404 on already-expired PIT) is a RuntimeException — must not
          // escape finally and replace a successful RankedGroupsResult (or mask the real failure).
          log.warn("Failed to delete OpenSearch PIT {} after ranked-groups scan; it will expire after keepAlive",
              pitId, e);
        }
      }
    }
  }

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
    try {
      String initialQuery = createInitialQuery(metricQuery, true);
      Set<String> fieldNames = getFieldNames(initialQuery);
      checkFieldNames(fieldNames);

      String groupLabel = resolveCompositeKeyFieldLabel(groupField);
      String distinctLabel = resolveCompositeKeyFieldLabel(distinctField);

      // The grouped keyword fields carry a lowercase normalizer (see IndexMapping), so aggregation
      // bucket keys are already lowercase while groupValues arrive verbatim from _source (mixed case,
      // e.g. "CVE-2021-44228"). Match and key the result map on the lowercased value so callers can look
      // up counts consistently; enrichLocalCounts lowercases the lookup key the same way.
      Set<String> requested = new HashSet<>();
      for (String groupValue : groupValues) {
        requested.add(groupValue.toLowerCase(Locale.ROOT));
      }
      // Restrict the terms aggregation to exactly the requested group values. Without an include
      // filter a plain terms agg returns only the global top-`size` buckets by doc count, so any
      // requested value outside that window is silently dropped and reported as zero once the corpus
      // holds more distinct group values than a page (e.g. affectedApps read 0 for most components).
      List<String> includeTerms = new ArrayList<>(requested);

      SearchRequest searchRequest = new SearchRequest.Builder()
          .index(indexConfigProvider.getIndexConfig().getIndexName())
          .size(0)
          .query(buildMetricQuery(initialQuery))
          .aggregations("groups", a -> a
              .terms(t -> t.field(groupLabel)
                  .size(includeTerms.size())
                  .include(ti -> ti.terms(includeTerms)))
              .aggregations("distinct", sub -> sub
                  .cardinality(c -> c.field(distinctLabel).precisionThreshold(40_000))))
          .build();

      SearchResponse<Map> searchResponse = getClient().search(searchRequest, Map.class);
      Aggregate aggregate = searchResponse.aggregations() == null
          ? null
          : searchResponse.aggregations().get("groups");
      if (aggregate == null || !aggregate.isSterms()) {
        return Map.of();
      }
      Map<String, Long> counts = new LinkedHashMap<>();
      for (StringTermsBucket bucket : aggregate.sterms().buckets().array()) {
        String key = bucket.key();
        if (!requested.contains(key)) {
          continue;
        }
        Aggregate distinct = bucket.aggregations().get("distinct");
        long value = distinct != null && distinct.isCardinality() ? distinct.cardinality().value() : 0L;
        if (value > 0) {
          counts.put(key, value);
        }
      }
      return counts;
    }
    catch (Exception e) {
      throwMetricSearchException(e);
      throw new IllegalStateException("unreachable: throwMetricSearchException always throws", e);
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
    try {
      String initialQuery = createInitialQuery(metricQuery, true);
      Set<String> fieldNames = getFieldNames(initialQuery);
      checkFieldNames(fieldNames);

      String groupLabel = resolveCompositeKeyFieldLabel(groupField);
      String distinctLabel = resolveCompositeKeyFieldLabel(distinctField);

      // Group values arrive verbatim from _source (mixed case); the grouped keyword field carries a
      // lowercase normalizer, so match/key on the lowercased value (same as countDistinctGroupedBy).
      Set<String> requested = new HashSet<>();
      for (String groupValue : groupValues) {
        requested.add(groupValue.toLowerCase(Locale.ROOT));
      }
      List<String> includeTerms = new ArrayList<>(requested);

      // Half-open [from, to): our int bands are inclusive-upper, so add 1 (long arithmetic guards the
      // Integer.MAX_VALUE overflow), matching aggregateCountByField. Programmatic ranges, not a
      // string-interpolated band clause.
      List<AggregationRange> aggregationRanges = new ArrayList<>();
      bands.forEach((label, bounds) -> aggregationRanges.add(AggregationRange.of(r -> r
          .key(label)
          .from(String.valueOf(bounds[0]))
          .to(String.valueOf((long) bounds[1] + 1)))));

      SearchRequest searchRequest = new SearchRequest.Builder()
          .index(indexConfigProvider.getIndexConfig().getIndexName())
          .size(0)
          .query(buildMetricQuery(initialQuery))
          .aggregations("bands", a -> a
              .range(r -> r.field(bandField).ranges(aggregationRanges))
              .aggregations("groups", g -> g
                  .terms(t -> t.field(groupLabel)
                      .size(includeTerms.size())
                      .include(ti -> ti.terms(includeTerms)))
                  .aggregations("distinct", sub -> sub
                      .cardinality(c -> c.field(distinctLabel).precisionThreshold(40_000)))))
          .build();

      SearchResponse<Map> searchResponse = getClient().search(searchRequest, Map.class);
      Aggregate bandsAgg = searchResponse.aggregations() == null
          ? null
          : searchResponse.aggregations().get("bands");
      Map<String, Map<String, Long>> byGroup = new LinkedHashMap<>();
      if (bandsAgg == null || !bandsAgg.isRange()) {
        return byGroup;
      }
      for (RangeBucket bandBucket : bandsAgg.range().buckets().array()) {
        String bandLabel = bandBucket.key();
        if (bandLabel == null) {
          continue;
        }
        Aggregate groupsAgg = bandBucket.aggregations().get("groups");
        if (groupsAgg == null || !groupsAgg.isSterms()) {
          continue;
        }
        for (StringTermsBucket groupBucket : groupsAgg.sterms().buckets().array()) {
          String group = groupBucket.key();
          if (!requested.contains(group)) {
            continue;
          }
          Aggregate distinct = groupBucket.aggregations().get("distinct");
          long value = distinct != null && distinct.isCardinality() ? distinct.cardinality().value() : 0L;
          if (value > 0) {
            byGroup.computeIfAbsent(group, k -> new LinkedHashMap<>()).put(bandLabel, value);
          }
        }
      }
      return byGroup;
    }
    catch (Exception e) {
      throwMetricSearchException(e);
      throw new IllegalStateException("unreachable: throwMetricSearchException always throws", e);
    }
  }

  /**
   * Builds an inline painless script that concatenates the {@code compositeKeyFields} doc values into a single
   * string key (NUL-separated), used as the source for a {@code cardinality} aggregation to count distinct
   * composite keys (e.g. distinct {@code (applicationId, componentHash)} pairs). A script is required because no
   * single indexed field holds the composite key.
   */
  private Script buildCompositeKeyScript(final List<String> compositeKeyFields) {
    StringBuilder source = new StringBuilder();
    for (int i = 0; i < compositeKeyFields.size(); i++) {
      if (i > 0) {
        source.append(" + '\\u0000' + ");
      }
      String fieldLabel = resolveCompositeKeyFieldLabel(compositeKeyFields.get(i));
      source.append("(doc['")
          .append(fieldLabel)
          .append("'].size() > 0 ? doc['")
          .append(fieldLabel)
          .append("'].value : '')");
    }
    String painless = source.toString();
    return Script.of(s -> s.inline(i -> i.lang("painless").source(painless)));
  }

  private Query buildMetricQuery(final String initialQuery) {
    // Metric queries always carry an explicit field prefix (e.g. itemType:application),
    // so defaultField is never exercised today. Default to itemType (the only field
    // used by metric queries) so an accidentally prefix-less query degrades to a
    // sensible field rather than silently searching vulnerability ids.
    Query userQueryClause = Query.of(q -> q
        .queryString(qs -> qs
            .query(initialQuery)
            .defaultField(FieldIdentifier.ITEM_TYPE.label)));

    Query rbacFilter = buildOpenSearchRbacFilterQuery();
    if (rbacFilter == null) {
      return userQueryClause;
    }

    return Query.of(q -> q
        .bool(b -> b
            .must(userQueryClause)
            .filter(rbacFilter)));
  }

  /**
   * Programmatic RBAC filter mirroring {@link com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient}
   * {@code buildRbacFilterQuery()}. Keyword fields use a lowercase normalizer ({@link IndexMapping}), so context ids
   * are lowercased to match indexed values.
   *
   * @return {@code null} when access is unrestricted; otherwise a filter query (including {@code match_none} when
   *         fail-closed).
   */
  private Query buildOpenSearchRbacFilterQuery() {
    Optional<Map<String, OwnerType>> readableContexts = resolveReadableContextIdsForCurrentUser();
    if (readableContexts.isEmpty()) {
      return null;
    }

    Map<String, OwnerType> contextIdsWithReadPermissionMap = readableContexts.get();
    if (contextIdsWithReadPermissionMap.isEmpty()) {
      return Query.of(q -> q.matchNone(m -> m));
    }

    List<FieldValue> applicationTerms = new ArrayList<>();
    List<FieldValue> organizationTerms = new ArrayList<>();
    contextIdsWithReadPermissionMap.forEach((contextId, type) -> {
      FieldValue term = FieldValue.of(contextId.toLowerCase(Locale.ROOT));
      if (OwnerType.APPLICATION.equals(type)) {
        applicationTerms.add(term);
      }
      else if (OwnerType.ORGANIZATION.equals(type)) {
        organizationTerms.add(term);
      }
    });

    // Explicit fail-closed: if the user's readable contexts are all non-APPLICATION/
    // non-ORGANIZATION types (e.g. a Firewall-only user with only REPOSITORY*), both
    // term lists are empty. Rather than rely on the engine treating a zero-should
    // BooleanQuery + minimumShouldMatch=1 as match-none, say so explicitly so the
    // intent is clear and stays in lockstep with the Lucene sibling.
    if (applicationTerms.isEmpty() && organizationTerms.isEmpty()) {
      return Query.of(q -> q.matchNone(m -> m));
    }

    return Query.of(q -> q
        .bool(b -> {
          if (!applicationTerms.isEmpty()) {
            b.should(s -> s.terms(t -> t
                .field(APPLICATION_ID.label)
                .terms(tv -> tv.value(applicationTerms))));
          }
          if (!organizationTerms.isEmpty()) {
            b.should(s -> s.terms(t -> t
                .field(ORGANIZATION_ID.label)
                .terms(tv -> tv.value(organizationTerms))));
          }
          b.minimumShouldMatch("1");
          return b;
        }));
  }

  private void throwMetricSearchException(final Exception e) {
    if (e instanceof SearchIndexException searchIndexException) {
      throw searchIndexException;
    }
    throwIfIndexNotFound(e);
    if (e instanceof OpenSearchException openSearchException) {
      String responseJson = openSearchException.response().toJsonString();
      if (responseJson.contains("too_many_clauses")) {
        throw TOO_MANY_CLAUSES_EXCEPTION;
      }
    }
    if (e instanceof BadRequestException badRequestException) {
      throw badRequestException;
    }
    throw new SearchIndexException(e);
  }

  public static boolean isRateLimitError(final Exception e) {
    return AbstractSearchIndexClient.hasCauseOrMessage(e, cause -> {
      if (cause instanceof OpenSearchException ose) {
        if (ose.status() == 429) {
          return true;
        }
        ErrorCause error = ose.error();
        if (error != null) {
          String reason = error.reason();
          return reason != null && reason.toLowerCase().contains("too many requests");
        }
      }
      return false;
    });
  }
}
