/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.integration.ApplicationSummaryService;
import com.sonatype.insight.brain.integration.OrganizationSummaryService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.security.SecurityAspectControl;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexFilterRestriction;
import com.sonatype.insight.brain.search.index.IndexTermSetRestriction;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Violations list facet builder with a mocked {@link SearchIndexClient}. Covers the
 * state/threat/stage vocabularies, the org/app discovery maps, the per-dimension count cap, and the
 * root-organization skip branch — none of which were asserted directly before (CLM-42254 review).
 */
@RunWith(MockitoJUnitRunner.class)
public class ViolationsListFacetsBuilderTest
{
  private static final String QUERY = "itemType:POLICY_VIOLATION";

  @Mock
  private SearchIndexClient searchIndexClient;

  @Mock
  private StageTypeService stageTypeService;

  @Mock
  private DashboardIndexDimensionQueryBuilder dimensionQueryBuilder;

  @Mock
  private ConversionHelper conversionHelper;

  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private ApplicationDAO applicationDAO;

  @Mock
  private OrganizationSummaryService organizationSummaryService;

  @Mock
  private ApplicationSummaryService applicationSummaryService;

  @Mock
  private IndexReadSession session;

  private final Query sessionQuery = new MatchAllDocsQuery();

  private ViolationsListFacetsBuilder builder() {
    return new ViolationsListFacetsBuilder(
        searchIndexClient,
        stageTypeService,
        dimensionQueryBuilder,
        conversionHelper,
        organizationDAO,
        applicationDAO,
        organizationSummaryService,
        applicationSummaryService);
  }

  @Before
  public void stubSessionQueryConversion() {
    // Call-site CTW still advises mocked @AuthzFilter methods; disable Shiro for this unit test.
    SecurityAspectControl.disableEnforcement();
    lenient().when(conversionHelper.stringToQuery(anyString())).thenReturn(sessionQuery);
    lenient().when(organizationDAO.getByIds(any())).thenReturn(List.of());
    lenient().when(applicationDAO.getByIds(any())).thenReturn(List.of());
    // Default: every searched owner is READ-visible (individual tests override for RBAC cases).
    lenient().when(organizationSummaryService.getOrganizationsForRead(anySet())).thenReturn(List.of());
    lenient().when(applicationSummaryService.getApplicationsForRead(isNull(), anySet()))
        .thenReturn(List.of());
  }

  @After
  public void restoreSecurityAspectEnforcement() {
    SecurityAspectControl.enableEnforcement();
  }

  private void stubReadableOrgs(final Organization... organizations) {
    when(organizationSummaryService.getOrganizationsForRead(anySet())).thenReturn(List.of(organizations));
  }

  private void stubReadableApps(final Application... applications) {
    when(applicationSummaryService.getApplicationsForRead(isNull(), anySet()))
        .thenReturn(List.of(applications));
  }

  private static Organization org(final String id, final String name) {
    Organization organization = mock(Organization.class);
    when(organization.getId()).thenReturn(id);
    when(organization.getName()).thenReturn(name);
    return organization;
  }

  private static Application app(final String id, final String publicId, final String name) {
    Application application = mock(Application.class);
    when(application.getId()).thenReturn(id);
    when(application.getPublicId()).thenReturn(publicId);
    when(application.getName()).thenReturn(name);
    return application;
  }

