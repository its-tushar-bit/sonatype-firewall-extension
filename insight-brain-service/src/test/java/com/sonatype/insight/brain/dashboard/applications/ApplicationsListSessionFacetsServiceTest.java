/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dashboard.ApplicationRiskScoreDTO;
import com.sonatype.insight.brain.dashboard.ApplicationRiskService;
import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.DashboardResultsDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.session.IndexPageResult;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexReadSessionFactory;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;
import com.sonatype.insight.brain.service.Configuration;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.apache.lucene.document.Field.Store.YES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ApplicationsListSessionFacetsServiceTest
{
  @Mock
  private SearchIndexClient searchIndexClient;

  @Mock
  private ApplicationRiskService applicationRiskService;

  @Mock
  private ApplicationsListViolationScopeResolver violationScopeResolver;

  @Mock
  private ApplicationsListRequestValidator requestValidator;

  @Mock
  private StageTypeService stageTypeService;

  @Mock
  private Configuration configuration;

  @Mock
  private ConversionHelper conversionHelper;

  @Mock
  private IndexReadSessionFactory sessionFactory;

  @Mock
  private IndexReadSession session;

  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private ApplicationDAO applicationDAO;

  private ApplicationsListService service;

  @BeforeEach
  public void setUp() {
    System.setProperty("nexusOne.search.readPath.applications", "new");
    lenient().when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2048);
    when(conversionHelper.stringToQuery(any())).thenReturn(new MatchAllDocsQuery());
    when(sessionFactory.open()).thenReturn(session);
    lenient().when(applicationRiskService.getApplicationRiskCards(isNull(), anySet(), isNull(), isNull(), isNull(),
        isNull(), isNull())).thenReturn(emptyRiskResults());
    service = new ApplicationsListService(
        searchIndexClient,
        applicationRiskService,
        new ApplicationsListIndexQueryBuilder(new DashboardIndexDimensionQueryBuilder(null, configuration),
            violationScopeResolver),
        requestValidator,
        new ApplicationsListFacetsBuilder(searchIndexClient, stageTypeService, organizationDAO, applicationDAO,
            conversionHelper),
        sessionFactory,
        conversionHelper);
  }

  @AfterEach
  public void tearDown() {
    System.clearProperty("nexusOne.search.readPath.applications");
  }

  @Test
  public void hybridOpenSearchPinnedSession_stageFacetUnsupported_keepsOrgFacets() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.page = 0;
    request.pageSize = 50;
    request.includeFacets = true;
    Document application = applicationDocument("app-1", "org-1", "Org 1");
    when(session.count(any())).thenReturn(1L);
    when(session.searchPage(any())).thenReturn(new IndexPageResult(List.of(application), List.of(), false));
    when(session.termsAggregation(any(), any(), anyInt()))
        .thenReturn(List.of(new IndexTermsBucket("org-1", 1)));
    when(stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT))
        .thenReturn(List.of(stage("build")));
    when(session.countDistinctGroupedBy(any(), any(), any(), anyList())).thenThrow(new UnsupportedOperationException(
        "IndexReadSession.countDistinctGroupedBy is implemented by Lucene until Track B docValues cardinality"));

    ApplicationsListResponseDTO response = service.listApplications(request);

    assertThat(response.facets).isNotNull();
    assertThat(response.facets.organizations).containsExactly(Map.entry("org-1", 1L));
    assertThat(response.facets.applications).containsExactly(Map.entry("app-1", 1L));
    assertThat(response.facets.stages).isNull();
    verify(searchIndexClient, never()).countDistinct(any(), anyList());
  }

  private static DashboardResultsDTO<ApplicationRiskScoreDTO> emptyRiskResults() {
    DashboardResultsDTO<ApplicationRiskScoreDTO> results = new DashboardResultsDTO<>();
    results.dashboardResults = List.of();
    return results;
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
