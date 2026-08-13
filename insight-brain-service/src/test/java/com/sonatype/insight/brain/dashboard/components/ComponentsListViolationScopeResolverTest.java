/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ComponentsListViolationScopeResolverTest
{
  @Mock
  private SearchIndexClient searchIndexClient;

  @Mock
  private Configuration configuration;

  @Test
  public void resolveComponentHashes_continuesPastHotHashPagesUntilLaterHashesAppear() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);
    AtomicInteger pageCalls = new AtomicInteger();
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenAnswer(invocation -> {
          int page = pageCalls.getAndIncrement();
          // First 15 full pages are the same hot hash — old consecutive-no-new guard would stop at 10.
          if (page < 15) {
            return fullPageWithHashes("hot-hash");
          }
          return partialPageWithHashes("later-hash");
        });

    ComponentsListViolationScopeResolver resolver =
        new ComponentsListViolationScopeResolver(searchIndexClient, configuration);

    Set<String> hashes = resolver.resolveComponentHashes(
        ComponentsListViolationQuerySupport.COMPONENT_ITEM_TYPE_CLAUSE, null, List.of());

    assertThat(hashes).containsExactly("hot-hash", "later-hash");
    assertThat(pageCalls.get()).isGreaterThan(10);
  }

  @Test
  public void resolveComponentHashes_throwsWhenMaxIdsReachedOnFullPage() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(fullPageWithHashes("h1", "h2", "h3"));

    ComponentsListViolationScopeResolver resolver =
        new ComponentsListViolationScopeResolver(searchIndexClient, configuration);

    assertThatThrownBy(() -> resolver.resolveComponentHashes(
        ComponentsListViolationQuerySupport.COMPONENT_ITEM_TYPE_CLAUSE, null, List.of()))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("too many components");
  }

  @Test
  public void resolveComponentHashes_throwsWhenRawPageBudgetExhausted() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(10_000);
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(fullPageWithHashes("same-hash"));

    ComponentsListViolationScopeResolver resolver =
        new ComponentsListViolationScopeResolver(searchIndexClient, configuration);

    assertThatThrownBy(() -> resolver.resolveComponentHashes(
        ComponentsListViolationQuerySupport.COMPONENT_ITEM_TYPE_CLAUSE, null, List.of()))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("maximum raw-page walk");
  }

  private static SearchResultDTO fullPageWithHashes(final String... hashes) {
    return pageWithHashes(500, hashes);
  }

  private static SearchResultDTO partialPageWithHashes(final String... hashes) {
    return pageWithHashes(hashes.length, hashes);
  }

  private static SearchResultDTO pageWithHashes(final int hitCount, final String... hashes) {
    SearchResultDTO searchResult = new SearchResultDTO();
    GroupingByDTO group = new GroupingByDTO();
    List<SearchResultItemDTO> items = new ArrayList<>(hitCount);
    for (int i = 0; i < hitCount; i++) {
      SearchResultItemDTO item = new SearchResultItemDTO();
      item.componentHash = hashes[i % hashes.length];
      items.add(item);
    }
    group.searchResultItemDTOS = items;
    searchResult.groupingByDTOS = List.of(group);
    return searchResult;
  }
}
