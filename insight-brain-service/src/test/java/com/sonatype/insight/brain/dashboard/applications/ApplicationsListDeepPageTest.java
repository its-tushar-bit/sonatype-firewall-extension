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
import com.sonatype.insight.brain.search.session.IndexPageRequest;
import com.sonatype.insight.brain.search.session.IndexPageResult;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexReadSessionFactory;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ApplicationsListDeepPageTest
{
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
    System.setProperty("nexusOne.search.readPath.applications", "new");
    lenient().when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2048);
    lenient().when(conversionHelper.stringToQuery(any())).thenReturn(new MatchAllDocsQuery());
    lenient().when(sessionFactory.open()).thenReturn(session);
    lenient()
        .when(applicationRiskService.getApplicationRiskCards(isNull(), anySet(), isNull(), isNull(), isNull(), isNull(),
            isNull()))
        .thenReturn(emptyRiskResults());
    service = new ApplicationsListService(
        null,
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
  public void page201_rejected() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.page = ApplicationsListService.MAX_WALKABLE_PAGE + 1;
    request.pageSize = ApplicationsListService.DEFAULT_PAGE_SIZE;
    request.includeFacets = false;

    assertThatThrownBy(() -> service.listApplications(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Page must be <= " + ApplicationsListService.MAX_WALKABLE_PAGE);
  }

  @Test
  public void pastEnd_shortCircuitsWithoutWalk() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.page = 1;
    request.pageSize = 50;
    request.includeFacets = false;
    when(session.count(any())).thenReturn(10L);

    ApplicationsListResponseDTO response = service.listApplications(request);

    assertThat(response.total).isEqualTo(10);
    assertThat(response.applications).isEmpty();
    assertThat(response.hasNextPage).isFalse();
    verify(session, never()).searchPage(any());
  }

  @Test
  public void walksOnlyToRequestedPageUsingSearchAfter() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.page = 2;
    request.pageSize = 1;
    request.includeFacets = false;
    when(session.count(any())).thenReturn(3L);
    when(session.searchPage(any()))
        .thenReturn(new IndexPageResult(List.of(applicationDocument("app-1")), List.of("after-1"), true))
        .thenReturn(new IndexPageResult(List.of(applicationDocument("app-2")), List.of("after-2"), true))
        .thenReturn(new IndexPageResult(List.of(applicationDocument("app-3")), List.of(), false));

    ApplicationsListResponseDTO response = service.listApplications(request);

    assertThat(response.total).isEqualTo(3);
    assertThat(response.applications).extracting(card -> card.applicationId).containsExactly("app-3");
    ArgumentCaptor<IndexPageRequest> pageRequestCaptor = ArgumentCaptor.forClass(IndexPageRequest.class);
    verify(session, org.mockito.Mockito.times(3)).searchPage(pageRequestCaptor.capture());
    SortField[] sortFields = pageRequestCaptor.getAllValues().get(0).sort().getSort();
    assertThat(Arrays.stream(sortFields).map(SortField::getField).toList())
        .containsExactly(
            FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label,
            FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label,
            FieldIdentifier.DOCUMENT_KEY.label);
  }

  private static DashboardResultsDTO<ApplicationRiskScoreDTO> emptyRiskResults() {
    DashboardResultsDTO<ApplicationRiskScoreDTO> results = new DashboardResultsDTO<>();
    results.dashboardResults = List.of();
    return results;
  }

  static org.apache.lucene.document.Document applicationDocument(final String applicationId) {
    org.apache.lucene.document.Document document = new org.apache.lucene.document.Document();
    document.add(new org.apache.lucene.document.TextField("itemType", "APPLICATION",
        org.apache.lucene.document.Field.Store.YES));
    document.add(new org.apache.lucene.document.TextField("applicationId", applicationId,
        org.apache.lucene.document.Field.Store.YES));
    document.add(new org.apache.lucene.document.TextField("applicationPublicId", applicationId,
        org.apache.lucene.document.Field.Store.YES));
    document.add(new org.apache.lucene.document.TextField("applicationName", applicationId,
        org.apache.lucene.document.Field.Store.YES));
    document.add(new org.apache.lucene.document.TextField("organizationId", "org-1",
        org.apache.lucene.document.Field.Store.YES));
    document.add(new org.apache.lucene.document.TextField("organizationName", "Org 1",
        org.apache.lucene.document.Field.Store.YES));
    return document;
  }
}
