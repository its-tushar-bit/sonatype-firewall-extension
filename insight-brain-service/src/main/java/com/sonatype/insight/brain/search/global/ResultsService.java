/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.sonatype.insight.brain.search.global.catalog.GlobalSearchResultsCatalogClient;
import com.sonatype.insight.brain.search.global.catalog.GlobalSearchResultsCatalogClientImpl;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
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
 * threads + per-leg timeout) is owed as a follow-up and will wrap this service rather than replace it.
 */
@Named
@Singleton
public class ResultsService
{
  private static final Logger log = LoggerFactory.getLogger(ResultsService.class);

  private final GlobalSearchResultsIqLocalClient iqLocalResultsService;

  private final GlobalSearchResultsCatalogClient catalogClient;

  private final PerUserRateLimiter rateLimiter;

  private final CurrentUser currentUser;

  @Inject
  public ResultsService(
      final GlobalSearchResultsIqLocalClient iqLocalResultsService,
      final GlobalSearchResultsCatalogClient catalogClient,
      final CurrentUser currentUser)
  {
    this(iqLocalResultsService, catalogClient, currentUser,
        new PerUserRateLimiter(PerUserRateLimiter.DEFAULT_PERMITS_PER_USER));
  }

  /** Test-friendly constructor allowing the caller to inject a rate limiter. */
  ResultsService(
      final GlobalSearchResultsIqLocalClient iqLocalResultsService,
      final GlobalSearchResultsCatalogClient catalogClient,
      final CurrentUser currentUser,
      final PerUserRateLimiter rateLimiter)
  {
    this.iqLocalResultsService = iqLocalResultsService;
    this.catalogClient = catalogClient;
    this.currentUser = currentUser;
    this.rateLimiter = rateLimiter;
  }

  /**
   * Test-friendly constructor for callers that do not care about rate-limiting or the current user.
   * Wires a no-op-effective limiter and a dummy CurrentUser that reports {@code anonymous}.
   */
  ResultsService(
      final GlobalSearchResultsIqLocalClient iqLocalResultsService,
      final GlobalSearchResultsCatalogClient catalogClient)
  {
    this(iqLocalResultsService, catalogClient, new CurrentUser(),
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
    return toResponse(request, section);
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
    final boolean catalog = request.getSource() == SearchSource.CATALOG;
    Function<Tab, AllTabPacker.SectionSupplier> suppliers = tab -> (String upstreamCursor) -> {
      ResultsRequest perSection = withTabAndCursor(request, tab, upstreamCursor);
      if (catalog) {
        return catalogClient.searchResults(perSection)
            .orElseGet(() -> SectionResult.empty(perSection.getTab()));
      }
      return iqLocalResultsService.searchNative(perSection)
          .orElseGet(() -> SectionResult.empty(perSection.getTab()));
    };

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
        packed.rows(),
        nextCursor,
        packed.warnings(),
        packed.catalogAvailable());
  }

  private ResultsResponse toResponse(ResultsRequest request, SectionResult section) {
    // Section-side warnings (from AST parser + QueryCompiler in the adapter) flow through the
    // ResultsResponse and eventually the X-Search-Warnings header.
    return toResponse(request, section, section.warnings());
  }

  private ResultsResponse toResponse(ResultsRequest request, SectionResult section, List<String> warnings) {
    long total = capTotal(section.totalEstimate());
    // Merge caller-supplied warnings with any warnings the section itself carries. Deduplicate
    // exact-string matches; preserve order.
    LinkedHashSet<String> merged = new LinkedHashSet<>();
    if (section.warnings() != null)
      merged.addAll(section.warnings());
    if (warnings != null)
      merged.addAll(warnings);
    return new ResultsResponse(
        request.getTab(),
        request.getPage(),
        request.getPageSize(),
        total,
        section.rows(),
        section.nextSearchAfter(),
        List.copyOf(merged),
        section.catalogAvailable());
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
