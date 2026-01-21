/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.search.results.SearchResultDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.module.installer.scanner.InvisibleForScanner;

/**
 * Hybrid search index client that supports OpenSearch with Lucene fallback.
 * <p>
 * This client is designed to support the transition period when OpenSearch is performing its first full index.
 * During this time:
 * <ul>
 *   <li>Write operations are sent to both primary and secondary clients</li>
 *   <li>Read operations are attempted on the primary client first, falling back to secondary if primary fails</li>
 *   <li>Updates are paused when either client is performing a full re-index</li>
 * </ul>
 * <p>
 * The primary client is typically OpenSearch (target state) and the secondary is Lucene (fallback).
 */
@Singleton
@InvisibleForScanner
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
   */
  @Override
  public void updateIndex() {
    // Skip updates if either client is performing a full re-index
    if (primaryReindexing.get() || secondaryReindexing.get()) {
      log.debug("Skipping incremental update - reindexing in progress (primary: {}, secondary: {})",
          primaryReindexing.get(), secondaryReindexing.get());
      return;
    }

    // Update both clients to keep them in sync
    try {
      primaryClient.updateIndex();
    }
    catch (Exception e) {
      log.warn("Failed to update primary client index, continuing with secondary", e);
    }

    try {
      secondaryClient.updateIndex();
    }
    catch (Exception e) {
      log.warn("Failed to update secondary client index", e);
    }
  }

  /**
   * Returns the last index time from the primary client.
   * Falls back to secondary client if primary fails.
   */
  @Override
  public Long getLastIndexTime() {
    try {
      return primaryClient.getLastIndexTime();
    }
    catch (Exception e) {
      log.warn("Failed to get last index time from primary client, falling back to secondary", e);
      try {
        return secondaryClient.getLastIndexTime();
      }
      catch (Exception e2) {
        log.error("Failed to get last index time from both primary and secondary clients", e2);
        return null;
      }
    }
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
      try {
        return secondaryClient.getIndexSize();
      }
      catch (Exception e2) {
        log.error("Failed to get index size from both primary and secondary clients", e2);
        return 0L;
      }
    }
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

    // Try primary client first
    try {
      SearchResultDTO result = primaryClient.searchIndex(
          searchQuery, pageSize, page, allComponents, isSbomManagerMode, searchAfter);
      log.debug("Search completed successfully using primary client");
      return result;
    }
    catch (Exception e) {
      log.debug("Search failed on primary client, falling back to secondary. Error: {}", e.getMessage(), e);
      // Fall back to secondary client
      try {
        SearchResultDTO result = secondaryClient.searchIndex(
            searchQuery, pageSize, page, allComponents, isSbomManagerMode, searchAfter);
        log.debug("Search completed successfully using secondary client (fallback)");
        return result;
      }
      catch (Exception e2) {
        log.error("Search failed on both primary and secondary clients", e2);
        throw new SearchIndexException(
            "Search failed on both primary and secondary clients. " +
                "Primary error: " + e.getMessage() + ", Secondary error: " + e2.getMessage(), e2);
      }
    }
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
