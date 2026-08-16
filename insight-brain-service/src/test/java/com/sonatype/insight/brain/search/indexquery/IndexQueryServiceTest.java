/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.integration.OrganizationSummaryService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.security.SecurityAspectControl;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.global.FilterValidationException;
import com.sonatype.insight.brain.search.global.GlobalSearchCursor;
import com.sonatype.insight.brain.search.global.GlobalSearchRequest;
import com.sonatype.insight.brain.search.global.GlobalSearchResult;
import com.sonatype.insight.brain.search.global.GlobalSearchSortAllowlist;
import com.sonatype.insight.brain.search.global.IqLocalSearchService;
import com.sonatype.insight.brain.search.global.StaleCursorException;
import com.sonatype.insight.brain.search.global.Tab;
import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexReadSessionFactory;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class IndexQueryServiceTest
{
  @Mock
  private SearchIndexClient searchIndexClient;

  @Mock
  private IndexReadSessionFactory sessionFactory;

  @Mock
  private IndexReadSession session;

  @Mock
  private ConversionHelper conversionHelper;

  private IndexQueryService service;

  private IqLocalSearchService iq;

  private OrganizationDAO organizationDAO;

  private ApplicationDAO applicationDAO;

  private TagDAO tagDAO;

  private PolicyDAO policyDAO;

  private OrganizationSummaryService organizationSummaryService;

  @BeforeEach
  public void setUp() {
    // ROOT org exclusion is now id-based (Organization.ROOT_ORGANIZATION_ID), so no
    // organizationDAO.getById stub is needed to drive it (CLM-44713 slice 2).
    organizationDAO = mock(OrganizationDAO.class);
    applicationDAO = mock(ApplicationDAO.class);
    tagDAO = mock(TagDAO.class);
    policyDAO = mock(PolicyDAO.class);
    organizationSummaryService = mock(OrganizationSummaryService.class);
    lenient().when(organizationSummaryService.getOrganizationsForRead(anySet())).thenAnswer(inv -> {
      Set<String> ids = inv.getArgument(0);
      return ids.stream().map(id -> {
        Organization o = new Organization();
        o.setId(id);
        // The read gate returns rows loaded by OrganizationDAO#getByIds, so they carry names; the org
        // facet takes its display names from them rather than re-fetching by id.
        o.setName("Org " + id);
        return o;
      }).toList();
    });

    iq = new IqLocalSearchService(searchIndexClient);
    service = new IndexQueryService(organizationDAO, applicationDAO, tagDAO, policyDAO, iq, searchIndexClient,
        sessionFactory, conversionHelper, organizationSummaryService, null);

    when(searchIndexClient.isSearchPreviewEnabled()).thenReturn(true);
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of());
    when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenReturn(null);
    when(searchIndexClient.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    when(searchIndexClient.buildPermittedQuery(any())).thenCallRealMethod();
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());
    when(sessionFactory.open()).thenReturn(session);
    when(conversionHelper.stringToQuery(anyString())).thenReturn(new MatchAllDocsQuery());
    // Default mock for NUMERIC threatLevel facet (used by WAIVER queries)
    when(searchIndexClient.aggregateCountByField(anyString(), eq("policyWaiverThreatLevel"), any()))
        .thenReturn(new MetricAggregationResult(0L, Map.of()));
    // The org value-facet path calls the @AuthzFilter-woven OrganizationSummaryService#getOrganizationsForRead;
    // these pure-Mockito tests bind no Shiro SecurityManager, so disable CTW enforcement for the aspect.
    SecurityAspectControl.disableEnforcement();
  }

  @AfterEach
  public void restoreSecurityEnforcement() {
    SecurityAspectControl.enableEnforcement();
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
    // VALUE facets now compute via termsAggregation, bucketed on the opaque org id (CLM-44713 slice 2)
    // rather than the display name; the human-readable name is resolved separately via OrganizationDAO.
    when(session.termsAggregation(any(), eq("parentOrganizationId"), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("acme", 9L), new IndexTermsBucket("widget-co", 4L)));
    stubOrganizationNames(Map.of("acme", "Acme", "widget-co", "Widget Co"));

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
    // Whole-corpus counts (9/4), NOT the page tallies (2/1). Buckets are keyed on the org id, resolved
    // (via one batched OrganizationDAO.getByIds call) to a displayName for rendering.
    assertThat(orgCounts).containsEntry("acme", 9L).containsEntry("widget-co", 4L);
    Map<String, String> orgDisplayNames = resp.facets()
        .get("organizations")
        .stream()
        .collect(java.util.stream.Collectors.toMap(
            IndexQueryResponse.IndexQueryFacetBucket::value, IndexQueryResponse.IndexQueryFacetBucket::displayName));
    assertThat(orgDisplayNames).containsEntry("acme", "Acme").containsEntry("widget-co", "Widget Co");
  }

  @Test
  public void query_organizationsFacet_excludesOrgsOutsideCallerReadScope() {
    // The org facet aggregates on the ancestor closure, which can surface parent orgs above the caller's
    // read scope. Only orgs the caller may read must be emitted: here the read gate reports acme readable
    // but widget-co not, so widget-co must be absent from the facet even though it has a bucket.
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(appDto("acme-1", "App One", "Acme")), 1, List.of()));
    when(session.termsAggregation(any(), eq("parentOrganizationId"), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("acme", 9L), new IndexTermsBucket("widget-co", 4L)));
    stubOrganizationNames(Map.of("acme", "Acme"));
    when(organizationSummaryService.getOrganizationsForRead(anySet())).thenAnswer(inv -> {
      Set<String> ids = inv.getArgument(0);
      return ids.stream()
          .filter(id -> !"widget-co".equals(id))
          .map(id -> {
            Organization o = new Organization();
            o.setId(id);
            o.setName("Acme");
            return o;
          })
          .toList();
    });

    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);

    assertThat(resp.facets().get("organizations"))
        .extracting(IndexQueryResponse.IndexQueryFacetBucket::value)
        .containsExactly("acme")
        .doesNotContain("widget-co");
  }

  @Test
  public void query_organizationsFilterActive_stillOffersTheOtherOrganizations() {
    // Selecting one organization must not collapse the Organizations facet to just that organization,
    // or the section becomes single-use: no multi-select, no switching, no way back but Reset. The facet
    // is computed via termsAggregation with its own clauses dropped, so unselected orgs are offered.
    SearchResultItemDTO acme = appDto("acme-1", "App One", "Acme");
    SearchResultItemDTO widget = appDto("widget-1", "App Two", "Widget Co");
    SearchResultItemDTO globex = appDto("globex-1", "App Three", "Globex");
    // The filtered page holds only Acme.
    when(searchIndexClient
        .searchGlobal(argThat(r -> r != null && r.baseQuery().toString().contains("parentOrganizationName:acme"))))
            .thenReturn(new GlobalSearchResult(List.of(acme), 1, List.of()));
    // VALUE facets now return buckets from termsAggregation, bucketed on the opaque org id.
    when(session.termsAggregation(any(), eq("parentOrganizationId"), anyInt()))
        .thenReturn(List.of(
            new IndexTermsBucket("acme", 1L),
            new IndexTermsBucket("widget-co", 1L),
            new IndexTermsBucket("globex", 1L)));

    IndexQueryRequest req = new IndexQueryRequest(
        "APPLICATION", Map.of("organizations", List.of("Acme")), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);

    // The rows honour the filter...
    assertThat(resp.rows()).hasSize(1);
    // ...but every organization is still selectable, so a second org can be added or swapped in.
    assertThat(resp.facets().get("organizations")).extracting(IndexQueryResponse.IndexQueryFacetBucket::value)
        .containsExactlyInAnyOrder("acme", "widget-co", "globex");
  }

  @Test
  public void query_organizationsFilterActive_doesNotWidenAnUnrelatedFacet() {
    // Only the facet on the filtered field drops its own clauses. An unrelated facet keeps counting
    // against the fully filtered base, so selecting an org still narrows the applications facet.
    SearchResultItemDTO acme = appDto("acme-1", "App One", "Acme");
    SearchResultItemDTO widget = appDto("widget-1", "App Two", "Widget Co");
    when(searchIndexClient
        .searchGlobal(argThat(r -> r != null && r.baseQuery().toString().contains("parentOrganizationName:acme"))))
            .thenReturn(new GlobalSearchResult(List.of(acme), 1, List.of()));
    // VALUE facets now compute via termsAggregation, bucketed on the opaque application id. The
    // applications facet is narrowed by the org filter.
    when(session.termsAggregation(any(), eq("applicationId"), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("acme-1-id", 1L)));

    IndexQueryRequest req = new IndexQueryRequest(
        "APPLICATION", Map.of("organizations", List.of("Acme")), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);

    assertThat(resp.facets().get("applications")).extracting(IndexQueryResponse.IndexQueryFacetBucket::value)
        .containsExactly("acme-1-id");
  }

  @Test
  public void query_freeTextAndOrgFilter_facetBaseKeepsTheFreeText() {
    // A facet's base drops only its OWN structured dimension. The free-text refinement stays applied, or
    // the facet offers organizations that match no row the user can currently see.
    SearchResultItemDTO acme = appDto("acme-1", "Log4j App", "Acme");
    SearchResultItemDTO widget = appDto("widget-1", "Log4j Two", "Widget Co");

    // Page: free text + org filter -> Acme only.
    when(searchIndexClient.searchGlobal(argThat(r -> r != null && queryOf(r).contains("parentOrganizationName:acme"))))
        .thenReturn(new GlobalSearchResult(List.of(acme), 1, List.of()));
    // VALUE facets now compute via termsAggregation, bucketed on the opaque org id.
    when(session.termsAggregation(any(), eq("parentOrganizationId"), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("acme", 1L), new IndexTermsBucket("widget-co", 1L)));

    IndexQueryRequest req = new IndexQueryRequest(
        "APPLICATION",
        Map.of("query", "log4j", "organizations", List.of("Acme")),
        1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);

    // The page search keeps the free-text refinement.
    ArgumentCaptor<GlobalSearchRequest> requests = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient, times(1)).searchGlobal(requests.capture());
    assertThat(requests.getAllValues()).isNotEmpty()
        .allSatisfy(r -> assertThat(queryOf(r)).contains("log4j"));

    // Other orgs matching the text are still offered, so the facet is not collapsed to the selection.
    assertThat(resp.facets().get("organizations")).extracting(IndexQueryResponse.IndexQueryFacetBucket::value)
        .containsExactlyInAnyOrder("acme", "widget-co");
  }

  @Test
  public void query_facetCounts_areWholeCorpus_notPageOnly_andItemTypeScoped() {
    // One Acme app fits on this page, but the whole RBAC-scoped corpus has 42 Acme apps.
    SearchResultItemDTO a = appDto("acme-1", "App One", "Acme");
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(a), 1, List.of()));
    // VALUE facets now compute via termsAggregation, bucketed on the opaque org id.
    when(session.termsAggregation(any(), eq("parentOrganizationId"), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("acme", 42L)));

    IndexQueryRequest req = new IndexQueryRequest(
        "APPLICATION", Map.of("organizations", List.of("Acme")), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);

    long acme = resp.facets()
        .get("organizations")
        .stream()
        .filter(bkt -> bkt.value().equals("acme"))
        .findFirst()
        .orElseThrow()
        .count();
    // Whole-corpus count (42), not the single page row.
    assertThat(acme).isEqualTo(42L);
  }

  @Test
  public void query_facetValuesExceedingBudget_boundsCountCalls_andWarnsTruncated_butKeepsFixedFacets() {
    // A diverse VIOLATION page: 20 distinct orgs + 20 apps + 20 categories + 20 stages + 20 policy
    // types. The per-field cap (20) admits all of each. VALUE facets now use termsAggregation,
    // so the fixed states/waiverType facets are still computed first.
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

    IndexQueryRequest req = new IndexQueryRequest("VIOLATION", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.VIOLATION, req);

    // The bounded, always-wanted fixed facets are computed first, so they are never truncated away.
    assertThat(resp.facets().get("states")).extracting(IndexQueryResponse.IndexQueryFacetBucket::value)
        .containsExactlyInAnyOrder("OPEN", "WAIVED");
    assertThat(resp.facets().get("waiverType")).extracting(IndexQueryResponse.IndexQueryFacetBucket::value)
        .containsExactlyInAnyOrder("AUTO", "MANUAL");
  }

  @Test
  public void query_truncationWarning_namesTheTruncatedFacetKeys() {
    // The truncation warning names the facets whose buckets were omitted so a client can tell which
    // rail sections are incomplete. With VALUE facets using termsAggregation (no count budget),
    // no truncation warning appears.
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

    IndexQueryRequest req = new IndexQueryRequest("VIOLATION", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.VIOLATION, req);

    // VALUE facets use termsAggregation (no count budget for VALUE facets), so no truncation warning.
    assertThat(resp.warnings())
        .noneSatisfy(w -> assertThat(w).startsWith(IndexQueryService.FACET_COUNTS_TRUNCATED));
    // The fixed facets are still complete.
    assertThat(resp.facets().get("states")).extracting(IndexQueryResponse.IndexQueryFacetBucket::value)
        .containsExactlyInAnyOrder("OPEN", "WAIVED");
    assertThat(resp.facets().get("waiverType")).extracting(IndexQueryResponse.IndexQueryFacetBucket::value)
        .containsExactlyInAnyOrder("AUTO", "MANUAL");
  }

  @Test
  public void query_truncationWarning_namesEveryStarvedFacet_notJustTheFirst() {
    // With VALUE facets using termsAggregation, no count budget is used for VALUE facets.
    // This test now verifies that no truncation warning appears when all facet values fit.
    List<SearchResultItemDTO> page = new java.util.ArrayList<>();
    for (int i = 0; i < 20; i++) {
      SearchResultItemDTO d = appDto("app-" + i, "App " + i, "Org " + i);
      d.applicationCategoryNames = List.of("Cat " + i);
      d.applicationViolationStages = List.of("stage-" + i);
      d.applicationViolationPolicyTypes = List.of("ptype-" + i);
      d.applicationViolationStates = List.of("state-" + i);
      page.add(d);
    }
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(page, page.size(), List.of()));

    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);

    // VALUE facets use termsAggregation (no count budget for VALUE facets), so no truncation warning.
    assertThat(resp.warnings())
        .noneSatisfy(w -> assertThat(w).startsWith(IndexQueryService.FACET_COUNTS_TRUNCATED));
  }

  @Test
  public void query_facetValuesWithinBudget_noTruncationWarning() {
    // Two distinct orgs: no count budget is used for VALUE facets, so no warning.
    SearchResultItemDTO a = appDto("acme-1", "App One", "Acme");
    SearchResultItemDTO b = appDto("widget-1", "App Two", "Widget Co");
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(List.of(a, b), 2, List.of()));

    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);

    assertThat(resp.warnings())
        .noneSatisfy(w -> assertThat(w).startsWith(IndexQueryService.FACET_COUNTS_TRUNCATED));
  }

  @Test
  public void query_countBudgetExhausted_warnsAboutCountsOnly_notValueNarrowing() {
    // A dense page: VALUE facets use termsAggregation, no count budget for VALUE facets.
    List<SearchResultItemDTO> page = new java.util.ArrayList<>();
    for (int i = 0; i < 20; i++) {
      page.add(appDto("app-" + i, "App " + i, "Org " + i));
    }
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(page, page.size(), List.of()));

    IndexQueryRequest req = new IndexQueryRequest("VIOLATION", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.VIOLATION, req);

    // No truncation warnings for VALUE facets (now computed via termsAggregation)
    assertThat(resp.warnings())
        .noneSatisfy(w -> assertThat(w).startsWith(IndexQueryService.FACET_COUNTS_TRUNCATED));
  }

  @Test
  public void query_selfFilteredValueFacets_stillOfferUnselectedValues() {
    // A self-filtered VALUE/NUMERIC facet aggregates against a base with its own clauses removed, so it
    // still offers sibling values, and does so without an extra search.
    SearchResultItemDTO acme = appDto("acme-1", "App One", "Acme");
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(List.of(acme), 1, List.of()));
    when(session.termsAggregation(any(), eq("parentOrganizationId"), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("acme", 1L), new IndexTermsBucket("widget-co", 5L)));
    when(session.termsAggregation(any(), eq("applicationId"), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("acme-1-id", 1L), new IndexTermsBucket("app-2-id", 3L)));

    IndexQueryRequest req = new IndexQueryRequest(
        "APPLICATION",
        Map.of(
            "organizations", List.of("Acme"),
            "applications", List.of("App One")),
        1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);

    // One page search only: the facet values come from aggregations, not extra searches.
    verify(searchIndexClient, times(1)).searchGlobal(any());
    // Sibling values still offered via aggregation
    assertThat(resp.facets().get("organizations")).extracting(IndexQueryResponse.IndexQueryFacetBucket::value)
        .containsExactlyInAnyOrder("acme", "widget-co");
    assertThat(resp.warnings())
        .noneSatisfy(w -> assertThat(w).startsWith(IndexQueryService.FACET_COUNTS_TRUNCATED));
  }

  @Test
  public void query_everyWaiverValueFacetFiltered_returnsAllFacetsViaAggregation() {
    // WAIVER declares the most value facets of any entity type. All are now computed via aggregation.
    SearchResultItemDTO w = waiverDto("w-1", "APPLICATION", 5, false);
    w.policyWaiverScope = "application";
    w.policyWaiverPolicyType = "security";
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(List.of(w), 1, List.of()));

    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER",
        Map.of(
            "organizations", List.of("Acme"),
            "applications", List.of("App One"),
            "policy", List.of("Security-Critical"),
            "policyTypes", List.of("security"),
            "scope", List.of("application"),
            "policyThreatLevel", List.of(5, 10)),
        1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.WAIVER, req);

    // No truncation warnings (aggregation-based)
    assertThat(resp.warnings())
        .noneSatisfy(warning -> assertThat(warning).startsWith(IndexQueryService.FACET_COUNTS_TRUNCATED));
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
    // whole-corpus counts via termsAggregation. The component bucket is the follow-up addition.
    SearchResultItemDTO app = waiverDtoWithScope("w-app", "application");
    SearchResultItemDTO org = waiverDtoWithScope("w-org", "organization");
    SearchResultItemDTO comp = waiverDtoWithScope("w-comp", "component");
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(app, org, comp), 3, List.of()));
    // VALUE facet now computes via termsAggregation
    when(session.termsAggregation(any(), eq("policyWaiverScope"), anyInt()))
        .thenReturn(List.of(
            new IndexTermsBucket("application", 30L),
            new IndexTermsBucket("organization", 12L),
            new IndexTermsBucket("component", 7L)));

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
  public void query_applicationStagesFacet_bucketsByDenormalizedStageIds_wholeCorpusCounts() {
    // Two apps whose denormalized applicationViolationStage sets union to {build, stage-release}; each
    // bucket counts the whole RBAC-scoped corpus on the applicationViolationStage field via termsAggregation.
    SearchResultItemDTO a = appDto("acme-1", "App One", "Acme");
    a.applicationViolationStages = List.of("build", "stage-release");
    SearchResultItemDTO b = appDto("acme-2", "App Two", "Acme");
    b.applicationViolationStages = List.of("build");
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(a, b), 2, List.of()));
    // VALUE facet now computes via termsAggregation
    when(session.termsAggregation(any(), eq("applicationViolationStage"), anyInt()))
        .thenReturn(List.of(
            new IndexTermsBucket("build", 11L),
            new IndexTermsBucket("stage-release", 3L)));

    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);

    Map<String, Long> stages = resp.facets()
        .get("stages")
        .stream()
        .collect(java.util.stream.Collectors.toMap(
            IndexQueryResponse.IndexQueryFacetBucket::value, IndexQueryResponse.IndexQueryFacetBucket::count));
    assertThat(stages).containsEntry("build", 11L).containsEntry("stage-release", 3L);
  }

  @Test
  public void query_applicationDiversePage_keepsBoundedFacets_andOnlyTruncatesNameFacets() {
    // A maximally-diverse APPLICATION page: 20 distinct orgs + 20 app names + 20 categories, which alone
    // want 60 of the 90-count budget, plus 20 distinct stage/policyType/state values each. The bounded
    // stages/policyTypes/violationStates facets are processed FIRST so they survive; ordering them after the
    // name facets would return them EMPTY, silently removing those sections from the left nav.
    List<SearchResultItemDTO> page = new java.util.ArrayList<>();
    for (int i = 0; i < 20; i++) {
      SearchResultItemDTO d = appDto("app-" + i, "App " + i, "Org " + i);
      d.applicationCategoryNames = List.of("Cat " + i);
      d.applicationViolationStages = List.of("stage-" + i);
      d.applicationViolationPolicyTypes = List.of("ptype-" + i);
      d.applicationViolationStates = List.of("state-" + i);
      page.add(d);
    }
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(page, page.size(), List.of()));
    // VALUE facets: stub termsAggregation to return 20 buckets each (per-field cap)
    java.util.List<IndexTermsBucket> stageBuckets = new java.util.ArrayList<>();
    java.util.List<IndexTermsBucket> policyTypeBuckets = new java.util.ArrayList<>();
    java.util.List<IndexTermsBucket> stateBuckets = new java.util.ArrayList<>();
    for (int i = 0; i < IndexQueryService.MAX_FACET_BUCKETS_PER_FIELD; i++) {
      stageBuckets.add(new IndexTermsBucket("stage-" + i, 1L));
      policyTypeBuckets.add(new IndexTermsBucket("ptype-" + i, 1L));
      stateBuckets.add(new IndexTermsBucket("state-" + i, 1L));
    }
    when(session.termsAggregation(any(), eq("applicationViolationStage"), anyInt())).thenReturn(stageBuckets);
    when(session.termsAggregation(any(), eq("applicationViolationPolicyType"), anyInt())).thenReturn(policyTypeBuckets);
    when(session.termsAggregation(any(), eq("applicationViolationState"), anyInt())).thenReturn(stateBuckets);

    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);

    // With VALUE facets using termsAggregation, all facets are complete at the per-field cap.
    assertThat(resp.facets().get("stages")).hasSize(IndexQueryService.MAX_FACET_BUCKETS_PER_FIELD);
    assertThat(resp.facets().get("policyTypes")).hasSize(IndexQueryService.MAX_FACET_BUCKETS_PER_FIELD);
    assertThat(resp.facets().get("violationStates")).hasSize(IndexQueryService.MAX_FACET_BUCKETS_PER_FIELD);
  }

  @Test
  public void query_applicationPolicyTypesFacet_bucketsByDenormalizedPolicyTypes_wholeCorpusCounts() {
    SearchResultItemDTO a = appDto("acme-1", "App One", "Acme");
    a.applicationViolationPolicyTypes = List.of("security", "license");
    SearchResultItemDTO b = appDto("acme-2", "App Two", "Acme");
    b.applicationViolationPolicyTypes = List.of("security");
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(a, b), 2, List.of()));
    // VALUE facet now computes via termsAggregation
    when(session.termsAggregation(any(), eq("applicationViolationPolicyType"), anyInt()))
        .thenReturn(List.of(
            new IndexTermsBucket("security", 8L),
            new IndexTermsBucket("license", 2L)));

    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);

    Map<String, Long> policyTypes = resp.facets()
        .get("policyTypes")
        .stream()
        .collect(java.util.stream.Collectors.toMap(
            IndexQueryResponse.IndexQueryFacetBucket::value, IndexQueryResponse.IndexQueryFacetBucket::count));
    assertThat(policyTypes).containsEntry("security", 8L).containsEntry("license", 2L);
  }

  @Test
  public void query_applicationViolationStatesFacet_bucketsByDenormalizedStates_wholeCorpusCounts() {
    SearchResultItemDTO a = appDto("acme-1", "App One", "Acme");
    a.applicationViolationStates = List.of("open", "waived");
    SearchResultItemDTO b = appDto("acme-2", "App Two", "Acme");
    b.applicationViolationStates = List.of("open");
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(a, b), 2, List.of()));
    // VALUE facet now computes via termsAggregation
    when(session.termsAggregation(any(), eq("applicationViolationState"), anyInt()))
        .thenReturn(List.of(
            new IndexTermsBucket("open", 14L),
            new IndexTermsBucket("waived", 5L)));

    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.APPLICATION, req);

    Map<String, Long> states = resp.facets()
        .get("violationStates")
        .stream()
        .collect(java.util.stream.Collectors.toMap(
            IndexQueryResponse.IndexQueryFacetBucket::value, IndexQueryResponse.IndexQueryFacetBucket::count));
    assertThat(states).containsEntry("open", 14L).containsEntry("waived", 5L);
  }

  @Test
  public void query_waiverPolicyFacet_bucketsByPolicyId_wholeCorpusCounts_withResolvedDisplayNames() {
    // Buckets come from termsAggregation on policyWaiverPolicyId (CLM-44713 slice 2); the display name
    // is resolved via one batched PolicyDAO.getByIds call.
    SearchResultItemDTO a = waiverDto("w-1", "Security-High", "App One", "Acme");
    SearchResultItemDTO b = waiverDto("w-2", "Security-High", "App Two", "Acme");
    SearchResultItemDTO c = waiverDto("w-3", "License-Copyleft", "App Three", "Acme");
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(a, b, c), 3, List.of()));
    // VALUE facet now computes via termsAggregation
    when(session.termsAggregation(any(), eq("policyWaiverPolicyId"), anyInt()))
        .thenReturn(List.of(
            new IndexTermsBucket("policy-high", 17L),
            new IndexTermsBucket("policy-copyleft", 6L)));
    stubPolicyNames(Map.of("policy-high", "Security-High", "policy-copyleft", "License-Copyleft"));

    IndexQueryRequest req = new IndexQueryRequest("WAIVER", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.WAIVER, req);

    Map<String, Long> policy = resp.facets()
        .get("policy")
        .stream()
        .collect(java.util.stream.Collectors.toMap(
            IndexQueryResponse.IndexQueryFacetBucket::value, IndexQueryResponse.IndexQueryFacetBucket::count));
    assertThat(policy).containsEntry("policy-high", 17L).containsEntry("policy-copyleft", 6L);
    Map<String, String> policyDisplayNames = resp.facets()
        .get("policy")
        .stream()
        .collect(java.util.stream.Collectors.toMap(
            IndexQueryResponse.IndexQueryFacetBucket::value, IndexQueryResponse.IndexQueryFacetBucket::displayName));
    assertThat(policyDisplayNames)
        .containsEntry("policy-high", "Security-High")
        .containsEntry("policy-copyleft", "License-Copyleft");
  }

  @Test
  public void query_waiverApplicationsFacet_bucketsByApplicationId_wholeCorpusCounts_withResolvedDisplayNames() {
    SearchResultItemDTO a = waiverDto("w-1", "Security-High", "Checkout", "Acme");
    SearchResultItemDTO b = waiverDto("w-2", "Security-High", "Billing", "Acme");
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(a, b), 2, List.of()));
    // VALUE facet now computes via termsAggregation, bucketed on the opaque application id.
    when(session.termsAggregation(any(), eq("applicationId"), anyInt()))
        .thenReturn(List.of(
            new IndexTermsBucket("checkout-id", 9L),
            new IndexTermsBucket("billing-id", 4L)));
    stubApplicationNames(Map.of("checkout-id", "Checkout", "billing-id", "Billing"));

    IndexQueryRequest req = new IndexQueryRequest("WAIVER", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.WAIVER, req);

    Map<String, Long> apps = resp.facets()
        .get("applications")
        .stream()
        .collect(java.util.stream.Collectors.toMap(
            IndexQueryResponse.IndexQueryFacetBucket::value, IndexQueryResponse.IndexQueryFacetBucket::count));
    assertThat(apps).containsEntry("checkout-id", 9L).containsEntry("billing-id", 4L);
    Map<String, String> appDisplayNames = resp.facets()
        .get("applications")
        .stream()
        .collect(java.util.stream.Collectors.toMap(
            IndexQueryResponse.IndexQueryFacetBucket::value, IndexQueryResponse.IndexQueryFacetBucket::displayName));
    assertThat(appDisplayNames).containsEntry("checkout-id", "Checkout").containsEntry("billing-id", "Billing");
  }

  @Test
  public void query_waiverStatusFacet_countsActiveExpiringExpiredAutoWaived_wholeCorpus() {
    // Fixed clock so the active/expiring/expired epoch boundaries are deterministic.
    final java.time.Clock fixed =
        java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);
    final IndexQueryService svc =
        new IndexQueryService(organizationDAO, applicationDAO, tagDAO, policyDAO, iq, searchIndexClient,
            sessionFactory, conversionHelper, organizationSummaryService, null, fixed);
    final long now = fixed.millis();
    assertThat(IndexQueryService.STATUS_EXPIRING_WINDOW_DAYS).isEqualTo(7);
    final long windowEnd = classicExpiringWindowEnd(fixed);

    SearchResultItemDTO w = waiverDto("w-1", "APPLICATION", 5, false);
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(List.of(w), 1, List.of()));
    final String expiredRange = "policyWaiverExpiresAtEpochMs:[* TO " + now + "]";
    final String expiringRange = "policyWaiverExpiresAtEpochMs:{" + now + " TO " + windowEnd + "]";
    // active = "NOT <expiredRange>"; expired = ends with <expiredRange> but has no NOT;
    // expiring = the {now TO now+window] range; auto = policyWaiverAuto:"true". argThat disambiguates
    // active-vs-expired since both contain the expired range substring. Every expiry-derived bucket is
    // scoped to committed waivers (itemType:policy_waiver) so pending-request docs never inflate them.
    when(searchIndexClient.count(argThat(qy -> qy != null && qy.contains("NOT " + expiredRange)
        && qy.contains("itemType:policy_waiver AND NOT")))).thenReturn(80L);
    when(searchIndexClient.count(argThat(qy -> qy != null && qy.contains(expiringRange)
        && qy.contains("itemType:policy_waiver AND policyWaiverExpiresAtEpochMs:{" + now)))).thenReturn(15L);
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
  public void query_waiverLifecycleStatusFilter_appliesExpiringStatusClause() {
    final Clock fixed = Clock.fixed(Instant.parse("2026-01-01T12:34:00Z"), ZoneOffset.UTC);
    final IndexQueryService svc =
        new IndexQueryService(organizationDAO, applicationDAO, tagDAO, policyDAO, iq, searchIndexClient,
            sessionFactory, conversionHelper, organizationSummaryService, null, fixed);
    final long now = fixed.millis();
    assertThat(IndexQueryService.STATUS_EXPIRING_WINDOW_DAYS).isEqualTo(7);
    final long oldExactSevenDayEnd = Instant.parse("2026-01-08T12:34:00Z").toEpochMilli();
    final long windowEnd = classicExpiringWindowEnd(fixed);
    final long lateSeventhCalendarDayExpiry = Instant.parse("2026-01-08T23:59:00Z").toEpochMilli();
    assertThat(lateSeventhCalendarDayExpiry)
        .isGreaterThan(oldExactSevenDayEnd)
        .isLessThanOrEqualTo(windowEnd);
    final String expiringRange = "policyWaiverExpiresAtEpochMs:{" + now + " TO " + windowEnd + "]";
    final String parsedExpiringRange = "policyWaiverExpiresAtEpochMs:[" + (now + 1) + " TO " + windowEnd + "]";
    SearchResultItemDTO expiringWaiver = waiverDto("w-expiring", "APPLICATION", 5, false);

    when(searchIndexClient.searchGlobal(any())).thenAnswer(inv -> {
      GlobalSearchRequest searchRequest = inv.getArgument(0);
      if (searchRequest.baseQuery().toString().contains(parsedExpiringRange)) {
        return new GlobalSearchResult(List.of(expiringWaiver), 1, List.of());
      }
      return emptyResult();
    });
    when(searchIndexClient.count(argThat(qy -> qy != null && qy.contains(expiringRange)))).thenReturn(1L);

    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("lifecycleStatus", List.of(IndexQueryService.STATUS_EXPIRING)), 1, 25, null, null, true);
    IndexQueryResponse resp = svc.query(IndexQueryType.WAIVER, req);

    Query sent = capture();
    assertThat(sent.toString())
        .contains(parsedExpiringRange)
        .doesNotContain("policyWaiverExpiresAtEpochMs:[" + now + " TO ");
    assertThat(resp.totalEstimate()).isEqualTo(1);
    assertThat(resp.rows()).extracting(IndexQueryRow::getId).containsExactly("w-expiring");
    Map<String, Long> status = resp.facets()
        .get("status")
        .stream()
        .collect(java.util.stream.Collectors.toMap(
            IndexQueryResponse.IndexQueryFacetBucket::value, IndexQueryResponse.IndexQueryFacetBucket::count));
    assertThat(status).containsEntry(IndexQueryService.STATUS_EXPIRING, 1L);
  }

  @Test
  public void statusClauses_expiryAtNowIsExpiredOnly() {
    final Clock fixed = Clock.fixed(Instant.parse("2026-01-01T12:34:00Z"), ZoneOffset.UTC);
    final long now = fixed.millis();
    final long windowEnd = classicExpiringWindowEnd(fixed);

    Map<String, String> clauses = IndexQueryService.statusClauses(fixed);

    assertThat(clauses.get(IndexQueryService.STATUS_EXPIRED))
        .contains("policyWaiverExpiresAtEpochMs:[* TO " + now + "]");
    assertThat(clauses.get(IndexQueryService.STATUS_EXPIRING))
        .contains("policyWaiverExpiresAtEpochMs:{" + now + " TO " + windowEnd + "]")
        .doesNotContain("policyWaiverExpiresAtEpochMs:[" + now + " TO ");
  }

  @Test
  public void query_waiverPolicyTypeFacet_bucketsByDenormalizedPolicyType_wholeCorpus() {
    // A page of waivers with distinct policy types; the policyType facet buckets via termsAggregation.
    SearchResultItemDTO sec = waiverDtoWithPolicyType("w-sec", "security");
    SearchResultItemDTO lic = waiverDtoWithPolicyType("w-lic", "license");
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(List.of(sec, lic), 2, List.of()));
    // VALUE facet now computes via termsAggregation
    when(session.termsAggregation(any(), eq("policyWaiverPolicyType"), anyInt()))
        .thenReturn(List.of(
            new IndexTermsBucket("security", 40L),
            new IndexTermsBucket("license", 9L)));

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
  public void query_waiverApplicationAndPolicyIdFacets_areIdKeyedWithWholeCorpusCounts() {
    SearchResultItemDTO a = waiverDto("w-a", "Security-Critical", "Apple - Java", "Acme");
    SearchResultItemDTO b = waiverDto("w-b", "License-Banned", "Banana - Go", "Acme");
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(List.of(a, b), 2, List.of()));
    // VALUE facets now compute via termsAggregation, bucketed on the opaque application/policy id.
    when(session.termsAggregation(any(), eq("applicationId"), anyInt()))
        .thenReturn(List.of(
            new IndexTermsBucket("apple-java-id", 11L),
            new IndexTermsBucket("banana-go-id", 4L)));
    when(session.termsAggregation(any(), eq("policyWaiverPolicyId"), anyInt()))
        .thenReturn(List.of(
            new IndexTermsBucket("policy-critical", 22L),
            new IndexTermsBucket("policy-banned", 8L)));

    IndexQueryRequest req = new IndexQueryRequest("WAIVER", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.WAIVER, req);

    // The facet keys mirror the WAIVER filter keys in IndexQueryFilterSchema; the bucket value is now
    // the opaque id, which round-trips through the id-keyed applicationIds/policyIds structured filters
    // (the legacy name-keyed applications/policy filters remain supported as deprecated aliases).
    Map<String, Long> apps = resp.facets()
        .get("applications")
        .stream()
        .collect(java.util.stream.Collectors.toMap(
            IndexQueryResponse.IndexQueryFacetBucket::value, IndexQueryResponse.IndexQueryFacetBucket::count));
    Map<String, Long> policies = resp.facets()
        .get("policy")
        .stream()
        .collect(java.util.stream.Collectors.toMap(
            IndexQueryResponse.IndexQueryFacetBucket::value, IndexQueryResponse.IndexQueryFacetBucket::count));
    assertThat(apps).containsEntry("apple-java-id", 11L).containsEntry("banana-go-id", 4L);
    assertThat(policies).containsEntry("policy-critical", 22L).containsEntry("policy-banned", 8L);
  }

  @Test
  public void query_denseWaiverPage_keepsFixedFacetsWhenVariableNameFacetsAreMaxed() {
    // 20 orgs + 20 apps + 20 policy names max the variable facets. Fixed status/auto/threatLevel must
    // still be computed (they are ordered first) so the rail does not lose always-relevant sections
    // under a dense page at enterprise scale.
    List<SearchResultItemDTO> page = new java.util.ArrayList<>();
    for (int i = 0; i < 20; i++) {
      SearchResultItemDTO d = new SearchResultItemDTO();
      d.itemType = "POLICY_WAIVER";
      d.policyWaiverId = "w-" + i;
      d.policyWaiverPolicyId = "policy-" + i;
      d.policyWaiverPolicyName = "Policy " + i;
      d.policyWaiverScopeOwnerType = "APPLICATION";
      d.policyWaiverScopeOwnerId = "app-" + i;
      d.policyWaiverIsAuto = false;
      d.policyWaiverAuto = false;
      d.policyWaiverThreatLevel = i % 10;
      d.policyWaiverScope = "application";
      d.policyWaiverPolicyType = "security";
      d.applicationName = "App " + i;
      d.organizationName = "Org " + i;
      page.add(d);
    }
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(page, page.size(), List.of()));
    // Mock aggregateCountByField for NUMERIC threatLevel facet
    when(searchIndexClient.aggregateCountByField(anyString(), eq("policyWaiverThreatLevel"), any()))
        .thenReturn(new MetricAggregationResult(20L, Map.of("5", 5L, "7", 3L)));

    IndexQueryRequest req = new IndexQueryRequest("WAIVER", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.WAIVER, req);

    // COUNT fan-out no longer used for VALUE facets (termsAggregation instead).
    assertThat(resp.facets().get("status")).isNotEmpty();
    assertThat(resp.facets().get("auto")).extracting(IndexQueryResponse.IndexQueryFacetBucket::value)
        .containsExactlyInAnyOrder("true", "false");
    assertThat(resp.facets().get("threatLevel")).isNotEmpty();
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

  // =========================================
  // NUMERIC facet aggregation tests (CLM-44713)
  // =========================================

  @Test
  public void query_waiverNumericFacet_withThreatLevelFilter_noCollapse() {
    // When the request filters on threatLevel, the NUMERIC facet must not collapse to that single
    // value. Own-field clauses are removed from facetBaseQuery, so sibling values are offered.
    SearchResultItemDTO w = waiverDto("w-1", "APPLICATION", 5, false);
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(List.of(w), 1, List.of()));
    when(session.termsAggregation(any(), anyString(), anyInt())).thenReturn(List.of());
    when(searchIndexClient.aggregateCountByField(anyString(), eq("policyWaiverThreatLevel"), any()))
        .thenAnswer(inv -> {
          @SuppressWarnings("unchecked")
          Map<String, int[]> ranges = inv.getArgument(2);
          Map<String, Long> buckets = new java.util.LinkedHashMap<>();
          for (Map.Entry<String, int[]> entry : ranges.entrySet()) {
            // All values offered despite filter selecting one
            buckets.put(entry.getKey(), 1L);
          }
          return new MetricAggregationResult(11L, buckets);
        });

    IndexQueryRequest req =
        new IndexQueryRequest("WAIVER", Map.of("policyThreatLevel", List.of(5, 5)), 1, 25, null, null, true);
    IndexQueryResponse resp = service.query(IndexQueryType.WAIVER, req);

    // Multiple threat levels appear (not collapsed to just "5")
    assertThat(resp.facets().get("threatLevel").size()).isGreaterThan(1);
  }

  /**
   * Stubs both places organization names can come from, because in production they are the same rows:
   * the read gate returns {@code OrganizationDAO#getByIds} results, and the org facet takes its display
   * names from those rather than fetching by id a second time.
   */
  private void stubOrganizationNames(final Map<String, String> idToName) {
    // Lenient: the org facet reads its names from the read gate below, so a test exercising only that
    // facet never reaches the DAO. It stays stubbed for the paths that do resolve names by id.
    lenient()
        .when(organizationDAO.getByIds(any()))
        .thenAnswer(inv -> namedOrganizations(idToName.keySet(), idToName));
    when(organizationSummaryService.getOrganizationsForRead(anySet())).thenAnswer(inv -> {
      Set<String> requested = inv.getArgument(0);
      return namedOrganizations(requested, idToName);
    });
  }

  private static List<Organization> namedOrganizations(
      final Set<String> ids,
      final Map<String, String> idToName)
  {
    List<Organization> orgs = new ArrayList<>();
    for (String id : ids) {
      if (!idToName.containsKey(id)) {
        continue;
      }
      Organization org = mock(Organization.class);
      when(org.getId()).thenReturn(id);
      when(org.getName()).thenReturn(idToName.get(id));
      orgs.add(org);
    }
    return orgs;
  }

  /** Stubs {@code applicationDAO.getByIds} to resolve the given id -> name pairs (CLM-44713). */
  private void stubApplicationNames(final Map<String, String> idToName) {
    when(applicationDAO.getByIds(any())).thenAnswer(inv -> {
      List<Application> apps = new ArrayList<>();
      for (String id : idToName.keySet()) {
        Application app = mock(Application.class);
        when(app.getId()).thenReturn(id);
        when(app.getName()).thenReturn(idToName.get(id));
        apps.add(app);
      }
      return apps;
    });
  }

  /** Stubs {@code tagDAO.getByIds} to resolve the given id -> name pairs (CLM-44713). */
  private void stubTagNames(final Map<String, String> idToName) {
    when(tagDAO.getByIds(any())).thenAnswer(inv -> {
      List<Tag> tags = new ArrayList<>();
      for (String id : idToName.keySet()) {
        Tag tag = mock(Tag.class);
        when(tag.getId()).thenReturn(id);
        when(tag.getName()).thenReturn(idToName.get(id));
        tags.add(tag);
      }
      return tags;
    });
  }

  /** Stubs {@code policyDAO.getByIds} to resolve the given id -> name pairs (CLM-44713). */
  private void stubPolicyNames(final Map<String, String> idToName) {
    when(policyDAO.getByIds(any())).thenAnswer(inv -> {
      List<Policy> policies = new ArrayList<>();
      for (String id : idToName.keySet()) {
        Policy policy = mock(Policy.class);
        when(policy.getId()).thenReturn(id);
        when(policy.getName()).thenReturn(idToName.get(id));
        policies.add(policy);
      }
      return policies;
    });
  }

  private Query capture() {
    return captureRequest().baseQuery();
  }

  private GlobalSearchRequest captureRequest() {
    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient).searchGlobal(captor.capture());
    return captor.getValue();
  }

  /** The Lucene query string a search request carries, for asserting which clauses survived. */
  private static String queryOf(final GlobalSearchRequest request) {
    return request.baseQuery() == null ? "" : request.baseQuery().toString();
  }

  private static IndexQueryRequest request(final Map<String, Object> filters) {
    return new IndexQueryRequest("ANY", filters, 1, 25, null, null, false);
  }

  private static long classicExpiringWindowEnd(final Clock clock) {
    return clock.instant()
        .truncatedTo(ChronoUnit.DAYS)
        .plus(IndexQueryService.STATUS_EXPIRING_WINDOW_DAYS, ChronoUnit.DAYS)
        .plus(1, ChronoUnit.DAYS)
        .toEpochMilli();
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
