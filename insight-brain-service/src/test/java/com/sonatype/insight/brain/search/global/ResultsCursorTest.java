/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import com.sonatype.insight.brain.search.global.catalog.GlobalSearchResultsCatalogClient;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end cursor behaviour: fresh cursor preserves ordering across two consecutive page fetches; a
 * simulated generation bump invalidates a previously-issued cursor with {@link StaleCursorException}.
 */
public class ResultsCursorTest
{
  private String originalToken;

  @Before
  public void saveToken() {
    originalToken = GlobalSearchCursor.currentGenerationToken();
  }

  @After
  public void restoreToken() {
    GlobalSearchCursor.bumpGenerationToken(originalToken);
  }

  private static ResultRow row(Tab tab, int idx) {
    return ResultRow.builder()
        .type(tab.name())
        .source(SearchSource.LOCAL.value())
        .id(tab.name() + "-" + idx)
        .title(tab.name() + " row " + idx)
        .build();
  }

  private static AllTabPacker.SectionSupplier supplier(Tab tab, int rowCount) {
    return cursor -> {
      List<ResultRow> rows = new ArrayList<>();
      for (int i = 0; i < rowCount; i++) {
        rows.add(row(tab, i));
      }
      return new SectionResult(tab, rows, rowCount, null, true);
    };
  }

  private static Function<Tab, AllTabPacker.SectionSupplier> suppliersFor(Map<Tab, AllTabPacker.SectionSupplier> map) {
    return t -> map.getOrDefault(t, c -> SectionResult.empty(t));
  }

  @Test
  public void freshCursor_preservesOrderingAcrossTwoPageFetches() {
    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    map.put(Tab.APPLICATION, supplier(Tab.APPLICATION, 7));
    Function<Tab, AllTabPacker.SectionSupplier> suppliers = suppliersFor(map);

    AllTabPacker.PackResult page1 = AllTabPacker.pack(suppliers, 1, 3, null);
    assertThat(page1.rows()).extracting(ResultRow::getId)
        .containsExactly("APPLICATION-0", "APPLICATION-1", "APPLICATION-2");
    assertThat(page1.nextCursor()).isNotNull();

    AllTabPacker.PackResult page2 = AllTabPacker.pack(suppliers, 2, 3, page1.nextCursor());
    assertThat(page2.rows()).extracting(ResultRow::getId)
        .containsExactly("APPLICATION-3", "APPLICATION-4", "APPLICATION-5");
  }

  private static final String SORT = null;

  private static final int PAGE_SIZE = 25;

  @Test
  public void cursorInvalidatesAfterGenerationTokenBump() {
    Map<Tab, AllTabCursor.SectionCursor> cursors = new EnumMap<>(Tab.class);
    cursors.put(Tab.APPLICATION, AllTabCursor.SectionCursor.nonExhausted(null, 3));
    String encoded = new AllTabCursor(SORT, PAGE_SIZE, cursors).encode();

    GlobalSearchCursor.bumpGenerationToken("g-bumped-after-reindex");

    assertThatThrownBy(() -> AllTabCursor.decode(encoded, SORT, PAGE_SIZE))
        .isInstanceOf(StaleCursorException.class)
        .hasMessageContaining("retry from page 1");
  }

  @Test
  public void decode_differentSort_throwsStale() {
    Map<Tab, AllTabCursor.SectionCursor> cursors = new EnumMap<>(Tab.class);
    cursors.put(Tab.APPLICATION, AllTabCursor.SectionCursor.nonExhausted(null, 1));
    String encoded = new AllTabCursor("relevance", PAGE_SIZE, cursors).encode();

    assertThatThrownBy(() -> AllTabCursor.decode(encoded, "name", PAGE_SIZE))
        .isInstanceOf(StaleCursorException.class);
  }

  @Test
  public void decode_differentPageSize_throwsStale() {
    Map<Tab, AllTabCursor.SectionCursor> cursors = new EnumMap<>(Tab.class);
    cursors.put(Tab.APPLICATION, AllTabCursor.SectionCursor.nonExhausted(null, 1));
    String encoded = new AllTabCursor(SORT, 25, cursors).encode();

    assertThatThrownBy(() -> AllTabCursor.decode(encoded, SORT, 50))
        .isInstanceOf(StaleCursorException.class);
  }

