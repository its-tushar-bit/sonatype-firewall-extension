/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

/**
 * Resolves distinct {@code componentHash} values matching violation-scoped filters (stage, threat).
 * <p>
 * Discovery is capped at {@link Configuration#getMaxAdvancedSearchClauseCount()} unique hashes and
 * {@link #MAX_VIOLATION_DISCOVERY_RAW_PAGES} raw index pages. The raw-page budget is the estate-scale
 * guard (a hot hash can fill many consecutive pages without yielding new distinct hashes — stopping
 * on "no new hashes" would miss later components).
 * <p>
 * V1 uses the legacy {@link SearchIndexClient#searchIndex} read path only (no Components session surface yet).
 */
@Named
@Singleton
final class ComponentsListViolationScopeResolver
{
  private static final int VIOLATION_DISCOVERY_PAGE_SIZE = 500;

  /** Hard cap on raw violation pages walked per discovery (500 × 80 = 40k docs). */
  static final int MAX_VIOLATION_DISCOVERY_RAW_PAGES = 80;

  /**
   * Fallback discovery cap when configuration is unset. Kept at the scoped-hash filter clause
   * budget so discovered hashes can always be expressed as one Lucene OR without TooManyClauses.
   */
  private static final int DEFAULT_MAX_DISCOVERY_IDS =
      ComponentsListIndexQueryBuilder.MAX_SCOPED_COMPONENT_HASH_FILTER_CLAUSES;

  private final SearchIndexClient searchIndexClient;

  private final Configuration configuration;

  @Inject
  ComponentsListViolationScopeResolver(
      final SearchIndexClient searchIndexClient,
      final Configuration configuration)
  {
    this.searchIndexClient = searchIndexClient;
    this.configuration = configuration;
  }

  Set<String> resolveComponentHashes(
      final String baseComponentQuery,
      final Set<String> stageIds,
      final List<PolicyThreatLevelFilter> threatFilters)
  {
    String violationQuery = buildScopedViolationQuery(baseComponentQuery, stageIds, threatFilters);
    int maxIds = configuration.getMaxAdvancedSearchClauseCount();
    if (maxIds <= 0) {
      maxIds = DEFAULT_MAX_DISCOVERY_IDS;
    }
    // Never discover more hashes than the componentHash OR clause can safely express.
    maxIds = Math.min(maxIds, ComponentsListIndexQueryBuilder.MAX_SCOPED_COMPONENT_HASH_FILTER_CLAUSES);
    return resolveHashesWithSearchIndex(violationQuery, maxIds);
  }

  private Set<String> resolveHashesWithSearchIndex(final String violationQuery, final int maxIds) {
    LinkedHashSet<String> hashes = new LinkedHashSet<>();
    int page = 0;
    while (hashes.size() < maxIds && page < MAX_VIOLATION_DISCOVERY_RAW_PAGES) {
      SearchResultDTO searchResult = searchIndexClient.searchIndex(
          violationQuery,
          VIOLATION_DISCOVERY_PAGE_SIZE,
          ComponentsListService.toSearchIndexPage(page),
          false,
          false,
          List.of());
      if (searchResult == null || searchResult.groupingByDTOS == null || searchResult.groupingByDTOS.isEmpty()) {
        break;
      }
      int rawHitsThisPage = countRawViolationHits(searchResult);
      boolean discoveryCapped = false;
      for (var group : searchResult.groupingByDTOS) {
        if (group == null || group.searchResultItemDTOS == null) {
          continue;
        }
        for (SearchResultItemDTO item : group.searchResultItemDTOS) {
          if (item == null || StringUtils.isBlank(item.componentHash)) {
            continue;
          }
          hashes.add(item.componentHash);
          if (hashes.size() >= maxIds) {
            discoveryCapped = true;
            break;
          }
        }
        if (discoveryCapped) {
          break;
        }
      }
      if (discoveryCapped) {
        if (rawHitsThisPage >= VIOLATION_DISCOVERY_PAGE_SIZE) {
          throw new BadRequestException(
              "Violation-scoped component discovery matched too many components (max " + maxIds + ").");
        }
        break;
      }
      if (rawHitsThisPage < VIOLATION_DISCOVERY_PAGE_SIZE) {
        break;
      }
      page++;
      if (page >= MAX_VIOLATION_DISCOVERY_RAW_PAGES) {
        throw new BadRequestException(
            "Violation-scoped component discovery exceeded the maximum raw-page walk ("
                + MAX_VIOLATION_DISCOVERY_RAW_PAGES
                + "). Narrow stage/threat filters.");
      }
    }
    return hashes;
  }

  private String buildScopedViolationQuery(
      final String baseComponentQuery,
      final Set<String> stageIds,
      final List<PolicyThreatLevelFilter> threatFilters)
  {
    String violationQuery = ComponentsListViolationQuerySupport.toViolationQuery(baseComponentQuery);
    List<String> scopedClauses = new ArrayList<>(2);
    int maxClauseCount = configuration.getMaxAdvancedSearchClauseCount();
    if (maxClauseCount <= 0) {
      maxClauseCount = DEFAULT_MAX_DISCOVERY_IDS;
    }
    String stageClause = ComponentsListViolationQuerySupport.buildStageFilterClause(stageIds, maxClauseCount);
    if (stageClause != null) {
      scopedClauses.add(stageClause);
    }
    String threatClause = ComponentsListViolationQuerySupport.buildThreatFilterClause(threatFilters);
    if (threatClause != null) {
      scopedClauses.add(threatClause);
    }
    return ComponentsListViolationQuerySupport.appendClauses(violationQuery, scopedClauses);
  }

  private static int countRawViolationHits(final SearchResultDTO searchResult) {
    int rawHits = 0;
    for (var group : searchResult.groupingByDTOS) {
      if (group != null && group.searchResultItemDTOS != null) {
        rawHits += group.searchResultItemDTOS.size();
      }
    }
    return rawHits;
  }
}
