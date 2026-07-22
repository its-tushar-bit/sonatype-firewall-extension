/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
  private IndexReadSession session;

  private final Query sessionQuery = new MatchAllDocsQuery();

  private ViolationsListFacetsBuilder builder() {
    return new ViolationsListFacetsBuilder(
        searchIndexClient,
        stageTypeService,
        dimensionQueryBuilder,
        conversionHelper,
        organizationDAO,
        applicationDAO);
  }

  @Before
  public void stubSessionQueryConversion() {
    lenient().when(conversionHelper.stringToQuery(anyString())).thenReturn(sessionQuery);
    lenient().when(organizationDAO.getByIds(any())).thenReturn(List.of());
    lenient().when(applicationDAO.getByIds(any())).thenReturn(List.of());
  }

  private void stubEmptyDiscovery() {
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
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
  public void buildFacets_openStateCountedAsNotWaived() {
    stubEmptyDiscovery();
    // OPEN is counted as "NOT waived"; WAIVED via its own combined ":(Waived AutoWaived)" count. The
    // "!NOT" guard keeps the WAIVED matcher from also matching the OPEN "AND NOT (...)" query.
    when(searchIndexClient.count(argThat(q -> q != null && q.contains("AND NOT ")))).thenReturn(7L);
    when(searchIndexClient.count(argThat(q -> q != null
        && q.contains("policyViolationWaiverStatus:(Waived AutoWaived)") && !q.contains("NOT"))))
            .thenReturn(3L);

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, 10);

    assertThat(facets.states)
        .containsEntry("OPEN", 7L)
        .containsEntry("WAIVED", 3L);
  }

  @Test
  public void buildFacets_waiverTypeCounts_autoAndManual() {
    stubEmptyDiscovery();
    when(searchIndexClient.count(argThat(q -> q != null && q.contains("policyViolationWaiverStatus:(AutoWaived)"))))
        .thenReturn(4L);
    when(searchIndexClient.count(argThat(q -> q != null && q.contains("policyViolationWaiverStatus:(Waived)"))))
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
        && q.contains("policyViolationWaiverStatus:(AutoWaived)")))).thenReturn(4L);
    when(searchIndexClient.count(argThat(q -> q != null && q.startsWith(waiverFacetQuery)
        && q.contains("policyViolationWaiverStatus:(Waived)")))).thenReturn(2L);

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
        .count(argThat(q -> q != null && q.contains("policyViolationThreatCategory:(" + security + ")"))))
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
    when(searchIndexClient.count(argThat(q -> q != null && q.contains("policyEvaluationStage:(build)"))))
        .thenReturn(4L);

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, 10);

    assertThat(facets.stages).containsEntry("build", 4L);
  }

  @Test
  public void buildFacets_organizationAndApplicationMaps_skipRootOrg() {
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultWith(List.of(
            item("app-1", "org-A"),
            item("app-2", Organization.ROOT_ORGANIZATION_ID))));

    when(dimensionQueryBuilder.buildOrganizationFilterClause(Set.of("org-A"))).thenReturn("organizationId:(org-A)");
    // Root org yields a null clause and must be skipped rather than counted against the full query.
    when(dimensionQueryBuilder.buildOrganizationFilterClause(Set.of(Organization.ROOT_ORGANIZATION_ID)))
        .thenReturn(null);
    when(dimensionQueryBuilder.buildEscapedApplicationFilterClause(Set.of("app-1")))
        .thenReturn("applicationId:(app-1)");
    when(dimensionQueryBuilder.buildEscapedApplicationFilterClause(Set.of("app-2")))
        .thenReturn("applicationId:(app-2)");

    when(searchIndexClient.count(argThat(q -> q != null && q.contains("organizationId:(org-A)")))).thenReturn(5L);
    when(searchIndexClient.count(argThat(q -> q != null && q.contains("applicationId:(app-1)")))).thenReturn(3L);
    when(searchIndexClient.count(argThat(q -> q != null && q.contains("applicationId:(app-2)")))).thenReturn(2L);

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
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultWith(items));
    when(dimensionQueryBuilder.buildOrganizationFilterClause(any())).thenReturn("organizationId:(org-A)");
    when(dimensionQueryBuilder.buildEscapedApplicationFilterClause(any())).thenReturn("applicationId:(app)");
    when(searchIndexClient.count(anyString())).thenReturn(1L);

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, 100);

    assertThat(facets.applications).hasSize(ViolationsListFacetsBuilder.MAX_APPLICATION_FACETS);
  }

  @Test
  public void buildFacets_session_zeroTotal_shortCircuitsWithNoSessionQueries() {
    ViolationsListFacetsDTO facets = builder().buildFacets(session, QUERY, QUERY, 0);

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

    ViolationsListFacetsDTO facets = builder().buildFacets(session, QUERY, QUERY, 10);

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

    ViolationsListFacetsDTO facets = builder().buildFacets(session, listQuery, waiverFacetQuery, 10);

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

    ViolationsListFacetsDTO facets = builder().buildFacets(session, QUERY, QUERY, 1);

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

    ViolationsListFacetsDTO facets = builder().buildFacets(session, QUERY, QUERY, 10);

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

    assertThatThrownBy(() -> builder().buildFacets(session, QUERY, QUERY, 10))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("db unavailable");
  }

  @Test
  public void buildFacets_legacy_prefersDiscoveryNamesOverDao() {
    SearchResultItemDTO discovered = item("app-1", "org-A");
    discovered.organizationName = "From-Index";
    discovered.applicationName = "From-Index-App";
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultWith(List.of(discovered)));
    when(dimensionQueryBuilder.buildOrganizationFilterClause(Set.of("org-A"))).thenReturn("organizationId:(org-A)");
    when(dimensionQueryBuilder.buildEscapedApplicationFilterClause(Set.of("app-1")))
        .thenReturn("applicationId:(app-1)");
    when(searchIndexClient.count(anyString())).thenReturn(1L);

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, 1);

    assertThat(facets.organizationNames).containsEntry("org-A", "From-Index");
    assertThat(facets.applicationNames).containsEntry("app-1", "From-Index-App");
    verify(organizationDAO, never()).getByIds(any());
    verify(applicationDAO, never()).getByIds(any());
  }
}
