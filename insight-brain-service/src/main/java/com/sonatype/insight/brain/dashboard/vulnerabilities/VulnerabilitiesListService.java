/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dashboard.vulnerabilities.VulnerabilitiesListIndexQueryBuilder.FacetDimension;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.GroupedDistinctCounts;
import com.sonatype.insight.brain.search.index.IndexFilterRestriction;
import com.sonatype.insight.brain.search.index.IndexTermSetRestriction;
import com.sonatype.insight.brain.search.index.RankedGroup;
import com.sonatype.insight.brain.search.index.RankedGroupsResult;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Index-backed Martha V1 Vulnerabilities list (My Scan Data).
 * <p>
 * Rows are estate-distinct by {@code vulnerabilityId}. A single
 * {@link SearchIndexClient#rankGroupsByMaxMetric} call ranks the whole RBAC-scoped estate by the highest
 * CVSS score per vulnerability and returns, from that one pass, the ordered ids, the distinct estate
 * {@code total}, and the per-severity-band distinct counts. Ordering is the primitive's contract, so
 * this class never sorts rows itself. Ranking stops at {@link #MAX_RANK_DEPTH}; {@code hasNextPage} is
 * {@code false} once a caller reaches that bound even when {@code total} is larger.
 * <p>
 * <b>Hydration:</b> a follow-up read fetches one representative document per ranked id on the current
 * page and supplies row title and ecosystem, stopping as soon as those ids are covered and bounded by
 * {@link #MAX_HYDRATION_DOCS}. The page's ids are applied as a boolean-clause-budget-exempt terms
 * filter (CLM-44783), so hydrate cost does not grow with organization-scope clause spend. Ranked ids
 * are lower-cased by the index primitive, so hydrated documents are keyed on their lower-cased id.
 * Per-row application counts come from {@link SearchIndexClient#countDistinctGroupedBy} rather than
 * from that walk, so a vulnerability spanning thousands of applications costs one aggregation instead of a
 * document-by-document scan.
 * <p>
 * <b>Facets:</b> {@code total} and the severity bands come from the ranking pass and cover the whole
 * estate; ecosystem counts come from a grouped aggregation over the closed set of component formats.
 * Both are recomputed with their own dimension dropped when it is filtered, so a selection never zeroes
 * the sibling values a user would widen to. Organization, application, and stage facets vary across the
 * uncollapsed hits behind a single row, so {@link VulnerabilitiesListScopeFacetsBuilder} aggregates them
 * separately.
 * <p>
 * Catalog tab delegates to {@link VulnerabilitiesCatalogListService} (HDS vulnerability search).
 * <p>
 * Row and count reads go through {@link SearchIndexClient} so {@code ReadableContextAuthzCache}
 * (PR-0) applies automatically — do not fork a parallel session/authz stack for them.
 * The scope facets builder is the one exception and opens its own short-lived read session for
 * term aggregation, which {@link SearchIndexClient} does not expose; it applies RBAC through the
 * session the same way {@code ComponentsListFacetsBuilder} does.
 */
@Named
@Singleton
public class VulnerabilitiesListService
{
  private static final Logger log = LoggerFactory.getLogger(VulnerabilitiesListService.class);

  public static final int DEFAULT_PAGE_SIZE = 25;

  public static final int MAX_PAGE_SIZE = 100;

  public static final int MAX_SEARCH_LENGTH = 200;

  /** Deepest rank the list will compute, matching the global search total-hits cap precedent. */
  static final int MAX_RANK_DEPTH = 10_000;

  /** Documents read while hydrating row detail; beyond this, remaining rows carry no title or ecosystem. */
  static final int MAX_HYDRATION_DOCS = 50_000;

  /**
   * Documents per hydration read. Larger than the Impact walks' page because a hydration read wants
   * the covering documents in as few round trips as possible, not a caller-sized page.
   */
  static final int HYDRATION_FETCH_PAGE_SIZE = 1_000;

  /** Ranked ids of this shape display upper-cased; see {@link #displayVulnerabilityId}. */
  private static final Pattern CVE_ID = Pattern.compile("cve-\\d{4}-\\d+");

  /** Reads allowed per id batch before giving up on ids that yield no document. */
  static final int MAX_HYDRATION_PAGES_PER_BATCH = 10;

  /** Max distinct applications materialized for a single vulnerability Applications tab. */
  static final int MAX_AFFECTED_APPLICATIONS = 500;

  /** Max distinct components materialized for a single vulnerability Components Impacted tab. */
  static final int MAX_IMPACTED_COMPONENTS = 500;

  private static final int INDEX_FETCH_PAGE_SIZE = 100;

  private static final int MAX_AFFECTED_APP_INDEX_PAGES = 50;

  private static final int MAX_IMPACTED_COMPONENT_INDEX_PAGES = 50;

  private final SearchIndexClient searchIndexClient;

  private final VulnerabilitiesListIndexQueryBuilder indexQueryBuilder;

  private final VulnerabilitiesListRequestValidator requestValidator;

  private final VulnerabilitiesCatalogListService catalogListService;

  private final VulnerabilitiesListScopeFacetsBuilder scopeFacetsBuilder;

  private final ConversionHelper conversionHelper;

  private final Configuration configuration;

  @Inject
  public VulnerabilitiesListService(
      final SearchIndexClient searchIndexClient,
      final VulnerabilitiesListIndexQueryBuilder indexQueryBuilder,
      final VulnerabilitiesListRequestValidator requestValidator,
      final VulnerabilitiesCatalogListService catalogListService,
      final VulnerabilitiesListScopeFacetsBuilder scopeFacetsBuilder,
      final ConversionHelper conversionHelper,
      final Configuration configuration)
  {
    this.searchIndexClient = searchIndexClient;
    this.indexQueryBuilder = indexQueryBuilder;
    this.requestValidator = requestValidator;
    this.catalogListService = catalogListService;
    this.scopeFacetsBuilder = scopeFacetsBuilder;
    this.conversionHelper = conversionHelper;
    this.configuration = configuration;
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
    List<IndexFilterRestriction> scopeRestrictions = indexQueryBuilder.buildScopeRestrictions(request);

    long consumed = ((long) page + 1) * pageSize;
    // Rank one group past the page so hasNextPage can be read off the ranked set. Deriving it from
    // the total instead would promise a page that does not exist whenever the backend estimates
    // that total high.
    int rankDepth = (int) Math.min(consumed + 1, MAX_RANK_DEPTH);

    RankedGroupsResult ranked = searchIndexClient.rankGroupsByMaxMetric(
        query,
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        rankDepth,
        "cvssScore".equals(orderBy),
        CvssV3Severity.halfOpenScoreBands(),
        scopeRestrictions);

    List<RankedGroup> rankedGroups = ranked.groups();
    long fromL = (long) page * pageSize;
    List<RankedGroup> pageGroups = fromL >= rankedGroups.size()
        ? List.of()
        : rankedGroups.subList((int) fromL, (int) Math.min(fromL + pageSize, rankedGroups.size()));

    HydratedVulnerabilities hydrated = hydrate(query, scopeRestrictions, pageGroups);
    List<VulnerabilityRowDTO> pageRows = new ArrayList<>(pageGroups.size());
    for (RankedGroup group : pageGroups) {
      pageRows.add(toRow(group, hydrated));
    }

    VulnerabilitiesListResponseDTO response = new VulnerabilitiesListResponseDTO();
    response.vulnerabilities = pageRows;
    response.total = ranked.distinctGroupCount();
    response.totalExact = ranked.distinctGroupCountExact();
    response.page = page;
    response.pageSize = pageSize;
    response.hasNextPage = rankedGroups.size() > consumed && consumed < MAX_RANK_DEPTH;
    response.source = VulnerabilitiesListResponseDTO.SOURCE_INDEX;
    if (includeFacets) {
      try {
        response.facets = buildFacets(request, ranked);
      }
      catch (RuntimeException e) {
        log.warn("Vulnerabilities list facet build failed; returning page without facets", e);
      }
    }
    return response;
  }

  /**
   * Reads the detail behind the current page's ranked ids. Title and ecosystem need one representative
   * document per id, so the walk narrows the query by {@code vulnerabilityId} and touches a small slice
   * of the index whatever the estate size. Application counts are aggregated over the whole matching set
   * instead, so a vulnerability present in every application costs the same as one present in a single
   * application.
   */
  private HydratedVulnerabilities hydrate(
      final String query,
      final List<IndexFilterRestriction> scopeRestrictions,
      final List<RankedGroup> pageGroups)
  {
    LinkedHashSet<String> ids = new LinkedHashSet<>();
    for (RankedGroup group : pageGroups) {
      // A blank id would drop out of the restricting clause and widen the walk to the whole estate.
      if (StringUtils.isNotBlank(group.groupValue())) {
        ids.add(group.groupValue());
      }
    }
    if (ids.isEmpty()) {
      return new HydratedVulnerabilities(new LinkedHashMap<>(), Map.of(), true);
    }

    LinkedHashMap<String, SearchResultItemDTO> items = new LinkedHashMap<>();
    HydrationBudget budget = new HydrationBudget();
    List<String> allIds = new ArrayList<>(ids);
    // Org/app scope + vulnerability-id restriction are both budget-exempt term sets, so the whole
    // page's ids fit one read regardless of organization expansion size (CLM-44783). Scope must be
    // present on application-count aggregation so per-row counts stay within the user's filter.
    List<IndexFilterRestriction> hydrationRestrictions =
        mergeHydrationRestrictions(scopeRestrictions, allIds);
    walkHydrationBatch(query, allIds, hydrationRestrictions, items, budget);

    // Term-set restriction scopes the Lucene collector the same way string id clauses did before
    // CLM-44783, without spending the boolean clause budget. Exactness follows the producing backend
    // (hybrid failover must not borrow primary backendId()).
    GroupedDistinctCounts applicationCountResult = searchIndexClient.countDistinctGroupedByWithExactness(
        query,
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.APPLICATION_PUBLIC_ID.label,
        allIds,
        hydrationRestrictions);
    return new HydratedVulnerabilities(
        byRankedId(items), applicationCountResult.counts(), applicationCountResult.exact());
  }

  private static List<IndexFilterRestriction> mergeHydrationRestrictions(
      final List<IndexFilterRestriction> scopeRestrictions,
      final List<String> vulnerabilityIds)
  {
    List<IndexFilterRestriction> scope =
        scopeRestrictions == null ? List.of() : scopeRestrictions;
    List<IndexFilterRestriction> merged = new ArrayList<>(scope.size() + 1);
    merged.addAll(scope);
    merged.add(IndexTermSetRestriction.of(FieldIdentifier.VULNERABILITY_ID.label, vulnerabilityIds));
    return List.copyOf(merged);
  }

  /**
   * Pages until every id has yielded a document. Rows need one representative hit each for title and
   * ecosystem, so a vulnerability present in thousands of applications does not drag the walk through
   * thousands of documents.
   * <p>
   * The page count is bounded as well as the document budget: an id that matches the ranking pass but
   * yields no document — one whose hits were deleted between the two reads — would otherwise never
   * satisfy the covered check and would page until the whole budget was spent. Each read is a full
   * collection on Lucene and restarts the walk on OpenSearch, so the page is large and the bound is
   * small rather than the reverse.
   */
  private void walkHydrationBatch(
      final String baseQuery,
      final List<String> batch,
      final List<IndexFilterRestriction> hydrationRestrictions,
      final LinkedHashMap<String, SearchResultItemDTO> items,
      final HydrationBudget budget)
  {
    // Narrowed as pages land rather than recomputed from everything hydrated so far, so the cost of
    // the covered check follows the page that was just read.
    Set<String> awaitingDocument = new HashSet<>(batch);
    for (int indexPage = 0; indexPage < MAX_HYDRATION_PAGES_PER_BATCH && !budget.exhausted(); indexPage++) {
      SearchResultDTO searchResult = searchIndexClient.searchIndex(
          baseQuery,
          HYDRATION_FETCH_PAGE_SIZE,
          toSearchIndexPage(indexPage),
          false,
          false,
          List.of(),
          hydrationRestrictions);
      if (searchResult == null) {
        return;
      }
      Set<String> pageIds = VulnerabilitiesListIndexItems.mergeDistinctVulnerabilityItems(searchResult, items, null);
      pageIds.forEach(vulnerabilityId -> awaitingDocument.remove(vulnerabilityId.toLowerCase(Locale.ROOT)));
      int pageCount = countItems(searchResult);
      budget.consume(pageCount);
      if (pageCount < HYDRATION_FETCH_PAGE_SIZE || awaitingDocument.isEmpty()) {
        return;
      }
    }
  }

  /** Shared document allowance across hydration batches. */
  private static final class HydrationBudget
  {
    private int read;

    boolean exhausted() {
      return read >= MAX_HYDRATION_DOCS;
    }

    void consume(final int documents) {
      read += documents;
    }
  }

  /**
   * Ranked group values are lower-cased by the index primitive while hydrated documents carry their
   * display casing, so hydrated lookups are re-keyed to match the ranked id.
   */
  private static LinkedHashMap<String, SearchResultItemDTO> byRankedId(
      final LinkedHashMap<String, SearchResultItemDTO> items)
  {
    LinkedHashMap<String, SearchResultItemDTO> byRankedId = new LinkedHashMap<>();
    items.forEach((vulnerabilityId, item) -> byRankedId.putIfAbsent(vulnerabilityId.toLowerCase(Locale.ROOT), item));
    return byRankedId;
  }

  /**
   * @param applicationCountsByVulnerabilityId distinct applications per vulnerability, keyed on the
   *          ranked lower-cased id, absent when the vulnerability has none
   * @param applicationCountsExact false when the backend counts distinct values by estimation
   */
  private record HydratedVulnerabilities(
      LinkedHashMap<String, SearchResultItemDTO> itemsByVulnerabilityId,
      Map<String, Long> applicationCountsByVulnerabilityId,
      boolean applicationCountsExact)
  {
  }

  private VulnerabilitiesListFacetsDTO buildFacets(
      final VulnerabilitiesListRequestDTO request,
      final RankedGroupsResult ranked)
  {
    VulnerabilitiesListFacetsDTO facets = new VulnerabilitiesListFacetsDTO();
    facets.totalVulnerabilities = ranked.distinctGroupCount();
    RankedGroupsResult bandSource = severityBandSource(request, ranked);
    facets.severities = severityFacets(bandSource);
    // The bands are distinct counts from the same read that produced the total, so they carry that
    // read's exactness rather than being exact in their own right.
    facets.severitiesExact = bandSource.distinctGroupCountExact();
    GroupedDistinctCounts ecosystems = ecosystemFacets(request);
    facets.ecosystems = ecosystems.counts();
    // Ecosystem counts come from countDistinctGroupedBy (exact on Lucene, HLL on OpenSearch) — not
    // from the ranking pass. Exactness is reported by the producing backend (hybrid-safe).
    facets.ecosystemsExact = ecosystems.exact();
    // Hierarchical owner (org/app) facets, aggregated over an owner-removed base so that selecting an
    // org/app does not collapse the org/app rails. The builder opens its own read session and degrades to
    // no owner facets on failure, so the query build and parse sit inside the same guard: a failure there
    // must cost the owner rails only, not the severity and ecosystem bands already populated above.
    try {
      String ownerRemovedQueryString = indexQueryBuilder.buildVulnerabilityQueryWithoutOwner(request);
      Query ownerRemovedBase = conversionHelper.stringToQuery(ownerRemovedQueryString);
      scopeFacetsBuilder.attachScopeFacets(facets, ownerRemovedBase, request);
    }
    catch (RuntimeException e) {
      log.warn("Vulnerabilities owner facets unavailable; returning the other facets", e);
    }
    return facets;
  }

  /**
   * Ranking result the severity bands are read from. A severity or CVSS selection narrows the main
   * query, which would report every unselected band as zero and leave the user unable to widen the
   * selection, so those bands come from a second pass with that dimension dropped. The pass ranks a
   * single group because band counts and the distinct total cover every group regardless of depth.
   */
  private RankedGroupsResult severityBandSource(
      final VulnerabilitiesListRequestDTO request,
      final RankedGroupsResult ranked)
  {
    if (!hasSeverityFilter(request)) {
      return ranked;
    }
    return searchIndexClient.rankGroupsByMaxMetric(
        indexQueryBuilder.buildMyScanDataQuery(request, FacetDimension.SEVERITY),
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        1,
        false,
        CvssV3Severity.halfOpenScoreBands(),
        indexQueryBuilder.buildScopeRestrictions(request, FacetDimension.SEVERITY));
  }

  /**
   * Distinct vulnerabilities per component format, over the whole estate rather than a sample, with
   * the ecosystem selection dropped so unselected formats stay pickable. Formats are a closed set, so
   * one grouped aggregation replaces bucketing the formats seen on a page.
   */
  private GroupedDistinctCounts ecosystemFacets(final VulnerabilitiesListRequestDTO request) {
    return searchIndexClient.countDistinctGroupedByWithExactness(
        indexQueryBuilder.buildMyScanDataQuery(request, FacetDimension.ECOSYSTEM),
        FieldIdentifier.COMPONENT_FORMAT.label,
        FieldIdentifier.VULNERABILITY_ID.label,
        ComponentIdentifier.getAllFormats(),
        indexQueryBuilder.buildScopeRestrictions(request, FacetDimension.ECOSYSTEM));
  }

  private static boolean hasSeverityFilter(final VulnerabilitiesListRequestDTO request) {
    if (request == null) {
      return false;
    }
    // The CVSS clamp narrows the same field the bands describe, so it collapses them the same way.
    return (request.severities != null && !request.severities.isEmpty())
        || request.minCvssScore != null
        || request.maxCvssScore != null;
  }

  /**
   * Band counts with unscored and out-of-range vulnerabilities folded into {@code none}, so the buckets
   * sum to the total. This matches how {@code severityBand} resolves a single row.
   */
  static Map<String, Long> severityFacets(final RankedGroupsResult ranked) {
    Map<String, Long> severities = new LinkedHashMap<>(ranked.bandCounts());
    String none = CvssV3Severity.NONE.name().toLowerCase(Locale.ROOT);
    severities.merge(none, ranked.unbandedGroupCount(), Long::sum);
    return severities;
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
   * True while more rows remain within the pageable window ({@code min(total, materializedSize)}), for
   * the Impact tabs, whose rows are materialized by a bounded walk. Uses consumed = page×pageSize +
   * pageRowCount so an empty page past the materialization cap does not keep {@code hasNextPage=true}
   * when {@code total} is larger than the walk produced.
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

  private static VulnerabilityRowDTO toRow(
      final RankedGroup group,
      final HydratedVulnerabilities hydrated)
  {
    VulnerabilityRowDTO row = new VulnerabilityRowDTO();
    row.vulnerabilityId = displayVulnerabilityId(group.groupValue());
    row.cvssScore = group.metricValue();
    row.severity = VulnerabilitiesListRequestValidator.severityBand(group.metricValue());

    SearchResultItemDTO item = hydrated.itemsByVulnerabilityId().get(group.groupValue());
    if (item != null) {
      row.vulnerabilityId = item.vulnerabilityId;
      row.title = item.vulnerabilityDescription;
      if (item.componentIdentifier != null) {
        row.ecosystem = item.componentIdentifier.getFormat();
      }
    }
    Long applicationCount = hydrated.applicationCountsByVulnerabilityId().get(group.groupValue());
    if (applicationCount != null && applicationCount > 0) {
      row.applicationCount = applicationCount.intValue();
      row.applicationCountExact = hydrated.applicationCountsExact();
    }
    return row;
  }

  /**
   * Display form of a ranked id, which the index primitive hands back lower-cased. A hydrated
   * document supplies the real casing; this is what a row falls back to when the page's ids exhaust
   * the hydration budget before that document is reached. Only the CVE form is restored, because it
   * is the one whose casing is fixed — a GHSA id keeps a lower-cased suffix and a Sonatype id a
   * lower-cased prefix, so upper-casing either would be wrong.
   */
  private static String displayVulnerabilityId(final String rankedId) {
    return rankedId != null && CVE_ID.matcher(rankedId).matches()
        ? rankedId.toUpperCase(Locale.ROOT)
        : rankedId;
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
