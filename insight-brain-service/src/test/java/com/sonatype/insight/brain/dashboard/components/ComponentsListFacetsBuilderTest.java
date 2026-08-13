/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexReadSessionFactory;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ComponentsListFacetsBuilderTest
{
  @Mock
  private SearchIndexClient searchIndexClient;

  @Mock
  private IndexReadSessionFactory indexReadSessionFactory;

  @Mock
  private IndexReadSession indexReadSession;

  @Mock
  private ConversionHelper conversionHelper;

  @Mock
  private StageTypeService stageTypeService;

  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private ApplicationDAO applicationDAO;

  private ComponentsListFacetsBuilder facetsBuilder;

  @BeforeEach
  public void setUp() {
    when(indexReadSessionFactory.open()).thenReturn(indexReadSession);
    when(conversionHelper.stringToQuery(anyString())).thenReturn(new MatchAllDocsQuery());
    when(stageTypeService.getLicensedStageTypes(any())).thenReturn(List.of());
    lenient().when(organizationDAO.getByIds(anyCollection())).thenReturn(List.of());
    lenient().when(applicationDAO.getByIds(anySet())).thenReturn(List.of());
    facetsBuilder = new ComponentsListFacetsBuilder(
        searchIndexClient,
        indexReadSessionFactory,
        conversionHelper,
        stageTypeService,
        organizationDAO,
        applicationDAO);
  }

  @Test
  public void buildFacets_discoversOrgAndAppKeysViaTermsAggregation() {
    discoverKeys(List.of("org-1", "org-2"), List.of("app-1"));
    when(indexReadSession.countDistinctGroupedBy(any(), eq(FieldIdentifier.ORGANIZATION_ID.label),
        eq(FieldIdentifier.COMPONENT_HASH.label), anyCollection()))
            .thenReturn(Map.of("org-1", 7L, "org-2", 4L));
    when(indexReadSession.countDistinctGroupedBy(any(), eq(FieldIdentifier.APPLICATION_ID.label),
        eq(FieldIdentifier.COMPONENT_HASH.label), anyCollection())).thenReturn(Map.of("app-1", 3L));

    Organization org1 = new Organization();
    org1.setId("org-1");
    org1.setName("Alpha Org");
    Organization org2 = new Organization();
    org2.setId("org-2");
    org2.setName("Beta Org");
    when(organizationDAO.getByIds(anyCollection())).thenReturn(List.of(org1, org2));

    Application app1 = new Application();
    app1.setId("app-1");
    app1.setName("Payments Service");
    when(applicationDAO.getByIds(anySet())).thenReturn(List.of(app1));

    ComponentsListFacetsDTO facets =
        facetsBuilder.buildFacets(ComponentsListViolationQuerySupport.COMPONENT_ITEM_TYPE_CLAUSE, 42);

    assertThat(facets.totalComponents).isEqualTo(42);
    assertThat(facets.organizations).containsEntry("org-1", 7L).containsEntry("org-2", 4L);
    assertThat(facets.applications).containsEntry("app-1", 3L);
    assertThat(facets.organizationNames)
        .containsEntry("org-1", "Alpha Org")
        .containsEntry("org-2", "Beta Org");
    assertThat(facets.applicationNames).containsEntry("app-1", "Payments Service");
    verify(indexReadSession).termsAggregation(any(), eq(FieldIdentifier.ORGANIZATION_ID.label),
        eq(ComponentsListFacetsBuilder.MAX_FACET_TERM_BUCKETS));
    verify(indexReadSession).termsAggregation(any(), eq(FieldIdentifier.APPLICATION_ID.label),
        eq(ComponentsListFacetsBuilder.MAX_FACET_TERM_BUCKETS));
  }

  /**
   * Round trips must not grow with the estate: one grouped call per dimension, and no per-key
   * {@code countDistinct} fan-out, even with far more facet keys than the entry cap.
   */
  @Test
  public void buildFacets_manyKeys_issuesOneGroupedQueryPerDimensionAndNoPerKeyQueries() {
    List<String> manyOrgs = ids("org-", 80);
    List<String> manyApps = ids("app-", 80);
    discoverKeys(manyOrgs, manyApps);

    facetsBuilder.buildFacets(ComponentsListViolationQuerySupport.COMPONENT_ITEM_TYPE_CLAUSE, 500);

    verify(indexReadSession, times(1)).countDistinctGroupedBy(any(),
        eq(FieldIdentifier.ORGANIZATION_ID.label), eq(FieldIdentifier.COMPONENT_HASH.label), anyCollection());
    verify(indexReadSession, times(1)).countDistinctGroupedBy(any(),
        eq(FieldIdentifier.APPLICATION_ID.label), eq(FieldIdentifier.COMPONENT_HASH.label), anyCollection());
    verify(searchIndexClient, never()).countDistinct(anyString(), anyList());
    verify(indexReadSessionFactory, times(1)).open();
  }

  @Test
  public void buildFacets_cappsFacetEntriesReturnedToTheRail() {
    discoverKeys(ids("org-", 80), ids("app-", 80));

    ComponentsListFacetsDTO facets =
        facetsBuilder.buildFacets(ComponentsListViolationQuerySupport.COMPONENT_ITEM_TYPE_CLAUSE, 500);

    assertThat(facets.organizations).hasSize(ComponentsListFacetsBuilder.MAX_ORGANIZATION_FACET_ENTRIES);
    assertThat(facets.applications).hasSize(ComponentsListFacetsBuilder.MAX_APPLICATION_FACET_ENTRIES);
  }

  @Test
  public void buildFacets_backendWithoutGroupedCounting_fallsBackToBoundedPerKeyCounts() {
    discoverKeys(List.of("org-1", "org-2"), List.of());
    when(indexReadSession.backendId()).thenReturn("opensearch");
    when(indexReadSession.countDistinctGroupedBy(any(), anyString(), anyString(), anyCollection()))
        .thenThrow(new UnsupportedOperationException("not implemented"));
    when(searchIndexClient.countDistinct(anyString(), anyList())).thenReturn(9L);

    ComponentsListFacetsDTO facets =
        facetsBuilder.buildFacets(ComponentsListViolationQuerySupport.COMPONENT_ITEM_TYPE_CLAUSE, 42);

    assertThat(facets.organizations).containsEntry("org-1", 9L).containsEntry("org-2", 9L);
    verify(searchIndexClient, times(2)).countDistinct(anyString(), anyList());
  }

  @Test
  public void buildFacets_groupedCountsMissingKey_reportsZeroRatherThanOmitting() {
    discoverKeys(List.of("org-1", "org-2"), List.of());
    when(indexReadSession.countDistinctGroupedBy(any(), eq(FieldIdentifier.ORGANIZATION_ID.label),
        eq(FieldIdentifier.COMPONENT_HASH.label), anyCollection())).thenReturn(Map.of("org-1", 5L));

    ComponentsListFacetsDTO facets =
        facetsBuilder.buildFacets(ComponentsListViolationQuerySupport.COMPONENT_ITEM_TYPE_CLAUSE, 42);

    assertThat(facets.organizations).containsEntry("org-1", 5L).containsEntry("org-2", 0L);
  }

  /**
   * {@code countDistinctGroupedBy} keys its result map by the lowercased group value on both
   * backends, so reading it with the verbatim id would silently report zero for any organization,
   * application or stage whose id is not already lowercase.
   */
  @Test
  public void buildFacets_mixedCaseGroupId_readsGroupedCountByLowercasedKey() {
    discoverKeys(List.of("Org-Mixed-Case"), List.of());
    when(indexReadSession.countDistinctGroupedBy(any(), eq(FieldIdentifier.ORGANIZATION_ID.label),
        eq(FieldIdentifier.COMPONENT_HASH.label), anyCollection())).thenReturn(Map.of("org-mixed-case", 12L));

    ComponentsListFacetsDTO facets =
        facetsBuilder.buildFacets(ComponentsListViolationQuerySupport.COMPONENT_ITEM_TYPE_CLAUSE, 42);

    assertThat(facets.organizations).containsEntry("Org-Mixed-Case", 12L);
  }

  /**
   * A component row carries no stage breakdown, so the rail has nothing to label a stage with
   * unless the facets supply the names (CLM-43211).
   */
  @Test
  public void buildFacets_stagesWithCounts_returnsDisplayNamesForTheRail() {
    discoverKeys(List.of(), List.of());
    List<StageType> licensedStages = List.of(stageType("build", "Build"), stageType("release", "Release"));
    when(stageTypeService.getLicensedStageTypes(any())).thenReturn(licensedStages);
    when(indexReadSession.countDistinctGroupedBy(any(), eq(FieldIdentifier.POLICY_EVALUATION_STAGE.label),
        eq(FieldIdentifier.COMPONENT_HASH.label), anyCollection()))
            .thenReturn(Map.of("build", 9L));

    ComponentsListFacetsDTO facets =
        facetsBuilder.buildFacets(ComponentsListViolationQuerySupport.COMPONENT_ITEM_TYPE_CLAUSE, 42);

    // Only stages with a non-zero count reach the rail, so only those need a name.
    assertThat(facets.stages).containsExactly(Map.entry("build", 9L));
    assertThat(facets.stageNames).containsExactly(Map.entry("build", "Build"));
  }

  @Test
  public void buildFacets_noStageCounts_omitsStageNames() {
    discoverKeys(List.of(), List.of());
    List<StageType> licensedStages = List.of(stageType("build", "Build"));
    when(stageTypeService.getLicensedStageTypes(any())).thenReturn(licensedStages);
    when(indexReadSession.countDistinctGroupedBy(any(), eq(FieldIdentifier.POLICY_EVALUATION_STAGE.label),
        eq(FieldIdentifier.COMPONENT_HASH.label), anyCollection())).thenReturn(Map.of());

    ComponentsListFacetsDTO facets =
        facetsBuilder.buildFacets(ComponentsListViolationQuerySupport.COMPONENT_ITEM_TYPE_CLAUSE, 42);

    assertThat(facets.stages).isNull();
    assertThat(facets.stageNames).isNull();
  }

  private static StageType stageType(final String id, final String name) {
    StageType stageType = mock(StageType.class);
    lenient().when(stageType.getId()).thenReturn(id);
    lenient().when(stageType.getName()).thenReturn(name);
    return stageType;
  }

  private void discoverKeys(final List<String> organizationIds, final List<String> applicationIds) {
    lenient().when(indexReadSession.termsAggregation(any(), eq(FieldIdentifier.ORGANIZATION_ID.label), anyInt()))
        .thenReturn(buckets(organizationIds));
    lenient().when(indexReadSession.termsAggregation(any(), eq(FieldIdentifier.APPLICATION_ID.label), anyInt()))
        .thenReturn(buckets(applicationIds));
  }

  private static List<IndexTermsBucket> buckets(final List<String> keys) {
    return keys.stream().map(key -> new IndexTermsBucket(key, 1L)).toList();
  }

  private static List<String> ids(final String prefix, final int count) {
    return java.util.stream.IntStream.range(0, count).mapToObj(index -> prefix + index).toList();
  }
}
