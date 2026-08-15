/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.dashboard.ApplicationRiskScoreDTO;
import com.sonatype.insight.brain.dashboard.ApplicationRiskService;
import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.DashboardResultsDTO;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.search.session.IndexPageRequest;
import com.sonatype.insight.brain.search.session.IndexPageResult;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexReadSessionFactory;
import com.sonatype.insight.brain.service.Configuration;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.SortField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ApplicationsListPaginationParityTest
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
  private ApplicationsListFacetsBuilder facetsBuilder;

  @Mock
  private Configuration configuration;

  @Mock
  private ConversionHelper conversionHelper;

  @Mock
  private IndexReadSessionFactory sessionFactory;

  @Mock
  private IndexReadSession session;

  private ApplicationsListService service;

  @BeforeEach
  public void setUp() {
    lenient().when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2048);
    when(conversionHelper.stringToQuery(any())).thenReturn(new MatchAllDocsQuery());
    when(sessionFactory.open()).thenReturn(session);
    lenient()
        .when(applicationRiskService.getApplicationRiskCards(isNull(), anySet(), isNull(), isNull(), isNull(), isNull(),
            isNull()))
        .thenReturn(emptyRiskResults());
    service = new ApplicationsListService(
        searchIndexClient,
        applicationRiskService,
        new ApplicationsListIndexQueryBuilder(new DashboardIndexDimensionQueryBuilder(null, configuration),
            violationScopeResolver),
        requestValidator,
        facetsBuilder,
        sessionFactory,
        conversionHelper);
  }

  @AfterEach
  public void tearDown() {
    System.clearProperty("nexusOne.search.readPath.applications");
  }

  @Test
  public void page0_oldVsNew_byteIdenticalRowsAndTotal() {
    ApplicationsListRequestDTO request = request();
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(), anyList()))
        .thenReturn(searchResult("app-1", "app-2"));
    when(session.count(any())).thenReturn(2L);
    when(session.searchPage(any())).thenReturn(new IndexPageResult(List.of(
        ApplicationsListDeepPageTest.applicationDocument("app-1"),
        ApplicationsListDeepPageTest.applicationDocument("app-2")), List.of(), false));

    System.setProperty("nexusOne.search.readPath.applications", "old");
    ApplicationsListResponseDTO oldResponse = service.listApplications(request);
    System.setProperty("nexusOne.search.readPath.applications", "new");
    ApplicationsListResponseDTO newResponse = service.listApplications(request);

    assertEquivalent(oldResponse, newResponse);
  }

  @Test
  public void flagFlip_sameRequest_byteIdenticalAndUsesOneBackendPerFlag() {
    ApplicationsListRequestDTO request = request();
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(), anyList()))
        .thenReturn(searchResult("app-1"));
    when(session.count(any())).thenReturn(1L);
    when(session.searchPage(any())).thenReturn(new IndexPageResult(
        List.of(ApplicationsListDeepPageTest.applicationDocument("app-1")), List.of(), false));

    System.setProperty("nexusOne.search.readPath.applications", "old");
    ApplicationsListResponseDTO oldResponse = service.listApplications(request);
    System.setProperty("nexusOne.search.readPath.applications", "new");
    ApplicationsListResponseDTO newResponse = service.listApplications(request);

    assertEquivalent(oldResponse, newResponse);
    verify(searchIndexClient, times(1)).searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(),
        anyList());
    ArgumentCaptor<IndexPageRequest> pageRequestCaptor = ArgumentCaptor.forClass(IndexPageRequest.class);
    verify(session, times(1)).searchPage(pageRequestCaptor.capture());
    SortField[] sortFields = pageRequestCaptor.getValue().sort().getSort();
    assertThat(Arrays.stream(sortFields).map(SortField::getField).toList())
        .containsExactly(
            FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label,
            FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label,
            FieldIdentifier.DOCUMENT_KEY.label);
  }

  private static ApplicationsListRequestDTO request() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.page = 0;
    request.pageSize = 50;
    request.includeFacets = false;
    return request;
  }

  private static DashboardResultsDTO<ApplicationRiskScoreDTO> emptyRiskResults() {
    DashboardResultsDTO<ApplicationRiskScoreDTO> results = new DashboardResultsDTO<>();
    results.dashboardResults = List.of();
    return results;
  }

  private static SearchResultDTO searchResult(final String... applicationIds) {
    SearchResultDTO result = new SearchResultDTO();
    result.totalNumberOfHits = applicationIds.length;
    GroupingByDTO group = new GroupingByDTO();
    for (String applicationId : applicationIds) {
      SearchResultItemDTO item = new SearchResultItemDTO();
      item.itemType = "APPLICATION";
      item.applicationId = applicationId;
      item.applicationPublicId = applicationId;
      item.applicationName = applicationId;
      item.organizationId = "org-1";
      item.organizationName = "Org 1";
      group.searchResultItemDTOS.add(item);
    }
    result.groupingByDTOS = List.of(group);
    return result;
  }

  private static void assertEquivalent(
      final ApplicationsListResponseDTO oldResponse,
      final ApplicationsListResponseDTO newResponse)
  {
    assertThat(newResponse.total).isEqualTo(oldResponse.total);
    assertThat(newResponse.page).isEqualTo(oldResponse.page);
    assertThat(newResponse.pageSize).isEqualTo(oldResponse.pageSize);
    assertThat(newResponse.hasNextPage).isEqualTo(oldResponse.hasNextPage);
    assertThat(newResponse.source).isEqualTo(oldResponse.source);
    assertThat(newResponse.applications)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyElementsOf(oldResponse.applications);
  }
}
