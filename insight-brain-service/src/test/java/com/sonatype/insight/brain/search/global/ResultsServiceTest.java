/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import com.sonatype.insight.brain.search.global.catalog.GlobalSearchResultsCatalogClient;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResultsServiceTest
{
  private GlobalSearchResultsIqLocalClient iq;

  private GlobalSearchResultsCatalogClient catalog;

  private ResultsService service;

  @BeforeEach
  public void setUp() {
    iq = mock(GlobalSearchResultsIqLocalClient.class);
    catalog = mock(GlobalSearchResultsCatalogClient.class);
    service = new ResultsService(iq, catalog, UnusedIndexQueryServices.throwOnUse());
  }

  private static ResultsRequest request(Tab tab) {
    return new ResultsRequest("foo", tab, 1, 25, null, null);
  }

  private static ResultsRequest request(Tab tab, SearchSource source) {
    return new ResultsRequest("foo", tab, 1, 25, null, null, source);
  }

  /** Request opting into the sibling tab-count probe, which is off by default. */
  private static ResultsRequest tabCountsRequest(Tab tab) {
    return tabCountsRequest(tab, SearchSource.DEFAULT);
  }

  private static ResultsRequest tabCountsRequest(Tab tab, SearchSource source) {
    return new ResultsRequest("foo", tab, 1, 25, null, null, source, false, true);
  }

  private static ResultRow row(String type, String source, String id) {
    return ResultRow.builder().type(type).source(source).id(id).title(id).build();
  }

  private static String pinnedBackend(SearchSource source, String backendId) {
    return source.value() + ":" + backendId;
  }

  @Test
  public void defaultSource_isLocal_andDispatchesToIqNative() {
    ResultsRequest req = request(Tab.APPLICATION);
    when(iq.searchNative(req)).thenReturn(Optional.of(
        new SectionResult(Tab.APPLICATION, List.of(row("APPLICATION", SearchSource.LOCAL.value(), "app-1")), 1L, null,
            true)));

    ResultsResponse response = service.search(req);

    assertThat(response.getTab()).isEqualTo(Tab.APPLICATION);
    assertThat(response.getResults()).hasSize(1);
    assertThat(response.getResults().get(0).getSource()).isEqualTo(SearchSource.LOCAL.value());
    verify(catalog, never()).searchResults(any());
  }

  @Test
  public void sourceLocal_dispatchesToIqNative_evenForComponentTab() {
    ResultsRequest req = request(Tab.COMPONENT, SearchSource.LOCAL);
    when(iq.searchNative(req)).thenReturn(Optional.of(
        new SectionResult(Tab.COMPONENT,
            List.of(row("NON_VULNERABLE_COMPONENT", SearchSource.LOCAL.value(), "c1")), 1L, null, true)));

    ResultsResponse response = service.search(req);

    assertThat(response.getResults()).hasSize(1);
    assertThat(response.getResults().get(0).getSource()).isEqualTo(SearchSource.LOCAL.value());
    verify(catalog, never()).searchResults(any());
  }

  @Test
  public void sourceCatalog_dispatchesToCatalog_notIq() {
    ResultsRequest req = request(Tab.COMPONENT, SearchSource.CATALOG);
    when(catalog.isEnabled()).thenReturn(true);
    when(catalog.searchResults(req)).thenReturn(Optional.of(
        new SectionResult(Tab.COMPONENT, List.of(row("COMPONENT", SearchSource.CATALOG.value(), "c1")), 1L, null,
            true)));

    ResultsResponse response = service.search(req);

    assertThat(response.getResults()).hasSize(1);
    assertThat(response.getResults().get(0).getSource()).isEqualTo(SearchSource.CATALOG.value());
    verify(iq, never()).searchNative(any());
  }

  @Test
  public void sourceCatalog_catalogDisabled_returnsEmptyWithWarning_doesNotFallThroughToIq() {
    ResultsRequest req = request(Tab.COMPONENT, SearchSource.CATALOG);
    when(catalog.isEnabled()).thenReturn(false);

    ResultsResponse response = service.search(req);

    assertThat(response.getResults()).isEmpty();
    assertThat(response.getWarnings()).isNotEmpty();
    verify(iq, never()).searchNative(any());
  }

  @Test
  public void sourceCatalog_catalogReturnsEmpty_returnsEmptyWithWarning_doesNotFallThroughToIq() {
    ResultsRequest req = request(Tab.COMPONENT, SearchSource.CATALOG);
    when(catalog.isEnabled()).thenReturn(true);
    when(catalog.searchResults(req)).thenReturn(Optional.empty());

    ResultsResponse response = service.search(req);

    assertThat(response.getResults()).isEmpty();
    assertThat(response.getWarnings()).isNotEmpty();
    verify(iq, never()).searchNative(any());
  }

  @Test
  public void allTab_iteratesEverySectionInPresentationOrder() {
    when(iq.searchNative(any())).thenAnswer(inv -> {
      ResultsRequest r = inv.getArgument(0);
      return Optional.of(new SectionResult(r.getTab(),
          List.of(row(r.getTab().name(), SearchSource.LOCAL.value(), r.getTab().name() + "-iq")), 1L, null, true));
    });

    ResultsRequest req = new ResultsRequest("foo", Tab.ALL, 1, 100, null, null);
    ResultsResponse response = service.search(req);

    // Under single-source (default = local) every section is served IQ-local.
    assertThat(response.getResults()).isNotEmpty();
    assertThat(response.getResults()).allMatch(r -> SearchSource.LOCAL.value().equals(r.getSource()));
  }

  @Test
  public void allTab_surfacesSectionWarnings_onResponse() {
    when(iq.searchNative(any())).thenAnswer(inv -> {
      ResultsRequest r = inv.getArgument(0);
      List<String> warnings = r.getTab() == Tab.APPLICATION
          ? List.of("Unknown filter \"bogus\" \u2014 ignored.")
          : List.of();
      return Optional.of(new SectionResult(r.getTab(),
          List.of(row(r.getTab().name(), SearchSource.LOCAL.value(), r.getTab().name() + "-iq")), 1L, null, true,
          warnings));
    });

    ResultsRequest req = new ResultsRequest("foo", Tab.ALL, 1, 100, null, null);
    ResultsResponse response = service.search(req);

    assertThat(response.getWarnings()).contains("Unknown filter \"bogus\" \u2014 ignored.");
  }

  @Test
  public void totalEstimate_isCappedAt10000() {
    ResultsRequest req = request(Tab.APPLICATION);
    when(iq.searchNative(req))
        .thenReturn(Optional.of(new SectionResult(Tab.APPLICATION, List.of(), 50_000L, null, true)));

    ResultsResponse response = service.search(req);

    assertThat(response.getTotalEstimate()).isEqualTo(10_000L);
  }

  @Test
  public void totalEstimate_isExactBelow10000() {
    ResultsRequest req = request(Tab.APPLICATION);
    when(iq.searchNative(req))
        .thenReturn(Optional.of(new SectionResult(Tab.APPLICATION, List.of(), 9876L, null, true)));

    ResultsResponse response = service.search(req);

    assertThat(response.getTotalEstimate()).isEqualTo(9876L);
  }

  @Test
  public void allTab_response_carriesTabCountsForEverySection() {
    // Each section reports a distinct per-tab total; the ALL response must expose all six counts.
    java.util.Map<Tab, Long> totals = new java.util.EnumMap<>(Tab.class);
    totals.put(Tab.APPLICATION, 3L);
    totals.put(Tab.COMPONENT, 40L);
    totals.put(Tab.VULNERABILITY, 7L);
    totals.put(Tab.VIOLATION, 12L);
    totals.put(Tab.WAIVER, 5L);
    when(iq.searchNative(any())).thenAnswer(inv -> {
      ResultsRequest r = inv.getArgument(0);
      long total = totals.getOrDefault(r.getTab(), 0L);
      return Optional.of(new SectionResult(r.getTab(),
          List.of(row(r.getTab().name(), SearchSource.LOCAL.value(), r.getTab().name() + "-iq")), total, null, true));
    });

    ResultsResponse response = service.search(new ResultsRequest("foo", Tab.ALL, 1, 100, null, null));

    assertThat(response.getTabCounts()).containsOnlyKeys(
        Tab.ALL, Tab.APPLICATION, Tab.COMPONENT, Tab.VULNERABILITY, Tab.VIOLATION, Tab.WAIVER);
    assertThat(response.getTabCounts()).containsEntry(Tab.APPLICATION, 3L);
    assertThat(response.getTabCounts()).containsEntry(Tab.COMPONENT, 40L);
    assertThat(response.getTabCounts()).containsEntry(Tab.VULNERABILITY, 7L);
    assertThat(response.getTabCounts()).containsEntry(Tab.VIOLATION, 12L);
    assertThat(response.getTabCounts()).containsEntry(Tab.WAIVER, 5L);
    // ALL badge = sum of the per-section totals (3+40+7+12+5).
    assertThat(response.getTabCounts()).containsEntry(Tab.ALL, 67L);
  }

  @Test
  public void singleTab_response_stillCarriesAllSixCounts_viaCountPass() {
    // A single-tab (APPLICATION) request must still return counts for every tab: the active tab reuses
    // its own total, the other five come from a count-only pass over the same IQ-local supplier.
    java.util.Map<Tab, Long> totals = new java.util.EnumMap<>(Tab.class);
    totals.put(Tab.APPLICATION, 3L);
    totals.put(Tab.COMPONENT, 40L);
    totals.put(Tab.VULNERABILITY, 7L);
    totals.put(Tab.VIOLATION, 12L);
    totals.put(Tab.WAIVER, 5L);
    when(iq.searchNative(any())).thenAnswer(inv -> {
      ResultsRequest r = inv.getArgument(0);
      long total = totals.getOrDefault(r.getTab(), 0L);
      List<ResultRow> rows =
          r.getTab() == Tab.APPLICATION ? List.of(row("APPLICATION", SearchSource.LOCAL.value(), "app-1")) : List.of();
      return Optional.of(new SectionResult(r.getTab(), rows, total, null, true));
    });

    ResultsResponse response = service.search(tabCountsRequest(Tab.APPLICATION));

    assertThat(response.getTab()).isEqualTo(Tab.APPLICATION);
    assertThat(response.getTabCounts()).containsOnlyKeys(
        Tab.ALL, Tab.APPLICATION, Tab.COMPONENT, Tab.VULNERABILITY, Tab.VIOLATION, Tab.WAIVER);
    assertThat(response.getTabCounts()).containsEntry(Tab.APPLICATION, 3L);
    assertThat(response.getTabCounts()).containsEntry(Tab.COMPONENT, 40L);
    assertThat(response.getTabCounts()).containsEntry(Tab.VULNERABILITY, 7L);
    assertThat(response.getTabCounts()).containsEntry(Tab.VIOLATION, 12L);
    assertThat(response.getTabCounts()).containsEntry(Tab.WAIVER, 5L);
    assertThat(response.getTabCounts()).containsEntry(Tab.ALL, 67L);
  }

  @Test
  public void singleTab_withoutIncludeTabCounts_emitsActiveOnly_andIssuesOneSearch() {
    // The sibling probe is opt-in: absent includeTabCounts a plain single-tab first-page request must
    // issue exactly ONE search (its own page) rather than six, and emit only the active tab's badge.
    when(iq.searchNative(any())).thenAnswer(inv -> {
      ResultsRequest r = inv.getArgument(0);
      return Optional.of(new SectionResult(r.getTab(), List.of(), 42L, null, true));
    });

    ResultsResponse response = service.search(request(Tab.APPLICATION));

    assertThat(response.getTabCounts()).containsOnlyKeys(Tab.APPLICATION);
    assertThat(response.getTabCounts()).containsEntry(Tab.APPLICATION, 42L);
    verify(iq, times(1)).searchNative(any());
  }

  @Test
  public void singleTab_countProbe_omitsTimedOutTabFromTabCounts_ratherThanReporting0() {
    // The single-tab count probe fans out through AllTabPacker.countTotals (parallel first-fetch +
    // per-section timeout + bounded semaphore). Draining the packer's first-fetch permits forces every
    // PROBED (non-active) section to retire as unavailable, so those badges are OMITTED. The active tab
    // reuses its own already-computed total and is unaffected because it is never probed.
    int drained = AllTabPacker.FIRST_FETCH_SEMAPHORE.drainPermits();
    try {
      when(iq.searchNative(any())).thenAnswer(inv -> {
        ResultsRequest r = inv.getArgument(0);
        List<ResultRow> rows = r.getTab() == Tab.APPLICATION
            ? List.of(row("APPLICATION", SearchSource.LOCAL.value(), "app-1"))
            : List.of();
        return Optional.of(new SectionResult(r.getTab(), rows, 3L, null, true));
      });

      ResultsResponse response = service.search(tabCountsRequest(Tab.APPLICATION));

      // Active tab survives (reuses its own total, never probed); every probed sibling is omitted. ALL
      // is omitted too because at least one contributing section was unavailable — a sum that silently
      // dropped the five probed siblings would be a misleading undercount.
      assertThat(response.getTabCounts()).containsOnlyKeys(Tab.APPLICATION);
      assertThat(response.getTabCounts()).containsEntry(Tab.APPLICATION, 3L);
    }
    finally {
      AllTabPacker.FIRST_FETCH_SEMAPHORE.release(drained);
    }
  }

  @Test
  public void singleTab_countProbe_dropsPerTabSort_soSiblingProbesSurviveWithTabSpecificSort() {
    // The active tab carries a sort key that is valid ONLY for that tab (APPLICATION's own default,
    // lastEvaluationTime). If that per-tab sort were propagated to the sibling count probes, each sibling
    // would fail GlobalSearchSortAllowlist.requireAllowed, retire as FAILED, and be omitted — silently
    // undercounting ALL. The count probe must drop the sort (relevance is allowlisted everywhere), so all
    // six counts survive. This mock ENFORCES the allowlist so a propagated per-tab sort would throw.
    java.util.Map<Tab, Long> totals = new java.util.EnumMap<>(Tab.class);
    totals.put(Tab.APPLICATION, 3L);
    totals.put(Tab.COMPONENT, 40L);
    totals.put(Tab.VULNERABILITY, 7L);
    totals.put(Tab.VIOLATION, 12L);
    totals.put(Tab.WAIVER, 5L);
    when(iq.searchNative(any())).thenAnswer(inv -> {
      ResultsRequest r = inv.getArgument(0);
      // Reproduce the real IqLocalSearchService.search behaviour: reject a sort key not allowlisted for
      // this tab. A propagated APPLICATION-only sort (lastEvaluationTime) on COMPONENT throws here.
      GlobalSearchSortAllowlist.requireAllowed(r.getTab(), r.getSort());
      long total = totals.getOrDefault(r.getTab(), 0L);
      List<ResultRow> rows =
          r.getTab() == Tab.APPLICATION ? List.of(row("APPLICATION", SearchSource.LOCAL.value(), "app-1")) : List.of();
      return Optional.of(new SectionResult(r.getTab(), rows, total, null, true));
    });

    ResultsResponse response = service.search(new ResultsRequest(
        "foo", Tab.APPLICATION, 1, 25, "lastEvaluationTime", null, SearchSource.DEFAULT, false, true));

    // Every sibling probe survived (no sort thrown), so all six badges plus ALL are present.
    assertThat(response.getTabCounts()).containsOnlyKeys(
        Tab.ALL, Tab.APPLICATION, Tab.COMPONENT, Tab.VULNERABILITY, Tab.VIOLATION, Tab.WAIVER);
    assertThat(response.getTabCounts()).containsEntry(Tab.APPLICATION, 3L);
    assertThat(response.getTabCounts()).containsEntry(Tab.COMPONENT, 40L);
    assertThat(response.getTabCounts()).containsEntry(Tab.VULNERABILITY, 7L);
    assertThat(response.getTabCounts()).containsEntry(Tab.VIOLATION, 12L);
    assertThat(response.getTabCounts()).containsEntry(Tab.WAIVER, 5L);
    assertThat(response.getTabCounts()).containsEntry(Tab.ALL, 67L);
  }

  @Test
  public void tabCounts_areCappedAt10000_perTabAndForAll() {
    when(iq.searchNative(any())).thenAnswer(inv -> {
      ResultsRequest r = inv.getArgument(0);
      // Every section reports far above the cap; each badge must clamp to 10000, and so must ALL.
      return Optional.of(new SectionResult(r.getTab(), List.of(), 50_000L, null, true));
    });

    ResultsResponse response = service.search(tabCountsRequest(Tab.APPLICATION));

    assertThat(response.getTabCounts().get(Tab.APPLICATION)).isEqualTo(10_000L);
    assertThat(response.getTabCounts().get(Tab.COMPONENT)).isEqualTo(10_000L);
    assertThat(response.getTabCounts().get(Tab.VULNERABILITY)).isEqualTo(10_000L);
    assertThat(response.getTabCounts().get(Tab.VIOLATION)).isEqualTo(10_000L);
    assertThat(response.getTabCounts().get(Tab.WAIVER)).isEqualTo(10_000L);
    assertThat(response.getTabCounts().get(Tab.ALL)).isEqualTo(10_000L);
  }

  @Test
  public void singleTab_laterPage_skipsSiblingProbe_emitsActiveOnly() {
    // Badge counts are a property of the query, not of the page, so deep paging must not re-run the
    // five-section count fan-out on every request. Page 2 reuses the active tab's own total and probes
    // nothing else: exactly one searchNative call (the page itself).
    ResultsRequest page2 = new ResultsRequest(
        "foo", Tab.APPLICATION, 2, 25, null, null, SearchSource.DEFAULT, false, true);
    when(iq.searchNative(any()))
        .thenReturn(Optional.of(new SectionResult(Tab.APPLICATION, List.of(), 42L, null, true)));

    ResultsResponse response = service.search(page2);

    assertThat(response.getTabCounts()).containsOnlyKeys(Tab.APPLICATION);
    assertThat(response.getTabCounts()).containsEntry(Tab.APPLICATION, 42L);
    verify(iq, times(1)).searchNative(any());
  }

  @Test
  public void singleTab_firstPageWithCursor_skipsSiblingProbe_emitsActiveOnly() {
    // A cursor resume is a continuation even when it reports page=1, so it must take the same
    // skip-the-fan-out path as a later page rather than re-probing all five sections.
    ResultsRequest cursorPage = new ResultsRequest(
        "foo", Tab.APPLICATION, 1, 25, null, "cursor-token", SearchSource.DEFAULT, false, true);
    when(iq.searchNative(any()))
        .thenReturn(Optional.of(new SectionResult(Tab.APPLICATION, List.of(), 42L, null, true)));

    ResultsResponse response = service.search(cursorPage);

    assertThat(response.getTabCounts()).containsOnlyKeys(Tab.APPLICATION);
    assertThat(response.getTabCounts()).containsEntry(Tab.APPLICATION, 42L);
    verify(iq, times(1)).searchNative(any());
  }

  @Test
  public void singleTab_firstPage_stillRunsSiblingProbe() {
    // Guard is page-scoped only: page 1 with the probe requested must still fan out and populate every
    // badge.
    when(iq.searchNative(any())).thenAnswer(inv -> {
      ResultsRequest r = inv.getArgument(0);
      return Optional.of(new SectionResult(r.getTab(), List.of(), 11L, null, true));
    });

    ResultsResponse response = service.search(tabCountsRequest(Tab.APPLICATION));

    assertThat(response.getTabCounts()).containsOnlyKeys(
        Tab.ALL, Tab.APPLICATION, Tab.COMPONENT, Tab.VULNERABILITY, Tab.VIOLATION, Tab.WAIVER);
  }

  @Test
  public void singleTab_degradedSibling_isOmittedRatherThanCountedAsZero() {
    // A degraded sibling returns a successful 0 with catalogAvailable=false; it must be omitted from the
    // badges (placeholder) instead of showing a misleading 0, and ALL is omitted with it.
    when(iq.searchNative(any())).thenAnswer(inv -> {
      ResultsRequest r = inv.getArgument(0);
      if (r.getTab() == Tab.APPLICATION) {
        return Optional.of(new SectionResult(Tab.APPLICATION, List.of(), 9L, null, true));
      }
      return Optional.of(new SectionResult(r.getTab(), List.of(), 0L, null, false));
    });

    ResultsResponse response = service.search(tabCountsRequest(Tab.APPLICATION));

    assertThat(response.getTabCounts()).containsOnlyKeys(Tab.APPLICATION);
    assertThat(response.getTabCounts()).containsEntry(Tab.APPLICATION, 9L);
  }

  @Test
  public void singleTab_catalogUnavailable_emitsActiveOnly_withoutProbingOtherTabs() {
    // Catalog disabled: the response must not probe the other tabs (the source is dead). Only the active
    // tab is emitted so the frontend can render placeholders for the rest; ALL is omitted too because
    // the sibling sections were unavailable (an "All" total next to unavailable placeholders would be a
    // silent undercount).
    ResultsRequest req = tabCountsRequest(Tab.COMPONENT, SearchSource.CATALOG);
    when(catalog.isEnabled()).thenReturn(false);

    ResultsResponse response = service.search(req);

    assertThat(response.getTabCounts()).containsOnlyKeys(Tab.COMPONENT);
    verify(iq, never()).searchNative(any());
  }

  @Test
  public void allTab_omitsUnavailableSectionFromTabCounts_ratherThanReporting0() {
    // Drain the packer's first-fetch permits so every section retires as unavailable. The ALL response
    // must OMIT those tabs from tabCounts (so the frontend renders a placeholder), not report a
    // misleading 0 — matching the degraded single-tab fallback semantics.
    int drained = AllTabPacker.FIRST_FETCH_SEMAPHORE.drainPermits();
    try {
      when(iq.searchNative(any())).thenAnswer(inv -> {
        ResultsRequest r = inv.getArgument(0);
        return Optional.of(new SectionResult(r.getTab(),
            List.of(row(r.getTab().name(), SearchSource.LOCAL.value(), r.getTab().name() + "-iq")), 5L, null, true));
      });

      ResultsResponse response = service.search(new ResultsRequest("foo", Tab.ALL, 1, 25, null, null));

      // Every per-section badge is omitted because each section was unavailable, and ALL is omitted too
      // (it would be a sum of nothing) — so the map is empty rather than carrying a misleading total.
      assertThat(response.getTabCounts()).isEmpty();
    }
    finally {
      AllTabPacker.FIRST_FETCH_SEMAPHORE.release(drained);
    }
  }

  @Test
  public void sourceCatalog_catalogReturnsEmpty_emitsActiveOnly_withoutProbingOtherTabs() {
    // catalog.isEnabled()==true but searchResults() returns Optional.empty(): the response degrades
    // (catalogAvailable=false via SectionResult.empty(tab,false)), so tabCounts must NOT probe the other
    // tabs (the source produced no usable data). Only the active tab is emitted (ALL omitted, mirroring
    // the omit-on-unavailable rule), matching the disabled-catalog fallback.
    ResultsRequest req = tabCountsRequest(Tab.COMPONENT, SearchSource.CATALOG);
    when(catalog.isEnabled()).thenReturn(true);
    when(catalog.searchResults(req)).thenReturn(Optional.empty());

    ResultsResponse response = service.search(req);

    assertThat(response.getTabCounts()).containsOnlyKeys(Tab.COMPONENT);
    assertThat(response.getTabCounts()).containsEntry(Tab.COMPONENT, 0L);
    assertThat(response.isCatalogAvailable()).isFalse();
    verify(iq, never()).searchNative(any());
  }

  @Test
  public void singleTab_catalogAvailable_countsSiblingCatalogTab_viaCountPass() {
    // A single-tab COMPONENT request on the catalog source counts the sibling catalog tab
    // (VULNERABILITY) via a count-only pass through the catalog supplier -- not the IQ-local one.
    ResultsRequest req = tabCountsRequest(Tab.COMPONENT, SearchSource.CATALOG);
    when(catalog.isEnabled()).thenReturn(true);
    when(catalog.searchResults(any())).thenAnswer(inv -> {
      ResultsRequest r = inv.getArgument(0);
      long total = switch (r.getTab()) {
        case COMPONENT -> 8L;
        case VULNERABILITY -> 30L;
        default -> 0L;
      };
      List<ResultRow> rows = r.getTab() == Tab.COMPONENT
          ? List.of(row("COMPONENT", SearchSource.CATALOG.value(), "c1"))
          : List.of();
      return Optional.of(new SectionResult(r.getTab(), rows, total, null, true));
    });

    ResultsResponse response = service.search(req);

    assertThat(response.getTab()).isEqualTo(Tab.COMPONENT);
    assertThat(response.getTabCounts()).containsEntry(Tab.COMPONENT, 8L);
    assertThat(response.getTabCounts()).containsEntry(Tab.VULNERABILITY, 30L);
    verify(iq, never()).searchNative(any());
  }

  @Test
  public void catalogSource_omitsTabsTheCatalogCannotServe_ratherThanReportingZero() {
    // The catalog leg answers APPLICATION/VIOLATION/WAIVER with an entitled-but-empty section, so
    // counting them would record a genuine 0 -- inverting the contract that an absent key means
    // "unavailable" and a present 0 means "no hits". Those tabs must be ABSENT, and ALL must be omitted
    // with them. Only the two catalog-backed tabs carry a badge, and no probe is spent on the rest.
    ResultsRequest req = tabCountsRequest(Tab.COMPONENT, SearchSource.CATALOG);
    when(catalog.isEnabled()).thenReturn(true);
    when(catalog.searchResults(any())).thenAnswer(inv -> {
      ResultsRequest r = inv.getArgument(0);
      if (r.getTab() != Tab.COMPONENT && r.getTab() != Tab.VULNERABILITY) {
        // Mirrors the real catalog leg's tab guard: entitled, but no rows and a 0 it cannot vouch for.
        return Optional.of(SectionResult.empty(r.getTab(), true));
      }
      long total = r.getTab() == Tab.COMPONENT ? 8L : 30L;
      return Optional.of(new SectionResult(r.getTab(), List.of(), total, null, true));
    });

    ResultsResponse response = service.search(req);

    assertThat(response.getTabCounts()).containsOnlyKeys(Tab.COMPONENT, Tab.VULNERABILITY);
    assertThat(response.getTabCounts()).doesNotContainKeys(
        Tab.APPLICATION, Tab.VIOLATION, Tab.WAIVER, Tab.ALL);
    assertThat(response.getTabCounts()).containsEntry(Tab.COMPONENT, 8L);
    assertThat(response.getTabCounts()).containsEntry(Tab.VULNERABILITY, 30L);
  }

  @Test
  public void unknownSortKey_throws400() {
    ResultsRequest req = new ResultsRequest("foo", Tab.APPLICATION, 1, 25, "nope", null);

    assertThatThrownBy(() -> service.search(req))
        .isInstanceOf(FilterValidationException.class)
        .extracting(e -> ((FilterValidationException) e).getCode())
        .isEqualTo(FilterValidationException.Code.SORT_NOT_ALLOWED);
  }

  @Test
  public void unknownSortKeyWithNewline_detailIsSingleLine() {
    // The rejected sort key is embedded in the (logged) detail string; CR/LF must be stripped so a
    // hostile sort value cannot forge extra log lines.
    ResultsRequest req = new ResultsRequest("foo", Tab.APPLICATION, 1, 25, "nope\nINFO forged log line", null);

    assertThatThrownBy(() -> service.search(req))
        .isInstanceOf(FilterValidationException.class)
        .extracting(e -> ((FilterValidationException) e).getDetail())
        .satisfies(detail -> {
          String d = (String) detail;
          assertThat(d).doesNotContain("\n").doesNotContain("\r");
          assertThat(d.lines().count()).isEqualTo(1L);
        });
  }

  @Test
  public void allowedSortKey_passes() {
    ResultsRequest req = new ResultsRequest("foo", Tab.APPLICATION, 1, 25, "name", null);
    when(iq.searchNative(req)).thenReturn(Optional.of(SectionResult.empty(Tab.APPLICATION)));

    ResultsResponse response = service.search(req);

    assertThat(response).isNotNull();
    assertThat(response.getTab()).isEqualTo(Tab.APPLICATION);
    verify(iq).searchNative(req);
  }

  @Test
  public void deepPagingWithoutCursor_throws400() {
    ResultsRequest req = new ResultsRequest("foo", Tab.APPLICATION, 100, 100, null, null);

    assertThatThrownBy(() -> service.search(req))
        .isInstanceOf(FilterValidationException.class)
        .extracting(e -> ((FilterValidationException) e).getCode())
        .isEqualTo(FilterValidationException.Code.DEEP_PAGINATION_NOT_SUPPORTED);
  }

  @Test
  public void integerMaxValuePage_doesNotOverflowAndIsRejected() {
    ResultsRequest req = new ResultsRequest("foo", Tab.APPLICATION, Integer.MAX_VALUE, 25, null, null);

    assertThatThrownBy(() -> service.search(req))
        .isInstanceOf(FilterValidationException.class)
        .extracting(e -> ((FilterValidationException) e).getCode())
        .isEqualTo(FilterValidationException.Code.DEEP_PAGINATION_NOT_SUPPORTED);
  }

  @Test
  public void exactlyThresholdPage_isRejected() {
    // offset = (page-1)*pageSize = (41-1)*25 = 1000 == DEEP_PAGINATION_THRESHOLD; gate is >=, so rejected.
    ResultsRequest req = new ResultsRequest("foo", Tab.APPLICATION, 41, 25, null, null);

    assertThatThrownBy(() -> service.search(req))
        .isInstanceOf(FilterValidationException.class)
        .extracting(e -> ((FilterValidationException) e).getCode())
        .isEqualTo(FilterValidationException.Code.DEEP_PAGINATION_NOT_SUPPORTED);
  }

  @Test
  public void deepPagingWithCursor_passes() {
    // Cursor generation-token pin bakes in tab, sort, pageSize, and source-scoped backendId.
    String token = GlobalSearchCursor.computeGenerationToken(
        GlobalSearchCursor.currentGenerationToken(),
        Tab.APPLICATION.name(),
        GlobalSearchSortAllowlist.RELEVANCE,
        100,
        pinnedBackend(SearchSource.LOCAL, "iq-local"),
        GlobalSearchTenancy.currentTenantId());
    String cursor = new GlobalSearchCursor(token, List.of("x")).encode();
    ResultsRequest req = new ResultsRequest("foo", Tab.APPLICATION, 100, 100, null, cursor);
    when(iq.searchNative(req)).thenReturn(Optional.of(SectionResult.empty(Tab.APPLICATION)));

    ResultsResponse response = service.search(req);
    assertThat(response).isNotNull();
    assertThat(response.getTab()).isEqualTo(Tab.APPLICATION);
    verify(iq).searchNative(req);
  }

  @Test
  public void perTabLocalCursor_isDelegatedToIqLeg_forValidation() {
    // The IQ-local leg (IqLocalSearchService.search inside searchNative) owns local-cursor
    // validation and mints via the same preimage, so the dispatcher must NOT re-validate a local
    // cursor with a separately computed token (that divergence 410'd every legitimate page 2). Here
    // the mock IQ leg accepts any cursor; the assertion is that the dispatcher passes the request
    // (cursor and all) straight through to searchNative rather than rejecting it up front.
    String cursor = new GlobalSearchCursor(GlobalSearchCursor.currentGenerationToken(), List.of("x")).encode();
    ResultsRequest req = new ResultsRequest("foo", Tab.APPLICATION, 100, 25, null, cursor);
    when(iq.searchNative(req)).thenReturn(Optional.of(SectionResult.empty(Tab.APPLICATION)));

    ResultsResponse response = service.search(req);

    assertThat(response.getTab()).isEqualTo(Tab.APPLICATION);
    verify(iq).searchNative(req);
  }

  @Test
  public void perTabCursorMintedForLocalCannotBeReplayedAgainstCatalog() {
    // A cursor minted for source=local should not decode when the caller flips to source=catalog. The
    // pin bakes the source into the backend id, so the expected token changes across sources.
    String localToken = GlobalSearchCursor.computeGenerationToken(
        GlobalSearchCursor.currentGenerationToken(),
        Tab.COMPONENT.name(),
        GlobalSearchSortAllowlist.RELEVANCE,
        25,
        pinnedBackend(SearchSource.LOCAL, "iq-local"),
        "");
    String localCursor = new GlobalSearchCursor(localToken, List.of("x")).encode();
    when(catalog.isEnabled()).thenReturn(true);
    ResultsRequest replayed = new ResultsRequest("foo", Tab.COMPONENT, 1, 25, null, localCursor, SearchSource.CATALOG);

    assertThatThrownBy(() -> service.search(replayed))
        .isInstanceOf(StaleCursorException.class);
  }

  @Test
  public void staleAllTabCursor_throws410ViaStaleCursorException() {
    // Build a cursor at one generation, bump generation, then decode -> should throw StaleCursorException.
    String original = GlobalSearchCursor.currentGenerationToken();
    try {
      java.util.Map<Tab, AllTabCursor.SectionCursor> cursors = new java.util.EnumMap<>(Tab.class);
      cursors.put(Tab.COMPONENT, AllTabCursor.SectionCursor.nonExhausted(null, 3));
      String encoded = new AllTabCursor(null, 25, cursors).encode();

      GlobalSearchCursor.bumpGenerationToken("g-bumped");
      ResultsRequest req = new ResultsRequest("foo", Tab.ALL, 1, 25, null, encoded);

      assertThatThrownBy(() -> service.search(req))
          .isInstanceOf(StaleCursorException.class)
          .hasMessageContaining("retry from page 1");
    }
    finally {
      GlobalSearchCursor.bumpGenerationToken(original);
    }
  }

  @Test
  public void allTabInnerCursorReplayedUnderWrongSource_isRejectedByOuterPin() {
    // The outer AllTabCursor pin folds the source into its backend id, so an ALL-tab cursor whose
    // outer pin was minted for source=catalog cannot be replayed under source=local (and vice-versa):
    // AllTabCursor.decode fails the outer-pin check before any section runs. Per-section inner-cursor
    // validation is delegated to each section leg (the IQ leg mints/validates through the same
    // IqLocalSearchService preimage), so the dispatcher no longer recomputes a divergent inner token.
    java.util.Map<Tab, AllTabCursor.SectionCursor> cursors = new java.util.EnumMap<>(Tab.class);
    cursors.put(Tab.COMPONENT, AllTabCursor.SectionCursor.nonExhausted("inner", 0));

    when(catalog.isEnabled()).thenReturn(true);
    when(catalog.searchResults(any())).thenReturn(Optional.of(SectionResult.empty(Tab.COMPONENT)));
    when(iq.searchNative(any())).thenReturn(Optional.of(SectionResult.empty(Tab.APPLICATION)));

    // Cursor minted (outer pin) for source=catalog decodes cleanly under source=catalog.
    String catalogEncoded = new AllTabCursor(null, 25, SearchSource.CATALOG, cursors).encode();
    ResultsRequest catalogReplay =
        new ResultsRequest("foo", Tab.ALL, 1, 25, null, catalogEncoded, SearchSource.CATALOG);
    service.search(catalogReplay);

    // Replay the SAME encoded cursor under source=local: the outer pin no longer matches -> 410.
    ResultsRequest crossSourceReplay =
        new ResultsRequest("foo", Tab.ALL, 1, 25, null, catalogEncoded, SearchSource.LOCAL);
    assertThatThrownBy(() -> service.search(crossSourceReplay))
        .isInstanceOf(StaleCursorException.class);
  }

  @Test
  public void sourceCatalog_page1Cursor_roundTripsThroughDispatcherDecode() {
    // A cursor the catalog leg mints must round-trip through the dispatcher's decode/validate on page 2
    // (no 410). The catalog client owns the actual offset paging; here we assert the dispatcher accepts
    // and forwards the minted cursor rather than rejecting it as stale.
    ResultsRequest page1 = request(Tab.COMPONENT, SearchSource.CATALOG);
    when(catalog.isEnabled()).thenReturn(true);
    // Page 1: catalog reports more rows exist, so it mints a real next cursor pinned to the catalog
    // backend token the dispatcher validates against.
    String mintedCursor = GlobalSearchCursor.newCursor(
        ResultsService.expectedTokenFor(page1, "catalog"), List.of("25")).encode();
    when(catalog.searchResults(page1)).thenReturn(Optional.of(new SectionResult(
        Tab.COMPONENT, List.of(row("COMPONENT", SearchSource.CATALOG.value(), "c1")), 100L, mintedCursor, true)));

    ResultsResponse r1 = service.search(page1);
    assertThat(r1.getNextSearchAfter()).isEqualTo(mintedCursor);

    // Page 2: submit the minted cursor. The dispatcher decodes+validates it (no StaleCursorException).
    ResultsRequest page2 = new ResultsRequest("foo", Tab.COMPONENT, 1, 25, null, mintedCursor, SearchSource.CATALOG);
    when(catalog.searchResults(page2)).thenReturn(Optional.of(new SectionResult(
        Tab.COMPONENT, List.of(row("COMPONENT", SearchSource.CATALOG.value(), "c2")), 100L, null, true)));

    ResultsResponse r2 = service.search(page2);
    assertThat(r2.getResults()).hasSize(1);
    assertThat(r2.getResults().get(0).getId()).isEqualTo("c2");
    assertThat(r2.getNextSearchAfter()).isNull();
  }

  @Test
  public void sourceCatalog_disabled_reportsCatalogUnavailable() {
    ResultsRequest req = request(Tab.COMPONENT, SearchSource.CATALOG);
    when(catalog.isEnabled()).thenReturn(false);

    ResultsResponse response = service.search(req);

    assertThat(response.isCatalogAvailable()).isFalse();
  }
}
