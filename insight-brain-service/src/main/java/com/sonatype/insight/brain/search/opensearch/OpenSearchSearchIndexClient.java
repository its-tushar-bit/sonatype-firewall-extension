/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.lucene.LuceneComponents;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.annotations.VisibleForTesting;
import org.apache.lucene.document.Document;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.ScoreSort;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.StoreStats;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.HitsMetadata;
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
import ru.vyarus.dropwizard.guice.module.installer.scanner.InvisibleForScanner;

/**
 * OpenSearch support for {@link SearchIndexClient}
 * <p>
 * Note: See {@link com.sonatype.insight.brain.search.SearchModule} for Guice bindings
 */
@Singleton
@InvisibleForScanner
public class OpenSearchSearchIndexClient
    extends AbstractSearchIndexClient
{
  private static final Logger log = LoggerFactory.getLogger(OpenSearchSearchIndexClient.class);

  private static final int DEFAULT_MAX_RESULT_WINDOW = 10000;

  private static final Duration INITIAL_COOLDOWN = Duration.ofSeconds(30);

  private static final Duration MAX_COOLDOWN = Duration.ofMinutes(10);

  private final OpenSearchTransport openSearchTransport;

  private final IndexConfigProvider indexConfigProvider;

  private final ClusterLockManager clusterLockManager;

  private volatile OpenSearchClient openSearchClient;

  private final AtomicLong lastRecordedConnectExceptionEpochMs = new AtomicLong();

  private final AtomicReference<Duration> currentCooldown = new AtomicReference<>(INITIAL_COOLDOWN);

  @Inject
  public OpenSearchSearchIndexClient(
      final ApplicationDAO applicationDAO,
      final LabelDAO labelDAO,
      final OrganizationDAO organizationDAO,
      final OwnerDAO ownerDAO,
      final PolicyDAO policyDAO,
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
      final CurrentUser currentUser,
      final ConversionHelper conversionHelper,
      final OpenSearchTransport openSearchTransport,
      final IndexConfigProvider indexConfigProvider,
      final ClusterLockManager clusterLockManager,
      final ShutdownHandler shutdownHandler)
  {
    super(applicationDAO, labelDAO, organizationDAO, ownerDAO, policyDAO, searchIndexChangeDAO, tagDAO,
        thirdPartySbomMetadataDAO, documentBuilderHelper, productLicense, telemetrySender, luceneComponents,
        advancedSearchTelemetryMetrics, configuration, permissionService, currentUser, conversionHelper,
        shutdownHandler);
    this.openSearchTransport = openSearchTransport;
    this.indexConfigProvider = indexConfigProvider;
    this.clusterLockManager = clusterLockManager;
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
      doPopulateIndex(new OpenSearchIndexingContext(ownerDAO, conversionHelper, () -> {
        IndexConfig newIndexConfig = new IndexConfig();
        newIndexConfig.setIndexMapping(indexConfig.getIndexMapping());
        newIndexConfig.setIndexName(newIndexName);
        return newIndexConfig;
      }, getClient()));
      updateIndexAlias(newIndexName);
      indexRotated = true;

      log.info("all indexing complete");
    }
    catch (Exception e) {
      throw new SearchIndexException("Error creating search index", e);
    }
    finally {
      if (indexRotated) {
        deleteIndex(oldIndexName);
      }
      else {
        if (newIndexCreated) {
          deleteIndex(newIndexName);
        }
      }
    }
    sendAdvancedSearchIndexingTelemetry(System.currentTimeMillis() - start);
    log.info("index creation exit");
  }

  @Override
  public void updateIndex() {
    List<SearchIndexChange> searchIndexChanges = getSearchIndexChanges();
    if (searchIndexChanges.isEmpty()) {
      return;
    }
    try (ClusterLock clusterLock = clusterLockManager.createForSearchIndexUpdate()) {
      if (clusterLock.tryLock()) {
        processSearchIndexChanges(searchIndexChanges,
            new OpenSearchIndexingContext(ownerDAO, conversionHelper, indexConfigProvider, getClient()));
      }
    }
    catch (Exception e) {
      if (shouldThrow(e, lastRecordedConnectExceptionEpochMs, currentCooldown, MAX_COOLDOWN)) {
        throw new SearchIndexException("Error updating the search index", e);
      }
      log.debug("Unable to connect to OpenSearch to update the search index.");
    }
  }

  // Visible for testing
  public boolean shouldThrow(
      final Exception e,
      final AtomicLong lastRecordedExceptionEpochMs,
      final AtomicReference<Duration> currentCooldown,
      final Duration maxCooldown)
  {
    long now = System.currentTimeMillis();
    if (e instanceof ConnectException) {
      Duration duration = Duration.ofMillis(now - lastRecordedExceptionEpochMs.get());
      if (duration.compareTo(currentCooldown.get()) < 0) {
        return false;
      }
    }
    Duration newCooldown = currentCooldown.get();
    newCooldown = newCooldown.multipliedBy(2);
    if (newCooldown.compareTo(maxCooldown) > 0) {
      newCooldown = maxCooldown;
    }
    currentCooldown.set(newCooldown);
    lastRecordedExceptionEpochMs.set(now);
    return true;
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
                  .order(SortOrder.Desc)
              )
          )
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

      List<String> currentSearchAfter = searchAfter;
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
                    .defaultField(FieldIdentifier.VULNERABILITY_ID.label)
                )
            )
            .size(newPageSize)
            .trackTotalHits(new TrackHits.Builder().enabled(true).build())
            .sort(List.of(
                new SortOptions.Builder()
                    .score(new ScoreSort.Builder().order(SortOrder.Desc).build())
                    .build(),
                new SortOptions.Builder()
                    .field(new FieldSort.Builder().field("_id").order(SortOrder.Asc).build())
                    .build()
            ));
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
      if (e instanceof OpenSearchException openSearchException) {
        if (openSearchException.response().toJsonString().contains("too_many_clauses")) {
          throw TOO_MANY_CLAUSES_EXCEPTION;
        }
      }
      if (e instanceof BadRequestException badRequestException) {
        throw badRequestException;
      }
      throw new SearchIndexException(e);
    }
  }

  private int getMaxResultWindow() throws IOException {
    GetIndicesSettingsRequest getIndicesSettingsRequest = new GetIndicesSettingsRequest.Builder()
        .index(indexConfigProvider.getIndexConfig().getIndexName())
        .build();
    GetIndicesSettingsResponse getIndicesSettingsResponse =
        getClient().indices().getSettings(getIndicesSettingsRequest);
    return Optional.ofNullable(
        getIndicesSettingsResponse.result().get(getRealIndexName())).map(
        IndexState::settings).map(IndexSettings::maxResultWindow).orElse(DEFAULT_MAX_RESULT_WINDOW);
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
      throw new RuntimeException("Failed to get current index name for alias '%s'".formatted(
          indexConfigProvider.getIndexConfig().getIndexName()), e);
    }
  }

  private String generateIndexName() {
    return indexConfigProvider.getIndexConfig().getIndexName() + "-" + UUID.randomUUID().toString().replace("-", "");
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
}
