/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.session.IndexPageRequest;
import com.sonatype.insight.brain.search.session.IndexPageResult;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexReadSessionFactory;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit coverage for the IndexReadSession violations list path (searchAfter walk, walkable-page guard,
 * hasNextPage). Enabled via {@code nexusOne.search.readPath.violations=new}.
 */
@ExtendWith(MockitoExtension.class)
public class ViolationsListServiceSessionTest
{
  private static final String QUERY = "itemType:POLICY_VIOLATION";

  private static final Query SESSION_QUERY = new MatchAllDocsQuery();

  @Mock
  private SearchIndexClient searchIndexClient;

  @Mock
  private ViolationsListIndexQueryBuilder indexQueryBuilder;

  @Mock
  private ViolationsListRequestValidator requestValidator;

  @Mock
  private ViolationsListFacetsBuilder facetsBuilder;

  @Mock
  private IndexReadSessionFactory sessionFactory;

  @Mock
  private ConversionHelper conversionHelper;

  @Mock
  private PolicyViolationDAO policyViolationDAO;

  @Mock
  private IndexReadSession session;

  @BeforeEach
  public void setUp() {
    System.setProperty("nexusOne.search.readPath.violations", "new");
    when(sessionFactory.open()).thenReturn(session);
    when(conversionHelper.stringToQuery(anyString())).thenReturn(SESSION_QUERY);
    when(indexQueryBuilder.buildViolationQuery(any())).thenReturn(QUERY);
    lenient().when(indexQueryBuilder.buildViolationQueryExcludingWaiverType(any())).thenReturn(QUERY);
    lenient().when(policyViolationDAO.getByIds(any())).thenReturn(List.of());
  }

  @AfterEach
  public void tearDown() {
    System.clearProperty("nexusOne.search.readPath.violations");
  }

  private ViolationsListService service() {
    return new ViolationsListService(
        searchIndexClient,
        indexQueryBuilder,
        requestValidator,
        facetsBuilder,
        sessionFactory,
        conversionHelper,
        policyViolationDAO);
  }

  private static ViolationsListRequestDTO request(final int page, final int pageSize) {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.page = page;
    request.pageSize = pageSize;
    request.includeFacets = false;
    return request;
  }

  private static Document violationDoc(final String policyViolationId) {
    Document document = new Document();
    document.add(new StringField(FieldIdentifier.ITEM_TYPE.label, ItemType.POLICY_VIOLATION.name(), Store.YES));
    document.add(new StringField(FieldIdentifier.POLICY_VIOLATION_ID.label, policyViolationId, Store.YES));
    return document;
  }

  @Test
  public void sessionPath_requestsThreatLevelSortDescendingByDefault() {
    when(session.count(SESSION_QUERY)).thenReturn(1L);
    AtomicReference<Sort> seenSort = new AtomicReference<>();
    when(session.searchPage(any(IndexPageRequest.class))).thenAnswer(invocation -> {
      IndexPageRequest req = invocation.getArgument(0);
      seenSort.set(req.sort());
      return new IndexPageResult(List.of(violationDoc("pv-1")), List.of(), false);
    });

    service().listViolations(request(0, 10));

    SortField[] fields = seenSort.get().getSort();
    assertThat(fields).hasSize(2);
    assertThat(fields[0].getField()).isEqualTo(FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label);
    assertThat(fields[0].getReverse()).isTrue();
    assertThat(fields[1].getField()).isEqualTo(FieldIdentifier.DOCUMENT_KEY.label);
  }

  @Test
  public void sessionPath_requestsThreatLevelSortAscendingWhenRequested() {
    when(session.count(SESSION_QUERY)).thenReturn(1L);
    AtomicReference<Sort> seenSort = new AtomicReference<>();
    when(session.searchPage(any(IndexPageRequest.class))).thenAnswer(invocation -> {
      IndexPageRequest req = invocation.getArgument(0);
      seenSort.set(req.sort());
      return new IndexPageResult(List.of(violationDoc("pv-1")), List.of(), false);
    });

    ViolationsListRequestDTO request = request(0, 10);
    request.orderBy = "policyThreatLevel";
    service().listViolations(request);

    SortField[] fields = seenSort.get().getSort();
    assertThat(fields).hasSize(2);
    assertThat(fields[0].getField()).isEqualTo(FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label);
    assertThat(fields[0].getReverse()).isFalse();
    assertThat(fields[1].getField()).isEqualTo(FieldIdentifier.DOCUMENT_KEY.label);
  }

  @Test
  public void listViolations_session_pageZero_walksOnceAndReturnsFirstPage() {
    when(session.count(SESSION_QUERY)).thenReturn(3L);
    when(session.searchPage(any(IndexPageRequest.class)))
        .thenReturn(new IndexPageResult(List.of(violationDoc("pv-1"), violationDoc("pv-2")), List.of("after-0"), true));

    ViolationsListResponseDTO response = service().listViolations(request(0, 2));

    assertThat(response.violations).extracting(row -> row.policyViolationId).containsExactly("pv-1", "pv-2");
    assertThat(response.total).isEqualTo(3);
    assertThat(response.hasNextPage).isTrue();
    verify(session, times(1)).searchPage(any(IndexPageRequest.class));
    verify(searchIndexClient, never()).searchIndex(anyString(), any(Integer.class), any(Integer.class),
        any(Boolean.class), any(Boolean.class), any(List.class));
  }

