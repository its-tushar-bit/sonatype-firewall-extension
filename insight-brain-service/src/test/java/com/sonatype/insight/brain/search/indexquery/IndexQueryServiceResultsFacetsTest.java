/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.integration.OrganizationSummaryService;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.security.SecurityAspectControl;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.global.GlobalSearchResult;
import com.sonatype.insight.brain.search.global.IqLocalSearchService;
import com.sonatype.insight.brain.search.global.Tab;
import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.indexquery.IndexQueryResponse.IndexQueryFacetBucket;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexReadSessionFactory;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Tests {@link IndexQueryService#facetsForResults}, the entry point the Global-Search {@code /results}
 * endpoint uses to reuse the index-query facet engine for its {@code q=}-driven query.
 */
// Facet counts come from single-pass session aggregation, so several per-test searchGlobal stubs are
// only relevant to the tests that assert page-shaped behavior; lenient strictness avoids duplicating a
// bespoke setup per test. These tests are tracked for real-index conversion (CLM-45220).
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class IndexQueryServiceResultsFacetsTest
{
  @Mock
  private SearchIndexClient searchIndexClient;

  @Mock
  private IndexReadSessionFactory sessionFactory;

  @Mock
  private IndexReadSession session;

  @Mock
  private ConversionHelper conversionHelper;

  @Mock
  private OrganizationSummaryService organizationSummaryService;

  private IndexQueryService service;

  @BeforeEach
  public void setUp() {
    OrganizationDAO organizationDAO = mock(OrganizationDAO.class);

    lenient().when(organizationSummaryService.getOrganizationsForRead(anySet())).thenAnswer(inv -> {
      Set<String> ids = inv.getArgument(0);
      return ids.stream().map(id -> {
        Organization o = new Organization();
        o.setId(id);
        return o;
      }).toList();
    });

    IqLocalSearchService iq = new IqLocalSearchService(searchIndexClient);
    service = new IndexQueryService(organizationDAO, mock(ApplicationDAO.class), mock(TagDAO.class),
        mock(PolicyDAO.class), iq, searchIndexClient, sessionFactory, conversionHelper, organizationSummaryService,
        null);

    lenient().when(searchIndexClient.isSearchPreviewEnabled()).thenReturn(true);
    lenient().when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of());
    lenient().when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenReturn(null);
    lenient().when(searchIndexClient.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    lenient().when(searchIndexClient.buildPermittedQuery(any())).thenCallRealMethod();
    lenient().when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(List.of(), 0, List.of()));
    lenient().when(searchIndexClient.count(any())).thenReturn(0L);
    lenient().when(sessionFactory.open()).thenReturn(session);
    lenient().when(conversionHelper.stringToQuery(anyString())).thenReturn(new MatchAllDocsQuery());
    // Default stub for the NUMERIC threatLevel facet, used by WAIVER queries.
    lenient().when(searchIndexClient.aggregateCountByField(anyString(), eq("policyWaiverThreatLevel"), any()))
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

  /**
   * The filter rail enriches the results page, so losing it must not lose the page. A facet failure is
   * reported through the warnings the caller merges into the response, not by propagating.
   */
  @Test
  public void facetBuildFailure_reportsAWarningInsteadOfFailingTheResultsPage() {
    when(sessionFactory.open()).thenThrow(new IllegalStateException("index unavailable"));
    List<String> warnings = new ArrayList<>();

    Map<String, List<IndexQueryFacetBucket>> facets =
        service.facetsForResults(Tab.APPLICATION, "app", warnings);

    assertThat(facets).isEmpty();
    assertThat(warnings).contains(IndexQueryService.FACETS_UNAVAILABLE);
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
        "status", "auto", "threatLevel", "scope", "policyType", "policy", "organizations",
        "applications");
  }

  @Test
  public void violationsTab_freeTextOnly_stillReturnsWholeCorpusBuckets() {
    // The guard scenario: a q with no field:value chips must still return the tab's facet buckets
    // via termsAggregation.
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(violationDto("pv-1", "Acme", "SECURITY", "build")), 1, List.of()));
    // VALUE facet now computes via termsAggregation
    when(session.termsAggregation(any(), eq("policyViolationThreatCategory"), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("SECURITY", 7L)));

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
  public void violationsTab_wholeCorpusCount_roundTripsThroughPolicyTypeFilter() {
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(violationDto("pv-1", "Acme", "SECURITY", "build")), 1, List.of()));
    // VALUE facet now computes via termsAggregation
    when(session.termsAggregation(any(), eq("policyViolationThreatCategory"), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("SECURITY", 42L)));

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
  public void waiverFacetValues_areWholeCorpus_notLimitedToThePageRows() {
    // Facet values come from an aggregation over the session, so they are not bounded by what the page
    // happens to contain.
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(waiverDto("w-1")), 1, List.of()));
    // organizations/policy aggregate on the opaque id field; with no OrganizationDAO/PolicyDAO stub the
    // display name simply is not resolved, so the bucket value (the id) is what the assertion below sees.
    when(session.termsAggregation(any(), eq("policyWaiverScope"), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("application", 4L)));
    when(session.termsAggregation(any(), eq("policyWaiverPolicyType"), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("SECURITY", 1L)));
    when(session.termsAggregation(any(), eq("parentOrganizationId"), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("acme-id", 1L)));
    when(session.termsAggregation(any(), eq("policyWaiverPolicyId"), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("policy-1", 1L)));
    // NUMERIC facet uses aggregateCountByField
    when(searchIndexClient.aggregateCountByField(anyString(), eq("policyWaiverThreatLevel"), any()))
        .thenReturn(new MetricAggregationResult(1L, Map.of("7", 1L)));

    Map<String, List<IndexQueryFacetBucket>> facets =
        service.facetsForResults(Tab.WAIVER, "security", new ArrayList<>());

    assertThat(facets.get("scope")).extracting(IndexQueryFacetBucket::value).containsExactly("application");
    assertThat(facets.get("policyType")).extracting(IndexQueryFacetBucket::value).containsExactly("SECURITY");
    assertThat(facets.get("organizations")).extracting(IndexQueryFacetBucket::value).containsExactly("acme-id");
    assertThat(facets.get("policy")).extracting(IndexQueryFacetBucket::value).containsExactly("policy-1");
    assertThat(facets.get("threatLevel")).isNotEmpty();
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