  @Test
  public void decode_sameSortAndPageSize_roundTrips() {
    Map<Tab, AllTabCursor.SectionCursor> cursors = new EnumMap<>(Tab.class);
    cursors.put(Tab.APPLICATION, AllTabCursor.SectionCursor.nonExhausted("inner", 2));
    String encoded = new AllTabCursor("name", 50, cursors).encode();

    AllTabCursor decoded = AllTabCursor.decode(encoded, "name", 50);
    assertThat(decoded.cursorFor(Tab.APPLICATION).upstreamCursor()).isEqualTo("inner");
    assertThat(decoded.cursorFor(Tab.APPLICATION).skipWithinPage()).isEqualTo(2);
  }

  @Test
  public void decode_corruptedBase64_throwsStale() {
    assertThatThrownBy(() -> AllTabCursor.decode("!!!not-base64!!!", SORT, PAGE_SIZE))
        .isInstanceOf(StaleCursorException.class);
  }

  @Test
  public void decode_missingMagic_throwsStale() {
    String raw = "nope:" + AllTabCursor.computePin(SORT, PAGE_SIZE);
    String encoded = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    assertThatThrownBy(() -> AllTabCursor.decode(encoded, SORT, PAGE_SIZE))
        .isInstanceOf(StaleCursorException.class);
  }

  @Test
  public void decode_malformedSegment_throwsStale() {
    // Segment without the FIELD_SEP '='.
    String raw = "alltab:" + AllTabCursor.computePin(SORT, PAGE_SIZE) + "|notAFieldSep";
    String encoded = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    assertThatThrownBy(() -> AllTabCursor.decode(encoded, SORT, PAGE_SIZE))
        .isInstanceOf(StaleCursorException.class);
  }

  @Test
  public void decode_unknownTabName_throwsStale() {
    String raw = "alltab:" + AllTabCursor.computePin(SORT, PAGE_SIZE) + "|NOT_A_TAB=,0,0";
    String encoded = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    assertThatThrownBy(() -> AllTabCursor.decode(encoded, SORT, PAGE_SIZE))
        .isInstanceOf(StaleCursorException.class);
  }

  @Test
  public void decode_nonNumericSkip_throwsStale() {
    String raw = "alltab:" + AllTabCursor.computePin(SORT, PAGE_SIZE) + "|APPLICATION=,notANumber,0";
    String encoded = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    assertThatThrownBy(() -> AllTabCursor.decode(encoded, SORT, PAGE_SIZE))
        .isInstanceOf(StaleCursorException.class);
  }

  @Test
  public void decode_oversizeInput_throwsStale() {
    String oversized = "a".repeat(AllTabCursor.MAX_ENCODED_LENGTH + 1);
    assertThatThrownBy(() -> AllTabCursor.decode(oversized, SORT, PAGE_SIZE))
        .isInstanceOf(StaleCursorException.class);
  }

  @Test
  public void decode_innerCursorTooLarge_throwsStale() {
    // Build an inner cursor whose decoded length is 1200 bytes; the cap is 1024.
    String bigInner = "x".repeat(1200);
    Map<Tab, AllTabCursor.SectionCursor> cursors = new EnumMap<>(Tab.class);
    cursors.put(Tab.APPLICATION, AllTabCursor.SectionCursor.nonExhausted(bigInner, 0));
    String encoded = new AllTabCursor(SORT, PAGE_SIZE, cursors).encode();

    assertThatThrownBy(() -> AllTabCursor.decode(encoded, SORT, PAGE_SIZE))
        .isInstanceOf(StaleCursorException.class);
  }

  @Test
  public void decode_separatorInInnerCursor_roundTrips() {
    // Per-section inner cursor contains '|', '=', ',' — the base64-wrapped inner payload must keep the
    // outer separators unambiguous so decode reconstructs the original string.
    String innerWithSeparators = "gen=abc;t=a|b,c=d|e";
    Map<Tab, AllTabCursor.SectionCursor> cursors = new EnumMap<>(Tab.class);
    cursors.put(Tab.APPLICATION, AllTabCursor.SectionCursor.nonExhausted(innerWithSeparators, 2));
    String encoded = new AllTabCursor(SORT, PAGE_SIZE, cursors).encode();

    AllTabCursor decoded = AllTabCursor.decode(encoded, SORT, PAGE_SIZE);
    assertThat(decoded.cursorFor(Tab.APPLICATION).upstreamCursor()).isEqualTo(innerWithSeparators);
    assertThat(decoded.cursorFor(Tab.APPLICATION).skipWithinPage()).isEqualTo(2);
  }

