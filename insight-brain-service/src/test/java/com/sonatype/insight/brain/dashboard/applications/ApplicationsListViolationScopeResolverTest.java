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

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

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
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationsListViolationScopeResolverTest
{
  @Mock
  private SearchIndexClient searchIndexClient;

  @Mock
  private Configuration configuration;

  @Test
  public void resolveApplicationIds_returnsWhenMatchCountEqualsMaxIdsOnShortPage() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(partialPageWithApplicationIds("app-1", "app-2"));

    ApplicationsListViolationScopeResolver resolver =
        new ApplicationsListViolationScopeResolver(searchIndexClient, configuration);

    Set<String> applicationIds = resolver.resolveApplicationIds("itemType:APPLICATION", null, List.of());

    assertThat(applicationIds).containsExactly("app-1", "app-2");
  }

  @Test
  public void resolveApplicationIds_throwsWhenMaxIdsReachedOnFullPageWithRepeatedViolations() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(fullPageWithApplicationIds("app-1", "app-2", "app-3"));

    ApplicationsListViolationScopeResolver resolver =
        new ApplicationsListViolationScopeResolver(searchIndexClient, configuration);

    assertThatThrownBy(() -> resolver.resolveApplicationIds("itemType:APPLICATION", null, List.of()))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("too many applications");
  }

  @Test
  public void resolveApplicationIds_throwsWhenDiscoveryExceedsMaxIds() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(fullPageHittingMaxIdsOnLastHit("app-1", "app-2"));

    ApplicationsListViolationScopeResolver resolver =
        new ApplicationsListViolationScopeResolver(searchIndexClient, configuration);

    assertThatThrownBy(() -> resolver.resolveApplicationIds("itemType:APPLICATION", null, List.of()))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("too many applications");
  }

  @Test
  public void resolveApplicationIds_stopsWhenConsecutivePagesAddNoNewApplicationIds() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(fullPageWithApplicationIds("app-1"));

    ApplicationsListViolationScopeResolver resolver =
        new ApplicationsListViolationScopeResolver(searchIndexClient, configuration);

    Set<String> applicationIds = resolver.resolveApplicationIds("itemType:APPLICATION", null, List.of());

    assertThat(applicationIds).containsExactly("app-1");
  }

  @Test
  public void resolveApplicationIds_continuesPagingWhenEachPageAddsNewApplicationIds() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);
    AtomicInteger pageCalls = new AtomicInteger();
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenAnswer(invocation -> {
          int page = pageCalls.getAndIncrement();
          if (page < 12) {
            return fullPageWithApplicationIds("app-" + page);
          }
          return partialPageWithApplicationIds();
        });

    ApplicationsListViolationScopeResolver resolver =
        new ApplicationsListViolationScopeResolver(searchIndexClient, configuration);

    Set<String> applicationIds = resolver.resolveApplicationIds("itemType:APPLICATION", null, List.of());

    assertThat(applicationIds).hasSize(12);
    assertThat(pageCalls.get()).isGreaterThanOrEqualTo(12);
  }

  @Test
  public void resolveApplicationIds_passesDisjointThreatRangesToQuerySupport() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(partialPageWithApplicationIds("critical-app"));

    ApplicationsListViolationScopeResolver resolver =
        new ApplicationsListViolationScopeResolver(searchIndexClient, configuration);
    List<PolicyThreatLevelFilter> threatFilters = List.of(
        new PolicyThreatLevelFilter(8, 10),
        new PolicyThreatLevelFilter(1, 1));

    Set<String> applicationIds = resolver.resolveApplicationIds("itemType:APPLICATION", null, threatFilters);

    assertThat(applicationIds).containsExactly("critical-app");
  }

  private static SearchResultDTO fullPageWithApplicationIds(String... applicationIds) {
    return pageWithApplicationIds(500, applicationIds);
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
}
