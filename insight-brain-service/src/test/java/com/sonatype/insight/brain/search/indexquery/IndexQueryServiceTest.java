/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.search.global.FilterValidationException;
import com.sonatype.insight.brain.search.global.GlobalSearchCursor;
import com.sonatype.insight.brain.search.global.GlobalSearchRequest;
import com.sonatype.insight.brain.search.global.GlobalSearchResult;
import com.sonatype.insight.brain.search.global.GlobalSearchSortAllowlist;
import com.sonatype.insight.brain.search.global.IqLocalSearchService;
import com.sonatype.insight.brain.search.global.StaleCursorException;
import com.sonatype.insight.brain.search.global.Tab;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IndexQueryServiceTest
{
  @Mock
  private SearchIndexClient searchIndexClient;

  private IndexQueryService service;

  private IqLocalSearchService iq;

  @Before
  public void setUp() {
    iq = new IqLocalSearchService(searchIndexClient);
    service = new IndexQueryService(iq, searchIndexClient, null);

    when(searchIndexClient.isGlobalSearchEnabled()).thenReturn(true);
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of());
    when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenReturn(null);
    when(searchIndexClient.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    when(searchIndexClient.buildPermittedQuery(any())).thenCallRealMethod();
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());
  }

  @Test
  public void query_applicationEntityType_appliesPermissionPrimitive_notLegacyEnvelope() {
    IndexQueryRequest req = request(Map.of("query", "acme"));
    service.query(IndexQueryType.APPLICATION, req);

    verify(searchIndexClient, times(1)).buildPermittedQuery(any());
    verify(searchIndexClient, times(1)).searchGlobal(any());
  }

  @Test
  public void query_violationEntityType_unionsTwoItemTypes() {
    IndexQueryRequest req = request(Map.of("query", "log4j"));
    service.query(IndexQueryType.VIOLATION, req);

    Query top = capture();
    assertThat(top).isInstanceOf(BooleanQuery.class);
    long shoulds = ((BooleanQuery) top).clauses()
        .stream()
        .filter(c -> c.getOccur() == BooleanClause.Occur.SHOULD)
        .count();
    // VIOLATION unions POLICY_VIOLATION + LEGAL_VIOLATION → two top-level SHOULD subqueries.
    assertThat(shoulds).isEqualTo(2);
  }

  @Test
  public void query_policyEntityType_appliesPermissionPrimitive() {
    IndexQueryRequest req = request(Map.of("policyTypes", List.of("Security")));
    service.query(IndexQueryType.POLICY, req);
    verify(searchIndexClient, times(1)).buildPermittedQuery(any());
  }

  @Test
  public void query_rangeFilter_compilesToRangeQuery() {
    IndexQueryRequest req = request(Map.of("policyThreatLevel", List.of(7, 10)));
    service.query(IndexQueryType.POLICY, req);
    Query sent = capture();
    assertThat(sent.toString()).contains("policyThreatLevel");
  }

  @Test
  public void query_nonFiniteRangeBound_rejectedWith400() {
    IndexQueryRequest req = request(Map.of("policyThreatLevel", List.of(0, Double.NaN)));
    assertThatExceptionOfType(FilterValidationException.class)
        .isThrownBy(() -> service.query(IndexQueryType.POLICY, req))
        .satisfies(e -> assertThat(e.getCode()).isEqualTo(FilterValidationException.Code.INVALID_FILTER));
  }

  @Test
  public void query_outOfRangeIntegerRangeBound_rejectedWith400() {
    IndexQueryRequest req = request(Map.of("policyThreatLevel", List.of(0, 1e20)));
    assertThatExceptionOfType(FilterValidationException.class)
        .isThrownBy(() -> service.query(IndexQueryType.POLICY, req))
        .satisfies(e -> assertThat(e.getCode()).isEqualTo(FilterValidationException.Code.INVALID_FILTER));
  }

  @Test
  public void query_unknownFilterKey_rejectedWith400() {
    IndexQueryRequest req = request(Map.of("bogusKey", "x"));
    assertThatExceptionOfType(FilterValidationException.class)
        .isThrownBy(() -> service.query(IndexQueryType.APPLICATION, req));
  }

  @Test
  public void query_disallowedSortOnPolicy_rejectedWith400() {
    IndexQueryRequest req = new IndexQueryRequest("POLICY", Map.of(), 1, 25, "name", null, false);
    assertThatExceptionOfType(FilterValidationException.class)
        .isThrownBy(() -> service.query(IndexQueryType.POLICY, req))
        .satisfies(e -> assertThat(e.getCode()).isEqualTo(FilterValidationException.Code.SORT_NOT_ALLOWED));
  }

  @Test
  public void query_malformedSearchAfter_rejectedWith400_notServerError() {
    // page > 1 so the request clears the page-1-must-not-carry-cursor guard and reaches cursor decode.
    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 2, 25, null, "!!!not-base64!!!", false);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.query(IndexQueryType.APPLICATION, req));
  }

  @Test
  public void query_pageOneWithCursor_rejectedWith400() {
    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, "some-stale-cursor", false);
    assertThatExceptionOfType(FilterValidationException.class)
        .isThrownBy(() -> service.query(IndexQueryType.APPLICATION, req))
        .satisfies(e -> assertThat(e.getCode()).isEqualTo(FilterValidationException.Code.INVALID_FILTER));
  }

  @Test
  public void query_nullPageWithCursor_rejectedWith400() {
    // A null page must not silently paginate a cursor as page 1: the consistency check runs before the
    // null->1 default.
    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), null, 25, null, "some-stale-cursor", false);
    assertThatExceptionOfType(FilterValidationException.class)
        .isThrownBy(() -> service.query(IndexQueryType.APPLICATION, req))
        .satisfies(e -> assertThat(e.getCode()).isEqualTo(FilterValidationException.Code.INVALID_FILTER));
  }

  @Test
  public void query_rangeFilterWithBothBoundsNull_rejectedWith400() {
    Map<String, Object> filters = new java.util.HashMap<>();
    filters.put("policyThreatLevel", java.util.Arrays.asList(null, null));
    IndexQueryRequest req = request(filters);
    assertThatExceptionOfType(FilterValidationException.class)
        .isThrownBy(() -> service.query(IndexQueryType.POLICY, req))
        .satisfies(e -> assertThat(e.getCode()).isEqualTo(FilterValidationException.Code.INVALID_FILTER));
  }

  @Test
  public void query_pageSizeOutOfRange_rejectedWith400() {
    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 999999, null, null, false);
    assertThatExceptionOfType(FilterValidationException.class)
        .isThrownBy(() -> service.query(IndexQueryType.APPLICATION, req))
        .satisfies(e -> assertThat(e.getCode()).isEqualTo(FilterValidationException.Code.INVALID_FILTER));
  }

  @Test
  public void query_facetsRequested_populatesFacetValuesFromReturnedRows_withWholeCorpusCounts() {
    SearchResultItemDTO a = appDto("acme-1", "App One", "Acme");
    SearchResultItemDTO b = appDto("acme-2", "App Two", "Acme");
    SearchResultItemDTO c = appDto("widget-1", "App Three", "Widget Co");
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(a, b, c), 3, List.of()));
    when(searchIndexClient.count(contains("organizationName:\"Acme\""))).thenReturn(9L);
    when(searchIndexClient.count(contains("organizationName:\"Widget Co\""))).thenReturn(4L);

    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);

    assertThat(resp.facets()).isNotNull();
    // Counts are whole-corpus now, so the page-only flag is false.
    assertThat(resp.facetsOverPageOnly()).isFalse();
    assertThat(resp.facets()).containsKey("organizations");
    Map<String, Long> orgCounts = resp.facets()
        .get("organizations")
        .stream()
        .collect(java.util.stream.Collectors.toMap(
            IndexQueryResponse.IndexQueryFacetBucket::value, IndexQueryResponse.IndexQueryFacetBucket::count));
    // Whole-corpus counts (9/4), NOT the page tallies (2/1). Buckets are keyed on the org name so the
    // value round-trips back through the name-matching organizations filter.
    assertThat(orgCounts).containsEntry("Acme", 9L).containsEntry("Widget Co", 4L);
  }

  @Test
  public void query_facetCounts_areWholeCorpus_notPageOnly_andItemTypeScoped() {
    // One Acme app fits on this page, but the whole RBAC-scoped corpus has 42 Acme apps.
    SearchResultItemDTO a = appDto("acme-1", "App One", "Acme");
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(a), 1, List.of()));
    when(searchIndexClient.count(contains("organizationName:\"Acme\""))).thenReturn(42L);

    IndexQueryRequest req = new IndexQueryRequest(
        "APPLICATION", Map.of("organizations", List.of("Acme")), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);

    long acme = resp.facets()
        .get("organizations")
        .stream()
        .filter(bkt -> bkt.value().equals("Acme"))
        .findFirst()
        .orElseThrow()
        .count();
    // Whole-corpus count (42), not the single page row.
    assertThat(acme).isEqualTo(42L);
    // The count must be item-type + filter scoped; the org facet buckets and counts by name so it
    // round-trips through the name-matching organizations filter.
    ArgumentCaptor<String> countQueries = ArgumentCaptor.forClass(String.class);
    verify(searchIndexClient, org.mockito.Mockito.atLeastOnce()).count(countQueries.capture());
    assertThat(countQueries.getAllValues()).anySatisfy(qy -> {
      assertThat(qy).contains("itemType:application");
      assertThat(qy).contains("organizationName:\"Acme\"");
    });
  }

  @Test
  public void query_facetValuesExceedingBudget_boundsCountCalls_andWarnsTruncated_butKeepsFixedFacets() {
    // A diverse VIOLATION page: 20 distinct orgs + 20 apps + 20 categories + 20 stages + 20 policy
    // types. The per-field cap (20) admits all of each (100 candidate dynamic counts), far over the
    // budget, so count() fan-out is bounded and a truncation warning is surfaced. The fixed
    // states/waiverType facets are processed FIRST, so the always-relevant OPEN/WAIVED and AUTO/MANUAL
    // counts survive rather than being starved by the dynamic facets.
    List<SearchResultItemDTO> page = new java.util.ArrayList<>();
    for (int i = 0; i < 20; i++) {
      SearchResultItemDTO d = new SearchResultItemDTO();
      d.itemType = "POLICY_VIOLATION";
      d.policyViolationId = "pv-" + i;
      d.applicationName = "App " + i;
      d.applicationId = "app-" + i + "-id";
      d.organizationName = "Org " + i;
      d.organizationId = "org-" + i;
      d.applicationCategoryNames = List.of("Cat " + i);
      d.policyEvaluationStage = "stage-" + i;
      d.policyViolationThreatCategory = "cat-" + i;
      d.policyViolationWaiverStatus = "Active";
      page.add(d);
    }
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(page, page.size(), List.of()));
    when(searchIndexClient.count(any())).thenReturn(1L);

    IndexQueryRequest req = new IndexQueryRequest("VIOLATION", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.VIOLATION, req);

    verify(searchIndexClient, org.mockito.Mockito.atMost(IndexQueryService.MAX_FACET_COUNT_QUERIES)).count(any());
    assertThat(resp.warnings()).contains(IndexQueryService.FACET_COUNTS_TRUNCATED);
    // The bounded, always-wanted fixed facets are computed first, so they are never truncated away.
    assertThat(resp.facets().get("states")).extracting(IndexQueryResponse.IndexQueryFacetBucket::value)
        .containsExactlyInAnyOrder("OPEN", "WAIVED");
    assertThat(resp.facets().get("waiverType")).extracting(IndexQueryResponse.IndexQueryFacetBucket::value)
        .containsExactlyInAnyOrder("AUTO", "MANUAL");
  }

  @Test
  public void query_facetValuesWithinBudget_noTruncationWarning() {
    // Two distinct orgs: a handful of count() calls, well within the budget, so no warning.
    SearchResultItemDTO a = appDto("acme-1", "App One", "Acme");
    SearchResultItemDTO b = appDto("widget-1", "App Two", "Widget Co");
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(List.of(a, b), 2, List.of()));
    when(searchIndexClient.count(any())).thenReturn(1L);

    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);

    assertThat(resp.warnings()).doesNotContain(IndexQueryService.FACET_COUNTS_TRUNCATED);
  }

  @Test
  public void query_facetsNotRequested_facetsOmitted() {
    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, false);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);
    assertThat(resp.facets()).isNull();
    assertThat(resp.facetsOverPageOnly()).isFalse();
  }

  @Test
  public void query_facetsRequestedButNoBuckets_returnsEmptyMap() {
    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);
    assertThat(resp.facets()).isNotNull();
    assertThat(resp.facets().values()).allSatisfy(buckets -> assertThat(buckets).isEmpty());
  }

  @Test
  public void query_runsInDefaultMode_notSbomManagerMode() {
    SearchResultItemDTO a = appDto("acme-1", "App One", "Acme");
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(List.of(a), 1, List.of()));

    IndexQueryRequest req = request(Map.of("query", "acme"));
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);

    // Observable state: the Lifecycle APPLICATION row surfaces. SBOM-manager mode searches different
    // item types, so the returned row would be absent (or a different entityType) had the mode flipped.
    assertThat(resp.entityType()).isEqualTo("APPLICATION");
    assertThat(resp.rows()).singleElement()
        .satisfies(r -> assertThat(r.getEntityType()).isEqualTo("APPLICATION"));
    // Lifecycle-only endpoint: the mode gate is always driven with isSbomManagerMode=false. The current
    // doubles cannot prove the Lifecycle-only guarantee purely by state, so the interaction stays.
    verify(searchIndexClient).checkGlobalSearchMode(eq(false));
  }

  @Test
  public void query_relevanceSortUppercaseOnPolicy_rejectedCaseSensitively() {
    IndexQueryRequest req = new IndexQueryRequest("POLICY", Map.of(), 1, 25, "RELEVANCE", null, false);
    assertThatExceptionOfType(FilterValidationException.class)
        .isThrownBy(() -> service.query(IndexQueryType.POLICY, req))
        .satisfies(e -> assertThat(e.getCode()).isEqualTo(FilterValidationException.Code.SORT_NOT_ALLOWED));
  }

  @Test
  public void query_pageZero_rejectedWith400() {
    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 0, 25, null, null, false);
    assertThatExceptionOfType(FilterValidationException.class)
        .isThrownBy(() -> service.query(IndexQueryType.APPLICATION, req))
        .satisfies(e -> assertThat(e.getCode()).isEqualTo(FilterValidationException.Code.INVALID_FILTER));
  }

  @Test
  public void query_pageBeyondFirstWithoutCursor_rejectedWith400() {
    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 3, 25, null, null, false);
    assertThatExceptionOfType(FilterValidationException.class)
        .isThrownBy(() -> service.query(IndexQueryType.APPLICATION, req))
        .satisfies(e -> assertThat(e.getCode())
            .isEqualTo(FilterValidationException.Code.DEEP_PAGINATION_NOT_SUPPORTED));
  }

  @Test
  public void query_droppedRowWithoutIdField_addsWarning() {
    SearchResultItemDTO missingId = new SearchResultItemDTO();
    missingId.itemType = "APPLICATION";
    missingId.organizationName = "Acme";
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(missingId), 1, List.of()));

    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, false);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);

    assertThat(resp.rows()).isEmpty();
    assertThat(resp.warnings()).anySatisfy(w -> assertThat(w).contains("omitted"));
  }

  @Test
  public void query_violationWithNullId_droppedNotFallenBackToComponentName() {
    // componentName is a non-unique display name; a violation missing its unique policyViolationId must
    // be dropped (and counted in the dropped-row warning), never emitted with componentName as its id.
    SearchResultItemDTO v = new SearchResultItemDTO();
    v.itemType = "POLICY_VIOLATION";
    v.componentName = "log4j-core";
    v.policyViolationId = null;
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(List.of(v), 1, List.of()));

    IndexQueryRequest req = new IndexQueryRequest("VIOLATION", Map.of(), 1, 25, null, null, false);
    IndexQueryResponse resp = service.query(IndexQueryType.VIOLATION, req);

    assertThat(resp.rows()).isEmpty();
    assertThat(resp.rows()).noneSatisfy(r -> assertThat(r.getId()).isEqualTo("log4j-core"));
    assertThat(resp.warnings()).anySatisfy(w -> assertThat(w).contains("omitted"));
  }

  @Test
  public void query_pagination_mintsNextCursorWhenMoreResults() {
    SearchResultItemDTO a = appDto("acme-1", "App One", "Acme");
    when(searchIndexClient.getLastIndexTime()).thenReturn(1000L);
    when(searchIndexClient.backendId()).thenReturn("lucene");
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(a), 50, List.of("cursorTuple")));

    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, false);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);
    assertThat(resp.nextSearchAfter()).isNotBlank();
  }

  @Test
  public void query_pagination_pinsNextCursorToServingBackend() {
    SearchResultItemDTO a = appDto("acme-1", "App One", "Acme");
    when(searchIndexClient.getLastIndexTime()).thenReturn(1000L);
    when(searchIndexClient.backendId()).thenReturn("primary");
    // The secondary served this page under a Hybrid fallback; the next cursor must pin to it.
    when(searchIndexClient.searchGlobal(any())).thenReturn(
        new GlobalSearchResult(List.of(a), 50, List.of("cursorTuple"), true, "secondary"));

    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, false);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);
    assertThat(resp.nextSearchAfter()).isNotBlank();

    // A blank APPLICATION request sort resolves to the per-entity default (lastEvaluationTime), so
    // the cursor is minted/validated against that sort key, not relevance.
    String defaultSort = GlobalSearchSortAllowlist.defaultSortFor(Tab.APPLICATION);
    // The generation token the default backend would mint/validate on the follow-up request (5-arg
    // with null servingBackendId falls back to backendId() == "primary"). The served cursor must NOT
    // match it — it is pinned to "secondary" — so the default backend rejects it as stale rather than
    // silently mis-paginating a secondary-format cursor.
    String defaultBackendToken = iq.mintNextCursor(
        Tab.APPLICATION, defaultSort, 25, List.of("cursorTuple"), null)
        .generationToken();
    assertThatExceptionOfType(StaleCursorException.class)
        .isThrownBy(() -> GlobalSearchCursor.decode(resp.nextSearchAfter(), defaultBackendToken));

    // The same cursor minted for the serving backend does validate, confirming it is pinned there.
    String servingToken = iq.mintNextCursor(
        Tab.APPLICATION, defaultSort, 25, List.of("cursorTuple"), "secondary")
        .generationToken();
    assertThat(servingToken).isNotEqualTo(defaultBackendToken);
    assertThat(GlobalSearchCursor.decode(resp.nextSearchAfter(), servingToken).sortValues())
        .containsExactly("cursorTuple");
  }

  @Test
  public void query_lastPage_noNextCursor() {
    SearchResultItemDTO a = appDto("acme-1", "App One", "Acme");
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(a), 1, List.of()));
    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, false);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);
    assertThat(resp.nextSearchAfter()).isNull();
  }

  @Test
  public void query_blankSort_applicationDefaultsToLastEvaluationTimeDesc() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());
    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, false);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);

    org.apache.lucene.search.Sort sort = captureRequest().sort();
    assertThat(sort).as("application default sort must be a field sort, not relevance").isNotNull();
    org.apache.lucene.search.SortField field = sort.getSort()[0];
    assertThat(field.getField())
        .isEqualTo(
            com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label);
    assertThat(field.getReverse()).as("newest first").isTrue();
  }

  @Test
  public void query_blankSort_violationDefaultsToThreatDesc() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());
    IndexQueryRequest req = new IndexQueryRequest("VIOLATION", Map.of(), 1, 25, null, null, false);
    service.query(IndexQueryType.VIOLATION, req);

    org.apache.lucene.search.Sort sort = captureRequest().sort();
    assertThat(sort).isNotNull();
    org.apache.lucene.search.SortField field = sort.getSort()[0];
    assertThat(field.getField())
        .isEqualTo(com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label);
    assertThat(field.getReverse()).isTrue();
  }

  @Test
  public void query_blankSort_waiverDefaultsToCreatedDesc() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());
    IndexQueryRequest req = new IndexQueryRequest("WAIVER", Map.of(), 1, 25, null, null, false);
    service.query(IndexQueryType.WAIVER, req);

    org.apache.lucene.search.Sort sort = captureRequest().sort();
    assertThat(sort).isNotNull();
    org.apache.lucene.search.SortField field = sort.getSort()[0];
    assertThat(field.getField())
        .isEqualTo(com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_CREATED_AT_EPOCH_MS.label);
    assertThat(field.getReverse()).isTrue();
  }

  @Test
  public void query_blankSort_policyStaysRelevance() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());
    IndexQueryRequest req = new IndexQueryRequest("POLICY", Map.of(), 1, 25, null, null, false);
    service.query(IndexQueryType.POLICY, req);
    // POLICY has no tab and is relevance-only: sortFor returns null (native _score).
    assertThat(captureRequest().sort()).isNull();
  }

  private Query capture() {
    return captureRequest().baseQuery();
  }

  private GlobalSearchRequest captureRequest() {
    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient).searchGlobal(captor.capture());
    return captor.getValue();
  }

  private static IndexQueryRequest request(final Map<String, Object> filters) {
    return new IndexQueryRequest("ANY", filters, 1, 25, null, null, false);
  }

  private static SearchResultItemDTO appDto(final String publicId, final String name, final String org) {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "APPLICATION";
    d.applicationPublicId = publicId;
    d.applicationId = publicId + "-id";
    d.applicationName = name;
    d.organizationName = org;
    d.organizationId = org.toLowerCase(java.util.Locale.ROOT).replace(' ', '-');
    return d;
  }

  private static GlobalSearchResult emptyResult() {
    return new GlobalSearchResult(List.of(), 0, List.of());
  }
}
