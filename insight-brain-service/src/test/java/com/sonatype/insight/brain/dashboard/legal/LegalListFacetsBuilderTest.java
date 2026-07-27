/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.legal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.junit.Before;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Legal list facet builder — zero-total short-circuit, LTG phrase quoting,
 * org/app/LTG caps, and owner-label DAO fallback (CLM-43207).
 */
@RunWith(MockitoJUnitRunner.class)
public class LegalListFacetsBuilderTest
{
  private static final String QUERY = "itemType:LEGAL_VIOLATION";

  @Mock
  private SearchIndexClient searchIndexClient;

  @Mock
  private StageTypeService stageTypeService;

  @Mock
  private DashboardIndexDimensionQueryBuilder dimensionQueryBuilder;

  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private ApplicationDAO applicationDAO;

  private LegalListFacetsBuilder builder() {
    return new LegalListFacetsBuilder(
        searchIndexClient,
        stageTypeService,
        dimensionQueryBuilder,
        organizationDAO,
        applicationDAO);
  }

  @Before
  public void stubDefaults() {
    lenient().when(organizationDAO.getByIds(any())).thenReturn(List.of());
    lenient().when(applicationDAO.getByIds(any())).thenReturn(List.of());
    lenient().when(stageTypeService.getLicensedStageTypes(any())).thenReturn(List.of());
  }

  @Test
  public void buildFacets_zeroTotal_shortCircuitsWithoutIndexCalls() {
    LegalListFacetsDTO facets = builder().buildFacets(QUERY, 0);

    assertThat(facets.totalFindings).isEqualTo(0);
    assertThat(facets.stages).isNull();
    assertThat(facets.licenseThreatGroups).isNull();
    verify(searchIndexClient, never()).searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(),
        anyList());
    verify(searchIndexClient, never()).count(anyString());
  }

  @Test
  public void buildFacets_quotesMultiWordLtgInCountQuery() {
    when(stageTypeService.getLicensedStageTypes(any())).thenReturn(List.of());
    when(searchIndexClient.count(anyString())).thenReturn(1L);

    SearchResultItemDTO item = legalItem("app-1", "org-1", "Weak Copyleft");
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultWith(List.of(item)));
    when(dimensionQueryBuilder.buildOrganizationFilterClause(Set.of("org-1"))).thenReturn("organizationId:(org-1)");
    when(dimensionQueryBuilder.buildEscapedApplicationFilterClause(Set.of("app-1")))
        .thenReturn("applicationId:(app-1)");

    LegalListFacetsDTO facets = builder().buildFacets(QUERY, 1);

    assertThat(facets.licenseThreatGroups).containsEntry("Weak Copyleft", 1L);
    verify(searchIndexClient).count(contains("componentLicenseThreatGroupName:(\"Weak Copyleft\")"));
    // Single discovery search for owners + LTG names (no duplicate LTG discovery query).
    verify(searchIndexClient).searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList());
  }

  @Test
  public void buildFacets_attachesOwnerLabelsFromDaoWhenMissingOnHits() {
    when(stageTypeService.getLicensedStageTypes(any())).thenReturn(List.of());
    SearchResultItemDTO item = legalItem("app-1", "org-1", "Permissive");
    item.organizationName = null;
    item.applicationName = null;
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultWith(List.of(item)));
    when(searchIndexClient.count(anyString())).thenReturn(1L);
    when(dimensionQueryBuilder.buildOrganizationFilterClause(Set.of("org-1"))).thenReturn("organizationId:(org-1)");
    when(dimensionQueryBuilder.buildEscapedApplicationFilterClause(Set.of("app-1")))
        .thenReturn("applicationId:(app-1)");

    Organization org = mock(Organization.class);
    when(org.getId()).thenReturn("org-1");
    when(org.getName()).thenReturn("Org From DAO");
    Application app = mock(Application.class);
    when(app.getId()).thenReturn("app-1");
    when(app.getName()).thenReturn("App From DAO");
    when(organizationDAO.getByIds(Set.of("org-1"))).thenReturn(List.of(org));
    when(applicationDAO.getByIds(Set.of("app-1"))).thenReturn(List.of(app));

    LegalListFacetsDTO facets = builder().buildFacets(QUERY, 1);

    assertThat(facets.organizationNames).containsEntry("org-1", "Org From DAO");
    assertThat(facets.applicationNames).containsEntry("app-1", "App From DAO");
  }

  private static SearchResultItemDTO legalItem(
      final String applicationId,
      final String organizationId,
      final String ltgName)
  {
    SearchResultItemDTO item = new SearchResultItemDTO();
    item.itemType = ItemType.LEGAL_VIOLATION.name();
    item.applicationId = applicationId;
    item.organizationId = organizationId;
    item.componentLicenseThreatGroupName = ltgName;
    return item;
  }

  private static SearchResultDTO resultWith(final List<SearchResultItemDTO> items) {
    SearchResultDTO result = new SearchResultDTO();
    GroupingByDTO group = new GroupingByDTO();
    group.searchResultItemDTOS = new ArrayList<>(items);
    result.groupingByDTOS = new ArrayList<>(List.of(group));
    return result;
  }
}
