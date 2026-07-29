/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
  public void query_waiverEntityType_usesPolicyWaiverItemType() {
    IndexQueryRequest req = request(Map.of("query", "security"));
    service.query(IndexQueryType.WAIVER, req);

    Query sent = capture();
    assertThat(sent.toString()).contains("policy_waiver");
  }

  @Test
  public void query_waiverUnknownFilterKey_rejectedWith400() {
    IndexQueryRequest req = request(Map.of("bogusKey", "x"));
    assertThatExceptionOfType(FilterValidationException.class)
        .isThrownBy(() -> service.query(IndexQueryType.WAIVER, req));
  }

  @Test
  public void query_waiverHappyPath_mapsRowFields() {
    SearchResultItemDTO w = waiverDto("waiver-1", "Security Policy", "Acme Prod", "Acme");
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(w), 1, List.of()));

    IndexQueryRequest req = new IndexQueryRequest("WAIVER", Map.of("query", "security"), 1, 25, null, null, false);
    IndexQueryResponse resp = service.query(IndexQueryType.WAIVER, req);

    assertThat(resp.entityType()).isEqualTo("WAIVER");
    assertThat(resp.rows()).singleElement()
        .satisfies(r -> {
          assertThat(r.getId()).isEqualTo("waiver-1");
          assertThat(r.getTitle()).isEqualTo("Security Policy");
          assertThat(r.getFields()).containsEntry("isAuto", false);
          assertThat(r.getHref()).isEqualTo("/preview/waivers/application/app-1/waiver-1");
        });
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
  public void query_waiverScopeFacet_bucketsByGranularityIncludingComponent_wholeCorpusCounts() {
    // A page mixing application-, organization- and component-scoped waivers; the scope facet buckets
    // by the indexed policyWaiverScope granularity (application / organization / component) with
    // whole-corpus counts. The component bucket is the follow-up addition (component-targeted waivers).
    SearchResultItemDTO app = waiverDtoWithScope("w-app", "application");
    SearchResultItemDTO org = waiverDtoWithScope("w-org", "organization");
    SearchResultItemDTO comp = waiverDtoWithScope("w-comp", "component");
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(app, org, comp), 3, List.of()));
    when(searchIndexClient.count(contains("policyWaiverScope:\"application\""))).thenReturn(30L);
    when(searchIndexClient.count(contains("policyWaiverScope:\"organization\""))).thenReturn(12L);
    when(searchIndexClient.count(contains("policyWaiverScope:\"component\""))).thenReturn(7L);

    IndexQueryRequest req = new IndexQueryRequest("WAIVER", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.WAIVER, req);

    Map<String, Long> scope = resp.facets()
        .get("scope")
        .stream()
        .collect(java.util.stream.Collectors.toMap(
            IndexQueryResponse.IndexQueryFacetBucket::value, IndexQueryResponse.IndexQueryFacetBucket::count));
    assertThat(scope)
        .containsEntry("application", 30L)
        .containsEntry("organization", 12L)
        .containsEntry("component", 7L);
  }

  private static SearchResultItemDTO waiverDtoWithScope(final String id, final String scope) {
    SearchResultItemDTO d = waiverDto(id, "APPLICATION", 5, false);
    d.policyWaiverScope = scope;
    return d;
  }

  @Test
  public void query_waiverStatusFacet_countsActiveExpiringExpiredAutoWaived_wholeCorpus() {
    // Fixed clock so the active/expiring/expired epoch boundaries are deterministic.
    final java.time.Clock fixed =
        java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);
    final IndexQueryService svc = new IndexQueryService(iq, searchIndexClient, null, fixed);
    final long now = fixed.millis();
    final long windowEnd = now + java.time.Duration.ofDays(IndexQueryService.STATUS_EXPIRING_WINDOW_DAYS).toMillis();

    SearchResultItemDTO w = waiverDto("w-1", "APPLICATION", 5, false);
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(List.of(w), 1, List.of()));
    final String expiredRange = "policyWaiverExpiresAtEpochMs:[* TO " + now + "]";
    final String expiringRange = "policyWaiverExpiresAtEpochMs:[" + now + " TO " + windowEnd + "]";
    // active = "NOT <expiredRange>"; expired = ends with <expiredRange> but has no NOT;
    // expiring = the [now TO now+window] range; auto = policyWaiverAuto:"true". argThat disambiguates
    // active-vs-expired since both contain the expired range substring. Every expiry-derived bucket is
    // scoped to committed waivers (itemType:policy_waiver) so pending-request docs never inflate them.
    when(searchIndexClient.count(argThat(qy -> qy != null && qy.contains("NOT " + expiredRange)
        && qy.contains("itemType:policy_waiver AND NOT")))).thenReturn(80L);
    when(searchIndexClient.count(argThat(qy -> qy != null && qy.contains(expiringRange)
        && qy.contains("itemType:policy_waiver AND policyWaiverExpiresAtEpochMs:[" + now)))).thenReturn(15L);
    when(searchIndexClient.count(argThat(
        qy -> qy != null && qy.contains(expiredRange) && !qy.contains("NOT ") && !qy.contains(expiringRange)
            && qy.contains("itemType:policy_waiver AND " + expiredRange))))
                .thenReturn(20L);
    when(searchIndexClient.count(argThat(qy -> qy != null && qy.contains("policyWaiverAuto:\"true\"")))).thenReturn(5L);

    IndexQueryRequest req = new IndexQueryRequest("WAIVER", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = svc.query(IndexQueryType.WAIVER, req);

    Map<String, Long> status = resp.facets()
        .get("status")
        .stream()
        .collect(java.util.stream.Collectors.toMap(
            IndexQueryResponse.IndexQueryFacetBucket::value, IndexQueryResponse.IndexQueryFacetBucket::count));
    assertThat(status)
        .containsEntry(IndexQueryService.STATUS_ACTIVE, 80L)
        .containsEntry(IndexQueryService.STATUS_EXPIRING, 15L)
        .containsEntry(IndexQueryService.STATUS_EXPIRED, 20L)
        .containsEntry(IndexQueryService.STATUS_AUTO_WAIVED, 5L);
  }

  @Test
  public void query_waiverPolicyTypeFacet_bucketsByDenormalizedPolicyType_wholeCorpus() {
    // A page of waivers with distinct policy types; the policyType facet buckets by the denormalized
    // policyWaiverPolicyType keyword with whole-corpus counts.
    SearchResultItemDTO sec = waiverDtoWithPolicyType("w-sec", "security");
    SearchResultItemDTO lic = waiverDtoWithPolicyType("w-lic", "license");
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(List.of(sec, lic), 2, List.of()));
    when(searchIndexClient.count(contains("policyWaiverPolicyType:\"security\""))).thenReturn(40L);
    when(searchIndexClient.count(contains("policyWaiverPolicyType:\"license\""))).thenReturn(9L);

    IndexQueryRequest req = new IndexQueryRequest("WAIVER", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.WAIVER, req);

    Map<String, Long> policyType = resp.facets()
        .get("policyType")
        .stream()
        .collect(java.util.stream.Collectors.toMap(
            IndexQueryResponse.IndexQueryFacetBucket::value, IndexQueryResponse.IndexQueryFacetBucket::count));
    assertThat(policyType).containsEntry("security", 40L).containsEntry("license", 9L);
  }

  @Test
  public void query_waiverStatesFilter_spansBothItemTypes() {
    // requested + rejected states compile to POLICY_WAIVER_REQUEST status clauses; existing to
    // POLICY_WAIVER. Proves the waiverStates multi-select query spans both item types.
    IndexQueryFilterCompiler.CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.WAIVER, Map.of("waiverStates", List.of("existing", "requested", "rejected")));
    assertThat(compiled.q()).contains("itemType:policy_waiver AND policyWaiverAuto:\"false\"");
    assertThat(compiled.q())
        .contains("itemType:policy_waiver_request AND policyWaiverRequestStatus:\"REQUESTED\"");
    assertThat(compiled.q())
        .contains("itemType:policy_waiver_request AND policyWaiverRequestStatus:\"REJECTED\"");
  }

  @Test
  public void query_waiverStatesWithRequestStates_doesNotLayerManualOnlyAutoDefault() {
    // Regression: the absent-includeAutoWaivers default must NOT append a top-level auto:"false"
    // restriction when waiverStates is present. A bare policyWaiverAuto:"false" AND'd on top would
    // drop every POLICY_WAIVER_REQUEST doc (request docs carry no policyWaiverAuto field), zeroing
    // out the requested/rejected tabs. waiverStates owns the item-type + auto scoping per state.
    IndexQueryFilterCompiler.CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.WAIVER, Map.of("waiverStates", List.of("requested")));
    // The request-state clause is present...
    assertThat(compiled.q())
        .contains("itemType:policy_waiver_request AND policyWaiverRequestStatus:\"REQUESTED\"");
    // ...and NO manual-only auto default is layered on (which would exclude all request docs).
    assertThat(compiled.autoWaiverRestrictionClause()).isNull();
    assertThat(compiled.q()).doesNotContain("policyWaiverAuto");
  }

  @Test
  public void query_waiverStatesWithExplicitAutoFalse_doesNotANDMutuallyExclusiveClauses() {
    // Regression: an explicit includeAutoWaivers:false must NOT layer the manual-only
    // (itemType:policy_waiver AND auto:"false") restriction on top of a request-scoped waiverStates
    // selection. AND'ing it with the request-doc clause (itemType:policy_waiver_request AND ...) is an
    // impossible item-type condition that zeroes the requested/rejected tabs regardless of Map order.
    IndexQueryFilterCompiler.CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.WAIVER,
        new java.util.LinkedHashMap<>(Map.of("waiverStates", List.of("requested"), "includeAutoWaivers", false)));
    // The request-state clause survives...
    assertThat(compiled.q())
        .contains("itemType:policy_waiver_request AND policyWaiverRequestStatus:\"REQUESTED\"");
    // ...and the manual-only auto restriction is dropped, so the two item types are not AND'd.
    assertThat(compiled.autoWaiverRestrictionClause()).isNull();
    assertThat(compiled.q()).doesNotContain("policyWaiverAuto");
  }

  @Test
  public void query_emptyWaiverStates_behavesLikeAbsent_noManualOnlyDefault() {
    // Classic includeAutoWaivers: absent/true → both kinds (no default clause). Empty
    // waiverStates:[] selects no state, so it matches absent filters: no auto restriction.
    IndexQueryFilterCompiler.CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.WAIVER, Map.of("waiverStates", List.of()));
    assertThat(compiled.autoWaiverRestrictionClause()).isNull();
    assertThat(compiled.q()).doesNotContain("policyWaiverAuto");
  }

  @Test
  public void query_waiverStatesRequested_returnsRequestDocs_notZeroedByAutoDefault() {
    // End-to-end at the service layer: waiverStates=[requested] returns the request row rather than
    // being zeroed by a spurious auto:false restriction. The search mock echoes the request doc.
    SearchResultItemDTO reqDoc = new SearchResultItemDTO();
    reqDoc.itemType = "POLICY_WAIVER_REQUEST";
    reqDoc.policyWaiverId = "req-1";
    reqDoc.policyWaiverRequestStatus = "REQUESTED";
    reqDoc.requesterName = "Alice";
    reqDoc.policyWaiverScopeOwnerType = "APPLICATION";
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(List.of(reqDoc), 1, List.of()));

    IndexQueryRequest req =
        new IndexQueryRequest("WAIVER", Map.of("waiverStates", List.of("requested")), 1, 25, null, null, false);
    IndexQueryResponse resp = service.query(IndexQueryType.WAIVER, req);

    assertThat(resp.rows()).singleElement().satisfies(r -> {
      assertThat(r.getId()).isEqualTo("req-1");
      assertThat(r.getFields().get("status")).isEqualTo("REQUESTED");
      assertThat(r.getFields().get("isRequested")).isEqualTo(Boolean.TRUE);
    });
  }

  @Test
  public void query_statusFilter_doesNotLayerManualOnlyAutoDefault() {
    // A status filter targets the request-only policyWaiverRequestStatus field, so the manual-only
    // default (which restricts to POLICY_WAIVER docs) must be suppressed — otherwise status=REJECTED
    // AND (itemType:policy_waiver AND auto:false) matches no request docs and returns nothing.
    IndexQueryFilterCompiler.CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.WAIVER, Map.of("status", List.of("REJECTED")));
    assertThat(compiled.q()).contains("policyWaiverRequestStatus:\"REJECTED\"");
    assertThat(compiled.autoWaiverRestrictionClause()).isNull();
    assertThat(compiled.q()).doesNotContain("policyWaiverAuto");
  }

  @Test
  public void query_waiverDefault_absentIncludeAutoWaivers_includesBothKinds() {
    // Absent includeAutoWaivers means Classic "include both" (no clause). Auto-only is Ana
    // isAuto:["true"]; manual-only is an explicit includeAutoWaivers:false.
    IndexQueryFilterCompiler.CompiledQuery compiled =
        IndexQueryFilterCompiler.compileWithClauses(IndexQueryType.WAIVER, Map.of());
    assertThat(compiled.autoWaiverRestrictionClause()).isNull();
    assertThat(compiled.q()).doesNotContain("policyWaiverAuto");
  }

  @Test
  public void query_waiverStatesFilter_rejectsUnknownState() {
    assertThatExceptionOfType(FilterValidationException.class).isThrownBy(
        () -> IndexQueryFilterCompiler.compileWithClauses(
            IndexQueryType.WAIVER, Map.of("waiverStates", List.of("bogus"))))
        .satisfies(e -> assertThat(e.getCode()).isEqualTo(FilterValidationException.Code.INVALID_FILTER));
  }

  private static SearchResultItemDTO waiverDtoWithPolicyType(final String id, final String policyType) {
    SearchResultItemDTO d = waiverDto(id, "APPLICATION", 5, false);
    d.policyWaiverPolicyType = policyType;
    return d;
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
  public void query_waiverThreatSort_appliesDescendingThreatLevelSort() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());
    IndexQueryRequest req = new IndexQueryRequest("WAIVER", Map.of(), 1, 25, "threat", null, false);
    service.query(IndexQueryType.WAIVER, req);

    org.apache.lucene.search.SortField field = captureRequest().sort().getSort()[0];
    assertThat(field.getField())
        .isEqualTo(com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label);
    assertThat(field.getReverse()).as("threat highest-first").isTrue();
  }

  @Test
  public void query_waiverExpirationSort_appliesAscendingExpiresSort() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());
    IndexQueryRequest req = new IndexQueryRequest("WAIVER", Map.of(), 1, 25, "expiration", null, false);
    service.query(IndexQueryType.WAIVER, req);

    org.apache.lucene.search.SortField field = captureRequest().sort().getSort()[0];
    assertThat(field.getField())
        .isEqualTo(com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_EXPIRES_AT_EPOCH_MS.label);
    assertThat(field.getReverse()).as("expiration soonest-first (ascending)").isFalse();
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

  private static SearchResultItemDTO waiverDto(
      final String waiverId,
      final String policyName,
      final String appName,
      final String org)
  {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "POLICY_WAIVER";
    d.policyWaiverId = waiverId;
    d.policyWaiverPolicyId = "policy-1";
    d.policyWaiverPolicyName = policyName;
    d.policyWaiverScopeOwnerType = "APPLICATION";
    d.policyWaiverScopeOwnerId = "app-1";
    d.policyWaiverIsAuto = false;
    d.applicationName = appName;
    d.organizationName = org;
    return d;
  }

  private static GlobalSearchResult emptyResult() {
    return new GlobalSearchResult(List.of(), 0, List.of());
  }

  private static SearchResultItemDTO waiverDto(
      final String id,
      final String scopeOwnerType,
      final int threatLevel,
      final boolean auto)
  {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "POLICY_WAIVER";
    d.policyWaiverId = id;
    d.policyWaiverScopeOwnerType = scopeOwnerType;
    d.policyWaiverThreatLevel = threatLevel;
    d.policyWaiverAuto = auto;
    d.organizationName = "Acme";
    return d;
  }
}