  @Test
  public void localSectionNextCursor_roundTripsThroughResultsService_noStale410() {
    // Regression for the page-2-always-410 bug: the /results local leg used to mint its next cursor
    // with a hand-rolled preimage (ResultsService.expectedTokenFor) that diverged from what
    // IqLocalSearchService validates on the follow-up request, so every page 2 threw
    // StaleCursorException (HTTP 410). This test mints page 1's next cursor through the real
    // GlobalSearchResultsIqLocalClientImpl -> IqLocalSearchService.mintNextCursor path, then feeds it
    // back as searchAfter and asserts the follow-up search does NOT 410 and returns the next rows
    // with zero overlap with page 1.
    SearchIndexClient index = mock(SearchIndexClient.class);
    when(index.isGlobalSearchEnabled()).thenReturn(true);
    when(index.getCurrentUserContextIdsWithReadPermission()).thenReturn(java.util.Set.of("org-1"));
    when(index.buildAllowedContextIdsFilter(any())).thenReturn(null);
    when(index.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    when(index.buildPermittedQuery(any())).thenCallRealMethod();
    when(index.getLastIndexTime()).thenReturn(12345L);
    when(index.backendId()).thenReturn("lucene");

    // Page 1 returns one row plus a non-empty nextSearchAfter (so a next cursor is minted); page 2
    // (searchAfter present) returns the next row with no continuation.
    when(index.searchGlobal(any())).thenAnswer(inv -> {
      GlobalSearchRequest req = inv.getArgument(0);
      if (req.searchAfter() == null || req.searchAfter().isEmpty()) {
        return new GlobalSearchResult(List.of(appRow("app-1")), 2L, List.of("app-1"), true, "lucene");
      }
      return new GlobalSearchResult(List.of(appRow("app-2")), 2L, List.of(), true, "lucene");
    });

    IqLocalSearchService iqService = new IqLocalSearchService(index,
        com.sonatype.insight.brain.search.global.fieldmap.FieldMap.defaultMap());
    GlobalSearchResultsIqLocalClientImpl iqClient = new GlobalSearchResultsIqLocalClientImpl(iqService);
    ResultsService service = new ResultsService(iqClient, mock(GlobalSearchResultsCatalogClient.class));

    ResultsRequest page1Req = new ResultsRequest("acme", Tab.APPLICATION, 1, 25, null, null);
    ResultsResponse page1 = service.search(page1Req);
    assertThat(page1.getResults()).extracting(ResultRow::getId).containsExactly("app-1");
    assertThat(page1.getNextSearchAfter()).isNotBlank();

    ResultsRequest page2Req =
        new ResultsRequest("acme", Tab.APPLICATION, 1, 25, null, page1.getNextSearchAfter());
    ResultsResponse page2 = assertThatNoStale(() -> service.search(page2Req));
    assertThat(page2.getResults()).extracting(ResultRow::getId).containsExactly("app-2");
    assertThat(page2.getResults()).extracting(ResultRow::getId)
        .doesNotContainAnyElementsOf(page1.getResults().stream().map(ResultRow::getId).toList());
  }

  private static ResultsResponse assertThatNoStale(java.util.concurrent.Callable<ResultsResponse> call) {
    try {
      return call.call();
    }
    catch (StaleCursorException e) {
      throw new AssertionError("follow-up search 410'd on a freshly minted local cursor", e);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static SearchResultItemDTO appRow(String publicId) {
    SearchResultItemDTO dto = new SearchResultItemDTO();
    dto.applicationId = publicId;
    dto.applicationName = publicId;
    dto.applicationPublicId = publicId;
    return dto;
  }

  @Test
  public void serviceTranslatesStaleAllTabCursorToStaleCursorException() {
    GlobalSearchResultsIqLocalClient iq = mock(GlobalSearchResultsIqLocalClient.class);
    GlobalSearchResultsCatalogClient catalog = mock(GlobalSearchResultsCatalogClient.class);
    when(iq.searchNative(any())).thenReturn(Optional.of(SectionResult.empty(Tab.APPLICATION)));
    when(catalog.isEnabled()).thenReturn(false);
    when(catalog.searchResults(any())).thenReturn(Optional.empty());
    ResultsService service = new ResultsService(iq, catalog);

    Map<Tab, AllTabCursor.SectionCursor> cursors = new EnumMap<>(Tab.class);
    cursors.put(Tab.APPLICATION, AllTabCursor.SectionCursor.nonExhausted(null, 1));
    String encoded = new AllTabCursor(SORT, PAGE_SIZE, cursors).encode();

    GlobalSearchCursor.bumpGenerationToken("g-rotated");

    ResultsRequest req = new ResultsRequest("q", Tab.ALL, 1, 25, null, encoded);

    assertThatThrownBy(() -> service.search(req))
        .isInstanceOf(StaleCursorException.class)
        .hasMessageContaining("retry from page 1");
  }
}
