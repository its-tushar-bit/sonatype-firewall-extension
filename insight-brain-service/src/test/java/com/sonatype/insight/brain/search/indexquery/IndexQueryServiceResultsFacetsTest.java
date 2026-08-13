/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.search.global.GlobalSearchResult;
import com.sonatype.insight.brain.search.global.GlobalSearchSortAllowlist;
import com.sonatype.insight.brain.search.global.IqLocalSearchService;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.IqLocalSearchResponse;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.SearchInputs;
import com.sonatype.insight.brain.search.global.Tab;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.indexquery.IndexQueryResponse.IndexQueryFacetBucket;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests {@link IndexQueryService#facetsForResults}, the entry point the Global-Search {@code /results}
 * endpoint uses to reuse the index-query facet engine for its {@code q=}-driven query.
 */
@ExtendWith(MockitoExtension.class)
public class IndexQueryServiceResultsFacetsTest
{
  @Mock
  private SearchIndexClient searchIndexClient;

  private IndexQueryService service;

  @BeforeEach
  public void setUp() {
    IqLocalSearchService iq = new IqLocalSearchService(searchIndexClient);
    service = new IndexQueryService(iq, searchIndexClient, null);

    lenient().when(searchIndexClient.isSearchPreviewEnabled()).thenReturn(true);
    lenient().when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of());
    lenient().when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenReturn(null);
    lenient().when(searchIndexClient.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    lenient().when(searchIndexClient.buildPermittedQuery(any())).thenCallRealMethod();
    lenient().when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(List.of(), 0, List.of()));
    lenient().when(searchIndexClient.count(any())).thenReturn(0L);
  }

  @Test
  public void allTab_returnsNullFacets() {
    assertThat(service.facetsForResults(Tab.ALL, "log4j", new ArrayList<>())).isNull();
  }

  @Test
  public void componentTab_hasNoFacetSet_returnsNull() {
    assertThat(service.facetsForResults(Tab.COMPONENT, "jackson", new ArrayList<>())).isNull();
  }

  @Test
  public void vulnerabilityTab_hasNoFacetSet_returnsNull() {
    // VULNERABILITY carries no FACET_FIELDS entry today; note the prototype sample also had 0 vuln rows.
    assertThat(service.facetsForResults(Tab.VULNERABILITY, "CVE", new ArrayList<>())).isNull();
  }

  @Test
  public void applicationsTab_returnsApplicationFacetSet() {
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(appDto("acme-1", "App One", "Acme")), 1, List.of()));

    Map<String, List<IndexQueryFacetBucket>> facets =
        service.facetsForResults(Tab.APPLICATION, "app", new ArrayList<>());

    assertThat(facets).isNotNull();
    // FACET_FIELDS[APPLICATION] = stages, policyTypes, violationStates, organizations, applications,
    // applicationCategories.
    assertThat(facets.keySet()).containsExactly("stages", "policyTypes", "violationStates", "organizations",
        "applications", "applicationCategories");
  }

  @Test
  public void violationsTab_returnsViolationFacetSet_withFixedStates() {
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(violationDto("pv-1", "Acme", "SECURITY", "build")), 1, List.of()));

    Map<String, List<IndexQueryFacetBucket>> facets =
        service.facetsForResults(Tab.VIOLATION, "log4j", new ArrayList<>());

    assertThat(facets).isNotNull();
    // FACET_FIELDS[VIOLATION]: states, waiverType, stages, policyTypes, organizations, applications,
    // applicationCategories.
    assertThat(facets.keySet())
        .containsExactly("states", "waiverType", "stages", "policyTypes", "organizations", "applications",
            "applicationCategories");
    assertThat(facets.get("states")).extracting(IndexQueryFacetBucket::value)
        .containsExactlyInAnyOrder("OPEN", "WAIVED");
  }

  @Test
  public void waiversTab_returnsWaiverFacetSet() {
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(waiverDto("w-1")), 1, List.of()));

    Map<String, List<IndexQueryFacetBucket>> facets =
        service.facetsForResults(Tab.WAIVER, "security", new ArrayList<>());

    assertThat(facets).isNotNull();
    // Every facet in FACET_FIELDS[WAIVER] is emitted, in declaration order, including ones whose bucket
    // list is empty for this query.
    assertThat(facets.keySet()).containsExactly(
        "status", "auto", "threatLevel", "scope", "policyType", "policy", "organizationName",
        "applications");
  }

  @Test
  public void violationsTab_freeTextOnly_stillReturnsWholeCorpusBuckets() {
    // The guard scenario: a q with no field:value chips must still return the tab's facet buckets
    // (counted against the whole-corpus item-type base) rather than erroring or returning nothing.
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(violationDto("pv-1", "Acme", "SECURITY", "build")), 1, List.of()));
    // Facet VALUES are seeded from the row's policyType field ("SECURITY") and counted verbatim, so the
    // count query carries the un-lowercased value.
    when(searchIndexClient.count(contains("policyViolationThreatCategory:\"SECURITY\""))).thenReturn(7L);

    Map<String, List<IndexQueryFacetBucket>> facets =
        service.facetsForResults(Tab.VIOLATION, "log4j", new ArrayList<>());

    assertThat(facets.get("states")).extracting(IndexQueryFacetBucket::value)
        .containsExactlyInAnyOrder("OPEN", "WAIVED");
    Map<String, Long> policyTypes = facets.get("policyTypes")
        .stream()
        .collect(Collectors.toMap(IndexQueryFacetBucket::value, IndexQueryFacetBucket::count));
    assertThat(policyTypes).containsEntry("SECURITY", 7L);
  }

  @Test
  public void violationCounts_spanBothPolicyAndLegalViolationItemTypes() {
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(violationDto("pv-1", "Acme", "SECURITY", "build")), 1, List.of()));

    service.facetsForResults(Tab.VIOLATION, "policyViolationThreatCategory:security", new ArrayList<>());

    ArgumentCaptor<String> countQueries = ArgumentCaptor.forClass(String.class);
    verify(searchIndexClient, atLeastOnce()).count(countQueries.capture());
    // VIOLATION unions POLICY_VIOLATION + LEGAL_VIOLATION, so the item-type clause in the count base is
    // an OR of both search-field tokens.
    assertThat(countQueries.getAllValues())
        .anySatisfy(qy -> assertThat(qy).contains("policy_violation").contains("legal_violation"));
  }

  @Test
  public void violationsTab_seedQueryUsesRelevanceSort_notEntityDefaultThreatSort() {
    // Regression: the facet-seed page query must sort by RELEVANCE, never the per-tab default sort.
    // The VIOLATION default "threat" sort builds an 8-byte LONG SortedNumericSortField over the
    // 4-byte-indexed policyViolationThreatLevel point, which throws (HTTP 500) once the index holds
    // real violation docs. Facet VALUES are an unordered set, so relevance is both correct and safe.
    IqLocalSearchService mockIq = org.mockito.Mockito.mock(IqLocalSearchService.class);
    ArgumentCaptor<SearchInputs> inputs = ArgumentCaptor.forClass(SearchInputs.class);
    when(mockIq.search(inputs.capture()))
        .thenReturn(new IqLocalSearchResponse(List.of(), 0L, true, List.of(), GlobalSearchSortAllowlist.RELEVANCE));
    IndexQueryService svc = new IndexQueryService(mockIq, searchIndexClient, null);

    svc.facetsForResults(Tab.VIOLATION, "log4j", new ArrayList<>());

    assertThat(inputs.getValue().sortKey()).isEqualTo(GlobalSearchSortAllowlist.RELEVANCE);
  }

  @Test
  public void violationsTab_wholeCorpusCount_roundTripsThroughPolicyTypeFilter() {
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(violationDto("pv-1", "Acme", "SECURITY", "build")), 1, List.of()));
    // The bucket value is the row's policyType ("SECURITY"); the whole-corpus count query carries it verbatim.
    when(searchIndexClient.count(contains("policyViolationThreatCategory:\"SECURITY\""))).thenReturn(42L);

    Map<String, List<IndexQueryFacetBucket>> facets =
        service.facetsForResults(Tab.VIOLATION, "policyViolationThreatCategory:security", new ArrayList<>());

    long security = facets.get("policyTypes")
        .stream()
        .filter(b -> b.value().equals("SECURITY"))
        .findFirst()
        .orElseThrow()
        .count();
    assertThat(security).isEqualTo(42L);
  }

  @Test
  public void facetedRequest_issuesExactlyOneSeedSearch() {
    // The facet map costs one seed search per faceted request on top of the caller's page search. Pin
    // that at one so a future change cannot fan out into a search per facet key.
    IqLocalSearchService mockIq = org.mockito.Mockito.mock(IqLocalSearchService.class);
    when(mockIq.search(any()))
        .thenReturn(new IqLocalSearchResponse(List.of(), 0L, true, List.of(), GlobalSearchSortAllowlist.RELEVANCE));
    IndexQueryService svc = new IndexQueryService(mockIq, searchIndexClient, null);

    svc.facetsForResults(Tab.VIOLATION, "log4j", new ArrayList<>());

    verify(mockIq, times(1)).search(any());
  }

  @Test
  public void waiverFacetValues_comeFromTheSeedRowShape_notThePageRowShape() {
    // Facet VALUES are seeded via distinctRowValues over the SEED rows, which IndexQueryRowMapper builds
    // with the full waiver field bag. The /results page row shape carries only policyId and ownerId, so
    // seeding from page rows would leave every one of these value facets empty.
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(waiverDto("w-1")), 1, List.of()));
    when(searchIndexClient.count(any())).thenReturn(4L);

    Map<String, List<IndexQueryFacetBucket>> facets =
        service.facetsForResults(Tab.WAIVER, "security", new ArrayList<>());

    assertThat(facets.get("scope")).extracting(IndexQueryFacetBucket::value).containsExactly("application");
    assertThat(facets.get("policyType")).extracting(IndexQueryFacetBucket::value).containsExactly("SECURITY");
    assertThat(facets.get("organizationName")).extracting(IndexQueryFacetBucket::value).containsExactly("Acme");
    assertThat(facets.get("policy")).extracting(IndexQueryFacetBucket::value).containsExactly("Some Policy");
    assertThat(facets.get("threatLevel")).isNotEmpty();
    // Counts stay whole-corpus: they come from count() queries, not from the seed rows.
    assertThat(facets.get("scope")).extracting(IndexQueryFacetBucket::count).containsExactly(4L);
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

  private static SearchResultItemDTO violationDto(
      final String id,
      final String org,
      final String threatCategory,
      final String stage)
  {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "POLICY_VIOLATION";
    d.policyViolationId = id;
    d.policyViolationPolicyName = "Some Policy";
    d.policyViolationPolicyId = "policy-1";
    d.policyViolationThreatCategory = threatCategory;
    d.policyEvaluationStage = stage;
    d.policyViolationWaiverStatus = "Active";
    d.applicationName = "App One";
    d.applicationId = "app-1";
    d.organizationName = org;
    d.organizationId = org.toLowerCase(java.util.Locale.ROOT);
    return d;
  }

  private static SearchResultItemDTO waiverDto(final String id) {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "POLICY_WAIVER";
    d.policyWaiverId = id;
    d.policyWaiverPolicyId = "policy-1";
    d.policyWaiverPolicyName = "Some Policy";
    d.policyWaiverPolicyType = "SECURITY";
    d.policyWaiverScope = "application";
    d.policyWaiverScopeOwnerType = "APPLICATION";
    d.policyWaiverScopeOwnerId = "app-1";
    d.policyWaiverThreatLevel = 7;
    d.policyWaiverAuto = false;
    d.organizationName = "Acme";
    return d;
  }
}
