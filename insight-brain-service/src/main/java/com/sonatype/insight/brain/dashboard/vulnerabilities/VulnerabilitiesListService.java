/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

/**
 * Index-backed Martha V1 Vulnerabilities list (My Scan Data).
 * <p>
 * Rows are estate-distinct by {@code vulnerabilityId}. {@link SearchIndexClient#countDistinct} supplies
 * {@code total}. Page assembly walks index pages collapsing hits with {@code putIfAbsent}, then sorts
 * the collected distinct set by CVSS before slicing — same class of per-collection sort caveat as
 * Violations until index-level sort+collapse lands.
 * <p>
 * <b>Sort vs pagination / materialization cap:</b> CVSS ordering applies only to the distinct set
 * materialized in memory ({@link #MAX_DISTINCT_COLLECT}), not across the full estate. Index fetches
 * use {@link #toSearchIndexPage} (0→0, 1→2, …) for {@link SearchIndexClient} compatibility.
 * {@code hasNextPage} is based on consumed rows vs {@code min(total, materializedSize)} so paging
 * stops when the V1 collect cap is reached even if {@code total} is larger.
 * <p>
 * <b>Facets:</b> Severity and ecosystem facet counts are derived by bucketing a
 * {@link #collectDistinct} materialization (same V1 cap as the page), not by issuing one
 * {@code countDistinct} per severity band. When a facet dimension is filtered, at most one extra
 * collect runs with that dimension omitted so sibling buckets stay visible — never an N-query
 * fan-out per band.
 * <p>
 * Catalog tab delegates to {@link VulnerabilitiesCatalogListService} (HDS vulnerability search).
 * <p>
 * My Scan Data index reads go through {@link SearchIndexClient} so
 * {@code ReadableContextAuthzCache} (PR-0 / CLM-42705) applies automatically — do not fork a
 * parallel session/authz stack; {@code IndexReadSessionFactory.open()} is for searchAfter cutover
 * surfaces only.
 */
@Named
@Singleton
public class VulnerabilitiesListService
{
  public static final int DEFAULT_PAGE_SIZE = 25;

  public static final int MAX_PAGE_SIZE = 100;

  public static final int MAX_SEARCH_LENGTH = 200;

  /** Max distinct vulnerabilityIds materialized for sort/slice in V1. */
  static final int MAX_DISTINCT_COLLECT = 5_000;

  private static final int INDEX_FETCH_PAGE_SIZE = 100;

  private static final int MAX_INDEX_PAGES = 50;

  private static final List<String> ESTATE_VULNERABILITY_KEY_FIELDS =
      List.of(FieldIdentifier.VULNERABILITY_ID.label);

  private static final List<CvssV3Severity> SEVERITY_FACET_BANDS = List.of(
      CvssV3Severity.CRITICAL,
      CvssV3Severity.HIGH,
      CvssV3Severity.MEDIUM,
      CvssV3Severity.LOW,
      CvssV3Severity.NONE);

  private final SearchIndexClient searchIndexClient;

  private final VulnerabilitiesListIndexQueryBuilder indexQueryBuilder;

  private final VulnerabilitiesListRequestValidator requestValidator;

  private final VulnerabilitiesCatalogListService catalogListService;

  @Inject
  public VulnerabilitiesListService(
      final SearchIndexClient searchIndexClient,
      final VulnerabilitiesListIndexQueryBuilder indexQueryBuilder,
      final VulnerabilitiesListRequestValidator requestValidator,
      final VulnerabilitiesCatalogListService catalogListService)
  {
    this.searchIndexClient = searchIndexClient;
    this.indexQueryBuilder = indexQueryBuilder;
    this.requestValidator = requestValidator;
    this.catalogListService = catalogListService;
  }

