/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dashboard.ComponentRiskDTO;
import com.sonatype.insight.brain.dashboard.ComponentRiskDTOComparator;
import com.sonatype.insight.brain.dashboard.DashboardComponentRiskService;
import com.sonatype.insight.brain.dashboard.DashboardResultsDTO;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexFilterRestriction;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Index-backed Martha V1 Components list.
 * <p>
 * Rows are distinct {@code componentHash} values folded from
 * {@code NON_VULNERABLE_COMPONENT} and {@code SECURITY_VULNERABILITY} docs. Index owns discovery,
 * RBAC, paging, and facets; Classic SQL enriches the visible page (risk scores, affected apps,
 * format/coords display names) — same hybrid as Applications.
 * <p>
 * Because the index returns multi-doc hits per hash, paging walks raw index pages until the
 * requested distinct-hash window is filled ({@link ComponentsListDistinctPageFetcher}) so
 * {@code page}/{@code hasNextPage}/{@code total} stay in distinct-hash units.
 * <p>
 * Index reads use {@link SearchIndexClient} (PR-0 / CLM-42705). RBAC for the current user is applied
 * via {@code resolveReadableContextRbacFilterForCurrentUser()} — {@code ReadableContextAuthzCache}
 * is injected into the client stack. There is no {@code SearchReadPathSurface.COMPONENTS} yet; do
 * not fork a parallel session/authz stack.
 * <p>
 * Request defaults: {@code page=0}, {@code pageSize=50}, {@code includeFacets=true},
 * {@code orderBy=-TOTAL_RISK} when omitted.
 * <p>
 * Sort orders rows within each index page after enrichment — same per-page caveat as Violations /
 * Applications until index-level sort lands.
 */
@Named
@Singleton
public class ComponentsListService
{
  private static final Logger log = LoggerFactory.getLogger(ComponentsListService.class);

  public static final int DEFAULT_PAGE_SIZE = 50;

  public static final int MAX_PAGE_SIZE = 100;

  public static final int MAX_SEARCH_LENGTH = 200;

  private final SearchIndexClient searchIndexClient;

  private final ComponentsListDistinctPageFetcher distinctPageFetcher;

  private final ComponentsListIndexQueryBuilder indexQueryBuilder;

  private final ComponentsListRequestValidator requestValidator;

  private final ComponentsListFacetsBuilder facetsBuilder;

  private final DashboardComponentRiskService componentRiskService;

  @Inject
  public ComponentsListService(
      final SearchIndexClient searchIndexClient,
      final ComponentsListIndexQueryBuilder indexQueryBuilder,
      final ComponentsListRequestValidator requestValidator,
      final ComponentsListFacetsBuilder facetsBuilder,
      final DashboardComponentRiskService componentRiskService)
  {
    this.searchIndexClient = searchIndexClient;
    this.distinctPageFetcher = new ComponentsListDistinctPageFetcher(searchIndexClient);
    this.indexQueryBuilder = indexQueryBuilder;
    this.requestValidator = requestValidator;
    this.facetsBuilder = facetsBuilder;
    this.componentRiskService = componentRiskService;
  }

  public ComponentsListResponseDTO listComponents(final ComponentsListRequestDTO request) {
    int page = request == null || request.page == null ? 0 : request.page;
    int pageSize = request == null || request.pageSize == null ? DEFAULT_PAGE_SIZE : request.pageSize;
    String search = request == null ? null : request.search;
    boolean includeFacets = request == null || request.includeFacets == null || request.includeFacets;

    // Soft-clamp deep pages (stale deep links / aggressive paging) instead of a hard 400.
    if (page > ComponentsListDistinctPageFetcher.MAX_DISTINCT_PAGE) {
      page = ComponentsListDistinctPageFetcher.MAX_DISTINCT_PAGE;
    }
    validatePagination(page, pageSize);
    validateSearch(search);
    requestValidator.validate(request);

    String orderBy = request == null || StringUtils.isBlank(request.orderBy)
        ? ComponentsListRequestValidator.DEFAULT_ORDER_BY
        : request.orderBy;

    ComponentsIndexQuery indexQuery = indexQueryBuilder.buildComponentIndexQuery(request);
    String query = indexQuery.query();
    ComponentsListDistinctPageFetcher.DistinctPage distinctPage =
        distinctPageFetcher.fetch(query, indexQuery.termSets(), page, pageSize);
    LinkedHashMap<String, SearchResultItemDTO> pageItems = distinctPage.pageItems();
    Map<String, Set<String>> affectedApps = distinctPage.affectedApplicationIds();

    List<ComponentRiskDTO> rows = new ArrayList<>();
    if (!pageItems.isEmpty()) {
      List<ComponentRiskDTO> enriched = List.of();
      try {
        DashboardResultsDTO<ComponentRiskDTO> risks = componentRiskService.getComponentRiskCards(
            request == null ? null : request.organizationIds,
            request == null ? null : request.applicationIds,
            pageItems.keySet(),
            request == null ? null : request.stageIds,
            request == null ? null : request.tagIds,
            request == null ? null : request.policyThreatCategories,
            ComponentsListViolationQuerySupport.threatLevelFilterForCardEnrichment(request),
            request == null ? null : request.policyViolationStates);
        enriched = risks.dashboardResults == null ? List.of() : risks.dashboardResults;
      }
      catch (ConflictException e) {
        // Only Classic Dashboard licence checks throw ConflictException here; real enrichment
        // failures (DB, policy load) propagate as 500 so operators see actionable errors.
        log.warn(
            "Components list SQL enrich skipped (Classic Dashboard license conflict); "
                + "returning index stubs without risk scores",
            e);
      }
      rows = mergeIndexPageWithEnrichment(pageItems, affectedApps, enriched);
    }
    rows.sort(new ComponentRiskDTOComparator(orderBy));

    long total = countDistinctComponents(query, indexQuery.termSets());

    ComponentsListResponseDTO response = new ComponentsListResponseDTO();
    response.components = rows;
    response.total = total;
    response.page = page;
    response.pageSize = pageSize;
    // Distinct-hash units only — never compare raw-doc offsets to countDistinct(total).
    response.hasNextPage = distinctPage.hasNextPage();
    response.source = ComponentsListResponseDTO.SOURCE_INDEX;
    if (includeFacets) {
      try {
        response.facets = facetsBuilder.buildFacets(query, indexQuery.termSets(), total);
      }
      catch (RuntimeException e) {
        log.warn("Components list facet build failed; returning page without facets", e);
      }
    }
    return response;
  }

