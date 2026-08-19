/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.search.index.IndexFilterRestriction;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * Pages distinct {@code componentHash} rows from an index that returns multi-doc hits per hash
 * (CVE docs, per-app occurrences). Walks raw index pages until the requested distinct window is
 * filled so {@code hasNextPage} / {@code total} stay in distinct-hash units.
 */
final class ComponentsListDistinctPageFetcher
{
  /**
   * Over-fetch raw docs so each round-trip typically yields multiple new hashes under fold.
   * Bounded so a single request cannot scan unbounded raw pages.
   */
  static final int RAW_FETCH_PAGE_SIZE = 200;

  /** Max raw index pages walked per list request (200 × 80 = 16k docs). */
  static final int MAX_RAW_PAGES_PER_REQUEST = 80;

  /** Deep distinct pages beyond this require a tighter filter (estate-scale guard). */
  static final int MAX_DISTINCT_PAGE = 200;

  private final SearchIndexClient searchIndexClient;

  ComponentsListDistinctPageFetcher(final SearchIndexClient searchIndexClient) {
    this.searchIndexClient = searchIndexClient;
  }

  record DistinctPage(
      LinkedHashMap<String, SearchResultItemDTO> pageItems,
      Map<String, Set<String>> affectedApplicationIds,
      boolean hasNextPage)
  {
  }

  DistinctPage fetch(final String query, final int page, final int pageSize) {
    return fetch(query, List.of(), page, pageSize);
  }

  DistinctPage fetch(
      final String query,
      final List<? extends IndexFilterRestriction> termSets,
      final int page,
      final int pageSize)
  {
    // Callers (ComponentsListService) soft-clamp; keep a defensive clamp for direct use/tests.
    int safePage = Math.min(Math.max(page, 0), MAX_DISTINCT_PAGE);

    int distinctStart = safePage * pageSize;
    int distinctEnd = distinctStart + pageSize;

    LinkedHashMap<String, SearchResultItemDTO> ordered = new LinkedHashMap<>();
    Map<String, Set<String>> affectedApps = new LinkedHashMap<>();
    boolean exhausted = false;
    int rawPage = 0;
    List<? extends IndexFilterRestriction> restrictions = termSets == null ? List.of() : termSets;

    while (ordered.size() < distinctEnd && rawPage < MAX_RAW_PAGES_PER_REQUEST) {
      SearchResultDTO searchResult = searchIndexClient.searchIndex(
          query,
          RAW_FETCH_PAGE_SIZE,
          ComponentsListService.toSearchIndexPage(rawPage),
          false,
          false,
          List.of(),
          restrictions);
      int rawHits = countRawHits(searchResult);
      if (rawHits == 0) {
        exhausted = true;
        break;
      }

      mergeRawPage(searchResult, ordered, affectedApps);

      if (rawHits < RAW_FETCH_PAGE_SIZE) {
        exhausted = true;
        break;
      }
      rawPage++;
    }

    if (!exhausted && ordered.size() < distinctEnd && rawPage >= MAX_RAW_PAGES_PER_REQUEST) {
      throw new BadRequestException(
          "Component list distinct paging exceeded the maximum raw-page walk ("
              + MAX_RAW_PAGES_PER_REQUEST
              + "). Narrow filters or reduce page depth.");
    }

    LinkedHashMap<String, SearchResultItemDTO> pageItems = slice(ordered, distinctStart, distinctEnd);
    Map<String, Set<String>> pageAffectedApps = new LinkedHashMap<>();
    for (String hash : pageItems.keySet()) {
      pageAffectedApps.put(hash, affectedApps.getOrDefault(hash, Set.of()));
    }

    boolean hasNextPage = resolveHasNextPage(query, restrictions, ordered, distinctEnd, exhausted, rawPage);
    return new DistinctPage(pageItems, pageAffectedApps, hasNextPage);
  }

  /**
   * Prefer an overshoot ({@code ordered.size() > distinctEnd}). When the window is exact-full and
   * the last raw page was full, peek one more raw page for a <em>new</em> hash — do not assume
   * {@code hasNextPage} from a full raw page that may only re-fold already-seen hashes.
   */
  private boolean resolveHasNextPage(
      final String query,
      final List<? extends IndexFilterRestriction> termSets,
      final LinkedHashMap<String, SearchResultItemDTO> ordered,
      final int distinctEnd,
      final boolean exhausted,
      final int nextRawPage)
  {
    if (ordered.size() > distinctEnd) {
      return true;
    }
    if (ordered.size() < distinctEnd || exhausted || nextRawPage >= MAX_RAW_PAGES_PER_REQUEST) {
      return false;
    }
    SearchResultDTO peek = searchIndexClient.searchIndex(
        query,
        RAW_FETCH_PAGE_SIZE,
        ComponentsListService.toSearchIndexPage(nextRawPage),
        false,
        false,
        List.of(),
        termSets);
    if (countRawHits(peek) == 0) {
      return false;
    }
    for (String hash : ComponentsListIndexItems.extractComponentItems(peek).keySet()) {
      if (!ordered.containsKey(hash)) {
        return true;
      }
    }
    return false;
  }

  private static void mergeRawPage(
      final SearchResultDTO searchResult,
      final LinkedHashMap<String, SearchResultItemDTO> ordered,
      final Map<String, Set<String>> affectedApps)
  {
    LinkedHashMap<String, SearchResultItemDTO> pageHashes =
        ComponentsListIndexItems.extractComponentItems(searchResult);
    Map<String, Set<String>> pageAffected =
        ComponentsListIndexItems.collectAffectedApplicationIds(searchResult);
    for (Map.Entry<String, SearchResultItemDTO> entry : pageHashes.entrySet()) {
      ordered.putIfAbsent(entry.getKey(), entry.getValue());
      affectedApps
          .computeIfAbsent(entry.getKey(), key -> new LinkedHashSet<>())
          .addAll(pageAffected.getOrDefault(entry.getKey(), Set.of()));
    }
  }

  private static LinkedHashMap<String, SearchResultItemDTO> slice(
      final LinkedHashMap<String, SearchResultItemDTO> ordered,
      final int start,
      final int end)
  {
    LinkedHashMap<String, SearchResultItemDTO> page = new LinkedHashMap<>();
    if (start >= ordered.size()) {
      return page;
    }
    List<Map.Entry<String, SearchResultItemDTO>> entries = new ArrayList<>(ordered.entrySet());
    int to = Math.min(end, entries.size());
    for (int i = start; i < to; i++) {
      Map.Entry<String, SearchResultItemDTO> entry = entries.get(i);
      page.put(entry.getKey(), entry.getValue());
    }
    return page;
  }

  private static int countRawHits(final SearchResultDTO searchResult) {
    if (searchResult == null || searchResult.groupingByDTOS == null) {
      return 0;
    }
    int rawHits = 0;
    for (var group : searchResult.groupingByDTOS) {
      if (group != null && group.searchResultItemDTOS != null) {
        rawHits += group.searchResultItemDTOS.size();
      }
    }
    return rawHits;
  }
}