  public VulnerabilitiesListResponseDTO listVulnerabilities(final VulnerabilitiesListRequestDTO request) {
    int page = request == null || request.page == null ? 0 : request.page;
    int pageSize = request == null || request.pageSize == null ? DEFAULT_PAGE_SIZE : request.pageSize;
    String search = request == null ? null : request.search;
    boolean includeFacets = request == null || request.includeFacets == null || request.includeFacets;

    validatePagination(page, pageSize);
    validateSearch(search);
    requestValidator.validate(request);

    String tab = VulnerabilitiesListRequestValidator.normalizeTab(request == null ? null : request.tab);
    if (VulnerabilitiesListRequestValidator.TAB_CATALOG.equals(tab)) {
      return catalogListService.listCatalog(request, page, pageSize, includeFacets);
    }

    String orderBy = request == null || StringUtils.isBlank(request.orderBy)
        ? VulnerabilitiesListRequestValidator.DEFAULT_ORDER_BY
        : request.orderBy;

    String query = indexQueryBuilder.buildMyScanDataQuery(request);
    long total = searchIndexClient.countDistinct(query, ESTATE_VULNERABILITY_KEY_FIELDS);

    LinkedHashMap<String, SearchResultItemDTO> distinct = collectDistinct(query);
    List<VulnerabilityRowDTO> rows = new ArrayList<>(distinct.size());
    for (SearchResultItemDTO item : distinct.values()) {
      rows.add(toRow(item));
    }
    rows.sort(comparator(orderBy));

    long fromL = (long) page * pageSize;
    List<VulnerabilityRowDTO> pageRows;
    if (fromL >= rows.size()) {
      pageRows = List.of();
    }
    else {
      int from = (int) fromL;
      int to = (int) Math.min(fromL + pageSize, rows.size());
      pageRows = rows.subList(from, to);
    }

    VulnerabilitiesListResponseDTO response = new VulnerabilitiesListResponseDTO();
    response.vulnerabilities = new ArrayList<>(pageRows);
    response.total = total;
    response.page = page;
    response.pageSize = pageSize;
    response.hasNextPage = hasNextPage(page, pageSize, pageRows.size(), total, rows.size());
    response.source = VulnerabilitiesListResponseDTO.SOURCE_INDEX;
    if (includeFacets) {
      response.facets = buildFacets(request, total, distinct);
    }
    return response;
  }

  private VulnerabilitiesListFacetsDTO buildFacets(
      final VulnerabilitiesListRequestDTO request,
      final long total,
      final LinkedHashMap<String, SearchResultItemDTO> mainDistinct)
  {
    VulnerabilitiesListFacetsDTO facets = new VulnerabilitiesListFacetsDTO();
    facets.totalVulnerabilities = total;
    // Omit only the dimension being faceted so sibling buckets stay visible; reuse the main
    // collect when that dimension is not filtered (zero extra index walks).
    LinkedHashMap<String, SearchResultItemDTO> severitySource = hasSeverityFilter(request)
        ? collectDistinct(indexQueryBuilder.buildMyScanDataQuery(request, false, true, true))
        : mainDistinct;
    LinkedHashMap<String, SearchResultItemDTO> ecosystemSource = hasEcosystemFilter(request)
        ? collectDistinct(indexQueryBuilder.buildMyScanDataQuery(request, true, true, false))
        : mainDistinct;
    facets.severities = bucketSeverityFacets(severitySource);
    facets.ecosystems = bucketEcosystemFacets(ecosystemSource);
    return facets;
  }

  static Map<String, Long> bucketSeverityFacets(final Map<String, SearchResultItemDTO> distinct) {
    Map<String, Long> severities = new LinkedHashMap<>();
    for (CvssV3Severity band : SEVERITY_FACET_BANDS) {
      severities.put(band.name().toLowerCase(Locale.ROOT), 0L);
    }
    for (SearchResultItemDTO item : distinct.values()) {
      String band = VulnerabilitiesListRequestValidator.severityBand(item.vulnerabilitySeverity);
      if (band == null || !VulnerabilitiesListRequestValidator.SUPPORTED_SEVERITIES.contains(band)) {
        band = CvssV3Severity.NONE.name().toLowerCase(Locale.ROOT);
      }
      severities.merge(band, 1L, Long::sum);
    }
    return severities;
  }

