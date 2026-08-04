/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.vulnerabilities.VulnerabilitiesListIndexQueryBuilder.FacetDimension;
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
 * {@code total}. Page assembly walks index pages collapsing hits with {@code putIfAbsent}, accumulates
 * distinct {@code applicationPublicId}s per vulnerability for
 * {@code applicationCount}, then sorts the
 * collected distinct set by CVSS before slicing — same class of per-collection sort caveat as
 * Violations until index-level sort+collapse lands. When the collect walk stops early (distinct or
 * index-page caps), {@code applicationCount} is a lower bound and {@code applicationCountExact} is
 * {@code false}; the Impact tab's per-vuln walk can surface additional applications.
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
 * Organization, application, and stage facets cannot be bucketed that way, because they vary
 * across the uncollapsed hits behind a single row; {@link VulnerabilitiesListScopeFacetsBuilder}
 * aggregates them instead.
 * <p>
 * Catalog tab delegates to {@link VulnerabilitiesCatalogListService} (HDS vulnerability search).
 * <p>
 * Row and count reads go through {@link SearchIndexClient} so {@code ReadableContextAuthzCache}
 * (PR-0 / CLM-42705) applies automatically — do not fork a parallel session/authz stack for them.
 * The scope facets builder is the one exception and opens its own short-lived read session for
 * term aggregation, which {@link SearchIndexClient} does not expose; it applies RBAC through the
 * session the same way {@code ComponentsListFacetsBuilder} does.
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

  /** Max distinct applications materialized for a single vulnerability Applications tab. */
  static final int MAX_AFFECTED_APPLICATIONS = 500;

  /** Max distinct components materialized for a single vulnerability Components Impacted tab. */
  static final int MAX_IMPACTED_COMPONENTS = 500;

  private static final int INDEX_FETCH_PAGE_SIZE = 100;

  private static final int MAX_INDEX_PAGES = 50;

  private static final int MAX_AFFECTED_APP_INDEX_PAGES = 50;

  private static final int MAX_IMPACTED_COMPONENT_INDEX_PAGES = 50;

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

  private final VulnerabilitiesListScopeFacetsBuilder scopeFacetsBuilder;

  @Inject
  public VulnerabilitiesListService(
      final SearchIndexClient searchIndexClient,
      final VulnerabilitiesListIndexQueryBuilder indexQueryBuilder,
      final VulnerabilitiesListRequestValidator requestValidator,
      final VulnerabilitiesCatalogListService catalogListService,
      final VulnerabilitiesListScopeFacetsBuilder scopeFacetsBuilder)
  {
    this.searchIndexClient = searchIndexClient;
    this.indexQueryBuilder = indexQueryBuilder;
    this.requestValidator = requestValidator;
    this.catalogListService = catalogListService;
    this.scopeFacetsBuilder = scopeFacetsBuilder;
  }

  /**
   * Distinct applications with My Scan Data hits for {@code vulnerabilityId}, sorted by name.
   * <p>
   * When both {@code page} and {@code pageSize} are omitted, returns the full collected list (still
   * subject to walk caps). When either paging param is supplied, slices with defaults
   * ({@code page=0}, {@code pageSize=}{@link #DEFAULT_PAGE_SIZE}) for any omitted value.
   * End-of-list for a paged client is {@code !hasNextPage && !truncated} — {@code !hasNextPage}
   * alone can mean more matches exist beyond the walk budget.
   */
  public VulnerabilityAffectedApplicationsResponseDTO listAffectedApplications(
      final String vulnerabilityId,
      final Integer page,
      final Integer pageSize)
  {
    requireVulnerabilityId(vulnerabilityId);
    ImpactPagination paging = resolveImpactPagination(page, pageSize);

    String query = indexQueryBuilder.buildAffectedApplicationsQuery(vulnerabilityId);
    LinkedHashMap<String, VulnerabilityAffectedApplicationDTO> byPublicId = new LinkedHashMap<>();
    boolean scannedEveryMatch = false;
    for (int indexPage = 0; indexPage < MAX_AFFECTED_APP_INDEX_PAGES; indexPage++) {
      if (byPublicId.size() >= MAX_AFFECTED_APPLICATIONS) {
        break;
      }
      SearchResultDTO searchResult = searchIndexClient.searchIndex(
          query,
          INDEX_FETCH_PAGE_SIZE,
          toSearchIndexPage(indexPage),
          false,
          false,
          List.of());
      if (searchResult == null) {
        // A failed lookup is not evidence that the estate holds no more matches. Stop, but leave
        // the scan flagged incomplete so the caller does not render "affects nothing" for an
        // index outage.
        break;
      }
      mergeAffectedApplications(searchResult, byPublicId);
      boolean exhaustedPage = searchResult.groupingByDTOS == null
          || searchResult.groupingByDTOS.isEmpty()
          || countItems(searchResult) < INDEX_FETCH_PAGE_SIZE;
      if (exhaustedPage) {
        scannedEveryMatch = true;
        break;
      }
    }

    List<VulnerabilityAffectedApplicationDTO> applications = new ArrayList<>(byPublicId.values());
    applications.sort(Comparator
        .comparing(
            (VulnerabilityAffectedApplicationDTO row) -> row.applicationName,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
        .thenComparing(
            (VulnerabilityAffectedApplicationDTO row) -> row.applicationPublicId,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

    List<VulnerabilityAffectedApplicationDTO> pageRows =
        paging.unpaged() ? applications : slicePage(applications, paging.page(), paging.pageSize());
    VulnerabilityAffectedApplicationsResponseDTO response = new VulnerabilityAffectedApplicationsResponseDTO();
    response.applications = pageRows;
    response.total = applications.size();
    response.page = paging.unpaged() ? 0 : paging.page();
    response.pageSize = paging.unpaged() ? applications.size() : paging.pageSize();
    response.hasNextPage = paging.unpaged()
        ? false
        : hasNextPage(
            paging.page(), paging.pageSize(), pageRows.size(), applications.size(), applications.size());
    // Only a page that ran out of index hits proves the list is complete. Exhausting the page
    // budget or the distinct-app cap both stop the scan early with matches potentially unseen.
    response.truncated = !scannedEveryMatch || byPublicId.size() >= MAX_AFFECTED_APPLICATIONS;
    return response;
  }

  /**
   * Distinct components with My Scan Data hits for {@code vulnerabilityId}, sorted by name.
   * Same paging contract and walk-cap honesty as {@link #listAffectedApplications}.
   */
  public VulnerabilityImpactedComponentsResponseDTO listImpactedComponents(
      final String vulnerabilityId,
      final Integer page,
      final Integer pageSize)
  {
    requireVulnerabilityId(vulnerabilityId);
    ImpactPagination paging = resolveImpactPagination(page, pageSize);

    String query = indexQueryBuilder.buildAffectedApplicationsQuery(vulnerabilityId);
    LinkedHashMap<String, VulnerabilityImpactedComponentDTO> byHash = new LinkedHashMap<>();
    boolean scannedEveryMatch = false;
    for (int indexPage = 0; indexPage < MAX_IMPACTED_COMPONENT_INDEX_PAGES; indexPage++) {
      if (byHash.size() >= MAX_IMPACTED_COMPONENTS) {
        break;
      }
      SearchResultDTO searchResult = searchIndexClient.searchIndex(
          query,
          INDEX_FETCH_PAGE_SIZE,
          toSearchIndexPage(indexPage),
          false,
          false,
          List.of());
      if (searchResult == null) {
        break;
      }
      mergeImpactedComponents(searchResult, byHash);
      boolean exhaustedPage = searchResult.groupingByDTOS == null
          || searchResult.groupingByDTOS.isEmpty()
          || countItems(searchResult) < INDEX_FETCH_PAGE_SIZE;
      if (exhaustedPage) {
        scannedEveryMatch = true;
        break;
      }
    }

    List<VulnerabilityImpactedComponentDTO> components = new ArrayList<>(byHash.values());
    components.sort(Comparator
        .comparing(
            (VulnerabilityImpactedComponentDTO row) -> row.componentName,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
        .thenComparing(
            (VulnerabilityImpactedComponentDTO row) -> row.componentHash,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

    List<VulnerabilityImpactedComponentDTO> pageRows =
        paging.unpaged() ? components : slicePage(components, paging.page(), paging.pageSize());
    VulnerabilityImpactedComponentsResponseDTO response = new VulnerabilityImpactedComponentsResponseDTO();
    response.components = pageRows;
    response.total = components.size();
    response.page = paging.unpaged() ? 0 : paging.page();
    response.pageSize = paging.unpaged() ? components.size() : paging.pageSize();
    response.hasNextPage = paging.unpaged()
        ? false
        : hasNextPage(
            paging.page(), paging.pageSize(), pageRows.size(), components.size(), components.size());
    response.truncated = !scannedEveryMatch || byHash.size() >= MAX_IMPACTED_COMPONENTS;
    return response;
  }

  /**
   * Omitting both paging params returns the full collected window. Supplying either enables
   * slicing with {@link #DEFAULT_PAGE_SIZE} / page {@code 0} for omitted values.
   */
  private static ImpactPagination resolveImpactPagination(final Integer page, final Integer pageSize) {
    if (page == null && pageSize == null) {
      return new ImpactPagination(0, DEFAULT_PAGE_SIZE, true);
    }
    int safePage = page == null ? 0 : page;
    int safePageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
    validatePagination(safePage, safePageSize);
    return new ImpactPagination(safePage, safePageSize, false);
  }

  private record ImpactPagination(int page, int pageSize, boolean unpaged)
  {
  }

  static void mergeAffectedApplications(
      final SearchResultDTO searchResult,
      final LinkedHashMap<String, VulnerabilityAffectedApplicationDTO> byPublicId)
  {
    if (searchResult == null || searchResult.groupingByDTOS == null) {
      return;
    }
    for (var group : searchResult.groupingByDTOS) {
      if (group == null || group.searchResultItemDTOS == null) {
        continue;
      }
      for (SearchResultItemDTO item : group.searchResultItemDTOS) {
        if (item == null || StringUtils.isBlank(item.applicationPublicId)) {
          continue;
        }
        if (byPublicId.size() >= MAX_AFFECTED_APPLICATIONS) {
          return;
        }
        byPublicId.putIfAbsent(item.applicationPublicId, toAffectedApplication(item));
      }
    }
  }

  static void mergeImpactedComponents(
      final SearchResultDTO searchResult,
      final LinkedHashMap<String, VulnerabilityImpactedComponentDTO> byHash)
  {
    if (searchResult == null || searchResult.groupingByDTOS == null) {
      return;
    }
    for (var group : searchResult.groupingByDTOS) {
      if (group == null || group.searchResultItemDTOS == null) {
        continue;
      }
      for (SearchResultItemDTO item : group.searchResultItemDTOS) {
        if (item == null || StringUtils.isBlank(item.componentHash)) {
          continue;
        }
        if (byHash.size() >= MAX_IMPACTED_COMPONENTS) {
          return;
        }
        byHash.putIfAbsent(item.componentHash, toImpactedComponent(item));
      }
    }
  }

  private static VulnerabilityAffectedApplicationDTO toAffectedApplication(final SearchResultItemDTO item) {
    VulnerabilityAffectedApplicationDTO row = new VulnerabilityAffectedApplicationDTO();
    row.applicationPublicId = item.applicationPublicId;
    row.applicationName = StringUtils.isNotBlank(item.applicationName)
        ? item.applicationName
        : item.applicationPublicId;
    if (StringUtils.isNotBlank(item.organizationName)) {
      row.organizationName = item.organizationName;
    }
    return row;
  }

  private static VulnerabilityImpactedComponentDTO toImpactedComponent(final SearchResultItemDTO item) {
    VulnerabilityImpactedComponentDTO row = new VulnerabilityImpactedComponentDTO();
    row.componentHash = item.componentHash;
    row.componentName = StringUtils.isNotBlank(item.componentName) ? item.componentName : item.componentHash;
    if (item.componentIdentifier != null && StringUtils.isNotBlank(item.componentIdentifier.getFormat())) {
      row.ecosystem = item.componentIdentifier.getFormat();
    }
    return row;
  }

  private static void requireVulnerabilityId(final String vulnerabilityId) {
    if (StringUtils.isBlank(vulnerabilityId)) {
      throw new BadRequestException("vulnerabilityId is required.");
    }
    if (vulnerabilityId.length() > MAX_SEARCH_LENGTH) {
      throw new BadRequestException(
          "vulnerabilityId exceeds maximum length of " + MAX_SEARCH_LENGTH + " characters.");
    }
  }

  private static <T> List<T> slicePage(final List<T> rows, final int page, final int pageSize) {
    long fromL = (long) page * pageSize;
    if (fromL >= rows.size()) {
      return List.of();
    }
    int from = (int) fromL;
    int to = (int) Math.min(fromL + pageSize, rows.size());
    return rows.subList(from, to);
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

    DistinctCollectResult collected = collectDistinct(query);
    LinkedHashMap<String, SearchResultItemDTO> distinct = collected.distinctByVulnerabilityId();
    List<VulnerabilityRowDTO> rows = new ArrayList<>(distinct.size());
    for (SearchResultItemDTO item : distinct.values()) {
      rows.add(toRow(
          item,
          collected.applicationIdsByVulnerabilityId(),
          collected.applicationCountsExact()));
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
        ? collectDistinct(indexQueryBuilder.buildMyScanDataQuery(request, FacetDimension.SEVERITY))
            .distinctByVulnerabilityId()
        : mainDistinct;
    LinkedHashMap<String, SearchResultItemDTO> ecosystemSource = hasEcosystemFilter(request)
        ? collectDistinct(indexQueryBuilder.buildMyScanDataQuery(request, FacetDimension.ECOSYSTEM))
            .distinctByVulnerabilityId()
        : mainDistinct;
    facets.severities = bucketSeverityFacets(severitySource);
    facets.ecosystems = bucketEcosystemFacets(ecosystemSource);
    // Scope facets take grouped aggregations rather than more collect walks — see the builder.
    scopeFacetsBuilder.attachScopeFacets(facets, request);
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

  private DistinctCollectResult collectDistinct(final String query) {
    LinkedHashMap<String, SearchResultItemDTO> distinct = new LinkedHashMap<>();
    Map<String, Set<String>> applicationIdsByVulnerabilityId = new HashMap<>();
    boolean exhaustedIndex = false;
    for (int indexPage = 0; indexPage < MAX_INDEX_PAGES && distinct.size() < MAX_DISTINCT_COLLECT; indexPage++) {
      SearchResultDTO searchResult = searchIndexClient.searchIndex(
          query,
          INDEX_FETCH_PAGE_SIZE,
          toSearchIndexPage(indexPage),
          false,
          false,
          List.of());
      if (searchResult == null) {
        exhaustedIndex = true;
        break;
      }
      VulnerabilitiesListIndexItems.mergeDistinctVulnerabilityItems(
          searchResult, distinct, applicationIdsByVulnerabilityId);
      boolean exhaustedPage = searchResult.groupingByDTOS == null
          || searchResult.groupingByDTOS.isEmpty()
          || countItems(searchResult) < INDEX_FETCH_PAGE_SIZE;
      if (exhaustedPage) {
        exhaustedIndex = true;
        break;
      }
    }
    // Only exact when every matching hit was walked; distinct/page caps yield a lower bound.
    return new DistinctCollectResult(distinct, applicationIdsByVulnerabilityId, exhaustedIndex);
  }

  private record DistinctCollectResult(
      LinkedHashMap<String, SearchResultItemDTO> distinctByVulnerabilityId,
      Map<String, Set<String>> applicationIdsByVulnerabilityId,
      boolean applicationCountsExact)
  {
  }

  private static int countItems(final SearchResultDTO searchResult) {
    int count = 0;
    if (searchResult == null || searchResult.groupingByDTOS == null) {
      return 0;
    }
    for (var group : searchResult.groupingByDTOS) {
      if (group != null && group.searchResultItemDTOS != null) {
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

  private static VulnerabilityRowDTO toRow(
      final SearchResultItemDTO item,
      final Map<String, Set<String>> applicationIdsByVulnerabilityId,
      final boolean applicationCountsExact)
  {
    VulnerabilityRowDTO row = new VulnerabilityRowDTO();
    row.vulnerabilityId = item.vulnerabilityId;
    row.title = item.vulnerabilityDescription;
    row.cvssScore = item.vulnerabilitySeverity;
    row.severity = VulnerabilitiesListRequestValidator.severityBand(item.vulnerabilitySeverity);
    if (item.componentIdentifier != null) {
      row.ecosystem = item.componentIdentifier.getFormat();
    }
    Set<String> applicationIds = applicationIdsByVulnerabilityId == null
        ? null
        : applicationIdsByVulnerabilityId.get(item.vulnerabilityId);
    if (applicationIds != null && !applicationIds.isEmpty()) {
      row.applicationCount = applicationIds.size();
      row.applicationCountExact = applicationCountsExact;
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