  private long countDistinctComponents(final String query, final List<IndexFilterRestriction> termSets) {
    try {
      return searchIndexClient.countDistinct(
          query,
          List.of(FieldIdentifier.COMPONENT_HASH.label),
          termSets);
    }
    catch (RuntimeException e) {
      // Never fall back to raw multi-doc hit counts — those inflate CVE×app rows vs distinct hashes.
      log.warn("countDistinct(componentHash) failed; returning total=0", e);
      return 0L;
    }
  }

  /**
   * Prefers Classic SQL cards (scores, estate affected-apps, format/coords displayName). Falls back
   * to index stubs so the page still renders when enrichment is unavailable or a hash has no open
   * violations in SQL.
   */
  static List<ComponentRiskDTO> mergeIndexPageWithEnrichment(
      final LinkedHashMap<String, SearchResultItemDTO> pageItems,
      final Map<String, Set<String>> affectedApps,
      final List<ComponentRiskDTO> enriched)
  {
    Map<String, ComponentRiskDTO> enrichedByHash = enriched == null
        ? Map.of()
        : enriched.stream()
            .filter(card -> card.hash != null)
            .collect(Collectors.toMap(card -> card.hash, Function.identity(), (left, right) -> left));

    List<ComponentRiskDTO> cards = new ArrayList<>(pageItems.size());
    for (Map.Entry<String, SearchResultItemDTO> entry : pageItems.entrySet()) {
      ComponentRiskDTO card = enrichedByHash.get(entry.getKey());
      if (card != null) {
        cards.add(card);
      }
      else {
        int pageLocalApps = affectedApps == null
            ? 0
            : affectedApps.getOrDefault(entry.getKey(), Set.of()).size();
        cards.add(toIndexStubRow(entry.getValue(), pageLocalApps));
      }
    }
    return cards;
  }

  private static ComponentRiskDTO toIndexStubRow(final SearchResultItemDTO item, final int affectedApplications) {
    ComponentRiskDTO row = new ComponentRiskDTO();
    row.hash = item.componentHash;
    row.affectedApplications = affectedApplications;
    if (item.componentIdentifier != null) {
      try {
        row.displayName = ComponentDisplayNameUtil.fromIdentifier(item.componentIdentifier.toComponentIdentifier());
      }
      catch (RuntimeException ignored) {
        // Fall through to filename/hash naming.
      }
    }
    if (row.displayName == null) {
      if (StringUtils.isNotBlank(item.componentName)) {
        row.displayName = ComponentDisplayNameUtil.fromFilename(item.componentName, item.componentHash);
      }
      else {
        row.displayName = ComponentDisplayNameUtil.fromFilename(null, item.componentHash);
      }
    }
    row.derivedComponentName = ComponentDisplayNameUtil.deriveComponentName(row);
    if (StringUtils.isBlank(row.derivedComponentName)) {
      row.derivedComponentName = StringUtils.defaultIfBlank(item.componentName, item.componentHash);
    }
    return row;
  }

  /**
   * {@link SearchIndexClient#searchIndex} uses {@code page=0} as a first-page sentinel and
   * 1-based pages thereafter — same contract as Applications / Advanced Search.
   */
  static int toSearchIndexPage(final int zeroBasedPage) {
    return zeroBasedPage == 0 ? 0 : zeroBasedPage + 1;
  }

  private static void validatePagination(final int page, final int pageSize) {
    if (page < 0) {
      throw new BadRequestException("Invalid page: " + page + ". Page must be >= 0.");
    }
    if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
      throw new BadRequestException(
          "Invalid page size: " + pageSize + ". Page size must be between 1 and " + MAX_PAGE_SIZE + ".");
    }
  }

  private static void validateSearch(final String search) {
    if (search != null && search.length() > MAX_SEARCH_LENGTH) {
      throw new BadRequestException(
          "Search query exceeds maximum length of " + MAX_SEARCH_LENGTH + " characters.");
    }
  }
}
