/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.search.global.StaleCursorException;
import com.sonatype.insight.brain.search.global.GlobalSearchRequest;
import com.sonatype.insight.brain.search.global.GlobalSearchResult;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.session.HybridIndexReadSession;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hybrid search index client that supports OpenSearch with Lucene fallback.
 * <p>
 * This client is designed to support the transition period when OpenSearch is performing its first full index.
 * During this time:
 * <ul>
 * <li>Write operations are sent to both primary and secondary clients</li>
 * <li>Read operations are attempted on the primary client first, falling back to secondary if primary fails</li>
 * <li>Updates are paused when either client is performing a full re-index</li>
 * </ul>
 * <p>
 * The primary client is typically OpenSearch (target state) and the secondary is Lucene (fallback).
 */
public class HybridSearchIndexClient
    implements SearchIndexClient
{
  private static final Logger log = LoggerFactory.getLogger(HybridSearchIndexClient.class);

  private final SearchIndexClient primaryClient;

  private final SearchIndexClient secondaryClient;

  private final AtomicBoolean primaryReindexing = new AtomicBoolean(false);

  private final AtomicBoolean secondaryReindexing = new AtomicBoolean(false);

  @Inject
  public HybridSearchIndexClient(
      @Named("primary") final SearchIndexClient primaryClient,
      @Named("secondary") final SearchIndexClient secondaryClient)
  {
    this.primaryClient = primaryClient;
    this.secondaryClient = secondaryClient;
    log.debug("Initialized HybridSearchIndexClient with primary: {} and secondary: {}",
        primaryClient.getClass().getSimpleName(), secondaryClient.getClass().getSimpleName());
  }

  /**
   * Populates both primary and secondary indexes.
   * Sets reindexing flags to pause incremental updates during the operation.
   */
  @Override
  public void populateIndex() {
    log.debug("Starting full index population on both primary and secondary clients");

    // Set reindexing flag for primary client to pause incremental updates
    primaryReindexing.set(true);

    // Populate primary client first (OpenSearch - the target state)
    try {
      log.debug("Populating primary client index");
      primaryClient.populateIndex();
      log.info("Primary client index population completed");
    }
    catch (Exception e) {
      log.error("Failed to populate primary client index", e);
      throw new SearchIndexException("Failed to populate primary client index", e);
    }
    finally {
      primaryReindexing.set(false);
    }

    // Set reindexing flag for secondary client
    secondaryReindexing.set(true);

    // Then populate secondary client (Lucene - the fallback)
    try {
      log.debug("Populating secondary client index");
      secondaryClient.populateIndex();
      log.info("Secondary client index population completed");
    }
    catch (Exception e) {
      log.error("Failed to populate secondary client index", e);
      throw new SearchIndexException("Failed to populate secondary client index", e);
    }
    finally {
      secondaryReindexing.set(false);
    }
  }

  /**
   * Updates the index on both clients if neither is currently re-indexing.
   * If either client is re-indexing, the update is skipped to avoid processing the same changes twice.
   * <p>
   * We pass a no-op deletion callback to the delegate clients to prevent them from deleting
   * SearchIndexChanges during their updateIndex() calls. Instead, we delete the changes here
   * after both clients have successfully processed them.
   * <p>
   * If both clients fail to process changes, the changes will be retried on the next updateIndex() call.
   * To prevent unbounded accumulation, we limit the number of pending changes and delete excess if needed.
   */
  @Override
  public void updateIndex(
      final List<SearchIndexChange> searchIndexChanges,
      final Consumer<SearchIndexChange> deletionCallback)
  {
    // Skip updates if either client is performing a full re-index
    if (primaryReindexing.get() || secondaryReindexing.get()) {
      return;
    }

    if (searchIndexChanges.isEmpty()) {
      return;
    }

    // Pass a no-op callback to prevent the delegates from deleting changes
    try {
      primaryClient.updateIndex(searchIndexChanges, change -> {
      });
    }
    catch (Exception e) {
      log.warn("Failed to update primary client index, continuing with secondary", e);
    }

    try {
      secondaryClient.updateIndex(searchIndexChanges, change -> {
      });
    }
    catch (Exception e) {
      log.warn("Failed to update secondary client index", e);
    }

    try {
      for (SearchIndexChange searchIndexChange : searchIndexChanges) {
        // Delete a change if at least one client successfully processed it
        if (searchIndexChange.isProcessed()) {
          deletionCallback.accept(searchIndexChange);
        }
      }
    }
    catch (Exception e) {
      log.error("Failed to delete search index changes after processing", e);
    }

    List<SearchIndexChange> unprocessedSearchIndexChanges =
        searchIndexChanges.stream().filter(searchIndexChange -> !searchIndexChange.isProcessed()).toList();
    if (!unprocessedSearchIndexChanges.isEmpty()) {
      final int maxPendingChanges = 10000;
      if (searchIndexChanges.size() > maxPendingChanges) {
        int excessChanges = unprocessedSearchIndexChanges.size() - maxPendingChanges;
        List<SearchIndexChange> changesToDelete = unprocessedSearchIndexChanges.subList(0, excessChanges);

        log.warn("Both primary and secondary clients failed to process changes. " +
            "Deleting {} oldest changes to prevent unbounded accumulation (limit: {}). " +
            "Total pending changes: {}", excessChanges, maxPendingChanges, searchIndexChanges.size());
        try {
          changesToDelete.forEach(this::deleteSearchIndexChange);
        }
        catch (Exception e) {
          log.error("Failed to delete excess search index changes after both clients failed", e);
        }
      }
      else {
        log.warn("Both primary and secondary clients failed to process {} changes. " +
            "Changes will be retried on next update cycle.", searchIndexChanges.size());
      }
    }
  }

  /**
   * Returns the last index time from the primary client.
   * Falls back to secondary client if primary fails.
   */
  @Override
  public Long getLastIndexTime() {
    Long lastIndexTime = null;
    try {
      lastIndexTime = primaryClient.getLastIndexTime();
    }
    catch (Exception e) {
      log.warn("Failed to get last index time from primary client, falling back to secondary", e);
    }

    if (lastIndexTime != null) {
      return lastIndexTime;
    }

    // Fallback to secondary
    try {
      lastIndexTime = secondaryClient.getLastIndexTime();
    }
    catch (Exception e) {
      log.error("Failed to get last index time from both primary and secondary clients", e);
    }

    return lastIndexTime;
  }

  /**
   * Returns the index size from the primary client.
   * Falls back to secondary client if primary fails.
   */
  @Override
  public long getIndexSize() {
    try {
      return primaryClient.getIndexSize();
    }
    catch (Exception e) {
      log.warn("Failed to get index size from primary client, falling back to secondary", e);
    }

    // Fallback to secondary
    try {
      return secondaryClient.getIndexSize();
    }
    catch (Exception e) {
      log.error("Failed to get index size from both primary and secondary clients", e);
    }

    return 0L;
  }

  /**
   * Searches the index using the primary client first.
   * Falls back to the secondary client if the primary fails.
   * <p>
   * This ensures search functionality remains available even if the primary client
   * is unavailable or still building its index.
   */
  @Override
  public SearchResultDTO searchIndex(
      final String searchQuery,
      final int pageSize,
      final int page,
      final boolean allComponents,
      final boolean isSbomManagerMode,
      final List<String> searchAfter)
  {
    log.debug("Searching index with query: {}", searchQuery);

    Exception primaryException;
    try {
      SearchResultDTO result = primaryClient.searchIndex(
          searchQuery, pageSize, page, allComponents, isSbomManagerMode, searchAfter);
      log.debug("Search completed successfully using primary client");
      return result;
    }
    catch (Exception e) {
      log.debug("Search failed on primary client, falling back to secondary. Error: {}", e.getMessage(), e);
      primaryException = e;
    }

    // Fallback to secondary client
    Exception secondaryException;
    try {
      SearchResultDTO result = secondaryClient.searchIndex(
          searchQuery, pageSize, page, allComponents, isSbomManagerMode, searchAfter);
      log.debug("Search completed successfully using secondary client (fallback)");
      return result;
    }
    catch (Exception e) {
      log.error("Search failed on both primary and secondary clients", e);
      secondaryException = e;
    }

    if (primaryException instanceof ConflictException conflictException) {
      throw conflictException;
    }
    else {
      throw new SearchIndexException(
          "Search failed on both primary and secondary clients. " + "Primary error: " + primaryException.getMessage() +
              ", Secondary error: " + secondaryException.getMessage(),
          secondaryException);
    }
  }

  @Override
  public List<SearchIndexChange> getSearchIndexChanges() {
    return primaryClient.getSearchIndexChanges();
  }

  @Override
  public void deleteSearchIndexChange(final SearchIndexChange change) {
    primaryClient.deleteSearchIndexChange(change);
  }

  /**
   * Attempts the primary client first, falling back to secondary when primary fails — same pattern
   * as {@link #searchIndex} so dashboard tiles stay available during the OpenSearch re-index window.
   */
  @Override
  public long count(String metricQuery) {
    Exception primaryException = null;
    try {
      return primaryClient.count(metricQuery);
    }
    catch (Exception e) {
      log.warn("Failed to count from primary client, falling back to secondary", e);
      primaryException = e;
    }

    try {
      long result = secondaryClient.count(metricQuery);
      log.debug("Count completed successfully using secondary client (fallback)");
      return result;
    }
    catch (Exception e) {
      log.error("Failed to count from both primary and secondary clients", e);
      if (primaryException instanceof ConflictException conflictException) {
        throw conflictException;
      }
      throw new SearchIndexException(
          "Count failed on both primary and secondary clients. Primary error: " + primaryException.getMessage() +
              ", Secondary error: " + e.getMessage(),
          e);
    }
  }

  /**
   * Attempts the primary client first, falling back to secondary when primary fails — same pattern
   * as {@link #count}.
   */
  @Override
  public MetricAggregationResult aggregateCountByField(
      String metricQuery,
      String bucketField,
      Map<String, int[]> ranges)
  {
    Exception primaryException = null;
    try {
      return primaryClient.aggregateCountByField(metricQuery, bucketField, ranges);
    }
    catch (Exception e) {
      log.warn("Failed to aggregate counts from primary client, falling back to secondary", e);
      primaryException = e;
    }

    try {
      MetricAggregationResult result =
          secondaryClient.aggregateCountByField(metricQuery, bucketField, ranges);
      log.debug("Aggregate count completed successfully using secondary client (fallback)");
      return result;
    }
    catch (Exception e) {
      log.error("Failed to aggregate counts from both primary and secondary clients", e);
      if (primaryException instanceof ConflictException conflictException) {
        throw conflictException;
      }
      throw new SearchIndexException(
          "Aggregate count failed on both primary and secondary clients. Primary error: " +
              primaryException.getMessage() + ", Secondary error: " + e.getMessage(),
          e);
    }
  }

  /**
   * Attempts the primary client first, falling back to secondary when primary fails — same pattern
   * as {@link #aggregateCountByField}.
   */
  @Override
  public MetricAggregationResult aggregateCountByFloatField(
      String metricQuery,
      String bucketField,
      Map<String, float[]> ranges,
      String distinctField)
  {
    Exception primaryException = null;
    try {
      return primaryClient.aggregateCountByFloatField(metricQuery, bucketField, ranges, distinctField);
    }
    catch (Exception e) {
      log.warn("Failed to aggregate float counts from primary client, falling back to secondary", e);
      primaryException = e;
    }

    try {
      MetricAggregationResult result =
          secondaryClient.aggregateCountByFloatField(metricQuery, bucketField, ranges, distinctField);
      log.debug("Float aggregate count completed successfully using secondary client (fallback)");
      return result;
    }
    catch (Exception e) {
      log.error("Failed to aggregate float counts from both primary and secondary clients", e);
      if (primaryException instanceof ConflictException conflictException) {
        throw conflictException;
      }
      throw new SearchIndexException(
          "Float aggregate count failed on both primary and secondary clients. Primary error: " +
              primaryException.getMessage() + ", Secondary error: " + e.getMessage(),
          e);
    }
  }

  @Override
  public long countDistinct(String metricQuery, List<String> compositeKeyFields) {
    Exception primaryException = null;
    try {
      return primaryClient.countDistinct(metricQuery, compositeKeyFields);
    }
    catch (Exception e) {
      log.warn("Failed to count distinct from primary client, falling back to secondary", e);
      primaryException = e;
    }

    try {
      long result = secondaryClient.countDistinct(metricQuery, compositeKeyFields);
      log.debug("Distinct count completed successfully using secondary client (fallback)");
      return result;
    }
    catch (Exception e) {
      log.error("Failed to count distinct from both primary and secondary clients", e);
      if (primaryException instanceof ConflictException conflictException) {
        throw conflictException;
      }
      throw new SearchIndexException(
          "Distinct count failed on both primary and secondary clients. Primary error: " +
              primaryException.getMessage() + ", Secondary error: " + e.getMessage(),
          e);
    }
  }

  @Override
  public Map<String, Long> countDistinctGroupedBy(
      final String metricQuery,
      final String groupField,
      final String distinctField,
      final Collection<String> groupValues)
  {
    Exception primaryException = null;
    try {
      return primaryClient.countDistinctGroupedBy(metricQuery, groupField, distinctField, groupValues);
    }
    catch (Exception e) {
      log.warn("Failed to count distinct grouped from primary client, falling back to secondary", e);
      primaryException = e;
    }

    try {
      Map<String, Long> result =
          secondaryClient.countDistinctGroupedBy(metricQuery, groupField, distinctField, groupValues);
      log.debug("Grouped distinct count completed successfully using secondary client (fallback)");
      return result;
    }
    catch (Exception e) {
      log.error("Failed to count distinct grouped from both primary and secondary clients", e);
      if (primaryException instanceof ConflictException conflictException) {
        throw conflictException;
      }
      throw new SearchIndexException(
          "Grouped distinct count failed on both primary and secondary clients. Primary error: " +
              primaryException.getMessage() + ", Secondary error: " + e.getMessage(),
          e);
    }
  }

  /**
   * Falls back to the secondary only on infrastructure failure, never on client errors. Fallback is
   * refused once {@code searchAfter} is non-empty since cursor tuples are backend-specific.
   */
  @Override
  public GlobalSearchResult searchGlobal(final GlobalSearchRequest request) {
    Exception primary;
    try {
      return primaryClient.searchGlobal(request);
    }
    catch (BadRequestException | ConflictException | StaleCursorException clientError) {
      throw clientError;
    }
    catch (Exception infra) {
      if (!request.searchAfter().isEmpty()) {
        log.warn("Primary search failed with a cursor in flight; refusing fallback to preserve cursor pinning",
            infra);
        StaleCursorException stale =
            new StaleCursorException("Primary backend unavailable mid-pagination; retry from page 1");
        stale.addSuppressed(infra);
        throw stale;
      }
      log.warn("Primary search failed, falling back to secondary", infra);
      primary = infra;
    }

    try {
      GlobalSearchResult secondaryResult = secondaryClient.searchGlobal(request);
      // Pin the page to the secondary's backendId so the next-page cursor minted from it embeds the
      // secondary's generation token. If the primary recovers before the follow-up request, the
      // primary-expected token will not match and the cursor is rejected as stale (retry from page
      // 1) instead of feeding a secondary-format cursor to the primary.
      return new GlobalSearchResult(
          secondaryResult.rows(),
          secondaryResult.totalHits(),
          secondaryResult.nextSearchAfter(),
          secondaryResult.exactTotalHits(),
          secondaryClient.backendId());
    }
    catch (BadRequestException | ConflictException | StaleCursorException clientError) {
      clientError.addSuppressed(primary);
      throw clientError;
    }
    catch (Exception secondary) {
      log.warn("Search failed on secondary after primary failure", secondary);
      SearchIndexException wrapped =
          new SearchIndexException("Search failed on both primary and secondary clients", secondary);
      wrapped.addSuppressed(primary);
      throw wrapped;
    }
  }

  @Override
  public String backendId() {
    return primaryClient.backendId();
  }

  @Override
  public boolean isGlobalSearchEnabled() {
    return primaryClient.isGlobalSearchEnabled();
  }

  /**
   * Permission-filter helpers delegate to the primary client: the filter is constructed against
   * the primary index's {@code allowedContextIds} field, so there is no secondary fallback (unlike
   * {@link #count}) — a permission filter from the wrong index would be meaningless.
   */
  @Override
  public Set<String> getCurrentUserContextIdsWithReadPermission() {
    return primaryClient.getCurrentUserContextIdsWithReadPermission();
  }

  @Override
  public Query buildAllowedContextIdsFilter(final Set<String> userPermittedContextIds) {
    return primaryClient.buildAllowedContextIdsFilter(userPermittedContextIds);
  }

  @Override
  public Query wrapWithPermissionFilter(final Query baseQuery, final Query permissionFilter) {
    return primaryClient.wrapWithPermissionFilter(baseQuery, permissionFilter);
  }

  @Override
  public void checkGlobalSearchMode(final boolean isSbomManagerMode) {
    primaryClient.checkGlobalSearchMode(isSbomManagerMode);
  }

  public static IndexReadSession pinReadSession(final IndexReadSession session) {
    return new HybridIndexReadSession(session);
  }

  /**
   * Returns the primary search index client (typically OpenSearch).
   * This is useful for tests and diagnostic purposes.
   *
   * @return the primary client
   */
  public SearchIndexClient getPrimaryClient() {
    return primaryClient;
  }

  /**
   * Returns the secondary search index client (typically Lucene).
   * This is useful for tests and diagnostic purposes.
   *
   * @return the secondary client
   */
  public SearchIndexClient getSecondaryClient() {
    return secondaryClient;
  }
}
