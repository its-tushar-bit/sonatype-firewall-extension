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
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
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

  private ViolationsListFacetsBuilder builder() {
    return new ViolationsListFacetsBuilder(searchIndexClient, stageTypeService, dimensionQueryBuilder);
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
    assertThat(facets.threatCategories).isNull();
    assertThat(facets.stages).isNull();
    assertThat(facets.organizations).isNull();
    assertThat(facets.applications).isNull();
  }

  @Test
  public void buildFacets_openStateCountedAsNotWaived() {
    stubEmptyDiscovery();
    when(searchIndexClient.count(argThat(q -> q != null && q.contains("AND NOT ")))).thenReturn(7L);
    when(searchIndexClient.count(argThat(q -> q != null && q.contains("Waived AutoWaived") && !q.contains("NOT"))))
        .thenReturn(3L);

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, 10);

    assertThat(facets.states)
        .containsEntry("OPEN", 7L)
        .containsEntry("WAIVED", 3L);
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
    for (int i = 0; i < ViolationsListFacetsBuilder.MAX_APPLICATION_FACET_COUNT_QUERIES + 5; i++) {
      items.add(item("app-" + i, "org-A"));
    }
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultWith(items));
    when(dimensionQueryBuilder.buildOrganizationFilterClause(any())).thenReturn("organizationId:(org-A)");
    when(dimensionQueryBuilder.buildEscapedApplicationFilterClause(any())).thenReturn("applicationId:(app)");
    when(searchIndexClient.count(anyString())).thenReturn(1L);

    ViolationsListFacetsDTO facets = builder().buildFacets(QUERY, 100);

    assertThat(facets.applications).hasSize(ViolationsListFacetsBuilder.MAX_APPLICATION_FACET_COUNT_QUERIES);
  }
}
