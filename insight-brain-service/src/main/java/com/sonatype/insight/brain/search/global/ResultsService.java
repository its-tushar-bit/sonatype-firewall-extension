/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.sonatype.insight.brain.search.global.catalog.GlobalSearchResultsCatalogClient;
import com.sonatype.insight.brain.search.global.catalog.GlobalSearchResultsCatalogClientImpl;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.indexquery.IndexQueryResponse.IndexQueryFacetBucket;
import com.sonatype.insight.brain.search.indexquery.IndexQueryService;
import com.sonatype.insight.brain.security.CurrentUser;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single entry point for {@code GET /rest/search/results}.
 *
 * <p>
 * Dispatches by {@link Tab}:
 * <ul>
 * <li>IQ-local tabs → {@link GlobalSearchResultsIqLocalClient}.</li>
 * <li>Catalog tabs ({@link Tab#COMPONENT}, {@link Tab#VULNERABILITY}) → {@link GlobalSearchResultsCatalogClient},
 * degrading the catalog section on any failure (catalog off, 5xx, 429, timeout).</li>
 * <li>{@link Tab#ALL} → {@link AllTabPacker} stitching all sections together in fixed presentation order.
 * Catalog sections degrade independently on failure.</li>
 * </ul>
 *
 * <p>
 * The dispatcher runs the IQ-local and catalog legs sequentially today; a parallel orchestrator (virtual
 * threads + per-leg timeout) is owed as a follow-up and will wrap this service rather than replace it. The
 * single-tab count-only probe already fans out in parallel through {@link AllTabPacker#countTotals} (the
 * same virtual-thread pool + bounded semaphore + per-section timeout the ALL-tab pack uses).
 *
 * <p>
 * The single-tab sibling count probe is opt-in via {@code includeTabCounts}: it issues one count-only
 * search per sibling section on top of the caller's own page search, so a caller that renders one tab's
 * rows without badges must not be charged for it.
 */
@Named
@Singleton
public class ResultsService
{
  private static final Logger log = LoggerFactory.getLogger(ResultsService.class);

  /**
   * Sections the catalog source cannot answer. Derived from {@link AllTabPacker#SECTION_ORDER} minus the
   * two catalog-backed tabs, so adding a section to the enum cannot silently leave it reported as a false
   * {@code 0} on a catalog-source request.
   */
  private static final Set<Tab> CATALOG_UNSERVABLE_TABS = buildCatalogUnservableTabs();

  private final GlobalSearchResultsIqLocalClient iqLocalResultsService;

  private final GlobalSearchResultsCatalogClient catalogClient;

  private final IndexQueryService indexQueryService;

  private final PerUserRateLimiter rateLimiter;

  private final CurrentUser currentUser;

  @Inject
  public ResultsService(
      final GlobalSearchResultsIqLocalClient iqLocalResultsService,
      final GlobalSearchResultsCatalogClient catalogClient,
      final IndexQueryService indexQueryService,
      final CurrentUser currentUser)
  {
    this(iqLocalResultsService, catalogClient, indexQueryService, currentUser,
        new PerUserRateLimiter(PerUserRateLimiter.DEFAULT_PERMITS_PER_USER));
  }

  /** Test-friendly constructor allowing the caller to inject a rate limiter. */
  ResultsService(
      final GlobalSearchResultsIqLocalClient iqLocalResultsService,
      final GlobalSearchResultsCatalogClient catalogClient,
      final IndexQueryService indexQueryService,
      final CurrentUser currentUser,
      final PerUserRateLimiter rateLimiter)
  {
    this.iqLocalResultsService = iqLocalResultsService;
    this.catalogClient = catalogClient;
    this.indexQueryService = Objects.requireNonNull(indexQueryService, "indexQueryService");
    this.currentUser = currentUser;
    this.rateLimiter = rateLimiter;
  }

  /**
   * Test-friendly constructor for callers that do not care about rate-limiting or the current user.
   * Wires a no-op-effective limiter and a dummy CurrentUser that reports {@code anonymous}.
   */
  ResultsService(
      final GlobalSearchResultsIqLocalClient iqLocalResultsService,
      final GlobalSearchResultsCatalogClient catalogClient,
      final IndexQueryService indexQueryService)
  {
    this(iqLocalResultsService, catalogClient, indexQueryService, new CurrentUser(),
        new PerUserRateLimiter(Integer.MAX_VALUE));
  }

  /**
   * Resolves the request to a {@link ResultsResponse}. Throws:
   * <ul>
   * <li>{@link FilterValidationException} (HTTP 400) for unknown sort keys or requests that exceed the
   * deep-pagination threshold without a cursor;</li>
   * <li>{@link StaleCursorException} (HTTP 410) for cursors whose generation token does not match the
   * current server generation.</li>
   * </ul>
   */
  public ResultsResponse search(ResultsRequest request) {
    // Explicit blank-cursor rejection: an all-whitespace cursor is a client-side mistake that would
    // otherwise silently restart pagination at page 1, hiding the bug.
    if (request.getSearchAfter() != null && request.getSearchAfter().isBlank()) {
      throw new BadRequestException("searchAfter must not be blank");
    }
    validateSort(request);
    validateDeepPaging(request);

    String username = currentUser == null ? null : currentUser.getUsernameOrSystem();
    try (PerUserRateLimiter.Permit ignored = rateLimiter.acquire(username)) {
      if (request.getTab() == Tab.ALL) {
        return searchAll(request);
      }
      if (request.getSource() == SearchSource.CATALOG) {
        return searchCatalog(request);
      }
      return searchLocal(request);
    }
  }

  private ResultsResponse searchLocal(ResultsRequest request) {
    // Cursor validation is owned by the IQ-local leg (IqLocalSearchService.search decodes and
    // re-validates the raw cursor against its own preimage, throwing StaleCursorException on drift).
    // Re-validating here with a separately computed token would diverge from the mint preimage and
    // reject every legitimate page-2 request, so the dispatcher does not second-guess it.
    SectionResult section =
        iqLocalResultsService.searchNative(request).orElseGet(() -> SectionResult.empty(request.getTab()));
    // Facets are computed ONLY when the caller asked and ONLY for a single IQ-local entity tab. ALL is
    // handled by searchAll (no facets); the catalog leg is HDS-backed and emits no IQ-local facets.
    // facetsForResults returns null for tabs with no facet set (COMPONENT/VULNERABILITY today).
    //
    // First-page-only, matching the sibling count probe below: the facet map is a property of the query,
    // not of the page. facetsForResults pins its seed query to page 1 / RELEVANCE / default size and
    // counts every bucket with a whole-corpus count query, so it returns the same map on every page.
    // Recomputing it while paging would repeat a full index search plus one count query per bucket to
    // rebuild a map the first page already returned. The client retains the page-1 map for later pages.
    final boolean wantsFacets = request.isIncludeFacets() && isFirstPage(request);
    // Facet-count warnings (budget truncation) are merged into the response warnings so a truncated
    // filter rail is visible to the caller rather than silently incomplete.
    final List<String> facetWarnings = new ArrayList<>();
    final Map<String, List<IndexQueryFacetBucket>> facets = wantsFacets
        ? indexQueryService.facetsForResults(request.getTab(), request.getQ(), facetWarnings)
        : null;
    // Only the facet warnings are passed: toResponse already merges the section's own warnings.
    return toResponse(request, section, facetWarnings, facets);
  }

  /**
   * Per-section suppliers for a single request, identical to the fan-out the {@link Tab#ALL} packer
   * drives. On the catalog source only COMPONENT/VULNERABILITY yield rows (the catalog leg's own tab
   * guard returns empty for the rest); on the local source every section is served IQ-local.
   */
  private Function<Tab, AllTabPacker.SectionSupplier> sectionSuppliers(ResultsRequest request) {
    final boolean catalog = request.getSource() == SearchSource.CATALOG;
    return tab -> (String upstreamCursor) -> {
      ResultsRequest perSection = withTabAndCursor(request, tab, upstreamCursor);
      if (catalog) {
        return catalogClient.searchResults(perSection)
            .orElseGet(() -> SectionResult.empty(perSection.getTab()));
      }
      return iqLocalResultsService.searchNative(perSection)
          .orElseGet(() -> SectionResult.empty(perSection.getTab()));
    };
  }

  /**
   * Per-tab count badges for a single-tab response. The requested tab reuses the total already computed
   * for the page ({@code activeTotal}); the other five run a cheap count-only pass through
   * {@link AllTabPacker#countTotals}, which probes each section's first page in parallel (virtual-thread
   * pool + bounded semaphore + per-section timeout) and surfaces the section's capped {@code totalEstimate}
   * without materialising rows. A probe that times out or fails is OMITTED from the map (never reported as
   * a misleading {@code 0}), matching the ALL-tab {@code sectionTotals} semantics. {@link Tab#ALL} follows
   * the same omit rule via {@link #cappedTabCounts}: it is emitted only when every section contributed.
   */
  private Map<Tab, Long> countAllTabsForSingleTab(ResultsRequest request, long activeTotal) {
    // Sections the active source cannot serve are not probed at all. On source=catalog the catalog leg
    // answers APPLICATION/VIOLATION/WAIVER with an entitled-but-empty section, which cappedTabCounts
    // would otherwise record as a genuine 0 -- inverting the contract that a present 0 means "no hits"
    // and an absent key means "unavailable". Omitting them also spends no HDS round trip on a tab the
    // source can never answer.
    EnumSet<Tab> unprobed = EnumSet.of(request.getTab());
    unprobed.addAll(unservableTabs(request.getSource()));
    // The probe base pins pageSize to 1 so the count-only pass never materialises rows for the
    // non-active tabs: the section still reports its capped totalEstimate, but fetches at most one row.
    // page is reset to 1 and the cursor dropped so no deep-paging / stale-cursor path is exercised. The
    // sort is dropped (null -> relevance): a count needs no ordering, and the requesting tab's sort key
    // is per-tab, so carrying it would make GlobalSearchSortAllowlist.requireAllowed reject the sibling
    // probes (e.g. APPLICATION's lastEvaluationTime is invalid on COMPONENT), retiring them as FAILED
    // and silently undercounting ALL. Relevance is allowlisted on every tab.
    ResultsRequest countBase = new ResultsRequest(
        request.getQ(), request.getTab(), 1, 1, null, null, request.getSource());
    Function<Tab, AllTabPacker.SectionSupplier> suppliers = sectionSuppliers(countBase);
    Map<Tab, Long> probed = AllTabPacker.countTotals(suppliers, unprobed);
    Map<Tab, Long> raw = new EnumMap<>(probed);
    raw.put(request.getTab(), activeTotal);
    return cappedTabCounts(raw);
  }

  /**
   * Tabs the given source cannot serve rows for, and whose counts must therefore be omitted rather than
   * reported. The catalog leg serves only {@link Tab#COMPONENT} and {@link Tab#VULNERABILITY}; the
   * IQ-local index serves every section.
   */
  private static Set<Tab> unservableTabs(SearchSource source) {
    return source == SearchSource.CATALOG ? CATALOG_UNSERVABLE_TABS : Set.of();
  }

  private static Set<Tab> buildCatalogUnservableTabs() {
    EnumSet<Tab> unservable = EnumSet.noneOf(Tab.class);
    for (Tab tab : AllTabPacker.SECTION_ORDER) {
      if (tab != Tab.COMPONENT && tab != Tab.VULNERABILITY) {
        unservable.add(tab);
      }
    }
    return Collections.unmodifiableSet(unservable);
  }

  private ResultsResponse searchCatalog(ResultsRequest request) {
    if (!catalogClient.isEnabled()) {
      // Explicit source=catalog with no reachable catalog: return an empty section carrying a warning
      // rather than silently falling through to the tenant's IQ-local index. Callers who want local
      // rows must ask for them with ?source=local.
      SectionResult empty = SectionResult.empty(request.getTab(), false);
      return toResponse(request, empty, List.of(GlobalSearchResultsCatalogClientImpl.WARNING_UNAVAILABLE));
    }
    if (request.usesCursor()) {
      // Validate the incoming catalog cursor's generation token here (HTTP 410 on drift) before the
      // catalog leg decodes the same cursor to recover its offset. Both sides pin to the identical
      // backend id so the round-trip cannot diverge.
      GlobalSearchCursor.decode(request.getSearchAfter(),
          expectedTokenFor(request, GlobalSearchResultsCatalogClientImpl.BACKEND_CATALOG));
    }
    Optional<SectionResult> catalogResult = catalogClient.searchResults(request);
    if (catalogResult.isEmpty()) {
      SectionResult empty = SectionResult.empty(request.getTab(), false);
      return toResponse(request, empty, List.of("catalog source returned no data"));
    }
    return toResponse(request, catalogResult.get());
  }

  /**
   * Compute the expected per-tab cursor generation token. Pin includes the originating tab, sort key,
   * page size, the caller-selected source, and backend id so a cursor minted for one
   * tab/sort/source/backend cannot be replayed against another. The source is folded into the backend
   * id component so the underlying two-arg signature does not have to grow.
   */
  static String expectedTokenFor(ResultsRequest request, String backendId) {
    String sortKey = request.getSort() == null || request.getSort().isBlank()
        ? GlobalSearchSortAllowlist.RELEVANCE
        : request.getSort();
    String pinnedBackend = request.getSource().value() + ":" + backendId;
    return GlobalSearchCursor.computeGenerationToken(
        GlobalSearchCursor.currentGenerationToken(),
        request.getTab().name(),
        sortKey,
        request.getPageSize(),
        pinnedBackend,
        GlobalSearchTenancy.currentTenantId());
  }

  private ResultsResponse searchAll(ResultsRequest request) {
    AllTabCursor resumeCursor = decodeAllTabCursor(request);

    // Per-section inner cursors are validated by each section leg itself: the IQ-local supplier runs
    // IqLocalSearchService.search, which re-validates the raw cursor against the same preimage it
    // minted with. A stale inner cursor throws StaleCursorException, which AllTabPacker propagates
    // (rather than degrading the section) so the caller still gets HTTP 410 / retry-from-page-1.
    // The ALL tab is single-source per request. On the local source every section (applications,
    // violations, waivers AND local components/vulnerabilities from the IQ index) is served locally.
    // On the catalog source only COMPONENT/VULNERABILITY produce rows (the catalog leg's own tab guard
    // returns empty for the rest); there is no per-section cross-source mixing.
    Function<Tab, AllTabPacker.SectionSupplier> suppliers = sectionSuppliers(request);

    AllTabPacker.PackResult packed = AllTabPacker.pack(
        suppliers,
        request.getPage(),
        request.getPageSize(),
        resumeCursor,
        request.getSort(),
        request.getSource());

    String nextCursor = packed.nextCursor() == null ? null : packed.nextCursor().encode();
    return new ResultsResponse(
        Tab.ALL,
        request.getPage(),
        request.getPageSize(),
        capTotal(packed.totalEstimate()),
        tabCountsFromPack(packed),
        packed.rows(),
        nextCursor,
        packed.warnings(),
        packed.catalogAvailable());
  }

  /**
   * Assemble the per-tab count map for an {@link Tab#ALL} response from the packer's retained per-section
   * capped totals, via the shared {@link #cappedTabCounts} builder. A section absent from
   * {@code sectionTotals} (timed out / unavailable) is omitted so the frontend can render a placeholder
   * rather than a misleading {@code 0}; a section that returned zero hits is still recorded as {@code 0}.
   */
  private static Map<Tab, Long> tabCountsFromPack(AllTabPacker.PackResult packed) {
    return cappedTabCounts(packed.sectionTotals());
  }

  private ResultsResponse toResponse(ResultsRequest request, SectionResult section) {
    // Section-side warnings (from AST parser + QueryCompiler in the adapter) flow through the
    // ResultsResponse and eventually the X-Search-Warnings header.
    return toResponse(request, section, section.warnings(), null);
  }

  private ResultsResponse toResponse(ResultsRequest request, SectionResult section, List<String> warnings) {
    return toResponse(request, section, warnings, null);
  }

  private ResultsResponse toResponse(
      ResultsRequest request,
      SectionResult section,
      List<String> warnings,
      Map<String, List<IndexQueryFacetBucket>> facets)
  {
    long total = capTotal(section.totalEstimate());
    // Merge caller-supplied warnings with any warnings the section itself carries. Deduplicate
    // exact-string matches; preserve order.
    LinkedHashSet<String> merged = new LinkedHashSet<>();
    if (section.warnings() != null)
      merged.addAll(section.warnings());
    if (warnings != null)
      merged.addAll(warnings);
    // A single-tab response still carries all six count badges: the active tab reuses `total`, the
    // other five run a count-only pass. This keeps the results page a single request regardless of tab.
    // When the catalog source was degraded there is no reliable way to count the other tabs, so only the
    // active tab and the ALL total (both from `total`) are emitted rather than probing a dead source.
    // The sibling probe is first-page-only: badge counts are a property of the query, not of the page,
    // so paging deeper reuses the active tab's own total and skips the five-section fan-out. It is also
    // opt-in: the probe costs one count-only search per sibling section on top of the caller's own page
    // search, so a caller that renders a single tab's rows and no badges pays for one search, not six.
    Map<Tab, Long> tabCounts;
    if (!request.isIncludeTabCounts() || !section.catalogAvailable() || !isFirstPage(request)) {
      tabCounts = activeOnlyTabCounts(request.getTab(), total);
    }
    else {
      tabCounts = countAllTabsForSingleTab(request, total);
    }
    return new ResultsResponse(
        request.getTab(),
        request.getPage(),
        request.getPageSize(),
        total,
        tabCounts,
        section.rows(),
        section.nextSearchAfter(),
        List.copyOf(merged),
        section.catalogAvailable(),
        facets);
  }

  /**
   * Whether this request is asking for the first page of a query, i.e. the only page on which the
   * sibling count probe runs. A request carrying a cursor is resuming a previous page regardless of the
   * page number it reports, so both signals are checked.
   */
  private static boolean isFirstPage(ResultsRequest request) {
    return request.getPage() <= 1 && !request.usesCursor();
  }

  /**
   * Count map carrying only the active tab's count. Used when the caller did not opt into tab counts,
   * when the catalog source is unavailable and the other tabs cannot be probed, and on pages after the
   * first, where the sibling probe is skipped. Built through the shared {@link #cappedTabCounts} helper,
   * so the absent siblings are omitted AND {@code ALL} is omitted too (a sum that silently drops five
   * unavailable sections would be a misleading undercount).
   */
  private static Map<Tab, Long> activeOnlyTabCounts(Tab active, long total) {
    Map<Tab, Long> raw = new EnumMap<>(Tab.class);
    raw.put(active, total);
    return cappedTabCounts(raw);
  }

  /**
   * Single source of the per-tab count contract, shared by every builder
   * ({@link #countAllTabsForSingleTab}, {@link #tabCountsFromPack}, {@link #activeOnlyTabCounts}).
   *
   * <p>
   * The rule, stated once here rather than restated at each call site:
   * <ul>
   * <li>Each present per-section total is capped at 10000 ({@link #capTotal}).</li>
   * <li>A section absent from {@code rawSectionTotals} (timed out / failed / not probed) is OMITTED, never
   * reported as a misleading {@code 0}, so the frontend renders a placeholder; a section that returned
   * zero hits is present and recorded as {@code 0}.</li>
   * <li>{@link Tab#ALL} is the capped sum of the present sections, but is itself OMITTED whenever any
   * section of {@link AllTabPacker#SECTION_ORDER} was unavailable — an "All" total sitting next to
   * "unavailable" placeholders would be a silent undercount, so it follows the same omit rule as the
   * per-tab badges.</li>
   * </ul>
   *
   * @param rawSectionTotals per-section totals for the sections that produced a count; normally uncapped,
   *          but may already be capped ({@link #activeOnlyTabCounts} passes the page's own capped total)
   *          because {@link #capTotal} is idempotent. Absent sections are treated as unavailable
   */
  private static Map<Tab, Long> cappedTabCounts(Map<Tab, Long> rawSectionTotals) {
    Map<Tab, Long> counts = new EnumMap<>(Tab.class);
    long sum = 0L;
    boolean allSectionsPresent = true;
    for (Tab tab : AllTabPacker.SECTION_ORDER) {
      Long sectionTotal = rawSectionTotals.get(tab);
      if (sectionTotal == null) {
        allSectionsPresent = false;
        continue;
      }
      long capped = capTotal(sectionTotal);
      counts.put(tab, capped);
      sum += capped;
    }
    if (allSectionsPresent) {
      counts.put(Tab.ALL, capTotal(sum));
    }
    return counts;
  }

  private static long capTotal(long raw) {
    // The index client already enforces the 10k cap; we re-cap here in case the catalog leg reports a
    // different (uncapped) total.
    long cap = (long) AbstractSearchIndexClient.GLOBAL_SEARCH_TRACK_TOTAL_HITS_CAP;
    return Math.min(raw, cap);
  }

  private void validateSort(ResultsRequest request) {
    String sort = request.getSort();
    if (sort == null || sort.isBlank()) {
      return;
    }
    if (!GlobalSearchSortAllowlist.isAllowed(request.getTab(), sort)) {
      // Strip CR/LF before embedding the rejected key in the detail string: this detail is logged at INFO
      // by FilterValidationExceptionMapper, and SLF4J parameterization does not stop newline log forging.
      String safeSort = sort.replace('\n', ' ').replace('\r', ' ');
      throw new FilterValidationException(
          FilterValidationException.Code.SORT_NOT_ALLOWED,
          "Sort key '" + safeSort + "' is not allowed on tab " + request.getTab() + ". Allowed: "
              + GlobalSearchSortAllowlist.allowedFor(request.getTab()));
    }
  }

  /**
   * page+pageSize is supported only up to and including offset {@link ResultsRequest#DEEP_PAGINATION_THRESHOLD}
   * rows; anything beyond requires {@code searchAfter}. The boundary is rejected with {@code >=} (rather than
   * {@code >}) so the boundary row itself does not trigger a deep-pagination query.
   */
  private void validateDeepPaging(ResultsRequest request) {
    if (request.usesCursor()) {
      return;
    }
    if (request.offset() >= (long) ResultsRequest.DEEP_PAGINATION_THRESHOLD) {
      throw new FilterValidationException(
          FilterValidationException.Code.DEEP_PAGINATION_NOT_SUPPORTED,
          "page+pageSize is supported up to and including offset "
              + ResultsRequest.DEEP_PAGINATION_THRESHOLD
              + " rows; use searchAfter for deeper paging");
    }
  }

  private AllTabCursor decodeAllTabCursor(ResultsRequest request) {
    String encoded = request.getSearchAfter();
    if (encoded == null || encoded.isBlank()) {
      return null;
    }
    return AllTabCursor.decode(encoded, request.getSort(), request.getPageSize(), request.getSource());
  }

  /** Returns a copy of {@code base} with its tab swapped to {@code tab} and a per-section cursor injected. */
  private static ResultsRequest withTabAndCursor(ResultsRequest base, Tab tab, String searchAfter) {
    // The ALL-tab packer drives its own pagination via per-section cursors; per-section calls always start at
    // page 1 and override searchAfter with the supplier-provided cursor.
    return new ResultsRequest(
        base.getQ(),
        tab,
        1,
        base.getPageSize(),
        base.getSort(),
        searchAfter,
        base.getSource());
  }

}
