/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import com.sonatype.insight.brain.search.global.catalog.GlobalSearchResultsCatalogClient;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResultsServiceTest
{
  private GlobalSearchResultsIqLocalClient iq;

  private GlobalSearchResultsCatalogClient catalog;

  private ResultsService service;

  @Before
  public void setUp() {
    iq = mock(GlobalSearchResultsIqLocalClient.class);
    catalog = mock(GlobalSearchResultsCatalogClient.class);
    service = new ResultsService(iq, catalog);
  }

  private static ResultsRequest request(Tab tab) {
    return new ResultsRequest("foo", tab, 1, 25, null, null);
  }

  private static ResultsRequest request(Tab tab, SearchSource source) {
    return new ResultsRequest("foo", tab, 1, 25, null, null, source);
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
