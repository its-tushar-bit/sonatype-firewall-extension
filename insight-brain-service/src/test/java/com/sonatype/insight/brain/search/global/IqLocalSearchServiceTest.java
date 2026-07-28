/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.search.global.IqLocalSearchService.IqLocalSearchResponse;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.SearchInputs;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TermQuery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit test for {@link IqLocalSearchService}. Mocks the {@link SearchIndexClient} entirely;
 * exercise covers query composition, per-type capping, source tagging, and total-hit capping.
 */
@RunWith(MockitoJUnitRunner.class)
public class IqLocalSearchServiceTest
{
  @Mock
  private SearchIndexClient searchIndexClient;

  private IqLocalSearchService service;

  @Before
  public void setUp() {
    service = new IqLocalSearchService(searchIndexClient,
        com.sonatype.insight.brain.search.global.fieldmap.FieldMap.defaultMap());

    when(searchIndexClient.isGlobalSearchEnabled()).thenReturn(true);
    // Default permission wiring: open access (null permission filter → base query passed through).
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of());
    when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenReturn(null);
    // Match the production wrapWithPermissionFilter null-handling: pass baseQuery through.
    when(searchIndexClient.wrapWithPermissionFilter(any(), any()))
        .thenAnswer(inv -> inv.getArgument(0));
    // Let the default buildPermittedQuery run through the stubbed pieces above.
    when(searchIndexClient.buildPermittedQuery(any())).thenCallRealMethod();
  }

  @Test
  public void search_callsSearchGlobal_withBoolQueryBuiltByPlainTextBuilder() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());

    SearchInputs inputs = new SearchInputs(
        "log4j",
        Tab.APPLICATION,
        Set.of(ItemType.APPLICATION),
        25,
        "relevance",
        null);

    service.search(inputs);

    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient, times(1)).searchGlobal(captor.capture());
    Query sent = captor.getValue().baseQuery();
    assertThat(sent).isInstanceOf(BooleanQuery.class);
    // Confirm every node in the sent query is a builder-constructed Lucene primitive (no
    // QueryParser-derived class smuggled through).
    assertOnlyBuilderTypes(sent);
  }

  @Test
  public void search_passesPageSizeToIndexClient_clampedToMax() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());

    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION, Set.of(ItemType.APPLICATION),
        9999, "relevance", null);

    service.search(inputs);

    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient).searchGlobal(captor.capture());
    assertThat(captor.getValue().pageSize()).isEqualTo(IqLocalSearchService.MAX_PAGE_SIZE);
  }

  @Test
  public void search_negativeOrZeroPageSize_fallsBackToDefault() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());

    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION, Set.of(ItemType.APPLICATION),
        0, "relevance", null);
    service.search(inputs);

    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient).searchGlobal(captor.capture());
    assertThat(captor.getValue().pageSize()).isEqualTo(IqLocalSearchService.DEFAULT_PER_TYPE_PAGE_SIZE);
  }

  @Test
  public void search_multipleItemTypes_emitsShouldClausePerType() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());

    SearchInputs inputs = new SearchInputs(
        "log4j",
        Tab.APPLICATION,
        Set.of(ItemType.APPLICATION, ItemType.ORGANIZATION, ItemType.POLICY),
        25,
        "relevance",
        null);

    service.search(inputs);

    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient).searchGlobal(captor.capture());

    Query top = captor.getValue().baseQuery();
    assertThat(top).isInstanceOf(BooleanQuery.class);
    BooleanQuery topBool = (BooleanQuery) top;
    // Top-level SHOULD clauses must equal the requested type count.
    long shouldCount = topBool.clauses()
        .stream()
        .filter(c -> c.getOccur() == BooleanClause.Occur.SHOULD)
        .count();
    assertThat(shouldCount).isEqualTo(3);

    // Every per-type subquery is wrapped as a MUST + FILTER pair (subquery + itemType
    // filter), so each top-level SHOULD clause is itself a BooleanQuery whose clauses
    // include a FILTER on the itemType field.
    int itemTypeFilterCount = 0;
    for (BooleanClause c : topBool.clauses()) {
      assertThat(c.getQuery()).isInstanceOf(BooleanQuery.class);
      BooleanQuery perType = (BooleanQuery) c.getQuery();
      for (BooleanClause inner : perType.clauses()) {
        if (inner.getOccur() == BooleanClause.Occur.FILTER && inner.getQuery() instanceof TermQuery tq) {
          if ("itemType".equals(tq.getTerm().field())) {
            itemTypeFilterCount++;
          }
        }
      }
    }
    assertThat(itemTypeFilterCount).isEqualTo(3);
  }

  @Test
  public void search_emptyItemTypes_throwsIllegalArgument() {
    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION, Set.of(), 25, "relevance", null);
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> service.search(inputs));
    verify(searchIndexClient, never()).searchGlobal(any());
  }

  @Test
  public void search_throwsWhenGlobalSearchDisabled() {
    when(searchIndexClient.isGlobalSearchEnabled()).thenReturn(false);
    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null);
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> service.search(inputs));
    verify(searchIndexClient, never()).searchGlobal(any());
  }

  @Test
  public void search_userWithNoPermissions_returnsZeroRowsAndCarriesMatchNoDocs() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());
    org.apache.lucene.search.MatchNoDocsQuery matchNone =
        new org.apache.lucene.search.MatchNoDocsQuery("no permissions resolved");
    when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenReturn(matchNone);
    when(searchIndexClient.wrapWithPermissionFilter(any(), any()))
        .thenAnswer(inv -> {
          Query base = inv.getArgument(0);
          Query filter = inv.getArgument(1);
          return new BooleanQuery.Builder()
              .add(base, BooleanClause.Occur.MUST)
              .add(filter, BooleanClause.Occur.FILTER)
              .build();
        });

    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null);
    service.search(inputs);

    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient).searchGlobal(captor.capture());
    Query sent = captor.getValue().baseQuery();
    assertThat(sent).isInstanceOf(BooleanQuery.class);
    boolean hasMatchNoneFilter = ((BooleanQuery) sent).clauses()
        .stream()
        .anyMatch(c -> c.getOccur() == BooleanClause.Occur.FILTER
            && c.getQuery() instanceof org.apache.lucene.search.MatchNoDocsQuery);
    assertThat(hasMatchNoneFilter)
        .as("permission filter must be carried as a FILTER clause")
        .isTrue();
  }

  @Test
  public void search_returnedRowsTaggedAsIqSource() {
    SearchResultItemDTO row1 = new SearchResultItemDTO();
    row1.applicationName = "app-1";
    SearchResultItemDTO row2 = new SearchResultItemDTO();
    row2.applicationName = "app-2";
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(row1, row2), 2L, List.of()));

    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);

    assertThat(response.rows()).hasSize(2);
    assertThat(response.rows()).allSatisfy(r -> assertThat(r.source()).isEqualTo(SearchSource.LOCAL.value()));
    assertThat(response.rows()).extracting(r -> r.row().applicationName).containsExactly("app-1", "app-2");
  }

  @Test
  public void search_totalHits_cappedAtTrackTotalHitsCap() {
    long rawTotal = AbstractSearchIndexClient.GLOBAL_SEARCH_TRACK_TOTAL_HITS_CAP + 5000L;
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(), rawTotal, List.of()));

    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);

    assertThat(response.total()).isEqualTo(AbstractSearchIndexClient.GLOBAL_SEARCH_TRACK_TOTAL_HITS_CAP);
  }

  @Test
  public void search_totalHits_belowCap_passedThrough() {
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(), 42L, List.of("a", "b")));

    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);

    assertThat(response.total()).isEqualTo(42L);
    assertThat(response.nextSearchAfter()).containsExactly("a", "b");
  }

  @Test
  public void search_exactTotalHits_true_propagatedFromResult() {
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(), 42L, List.of(), true));

    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);

    assertThat(response.exactTotalHits()).isTrue();
  }

  @Test
  public void search_exactTotalHits_forcedFalse_whenCapLowersTotal() {
    // Backend reports an exact count above the cap; after capping the total, the reported count can
    // no longer be exact, so exactTotalHits must flip to false even though the backend said true.
    long rawTotal = AbstractSearchIndexClient.GLOBAL_SEARCH_TRACK_TOTAL_HITS_CAP + 5000L;
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(), rawTotal, List.of(), true));

    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);

    assertThat(response.total()).isEqualTo(AbstractSearchIndexClient.GLOBAL_SEARCH_TRACK_TOTAL_HITS_CAP);
    assertThat(response.exactTotalHits()).isFalse();
  }

  @Test
  public void search_exactTotalHits_false_propagatedFromResult() {
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(),
            AbstractSearchIndexClient.GLOBAL_SEARCH_TRACK_TOTAL_HITS_CAP, List.of(), false));

    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);

    assertThat(response.exactTotalHits()).isFalse();
  }

  @Test
  public void search_invalidSortKey_throwsIllegalArgument() {
    SearchInputs inputs = new SearchInputs("q", Tab.VULNERABILITY,
        Set.of(ItemType.SECURITY_VULNERABILITY), 25, "unknownSortKey", null);

    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> service.search(inputs));
  }

  @Test
  public void search_nullSortKey_defaultsToRelevance() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());

    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, null, null);
    IqLocalSearchResponse response = service.search(inputs);

    assertThat(response.sortKey()).isEqualTo(GlobalSearchSortAllowlist.RELEVANCE);
  }

  @Test
  public void search_cursorSupplied_searchAfterPropagated() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());
    when(searchIndexClient.getLastIndexTime()).thenReturn(42L);
    when(searchIndexClient.backendId()).thenReturn("lucene");

    String token = service.expectedGenerationToken(Tab.APPLICATION, "relevance", 25);
    GlobalSearchCursor cursor = GlobalSearchCursor.newCursor(token, List.of("0.7", "23"));
    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", cursor.encode());

    service.search(inputs);

    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient).searchGlobal(captor.capture());
    assertThat(captor.getValue().searchAfter()).containsExactly("0.7", "23");
  }

  @Test
  public void search_cursorWithStaleGenerationToken_throwsStaleCursor() {
    // Default mock returns: getLastIndexTime() → null, backendId() → null. The computed
    // token derived from those will never equal the hand-picked "stale-generation-token"
    // string, so the mismatch guard fires.
    GlobalSearchCursor cursor = GlobalSearchCursor.newCursor("stale-generation-token", List.of("a"));
    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", cursor.encode());

    assertThatExceptionOfType(StaleCursorException.class)
        .isThrownBy(() -> service.search(inputs));
    verify(searchIndexClient, never()).searchGlobal(any());
  }

  @Test
  public void search_malformedCursor_throwsBadRequest() {
    // A structurally invalid cursor string is client input, so it must surface as a 400
    // (BadRequestException), never a raw IllegalArgumentException that would map to a 500.
    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", "!!!not-base64!!!");

    assertThatThrownBy(() -> service.search(inputs)).isInstanceOf(BadRequestException.class);
    verify(searchIndexClient, never()).searchGlobal(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void search_permissionsRevokedBetweenPages_appliesFreshFilter() {
    // Token validity and permission freshness are independent invariants: a cursor minted while
    // the user had a broad permitted set must still validate on the next page, but the query for
    // that page must be built from the *current* (narrowed) permitted set, not the page-1 set.
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());
    when(searchIndexClient.getLastIndexTime()).thenReturn(42L);
    when(searchIndexClient.backendId()).thenReturn("lucene");

    Set<String> broad = Set.of("ctx-1", "ctx-2", "ctx-3");
    Set<String> narrow = Set.of("ctx-1");

    // Page 1: broad permitted set. Mint the cursor the client would return.
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(broad);
    String token = service.expectedGenerationToken(Tab.APPLICATION, "relevance", 25);
    GlobalSearchCursor cursor = GlobalSearchCursor.newCursor(token, List.of("0.7", "23"));

    // Page 2: permission is revoked mid-pagination — the client now reports the narrow set.
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(narrow);
    SearchInputs page2 = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", cursor.encode());

    // No StaleCursorException: the generation token is unaffected by the permission change.
    service.search(page2);

    ArgumentCaptor<Set<String>> permitted = ArgumentCaptor.forClass(Set.class);
    verify(searchIndexClient).buildAllowedContextIdsFilter(permitted.capture());
    assertThat(permitted.getValue())
        .as("page 2 must filter on the freshly-narrowed permitted set, not the page-1 broad set")
        .isEqualTo(narrow);
    verify(searchIndexClient, times(1)).searchGlobal(any());
  }

  @Test
  public void search_backendIdThrowsUnsupported_failsWithClearIllegalState() {
    // A backend that passes the isGlobalSearchEnabled() guard but leaves backendId() at the
    // throwing default is misconfigured; the service must surface a clear IllegalStateException
    // rather than let the raw UnsupportedOperationException escape as a 500.
    when(searchIndexClient.backendId())
        .thenThrow(new UnsupportedOperationException("StubClient does not implement Global Search backendId()"));

    // A supplied cursor drives the generation-token re-validation path, which calls backendId().
    GlobalSearchCursor cursor = GlobalSearchCursor.newCursor("any-token", List.of("a"));
    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", cursor.encode());

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> service.search(inputs))
        .withMessageContaining("backendId()");
  }

  @Test
  public void search_buildsBoolQuery_evenForEmptyUserQuery() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());

    SearchInputs inputs = new SearchInputs("", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null);
    service.search(inputs);

    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient).searchGlobal(captor.capture());

    Query sent = captor.getValue().baseQuery();
    // Per-type wrap turns the base MatchAllDocsQuery into a BooleanQuery (MUST + FILTER on
    // itemType). The user-empty case still yields a bool-wrapped query.
    assertThat(sent).isInstanceOf(BooleanQuery.class);
  }

  @Test
  public void search_invokesPermissionFilterWiring() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());

    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null);
    service.search(inputs);

    verify(searchIndexClient, times(1)).buildPermittedQuery(any());
    // Composing entry point delegates to the triplet; the interactions must still fire so
    // callers cannot bypass the lookup.
    verify(searchIndexClient, times(1)).getCurrentUserContextIdsWithReadPermission();
    verify(searchIndexClient, times(1)).buildAllowedContextIdsFilter(any());
    verify(searchIndexClient, times(1)).wrapWithPermissionFilter(any(), any());
  }

  @Test
  public void search_inputsNull_throwsNpe() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> service.search(null));
  }

  @Test
  public void search_defaultMode_delegatesLicenseCheckToClient() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());

    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null, false);
    service.search(inputs);

    verify(searchIndexClient, times(1)).checkGlobalSearchMode(false);
  }

  @Test
  public void search_sbomManagerMode_delegatesLicenseCheckToClient() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());

    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null, true);
    service.search(inputs);

    verify(searchIndexClient, times(1)).checkGlobalSearchMode(true);
  }

  @Test
  public void search_licenseCheckThrows_propagates() {
    org.mockito.Mockito.doThrow(new RuntimeException("bad license"))
        .when(searchIndexClient)
        .checkGlobalSearchMode(anyBoolean());

    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null, true);
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> service.search(inputs));
    verify(searchIndexClient, never()).searchGlobal(any());
  }

  @Test
  public void search_sbomManagerMode_excludesApplicationCategoryTypes() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());

    // Request a mix including a mode-excluded type; the excluded type should be dropped
    // silently and the remaining type still searched. With one type left the single-type
    // fast path returns the per-type MUST+FILTER wrapper directly (no top-level SHOULD).
    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION, ItemType.APPLICATION_CATEGORY), 25, "relevance", null, true);
    service.search(inputs);

    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient).searchGlobal(captor.capture());
    Query top = captor.getValue().baseQuery();
    assertThat(top).isInstanceOf(BooleanQuery.class);
    assertThat(hasItemTypeFilter((BooleanQuery) top, ItemType.APPLICATION.searchFieldName())).isTrue();
    assertThat(hasItemTypeFilter((BooleanQuery) top, ItemType.APPLICATION_CATEGORY.searchFieldName())).isFalse();
  }

  @Test
  public void search_defaultMode_excludesSbomMetadata() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());

    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION, ItemType.SBOM_METADATA), 25, "relevance", null, false);
    service.search(inputs);

    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient).searchGlobal(captor.capture());
    Query top = captor.getValue().baseQuery();
    assertThat(top).isInstanceOf(BooleanQuery.class);
    assertThat(hasItemTypeFilter((BooleanQuery) top, ItemType.APPLICATION.searchFieldName())).isTrue();
    assertThat(hasItemTypeFilter((BooleanQuery) top, ItemType.SBOM_METADATA.searchFieldName())).isFalse();
  }

  private static boolean hasItemTypeFilter(final BooleanQuery bq, final String typeName) {
    for (BooleanClause c : bq.clauses()) {
      if (c.getOccur() == BooleanClause.Occur.FILTER && c.getQuery() instanceof TermQuery tq) {
        if ("itemType".equals(tq.getTerm().field()) && typeName.equals(tq.getTerm().text())) {
          return true;
        }
      }
      if (c.getQuery() instanceof BooleanQuery inner && hasItemTypeFilter(inner, typeName)) {
        return true;
      }
    }
    return false;
  }

  @Test
  public void search_allTypesExcludedByMode_returnsEmptyResponseWithWarning() {
    // Legitimate empty state: every requested type is excluded by the current mode. The service
    // returns an empty response (with a warning) rather than throwing, and does not hit the index.
    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.SBOM_METADATA), 25, "relevance", null, false);
    IqLocalSearchResponse response = service.search(inputs);
    assertThat(response.rows()).isEmpty();
    assertThat(response.total()).isZero();
    assertThat(response.exactTotalHits()).isTrue();
    assertThat(response.warnings()).anyMatch(w -> w.contains("No requested item types apply"));
    verify(searchIndexClient, never()).searchGlobal(any());
  }

  @Test
  public void sortFor_relevanceKey_returnsNull() {
    assertThat(IqLocalSearchService.sortFor(Tab.APPLICATION, GlobalSearchSortAllowlist.RELEVANCE)).isNull();
  }

  @Test
  public void fieldSort_isEnabled() {
    // Field sort is on: every allowlisted key now has a sortable doc-values twin on both backends.
    assertThat(IqLocalSearchService.SORT_BY_FIELD_ENABLED).isTrue();
    assertThat(IqLocalSearchService.isFieldSortEnabled()).isTrue();
  }

  @Test
  public void sortFor_stringKey_buildsAscendingKeywordSortOnMappedField() {
    Sort sort = IqLocalSearchService.sortFor(Tab.APPLICATION, "name");
    assertThat(sort).isNotNull();
    SortField field = sort.getSort()[0];
    assertThat(field.getField()).isEqualTo(FieldIdentifier.APPLICATION_NAME.label);
    assertThat(field.getType()).isEqualTo(SortField.Type.STRING);
    assertThat(field.getReverse()).isFalse();
  }

  @Test
  public void sortFor_numericKey_buildsDescendingLongSortOnMappedField() {
    Sort sort = IqLocalSearchService.sortFor(Tab.APPLICATION, "lastEvaluationTime");
    assertThat(sort).isNotNull();
    SortField field = sort.getSort()[0];
    assertThat(field).isInstanceOf(SortedNumericSortField.class);
    assertThat(field.getField()).isEqualTo(FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label);
    assertThat(((SortedNumericSortField) field).getNumericType()).isEqualTo(SortField.Type.LONG);
    assertThat(field.getReverse()).isTrue();
  }

  @Test
  public void sortFor_waiverCreatedKey_buildsDescendingLongSortOnEpochTwin() {
    Sort sort = IqLocalSearchService.sortFor(Tab.WAIVER, GlobalSearchSortAllowlist.WAIVER_CREATED);
    assertThat(sort).isNotNull();
    SortField field = sort.getSort()[0];
    assertThat(field).isInstanceOf(SortedNumericSortField.class);
    assertThat(field.getField()).isEqualTo(FieldIdentifier.POLICY_WAIVER_CREATED_AT_EPOCH_MS.label);
    assertThat(((SortedNumericSortField) field).getNumericType()).isEqualTo(SortField.Type.LONG);
    assertThat(field.getReverse()).isTrue();
  }

  @Test
  public void sortFor_vulnerabilityCvssKey_buildsDescendingFloatSortOnSeverityTwin() {
    Sort sort = IqLocalSearchService.sortFor(Tab.VULNERABILITY, "cvss");
    assertThat(sort).isNotNull();
    SortField field = sort.getSort()[0];
    assertThat(field).isInstanceOf(SortedNumericSortField.class);
    assertThat(field.getField()).isEqualTo(FieldIdentifier.VULNERABILITY_SEVERITY.label);
    // FLOAT (not LONG): the twin is float-sortable-int encoded, so it must be compared as a float.
    assertThat(((SortedNumericSortField) field).getNumericType()).isEqualTo(SortField.Type.FLOAT);
    assertThat(field.getReverse()).isTrue();
  }

  @Test
  public void sortFor_componentPolicyThreatLevelKey_buildsDescendingLongSortOnMaxThreatTwin() {
    Sort sort = IqLocalSearchService.sortFor(Tab.COMPONENT, "policyThreatLevel");
    assertThat(sort).isNotNull();
    SortField field = sort.getSort()[0];
    assertThat(field).isInstanceOf(SortedNumericSortField.class);
    assertThat(field.getField()).isEqualTo(FieldIdentifier.COMPONENT_MAX_POLICY_THREAT_LEVEL.label);
    assertThat(((SortedNumericSortField) field).getNumericType()).isEqualTo(SortField.Type.LONG);
    assertThat(field.getReverse()).isTrue();
  }

  @Test
  public void sortableFieldMap_mapsNewComponentAndVulnerabilitySortKeys() {
    assertThat(IqLocalSearchService.sortableIndexFieldFor(Tab.COMPONENT, "policyThreatLevel"))
        .isEqualTo(FieldIdentifier.COMPONENT_MAX_POLICY_THREAT_LEVEL);
    assertThat(IqLocalSearchService.sortableIndexFieldFor(Tab.VULNERABILITY, "cvss"))
        .isEqualTo(FieldIdentifier.VULNERABILITY_SEVERITY);
  }

  @Test
  public void allowlistAndSortableMap_parity_everyNonRelevanceKeyHasABackingField() {
    // Drift guard: every allowlisted non-relevance key must resolve to a sortable index field, or
    // sortFor logs an invariant-violation and silently falls back to relevance.
    for (Tab tab : Tab.values()) {
      for (String key : GlobalSearchSortAllowlist.allowedFor(tab)) {
        if (GlobalSearchSortAllowlist.RELEVANCE.equals(key)) {
          continue;
        }
        assertThat(IqLocalSearchService.sortableIndexFieldFor(tab, key))
            .as("allowlisted key %s for tab %s must have a SORTABLE_FIELD_BY_KEY entry", key, tab)
            .isNotNull();
      }
    }
  }

  @Test
  public void sortFor_waiverThreatKey_buildsDescendingLongSortOnThreatLevel() {
    Sort sort = IqLocalSearchService.sortFor(Tab.WAIVER, GlobalSearchSortAllowlist.WAIVER_THREAT);
    assertThat(sort).isNotNull();
    SortField field = sort.getSort()[0];
    assertThat(field).isInstanceOf(SortedNumericSortField.class);
    assertThat(field.getField()).isEqualTo(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label);
    assertThat(((SortedNumericSortField) field).getNumericType()).isEqualTo(SortField.Type.LONG);
    assertThat(field.getReverse()).as("threat sorts highest-first (descending)").isTrue();
  }

  @Test
  public void sortFor_waiverExpirationKey_buildsAscendingLongSortOnExpiresTwin() {
    Sort sort = IqLocalSearchService.sortFor(Tab.WAIVER, GlobalSearchSortAllowlist.WAIVER_EXPIRATION);
    assertThat(sort).isNotNull();
    SortField field = sort.getSort()[0];
    assertThat(field).isInstanceOf(SortedNumericSortField.class);
    assertThat(field.getField()).isEqualTo(FieldIdentifier.POLICY_WAIVER_EXPIRES_AT_EPOCH_MS.label);
    assertThat(((SortedNumericSortField) field).getNumericType()).isEqualTo(SortField.Type.LONG);
    // Expiration sorts soonest-first (ASCENDING), unlike created/threat.
    assertThat(field.getReverse()).as("expiration sorts soonest-first (ascending)").isFalse();
    // Never-expiring waivers (missing value) must sort LAST under ascending order.
    assertThat(field.getMissingValue()).as("never-expires sorts last").isEqualTo(Long.MAX_VALUE);
  }

  @Test
  public void search_nonGlobalUser_emptyContextSet_gets_matchNoDocsFilter() {
    // Simulate a non-global user (no ROOT/global membership) with an empty permitted set.
    // buildAllowedContextIdsFilter must return MatchNoDocsQuery so the request cannot match any
    // document — not null (which would mean "global access").
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());
    when(searchIndexClient.buildAllowedContextIdsFilter(any()))
        .thenReturn(new org.apache.lucene.search.MatchNoDocsQuery("no permitted contexts"));
    when(searchIndexClient.wrapWithPermissionFilter(any(), any()))
        .thenAnswer(inv -> new BooleanQuery.Builder()
            .add((Query) inv.getArgument(0), BooleanClause.Occur.MUST)
            .add((Query) inv.getArgument(1), BooleanClause.Occur.FILTER)
            .build());

    SearchInputs inputs = new SearchInputs("q", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null);
    service.search(inputs);

    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient).searchGlobal(captor.capture());
    Query sent = captor.getValue().baseQuery();
    // The final query must carry MatchNoDocsQuery as a FILTER clause, guaranteeing zero rows.
    assertThat(sent).isInstanceOf(BooleanQuery.class);
    boolean hasMatchNone = ((BooleanQuery) sent).clauses()
        .stream()
        .anyMatch(c -> c.getOccur() == BooleanClause.Occur.FILTER
            && c.getQuery() instanceof org.apache.lucene.search.MatchNoDocsQuery);
    assertThat(hasMatchNone).isTrue();
  }

  @Test
  public void sortableFieldMap_mapsNewAppAndViolationSortKeys() {
    // PR-B sort keys: applications sort by latest evaluation, violations by threat level.
    assertThat(IqLocalSearchService.sortableIndexFieldFor(Tab.APPLICATION, "lastEvaluationTime"))
        .isEqualTo(com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS);
    assertThat(IqLocalSearchService.sortableIndexFieldFor(Tab.VIOLATION, "threat"))
        .isEqualTo(com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL);
    // WAIVER threat + expiration sort keys back onto the waiver threat-level and expires-at twins.
    assertThat(IqLocalSearchService.sortableIndexFieldFor(Tab.WAIVER, GlobalSearchSortAllowlist.WAIVER_THREAT))
        .isEqualTo(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL);
    assertThat(IqLocalSearchService.sortableIndexFieldFor(Tab.WAIVER, GlobalSearchSortAllowlist.WAIVER_EXPIRATION))
        .isEqualTo(FieldIdentifier.POLICY_WAIVER_EXPIRES_AT_EPOCH_MS);
  }

  @Test
  public void sortableFieldMap_mapsApplicationsPolicyThreatAndViolationStateSortKeys() {
    // A5: policy-threat-level sort -> max-threat int twin. A6: violation-state sort -> worst-state ordinal.
    assertThat(IqLocalSearchService.sortableIndexFieldFor(Tab.APPLICATION, "policyThreatLevel"))
        .isEqualTo(FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL);
    assertThat(IqLocalSearchService.sortableIndexFieldFor(Tab.APPLICATION, "violationState"))
        .isEqualTo(FieldIdentifier.APPLICATION_VIOLATION_STATE_SORT_ORDINAL);
  }

  @Test
  public void buildSortField_maxPolicyThreat_sortsDescending() {
    // A5: highest threat first (reverse LONG), matching the prototype policy-threat sort.
    SortField field = IqLocalSearchService.buildSortField(FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL)
        .getSort()[0];
    assertThat(field).isInstanceOf(SortedNumericSortField.class);
    assertThat(((SortedNumericSortField) field).getNumericType()).isEqualTo(SortField.Type.LONG);
    assertThat(field.getReverse()).isTrue();
    assertThat(field.getField()).isEqualTo(FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label);
  }

  @Test
  public void buildSortField_violationStateOrdinal_sortsAscendingOpenFirst() {
    // A6: Open(0) before Waived(1) before Legacy(2). Ascending numeric; apps with no ordinal sort last.
    SortField field = IqLocalSearchService.buildSortField(FieldIdentifier.APPLICATION_VIOLATION_STATE_SORT_ORDINAL)
        .getSort()[0];
    assertThat(field).isInstanceOf(SortedNumericSortField.class);
    assertThat(((SortedNumericSortField) field).getNumericType()).isEqualTo(SortField.Type.LONG);
    assertThat(field.getReverse()).as("violation-state ordinal sorts ascending (Open first)").isFalse();
    assertThat(field.getField()).isEqualTo(FieldIdentifier.APPLICATION_VIOLATION_STATE_SORT_ORDINAL.label);
  }

  @Test
  public void buildSortField_numericFields_sortDescendingAsLong() {
    // Numeric-backed fields (latest-evaluation, threat level, waiver created-at) sort on their
    // numeric doc-values twin, descending (newest/highest first). The numeric-vs-string branch is
    // chosen from the FieldIdentifier itself, not a hardcoded sort-key set.
    for (FieldIdentifier f : List.of(
        FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS,
        FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL,
        FieldIdentifier.POLICY_WAIVER_CREATED_AT_EPOCH_MS,
        FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL))
    {
      SortField field = IqLocalSearchService.buildSortField(f).getSort()[0];
      // Numeric fields use SortedNumericSortField (reads the SortedNumericDocValues twin); its
      // getType() is CUSTOM, so assert the numeric type via getNumericType().
      assertThat(field).as("field for %s", f).isInstanceOf(SortedNumericSortField.class);
      assertThat(((SortedNumericSortField) field).getNumericType())
          .as("numeric type for %s", f)
          .isEqualTo(SortField.Type.LONG);
      assertThat(field.getReverse()).as("reverse for %s", f).isTrue();
      assertThat(field.getField()).isEqualTo(f.label);
    }
  }

  @Test
  public void buildSortField_waiverExpiration_sortsAscendingLongMissingLast() {
    // Expiration is the one numeric field that sorts ASCENDING (soonest first); a never-expiring
    // waiver carries no value and must sort LAST (Long.MAX_VALUE missing sentinel).
    SortField field =
        IqLocalSearchService.buildSortField(FieldIdentifier.POLICY_WAIVER_EXPIRES_AT_EPOCH_MS).getSort()[0];
    assertThat(field).isInstanceOf(SortedNumericSortField.class);
    assertThat(((SortedNumericSortField) field).getNumericType()).isEqualTo(SortField.Type.LONG);
    assertThat(field.getReverse()).as("expiration ascending").isFalse();
    assertThat(field.getMissingValue()).as("never-expires last").isEqualTo(Long.MAX_VALUE);
    assertThat(field.getField()).isEqualTo(FieldIdentifier.POLICY_WAIVER_EXPIRES_AT_EPOCH_MS.label);
  }

  @Test
  public void buildSortField_stringFields_sortAscendingAsKeyword() {
    for (FieldIdentifier f : List.of(
        FieldIdentifier.APPLICATION_NAME,
        FieldIdentifier.COMPONENT_NAME,
        FieldIdentifier.VULNERABILITY_ID,
        FieldIdentifier.POLICY_EVALUATION_STAGE,
        FieldIdentifier.POLICY_VIOLATION_POLICY_NAME))
    {
      SortField field = IqLocalSearchService.buildSortField(f).getSort()[0];
      assertThat(field.getType()).as("type for %s", f).isEqualTo(SortField.Type.STRING);
      assertThat(field.getReverse()).as("reverse for %s", f).isFalse();
      assertThat(field.getField()).isEqualTo(f.label);
    }
  }

  @Test
  public void sortableFieldMap_coversAllAllowlistedNonRelevanceKeys() {
    // Drift guard: every allowlisted non-relevance (tab, key) MUST map to an IQ-local index field
    // in SORTABLE_FIELD_BY_KEY, checked independently of SORT_BY_FIELD_ENABLED. This is what keeps
    // the allowlist and the sortable-field map from diverging (e.g. re-adding WAIVER+name here
    // without a matching index field would fail this test rather than log an invariant violation
    // at request time once the flag flips on).
    for (Tab tab : Tab.values()) {
      for (String key : GlobalSearchSortAllowlist.allowedFor(tab)) {
        if (GlobalSearchSortAllowlist.RELEVANCE.equals(key)) {
          continue;
        }
        assertThat(IqLocalSearchService.sortableIndexFieldFor(tab, key))
            .as("Allowlisted sort key '%s' on tab %s must have a SORTABLE_FIELD_BY_KEY entry", key, tab)
            .isNotNull();
      }
    }
  }

  // -- helpers ---------------------------------------------------------------------------------

  private static GlobalSearchResult emptyResult() {
    return new GlobalSearchResult(new ArrayList<>(), 0L, List.of());
  }

  private static void assertOnlyBuilderTypes(final Query q) {
    Deque<Query> stack = new ArrayDeque<>();
    stack.push(q);
    while (!stack.isEmpty()) {
      Query current = stack.pop();
      String cls = current.getClass().getName();
      assertThat(cls)
          .as("QueryParser residue detected: %s", cls)
          .doesNotContain("queryparser");
      assertThat(current).isInstanceOfAny(
          BooleanQuery.class,
          TermQuery.class,
          MatchAllDocsQuery.class,
          org.apache.lucene.search.PrefixQuery.class);
      if (current instanceof BooleanQuery bq) {
        for (BooleanClause c : bq.clauses()) {
          stack.push(c.getQuery());
        }
      }
    }
  }
}
