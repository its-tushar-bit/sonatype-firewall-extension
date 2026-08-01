/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.sonatype.insight.brain.dashboard.PolicyViolationIndexClauses;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.search.session.IndexPageRequest;
import com.sonatype.insight.brain.search.session.IndexPageResult;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.apache.lucene.document.Field.Store.YES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationsListFacetsParityTest
{
  /** Violation-scoped query string used for the policy-type / violation-state facet counts. */
  private static final String VIOLATION_FACET_QUERY = "itemType:POLICY_VIOLATION";

  @Mock
  private SearchIndexClient searchIndexClient;

  @Mock
  private StageTypeService stageTypeService;

  @Mock
  private IndexReadSession session;

  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private ApplicationDAO applicationDAO;

  @Mock
  private ConversionHelper conversionHelper;

  private ApplicationsListFacetsBuilder builder;

  private Query query;

  @Before
  public void setUp() {
    builder = new ApplicationsListFacetsBuilder(
        searchIndexClient, stageTypeService, organizationDAO, applicationDAO, conversionHelper);
    query = new MatchAllDocsQuery();
    lenient().when(conversionHelper.stringToQuery(any())).thenReturn(new MatchAllDocsQuery());
    when(stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)).thenReturn(List.of());
    when(session.searchPage(any())).thenReturn(new IndexPageResult(List.of(
        applicationDocument("app-alpha", "org-b", "Alpha"),
        applicationDocument("app-beta", "org-a", "Beta")), List.of(), false));
    when(session.countDistinctGroupedBy(any(), any(), any(), any())).thenReturn(Map.of());
  }

  @Test
  public void omitsZeroCountOrg() {
    when(session.termsAggregation(query, FieldIdentifier.ORGANIZATION_ID.label,
        ApplicationsListFacetsBuilder.MAX_ORGANIZATION_FACET_ENTRIES))
            .thenReturn(List.of(new IndexTermsBucket("org-a", 0), new IndexTermsBucket("org-b", 2)));

    ApplicationsListFacetsDTO facets = builder.buildFacets(session, query, query, VIOLATION_FACET_QUERY, 2);

    assertThat(facets.organizations).containsExactly(Map.entry("org-b", 2L));
    assertThat(facets.organizationNames).containsEntry("org-b", "Alpha");
    assertThat(facets.applicationNames)
        .containsEntry("app-alpha", "app-alpha")
        .containsEntry("app-beta", "app-beta");
  }

  @Test
  public void nameSortStableForEqualCounts() {
    when(session.termsAggregation(query, FieldIdentifier.ORGANIZATION_ID.label,
        ApplicationsListFacetsBuilder.MAX_ORGANIZATION_FACET_ENTRIES))
            .thenReturn(List.of(new IndexTermsBucket("org-a", 7), new IndexTermsBucket("org-b", 7)));

    ApplicationsListFacetsDTO facets = builder.buildFacets(session, query, query, VIOLATION_FACET_QUERY, 2);

    assertThat(facets.organizations).containsExactly(Map.entry("org-b", 7L), Map.entry("org-a", 7L));
  }

  @Test
  public void fiveHundredFirstCap_matchesLegacy() {
    List<IndexTermsBucket> buckets = new ArrayList<>();
    for (int i = 0; i < ApplicationsListFacetsBuilder.MAX_ORGANIZATION_FACET_ENTRIES; i++) {
      buckets.add(new IndexTermsBucket("org-" + i, 1));
    }
    when(session.termsAggregation(query, FieldIdentifier.ORGANIZATION_ID.label,
        ApplicationsListFacetsBuilder.MAX_ORGANIZATION_FACET_ENTRIES)).thenReturn(buckets);

    ApplicationsListFacetsDTO facets = builder.buildFacets(session, query, query, VIOLATION_FACET_QUERY, 501);

    assertThat(facets.organizations).hasSize(ApplicationsListFacetsBuilder.MAX_ORGANIZATION_FACET_ENTRIES);
    verify(session).termsAggregation(query, FieldIdentifier.ORGANIZATION_ID.label,
        ApplicationsListFacetsBuilder.MAX_ORGANIZATION_FACET_ENTRIES);
  }

  @Test
  public void applicationSampleUsesSessionSearchPageCap() {
    when(session.termsAggregation(query, FieldIdentifier.ORGANIZATION_ID.label,
        ApplicationsListFacetsBuilder.MAX_ORGANIZATION_FACET_ENTRIES)).thenReturn(List.of());

    ApplicationsListFacetsDTO facets = builder.buildFacets(session, query, query, VIOLATION_FACET_QUERY, 2);

    assertThat(facets.applications).containsExactly(Map.entry("app-alpha", 1L), Map.entry("app-beta", 1L));
    ArgumentCaptor<IndexPageRequest> pageRequest = ArgumentCaptor.forClass(IndexPageRequest.class);
    verify(session).searchPage(pageRequest.capture());
    assertThat(pageRequest.getValue().query()).isSameAs(query);
    assertThat(pageRequest.getValue().pageSize()).isEqualTo(ApplicationsListFacetsBuilder.MAX_FACET_DISCOVERY_HITS);
  }

  @Test
  public void stageFacetUsesSessionCollectorWithoutCountDistinct() {
    StageType build = stage("build");
    when(stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)).thenReturn(List.of(build));
    when(session.termsAggregation(query, FieldIdentifier.ORGANIZATION_ID.label,
        ApplicationsListFacetsBuilder.MAX_ORGANIZATION_FACET_ENTRIES)).thenReturn(List.of());
    when(session.countDistinctGroupedBy(eq(query), eq(FieldIdentifier.POLICY_EVALUATION_STAGE.label),
        eq(FieldIdentifier.APPLICATION_ID.label), eq(List.of("build")))).thenReturn(Map.of("build", 3L));

    ApplicationsListFacetsDTO facets = builder.buildFacets(session, query, query, VIOLATION_FACET_QUERY, 2);

    assertThat(facets.stages).containsExactly(Map.entry("build", 3L));
    verify(session).countDistinctGroupedBy(eq(query), eq(FieldIdentifier.POLICY_EVALUATION_STAGE.label),
        eq(FieldIdentifier.APPLICATION_ID.label), eq(List.of("build")));
  }

  @Test
  public void legacyPathPreservesStageFacetsWithCountDistinct() {
    StageType build = stage("build");
    StageType release = stage("release");
    when(stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT))
        .thenReturn(List.of(build, release));
    when(searchIndexClient.searchIndex("itemType:APPLICATION", ApplicationsListFacetsBuilder.MAX_FACET_DISCOVERY_HITS,
        0, false, false, List.of())).thenReturn(searchResult("app-alpha"));
    when(searchIndexClient.count("itemType:APPLICATION AND organizationId:(org\\-a)")).thenReturn(1L);
    when(searchIndexClient.countDistinct(
        "itemType:POLICY_VIOLATION AND policyEvaluationStage:build",
        List.of(FieldIdentifier.APPLICATION_ID.label))).thenReturn(2L);
    when(searchIndexClient.countDistinct(
        "itemType:POLICY_VIOLATION AND policyEvaluationStage:release",
        List.of(FieldIdentifier.APPLICATION_ID.label))).thenReturn(0L);

    ApplicationsListFacetsDTO facets = builder.buildFacets("itemType:APPLICATION", 1);

    assertThat(facets.stages).containsExactly(Map.entry("build", 2L));
  }

  @Test
  public void sessionPath_countsPolicyTypesInOneGroupedPassAndStatesPerBucket() {
    when(session.termsAggregation(query, FieldIdentifier.ORGANIZATION_ID.label,
        ApplicationsListFacetsBuilder.MAX_ORGANIZATION_FACET_ENTRIES)).thenReturn(List.of());
    when(session.countDistinctGroupedBy(eq(query), eq(FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY.label),
        eq(FieldIdentifier.APPLICATION_ID.label), any()))
            .thenReturn(Map.of("security", 5L, "license", 0L, "quality", 2L));
    // Each state is a separate grouped call whose single POLICY_VIOLATION bucket is the distinct count.
    // Production backends lowercase the group-value key; the mock must match that contract.
    String policyViolationBucket = ItemType.POLICY_VIOLATION.name().toLowerCase(Locale.ROOT);
    when(session.countDistinctGroupedBy(any(), eq(FieldIdentifier.ITEM_TYPE.label),
        eq(FieldIdentifier.APPLICATION_ID.label), eq(List.of(policyViolationBucket))))
            .thenReturn(Map.of(policyViolationBucket, 7L));

    ApplicationsListFacetsDTO facets = builder.buildFacets(session, query, query, VIOLATION_FACET_QUERY, 9);

    assertThat(facets.policyTypes).containsExactly(Map.entry("security", 5L), Map.entry("quality", 2L));
    assertThat(facets.violationStates).containsOnlyKeys("OPEN", "WAIVED", "LEGACY_VIOLATION");
    // One grouped call for all four policy types plus one per state: constant in estate size.
    verify(session, times(3)).countDistinctGroupedBy(any(), eq(FieldIdentifier.ITEM_TYPE.label), any(), any());
    verify(searchIndexClient, never()).countDistinct(any(), any());
  }

  @Test
  public void sessionPath_omitsViolationScopedFacetsWhenBackendLacksGroupedDistinct() {
    when(session.termsAggregation(query, FieldIdentifier.ORGANIZATION_ID.label,
        ApplicationsListFacetsBuilder.MAX_ORGANIZATION_FACET_ENTRIES))
            .thenReturn(List.of(new IndexTermsBucket("org-b", 2)));
    when(session.countDistinctGroupedBy(any(), any(), any(), any()))
        .thenThrow(new UnsupportedOperationException("not implemented by this backend"));

    ApplicationsListFacetsDTO facets = builder.buildFacets(session, query, query, VIOLATION_FACET_QUERY, 2);

    assertThat(facets.policyTypes).isNull();
    assertThat(facets.violationStates).isNull();
    assertThat(facets.organizations).containsExactly(Map.entry("org-b", 2L));
    verify(searchIndexClient, never()).countDistinct(any(), any());
  }

  @Test
  public void legacyPath_countsPolicyTypesAndViolationStatesOverViolationDocs() {
    when(stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)).thenReturn(List.of());
    when(searchIndexClient.searchIndex("itemType:APPLICATION", ApplicationsListFacetsBuilder.MAX_FACET_DISCOVERY_HITS,
        0, false, false, List.of())).thenReturn(searchResult("app-alpha"));
    when(searchIndexClient.countDistinctGroupedBy(eq("itemType:POLICY_VIOLATION"),
        eq(FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY.label), eq(FieldIdentifier.APPLICATION_ID.label), any()))
            .thenReturn(Map.of("security", 4L));
    when(searchIndexClient.countDistinct(any(), eq(List.of(FieldIdentifier.APPLICATION_ID.label)))).thenReturn(0L);
    when(searchIndexClient.countDistinct(
        "itemType:POLICY_VIOLATION AND " + PolicyViolationIndexClauses.openClause(false),
        List.of(FieldIdentifier.APPLICATION_ID.label))).thenReturn(6L);

    ApplicationsListFacetsDTO facets = builder.buildFacets("itemType:APPLICATION", 1);

    assertThat(facets.policyTypes).containsExactly(Map.entry("security", 4L));
    // Zero-count states are omitted so the rail renders them from its fixed domain at zero.
    assertThat(facets.violationStates).containsExactly(Map.entry("OPEN", 6L));
  }

  private static Document applicationDocument(
      final String applicationId,
      final String organizationId,
      final String organizationName)
  {
    Document document = new Document();
    document.add(new StringField(FieldIdentifier.ITEM_TYPE.label, ItemType.APPLICATION.name(), YES));
    document.add(new StringField(FieldIdentifier.APPLICATION_ID.label, applicationId, YES));
    document.add(new StringField(FieldIdentifier.APPLICATION_PUBLIC_ID.label, applicationId, YES));
    document.add(new StringField(FieldIdentifier.APPLICATION_NAME.label, applicationId, YES));
    document.add(new StringField(FieldIdentifier.ORGANIZATION_ID.label, organizationId, YES));
    document.add(new StringField(FieldIdentifier.ORGANIZATION_NAME.label, organizationName, YES));
    return document;
  }

  private static SearchResultDTO searchResult(final String applicationId) {
    SearchResultDTO result = new SearchResultDTO();
    result.totalNumberOfHits = 1;
    GroupingByDTO group = new GroupingByDTO();
    SearchResultItemDTO item = new SearchResultItemDTO();
    item.itemType = ItemType.APPLICATION.name();
    item.applicationId = applicationId;
    item.applicationPublicId = applicationId;
    item.applicationName = applicationId;
    item.organizationId = "org-a";
    item.organizationName = "Org A";
    group.searchResultItemDTOS.add(item);
    result.groupingByDTOS = List.of(group);
    return result;
  }

  private static StageType stage(final String id) {
    return new StageType()
    {
      @Override
      public String getId() {
        return id;
      }

      @Override
      public String getName() {
        return id;
      }
    };
  }

}