  private void stubEmptyDiscovery() {
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(),
        anyList()))
            .thenReturn(new SearchResultDTO());
  }

  private static SearchResultItemDTO item(final String applicationId, final String organizationId) {
    SearchResultItemDTO item = new SearchResultItemDTO();
    item.itemType = ItemType.POLICY_VIOLATION.name();
    item.applicationId = applicationId;
    item.organizationId = organizationId;
    return item;
  }

  private static SearchResultDTO resultWith(final List<SearchResultItemDTO> items) {
    SearchResultDTO result = new SearchResultDTO();
    GroupingByDTO group = new GroupingByDTO();
    group.searchResultItemDTOS = new ArrayList<>(items);
    result.groupingByDTOS = new ArrayList<>(List.of(group));
    return result;
  }

  @Test
  public void buildFacets_zeroTotal_shortCircuitsWithNoQueries() {
    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, 0);

    assertThat(facets.totalViolations).isZero();
    assertThat(facets.states).isNull();
    assertThat(facets.waiverTypes).isNull();
    assertThat(facets.threatCategories).isNull();
    assertThat(facets.stages).isNull();
    assertThat(facets.organizations).isNull();
    assertThat(facets.applications).isNull();
  }

  @Test
  public void buildFacets_openExcludesLegacyAndWaived_legacyCountedSeparately() {
    stubEmptyDiscovery();
    // OPEN is the complement of the shared excluded set (Waived AutoWaived Legacy) — it MUST exclude
    // Legacy or Legacy leaks into OPEN. WAIVED counts the combined ":(Waived AutoWaived)" clause;
    // LEGACY counts its own ":(Legacy)" clause. The "!NOT" guard keeps the WAIVED/LEGACY matchers from
    // also matching the OPEN "AND NOT (...)" query, and the WAIVED matcher's "Waived AutoWaived" body is
    // distinct from the LEGACY ":(Legacy)" clause.
    when(searchIndexClient.count(argThat(q -> q != null
        && q.contains("AND NOT (policyViolationWaiverStatus:(Waived AutoWaived Legacy))")), anyList()))
            .thenReturn(7L);
    when(searchIndexClient.count(argThat(q -> q != null
        && q.contains("policyViolationWaiverStatus:(Waived AutoWaived)") && !q.contains("NOT")), anyList()))
            .thenReturn(3L);
    when(searchIndexClient.count(argThat(q -> q != null
        && q.contains("policyViolationWaiverStatus:(Legacy)") && !q.contains("NOT")), anyList()))
            .thenReturn(2L);

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, 12);

    assertThat(facets.states)
        .containsEntry("OPEN", 7L)
        .containsEntry("WAIVED", 3L)
        .containsEntry("LEGACY_VIOLATION", 2L);
  }

  @Test
  public void buildFacets_legacyCountOmittedWhenZero() {
    stubEmptyDiscovery();
    when(searchIndexClient.count(argThat(q -> q != null && q.contains("AND NOT ")), anyList())).thenReturn(7L);
    when(searchIndexClient.count(argThat(q -> q != null
        && q.contains("policyViolationWaiverStatus:(Waived AutoWaived)") && !q.contains("NOT")), anyList()))
            .thenReturn(3L);
    // Legacy count falls through to Mockito's 0L default, so the LEGACY key is omitted.

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, 10);

    assertThat(facets.states)
        .containsEntry("OPEN", 7L)
        .containsEntry("WAIVED", 3L)
        .doesNotContainKey("LEGACY_VIOLATION");
  }

  @Test
  public void buildFacets_waiverTypeCounts_autoAndManual() {
    stubEmptyDiscovery();
    when(searchIndexClient.count(argThat(q -> q != null && q.contains("policyViolationWaiverStatus:(AutoWaived)")),
        anyList()))
            .thenReturn(4L);
    when(searchIndexClient.count(argThat(q -> q != null && q.contains("policyViolationWaiverStatus:(Waived)")),
        anyList()))
            .thenReturn(2L);

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, 10);

    assertThat(facets.waiverTypes)
        .containsEntry(ViolationsListFacetsBuilder.WAIVER_TYPE_AUTO, 4L)
        .containsEntry(ViolationsListFacetsBuilder.WAIVER_TYPE_MANUAL, 2L);
    // Only the waiver-type count() calls are stubbed; the state queries (OPEN "NOT waived" and the
    // combined ":(Waived AutoWaived)" WAIVED count) fall through to Mockito's 0L default, so states is
    // omitted. Matchers stay unambiguous: ":(AutoWaived)"/":(Waived)" do not appear in the combined
    // ":(Waived AutoWaived)" state clause, so the waiver facet is counted independently of the states.
    assertThat(facets.states).isNull();
  }

  @Test
  public void buildFacets_waiverTypeFacet_countedAgainstWaiverExcludedQuery() {
    stubEmptyDiscovery();
    // Simulate an active AUTO waiver-type filter: the list query carries the waiver clause; the
    // waiver-facet query does not. The single-select waiver-type facet must count against the
    // waiver-excluded query so MANUAL still shows its switchable count instead of collapsing to 0
    // under the AUTO-narrowed list query (mealingr review, CLM-42261).
    String listQuery = "LIST_QUERY AND policyViolationWaiverStatus:(AutoWaived)";
    String waiverFacetQuery = "WAIVER_FACET_QUERY";
    when(searchIndexClient.count(argThat(q -> q != null && q.startsWith(waiverFacetQuery)
        && q.contains("policyViolationWaiverStatus:(AutoWaived)")), anyList())).thenReturn(4L);
    when(searchIndexClient.count(argThat(q -> q != null && q.startsWith(waiverFacetQuery)
        && q.contains("policyViolationWaiverStatus:(Waived)")), anyList())).thenReturn(2L);

    ViolationsListFacetsDTO facets = builder().buildFacets(listQuery, waiverFacetQuery, 10);

    // Both options are present even though the list query is AUTO-narrowed, proving the facet used the
    // waiver-excluded query (counts keyed off the "WAIVER_FACET_QUERY" prefix, not "LIST_QUERY").
    assertThat(facets.waiverTypes)
        .containsEntry(ViolationsListFacetsBuilder.WAIVER_TYPE_AUTO, 4L)
        .containsEntry(ViolationsListFacetsBuilder.WAIVER_TYPE_MANUAL, 2L);
  }

  @Test
  public void buildFacets_threatCategoryCounts_omitZeroCounts() {
    stubEmptyDiscovery();
    String security = PolicyThreatCategory.SECURITY.getName();
    when(searchIndexClient
        .count(argThat(q -> q != null && q.contains("policyViolationThreatCategory:(" + security + ")")), anyList()))
            .thenReturn(5L);

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, 10);

    assertThat(facets.threatCategories).containsEntry(security, 5L);
  }

  @Test
  public void buildFacets_stageCounts_fromLicensedStages() {
    StageType buildStage = mock(StageType.class);
    when(buildStage.getId()).thenReturn("build");
    when(stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)).thenReturn(List.of(buildStage));
    stubEmptyDiscovery();
    when(searchIndexClient.count(argThat(q -> q != null && q.contains("policyEvaluationStage:(build)")), anyList()))
        .thenReturn(4L);

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, 10);

    assertThat(facets.stages).containsEntry("build", 4L);
  }

  @Test
  public void buildFacets_organizationAndApplicationMaps_skipRootOrg() {
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(),
        anyList()))
            .thenReturn(resultWith(List.of(
                item("app-1", "org-A"),
                item("app-2", Organization.ROOT_ORGANIZATION_ID))));

    // Root is omitted from the expanded id map (same skip semantics as a null single-id clause).
    when(dimensionQueryBuilder.expandOrganizationFilterIdsById(any()))
        .thenReturn(Map.of("org-A", Set.of("org-A")));

    when(searchIndexClient.count(anyString(), anyList())).thenAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      List<IndexFilterRestriction> restrictions = invocation.getArgument(1);
      boolean hasOrgA = restrictions.stream()
          .filter(r -> r instanceof IndexTermSetRestriction)
          .map(r -> (IndexTermSetRestriction) r)
          .anyMatch(r -> r.field().equals(FieldIdentifier.ORGANIZATION_ID.label) && r.ids().contains("org-A"));
      boolean hasApp1 = restrictions.stream()
          .filter(r -> r instanceof IndexTermSetRestriction)
          .map(r -> (IndexTermSetRestriction) r)
          .anyMatch(r -> r.field().equals(FieldIdentifier.APPLICATION_ID.label) && r.ids().contains("app-1"));
      boolean hasApp2 = restrictions.stream()
          .filter(r -> r instanceof IndexTermSetRestriction)
          .map(r -> (IndexTermSetRestriction) r)
          .anyMatch(r -> r.field().equals(FieldIdentifier.APPLICATION_ID.label) && r.ids().contains("app-2"));
      if (hasOrgA) {
        return 5L;
      }
      if (hasApp1) {
        return 3L;
      }
      if (hasApp2) {
        return 2L;
      }
      return 0L;
    });

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, 10);

    assertThat(facets.organizations).containsOnlyKeys("org-A").containsEntry("org-A", 5L);
    assertThat(facets.applications)
        .containsEntry("app-1", 3L)
        .containsEntry("app-2", 2L);
  }

  @Test
  public void buildFacets_applicationCount_isCapped() {
    List<SearchResultItemDTO> items = new ArrayList<>();
    for (int i = 0; i < ViolationsListFacetsBuilder.MAX_APPLICATION_FACETS + 5; i++) {
      items.add(item("app-" + i, "org-A"));
    }
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(),
        anyList()))
            .thenReturn(resultWith(items));
    when(dimensionQueryBuilder.expandOrganizationFilterIdsById(any()))
        .thenReturn(Map.of("org-A", Set.of("org-A")));
    when(searchIndexClient.count(anyString(), anyList())).thenReturn(1L);

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, 100);

    assertThat(facets.applications).hasSize(ViolationsListFacetsBuilder.MAX_APPLICATION_FACETS);
  }

  @Test
  public void buildFacets_session_zeroTotal_shortCircuitsWithNoSessionQueries() {
    ViolationsListFacetsDTO facets = builder().buildFacets(session, QUERY, QUERY, 0, List.of());

    assertThat(facets.totalViolations).isZero();
    assertThat(facets.states).isNull();
    assertThat(facets.organizations).isNull();
    verify(session, never()).count(any(Query.class));
    verify(session, never()).termsAggregation(any(Query.class), anyString(), anyInt());
  }

  @Test
  public void buildFacets_session_countsStatesWaiverThreatStageAndTermsFacets() {
    StageType buildStage = mock(StageType.class);
    when(buildStage.getId()).thenReturn("build");
    when(stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)).thenReturn(List.of(buildStage));
    // Same Query stub for every stringToQuery; consecutive counts share one positive value.
    when(session.count(any(Query.class))).thenReturn(5L);
    when(session.termsAggregation(eq(sessionQuery), eq(FieldIdentifier.ORGANIZATION_ID.label), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("org-A", 5L), new IndexTermsBucket("org-B", 0L)));
    when(session.termsAggregation(eq(sessionQuery), eq(FieldIdentifier.APPLICATION_ID.label), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("app-1", 3L), new IndexTermsBucket("", 9L)));

    ViolationsListFacetsDTO facets = builder().buildFacets(session, QUERY, QUERY, 10, List.of());

    assertThat(facets.totalViolations).isEqualTo(10);
    assertThat(facets.states).containsEntry("OPEN", 5L).containsEntry("WAIVED", 5L);
    assertThat(facets.waiverTypes)
        .containsEntry(ViolationsListFacetsBuilder.WAIVER_TYPE_AUTO, 5L)
        .containsEntry(ViolationsListFacetsBuilder.WAIVER_TYPE_MANUAL, 5L);
    assertThat(facets.threatCategories).containsKeys(PolicyThreatCategory.SECURITY.getName());
    assertThat(facets.stages).containsEntry("build", 5L);
    assertThat(facets.organizations).containsOnlyKeys("org-A").containsEntry("org-A", 5L);
    assertThat(facets.applications).containsOnlyKeys("app-1").containsEntry("app-1", 3L);
  }

  @Test
  public void buildFacets_session_waiverTypeFacet_countedAgainstWaiverExcludedQuery() {
    // Session-path counterpart to buildFacets_waiverTypeFacet_countedAgainstWaiverExcludedQuery:
    // with an AUTO-narrowed list query, waiver facet counts must still come from waiverFacetQuery.
    String listQuery = "LIST_QUERY AND policyViolationWaiverStatus:(AutoWaived)";
    String waiverFacetQuery = "WAIVER_FACET_QUERY";
    Query waiverAutoQuery = mock(Query.class);
    Query waiverManualQuery = mock(Query.class);

    when(conversionHelper.stringToQuery(argThat(q -> q != null && q.startsWith(waiverFacetQuery)
        && q.contains("policyViolationWaiverStatus:(AutoWaived)")))).thenReturn(waiverAutoQuery);
    when(conversionHelper.stringToQuery(argThat(q -> q != null && q.startsWith(waiverFacetQuery)
        && q.contains("policyViolationWaiverStatus:(Waived)")))).thenReturn(waiverManualQuery);
    when(session.count(waiverAutoQuery)).thenReturn(4L);
    when(session.count(waiverManualQuery)).thenReturn(2L);
    when(session.termsAggregation(any(Query.class), anyString(), anyInt())).thenReturn(List.of());

    ViolationsListFacetsDTO facets = builder().buildFacets(session, listQuery, waiverFacetQuery, 10, List.of());

    assertThat(facets.waiverTypes)
        .containsEntry(ViolationsListFacetsBuilder.WAIVER_TYPE_AUTO, 4L)
        .containsEntry(ViolationsListFacetsBuilder.WAIVER_TYPE_MANUAL, 2L);
    verify(session).count(waiverAutoQuery);
    verify(session).count(waiverManualQuery);
  }

  @Test
  public void buildFacets_session_termsAggregation_nullBucketsYieldNullMaps() {
    when(session.count(any(Query.class))).thenReturn(0L);
    when(session.termsAggregation(any(Query.class), anyString(), anyInt())).thenReturn(null);

    ViolationsListFacetsDTO facets = builder().buildFacets(session, QUERY, QUERY, 1, List.of());

    assertThat(facets.organizations).isNull();
    assertThat(facets.applications).isNull();
  }

  @Test
  public void buildFacets_session_attachesFriendlyOwnerNamesFromDao() {
    when(session.count(any(Query.class))).thenReturn(1L);
    when(session.termsAggregation(eq(sessionQuery), eq(FieldIdentifier.ORGANIZATION_ID.label), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("org-A", 5L)));
    when(session.termsAggregation(eq(sessionQuery), eq(FieldIdentifier.APPLICATION_ID.label), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("app-1", 3L)));

    Organization organization = mock(Organization.class);
    when(organization.getId()).thenReturn("org-A");
    when(organization.getName()).thenReturn("Java-team");
    Application application = mock(Application.class);
    when(application.getId()).thenReturn("app-1");
    when(application.getName()).thenReturn("Apple - Java");
    when(organizationDAO.getByIds(any())).thenReturn(List.of(organization));
    when(applicationDAO.getByIds(any())).thenReturn(List.of(application));

    ViolationsListFacetsDTO facets = builder().buildFacets(session, QUERY, QUERY, 10, List.of());

    assertThat(facets.organizationNames).containsEntry("org-A", "Java-team");
    assertThat(facets.applicationNames).containsEntry("app-1", "Apple - Java");
  }

  @Test
  public void buildFacets_session_ownerLabelDaoFailure_propagates() {
    when(session.count(any(Query.class))).thenReturn(1L);
    when(session.termsAggregation(eq(sessionQuery), eq(FieldIdentifier.ORGANIZATION_ID.label), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("org-A", 5L)));
    when(session.termsAggregation(eq(sessionQuery), eq(FieldIdentifier.APPLICATION_ID.label), anyInt()))
        .thenReturn(List.of());
    when(organizationDAO.getByIds(any())).thenThrow(new RuntimeException("db unavailable"));

    assertThatThrownBy(() -> builder().buildFacets(session, QUERY, QUERY, 10, List.of()))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("db unavailable");
  }

  @Test
  public void buildFacets_legacy_prefersDiscoveryNamesOverDao() {
    SearchResultItemDTO discovered = item("app-1", "org-A");
    discovered.organizationName = "From-Index";
    discovered.applicationName = "From-Index-App";
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(),
        anyList()))
            .thenReturn(resultWith(List.of(discovered)));
    when(dimensionQueryBuilder.expandOrganizationFilterIdsById(Set.of("org-A")))
        .thenReturn(Map.of("org-A", Set.of("org-A")));
    when(searchIndexClient.count(anyString(), anyList())).thenReturn(1L);

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, 1);

    assertThat(facets.organizationNames).containsEntry("org-A", "From-Index");
    assertThat(facets.applicationNames).containsEntry("app-1", "From-Index-App");
    verify(organizationDAO, never()).getByIds(any());
    verify(applicationDAO, never()).getByIds(any());
  }

  @Test
  public void buildFacets_organizationFacetSearch_replacesTopByCountMap() {
    Organization match = org("org-zeta", "Zeta Finance");
    stubReadableOrgs(match);
    when(organizationDAO.searchByNameSubstring(
        "zeta", ViolationsListFacetsBuilder.MAX_ORGANIZATION_FACET_SEARCH_CANDIDATES))
            .thenReturn(List.of(match));
    when(dimensionQueryBuilder.expandOrganizationFilterIdsById(Set.of("org-zeta")))
        .thenReturn(Map.of("org-zeta", Set.of("org-zeta")));
    when(searchIndexClient.count(anyString(), anyList())).thenReturn(9L);
    // Application facet still uses discovery when only org search is set.
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(),
        anyList()))
            .thenReturn(resultWith(List.of(item("app-1", "org-A"))));

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, QUERY, 10, "zeta", null, List.of());

    assertThat(facets.organizations).containsOnlyKeys("org-zeta").containsEntry("org-zeta", 9L);
    assertThat(facets.organizationNames).containsEntry("org-zeta", "Zeta Finance");
    assertThat(facets.applications).containsEntry("app-1", 9L);
    verify(organizationDAO).searchByNameSubstring(
        "zeta", ViolationsListFacetsBuilder.MAX_ORGANIZATION_FACET_SEARCH_CANDIDATES);
    verify(organizationDAO, never()).getByIds(any());
    verify(dimensionQueryBuilder).expandOrganizationFilterIdsById(Set.of("org-zeta"));
    verify(dimensionQueryBuilder, never())
        .expandOrganizationFilterIdsById(argThat(ids -> ids != null && ids.contains("org-A")));
  }

  @Test
  public void buildFacets_organizationFacetSearch_batchesDescendantExpansionOnce() {
    Organization matchA = org("org-a", "Alpha");
    Organization matchB = org("org-b", "Beta");
    stubReadableOrgs(matchA, matchB);
    when(organizationDAO.searchByNameSubstring(
        "ab", ViolationsListFacetsBuilder.MAX_ORGANIZATION_FACET_SEARCH_CANDIDATES))
            .thenReturn(List.of(matchA, matchB));
    when(dimensionQueryBuilder.expandOrganizationFilterIdsById(Set.of("org-a", "org-b")))
        .thenReturn(Map.of(
            "org-a", Set.of("org-a"),
            "org-b", Set.of("org-b", "org-b-child")));
    when(searchIndexClient.count(anyString(), anyList())).thenAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      List<IndexFilterRestriction> restrictions = invocation.getArgument(1);
      boolean hasA = restrictions.stream()
          .filter(r -> r instanceof IndexTermSetRestriction)
          .map(r -> (IndexTermSetRestriction) r)
          .anyMatch(r -> r.ids().contains("org-a"));
      boolean hasB = restrictions.stream()
          .filter(r -> r instanceof IndexTermSetRestriction)
          .map(r -> (IndexTermSetRestriction) r)
          .anyMatch(r -> r.ids().contains("org-b"));
      if (hasA)
        return 3L;
      if (hasB)
        return 5L;
      return 0L;
    });
    stubEmptyDiscovery();

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, QUERY, 10, "ab", null, List.of());

    assertThat(facets.organizations)
        .containsEntry("org-a", 3L)
        .containsEntry("org-b", 5L);
    verify(dimensionQueryBuilder, times(1)).expandOrganizationFilterIdsById(Set.of("org-a", "org-b"));
    verify(dimensionQueryBuilder, never()).buildOrganizationFilterClause(any());
  }

  @Test
  public void buildFacets_organizationFacetSearch_omitsZeroCountMatches() {
    Organization match = org("org-zero", "Zero");
    stubReadableOrgs(match);
    when(organizationDAO.searchByNameSubstring(
        "zero", ViolationsListFacetsBuilder.MAX_ORGANIZATION_FACET_SEARCH_CANDIDATES))
            .thenReturn(List.of(match));
    when(dimensionQueryBuilder.expandOrganizationFilterIdsById(Set.of("org-zero")))
        .thenReturn(Map.of("org-zero", Set.of("org-zero")));
    when(searchIndexClient.count(anyString(), anyList())).thenReturn(0L);
    stubEmptyDiscovery();

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, QUERY, 10, "zero", null, List.of());

    assertThat(facets.organizations).isNull();
  }

  @Test
  public void buildFacets_organizationFacetSearch_skipsOrgsOmittedFromClauseMap() {
    Organization huge = org("org-huge", "Huge");
    Organization ok = org("org-ok", "Ok Org");
    stubReadableOrgs(huge, ok);
    when(organizationDAO.searchByNameSubstring(
        "org", ViolationsListFacetsBuilder.MAX_ORGANIZATION_FACET_SEARCH_CANDIDATES))
            .thenReturn(List.of(huge, ok));
    // Soft-skip contract: oversized expansions are omitted from the expanded id map.
    when(dimensionQueryBuilder.expandOrganizationFilterIdsById(Set.of("org-huge", "org-ok")))
        .thenReturn(Map.of("org-ok", Set.of("org-ok")));
    when(searchIndexClient.count(anyString(), anyList())).thenReturn(4L);
    stubEmptyDiscovery();

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, QUERY, 10, "org", null, List.of());

    assertThat(facets.organizations).containsOnlyKeys("org-ok").containsEntry("org-ok", 4L);
    assertThat(facets.organizationNames).containsEntry("org-ok", "Ok Org");
    assertThat(facets.organizationNames).doesNotContainKey("org-huge");
  }

  @Test
  public void buildFacets_organizationFacetSearch_overFetchesPastAlphabetZerosToFindPositive() {
    // Alphabetically-early zero-count owners must not hide a later positive-count match (Richard).
    List<Organization> candidates = new ArrayList<>();
    for (int i = 0; i < ViolationsListFacetsBuilder.MAX_ORGANIZATION_FACETS; i++) {
      candidates.add(org("org-zero-" + i, "Aaa Zero " + i));
    }
    Organization positive = org("org-zeta", "Zeta Finance");
    candidates.add(positive);
    stubReadableOrgs(candidates.toArray(Organization[]::new));
    when(organizationDAO.searchByNameSubstring(
        "a", ViolationsListFacetsBuilder.MAX_ORGANIZATION_FACET_SEARCH_CANDIDATES))
            .thenReturn(candidates);
    when(dimensionQueryBuilder.expandOrganizationFilterIdsById(anySet())).thenAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      Set<String> ids = invocation.getArgument(0);
      Map<String, Set<String>> expanded = new java.util.LinkedHashMap<>();
      for (String id : ids) {
        expanded.put(id, Set.of(id));
      }
      return expanded;
    });
    when(searchIndexClient.count(anyString(), anyList())).thenAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      List<IndexFilterRestriction> restrictions = invocation.getArgument(1);
      boolean hasZeta = restrictions.stream()
          .filter(r -> r instanceof IndexTermSetRestriction)
          .map(r -> (IndexTermSetRestriction) r)
          .anyMatch(r -> r.ids().contains("org-zeta"));
      return hasZeta ? 9L : 0L;
    });
    stubEmptyDiscovery();

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, QUERY, 10, "a", null, List.of());

    assertThat(facets.organizations).containsOnlyKeys("org-zeta").containsEntry("org-zeta", 9L);
    assertThat(facets.organizationNames).containsEntry("org-zeta", "Zeta Finance");
    verify(organizationDAO).searchByNameSubstring(
        "a", ViolationsListFacetsBuilder.MAX_ORGANIZATION_FACET_SEARCH_CANDIDATES);
  }

  @Test
  public void buildFacets_organizationFacetSearch_omitsParentOutsideReadVisibility() {
    // Parent matches by name and would count>0 via a visible child, but caller has no READ on parent.
    Organization parent = org("org-parent", "Parent Hidden");
    stubReadableOrgs(); // id-scoped READ returns empty for the parent candidate
    when(organizationDAO.searchByNameSubstring(
        "parent", ViolationsListFacetsBuilder.MAX_ORGANIZATION_FACET_SEARCH_CANDIDATES))
            .thenReturn(List.of(parent));
    stubEmptyDiscovery();

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, QUERY, 10, "parent", null, List.of());

    assertThat(facets.organizations).isNull();
    assertThat(facets.organizationNames).isNull();
    verify(organizationSummaryService).getOrganizationsForRead(Set.of("org-parent"));
    verify(dimensionQueryBuilder, never()).expandOrganizationFilterIdsById(anySet());
  }

  @Test
  public void buildFacets_legacy_applicationFacetSearch_keepsOrgDiscovery() {
    Application match = app("app-zeta", "zeta-billing", "Zeta Billing");
    stubReadableApps(match);
    when(applicationDAO.searchByNameSubstring(
        "billing", ViolationsListFacetsBuilder.MAX_APPLICATION_FACET_SEARCH_CANDIDATES))
            .thenReturn(List.of(match));
    when(searchIndexClient.count(anyString(), anyList())).thenReturn(4L);
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(),
        anyList()))
            .thenReturn(resultWith(List.of(item("app-1", "org-A"))));
    when(dimensionQueryBuilder.expandOrganizationFilterIdsById(Set.of("org-A")))
        .thenReturn(Map.of("org-A", Set.of("org-A")));

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, QUERY, 10, null, "billing", List.of());

    assertThat(facets.applications).containsOnlyKeys("app-zeta").containsEntry("app-zeta", 4L);
    assertThat(facets.applicationNames).containsEntry("app-zeta", "Zeta Billing");
    assertThat(facets.organizations).containsEntry("org-A", 4L);
    verify(applicationDAO).searchByNameSubstring(
        "billing", ViolationsListFacetsBuilder.MAX_APPLICATION_FACET_SEARCH_CANDIDATES);
    verify(applicationDAO, never()).getByIds(any());
    verify(dimensionQueryBuilder).expandOrganizationFilterIdsById(Set.of("org-A"));
  }

  @Test
  public void buildFacets_session_applicationFacetSearch_usesDimensionCounts() {
    Application match = app("app-zeta", "zeta-billing", "Zeta Billing");
    stubReadableApps(match);
    when(applicationDAO.searchByNameSubstring(
        "billing", ViolationsListFacetsBuilder.MAX_APPLICATION_FACET_SEARCH_CANDIDATES))
            .thenReturn(List.of(match));
    when(session.count(any(Query.class))).thenReturn(4L);
    when(session.termsAggregation(eq(sessionQuery), eq(FieldIdentifier.ORGANIZATION_ID.label), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("org-A", 5L)));
    Organization organization = org("org-A", "Alpha");
    when(organizationDAO.getByIds(any())).thenReturn(List.of(organization));

    ViolationsListFacetsDTO facets = builder().buildFacets(session, QUERY, QUERY, 10, null, "billing", List.of());

    assertThat(facets.applications).containsOnlyKeys("app-zeta").containsEntry("app-zeta", 4L);
    assertThat(facets.applicationNames).containsEntry("app-zeta", "Zeta Billing");
    assertThat(facets.organizations).containsEntry("org-A", 5L);
    verify(session, never())
        .termsAggregation(any(Query.class), eq(FieldIdentifier.APPLICATION_ID.label), anyInt());
    verify(searchIndexClient, never()).count(anyString(), anyList());
    verify(applicationDAO).searchByNameSubstring(
        "billing", ViolationsListFacetsBuilder.MAX_APPLICATION_FACET_SEARCH_CANDIDATES);
    verify(applicationDAO, never()).getByIds(any());
  }

  @Test
  public void buildFacets_session_organizationFacetSearch_usesDimensionCounts() {
    Organization match = org("org-zeta", "Zeta Finance");
    stubReadableOrgs(match);
    when(organizationDAO.searchByNameSubstring(
        "zeta", ViolationsListFacetsBuilder.MAX_ORGANIZATION_FACET_SEARCH_CANDIDATES))
            .thenReturn(List.of(match));
    when(dimensionQueryBuilder.expandOrganizationFilterIdsById(Set.of("org-zeta")))
        .thenReturn(Map.of("org-zeta", Set.of("org-zeta")));
    // Session-path org name-search counts must stay on the shared IndexReadSession snapshot.
    when(session.count(any(Query.class))).thenReturn(7L);
    when(session.termsAggregation(eq(sessionQuery), eq(FieldIdentifier.APPLICATION_ID.label), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("app-1", 3L)));
    Application application = app("app-1", "apple", "Apple");
    when(applicationDAO.getByIds(any())).thenReturn(List.of(application));

    ViolationsListFacetsDTO facets = builder().buildFacets(session, QUERY, QUERY, 10, "zeta", null, List.of());

    assertThat(facets.organizations).containsOnlyKeys("org-zeta").containsEntry("org-zeta", 7L);
    assertThat(facets.organizationNames).containsEntry("org-zeta", "Zeta Finance");
    assertThat(facets.applications).containsEntry("app-1", 3L);
    verify(session, atLeastOnce()).count(any(Query.class));
    verify(searchIndexClient, never()).count(anyString(), anyList());
    verify(session, never())
        .termsAggregation(any(Query.class), eq(FieldIdentifier.ORGANIZATION_ID.label), anyInt());
    verify(organizationDAO, never()).getByIds(any());
  }

  @Test
  public void buildFacets_bothFacetSearches_skipDiscovery() {
    Organization organization = org("org-zeta", "Zeta Finance");
    Application application = app("app-zeta", "zeta-billing", "Zeta Billing");
    stubReadableOrgs(organization);
    stubReadableApps(application);
    when(organizationDAO.searchByNameSubstring(
        "zeta", ViolationsListFacetsBuilder.MAX_ORGANIZATION_FACET_SEARCH_CANDIDATES))
            .thenReturn(List.of(organization));
    when(applicationDAO.searchByNameSubstring(
        "billing", ViolationsListFacetsBuilder.MAX_APPLICATION_FACET_SEARCH_CANDIDATES))
            .thenReturn(List.of(application));
    when(dimensionQueryBuilder.expandOrganizationFilterIdsById(Set.of("org-zeta")))
        .thenReturn(Map.of("org-zeta", Set.of("org-zeta")));
    when(searchIndexClient.count(anyString(), anyList())).thenReturn(2L);

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, QUERY, 10, "zeta", "billing", List.of());

    assertThat(facets.organizations).containsEntry("org-zeta", 2L);
    assertThat(facets.applications).containsEntry("app-zeta", 2L);
    verify(searchIndexClient, never())
        .searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList());
    verify(organizationDAO, never()).getByIds(any());
    verify(applicationDAO, never()).getByIds(any());
  }
}
