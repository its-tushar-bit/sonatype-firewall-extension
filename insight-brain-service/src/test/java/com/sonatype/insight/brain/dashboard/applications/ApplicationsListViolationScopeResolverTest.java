/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.dashboard.ViolationWaiverStatus;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.search.session.IndexPageResult;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexReadSessionFactory;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ApplicationsListViolationScopeResolverTest
{
  @Mock
  private SearchIndexClient searchIndexClient;

  @Mock
  private Configuration configuration;

  @Mock
  private IndexReadSessionFactory sessionFactory;

  @Mock
  private IndexReadSession session;

  @Mock
  private ConversionHelper conversionHelper;

  /** Paging behaviour under test is independent of which violation-scoped filters are set. */
  private final ApplicationsListRequestDTO noFilters = new ApplicationsListRequestDTO();

  @Test
  public void resolveApplicationIds_returnsWhenMatchCountEqualsMaxIdsOnShortPage() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(), anyList()))
        .thenReturn(partialPageWithApplicationIds("app-1", "app-2"));

    ApplicationsListViolationScopeResolver resolver =
        new ApplicationsListViolationScopeResolver(searchIndexClient, configuration);

    Set<String> applicationIds = resolver.resolveApplicationIds("itemType:APPLICATION", List.of(), noFilters);

    assertThat(applicationIds).containsExactly("app-1", "app-2");
  }

  @Test
  public void resolveApplicationIds_throwsWhenMaxIdsReachedOnFullPageWithRepeatedViolations() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(), anyList()))
        .thenReturn(fullPageWithApplicationIds("app-1", "app-2", "app-3"));

    ApplicationsListViolationScopeResolver resolver =
        new ApplicationsListViolationScopeResolver(searchIndexClient, configuration);

    assertThatThrownBy(() -> resolver.resolveApplicationIds("itemType:APPLICATION", List.of(), noFilters))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("too many applications");
  }

  @Test
  public void resolveApplicationIds_throwsWhenDiscoveryExceedsMaxIds() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(), anyList()))
        .thenReturn(fullPageHittingMaxIdsOnLastHit("app-1", "app-2"));

    ApplicationsListViolationScopeResolver resolver =
        new ApplicationsListViolationScopeResolver(searchIndexClient, configuration);

    assertThatThrownBy(() -> resolver.resolveApplicationIds("itemType:APPLICATION", List.of(), noFilters))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("too many applications");
  }

  @Test
  public void resolveApplicationIds_stopsWhenConsecutivePagesAddNoNewApplicationIds() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(), anyList()))
        .thenReturn(fullPageWithApplicationIds("app-1"));

    ApplicationsListViolationScopeResolver resolver =
        new ApplicationsListViolationScopeResolver(searchIndexClient, configuration);

    Set<String> applicationIds = resolver.resolveApplicationIds("itemType:APPLICATION", List.of(), noFilters);

    assertThat(applicationIds).containsExactly("app-1");
  }

  @Test
  public void resolveApplicationIds_skipsBlankApplicationIdsWithoutThrowingOrInflatingIdCount() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);
    AtomicInteger pageCalls = new AtomicInteger();
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(), anyList()))
        .thenAnswer(invocation -> {
          pageCalls.incrementAndGet();
          return fullPageWithBlankApplicationIds();
        });

    ApplicationsListViolationScopeResolver resolver =
        new ApplicationsListViolationScopeResolver(searchIndexClient, configuration);

    Set<String> applicationIds = resolver.resolveApplicationIds("itemType:APPLICATION", List.of(), noFilters);

    assertThat(applicationIds).isEmpty();
    assertThat(pageCalls.get())
        .isEqualTo(ApplicationsListViolationScopeResolver.MAX_CONSECUTIVE_VIOLATION_PAGES_WITHOUT_NEW_APPLICATION_IDS);
  }

  @Test
  public void resolveApplicationIds_continuesPagingWhenEachPageAddsNewApplicationIds() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);
    AtomicInteger pageCalls = new AtomicInteger();
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(), anyList()))
        .thenAnswer(invocation -> {
          int page = pageCalls.getAndIncrement();
          if (page < 12) {
            return fullPageWithApplicationIds("app-" + page);
          }
          return partialPageWithApplicationIds();
        });

    ApplicationsListViolationScopeResolver resolver =
        new ApplicationsListViolationScopeResolver(searchIndexClient, configuration);

    Set<String> applicationIds = resolver.resolveApplicationIds("itemType:APPLICATION", List.of(), noFilters);

    assertThat(applicationIds).hasSize(12);
    assertThat(pageCalls.get()).isGreaterThanOrEqualTo(12);
  }

  @Test
  public void resolveApplicationIds_passesDisjointThreatRangesToQuerySupport() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(), anyList()))
        .thenReturn(partialPageWithApplicationIds("critical-app"));

    ApplicationsListViolationScopeResolver resolver =
        new ApplicationsListViolationScopeResolver(searchIndexClient, configuration);
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.policyThreatLevelRanges = List.of(
        new PolicyThreatLevelFilter(8, 10),
        new PolicyThreatLevelFilter(1, 1));

    Set<String> applicationIds = resolver.resolveApplicationIds("itemType:APPLICATION", List.of(), request);

    assertThat(applicationIds).containsExactly("critical-app");
  }

  /**
   * CLM-43211 acceptance scenario: Security + Open must AND onto one violation query so only
   * applications with a single violation that is both Security and open are discovered — not
   * applications that separately have some Security violation and some unrelated open violation.
   */
  @Test
  public void resolveApplicationIds_policyTypeAndState_andCombineOnOneViolationQuery() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(), anyList()))
        .thenReturn(partialPageWithApplicationIds("security-open-app"));

    ApplicationsListViolationScopeResolver resolver =
        new ApplicationsListViolationScopeResolver(searchIndexClient, configuration);
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.policyThreatCategories = new PolicyThreatCategoryFilter(Set.of(PolicyThreatCategory.SECURITY));
    request.policyViolationStates = new PolicyViolationStateFilter(Set.of(PolicyViolationState.OPEN));

    Set<String> applicationIds = resolver.resolveApplicationIds("itemType:APPLICATION", List.of(), request);

    assertThat(applicationIds).containsExactly("security-open-app");

    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    verify(searchIndexClient)
        .searchIndex(queryCaptor.capture(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(), anyList());
    assertThat(queryCaptor.getValue()).isEqualTo(
        "itemType:POLICY_VIOLATION"
            + " AND " + FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY.label + ":(security)"
            + " AND NOT (" + FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS.label + ":("
            + ViolationWaiverStatus.openExclusionStatuses() + "))");
  }

  @Test
  public void resolveApplicationIds_newReadPathUsesSessionSearchPage() {
    System.setProperty("nexusOne.search.readPath.applications", "new");
    try {
      when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);
      when(conversionHelper.stringToQuery(any())).thenReturn(new MatchAllDocsQuery());
      when(sessionFactory.open()).thenReturn(session);
      when(session.searchPage(any()))
          .thenReturn(new IndexPageResult(
              List.of(violationDocument("app-1"), violationDocument("app-2")),
              List.of(),
              false));

      ApplicationsListViolationScopeResolver resolver =
          new ApplicationsListViolationScopeResolver(searchIndexClient, configuration, sessionFactory,
              conversionHelper);

      Set<String> applicationIds = resolver.resolveApplicationIds("itemType:APPLICATION", List.of(), noFilters);

      assertThat(applicationIds).containsExactly("app-1", "app-2");
      verify(session).searchPage(any());
      verify(searchIndexClient, never()).searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(),
          anyList());
    }
    finally {
      System.clearProperty("nexusOne.search.readPath.applications");
    }
  }

  private static SearchResultDTO fullPageWithApplicationIds(String... applicationIds) {
    return pageWithApplicationIds(500, applicationIds);
  }

  private static SearchResultDTO fullPageWithBlankApplicationIds() {
    SearchResultDTO searchResult = new SearchResultDTO();
    GroupingByDTO group = new GroupingByDTO();
    List<SearchResultItemDTO> items = new ArrayList<>(500);
    for (int i = 0; i < 500; i++) {
      SearchResultItemDTO item = new SearchResultItemDTO();
      item.applicationId = null;
      items.add(item);
    }
    group.searchResultItemDTOS = items;
    searchResult.groupingByDTOS = List.of(group);
    return searchResult;
  }

  private static SearchResultDTO fullPageHittingMaxIdsOnLastHit(String firstId, String lastId) {
    SearchResultDTO searchResult = new SearchResultDTO();
    GroupingByDTO group = new GroupingByDTO();
    List<SearchResultItemDTO> items = new ArrayList<>(500);
    for (int i = 0; i < 499; i++) {
      SearchResultItemDTO item = new SearchResultItemDTO();
      item.applicationId = firstId;
      items.add(item);
    }
    SearchResultItemDTO lastItem = new SearchResultItemDTO();
    lastItem.applicationId = lastId;
    items.add(lastItem);
    group.searchResultItemDTOS = items;
    searchResult.groupingByDTOS = List.of(group);
    return searchResult;
  }

  private static SearchResultDTO partialPageWithApplicationIds(String... applicationIds) {
    return pageWithApplicationIds(applicationIds.length, applicationIds);
  }

  private static SearchResultDTO pageWithApplicationIds(int hitCount, String... applicationIds) {
    SearchResultDTO searchResult = new SearchResultDTO();
    GroupingByDTO group = new GroupingByDTO();
    List<SearchResultItemDTO> items = new ArrayList<>(hitCount);
    for (int i = 0; i < hitCount; i++) {
      SearchResultItemDTO item = new SearchResultItemDTO();
      item.applicationId = applicationIds.length == 0 ? null : applicationIds[i % applicationIds.length];
      items.add(item);
    }
    group.searchResultItemDTOS = items;
    searchResult.groupingByDTOS = List.of(group);
    return searchResult;
  }

  private static Document violationDocument(final String applicationId) {
    Document document = new Document();
    document.add(new org.apache.lucene.document.TextField(
        "applicationId", applicationId, org.apache.lucene.document.Field.Store.YES));
    return document;
  }
}
