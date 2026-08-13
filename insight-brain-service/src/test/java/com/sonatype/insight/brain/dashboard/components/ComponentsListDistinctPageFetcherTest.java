/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ComponentsListDistinctPageFetcherTest
{
  @Mock
  private SearchIndexClient searchIndexClient;

  @BeforeEach
  public void setUp() {
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenAnswer(invocation -> {
          int searchIndexPage = invocation.getArgument(2);
          // toSearchIndexPage: 0 → 0, 1 → 2, 2 → 3
          if (searchIndexPage == 0) {
            return pageWithHashes(ComponentsListDistinctPageFetcher.RAW_FETCH_PAGE_SIZE, "hash-a", "hash-b");
          }
          if (searchIndexPage == 2) {
            return pageWithHashes(ComponentsListDistinctPageFetcher.RAW_FETCH_PAGE_SIZE, "hash-c", "hash-d");
          }
          if (searchIndexPage == 3) {
            return pageWithHashes(3, "hash-e");
          }
          return pageWithHashes(0);
        });
  }

  @Test
  public void fetch_foldsMultiDocHitsAndPagesDistinctHashes() {
    ComponentsListDistinctPageFetcher fetcher = new ComponentsListDistinctPageFetcher(searchIndexClient);

    ComponentsListDistinctPageFetcher.DistinctPage page0 = fetcher.fetch("itemType:COMPONENT", 0, 2);
    assertThat(page0.pageItems().keySet()).containsExactly("hash-a", "hash-b");
    assertThat(page0.hasNextPage()).isTrue();

    ComponentsListDistinctPageFetcher.DistinctPage page1 = fetcher.fetch("itemType:COMPONENT", 1, 2);
    assertThat(page1.pageItems().keySet()).containsExactly("hash-c", "hash-d");
    assertThat(page1.hasNextPage()).isTrue();

    ComponentsListDistinctPageFetcher.DistinctPage page2 = fetcher.fetch("itemType:COMPONENT", 2, 2);
    assertThat(page2.pageItems().keySet()).containsExactly("hash-e");
    assertThat(page2.hasNextPage()).isFalse();
  }

  @Test
  public void fetch_hasNextPageFalseWhenDistinctWindowExhausted() {
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(pageWithHashes(2, "only-a", "only-b"));

    ComponentsListDistinctPageFetcher fetcher = new ComponentsListDistinctPageFetcher(searchIndexClient);
    ComponentsListDistinctPageFetcher.DistinctPage page = fetcher.fetch("q", 0, 50);

    assertThat(page.pageItems()).hasSize(2);
    assertThat(page.hasNextPage()).isFalse();
  }

  @Test
  public void fetch_hasNextPageFalseWhenFullRawPagesOnlyRefoldKnownHashes() {
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(pageWithHashes(ComponentsListDistinctPageFetcher.RAW_FETCH_PAGE_SIZE, "hash-a", "hash-b"));

    ComponentsListDistinctPageFetcher fetcher = new ComponentsListDistinctPageFetcher(searchIndexClient);
    ComponentsListDistinctPageFetcher.DistinctPage page = fetcher.fetch("q", 0, 2);

    assertThat(page.pageItems().keySet()).containsExactly("hash-a", "hash-b");
    assertThat(page.hasNextPage()).isFalse();
  }

  @Test
  public void fetch_clampsDeepDistinctPages() {
    when(searchIndexClient.searchIndex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(pageWithHashes(2, "only-a", "only-b"));

    ComponentsListDistinctPageFetcher fetcher = new ComponentsListDistinctPageFetcher(searchIndexClient);
    ComponentsListDistinctPageFetcher.DistinctPage page =
        fetcher.fetch("q", ComponentsListDistinctPageFetcher.MAX_DISTINCT_PAGE + 5, 10);

    // Soft-clamp to MAX_DISTINCT_PAGE rather than 400 — empty/last window for a stale deep link.
    assertThat(page.pageItems()).isEmpty();
    assertThat(page.hasNextPage()).isFalse();
  }

  private static SearchResultDTO pageWithHashes(final int hitCount, final String... hashes) {
    SearchResultDTO searchResult = new SearchResultDTO();
    GroupingByDTO group = new GroupingByDTO();
    List<SearchResultItemDTO> items = new ArrayList<>(Math.max(hitCount, 0));
    for (int i = 0; i < hitCount; i++) {
      SearchResultItemDTO item = new SearchResultItemDTO();
      item.componentHash = hashes[i % hashes.length];
      item.applicationId = "app-" + (i % 3);
      item.itemType = "SECURITY_VULNERABILITY";
      items.add(item);
    }
    group.searchResultItemDTOS = items;
    searchResult.groupingByDTOS = List.of(group);
    return searchResult;
  }
}