  @Test
  public void listViolations_session_pageN_walksNPlusOneSearchPages() {
    when(session.count(SESSION_QUERY)).thenReturn(250L);
    AtomicInteger walks = new AtomicInteger();
    when(session.searchPage(any(IndexPageRequest.class))).thenAnswer(invocation -> {
      int n = walks.getAndIncrement();
      return new IndexPageResult(
          List.of(violationDoc("pv-page-" + n)),
          List.of("after-" + n),
          true);
    });

    ViolationsListResponseDTO response = service().listViolations(request(3, 50));

    assertThat(walks.get()).isEqualTo(4);
    assertThat(response.violations).extracting(row -> row.policyViolationId).containsExactly("pv-page-3");
    assertThat(response.hasNextPage).isTrue();
  }

  @Test
  public void listViolations_session_pageBeyondMaxWalkable_withHitsRemaining_throwsBadRequest() {
    when(session.count(SESSION_QUERY)).thenReturn(50_000L);

    assertThatThrownBy(() -> service().listViolations(request(ViolationsListService.MAX_WALKABLE_PAGE + 1, 50)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Page must be <= " + ViolationsListService.MAX_WALKABLE_PAGE);
    verify(session, never()).searchPage(any(IndexPageRequest.class));
  }

  @Test
  public void listViolations_session_pageBeyondMaxWalkable_pastTotal_returnsEmptyWithoutThrow() {
    // page * pageSize >= total → empty page, even when page > MAX_WALKABLE_PAGE.
    when(session.count(SESSION_QUERY)).thenReturn(100L);

    ViolationsListResponseDTO response =
        service().listViolations(request(ViolationsListService.MAX_WALKABLE_PAGE + 1, 50));

    assertThat(response.violations).isEmpty();
    assertThat(response.total).isEqualTo(100);
    assertThat(response.hasNextPage).isFalse();
    verify(session, never()).searchPage(any(IndexPageRequest.class));
  }

  @Test
  public void listViolations_session_includeFacets_buildsFacetsOnSharedSession() {
    when(session.count(SESSION_QUERY)).thenReturn(2L);
    when(session.searchPage(any(IndexPageRequest.class)))
        .thenReturn(new IndexPageResult(List.of(violationDoc("pv-1")), List.of(), false));
    ViolationsListFacetsDTO facets = new ViolationsListFacetsDTO();
    facets.totalViolations = 2;
    when(facetsBuilder.buildFacets(session, QUERY, QUERY, 2L, null, null)).thenReturn(facets);
    when(indexQueryBuilder.buildViolationQueryExcludingWaiverType(any())).thenReturn(QUERY);

    ViolationsListRequestDTO request = request(0, 50);
    request.includeFacets = true;
    ViolationsListResponseDTO response = service().listViolations(request);

    assertThat(response.facets).isSameAs(facets);
    verify(facetsBuilder).buildFacets(session, QUERY, QUERY, 2L, null, null);
  }

  @Test
  public void listViolations_session_exactLastPage_hasNextPageFalse() {
    when(session.count(SESSION_QUERY)).thenReturn(100L);
    // Walk page 0 then page 1; each window is a full pageSize so consumed == total.
    when(session.searchPage(any(IndexPageRequest.class))).thenAnswer(invocation -> {
      IndexPageRequest pageRequest = invocation.getArgument(0);
      List<Document> docs = java.util.stream.IntStream.range(0, pageRequest.pageSize())
          .mapToObj(i -> violationDoc("pv-" + pageRequest.searchAfter().size() + "-" + i))
          .toList();
      return new IndexPageResult(docs, List.of("after"), true);
    });

    ViolationsListResponseDTO response = service().listViolations(request(1, 50));

    // consumed = 1*50 + 50 = 100 == total → no next page.
    assertThat(response.violations).hasSize(50);
    assertThat(response.hasNextPage).isFalse();
    verify(session, times(2)).searchPage(any(IndexPageRequest.class));
  }

  @Test
  public void listViolations_session_atMaxWalkablePage_withHitsRemaining_hasNextPageFalse() {
    // Client must not see hasNextPage=true when the next page would 400 at the walkable guard.
    long total = ((long) ViolationsListService.MAX_WALKABLE_PAGE + 2) * 50;
    when(session.count(SESSION_QUERY)).thenReturn(total);
    when(session.searchPage(any(IndexPageRequest.class))).thenAnswer(invocation -> {
      IndexPageRequest pageRequest = invocation.getArgument(0);
      List<Document> docs = java.util.stream.IntStream.range(0, pageRequest.pageSize())
          .mapToObj(i -> violationDoc("pv-max-" + pageRequest.searchAfter().size() + "-" + i))
          .toList();
      return new IndexPageResult(docs, List.of("after"), true);
    });

    ViolationsListResponseDTO response =
        service().listViolations(request(ViolationsListService.MAX_WALKABLE_PAGE, 50));

    assertThat(response.violations).hasSize(50);
    assertThat(response.hasNextPage).isFalse();
  }
}