  static Map<String, Long> bucketEcosystemFacets(final Map<String, SearchResultItemDTO> distinct) {
    Map<String, Long> ecosystems = new LinkedHashMap<>();
    for (SearchResultItemDTO item : distinct.values()) {
      if (item.componentIdentifier == null || StringUtils.isBlank(item.componentIdentifier.getFormat())) {
        continue;
      }
      String format = item.componentIdentifier.getFormat().toLowerCase(Locale.ROOT);
      ecosystems.merge(format, 1L, Long::sum);
    }
    return ecosystems;
  }

  private static boolean hasSeverityFilter(final VulnerabilitiesListRequestDTO request) {
    return request != null && request.severities != null && !request.severities.isEmpty();
  }

  private static boolean hasEcosystemFilter(final VulnerabilitiesListRequestDTO request) {
    return request != null && request.ecosystems != null && !request.ecosystems.isEmpty();
  }

  private LinkedHashMap<String, SearchResultItemDTO> collectDistinct(final String query) {
    LinkedHashMap<String, SearchResultItemDTO> distinct = new LinkedHashMap<>();
    for (int indexPage = 0; indexPage < MAX_INDEX_PAGES && distinct.size() < MAX_DISTINCT_COLLECT; indexPage++) {
      SearchResultDTO searchResult = searchIndexClient.searchIndex(
          query,
          INDEX_FETCH_PAGE_SIZE,
          toSearchIndexPage(indexPage),
          false,
          false,
          List.of());
      VulnerabilitiesListIndexItems.mergeDistinctVulnerabilityItems(searchResult, distinct);
      boolean exhaustedPage = searchResult.groupingByDTOS == null
          || searchResult.groupingByDTOS.isEmpty()
          || countItems(searchResult) < INDEX_FETCH_PAGE_SIZE;
      if (exhaustedPage) {
        break;
      }
    }
    return distinct;
  }

  private static int countItems(final SearchResultDTO searchResult) {
    int count = 0;
    if (searchResult.groupingByDTOS == null) {
      return 0;
    }
    for (var group : searchResult.groupingByDTOS) {
      if (group.searchResultItemDTOS != null) {
        count += group.searchResultItemDTOS.size();
      }
    }
    return count;
  }

  static int toSearchIndexPage(final int zeroBasedPage) {
    return zeroBasedPage == 0 ? 0 : zeroBasedPage + 1;
  }

  /**
   * True while more rows remain within the pageable window ({@code min(total, materializedSize)}).
   * Uses consumed = page×pageSize + pageRowCount so an empty page past the materialization cap
   * does not keep {@code hasNextPage=true} when estate {@code total} exceeds the V1 collect cap.
   */
  static boolean hasNextPage(
      final long page,
      final int pageSize,
      final int pageRowCount,
      final long total,
      final int materializedSize)
  {
    long consumed = page * pageSize + pageRowCount;
    return consumed < Math.min(total, materializedSize);
  }

  static Comparator<VulnerabilityRowDTO> comparator(final String orderBy) {
    boolean ascending = "cvssScore".equals(orderBy);
    Comparator<Float> scoreOrder = ascending ? Comparator.naturalOrder() : Comparator.reverseOrder();
    return Comparator
        .comparing((VulnerabilityRowDTO row) -> row.cvssScore, Comparator.nullsLast(scoreOrder))
        .thenComparing(
            (VulnerabilityRowDTO row) -> row.vulnerabilityId,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
  }

  private static VulnerabilityRowDTO toRow(final SearchResultItemDTO item) {
    VulnerabilityRowDTO row = new VulnerabilityRowDTO();
    row.vulnerabilityId = item.vulnerabilityId;
    row.title = item.vulnerabilityDescription;
    row.cvssScore = item.vulnerabilitySeverity;
    row.severity = VulnerabilitiesListRequestValidator.severityBand(item.vulnerabilitySeverity);
    if (item.componentIdentifier != null) {
      row.ecosystem = item.componentIdentifier.getFormat();
    }
    return row;
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
