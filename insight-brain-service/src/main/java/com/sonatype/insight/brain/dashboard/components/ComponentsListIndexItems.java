/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.apache.commons.lang3.StringUtils;

/**
 * Extracts component hits from index search results, folding to distinct {@code componentHash}.
 */
final class ComponentsListIndexItems
{
  private ComponentsListIndexItems() {
  }

  static LinkedHashMap<String, SearchResultItemDTO> extractComponentItems(final SearchResultDTO searchResult) {
    LinkedHashMap<String, SearchResultItemDTO> items = new LinkedHashMap<>();
    if (searchResult == null || searchResult.groupingByDTOS == null) {
      return items;
    }
    for (var group : searchResult.groupingByDTOS) {
      if (group == null || group.searchResultItemDTOS == null) {
        continue;
      }
      for (SearchResultItemDTO item : group.searchResultItemDTOS) {
        if (!isComponentItem(item) || StringUtils.isBlank(item.componentHash)) {
          continue;
        }
        items.putIfAbsent(item.componentHash, item);
      }
    }
    return items;
  }

  /**
   * Collects distinct application ids seen for each component hash in the result window.
   * Used as a page-local fallback for {@code affectedApplications} when Classic SQL enrichment
   * does not return a card for that hash.
   */
  static Map<String, Set<String>> collectAffectedApplicationIds(final SearchResultDTO searchResult) {
    Map<String, Set<String>> affected = new LinkedHashMap<>();
    if (searchResult == null || searchResult.groupingByDTOS == null) {
      return affected;
    }
    for (var group : searchResult.groupingByDTOS) {
      if (group == null || group.searchResultItemDTOS == null) {
        continue;
      }
      for (SearchResultItemDTO item : group.searchResultItemDTOS) {
        if (!isComponentItem(item) || StringUtils.isBlank(item.componentHash)
            || StringUtils.isBlank(item.applicationId))
        {
          continue;
        }
        affected.computeIfAbsent(item.componentHash, key -> new LinkedHashSet<>()).add(item.applicationId);
      }
    }
    return affected;
  }

  private static boolean isComponentItem(final SearchResultItemDTO item) {
    if (item == null || item.itemType == null) {
      return false;
    }
    return ItemType.NON_VULNERABLE_COMPONENT.name().equals(item.itemType)
        || ItemType.SECURITY_VULNERABILITY.name().equals(item.itemType);
  }
}
